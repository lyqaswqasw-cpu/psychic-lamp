package com.example.ui.utils

import java.util.Locale

/**
 * Intelligent Football Team Logo & Match Banner Resolver.
 * Automatically resolves high-resolution official crests and logos for clubs & national teams.
 */
object TeamLogoHelper {

    // Standard high-reliability team logos map (Arabic & English name variations -> Logo URL)
    private val TEAM_LOGOS = mapOf(
        // Spanish La Liga
        "real madrid" to "https://images.fotmob.com/image_resources/logo/teamlogo/8633.png",
        "ريال مدريد" to "https://images.fotmob.com/image_resources/logo/teamlogo/8633.png",
        "الريال" to "https://images.fotmob.com/image_resources/logo/teamlogo/8633.png",
        "barcelona" to "https://images.fotmob.com/image_resources/logo/teamlogo/8634.png",
        "برشلونة" to "https://images.fotmob.com/image_resources/logo/teamlogo/8634.png",
        "البارسا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8634.png",
        "atletico madrid" to "https://images.fotmob.com/image_resources/logo/teamlogo/9906.png",
        "أتلتيكو مدريد" to "https://images.fotmob.com/image_resources/logo/teamlogo/9906.png",
        "اتلتيكو مدريد" to "https://images.fotmob.com/image_resources/logo/teamlogo/9906.png",
        "sevilla" to "https://images.fotmob.com/image_resources/logo/teamlogo/8302.png",
        "إشبيلية" to "https://images.fotmob.com/image_resources/logo/teamlogo/8302.png",
        "اشبيلية" to "https://images.fotmob.com/image_resources/logo/teamlogo/8302.png",
        "real betis" to "https://images.fotmob.com/image_resources/logo/teamlogo/8603.png",
        "ريال بيتيس" to "https://images.fotmob.com/image_resources/logo/teamlogo/8603.png",
        "بيتيس" to "https://images.fotmob.com/image_resources/logo/teamlogo/8603.png",
        "villarreal" to "https://images.fotmob.com/image_resources/logo/teamlogo/10205.png",
        "فياريال" to "https://images.fotmob.com/image_resources/logo/teamlogo/10205.png",
        "valencia" to "https://images.fotmob.com/image_resources/logo/teamlogo/10233.png",
        "فالنسيا" to "https://images.fotmob.com/image_resources/logo/teamlogo/10233.png",
        "athletic bilbao" to "https://images.fotmob.com/image_resources/logo/teamlogo/8315.png",
        "أتلتيك بيلباو" to "https://images.fotmob.com/image_resources/logo/teamlogo/8315.png",
        "اتلتيك بلباو" to "https://images.fotmob.com/image_resources/logo/teamlogo/8315.png",
        "بيلباو" to "https://images.fotmob.com/image_resources/logo/teamlogo/8315.png",
        "real sociedad" to "https://images.fotmob.com/image_resources/logo/teamlogo/8983.png",
        "ريال سوسيداد" to "https://images.fotmob.com/image_resources/logo/teamlogo/8983.png",
        "سوسيداد" to "https://images.fotmob.com/image_resources/logo/teamlogo/8983.png",
        "girona" to "https://images.fotmob.com/image_resources/logo/teamlogo/7732.png",
        "جيرونا" to "https://images.fotmob.com/image_resources/logo/teamlogo/7732.png",

        // English Premier League
        "manchester city" to "https://images.fotmob.com/image_resources/logo/teamlogo/8456.png",
        "مانشستر سيتي" to "https://images.fotmob.com/image_resources/logo/teamlogo/8456.png",
        "مان سيتي" to "https://images.fotmob.com/image_resources/logo/teamlogo/8456.png",
        "liverpool" to "https://images.fotmob.com/image_resources/logo/teamlogo/8650.png",
        "ليفربول" to "https://images.fotmob.com/image_resources/logo/teamlogo/8650.png",
        "الريدز" to "https://images.fotmob.com/image_resources/logo/teamlogo/8650.png",
        "arsenal" to "https://images.fotmob.com/image_resources/logo/teamlogo/9825.png",
        "أرسنال" to "https://images.fotmob.com/image_resources/logo/teamlogo/9825.png",
        "ارسنال" to "https://images.fotmob.com/image_resources/logo/teamlogo/9825.png",
        "chelsea" to "https://images.fotmob.com/image_resources/logo/teamlogo/8455.png",
        "تشيلسي" to "https://images.fotmob.com/image_resources/logo/teamlogo/8455.png",
        "manchester united" to "https://images.fotmob.com/image_resources/logo/teamlogo/10260.png",
        "مانشستر يونايتد" to "https://images.fotmob.com/image_resources/logo/teamlogo/10260.png",
        "مان يونايتد" to "https://images.fotmob.com/image_resources/logo/teamlogo/10260.png",
        "اليونايتد" to "https://images.fotmob.com/image_resources/logo/teamlogo/10260.png",
        "tottenham" to "https://images.fotmob.com/image_resources/logo/teamlogo/8586.png",
        "توتنهام" to "https://images.fotmob.com/image_resources/logo/teamlogo/8586.png",
        "newcastle" to "https://images.fotmob.com/image_resources/logo/teamlogo/10261.png",
        "نيوكاسل" to "https://images.fotmob.com/image_resources/logo/teamlogo/10261.png",
        "نيوكاسل يونايتد" to "https://images.fotmob.com/image_resources/logo/teamlogo/10261.png",
        "aston villa" to "https://images.fotmob.com/image_resources/logo/teamlogo/10252.png",
        "أستون فيلا" to "https://images.fotmob.com/image_resources/logo/teamlogo/10252.png",
        "استون فيلا" to "https://images.fotmob.com/image_resources/logo/teamlogo/10252.png",
        "brighton" to "https://images.fotmob.com/image_resources/logo/teamlogo/10204.png",
        "برايتون" to "https://images.fotmob.com/image_resources/logo/teamlogo/10204.png",
        "west ham" to "https://images.fotmob.com/image_resources/logo/teamlogo/8654.png",
        "وست هام" to "https://images.fotmob.com/image_resources/logo/teamlogo/8654.png",

        // German Bundesliga
        "bayern munich" to "https://images.fotmob.com/image_resources/logo/teamlogo/9823.png",
        "بايرن ميونخ" to "https://images.fotmob.com/image_resources/logo/teamlogo/9823.png",
        "بايرن ميونيخ" to "https://images.fotmob.com/image_resources/logo/teamlogo/9823.png",
        "البايرن" to "https://images.fotmob.com/image_resources/logo/teamlogo/9823.png",
        "borussia dortmund" to "https://images.fotmob.com/image_resources/logo/teamlogo/9789.png",
        "بوروسيا دورتموند" to "https://images.fotmob.com/image_resources/logo/teamlogo/9789.png",
        "دورتموند" to "https://images.fotmob.com/image_resources/logo/teamlogo/9789.png",
        "bayer leverkusen" to "https://images.fotmob.com/image_resources/logo/teamlogo/8178.png",
        "باير ليفركوزن" to "https://images.fotmob.com/image_resources/logo/teamlogo/8178.png",
        "ليفركوزن" to "https://images.fotmob.com/image_resources/logo/teamlogo/8178.png",
        "rb leipzig" to "https://images.fotmob.com/image_resources/logo/teamlogo/178475.png",
        "لايبزيج" to "https://images.fotmob.com/image_resources/logo/teamlogo/178475.png",
        "لايبزغ" to "https://images.fotmob.com/image_resources/logo/teamlogo/178475.png",

        // Italian Serie A
        "juventus" to "https://images.fotmob.com/image_resources/logo/teamlogo/9885.png",
        "يوفنتوس" to "https://images.fotmob.com/image_resources/logo/teamlogo/9885.png",
        "اليوفي" to "https://images.fotmob.com/image_resources/logo/teamlogo/9885.png",
        "inter milan" to "https://images.fotmob.com/image_resources/logo/teamlogo/8636.png",
        "إنتر ميلان" to "https://images.fotmob.com/image_resources/logo/teamlogo/8636.png",
        "انتر ميلان" to "https://images.fotmob.com/image_resources/logo/teamlogo/8636.png",
        "إنتر" to "https://images.fotmob.com/image_resources/logo/teamlogo/8636.png",
        "انتر" to "https://images.fotmob.com/image_resources/logo/teamlogo/8636.png",
        "ac milan" to "https://images.fotmob.com/image_resources/logo/teamlogo/8564.png",
        "ميلان" to "https://images.fotmob.com/image_resources/logo/teamlogo/8564.png",
        "إي سي ميلان" to "https://images.fotmob.com/image_resources/logo/teamlogo/8564.png",
        "اسي ميلان" to "https://images.fotmob.com/image_resources/logo/teamlogo/8564.png",
        "napoli" to "https://images.fotmob.com/image_resources/logo/teamlogo/9875.png",
        "نابولي" to "https://images.fotmob.com/image_resources/logo/teamlogo/9875.png",
        "roma" to "https://images.fotmob.com/image_resources/logo/teamlogo/8686.png",
        "روما" to "https://images.fotmob.com/image_resources/logo/teamlogo/8686.png",
        "lazio" to "https://images.fotmob.com/image_resources/logo/teamlogo/8543.png",
        "لاتسيو" to "https://images.fotmob.com/image_resources/logo/teamlogo/8543.png",
        "atalanta" to "https://images.fotmob.com/image_resources/logo/teamlogo/8524.png",
        "أتالانتا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8524.png",
        "اتالانتا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8524.png",

        // French Ligue 1
        "paris saint-germain" to "https://images.fotmob.com/image_resources/logo/teamlogo/9847.png",
        "psg" to "https://images.fotmob.com/image_resources/logo/teamlogo/9847.png",
        "باريس سان جيرمان" to "https://images.fotmob.com/image_resources/logo/teamlogo/9847.png",
        "باريس" to "https://images.fotmob.com/image_resources/logo/teamlogo/9847.png",
        "marseille" to "https://images.fotmob.com/image_resources/logo/teamlogo/8572.png",
        "مارسيليا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8572.png",
        "lyon" to "https://images.fotmob.com/image_resources/logo/teamlogo/9748.png",
        "ليون" to "https://images.fotmob.com/image_resources/logo/teamlogo/9748.png",
        "monaco" to "https://images.fotmob.com/image_resources/logo/teamlogo/9829.png",
        "موناكو" to "https://images.fotmob.com/image_resources/logo/teamlogo/9829.png",

        // Saudi Pro League
        "al hilal" to "https://images.fotmob.com/image_resources/logo/teamlogo/6510.png",
        "الهلال" to "https://images.fotmob.com/image_resources/logo/teamlogo/6510.png",
        "الهلال السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6510.png",
        "al nassr" to "https://images.fotmob.com/image_resources/logo/teamlogo/6511.png",
        "النصر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6511.png",
        "النصر السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6511.png",
        "al ittihad" to "https://images.fotmob.com/image_resources/logo/teamlogo/6509.png",
        "الاتحاد" to "https://images.fotmob.com/image_resources/logo/teamlogo/6509.png",
        "الاتحاد السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6509.png",
        "al ahli" to "https://images.fotmob.com/image_resources/logo/teamlogo/6508.png",
        "الأهلي السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6508.png",
        "الاهلي السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6508.png",
        "أهلي جدة" to "https://images.fotmob.com/image_resources/logo/teamlogo/6508.png",
        "اهلي جدة" to "https://images.fotmob.com/image_resources/logo/teamlogo/6508.png",
        "al shabab" to "https://images.fotmob.com/image_resources/logo/teamlogo/6512.png",
        "الشباب" to "https://images.fotmob.com/image_resources/logo/teamlogo/6512.png",
        "الشباب السعودي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6512.png",
        "al ettifaq" to "https://images.fotmob.com/image_resources/logo/teamlogo/6513.png",
        "الاتفاق" to "https://images.fotmob.com/image_resources/logo/teamlogo/6513.png",

        // Egyptian League
        "al ahly" to "https://images.fotmob.com/image_resources/logo/teamlogo/6561.png",
        "الأهلي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6561.png",
        "الاهلي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6561.png",
        "الأهلي المصري" to "https://images.fotmob.com/image_resources/logo/teamlogo/6561.png",
        "الاهلي المصري" to "https://images.fotmob.com/image_resources/logo/teamlogo/6561.png",
        "zamalek" to "https://images.fotmob.com/image_resources/logo/teamlogo/6562.png",
        "الزمالك" to "https://images.fotmob.com/image_resources/logo/teamlogo/6562.png",
        "نادي الزمالك" to "https://images.fotmob.com/image_resources/logo/teamlogo/6562.png",
        "pyramids" to "https://images.fotmob.com/image_resources/logo/teamlogo/817298.png",
        "بيراميدز" to "https://images.fotmob.com/image_resources/logo/teamlogo/817298.png",

        // North Africa / Arab Clubs
        "wydad" to "https://images.fotmob.com/image_resources/logo/teamlogo/178829.png",
        "الوداد" to "https://images.fotmob.com/image_resources/logo/teamlogo/178829.png",
        "الوداد الرياضي" to "https://images.fotmob.com/image_resources/logo/teamlogo/178829.png",
        "raja" to "https://images.fotmob.com/image_resources/logo/teamlogo/6686.png",
        "الرجاء" to "https://images.fotmob.com/image_resources/logo/teamlogo/6686.png",
        "الرجاء البيضاوي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6686.png",
        "esperance" to "https://images.fotmob.com/image_resources/logo/teamlogo/6676.png",
        "الترجي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6676.png",
        "الترجي التونسي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6676.png",
        "al sadd" to "https://images.fotmob.com/image_resources/logo/teamlogo/6520.png",
        "السد" to "https://images.fotmob.com/image_resources/logo/teamlogo/6520.png",
        "al duhail" to "https://images.fotmob.com/image_resources/logo/teamlogo/224213.png",
        "الدحيل" to "https://images.fotmob.com/image_resources/logo/teamlogo/224213.png",
        "al ain" to "https://images.fotmob.com/image_resources/logo/teamlogo/6517.png",
        "العين" to "https://images.fotmob.com/image_resources/logo/teamlogo/6517.png",
        "العين الإماراتي" to "https://images.fotmob.com/image_resources/logo/teamlogo/6517.png",
        "al shorta" to "https://images.fotmob.com/image_resources/logo/teamlogo/321897.png",
        "الشرطة" to "https://images.fotmob.com/image_resources/logo/teamlogo/321897.png",
        "الشرطة العراقي" to "https://images.fotmob.com/image_resources/logo/teamlogo/321897.png",
        "al quwa al jawiya" to "https://images.fotmob.com/image_resources/logo/teamlogo/321896.png",
        "القوة الجوية" to "https://images.fotmob.com/image_resources/logo/teamlogo/321896.png",
        "al zawraa" to "https://images.fotmob.com/image_resources/logo/teamlogo/321899.png",
        "الزوراء" to "https://images.fotmob.com/image_resources/logo/teamlogo/321899.png",

        // International National Teams
        "argentina" to "https://images.fotmob.com/image_resources/logo/teamlogo/6792.png",
        "الأرجنتين" to "https://images.fotmob.com/image_resources/logo/teamlogo/6792.png",
        "الارجنتين" to "https://images.fotmob.com/image_resources/logo/teamlogo/6792.png",
        "brazil" to "https://images.fotmob.com/image_resources/logo/teamlogo/8410.png",
        "البرازيل" to "https://images.fotmob.com/image_resources/logo/teamlogo/8410.png",
        "france" to "https://images.fotmob.com/image_resources/logo/teamlogo/8499.png",
        "فرنسا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8499.png",
        "germany" to "https://images.fotmob.com/image_resources/logo/teamlogo/8497.png",
        "ألمانيا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8497.png",
        "المانيا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8497.png",
        "spain" to "https://images.fotmob.com/image_resources/logo/teamlogo/8500.png",
        "إسبانيا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8500.png",
        "اسبانيا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8500.png",
        "portugal" to "https://images.fotmob.com/image_resources/logo/teamlogo/8498.png",
        "البرتغال" to "https://images.fotmob.com/image_resources/logo/teamlogo/8498.png",
        "england" to "https://images.fotmob.com/image_resources/logo/teamlogo/8496.png",
        "إنجلترا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8496.png",
        "انجلترا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8496.png",
        "italy" to "https://images.fotmob.com/image_resources/logo/teamlogo/8412.png",
        "إيطاليا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8412.png",
        "ايطاليا" to "https://images.fotmob.com/image_resources/logo/teamlogo/8412.png",
        "morocco" to "https://images.fotmob.com/image_resources/logo/teamlogo/6788.png",
        "المغرب" to "https://images.fotmob.com/image_resources/logo/teamlogo/6788.png",
        "منتخب المغرب" to "https://images.fotmob.com/image_resources/logo/teamlogo/6788.png",
        "saudi arabia" to "https://images.fotmob.com/image_resources/logo/teamlogo/6789.png",
        "السعودية" to "https://images.fotmob.com/image_resources/logo/teamlogo/6789.png",
        "منتخب السعودية" to "https://images.fotmob.com/image_resources/logo/teamlogo/6789.png",
        "egypt" to "https://images.fotmob.com/image_resources/logo/teamlogo/6784.png",
        "مصر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6784.png",
        "منتخب مصر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6784.png",
        "algeria" to "https://images.fotmob.com/image_resources/logo/teamlogo/6782.png",
        "الجزائر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6782.png",
        "منتخب الجزائر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6782.png",
        "tunisia" to "https://images.fotmob.com/image_resources/logo/teamlogo/6791.png",
        "تونس" to "https://images.fotmob.com/image_resources/logo/teamlogo/6791.png",
        "منتخب تونس" to "https://images.fotmob.com/image_resources/logo/teamlogo/6791.png",
        "iraq" to "https://images.fotmob.com/image_resources/logo/teamlogo/6785.png",
        "العراق" to "https://images.fotmob.com/image_resources/logo/teamlogo/6785.png",
        "منتخب العراق" to "https://images.fotmob.com/image_resources/logo/teamlogo/6785.png",
        "qatar" to "https://images.fotmob.com/image_resources/logo/teamlogo/6787.png",
        "قطر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6787.png",
        "منتخب قطر" to "https://images.fotmob.com/image_resources/logo/teamlogo/6787.png",
        "jordan" to "https://images.fotmob.com/image_resources/logo/teamlogo/6786.png",
        "الأردن" to "https://images.fotmob.com/image_resources/logo/teamlogo/6786.png",
        "الاردن" to "https://images.fotmob.com/image_resources/logo/teamlogo/6786.png"
    )

