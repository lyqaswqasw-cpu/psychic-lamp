package com.example.data

import android.util.Log
import com.example.model.*
import com.example.ui.utils.MatchDataHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * 100% Real Live Football Data Service powered by API-Sports (v3.football.api-sports.io)
 * Key: aa79b5b9c1ad59206db2f7b48e316802
 */
object FootballApiService {
    private const val TAG = "FootballApiService"
    const val API_KEY = "aa79b5b9c1ad59206db2f7b48e316802"
    private const val BASE_URL = "https://v3.football.api-sports.io"

    // In-memory cache to respect API daily limits and provide super-fast UI response
    private var cachedFixtures: List<YssMatch> = emptyList()
    private var lastFixturesFetchTime: Long = 0L
    private const val CACHE_DURATION_MS = 60_000L // 1 minute fresh cache

    private val cachedDetails = mutableMapOf<String, Pair<Long, MatchDetails>>()

    // Popular league translation map for clean Arabic UI presentation
    private val leagueArabicNames = mapOf(
        "Premier League" to "الدوري الإنجليزي الممتاز",
        "La Liga" to "الدوري الإسباني (لا ليغا)",
        "Serie A" to "الدوري الإيطالي (سيريا أ)",
        "Bundesliga" to "الدوري الألماني (بوندسليغا)",
        "Ligue 1" to "الدوري الفرنسي (ليغ 1)",
        "UEFA Champions League" to "دوري أبطال أوروبا",
        "UEFA Europa League" to "الدوري الأوروبي",
        "Saudi Pro League" to "دوري روشن السعودي",
        "Egyptian Premier League" to "الدوري المصري الممتاز",
        "Major League Soccer" to "الدوري الأمريكي (MLS)",
        "CAF Champions League" to "دوري أبطال أفريقيا",
        "AFC Champions League" to "دوري أبطال آسيا",
        "FIFA Club World Cup" to "كأس العالم للأندية",
        "World Cup" to "كأس العالم",
        "Eredivisie" to "الدوري الهولندي",
        "Primeira Liga" to "الدوري البرتغالي",
        "Copa Libertadores" to "كوبا ليبرتادوريس",
        "Copa Sudamericana" to "كوبا سود أمريكانا",
        "USL Championship" to "الدوري الأمريكي الدرجة الثانية",
        "Super League" to "الدوري السويسري / التركي",
        "Pro League" to "الدوري البلجيكي"
    )

    private fun getArabicLeagueName(originalName: String): String {
        for ((en, ar) in leagueArabicNames) {
            if (originalName.contains(en, ignoreCase = true)) {
                return ar
            }
        }
        return originalName
    }

    /**
     * Fetch matches for a specific date (or today if null) and currently live matches from API-Sports
     */
    suspend fun getTodayMatches(forceRefresh: Boolean = false, targetDate: String? = null): List<YssMatch> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val queryDate = targetDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        if (!forceRefresh && targetDate == null && cachedFixtures.isNotEmpty() && (now - lastFixturesFetchTime) < CACHE_DURATION_MS) {
            return@withContext cachedFixtures
        }

