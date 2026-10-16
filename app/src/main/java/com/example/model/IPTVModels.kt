package com.example.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoQuality(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isAuto: Boolean = false,
    val trackGroupIndex: Int = -1,
    val trackIndex: Int = -1
)

data class AccessCode(
    val code: String = "",
    val isActive: Boolean = true,
    val accountId: String = ""
)

data class ActivationCode(
    val code: String = "",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val forcedFormat: String = "",
    val allowedTypes: List<String> = listOf("live", "movies", "series"),
    val m3uUrl: String = ""
)

data class IPTVAccount(
    val id: String = "",
    val name: String = "",
    val allowedCategories: List<String> = emptyList(),
    val allowedTypes: List<String> = listOf("live", "movies", "series"),
    val expiryDate: String = "",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val forcedFormat: String = "",
    val allowVpn: Boolean = false,
    val m3uUrl: String = ""
)

data class Channel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val category: String = "",
    val userAgent: String = "",
    val forcedFormat: String = "m3u8"
)

data class CastMember(
    val name: String = "",
    val role: String = "",
    val photoUrl: String = "",
    val character: String = "",
    val profileUrl: String = ""
) {
    val effectivePhotoUrl: String get() = photoUrl.ifEmpty { profileUrl }
    val hasPhoto: Boolean get() = effectivePhotoUrl.isNotBlank() && (effectivePhotoUrl.startsWith("http://") || effectivePhotoUrl.startsWith("https://"))
}

data class Movie(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val category: String = "",
    val duration: String = "",
    val rating: Double = 0.0,
    val year: Int = 2026,
    val director: String = "",
    val cast: String = "",
    val castList: List<CastMember> = emptyList(),
    val genre: String = "",
    val backdropUrl: String = "",
    val trailerUrl: String = "",
    val ageRating: String = "+16",
    val quality: String = "4K UHD",
    val country: String = "العالمي"
)

data class Series(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val category: String = "",
    val rating: Double = 0.0,
    val year: Int = 2026,
    val director: String = "",
    val cast: String = "",
    val castList: List<CastMember> = emptyList(),
    val genre: String = "",
    val backdropUrl: String = "",
    val trailerUrl: String = "",
    val ageRating: String = "+16",
    val quality: String = "FHD 1080p",
    val country: String = "العالمي"
)

data class Season(
    val id: String = "",
    val seriesId: String = "",
    val number: Int = 1,
    val title: String = ""
)

data class Episode(
    val id: String = "",
    val seasonId: String = "",
    val seriesId: String = "",
    val title: String = "",
    val description: String = "",
    val posterUrl: String = "",
    val streamUrl: String = "",
    val duration: String = "",
    val number: Int = 1,
    val thumbnailUrl: String = "",
    val plot: String = ""
)

data class IPTVCategory(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "channel", "movie", "series", "exclusive"
    val imageUrl: String = ""
)

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PublishedPackage(
    val id: String = "",
    val name: String = "",
    val m3uUrl: String = "",
    val imageUrl: String = "",
    val description: String = ""
)

data class M3uChannel(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null
)

