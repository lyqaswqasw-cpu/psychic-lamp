package com.example.data

import android.os.Build
import com.example.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// -------------------------------------------------------------
// Models for ESPN Soccer Data Engine
// -------------------------------------------------------------

data class EspnLeague(
    val id: String,
    val name: String,
    val arabicName: String,
    val logoUrl: String,
    val slug: String
)

data class EspnTeam(
    val id: String,
    val displayName: String,
    val arabicName: String,
    val shortName: String,
    val abbreviation: String,
    val logoUrl: String,
    val color: String,
    val alternateColor: String,
    val score: String = "0",
    val isHome: Boolean = true,
    val isWinner: Boolean = false
)

data class EspnMatch(
    val id: String,
    val leagueId: String,
    val leagueName: String,
    val leagueArabicName: String,
    val leagueLogoUrl: String,
    val name: String,
    val dateIso: String,
    val timestampMs: Long,
    val formattedTime: String, // e.g. "10:00 مساءً"
    val formattedDate: String, // e.g. "4 مايو 2024"
    val statusState: String, // "pre", "in", "post"
    val statusDisplay: String, // "مباشر", "انتهت", "لم تبدأ", "58:23"
    val clock: String, // e.g. "58'"
    val homeTeam: EspnTeam,
    val awayTeam: EspnTeam,
    val venueName: String,
    val roundName: String,
    val matchNote: String = ""
)

data class EspnRosterPlayer(
    val id: String,
    val name: String,
    val jersey: String,
    val position: String,
    val positionCategory: String, // "G" (Goalkeeper), "D" (Defender), "M" (Midfielder), "F" (Forward)
    val headshotUrl: String,
    val isStarter: Boolean,
    val subbedOut: Boolean = false
)

data class EspnTeamRoster(
    val teamId: String,
    val teamName: String,
    val starters: List<EspnRosterPlayer>,
    val bench: List<EspnRosterPlayer>
)

data class EspnStatItem(
    val name: String,
    val labelArabic: String,
    val homeValue: String,
    val awayValue: String,
    val homeNumeric: Float,
    val awayNumeric: Float
)

data class EspnKeyEvent(
    val id: String,
    val clock: String, // e.g. "58'"
    val minute: Int,
    val type: String, // "goal", "sub", "card-yellow", "card-red", "var", "kickoff", "half", "full"
    val titleArabic: String,
    val description: String,
    val teamId: String,
    val teamLogo: String,
    val playerName: String,
    val assistPlayerName: String = "",
    val subOutPlayerName: String = "",
    val scoreText: String = ""
)

data class EspnGameInfo(
    val venue: String,
    val attendance: String,
    val referees: String,
    val weather: String,
    val dateFormatted: String,
    val timeFormatted: String
)

data class EspnMatchSummary(
    val matchId: String,
    val leagueId: String,
    val homeRoster: EspnTeamRoster,
    val awayRoster: EspnTeamRoster,
    val statistics: List<EspnStatItem>,
    val keyEvents: List<EspnKeyEvent>,
    val gameInfo: EspnGameInfo
)

data class EspnStandingEntry(
    val teamId: String,
    val teamName: String,
    val teamArabicName: String,
    val teamLogoUrl: String,
    val rank: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: String,
    val points: Int
)

data class YsNewsItem(
    val id: Int,
    val title: String,
    val description: String,
    val imageUrl: String,
    val timeAgo: String
)

// -------------------------------------------------------------
// Service Implementation
// -------------------------------------------------------------

object EspnSoccerService {

    private const val BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer"

