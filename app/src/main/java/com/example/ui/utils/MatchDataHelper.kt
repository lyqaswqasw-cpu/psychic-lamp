package com.example.ui.utils

import com.example.data.EspnSoccerService
import com.example.model.*
import java.util.Date

object MatchDataHelper {

    val TOURNAMENT_LOGOS = mapOf(
        "دوري أبطال أوروبا" to "https://images.fotmob.com/image_resources/logo/leaguelogo/42.png",
        "الدوري الإسباني" to "https://images.fotmob.com/image_resources/logo/leaguelogo/87.png",
        "الدوري الإنجليزي" to "https://images.fotmob.com/image_resources/logo/leaguelogo/47.png",
        "الدوري الإنجليزي الممتاز" to "https://images.fotmob.com/image_resources/logo/leaguelogo/47.png",
        "الدوري الإيطالي" to "https://images.fotmob.com/image_resources/logo/leaguelogo/55.png",
        "الدوري الألماني" to "https://images.fotmob.com/image_resources/logo/leaguelogo/54.png",
        "الدوري الفرنسي" to "https://images.fotmob.com/image_resources/logo/leaguelogo/53.png",
        "دوري روشن السعودي" to "https://images.fotmob.com/image_resources/logo/leaguelogo/9080.png",
        "الدوري السعودي" to "https://images.fotmob.com/image_resources/logo/leaguelogo/9080.png",
        "الدوري المصري" to "https://images.fotmob.com/image_resources/logo/leaguelogo/9044.png",
        "الدوري المصري الممتاز" to "https://images.fotmob.com/image_resources/logo/leaguelogo/9044.png",
        "كأس إسبانيا" to "https://images.fotmob.com/image_resources/logo/leaguelogo/138.png",
        "كأس إنجلترا" to "https://images.fotmob.com/image_resources/logo/leaguelogo/132.png"
    )

    fun getTournamentLogo(name: String): String? {
        val trimmed = name.trim()
        TOURNAMENT_LOGOS[trimmed]?.let { return it }
        for ((k, v) in TOURNAMENT_LOGOS) {
            if (trimmed.contains(k) || k.contains(trimmed)) return v
        }
        return "https://images.fotmob.com/image_resources/logo/leaguelogo/42.png"
    }

    fun mapEventToRichMatch(
        event: EventChannel,
        allChannels: List<Channel>,
        publishedEvents: List<EventChannel>
    ): RichMatch {
        val delimiters = listOf(" ضد ", " VS ", " vs ", " - ")
        var t1 = event.name
        var t2 = ""
        for (del in delimiters) {
            if (event.name.contains(del, ignoreCase = true)) {
                val parts = event.name.split(Regex(del, RegexOption.IGNORE_CASE))
                if (parts.size >= 2) {
                    t1 = parts[0].trim()
                    t2 = parts[1].trim()
                    break
                }
            }
        }

        val (logo1, logo2) = TeamLogoHelper.parseMatchLogos(event.imageUrl, t1, t2)
        val now = System.currentTimeMillis()
        val isLive = event.startTimeMs <= 0 || event.startTimeMs <= now

        val formattedTime = if (event.startTimeMs > 0) {
            EspnSoccerService.formatArabicMatchTime(event.startTimeMs)
        } else {
            "جارية الآن"
        }

        val targetName = if (event.targetChannelId.isNotBlank()) {
            val pub = publishedEvents.find { it.id == event.targetChannelId }
            val iptv = allChannels.find { it.id == event.targetChannelId }
            pub?.name ?: iptv?.name ?: "قناة البث المباشر"
        } else {
            "سيرفر +90 الرئيسي"
        }

        val detectedLeague = detectLeague(t1, t2)

        return RichMatch(
            id = event.id,
            homeTeam = Team(t1, logo1 ?: ""),
            awayTeam = Team(t2.ifBlank { "مباراة مباشرة" }, logo2 ?: ""),
            team1 = t1,
            team2 = t2.ifBlank { "مباراة مباشرة" },
            team1Logo = logo1,
            team2Logo = logo2,
            league = League(detectedLeague, getTournamentLogo(detectedLeague) ?: ""),
            leagueName = detectedLeague,
            leagueLogo = getTournamentLogo(detectedLeague),
            round = "مباراة رسمية",
            timeFormatted = formattedTime,
            status = if (isLive) MatchStatus.LIVE else MatchStatus.UPCOMING,
            minute = null,
            score1 = null,
            score2 = null,
            stadium = detectStadium(t1),
            referee = "طاقم تحكيم دولي",
            commentator = "أبرز المعلقين",
            streamUrl = event.streamUrl,
            servers = event.servers,
            forcedFormat = event.forcedFormat,
            targetChannelId = event.targetChannelId,
            rawEventChannel = event,
            isLive = isLive
        )
    }