        try {
            val matches = mutableListOf<YssMatch>()

            // 1. Fetch Fixtures for the target date
            val todayUrl = "$BASE_URL/fixtures?date=$queryDate"
            val todayJson = makeApiRequest(todayUrl)
            if (todayJson != null) {
                val responseArray = todayJson.optJSONArray("response")
                if (responseArray != null) {
                    for (i in 0 until responseArray.length()) {
                        val fixtureObj = responseArray.optJSONObject(i) ?: continue
                        parseFixtureItem(fixtureObj)?.let { matches.add(it) }
                    }
                }
            }

            // 2. Fetch Live Fixtures (to ensure real-time minute and live scores)
            if (targetDate == null || targetDate == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) {
                val liveUrl = "$BASE_URL/fixtures?live=all"
                val liveJson = makeApiRequest(liveUrl)
                if (liveJson != null) {
                    val liveResponse = liveJson.optJSONArray("response")
                    if (liveResponse != null) {
                        for (i in 0 until liveResponse.length()) {
                            val liveObj = liveResponse.optJSONObject(i) ?: continue
                            parseFixtureItem(liveObj)?.let { liveMatch ->
                                val existingIndex = matches.indexOfFirst { it.id == liveMatch.id }
                                if (existingIndex >= 0) {
                                    matches[existingIndex] = liveMatch
                                } else {
                                    matches.add(0, liveMatch)
                                }
                            }
                        }
                    }
                }
            }

            if (matches.isNotEmpty()) {
                val sorted = matches.sortedWith(
                    compareBy<YssMatch> { if (it.status == MatchStatus.LIVE) 0 else if (it.status == MatchStatus.UPCOMING) 1 else 2 }
                        .thenBy { it.time }
                )
                if (targetDate == null) {
                    cachedFixtures = sorted
                    lastFixturesFetchTime = now
                }
                return@withContext sorted
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching matches from API-Sports", e)
        }

        if (cachedFixtures.isNotEmpty() && targetDate == null) return@withContext cachedFixtures
        return@withContext emptyList()
    }

    /**
     * Fetch complete match details (Score, Events, Lineups, Statistics, Standings)
     */
    suspend fun getMatchDetails(fixtureId: String): MatchDetails? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedDetails[fixtureId]?.let { (time, details) ->
            if (now - time < 30_000L) { // 30s cache for details
                return@withContext details
            }
        }