    /**
     * Resolves a high-quality team logo URL given a team name string.
     */
    fun findLogo(teamName: String): String? {
        val cleanName = cleanTeamName(teamName)
        if (cleanName.isBlank()) return null

        // Direct key lookup
        TEAM_LOGOS[cleanName]?.let { return it }

        // Substring / partial key matching
        for ((key, url) in TEAM_LOGOS) {
            if (cleanName == key || cleanName.contains(key) || key.contains(cleanName)) {
                return url
            }
        }
        return null
    }

    /**
     * Extracts dual logos or fallback if imageUrl string contains multiple comma/pipe separated URLs.
     */
    fun parseMatchLogos(rawImageUrl: String, team1: String, team2: String): Pair<String?, String?> {
        val trimmed = rawImageUrl.trim()
        if (trimmed.contains(",") || trimmed.contains("|")) {
            val delimiter = if (trimmed.contains(",")) "," else "|"
            val parts = trimmed.split(delimiter).map { it.trim() }
            val logo1 = parts.getOrNull(0)?.ifBlank { null }
            val logo2 = parts.getOrNull(1)?.ifBlank { null }
            return Pair(logo1 ?: findLogo(team1), logo2 ?: findLogo(team2))
        }

        // If a single image URL is given, determine if it's meant as a team logo or match poster
        val auto1 = findLogo(team1)
        val auto2 = findLogo(team2)

        return Pair(auto1, auto2)
    }

    private fun cleanTeamName(name: String): String {
        return name.lowercase(Locale.ROOT)
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("نادي", "")
            .replace("فريق", "")
            .replace("منتخب", "")
            .replace("fc", "")
            .replace("cf", "")
            .replace("club", "")
            .trim()
    }
}