    private fun detectLeague(team1: String, team2: String): String {
        val t = "$team1 $team2".lowercase()
        return when {
            t.contains("مدريد") || t.contains("برشلونة") || t.contains("أتلتيكو") || t.contains("اشبيلية") || t.contains("فالنسيا") || t.contains("girona") -> "الدوري الإسباني"
            t.contains("سيتي") || t.contains("ليفربول") || t.contains("أرسنال") || t.contains("ارسنال") || t.contains("يونايتد") || t.contains("تشيلسي") || t.contains("توتنهام") -> "الدوري الإنجليزي الممتاز"
            t.contains("بايرن") || t.contains("دورتموند") || t.contains("ليفركوزن") || t.contains("لايبزيج") -> "الدوري الألماني"
            t.contains("إنتر") || t.contains("انتر") || t.contains("ميلان") || t.contains("يوفنتوس") || t.contains("نابولي") || t.contains("روما") -> "الدوري الإيطالي"
            t.contains("باريس") || t.contains("مارسيليا") || t.contains("موناكو") || t.contains("ليون") -> "الدوري الفرنسي"
            t.contains("الهلال") || t.contains("النصر") || t.contains("الاتحاد") || t.contains("الأهلي السعودي") || t.contains("الشباب") -> "دوري روشن السعودي"
            t.contains("الأهلي") || t.contains("الاهلي") || t.contains("الزمالك") || t.contains("بيراميدز") -> "الدوري المصري الممتاز"
            else -> "دوري أبطال أوروبا"
        }
    }

    private fun detectStadium(team1: String): String {
        val t = team1.lowercase()
        return when {
            t.contains("ريال مدريد") || t.contains("real madrid") -> "ملعب سانتياغو برنابيو 🏟️"
            t.contains("برشلونة") || t.contains("barcelona") -> "ملعب لويس كومبانيس الأولمبي 🏟️"
            t.contains("مانشستر سيتي") || t.contains("man city") -> "ملعب الاتحاد 🏟️"
            t.contains("ليفربول") || t.contains("liverpool") -> "ملعب أنفيلد رود 🏟️"
            t.contains("أرسنال") || t.contains("arsenal") -> "ملعب الإمارات 🏟️"
            t.contains("بايرن ميونخ") || t.contains("bayern") -> "أليانز أرينا 🏟️"
            t.contains("باريس سان جيرمان") || t.contains("psg") -> "ملعب حديقة الأمراء 🏟️"
            t.contains("الهلال") -> "المملكة أرينا 🏟️"
            t.contains("النصر") -> "ملعب الأول بارك 🏟️"
            t.contains("الاتحاد") || t.contains("الأهلي السعودي") -> "مدينة الملك عبدالله الرياضية (الجوهرة) 🏟️"
            t.contains("الأهلي") || t.contains("الزمالك") -> "استاد القاهرة الدولي 🏟️"
            else -> "الاستاد الدولي 🏟️"
        }
    }