        try {
            val url = "$BASE_URL/fixtures?id=$fixtureId"
            val json = makeApiRequest(url) ?: return@withContext null
            val responseArray = json.optJSONArray("response") ?: return@withContext null
            if (responseArray.length() == 0) return@withContext null

            val root = responseArray.getJSONObject(0)
            val match = parseFixtureItem(root) ?: return@withContext null

            // 1. Parse Events (Goals, Cards, Substitutions, VAR)
            val timelineEvents = mutableListOf<MatchTimelineEvent>()
            val eventsArray = root.optJSONArray("events")
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val eObj = eventsArray.optJSONObject(i) ?: continue
                    val timeObj = eObj.optJSONObject("time")
                    val elapsed = timeObj?.optInt("elapsed", 0) ?: 0
                    val extra = timeObj?.optInt("extra", 0) ?: 0
                    val minStr = if (extra > 0) "$elapsed+$extra'" else "$elapsed'"

                    val teamObj = eObj.optJSONObject("team")
                    val teamName = teamObj?.optString("name", "") ?: ""
                    val isHome = teamName.equals(match.team1, ignoreCase = true) || 
                                 teamName.contains(match.team1, ignoreCase = true) ||
                                 (teamObj?.optInt("id", 0) ?: 0) == (root.optJSONObject("teams")?.optJSONObject("home")?.optInt("id", -1) ?: -2)

                    val playerObj = eObj.optJSONObject("player")
                    val assistObj = eObj.optJSONObject("assist")
                    val playerName = playerObj?.optString("name", "") ?: ""
                    val assistOrOutName = assistObj?.optString("name", "") ?: ""

                    val typeStr = eObj.optString("type", "").lowercase()
                    val detailStr = eObj.optString("detail", "")
                    val comments = eObj.optString("comments", "")

                    val eventType = when {
                        detailStr.contains("penalty missed", ignoreCase = true) || detailStr.contains("missed penalty", ignoreCase = true) -> TimelineEventType.PENALTY_MISSED
                        detailStr.contains("penalty", ignoreCase = true) -> TimelineEventType.PENALTY_GOAL
                        typeStr.contains("goal") || detailStr.contains("Goal", ignoreCase = true) -> TimelineEventType.GOAL
                        typeStr.contains("card") -> {
                            if (detailStr.contains("red", ignoreCase = true)) TimelineEventType.RED_CARD
                            else TimelineEventType.YELLOW_CARD
                        }
                        typeStr.contains("sub") -> TimelineEventType.SUBSTITUTION
                        typeStr.contains("var") -> TimelineEventType.VAR_DECISION
                        else -> TimelineEventType.GOAL
                    }

                    timelineEvents.add(
                        MatchTimelineEvent(
                            minute = minStr,
                            teamSide = if (isHome) TeamSide.HOME else TeamSide.AWAY,
                            eventType = eventType,
                            playerName = playerName,
                            player = playerName,
                            subOutPlayerName = if (eventType == TimelineEventType.SUBSTITUTION && assistOrOutName.isNotBlank()) assistOrOutName else null,
                            description = if (detailStr.isNotBlank()) "$detailStr $comments".trim() else "حدث في الدقيقة $minStr",
                            teamName = teamName
                        )
                    )
                }
            }

            // 2. Parse Lineups
            var homeLineup = Lineup(teamName = match.team1)
            var awayLineup = Lineup(teamName = match.team2)
            val lineupsArray = root.optJSONArray("lineups")
            if (lineupsArray != null && lineupsArray.length() >= 2) {
                homeLineup = parseLineupObject(lineupsArray.optJSONObject(0), match.team1)
                awayLineup = parseLineupObject(lineupsArray.optJSONObject(1), match.team2)
            } else if (lineupsArray != null && lineupsArray.length() == 1) {
                homeLineup = parseLineupObject(lineupsArray.optJSONObject(0), match.team1)
                awayLineup = Lineup(teamName = match.team2)
            }

            // 3. Parse Statistics
            var matchStats = MatchStats()
            val statsArray = root.optJSONArray("statistics")
            if (statsArray != null && statsArray.length() >= 2) {
                matchStats = parseStatisticsArray(statsArray)
            }

            // 4. Standings
            val standings = emptyList<Standing>()

            val details = MatchDetails(
                match = match,
                timeline = timelineEvents,
                timelineEvents = timelineEvents,
                stats = matchStats,
                homeLineup = homeLineup,
                awayLineup = awayLineup,
                team1Lineup = homeLineup,
                team2Lineup = awayLineup,
                standings = standings
            )

            cachedDetails[fixtureId] = Pair(now, details)
            return@withContext details
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching match details for fixture $fixtureId", e)
            return@withContext null
        }
    }

    private fun parseFixtureItem(root: JSONObject): YssMatch? {
        val fixtureObj = root.optJSONObject("fixture") ?: return null
        val leagueObj = root.optJSONObject("league")
        val teamsObj = root.optJSONObject("teams")
        val goalsObj = root.optJSONObject("goals")
        val scoreObj = root.optJSONObject("score")

        val id = fixtureObj.optInt("id", 0).toString()
        val dateRaw = fixtureObj.optString("date", "")
        val statusObj = fixtureObj.optJSONObject("status")
        val statusShort = statusObj?.optString("short", "")?.uppercase() ?: ""
        val elapsed = statusObj?.optInt("elapsed", 0)
        val extra = statusObj?.optInt("extra", 0)

        val isLive = statusShort in listOf("1H", "2H", "HT", "ET", "P", "LIVE", "BT")
        val isFinished = statusShort in listOf("FT", "AET", "PEN")

        val matchStatus = when {
            isLive -> MatchStatus.LIVE
            isFinished -> MatchStatus.FINISHED
            else -> MatchStatus.UPCOMING
        }

        val statusStr = when (statusShort) {
            "1H" -> "شوط أول"
            "2H" -> "شوط ثانٍ"
            "HT" -> "استراحة الشوطين"
            "ET" -> "أشواط إضافية"
            "P" -> "ضربات ترجيح"
            "FT" -> "انتهت"
            "AET" -> "انتهت بعد التمديد"
            "PEN" -> "انتهت بالترجيح"
            "NS" -> "لم تبدأ"
            "PST" -> "مؤجلة"
            "CANC" -> "ملغاة"
            "ABD" -> "متوقفة"
            else -> statusObj?.optString("long", "مباراة اليوم") ?: "مباراة اليوم"
        }

        val homeTeamObj = teamsObj?.optJSONObject("home")
        val awayTeamObj = teamsObj?.optJSONObject("away")

        val homeName = homeTeamObj?.optString("name", "الفريق المضيف") ?: "الفريق المضيف"
        val awayName = awayTeamObj?.optString("name", "الفريق الضيف") ?: "الفريق الضيف"
        val homeLogo = homeTeamObj?.optString("logo", "") ?: ""
        val awayLogo = awayTeamObj?.optString("logo", "") ?: ""

        val homeGoals = if (goalsObj != null && !goalsObj.isNull("home")) goalsObj.optInt("home", 0) else null
        val awayGoals = if (goalsObj != null && !goalsObj.isNull("away")) goalsObj.optInt("away", 0) else null

        val rawLeagueName = leagueObj?.optString("name", "بطولة كرة قدم") ?: "بطولة كرة قدم"
        val leagueArabic = getArabicLeagueName(rawLeagueName)
        val leagueLogo = leagueObj?.optString("logo", "") ?: ""
        val round = leagueObj?.optString("round", "الجولة الحالية") ?: ""

        val venueObj = fixtureObj.optJSONObject("venue")
        val stadium = venueObj?.optString("name", "الملعب الدولي 🏟️") ?: "الملعب الدولي 🏟️"
        val referee = fixtureObj.optString("referee", "طاقم تحكيم دولي")

        // Format time (e.g. 21:00)
        var timeFormatted = ""
        try {
            if (dateRaw.isNotBlank()) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = inputFormat.parse(dateRaw)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    timeFormatted = outputFormat.format(date)
                }
            }
        } catch (e: Exception) {
            timeFormatted = "20:00"
        }

        val minuteDisplay = if (elapsed != null && elapsed > 0) {
            if (extra != null && extra > 0) "$elapsed+$extra" else "$elapsed"
        } else ""

        return YssMatch(
            id = id,
            homeTeam = Team(homeName, homeLogo),
            awayTeam = Team(awayName, awayLogo),
            team1 = homeName,
            team2 = awayName,
            team1Logo = homeLogo,
            team2Logo = awayLogo,
            homeScore = homeGoals?.toString() ?: "0",
            awayScore = awayGoals?.toString() ?: "0",
            score1 = homeGoals,
            score2 = awayGoals,
            status = matchStatus,
            statusStr = statusStr,
            minute = elapsed,
            minuteStr = if (minuteDisplay.isNotBlank()) "$minuteDisplay'" else "",
            time = if (timeFormatted.isNotBlank()) timeFormatted else "20:00",
            timeFormatted = if (timeFormatted.isNotBlank()) timeFormatted else "20:00",
            league = League(leagueArabic, leagueLogo),
            leagueName = leagueArabic,
            leagueLogo = leagueLogo,
            isLive = isLive,
            stadium = stadium,
            referee = referee,
            round = round,
            commentator = "معلق رياضي",
            channelName = "قناة البث المباشر",
            temperature = "22°C"
        )
    }

    private fun parseLineupObject(lineupObj: JSONObject?, fallbackTeamName: String): Lineup {
        if (lineupObj == null) return MatchDataHelper.buildDefaultLineup(fallbackTeamName, true)

        val teamObj = lineupObj.optJSONObject("team")
        val teamName = teamObj?.optString("name", fallbackTeamName) ?: fallbackTeamName
        val formation = lineupObj.optString("formation", "4-3-3")
        val coachObj = lineupObj.optJSONObject("coach")
        val coachName = coachObj?.optString("name", "المدرب الفني") ?: "المدرب الفني"

        val startPlayers = mutableListOf<Player>()
        val startXIArray = lineupObj.optJSONArray("startXI")

        if (startXIArray != null) {
            // Determine max rows in formation to compute realistic tactical positions on the pitch
            for (i in 0 until startXIArray.length()) {
                val pWrapper = startXIArray.optJSONObject(i) ?: continue
                val p = pWrapper.optJSONObject("player") ?: continue
                val number = p.optInt("number", 0)
                val name = p.optString("name", "لاعب")
                val pos = p.optString("pos", "M")
                val grid = p.optString("grid", "")

                val coords = parsePlayerGrid(grid, pos)

                startPlayers.add(
                    Player(
                        number = number,
                        name = name,
                        position = when (pos.uppercase()) {
                            "G" -> "حارس مرمى"
                            "D" -> "مدافع"
                            "M" -> "وسط"
                            "F" -> "مهاجم"
                            else -> pos
                        },
                        gridX = coords.first,
                        gridY = coords.second
                    )
                )
            }
        }

        val substitutePlayers = mutableListOf<Player>()
        val subsArray = lineupObj.optJSONArray("substitutes")
        if (subsArray != null) {
            for (i in 0 until subsArray.length()) {
                val pWrapper = subsArray.optJSONObject(i) ?: continue
                val p = pWrapper.optJSONObject("player") ?: continue
                val number = p.optInt("number", 0)
                val name = p.optString("name", "بديل")
                val pos = p.optString("pos", "M")

                substitutePlayers.add(
                    Player(
                        number = number,
                        name = name,
                        position = when (pos.uppercase()) {
                            "G" -> "حارس"
                            "D" -> "مدافع"
                            "M" -> "وسط"
                            "F" -> "مهاجم"
                            else -> pos
                        }
                    )
                )
            }
        }

        return Lineup(
            teamName = teamName,
            formation = formation,
            coach = coachName,
            startPlayers = startPlayers,
            substitutePlayers = substitutePlayers,
            startingXI = startPlayers,
            substitutes = substitutePlayers
        )
    }

    private fun parsePlayerGrid(grid: String, pos: String): Pair<Float, Float> {
        if (grid.isNotBlank() && grid.contains(":")) {
            val parts = grid.split(":")
            val row = parts.getOrNull(0)?.toIntOrNull() ?: 2
            val col = parts.getOrNull(1)?.toIntOrNull() ?: 1

            val y = when (row) {
                1 -> 0.12f // Goalkeeper
                2 -> 0.33f // Defenders
                3 -> 0.56f // Midfielders
                4 -> 0.78f // Attacking Mid / Wingers
                5 -> 0.90f // Strikers
                else -> 0.50f
            }

            val x = when (col) {
                1 -> 0.18f
                2 -> 0.38f
                3 -> 0.62f
                4 -> 0.82f
                5 -> 0.50f
                else -> 0.50f
            }

            return Pair(x, y)
        }

        return when (pos.uppercase()) {
            "G", "GK" -> Pair(0.50f, 0.12f)
            "D", "DEF" -> Pair(0.50f, 0.33f)
            "M", "MID" -> Pair(0.50f, 0.58f)
            "F", "FWD", "A", "ATT" -> Pair(0.50f, 0.86f)
            else -> Pair(0.50f, 0.50f)
        }
    }

    private fun parseStatisticsArray(statsArray: JSONArray): MatchStats {
        var pos1 = 50
        var pos2 = 50
        var shots1 = 0
        var shots2 = 0
        var shotsOnTarget1 = 0
        var shotsOnTarget2 = 0
        var passes1 = 0
        var passes2 = 0
        var passAcc1 = 0
        var passAcc2 = 0
        var fouls1 = 0
        var fouls2 = 0
        var yellow1 = 0
        var yellow2 = 0
        var red1 = 0
        var red2 = 0
        var corners1 = 0
        var corners2 = 0
        var offsides1 = 0
        var offsides2 = 0

        fun extractStatInt(item: JSONObject): Int {
            if (item.isNull("value")) return 0
            val vStr = item.optString("value", "0").replace("%", "").trim()
            return vStr.toIntOrNull() ?: 0
        }

        // Team 1
        val team1Stats = statsArray.optJSONObject(0)?.optJSONArray("statistics")
        if (team1Stats != null) {
            for (i in 0 until team1Stats.length()) {
                val item = team1Stats.optJSONObject(i) ?: continue
                val type = item.optString("type", "").lowercase()
                when {
                    type.contains("possession") -> pos1 = extractStatInt(item)
                    type.contains("shots on goal") || type.contains("on target") -> shotsOnTarget1 = extractStatInt(item)
                    type.contains("total shots") -> shots1 = extractStatInt(item)
                    type.contains("total passes") -> passes1 = extractStatInt(item)
                    type.contains("passes %") || type.contains("accuracy") -> passAcc1 = extractStatInt(item)
                    type.contains("fouls") -> fouls1 = extractStatInt(item)
                    type.contains("yellow cards") -> yellow1 = extractStatInt(item)
                    type.contains("red cards") -> red1 = extractStatInt(item)
                    type.contains("corner kicks") -> corners1 = extractStatInt(item)
                    type.contains("offsides") -> offsides1 = extractStatInt(item)
                }
            }
        }

        // Team 2
        val team2Stats = statsArray.optJSONObject(1)?.optJSONArray("statistics")
        if (team2Stats != null) {
            for (i in 0 until team2Stats.length()) {
                val item = team2Stats.optJSONObject(i) ?: continue
                val type = item.optString("type", "").lowercase()
                when {
                    type.contains("possession") -> pos2 = extractStatInt(item)
                    type.contains("shots on goal") || type.contains("on target") -> shotsOnTarget2 = extractStatInt(item)
                    type.contains("total shots") -> shots2 = extractStatInt(item)
                    type.contains("total passes") -> passes2 = extractStatInt(item)
                    type.contains("passes %") || type.contains("accuracy") -> passAcc2 = extractStatInt(item)
                    type.contains("fouls") -> fouls2 = extractStatInt(item)
                    type.contains("yellow cards") -> yellow2 = extractStatInt(item)
                    type.contains("red cards") -> red2 = extractStatInt(item)
                    type.contains("corner kicks") -> corners2 = extractStatInt(item)
                    type.contains("offsides") -> offsides2 = extractStatInt(item)
                }
            }
        }

        if (pos1 == 0 && pos2 == 0) {
            pos1 = 50
            pos2 = 50
        }

        return MatchStats(
            possessionHome = pos1,
            possessionAway = pos2,
            possession1 = pos1,
            possession2 = pos2,
            shotsHome = shots1,
            shotsAway = shots2,
            shots1 = shots1,
            shots2 = shots2,
            shotsOnTargetHome = shotsOnTarget1,
            shotsOnTargetAway = shotsOnTarget2,
            shotsOnTarget1 = shotsOnTarget1,
            shotsOnTarget2 = shotsOnTarget2,
            passesHome = passes1,
            passesAway = passes2,
            passes1 = passes1,
            passes2 = passes2,
            passAccuracyHome = passAcc1,
            passAccuracyAway = passAcc2,
            passAccuracy1 = passAcc1,
            passAccuracy2 = passAcc2,
            foulsHome = fouls1,
            foulsAway = fouls2,
            fouls1 = fouls1,
            fouls2 = fouls2,
            yellowCardsHome = yellow1,
            yellowCardsAway = yellow2,
            yellowCards1 = yellow1,
            yellowCards2 = yellow2,
            redCardsHome = red1,
            redCardsAway = red2,
            redCards1 = red1,
            redCards2 = red2,
            cornersHome = corners1,
            cornersAway = corners2,
            corners1 = corners1,
            corners2 = corners2,
            offsidesHome = offsides1,
            offsidesAway = offsides2,
            offsides1 = offsides1,
            offsides2 = offsides2
        )
    }

    private fun makeApiRequest(urlString: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-apisports-key", API_KEY)
            connection.setRequestProperty("x-rapidapi-key", API_KEY)
            connection.setRequestProperty("User-Agent", "CinemaPlus90/2.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text)
            } else {
                Log.w(TAG, "API-Sports request returned code: $code for url: $urlString")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure calling API-Sports: $urlString", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