data class PromoAd(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val targetUrl: String = "",
    val buttonText: String = "انضم الآن",
    val badge: String = "إعلان مميز",
    val isEnabled: Boolean = true,
    val type: String = "telegram", // "telegram", "general", "vip", "link"
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeHeroBanner(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val tag: String = "🔥 حصري 4K",
    val targetType: String = "tab", // "tab", "stream", "url"
    val targetValue: String = "channels", // "channels", "movies", "series", "news", or url
    val order: Int = 0,
    val isEnabled: Boolean = true
)

data class HomeSectionConfig(
    val showHeroCarousel: Boolean = true,
    val showQuickNav: Boolean = true,
    val showPromoAds: Boolean = true,
    val showTrendingChannels: Boolean = true,
    val showLatestMovies: Boolean = true,
    val showPopularSeries: Boolean = true,
    val showAnnouncements: Boolean = true,
    val showOnlineUsers: Boolean = true,
    val showNewsSection: Boolean = true,
    val showDevSection: Boolean = true,
    val announcementText: String = "مرحباً بكم في تطبيق +90 - استمتعوا بأقوى البث المباشر وأحدث مكتبة سينمائية بجودة 4K فائقة!",
    val customTitle: String = "الرئيسية"
)

data class VipCode(
    val code: String = "",
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val forcedFormat: String = "",
    val maxUsers: Int = 1,
    val currentUsers: Int = 0,
    val lastActive: Long = 0,
    val lastActiveSessionId: String = "",
    val allowedTypes: List<String> = listOf("live", "movies", "series"),
    val allowVpn: Boolean = false
)

data class EventServer(
    val id: String = "",
    val name: String = "سيرفر 1",
    val url: String = "",
    val forcedFormat: String = "m3u8"
)

data class EventChannel(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val streamUrl: String = "",
    val forcedFormat: String = "m3u8",
    val servers: List<EventServer> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "stream", // "stream" for normal event channels, "match" for match countdowns
    val targetChannelId: String = "", // Used when eventType is "match"
    val targetChannelIds: List<String> = emptyList(), // Support multiple channels for a match
    val startTimeMs: Long = 0L // Unix timestamp. 0 means no timer
)

data class LocalDownload(
    val id: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val videoUrl: String = "",
    val quality: String = "1080p FHD",
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val type: String = "movie", // "movie" or "episode"
    val seriesTitle: String = "",
    val episodeNumber: Int = 0,
    val seasonNumber: Int = 0,
    val isCompleted: Boolean = true,
    val duration: String = ""
)

data class ActiveDownloadState(
    val id: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val type: String = "movie", // "movie" or "episode"
    val quality: String = "1080p FHD",
    val progress: Float = 0f, // 0.0 to 1.0
    val speedFormatted: String = "0 KB/s",
    val downloadedSizeFormatted: String = "0 MB / 0 MB",
    val totalSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val etaFormatted: String = "",
    val phase: String = "analyzing" // "analyzing", "downloading", "merging", "completed", "failed"
)





data class NewsItem(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val dateStr: String = ""
)

// --- Match API Models ---

enum class MatchStatus {
    LIVE, UPCOMING, FINISHED
}

enum class TeamSide {
    HOME, AWAY
}

enum class TimelineEventType {
    GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION, PENALTY_GOAL, PENALTY_MISSED, VAR_DECISION
}

data class MatchTimelineEvent(
    val minute: String = "0",
    val extraMinute: String? = null,
    val teamSide: TeamSide = TeamSide.HOME,
    val eventType: TimelineEventType = TimelineEventType.GOAL,
    val playerName: String = "",
    val player: String = "",
    val assistPlayerName: String? = null,
    val subOutPlayerName: String? = null,
    val description: String = "",
    val teamName: String = ""
)

data class Team(
    val name: String = "",
    val logo: String = ""
)

data class League(
    val name: String = "",
    val badge: String = ""
)

data class Player(
    val number: Int = 0,
    val name: String = "",
    val position: String = "MF",
    val gridX: Float = 0.5f,
    val gridY: Float = 0.5f,
    val isCaptain: Boolean = false
)

data class Lineup(
    val teamName: String = "",
    val formation: String = "4-3-3",
    val coach: String = "المدير الفني",
    val startPlayers: List<Player> = emptyList(),
    val substitutePlayers: List<Player> = emptyList(),
    val startingXI: List<Player> = emptyList(),
    val substitutes: List<Player> = emptyList()
)

data class Standing(
    val rank: Int = 0,
    val team: Team = Team(),
    val teamName: String = "",
    val teamLogo: String? = null,
    val played: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val points: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val won: Int = 0,
    val draw: Int = 0,
    val lost: Int = 0,
    val goalDifference: Int = 0
)

data class MatchStats(
    val possessionHome: Int = 50,
    val possessionAway: Int = 50,
    val possession1: Int = 50,
    val possession2: Int = 50,
    val shotsHome: Int = 12,
    val shotsAway: Int = 9,
    val shots1: Int = 12,
    val shots2: Int = 9,
    val shotsOnTargetHome: Int = 5,
    val shotsOnTargetAway: Int = 4,
    val shotsOnTarget1: Int = 5,
    val shotsOnTarget2: Int = 4,
    val passesHome: Int = 450,
    val passesAway: Int = 380,
    val passes1: Int = 450,
    val passes2: Int = 380,
    val passAccuracyHome: Int = 86,
    val passAccuracyAway: Int = 82,
    val passAccuracy1: Int = 86,
    val passAccuracy2: Int = 82,
    val foulsHome: Int = 8,
    val foulsAway: Int = 11,
    val fouls1: Int = 8,
    val fouls2: Int = 11,
    val yellowCardsHome: Int = 1,
    val yellowCardsAway: Int = 2,
    val yellowCards1: Int = 1,
    val yellowCards2: Int = 2,
    val redCardsHome: Int = 0,
    val redCardsAway: Int = 0,
    val redCards1: Int = 0,
    val redCards2: Int = 0,
    val cornersHome: Int = 6,
    val cornersAway: Int = 4,
    val corners1: Int = 6,
    val corners2: Int = 4,
    val offsidesHome: Int = 2,
    val offsidesAway: Int = 1,
    val offsides1: Int = 2,
    val offsides2: Int = 1
)

data class YssMatch(
    val id: String = "",
    val homeTeam: Team = Team(),
    val awayTeam: Team = Team(),
    val team1: String = "",
    val team2: String = "",
    val team1Logo: String? = null,
    val team2Logo: String? = null,
    val homeScore: String = "0",
    val awayScore: String = "0",
    val score1: Int? = null,
    val score2: Int? = null,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val statusStr: String = "",
    val minute: Int? = null,
    val minuteStr: String = "",
    val time: String = "",
    val timeFormatted: String = "",
    val league: League = League(),
    val leagueName: String = "",
    val leagueLogo: String? = null,
    val isLive: Boolean = false,
    val stadium: String = "الملعب الرئيسي 🏟️",
    val referee: String = "طاقم تحكيم دولي",
    val commentator: String = "معلق رياضي",
    val channelName: String = "قناة البث المباشر",
    val temperature: String = "21°C",
    val streamUrl: String = "",
    val forcedFormat: String = "m3u8",
    val targetChannelId: String = "",
    val servers: List<EventServer> = emptyList(),
    val rawEventChannel: EventChannel? = null,
    val round: String = ""
)

typealias RichMatch = YssMatch

data class MatchDetails(
    val match: YssMatch = YssMatch(),
    val timeline: List<MatchTimelineEvent> = emptyList(),
    val timelineEvents: List<MatchTimelineEvent> = emptyList(),
    val stats: MatchStats = MatchStats(),
    val homeLineup: Lineup = Lineup(),
    val awayLineup: Lineup = Lineup(),
    val team1Lineup: Lineup = Lineup(),
    val team2Lineup: Lineup = Lineup(),
    val standings: List<Standing> = emptyList()
)