    fun getCuratedMatches(): List<RichMatch> {
        val now = System.currentTimeMillis()

        return listOf(
            RichMatch(
                id = "curated_live_1",
                homeTeam = Team("ريال مدريد", TeamLogoHelper.findLogo("ريال مدريد") ?: ""),
                awayTeam = Team("برشلونة", TeamLogoHelper.findLogo("برشلونة") ?: ""),
                team1 = "ريال مدريد",
                team2 = "برشلونة",
                team1Logo = TeamLogoHelper.findLogo("ريال مدريد"),
                team2Logo = TeamLogoHelper.findLogo("برشلونة"),
                league = League("الدوري الإسباني", getTournamentLogo("الدوري الإسباني") ?: ""),
                leagueName = "الدوري الإسباني",
                leagueLogo = getTournamentLogo("الدوري الإسباني"),
                round = "الكلاسيكو • الجولة 32",
                timeFormatted = "مباشر الآن 🔴",
                status = MatchStatus.LIVE,
                minute = 68,
                score1 = 2,
                score2 = 1,
                stadium = "ملعب سانتياغو برنابيو 🏟️",
                referee = "خيل مانزانو",
                commentator = "عصام الشوالي",
                streamUrl = ""
            ),
            RichMatch(
                id = "curated_today_2",
                homeTeam = Team("مانشستر سيتي", TeamLogoHelper.findLogo("مانشستر سيتي") ?: ""),
                awayTeam = Team("ليفربول", TeamLogoHelper.findLogo("ليفربول") ?: ""),
                team1 = "مانشستر سيتي",
                team2 = "ليفربول",
                team1Logo = TeamLogoHelper.findLogo("مانشستر سيتي"),
                team2Logo = TeamLogoHelper.findLogo("ليفربول"),
                league = League("الدوري الإنجليزي الممتاز", getTournamentLogo("الدوري الإنجليزي الممتاز") ?: ""),
                leagueName = "الدوري الإنجليزي الممتاز",
                leagueLogo = getTournamentLogo("الدوري الإنجليزي الممتاز"),
                round = "قمة الجولة 28",
                timeFormatted = "08:30 م",
                status = MatchStatus.UPCOMING,
                stadium = "ملعب الاتحاد 🏟️",
                referee = "مايكل أوليفر",
                commentator = "حفيظ دراجي"
            ),
            RichMatch(
                id = "curated_today_3",
                homeTeam = Team("أرسنال", TeamLogoHelper.findLogo("أرسنال") ?: ""),
                awayTeam = Team("تشيلسي", TeamLogoHelper.findLogo("تشيلسي") ?: ""),
                team1 = "أرسنال",
                team2 = "تشيلسي",
                team1Logo = TeamLogoHelper.findLogo("أرسنال"),
                team2Logo = TeamLogoHelper.findLogo("تشيلسي"),
                league = League("الدوري الإنجليزي الممتاز", getTournamentLogo("الدوري الإنجليزي الممتاز") ?: ""),
                leagueName = "الدوري الإنجليزي الممتاز",
                leagueLogo = getTournamentLogo("الدوري الإنجليزي الممتاز"),
                round = "ديربي لندن • الجولة 28",
                timeFormatted = "10:45 م",
                status = MatchStatus.UPCOMING,
                stadium = "ملعب الإمارات 🏟️",
                referee = "أنتوني تايلور",
                commentator = "خليل البلوشي"
            ),
            RichMatch(
                id = "curated_today_4",
                homeTeam = Team("الهلال", TeamLogoHelper.findLogo("الهلال") ?: ""),
                awayTeam = Team("النصر", TeamLogoHelper.findLogo("النصر") ?: ""),
                team1 = "الهلال",
                team2 = "النصر",
                team1Logo = TeamLogoHelper.findLogo("الهلال"),
                team2Logo = TeamLogoHelper.findLogo("النصر"),
                league = League("دوري روشن السعودي", getTournamentLogo("دوري روشن السعودي") ?: ""),
                leagueName = "دوري روشن السعودي",
                leagueLogo = getTournamentLogo("دوري روشن السعودي"),
                round = "ديربي الرياض • الجولة 22",
                timeFormatted = "09:00 م",
                status = MatchStatus.UPCOMING,
                stadium = "المملكة أرينا 🏟️",
                referee = "إستيفان كوفاكس",
                commentator = "فارس عوض"
            )
        )
    }

    val TOP_CLUBS = listOf(
        Pair("ريال مدريد", TeamLogoHelper.findLogo("ريال مدريد")),
        Pair("برشلونة", TeamLogoHelper.findLogo("برشلونة")),
        Pair("مانشستر سيتي", TeamLogoHelper.findLogo("مانشستر سيتي")),
        Pair("ليفربول", TeamLogoHelper.findLogo("ليفربول")),
        Pair("أرسنال", TeamLogoHelper.findLogo("أرسنال")),
        Pair("بايرن ميونخ", TeamLogoHelper.findLogo("بايرن ميونخ")),
        Pair("باريس سان جيرمان", TeamLogoHelper.findLogo("باريس سان جيرمان")),
        Pair("الهلال", TeamLogoHelper.findLogo("الهلال")),
        Pair("النصر", TeamLogoHelper.findLogo("النصر")),
        Pair("الاتحاد", TeamLogoHelper.findLogo("الاتحاد")),
        Pair("الأهلي", TeamLogoHelper.findLogo("الأهلي")),
        Pair("الزمالك", TeamLogoHelper.findLogo("الزمالك"))
    )

    val TOP_TOURNAMENTS = listOf(
        Pair("دوري أبطال أوروبا", TOURNAMENT_LOGOS["دوري أبطال أوروبا"]),
        Pair("الدوري الإنجليزي", TOURNAMENT_LOGOS["الدوري الإنجليزي"]),
        Pair("الدوري الإسباني", TOURNAMENT_LOGOS["الدوري الإسباني"]),
        Pair("دوري روشن السعودي", TOURNAMENT_LOGOS["دوري روشن السعودي"]),
        Pair("الدوري الإيطالي", TOURNAMENT_LOGOS["الدوري الإيطالي"]),
        Pair("الدوري المصري", TOURNAMENT_LOGOS["الدوري المصري"])
    )

    fun buildDefaultLineup(teamName: String, isHome: Boolean): Lineup {
        return Lineup(teamName = teamName)
    }

    fun buildDefaultTimeline(team1: String, team2: String, isLive: Boolean): List<MatchTimelineEvent> {
        return emptyList()
    }

    fun buildStandingsForLeague(leagueName: String): List<Standing> {
        return emptyList()
    }
}