    val DEFAULT_LEAGUES = listOf(
        EspnLeague("esp.1", "Spanish LALIGA", "الدوري الإسباني", "https://a.espncdn.com/i/leaguelogos/soccer/500/15.png", "esp.1"),
        EspnLeague("eng.1", "English Premier League", "الدوري الإنجليزي", "https://a.espncdn.com/i/leaguelogos/soccer/500/23.png", "eng.1"),
        EspnLeague("uefa.champions", "UEFA Champions League", "دوري أبطال أوروبا", "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png", "uefa.champions"),
        EspnLeague("ita.1", "Italian Serie A", "الدوري الإيطالي", "https://a.espncdn.com/i/leaguelogos/soccer/500/12.png", "ita.1"),
        EspnLeague("ger.1", "German Bundesliga", "الدوري الألماني", "https://a.espncdn.com/i/leaguelogos/soccer/500/10.png", "ger.1"),
        EspnLeague("fra.1", "French Ligue 1", "الدوري الفرنسي", "https://a.espncdn.com/i/leaguelogos/soccer/500/9.png", "fra.1"),
        EspnLeague("ksa.1", "Saudi Pro League", "دوري روشن السعودي", "https://a.espncdn.com/i/leaguelogos/soccer/500/2301.png", "ksa.1"),
        EspnLeague("afc.champions", "AFC Champions League", "دوري أبطال آسيا", "https://a.espncdn.com/i/leaguelogos/soccer/500/2436.png", "afc.champions")
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    // Popular Team Arabic translations map
    private val teamArabicNames = mapOf(
        "Real Madrid" to "ريال مدريد",
        "Barcelona" to "برشلونة",
        "Atlético Madrid" to "أتلتيكو مدريد",
        "Atletico Madrid" to "أتلتيكو مدريد",
        "Sevilla" to "إشبيلية",
        "Valencia" to "فالنسيا",
        "Real Sociedad" to "ريال سوسيداد",
        "Real Betis" to "ريال بيتيس",
        "Villarreal" to "فياريال",
        "Athletic Club" to "أتلتيك بيلباو",
        "Girona" to "جيرونا",
        "Celta Vigo" to "سيلتا فيغو",
        "Mallorca" to "مايوركا",
        "Osasuna" to "أوساسونا",
        "Getafe" to "خيتافي",
        "Rayo Vallecano" to "رايو فاليكانو",
        "Alavés" to "ديبورتيفو ألافيس",
        "Las Palmas" to "لاس بالماس",
        "Espanyol" to "إسبانيول",
        "Leganés" to "ليغانيس",
        "Valladolid" to "بلد الوليد",

        "Manchester City" to "مانشستر سيتي",
        "Manchester United" to "مانشستر يونايتد",
        "Liverpool" to "ليفربول",
        "Arsenal" to "أرسنال",
        "Chelsea" to "تشيلسي",
        "Tottenham Hotspur" to "توتنهام هوتسبير",
        "Aston Villa" to "أستون فيلا",
        "Newcastle United" to "نيوكاسل يونايتد",
        "Brighton & Hove Albion" to "برايتون",
        "West Ham United" to "وست هام يونايتد",
        "Wolverhampton Wanderers" to "وولفرهامبتون",
        "Fulham" to "فولهام",
        "Bournemouth" to "بورنموث",
        "Everton" to "إيفرتون",
        "Brentford" to "برينتفورد",
        "Crystal Palace" to "كريستال بالاس",
        "Nottingham Forest" to "نوتينغهام فورست",
        "Leicester City" to "ليستر سيتي",
        "Ipswich Town" to "إيبسويتش تاون",
        "Southampton" to "ساوثهامبتون",

        "Inter Milan" to "إنتر ميلان",
        "AC Milan" to "ميلان",
        "Juventus" to "يوفنتوس",
        "Napoli" to "نابولي",
        "Roma" to "روما",
        "Lazio" to "لاتسيو",
        "Atalanta" to "أتالانتا",
        "Fiorentina" to "فيورنتينا",

        "Bayern Munich" to "بايرن ميونخ",
        "Borussia Dortmund" to "بوروسيا دورتموند",
        "Bayer Leverkusen" to "باير ليفركوزن",
        "RB Leipzig" to "لايبزيغ",

        "Paris Saint-Germain" to "باريس سان جيرمان",
        "Marseille" to "مارسيليا",
        "Monaco" to "موناكو",
        "Lyon" to "ليون",
        "Lille" to "ليل",

        "Al Hilal" to "الهلال",
        "Al Nassr" to "النصر",
        "Al Ittihad" to "الاتحاد",
        "Al Ahli" to "الأهلي",
        "Al Shabab" to "الشباب",
        "Al Ettifaq" to "الاتفاق",
        "Al Taawoun" to "التعاون",

        "Al Ahly" to "الأهلي المصري",
        "Zamalek" to "الزمالك",
        "Pyramids" to "بيراميدز",
        "Esperance" to "الترجي",
        "Wydad" to "الوداد الرياضي",
        "Raja" to "الرجاء البيضاوي"
    )

    fun getArabicTeamName(englishName: String): String {
        return teamArabicNames[englishName.trim()] ?: englishName
    }

    val RIYADH_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Riyadh")

    fun getLocalMatchTime(utcString: String): String {
        if (utcString.isBlank()) return "--:--"
        val date = parseEspnDate(utcString) ?: return "--:--"
        return formatArabicMatchTime(date)
    }

    fun formatArabicMatchTime(date: Date): String {
        val cal = Calendar.getInstance(RIYADH_TIME_ZONE)
        cal.time = date
        val rawHour = cal.get(Calendar.HOUR)
        val hourInt: Int = if (rawHour == 0) 12 else rawHour
        val minuteInt: Int = cal.get(Calendar.MINUTE)
        val amPm: Int = cal.get(Calendar.AM_PM)
        val period = if (amPm == Calendar.AM) "ص" else "م"
        return String.format(Locale.US, "%d:%02d%s", hourInt, minuteInt, period)
    }

    fun formatArabicMatchTime(timestampMs: Long): String {
        if (timestampMs <= 0) return "--:--"
        return formatArabicMatchTime(Date(timestampMs))
    }

    fun formatArabicMatchDate(date: Date): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("ar")).apply {
            timeZone = RIYADH_TIME_ZONE
        }
        return dateFormat.format(date)
    }

    private val isoFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    )

    fun parseEspnDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val instant = java.time.Instant.parse(dateStr)
                return Date.from(instant)
            } catch (_: Exception) {}
            try {
                val odt = java.time.OffsetDateTime.parse(dateStr)
                return Date.from(odt.toInstant())
            } catch (_: Exception) {}
            try {
                val zdt = java.time.ZonedDateTime.parse(dateStr)
                return Date.from(zdt.toInstant())
            } catch (_: Exception) {}
        }
        for (format in isoFormats) {
            try {
                val parsed = format.parse(dateStr)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    private fun getArabicLeagueName(slug: String, rawName: String): String {
        return DEFAULT_LEAGUES.find { it.slug.equals(slug, ignoreCase = true) || it.id.equals(slug, ignoreCase = true) }?.arabicName ?: rawName
    }

    /**
     * Fetch scoreboard for single league on a specific date (YYYYMMDD)
     */
    suspend fun fetchScoreboard(leagueId: String, dateYYYYMMDD: String): List<EspnMatch> = withContext(Dispatchers.IO) {
        val url = if (dateYYYYMMDD.isNotBlank()) {
            "$BASE_URL/$leagueId/scoreboard?dates=$dateYYYYMMDD"
        } else {
            "$BASE_URL/$leagueId/scoreboard"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val jsonBody = response.body?.string() ?: return@withContext emptyList()
            parseScoreboardJson(jsonBody, leagueId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Fetch all scoreboards concurrently using coroutineScope and async
     */
    suspend fun fetchAllScoreboards(leagueIds: List<String>, dateYYYYMMDD: String): List<EspnMatch> = coroutineScope {
        leagueIds.map { leagueId ->
            async {
                fetchScoreboard(leagueId, dateYYYYMMDD)
            }
        }.awaitAll().flatten()
    }

    private fun parseScoreboardJson(jsonStr: String, defaultLeagueId: String): List<EspnMatch> {
        val matches = mutableListOf<EspnMatch>()
        val root = JSONObject(jsonStr)

        val leaguesArray = root.optJSONArray("leagues")
        val leagueObj = leaguesArray?.optJSONObject(0)
        val leagueName = leagueObj?.optString("name", "Soccer League") ?: "Soccer League"
        val leagueSlug = leagueObj?.optString("slug", defaultLeagueId) ?: defaultLeagueId
        val leagueLogo = leagueObj?.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: ""
        val leagueArabic = getArabicLeagueName(leagueSlug, leagueName)

        val eventsArray = root.optJSONArray("events") ?: return emptyList()

        for (i in 0 until eventsArray.length()) {
            val event = eventsArray.optJSONObject(i) ?: continue
            val eventId = event.optString("id", "")
            val eventName = event.optString("name", "")

            val competitions = event.optJSONArray("competitions")
            val competition = competitions?.optJSONObject(0) ?: continue

            val eventDateStr = event.optString("date", "")
            val compDateStr = competition.optString("date", "")
            val dateStr = if (eventDateStr.isNotBlank()) eventDateStr else compDateStr

            var timestampMs = 0L
            var formattedTime = getLocalMatchTime(dateStr)
            var formattedDate = ""
            try {
                if (dateStr.isNotBlank()) {
                    val parsed = parseEspnDate(dateStr)
                    if (parsed != null) {
                        timestampMs = parsed.time
                        formattedDate = formatArabicMatchDate(parsed)
                    }
                }
            } catch (_: Exception) {}

            val statusObj = event.optJSONObject("status")
            val statusType = statusObj?.optJSONObject("type")
            val state = statusType?.optString("state", "pre") ?: "pre" // "pre", "in", "post"
            val rawDescription = statusType?.optString("description", "") ?: ""
            val displayClock = statusObj?.optString("displayClock", "") ?: ""

            val statusDisplay = when (state) {
                "in" -> if (displayClock.isNotBlank()) displayClock else "مباشر"
                "post" -> "انتهت"
                else -> "لم تبدأ"
            }

            val venueObj = competition.optJSONObject("venue")
            val venueName = venueObj?.optString("displayName", "الملعب") ?: "الملعب"
            val roundObj = competition.optJSONObject("notes")
            val roundNote = roundObj?.optString("headline", "") ?: ""

            val competitors = competition.optJSONArray("competitors") ?: JSONArray()
            var homeTeam: EspnTeam? = null
            var awayTeam: EspnTeam? = null

            for (c in 0 until competitors.length()) {
                val comp = competitors.optJSONObject(c) ?: continue
                val homeAway = comp.optString("homeAway", "home")
                val isHome = homeAway == "home"
                val score = comp.optString("score", "0")
                val winner = comp.optBoolean("winner", false)

                val teamObj = comp.optJSONObject("team") ?: JSONObject()
                val teamId = teamObj.optString("id", "")
                val displayName = teamObj.optString("displayName", "فريق")
                val shortName = teamObj.optString("shortDisplayName", displayName)
                val abbrev = teamObj.optString("abbreviation", "")
                val logo = teamObj.optString("logo", "")
                    .ifEmpty { teamObj.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: "" }
                val color = teamObj.optString("color", "000000")
                val altColor = teamObj.optString("alternateColor", "FFFFFF")

                val parsedTeam = EspnTeam(
                    id = teamId,
                    displayName = displayName,
                    arabicName = getArabicTeamName(displayName),
                    shortName = shortName,
                    abbreviation = abbrev,
                    logoUrl = logo,
                    color = color,
                    alternateColor = altColor,
                    score = score,
                    isHome = isHome,
                    isWinner = winner
                )

                if (isHome) {
                    homeTeam = parsedTeam
                } else {
                    awayTeam = parsedTeam
                }
            }

            if (homeTeam != null && awayTeam != null) {
                matches.add(
                    EspnMatch(
                        id = eventId,
                        leagueId = leagueSlug,
                        leagueName = leagueName,
                        leagueArabicName = leagueArabic,
                        leagueLogoUrl = leagueLogo,
                        name = eventName,
                        dateIso = dateStr,
                        timestampMs = timestampMs,
                        formattedTime = formattedTime,
                        formattedDate = formattedDate,
                        statusState = state,
                        statusDisplay = statusDisplay,
                        clock = displayClock,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        venueName = venueName,
                        roundName = roundNote,
                        matchNote = ""
                    )
                )
            }
        }

        return matches
    }

    /**
     * Fetch match summary details (Lineups, Statistics, Timeline events, Venue)
     */
    suspend fun fetchMatchSummary(leagueId: String, eventId: String): EspnMatchSummary? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$leagueId/summary?event=$eventId"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val jsonBody = response.body?.string() ?: return@withContext null
            parseSummaryJson(jsonBody, leagueId, eventId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSummaryJson(jsonStr: String, leagueId: String, eventId: String): EspnMatchSummary? {
        val root = JSONObject(jsonStr)

        // 1. Parse Rosters
        val rostersArray = root.optJSONArray("rosters")
        var homeRoster = EspnTeamRoster("", "", emptyList(), emptyList())
        var awayRoster = EspnTeamRoster("", "", emptyList(), emptyList())

        if (rostersArray != null) {
            for (r in 0 until rostersArray.length()) {
                val rObj = rostersArray.optJSONObject(r) ?: continue
                val isHome = rObj.optString("homeAway", "home") == "home"
                val teamObj = rObj.optJSONObject("team") ?: JSONObject()
                val teamId = teamObj.optString("id", "")
                val teamName = teamObj.optString("displayName", "")
                val rosterList = rObj.optJSONArray("roster") ?: JSONArray()

                val starters = mutableListOf<EspnRosterPlayer>()
                val bench = mutableListOf<EspnRosterPlayer>()

                for (p in 0 until rosterList.length()) {
                    val pObj = rosterList.optJSONObject(p) ?: continue
                    val isStarter = pObj.optBoolean("starter", false)
                    val subbedOut = pObj.optBoolean("subbedOut", false)
                    val jersey = pObj.optString("jersey", "")
                    val athleteObj = pObj.optJSONObject("athlete") ?: JSONObject()
                    val pId = athleteObj.optString("id", "")
                    val pName = athleteObj.optString("displayName", "لاعب")
                    val headshot = athleteObj.optJSONObject("headshot")?.optString("href", "") ?: ""

                    val posObj = pObj.optJSONObject("position")
                    val posAbbrev = posObj?.optString("abbreviation", "M") ?: "M"
                    val posName = posObj?.optString("displayName", "لاعب") ?: "لاعب"

                    val category = when {
                        posAbbrev.contains("G", ignoreCase = true) -> "G"
                        posAbbrev.contains("D", ignoreCase = true) || posAbbrev.contains("B", ignoreCase = true) -> "D"
                        posAbbrev.contains("M", ignoreCase = true) -> "M"
                        else -> "F"
                    }

                    val player = EspnRosterPlayer(
                        id = pId,
                        name = pName,
                        jersey = jersey,
                        position = posName,
                        positionCategory = category,
                        headshotUrl = headshot,
                        isStarter = isStarter,
                        subbedOut = subbedOut
                    )

                    if (isStarter) {
                        starters.add(player)
                    } else {
                        bench.add(player)
                    }
                }

                // Sort Starters by position: Goalkeeper first, Defenders, Midfielders, Forwards
                val sortedStarters = starters.sortedBy { player ->
                    when (player.positionCategory) {
                        "G" -> 1
                        "D" -> 2
                        "M" -> 3
                        "F" -> 4
                        else -> 5
                    }
                }

                val teamRoster = EspnTeamRoster(
                    teamId = teamId,
                    teamName = getArabicTeamName(teamName),
                    starters = sortedStarters,
                    bench = bench
                )

                if (isHome) {
                    homeRoster = teamRoster
                } else {
                    awayRoster = teamRoster
                }
            }
        }

        // 2. Parse Statistics
        val statsList = mutableListOf<EspnStatItem>()
        val boxscoreObj = root.optJSONObject("boxscore")
        val teamsArray = boxscoreObj?.optJSONArray("teams")

        if (teamsArray != null && teamsArray.length() >= 2) {
            val homeTeamBox = teamsArray.optJSONObject(0)
            val awayTeamBox = teamsArray.optJSONObject(1)

            val homeStatsArray = homeTeamBox?.optJSONArray("statistics") ?: JSONArray()
            val awayStatsArray = awayTeamBox?.optJSONArray("statistics") ?: JSONArray()

            val statsMapHome = mutableMapOf<String, String>()
            val statsMapAway = mutableMapOf<String, String>()

            for (s in 0 until homeStatsArray.length()) {
                val sObj = homeStatsArray.optJSONObject(s) ?: continue
                statsMapHome[sObj.optString("name", "")] = sObj.optString("displayValue", "0")
            }

            for (s in 0 until awayStatsArray.length()) {
                val sObj = awayStatsArray.optJSONObject(s) ?: continue
                statsMapAway[sObj.optString("name", "")] = sObj.optString("displayValue", "0")
            }

            val statDefinitions = listOf(
                Triple("possessionPct", "الاستحواذ %", true),
                Triple("shotsOnTarget", "التسديدات على المرمى", false),
                Triple("totalShots", "إجمالي التسديدات", false),
                Triple("foulsCommitted", "الأخطاء المرتكبة", false),
                Triple("yellowCards", "البطاقات الصفراء", false),
                Triple("redCards", "البطاقات الحمراء", false),
                Triple("offsides", "التسلل", false),
                Triple("wonCorners", "الضربات الركنية", false),
                Triple("saves", "التصديات", false),
                Triple("accuratePasses", "التمريرات الناجحة", false)
            )

            for ((key, label, isPct) in statDefinitions) {
                val hVal = statsMapHome[key] ?: if (isPct) "50%" else "0"
                val aVal = statsMapAway[key] ?: if (isPct) "50%" else "0"

                val hNum = hVal.replace("%", "").toFloatOrNull() ?: 0f
                val aNum = aVal.replace("%", "").toFloatOrNull() ?: 0f

                statsList.add(
                    EspnStatItem(
                        name = key,
                        labelArabic = label,
                        homeValue = hVal,
                        awayValue = aVal,
                        homeNumeric = hNum,
                        awayNumeric = aNum
                    )
                )
            }
        }

        // 3. Parse Key Events (Goals, Subs, Cards)
        val keyEventsList = mutableListOf<EspnKeyEvent>()
        val keyEventsArray = root.optJSONArray("keyEvents")

        if (keyEventsArray != null) {
            for (k in 0 until keyEventsArray.length()) {
                val kObj = keyEventsArray.optJSONObject(k) ?: continue
                val id = kObj.optString("id", "")
                val clockObj = kObj.optJSONObject("clock")
                val clock = clockObj?.optString("displayValue", "") ?: ""
                val text = kObj.optString("text", "")
                val typeObj = kObj.optJSONObject("type")
                val typeText = typeObj?.optString("text", "") ?: ""

                val teamObj = kObj.optJSONObject("team")
                val teamId = teamObj?.optString("id", "") ?: ""
                val teamLogo = teamObj?.optString("logo", "") ?: ""

                val participants = kObj.optJSONArray("participants")
                var primaryPlayer = ""
                var assistPlayer = ""
                var subOutPlayer = ""

                if (participants != null) {
                    val p0 = participants.optJSONObject(0)?.optJSONObject("athlete")
                    primaryPlayer = p0?.optString("displayName", "") ?: ""
                    val p1 = participants.optJSONObject(1)?.optJSONObject("athlete")
                    assistPlayer = p1?.optString("displayName", "") ?: ""
                }

                val eventType = when {
                    typeText.contains("Goal", ignoreCase = true) -> "goal"
                    typeText.contains("Substitution", ignoreCase = true) || text.contains("Substitution", ignoreCase = true) -> "sub"
                    typeText.contains("Yellow", ignoreCase = true) -> "card-yellow"
                    typeText.contains("Red", ignoreCase = true) -> "card-red"
                    typeText.contains("Kickoff", ignoreCase = true) -> "kickoff"
                    typeText.contains("Halftime", ignoreCase = true) -> "half"
                    typeText.contains("Full Time", ignoreCase = true) -> "full"
                    else -> "other"
                }

                val titleArabic = when (eventType) {
                    "goal" -> "هدف ⚽"
                    "sub" -> "تبديل 🔄"
                    "card-yellow" -> "بطاقة صفراء 🟨"
                    "card-red" -> "بطاقة حمراء 🟥"
                    "kickoff" -> "بداية المباراة"
                    "half" -> "نهاية الشوط الأول"
                    "full" -> "نهاية المباراة"
                    else -> typeText
                }

                val minute = clock.replace("'", "").replace("+", "").toIntOrNull() ?: 0

                keyEventsList.add(
                    EspnKeyEvent(
                        id = id,
                        clock = clock,
                        minute = minute,
                        type = eventType,
                        titleArabic = titleArabic,
                        description = text,
                        teamId = teamId,
                        teamLogo = teamLogo,
                        playerName = primaryPlayer,
                        assistPlayerName = assistPlayer,
                        subOutPlayerName = subOutPlayer,
                        scoreText = ""
                    )
                )
            }
        }

        // 4. Parse Game Info
        val gameInfoObj = root.optJSONObject("gameInfo")
        val venueObj = gameInfoObj?.optJSONObject("venue")
        val venueName = venueObj?.optString("fullName", "")
            ?.ifEmpty { venueObj.optString("address", "") } ?: "الملعب"
        val attendance = gameInfoObj?.optString("attendance", "") ?: ""
        val weatherObj = gameInfoObj?.optJSONObject("weather")
        val temp = weatherObj?.optString("temperature", "") ?: ""
        val weatherDisplay = if (temp.isNotBlank()) "$temp°C" else "معتدل"

        val gameInfo = EspnGameInfo(
            venue = venueName,
            attendance = attendance,
            referees = "طاقم التحكيم الرسمي",
            weather = weatherDisplay,
            dateFormatted = "",
            timeFormatted = ""
        )

        return EspnMatchSummary(
            matchId = eventId,
            leagueId = leagueId,
            homeRoster = homeRoster,
            awayRoster = awayRoster,
            statistics = statsList,
            keyEvents = keyEventsList,
            gameInfo = gameInfo
        )
    }

    suspend fun fetchLeagueStandings(leagueId: String): List<EspnStandingEntry> = withContext(Dispatchers.IO) {
        val url = "https://site.api.espn.com/apis/v2/sports/soccer/$leagueId/standings"
        val request = Request.Builder().url(url).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(bodyStr)
                val children = root.optJSONArray("children") ?: return@withContext emptyList()
                if (children.length() == 0) return@withContext emptyList()
                val child = children.getJSONObject(0)
                val standings = child.optJSONObject("standings") ?: return@withContext emptyList()
                val entries = standings.optJSONArray("entries") ?: return@withContext emptyList()
                
                val list = mutableListOf<EspnStandingEntry>()
                for (i in 0 until entries.length()) {
                    val entry = entries.getJSONObject(i)
                    val teamObj = entry.optJSONObject("team") ?: continue
                    val teamId = teamObj.optString("id", "")
                    val displayName = teamObj.optString("displayName", "")
                    val arabicName = getArabicTeamName(displayName)
                    val logoUrl = teamObj.optJSONArray("logos")?.optJSONObject(0)?.optString("href", "") ?: ""
                    
                    val stats = entry.optJSONArray("stats")
                    var gamesPlayed = 0
                    var wins = 0
                    var draws = 0
                    var losses = 0
                    var goalsFor = 0
                    var goalsAgainst = 0
                    var goalDifference = "0"
                    var points = 0
                    var rank = i + 1
                    
                    if (stats != null) {
                        for (j in 0 until stats.length()) {
                            val stat = stats.getJSONObject(j)
                            val name = stat.optString("name", "")
                            val value = stat.optDouble("value", 0.0).toInt()
                            val dispVal = stat.optString("displayValue", "")
                            when (name) {
                                "gamesPlayed" -> gamesPlayed = value
                                "wins" -> wins = value
                                "ties" -> draws = value
                                "losses" -> losses = value
                                "pointsFor" -> goalsFor = value
                                "pointsAgainst" -> goalsAgainst = value
                                "pointDifferential" -> goalDifference = dispVal
                                "points" -> points = value
                                "rank" -> rank = value
                            }
                        }
                    }
                    
                    list.add(
                        EspnStandingEntry(
                            teamId = teamId,
                            teamName = displayName,
                            teamArabicName = arabicName,
                            teamLogoUrl = logoUrl,
                            rank = rank,
                            gamesPlayed = gamesPlayed,
                            wins = wins,
                            draws = draws,
                            losses = losses,
                            goalsFor = goalsFor,
                            goalsAgainst = goalsAgainst,
                            goalDifference = goalDifference,
                            points = points
                        )
                    )
                }
                list.sortBy { it.rank }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchYsNews(): List<YsNewsItem> = withContext(Dispatchers.IO) {
        val url = "https://news-ar.ysscores.com/api/News/all_news_page"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(bodyStr)
                val dataObj = root.optJSONObject("data") ?: return@withContext emptyList()
                
                val list = mutableListOf<YsNewsItem>()
                val seenIds = mutableSetOf<Int>()

                fun parseItem(item: JSONObject) {
                    val id = item.optInt("id", 0)
                    if (id == 0 || seenIds.contains(id)) return
                    seenIds.add(id)
                    val title = item.optString("title", "")
                    val description = item.optString("news_desc", "")
                    val imageName = item.optString("image", "")
                    val imageUrl = if (imageName.isNotBlank()) "https://imgs.ysscores.com/news/$imageName" else ""
                    
                    val createdAtObj = item.optJSONObject("created_at")
                    val dateStr = createdAtObj?.optString("date", "") ?: ""
                    val timeAgo = if (dateStr.isNotBlank()) parseArabicTimeAgo(dateStr) else "قبل فترة"

                    list.add(
                        YsNewsItem(
                            id = id,
                            title = title,
                            description = description,
                            imageUrl = imageUrl,
                            timeAgo = timeAgo
                        )
                    )
                }

                // 1. Parse main article if present
                val mainObj = dataObj.optJSONObject("main")
                if (mainObj != null) {
                    parseItem(mainObj)
                }

                // 2. Parse important news
                val importantArr = dataObj.optJSONArray("important")
                if (importantArr != null) {
                    for (i in 0 until importantArr.length()) {
                        parseItem(importantArr.getJSONObject(i))
                    }
                }

                // 3. Parse last news
                val lastNewsArr = dataObj.optJSONArray("last_news")
                if (lastNewsArr != null) {
                    for (i in 0 until lastNewsArr.length()) {
                        parseItem(lastNewsArr.getJSONObject(i))
                    }
                }

                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseArabicTimeAgo(dateStr: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
            val date = sdf.parse(dateStr) ?: return "قبل فترة"
            val now = System.currentTimeMillis()
            val diffMs = now - date.time
            if (diffMs <= 0) return "الآن"
            val diffMins = diffMs / (1000 * 60)
            if (diffMins < 60) {
                return "منذ $diffMins دقيقة"
            }
            val diffHours = diffMins / 60
            if (diffHours < 24) {
                return "منذ $diffHours ساعة"
            }
            val diffDays = diffHours / 24
            return "منذ $diffDays يوم"
        } catch (e: Exception) {
            return "منذ ساعات"
        }
    }
}
