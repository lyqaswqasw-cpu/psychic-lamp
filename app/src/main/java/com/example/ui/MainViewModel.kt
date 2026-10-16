package com.example.ui


import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.NewsItem
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseService
import com.example.data.FootballApiService
import com.example.data.PresenceService
import com.example.data.EspnMatch
import com.example.data.EspnMatchSummary
import com.example.data.EspnSoccerService
import com.example.data.await
import com.example.data.ChatService
import com.example.data.ChatMessage
import com.example.data.GlobalChatMessageItem
import com.example.data.GlobalChatUserItem
import com.example.data.Reel
import com.example.data.ReelComment
import com.example.data.ReelReply
import com.example.data.ReelsSettings
import com.example.data.ReelsService
import com.example.model.*
import com.example.ui.utils.MatchDataHelper
import com.example.ui.utils.*
import com.example.utils.NotificationHelper
import com.example.utils.VpnSecurityManager
import com.example.utils.AppVersionConfig
import com.example.utils.AppVersionManager
import com.example.utils.AppVersionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.utils.FastDownloaderEngine

data class AiApiKey(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val endpointUrl: String = "https://models.inference.ai.azure.com/chat/completions",
    val modelName: String = "gpt-4o-mini",
    val createdAt: Long = 0L
)

data class ChatUserProfile(
    val username: String = "",
    val name: String = "",
    val avatar: String = "neon_wolf",
    val role: String = "user"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("loop_live_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun <T> saveListToCache(key: String, list: List<T>, clazz: Class<T>) {
        try {
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            val json = adapter.toJson(list)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving $key to cache", e)
        }
    }

    private fun <T> loadListFromCache(key: String, clazz: Class<T>): List<T>? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            val type = Types.newParameterizedType(List::class.java, clazz)
            val adapter = moshi.adapter<List<T>>(type)
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading $key from cache", e)
            null
        }
    }

    private val _adFreeExpiry = MutableStateFlow<Long>(prefs.getLong("ad_free_expiry", 0L))
    val adFreeExpiry: StateFlow<Long> = _adFreeExpiry.asStateFlow()

    private val _adsWatchedCount = MutableStateFlow<Int>(prefs.getInt("ads_watched_count", 0))
    val adsWatchedCount: StateFlow<Int> = _adsWatchedCount.asStateFlow()

    private var vipHeartbeatJob: Job? = null

    val isAdFree: Boolean
        get() = _adFreeExpiry.value > System.currentTimeMillis()

    fun grantAdFreeHours(hours: Int) {
        val expiry = System.currentTimeMillis() + (hours * 3600000L)
        prefs.edit().putLong("ad_free_expiry", expiry).apply()
        _adFreeExpiry.value = expiry
    }

    fun incrementAdsWatchedCount(): Int {
        val newCount = _adsWatchedCount.value + 1
        _adsWatchedCount.value = newCount
        prefs.edit().putInt("ads_watched_count", newCount).apply()
        return newCount
    }

    fun resetAdsWatchedCount() {
        _adsWatchedCount.value = 0
        prefs.edit().putInt("ads_watched_count", 0).apply()
    }

    private val _aiApiKeys = MutableStateFlow<List<AiApiKey>>(emptyList())
    val aiApiKeys: StateFlow<List<AiApiKey>> = _aiApiKeys.asStateFlow()

    // Developer Control State values
    private val _isDeveloperAuthenticated = MutableStateFlow(false)
    val isDeveloperAuthenticated: StateFlow<Boolean> = _isDeveloperAuthenticated.asStateFlow()

    private val _isVersionGateBypassed = MutableStateFlow(false)
    val isVersionGateBypassed: StateFlow<Boolean> = _isVersionGateBypassed.asStateFlow()

    fun bypassVersionGateAsDeveloper() {
        _isDeveloperAuthenticated.value = true
        _isVersionGateBypassed.value = true
    }

    fun resetVersionGateBypass() {
        _isVersionGateBypassed.value = false
    }

    // Downloads system
    private val _downloads = MutableStateFlow<List<LocalDownload>>(emptyList())
    val downloads: StateFlow<List<LocalDownload>> = _downloads.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadPhase = MutableStateFlow("") // "", "analyzing", "downloading", "merging", "completed", "failed"
    val downloadPhase: StateFlow<String> = _downloadPhase.asStateFlow()

    private val _activeDownloadState = MutableStateFlow<ActiveDownloadState?>(null)
    val activeDownloadState: StateFlow<ActiveDownloadState?> = _activeDownloadState.asStateFlow()

    private val _isPipMode = MutableStateFlow(false)
    val isPipMode: StateFlow<Boolean> = _isPipMode.asStateFlow()

    fun setPipMode(enabled: Boolean) {
        _isPipMode.value = enabled
    }

    private val _downloadingItem = MutableStateFlow<LocalDownload?>(null)
    val downloadingItem: StateFlow<LocalDownload?> = _downloadingItem.asStateFlow()

    private val _developerActionStatus = MutableStateFlow<String?>(null)
    val developerActionStatus: StateFlow<String?> = _developerActionStatus.asStateFlow()

    private val _activationCodes = MutableStateFlow<List<ActivationCode>>(emptyList())
    val activationCodes: StateFlow<List<ActivationCode>> = _activationCodes.asStateFlow()

    // UI States
    private val _accessCode = MutableStateFlow<String?>(null)
    val accessCode: StateFlow<String?> = _accessCode.asStateFlow()

    private val _activeAccount = MutableStateFlow<IPTVAccount?>(null)
    val activeAccount: StateFlow<IPTVAccount?> = _activeAccount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _events = MutableStateFlow<List<EventChannel>>(emptyList())
    val events: StateFlow<List<EventChannel>> = _events.asStateFlow()

    private val _yssMatches = MutableStateFlow<List<YssMatch>>(emptyList())
    val yssMatches: StateFlow<List<YssMatch>> = _yssMatches.asStateFlow()

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _codeUpdateRequiredMessage = MutableStateFlow<String?>(null)
    val codeUpdateRequiredMessage: StateFlow<String?> = _codeUpdateRequiredMessage.asStateFlow()

    private val _isContentUpdating = MutableStateFlow(false)
    val isContentUpdating: StateFlow<Boolean> = _isContentUpdating.asStateFlow()

    private val _isMatchesLoading = MutableStateFlow(false)
    val isMatchesLoading: StateFlow<Boolean> = _isMatchesLoading.asStateFlow()

    private val _espnMatches = MutableStateFlow<List<EspnMatch>>(emptyList())
    val espnMatches: StateFlow<List<EspnMatch>> = _espnMatches.asStateFlow()

    private val _isEspnMatchesLoading = MutableStateFlow(false)
    val isEspnMatchesLoading: StateFlow<Boolean> = _isEspnMatchesLoading.asStateFlow()

    private val _selectedEspnLeague = MutableStateFlow<String>("all")
    val selectedEspnLeague: StateFlow<String> = _selectedEspnLeague.asStateFlow()

    private val _selectedEspnDate = MutableStateFlow<String>(
        java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
    )
    val selectedEspnDate: StateFlow<String> = _selectedEspnDate.asStateFlow()

    private val _selectedEspnMatch = MutableStateFlow<EspnMatch?>(null)
    val selectedEspnMatch: StateFlow<EspnMatch?> = _selectedEspnMatch.asStateFlow()

    private val _selectedEspnSummary = MutableStateFlow<EspnMatchSummary?>(null)
    val selectedEspnSummary: StateFlow<EspnMatchSummary?> = _selectedEspnSummary.asStateFlow()

    private val _isEspnSummaryLoading = MutableStateFlow(false)
    val isEspnSummaryLoading: StateFlow<Boolean> = _isEspnSummaryLoading.asStateFlow()

    private val _favoriteTeamNames = MutableStateFlow<Set<String>>(
        prefs.getStringSet("favorite_teams_set", null) ?: setOf("برشلونة", "ريال مدريد", "ليفربول", "النصر")
    )
    val favoriteTeamNames: StateFlow<Set<String>> = _favoriteTeamNames.asStateFlow()

    private val _selectedMatchDetails = MutableStateFlow<MatchDetails?>(null)
    val selectedMatchDetails: StateFlow<MatchDetails?> = _selectedMatchDetails.asStateFlow()

    private val _isMatchDetailsLoading = MutableStateFlow(false)
    val isMatchDetailsLoading: StateFlow<Boolean> = _isMatchDetailsLoading.asStateFlow()

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    private val _categories = MutableStateFlow<List<IPTVCategory>>(emptyList())
    val categories: StateFlow<List<IPTVCategory>> = _categories.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _isChannelsLoading = MutableStateFlow(false)
    val isChannelsLoading: StateFlow<Boolean> = _isChannelsLoading.asStateFlow()

    private val _isMoviesLoading = MutableStateFlow(false)
    val isMoviesLoading: StateFlow<Boolean> = _isMoviesLoading.asStateFlow()

    private val _isSeriesLoading = MutableStateFlow(false)
    val isSeriesLoading: StateFlow<Boolean> = _isSeriesLoading.asStateFlow()

    // Interactive UI variables
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Favorites & Recent
    private val _favorites = MutableStateFlow<Set<String>>(prefs.getStringSet("saved_favorites", null) ?: emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<Movie>>(emptyList())
    val continueWatching: StateFlow<List<Movie>> = _continueWatching.asStateFlow()

    // Navigation and Popups
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries.asStateFlow()

    private val _seasons = MutableStateFlow<List<Season>>(emptyList())
    val seasons: StateFlow<List<Season>> = _seasons.asStateFlow()

    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadNotificationsCount = MutableStateFlow(0)
    val unreadNotificationsCount: StateFlow<Int> = _unreadNotificationsCount.asStateFlow()

    private var lastReadNotificationTimestamp: Long
        get() = prefs.getLong("last_read_notification_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_read_notification_timestamp", value).apply()

    private fun updateUnreadCount() {
        val lastRead = lastReadNotificationTimestamp
        _unreadNotificationsCount.value = _notifications.value.count { it.timestamp > lastRead }
    }

    fun markNotificationsAsRead() {
        val latest = _notifications.value.maxOfOrNull { it.timestamp } ?: 0L
        if (latest > lastReadNotificationTimestamp) {
            lastReadNotificationTimestamp = latest
            _unreadNotificationsCount.value = 0
        }
    }

    // Home Hub and Promotional System
    private val defaultPromoAds = listOf(
        PromoAd(
            id = "default_telegram",
            title = "انضم إلى قناة التلغرام الرسمية",
            description = "احصل على أحدث الترددات، التحديثات الحصرية، وجدول المباريات اليومية أولاً بأول.",
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
            targetUrl = "https://t.me/jdj_q",
            buttonText = "انضم لقناة التيلجرام 🚀",
            badge = "⚡ قناة التليجرام",
            type = "telegram"
        ),
        PromoAd(
            id = "default_vip",
            title = "اشتراكات وسيرفرات VIP فائقة السرعة",
            description = "استمتع بمشاهدة بدون تقطيع وبأعلى جودة 4K UHD لجميع البطولات والقنوات المشفرة.",
            imageUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=800&auto=format&fit=crop&q=80",
            targetUrl = "https://t.me/jdj_q",
            buttonText = "تواصل للاشتراك 💎",
            badge = "👑 سيرفرات VIP",
            type = "vip"
        ),
        PromoAd(
            id = "default_cinema",
            title = "تحديثات يومية لأحدث الأفلام والمسلسلات",
            description = "أضخم مكتبة سينمائية عربية وعالمية مع ترجمة احترافية ودقة عالية بدون إعلانات مزعجة.",
            imageUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&auto=format&fit=crop&q=80",
            targetUrl = "https://t.me/jdj_q",
            buttonText = "استكشف الآن 🎬",
            badge = "🔥 سينما VIP",
            type = "general"
        )
    )

    private val defaultHeroBanners = listOf(
        HomeHeroBanner(
            id = "hero_1",
            title = "دوري أبطال أوروبا & البطولات الكبرى",
            subtitle = "شاهد البث المباشر لأقوى المباريات والقنوات الرياضية العالمية بجودة 4K بدون تقطيع",
            imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=1000&auto=format&fit=crop&q=80",
            tag = "⚽ بث مباشر الآن",
            targetType = "tab",
            targetValue = "channels",
            order = 1
        ),
        HomeHeroBanner(
            id = "hero_2",
            title = "أحدث الأفلام والسينما العالمية 2026",
            subtitle = "مكتبة سينمائية ضخمة متجددة يومياً بأعلى جودة وصوت محيطي نقي",
            imageUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1000&auto=format&fit=crop&q=80",
            tag = "🎬 سينما 4K UHD",
            targetType = "tab",
            targetValue = "movies",
            order = 2
        ),
        HomeHeroBanner(
            id = "hero_3",
            title = "أقوى المسلسلات الحصرية كاملة الحلقات",
            subtitle = "شاهد مسلسلاتك المفضلة بجودة Full HD مع دعم الترجمة والدبلجة الاحترافية",
            imageUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=1000&auto=format&fit=crop&q=80",
            tag = "🍿 مسلسلات حصرية",
            targetType = "tab",
            targetValue = "series",
            order = 3
        )
    )

    private val _promoAds = MutableStateFlow<List<PromoAd>>(defaultPromoAds)
    val promoAds: StateFlow<List<PromoAd>> = _promoAds.asStateFlow()

    private val _homeHeroBanners = MutableStateFlow<List<HomeHeroBanner>>(defaultHeroBanners)
    val homeHeroBanners: StateFlow<List<HomeHeroBanner>> = _homeHeroBanners.asStateFlow()

    private val _homeConfig = MutableStateFlow(HomeSectionConfig())
    val homeConfig: StateFlow<HomeSectionConfig> = _homeConfig.asStateFlow()

    // Player routing
    private val _playingStreamUrl = MutableStateFlow<String?>(null)
    val playingStreamUrl: StateFlow<String?> = _playingStreamUrl.asStateFlow()

    private val _playingTitle = MutableStateFlow("")
    val playingTitle: StateFlow<String> = _playingTitle.asStateFlow()

    private val _playingIsLive = MutableStateFlow(false)
    val playingIsLive: StateFlow<Boolean> = _playingIsLive.asStateFlow()

    private val _playingUserAgent = MutableStateFlow("")
    val playingUserAgent: StateFlow<String> = _playingUserAgent.asStateFlow()

    private val _forcedFormat = MutableStateFlow("")
    val forcedFormat: StateFlow<String> = _forcedFormat.asStateFlow()

    private val _playingServers = MutableStateFlow<List<EventServer>>(emptyList())
    val playingServers: StateFlow<List<EventServer>> = _playingServers.asStateFlow()

    private val _currentServerIndex = MutableStateFlow(0)
    val currentServerIndex: StateFlow<Int> = _currentServerIndex.asStateFlow()

    private val _isLivePlayerFullScreen = MutableStateFlow(false)
    val isLivePlayerFullScreen: StateFlow<Boolean> = _isLivePlayerFullScreen.asStateFlow()

    fun setLivePlayerFullScreen(enabled: Boolean) {
        _isLivePlayerFullScreen.value = enabled
    }

    private val _isEventChannelsPlayerActive = MutableStateFlow(false)
    val isEventChannelsPlayerActive: StateFlow<Boolean> = _isEventChannelsPlayerActive.asStateFlow()

    private val _currentViewerCount = MutableStateFlow(0)
    val currentViewerCount: StateFlow<Int> = _currentViewerCount.asStateFlow()

    private val _liveOnlineUsersCount = MutableStateFlow(1)
    val liveOnlineUsersCount: StateFlow<Int> = _liveOnlineUsersCount.asStateFlow()

    fun setEventChannelsPlayerActive(active: Boolean) {
        _isEventChannelsPlayerActive.value = active
    }

    private val _isPackagesPlayerFullScreen = MutableStateFlow(false)
    val isPackagesPlayerFullScreen: StateFlow<Boolean> = _isPackagesPlayerFullScreen.asStateFlow()

    private val _packagesActiveStreamUrl = MutableStateFlow<String?>(null)
    val packagesActiveStreamUrl: StateFlow<String?> = _packagesActiveStreamUrl.asStateFlow()

    private val _packagesActiveStreamName = MutableStateFlow<String>("")
    val packagesActiveStreamName: StateFlow<String> = _packagesActiveStreamName.asStateFlow()

    private val _packagesChannelsList = MutableStateFlow<List<M3uChannel>>(emptyList())
    val packagesChannelsList: StateFlow<List<M3uChannel>> = _packagesChannelsList.asStateFlow()

    fun setPackagesActiveStream(url: String?, name: String) {
        _packagesActiveStreamUrl.value = url
        _packagesActiveStreamName.value = name
    }

    fun setPackagesChannelsList(list: List<M3uChannel>) {
        _packagesChannelsList.value = list
    }

    fun setPackagesPlayerFullScreen(enabled: Boolean) {
        _isPackagesPlayerFullScreen.value = enabled
    }

    // Player format preferences: "smart", "ts", "m3u8", "normal"
    private val _playerMode = MutableStateFlow(prefs.getString("saved_player_type", "smart") ?: "smart")
    val playerMode: StateFlow<String> = _playerMode.asStateFlow()

    private val _showNotificationIcon = MutableStateFlow(prefs.getBoolean("show_notification_icon", true))
    val showNotificationIcon: StateFlow<Boolean> = _showNotificationIcon.asStateFlow()

    private val _isShareButtonVisible = MutableStateFlow(true)
    val isShareButtonVisible: StateFlow<Boolean> = _isShareButtonVisible.asStateFlow()

    private val _isDevMode = MutableStateFlow(false)
    val isDevMode: StateFlow<Boolean> = _isDevMode.asStateFlow()

    private val _isGlobalAdsEnabled = MutableStateFlow(true)
    val rawGlobalAdsEnabled: StateFlow<Boolean> = _isGlobalAdsEnabled.asStateFlow()
    val isGlobalAdsEnabled: StateFlow<Boolean> = combine(_isGlobalAdsEnabled, _activeAccount) { enabled, account ->
        val isVip = account?.name?.lowercase()?.let { it.contains("vip") || it.contains("+90") } ?: false
        if (isVip) false else enabled
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _showRemoveAdsIcon = MutableStateFlow(true)
    val rawShowRemoveAdsIcon: StateFlow<Boolean> = _showRemoveAdsIcon.asStateFlow()
    val showRemoveAdsIcon: StateFlow<Boolean> = combine(_showRemoveAdsIcon, _activeAccount) { show, account ->
        val isVip = account?.name?.lowercase()?.let { it.contains("vip") || it.contains("+90") } ?: false
        if (isVip) false else show
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _isVpnBlockEnabled = MutableStateFlow(prefs.getBoolean("is_vpn_block_enabled", true))
    val isVpnBlockEnabled: StateFlow<Boolean> = _isVpnBlockEnabled.asStateFlow()

    val isEffectiveVpnBlocked: StateFlow<Boolean> = combine(
        _isVpnBlockEnabled,
        _activeAccount,
        VpnSecurityManager.isVpnActiveState
    ) { globalEnabled, account, isVpnActive ->
        if (!globalEnabled) return@combine false
        if (account?.allowVpn == true) return@combine false
        isVpnActive
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _vipCodes = MutableStateFlow<List<VipCode>>(emptyList())
    val vipCodes: StateFlow<List<VipCode>> = _vipCodes.asStateFlow()

    private val _appVersionStatus = MutableStateFlow<AppVersionStatus>(AppVersionStatus.Loading)
    val appVersionStatus: StateFlow<AppVersionStatus> = _appVersionStatus.asStateFlow()

    val appVersionConfig = MutableStateFlow(AppVersionConfig())

    private val _publishedPackages = MutableStateFlow<List<PublishedPackage>>(emptyList())
    val publishedPackages: StateFlow<List<PublishedPackage>> = _publishedPackages.asStateFlow()

    private val _showSplashScreen = MutableStateFlow(true)
    val showSplashScreen: StateFlow<Boolean> = _showSplashScreen.asStateFlow()

    init {
        checkAppVersionStatus()
        fetchAppConfig()
        refreshDownloads()
        loadAiApiKeys()
        loadPublishedPackages()
        loadHomeData()
        loadEventChannels()
            loadNews()
        fetchNotifications()
        
        viewModelScope.launch {
            delay(800)
            _showSplashScreen.value = false
        }

        // Periodic match notification simulation / score changes for subscribed matches
        viewModelScope.launch {
            delay(10000L) // Wait a bit after startup
            val simulatedMatchScores = mutableMapOf<String, Pair<Int, Int>>() // Match ID to (Home, Away) score
            val notifiedMatchStarts = mutableSetOf<String>()
            
            while (isActive) {
                try {
                    val subs = _subscribedMatches.value
                    if (subs.isNotEmpty()) {
                        val matches = _espnMatches.value
                        for (matchId in subs) {
                            val match = matches.find { it.id == matchId } ?: _selectedEspnMatch.value
                            if (match != null) {
                                val currentHome = match.homeTeam.score.toIntOrNull() ?: 0
                                val currentAway = match.awayTeam.score.toIntOrNull() ?: 0
                                
                                val scoreKey = match.id
                                val lastKnown = simulatedMatchScores[scoreKey]
                                
                                if (lastKnown == null) {
                                    simulatedMatchScores[scoreKey] = Pair(currentHome, currentAway)
                                    if (match.statusState == "in" && !notifiedMatchStarts.contains(match.id)) {
                                        notifiedMatchStarts.add(match.id)
                                        com.example.utils.NotificationHelper.showSystemNotification(
                                            getApplication(),
                                            "⚽ بدأت المباراة الآن",
                                            "انطلقت مباراة ${match.homeTeam.arabicName} ضد ${match.awayTeam.arabicName}! تمنياتنا لكم بمشاهدة ممتعة."
                                        )
                                    }
                                } else {
                                    if (match.statusState == "in" && !notifiedMatchStarts.contains(match.id)) {
                                        notifiedMatchStarts.add(match.id)
                                        com.example.utils.NotificationHelper.showSystemNotification(
                                            getApplication(),
                                            "⚽ بدأت المباراة الآن",
                                            "انطلقت مباراة ${match.homeTeam.arabicName} ضد ${match.awayTeam.arabicName}! تمنياتنا لكم بمشاهدة ممتعة."
                                        )
                                    }

                                    // Random goal simulation (10% chance every 40s)
                                    if (match.statusState == "in" && Math.random() < 0.10) {
                                        val isHomeGoal = Math.random() < 0.5
                                        val newHome = if (isHomeGoal) lastKnown.first + 1 else lastKnown.first
                                        val newAway = if (!isHomeGoal) lastKnown.second + 1 else lastKnown.second
                                        simulatedMatchScores[scoreKey] = Pair(newHome, newAway)
                                        
                                        val scorerTeam = if (isHomeGoal) match.homeTeam.arabicName else match.awayTeam.arabicName
                                        com.example.utils.NotificationHelper.showSystemNotification(
                                            getApplication(),
                                            "⚽ جووووول! هدف لصالح $scorerTeam",
                                            "أحرز فريق $scorerTeam هدفاً رائعاً في شباك خصمه! النتيجة الآن: ${match.homeTeam.arabicName} $newHome - $newAway ${match.awayTeam.arabicName}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error in match alert notification simulation", e)
                }
                delay(40000L)
            }
        }

        // Realtime Online Presence Heartbeat & Live Online Counter (every 12 seconds)
        viewModelScope.launch {
            while (isActive) {
                try {
                    val uname = _chatUsername.value ?: _activeAccount.value?.username ?: ""
                    PresenceService.sendHeartbeat(getApplication(), uname)
                    val realCount = PresenceService.getLiveOnlineCount()
                    _liveOnlineUsersCount.value = realCount
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Presence heartbeat note: ${e.message}")
                }
                delay(12000L)
            }
        }

        // Realtime / periodic remote version & maintenance gate watcher (every 15 seconds)
        viewModelScope.launch {
            while (isActive) {
                try {
                    val config = FirebaseService.getAppVersionConfig()
                    appVersionConfig.value = config
                    val status = AppVersionManager.evaluate(config, AppVersionManager.getEffectiveAppVersion())
                    _appVersionStatus.value = status
                    if (!_isVersionGateBypassed.value && (status is AppVersionStatus.Maintenance || status is AppVersionStatus.UpdateRequired)) {
                        stopPlaying()
                        setPackagesPlayerFullScreen(false)
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Periodic version sync status: ${e.message}")
                }
                delay(15000L)
            }
        }

        // Realtime notification sync & alert watcher
        viewModelScope.launch {
            while (isActive) {
                try {
                    val remoteNotes = FirebaseService.getNotifications()
                    if (remoteNotes.isNotEmpty()) {
                        val lastNotifiedTimestamp = prefs.getLong("last_system_notified_timestamp", 0L)
                        val latestNote = remoteNotes.maxByOrNull { it.timestamp }
                        if (latestNote != null && latestNote.timestamp > lastNotifiedTimestamp && (System.currentTimeMillis() - latestNote.timestamp) < 24 * 3600 * 1000L) {
                            prefs.edit().putLong("last_system_notified_timestamp", latestNote.timestamp).apply()
                            NotificationHelper.showSystemNotification(
                                context = getApplication(),
                                title = latestNote.title,
                                body = latestNote.body,
                                imageUrl = latestNote.imageUrl
                            )
                        }
                        _notifications.value = remoteNotes
                        updateUnreadCount()
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Periodic notification sync error: ${e.message}")
                }
                delay(25000L)
            }
        }
    }

    fun checkAppVersionStatus(): Job = viewModelScope.launch {
        try {
            val config = FirebaseService.getAppVersionConfig()
            appVersionConfig.value = config
            val status = AppVersionManager.evaluate(config, AppVersionManager.getEffectiveAppVersion())
            _appVersionStatus.value = status
            if (!_isVersionGateBypassed.value && (status is AppVersionStatus.Maintenance || status is AppVersionStatus.UpdateRequired)) {
                Log.w("MainViewModel", "AppVersionGate active: $status. Halting playback.")
                stopPlaying()
                setPackagesPlayerFullScreen(false)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error fetching app version status", e)
            if (_appVersionStatus.value is AppVersionStatus.Loading) {
                _appVersionStatus.value = AppVersionStatus.Allowed(AppVersionConfig())
            }
        }
    }

    fun isAppAccessAllowed(): Boolean {
        if (_isVersionGateBypassed.value) return true
        val status = _appVersionStatus.value
        return status !is AppVersionStatus.Maintenance && status !is AppVersionStatus.UpdateRequired
    }

    fun updateRemoteVersionConfig(config: AppVersionConfig, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = FirebaseService.saveAppVersionConfig(config)
            if (success) {
                appVersionConfig.value = config
                _appVersionStatus.value = AppVersionManager.evaluate(config, AppVersionManager.getEffectiveAppVersion())
            }
            onComplete(success)
        }
    }

    fun loadPublishedPackages() {
        viewModelScope.launch {
            try {
                _publishedPackages.value = FirebaseService.getPublishedPackages()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading published packages", e)
            }
        }
    }

    fun publishPackage(name: String, m3uUrl: String, imageUrl: String, description: String = "") {
        viewModelScope.launch {
            val pkg = PublishedPackage(name = name, m3uUrl = m3uUrl, imageUrl = imageUrl, description = description)
            val success = FirebaseService.savePublishedPackage(pkg)
            if (success) {
                loadPublishedPackages()
            }
        }
    }

    fun deletePublishedPackage(id: String) {
        viewModelScope.launch {
            val success = FirebaseService.deletePublishedPackage(id)
            if (success) {
                loadPublishedPackages()
            }
        }
    }

    fun loadAiApiKeys() {
        viewModelScope.launch {
            try {
                val keys = FirebaseService.getAiApiKeys()
                _aiApiKeys.value = keys
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load AI API keys", e)
            }
        }
    }

    fun addAiApiKey(key: String, name: String) {
        addAiApiKey(key, name, "https://models.inference.ai.azure.com/chat/completions", "gpt-4o-mini")
    }

    fun addAiApiKey(key: String, name: String, endpointUrl: String, modelName: String) {
        viewModelScope.launch {
            try {
                val id = "ai_key_" + System.currentTimeMillis()
                val newKey = AiApiKey(
                    id = id,
                    key = key,
                    name = name,
                    endpointUrl = endpointUrl,
                    modelName = modelName,
                    createdAt = System.currentTimeMillis()
                )
                FirebaseService.saveAiApiKey(newKey)
                loadAiApiKeys()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to add AI API key", e)
            }
        }
    }

    fun updateAiApiKey(id: String, key: String, name: String) {
        updateAiApiKey(id, key, name, "https://models.inference.ai.azure.com/chat/completions", "gpt-4o-mini")
    }

    fun updateAiApiKey(id: String, key: String, name: String, endpointUrl: String, modelName: String) {
        viewModelScope.launch {
            try {
                val existing = _aiApiKeys.value.find { it.id == id }
                val createdAt = existing?.createdAt ?: System.currentTimeMillis()
                val updatedKey = AiApiKey(
                    id = id,
                    key = key,
                    name = name,
                    endpointUrl = endpointUrl,
                    modelName = modelName,
                    createdAt = createdAt
                )
                FirebaseService.saveAiApiKey(updatedKey)
                loadAiApiKeys()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update AI API key", e)
            }
        }
    }

    fun deleteAiApiKey(id: String) {
        viewModelScope.launch {
            try {
                FirebaseService.deleteAiApiKey(id)
                loadAiApiKeys()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to delete AI API key", e)
            }
        }
    }

    private fun fetchAppConfig() {
        viewModelScope.launch {
            val config = FirebaseService.getAppConfig()
            if (config != null) {
                _isShareButtonVisible.value = config["is_share_enabled"] ?: true
                _showAiSectionInDrawer.value = config["showAiSectionInDrawer"] ?: true
                _isGlobalAdsEnabled.value = config["isGlobalAdsEnabled"] ?: true
                _showRemoveAdsIcon.value = config["showRemoveAdsIcon"] ?: true
                _isVpnBlockEnabled.value = config["isVpnBlockEnabled"] ?: prefs.getBoolean("is_vpn_block_enabled", true)
            }
        }
    }

    fun updateEventChannel(id: String, updatedChannel: Channel) {
        viewModelScope.launch {
            val data = mapOf(
                "id" to updatedChannel.id,
                "name" to updatedChannel.name,
                "streamUrl" to updatedChannel.streamUrl,
                "logoUrl" to updatedChannel.logoUrl,
                "category" to updatedChannel.category
            )
            val success = FirebaseService.updateEventChannel(id, data)
            if (success) {
                loadEventChannels()
            loadNews()
            }
        }
    }

    fun deleteEventChannel(id: String) {
        viewModelScope.launch {
            val success = FirebaseService.deleteEventChannel(id)
            if (success) {
                loadEventChannels()
            loadNews()
            }
        }
    }

    fun setDevMode(enabled: Boolean) {
        _isDevMode.value = enabled
    }

    fun setShowNotificationIcon(show: Boolean) {
        _showNotificationIcon.value = show
        prefs.edit().putBoolean("show_notification_icon", show).apply()
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات أيقونة الإشعارات..."
            val success = FirebaseService.saveAppConfig("showNotificationIcon", show)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات الإشعارات بنجاح!"
                delay(3000)
                _developerActionStatus.value = null
            }
        }
    }

    fun setShareButtonVisibilityOnline(visible: Boolean) {
        _isShareButtonVisible.value = visible
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات المشاركة..."
            val success = FirebaseService.saveAppConfig("is_share_enabled", visible)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات المشاركة بنجاح!"
                delay(3000)
                _developerActionStatus.value = null
            }
        }
    }

    fun setGlobalAdsEnabled(enabled: Boolean) {
        _isGlobalAdsEnabled.value = enabled
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات الإعلانات العالمية..."
            val success = FirebaseService.saveAppConfig("isGlobalAdsEnabled", enabled)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات الإعلانات بنجاح!"
                delay(3000)
                _developerActionStatus.value = null
            }
        }
    }

    fun setShowRemoveAdsIcon(show: Boolean) {
        _showRemoveAdsIcon.value = show
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات أيقونة حذف الإعلانات..."
            val success = FirebaseService.saveAppConfig("showRemoveAdsIcon", show)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات حذف الإعلانات بنجاح!"
                delay(3000)
                _developerActionStatus.value = null
            }
        }
    }

    fun setVpnBlockEnabled(enabled: Boolean) {
        _isVpnBlockEnabled.value = enabled
        prefs.edit().putBoolean("is_vpn_block_enabled", enabled).apply()
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات حظر الـ VPN..."
            val success = FirebaseService.saveAppConfig("isVpnBlockEnabled", enabled)
            if (success) {
                _developerActionStatus.value = if (enabled) "تم تفعيل حظر الـ VPN بنجاح!" else "تم تعطيل حظر الـ VPN والسماح بالاستخدام!"
                delay(3000)
                _developerActionStatus.value = null
            }
        }
    }

    fun loadVipCodes() {
        viewModelScope.launch {
            _vipCodes.value = FirebaseService.getVipCodes()
        }
    }

    fun saveVipCode(vipCode: VipCode) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري الحفظ..."
            val success = FirebaseService.saveVipCode(vipCode)
            if (success) {
                _developerActionStatus.value = "تم حفظ الكود بنجاح!"
                loadVipCodes()
                delay(3000)
                _developerActionStatus.value = null
            } else {
                _developerActionStatus.value = "فشل حفظ الكود!"
            }
        }
    }

    fun deleteVipCode(code: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري الحذف..."
            val success = FirebaseService.deleteVipCode(code)
            if (success) {
                _developerActionStatus.value = "تم حذف الكود بنجاح!"
                loadVipCodes()
                delay(3000)
                _developerActionStatus.value = null
            } else {
                _developerActionStatus.value = "فشل حذف الكود!"
            }
        }
    }

    private val _showPackagesSection = MutableStateFlow(true)
    val showPackagesSection: StateFlow<Boolean> = _showPackagesSection.asStateFlow()

    fun setPlayerMode(mode: String) {
        _playerMode.value = mode
        prefs.edit().putString("saved_player_type", mode).apply()
    }

    // Auto landscape setting: saves state and triggers horizontal orientation immediately
    private val _autoLandscape = MutableStateFlow(prefs.getBoolean("saved_auto_landscape", false))
    val autoLandscape: StateFlow<Boolean> = _autoLandscape.asStateFlow()

    fun setAutoLandscape(enabled: Boolean) {
        _autoLandscape.value = enabled
        prefs.edit().putBoolean("saved_auto_landscape", enabled).apply()
    }

    // Theme preferences: "red", "gold", "purple", "emerald", "blue"
    private val _selectedTheme = MutableStateFlow(prefs.getString("saved_theme", "red") ?: "red")
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    fun setTheme(theme: String) {
        _selectedTheme.value = theme
        prefs.edit().putString("saved_theme", theme).apply()
    }

    // Dark / Light Mode preference
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("saved_is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("saved_is_dark_mode", enabled).apply()
        viewModelScope.launch {
            try {
                FirebaseService.saveAppConfig("isDarkMode", enabled)
            } catch (e: Exception) {
                // Ignore network error
            }
        }
    }

    // Grid Column Count: 2, 3, or 4
    private val _cardColumnsCount = MutableStateFlow(prefs.getInt("saved_card_columns", 2))
    val cardColumnsCount: StateFlow<Int> = _cardColumnsCount.asStateFlow()

    fun setCardColumnsCount(count: Int) {
        _cardColumnsCount.value = count
        prefs.edit().putInt("saved_card_columns", count).apply()
    }

    // Admin Panel Info
    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _adminCodesList = MutableStateFlow<List<AccessCode>>(emptyList())
    val adminCodesList: StateFlow<List<AccessCode>> = _adminCodesList.asStateFlow()

    private val _adminActionStatus = MutableStateFlow<String?>(null)
    val adminActionStatus: StateFlow<String?> = _adminActionStatus.asStateFlow()

    private val _showAiSectionInDrawer = MutableStateFlow(true)
    val showAiSectionInDrawer: StateFlow<Boolean> = _showAiSectionInDrawer.asStateFlow()

    private val _showChatTab = MutableStateFlow(true)
    val showChatTab: StateFlow<Boolean> = _showChatTab.asStateFlow()

    private val _eventsVisibilityMode = MutableStateFlow("ALL")
    val eventsVisibilityMode: StateFlow<String> = _eventsVisibilityMode.asStateFlow()

    private val _showEventsTab = MutableStateFlow(true)
    val showEventsTab: StateFlow<Boolean> = combine(_eventsVisibilityMode, _activeAccount, _showEventsTab) { mode, account, legacyShow ->
        if (!legacyShow || mode == "HIDDEN") {
            false
        } else if (mode == "VIP_ONLY") {
            val isVip = account?.name?.lowercase()?.let { it.contains("vip") || it.contains("+90") } ?: false
            val isVipExpiry = account?.expiryDate?.lowercase()?.contains("vip") ?: false
            isVip || isVipExpiry
        } else {
            true
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _showXtreamDirectLogin = MutableStateFlow(prefs.getBoolean("show_xtream_direct_login", true))
    val showXtreamDirectLogin: StateFlow<Boolean> = _showXtreamDirectLogin.asStateFlow()

    fun setShowXtreamDirectLogin(show: Boolean) {
        _showXtreamDirectLogin.value = show
        prefs.edit().putBoolean("show_xtream_direct_login", show).apply()
        viewModelScope.launch {
            FirebaseService.saveAppConfig("showXtreamDirectLogin", show)
        }
    }

    // Reels States
    private val _showReelsSection = MutableStateFlow(true)
    val showReelsSection: StateFlow<Boolean> = _showReelsSection.asStateFlow()

    private val _reelsList = MutableStateFlow<List<Reel>>(emptyList())
    val reelsList: StateFlow<List<Reel>> = _reelsList.asStateFlow()

    private val _isReelsLoading = MutableStateFlow(false)
    val isReelsLoading: StateFlow<Boolean> = _isReelsLoading.asStateFlow()

    private val _reelsSettings = MutableStateFlow(ReelsSettings())
    val reelsSettings: StateFlow<ReelsSettings> = _reelsSettings.asStateFlow()

    private val _currentReelComments = MutableStateFlow<List<ReelComment>>(emptyList())
    val currentReelComments: StateFlow<List<ReelComment>> = _currentReelComments.asStateFlow()

    private val _followingUsers = MutableStateFlow<Set<String>>(emptySet())
    val followingUsers: StateFlow<Set<String>> = _followingUsers.asStateFlow()

    // Secondary Realtime Database Chat States
    private val _isChatEnabledOnline = MutableStateFlow(true)
    val isChatEnabledOnline: StateFlow<Boolean> = _isChatEnabledOnline.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Chat Username and Display Name
    private val _chatUsername = MutableStateFlow<String?>(null)
    val chatUsername: StateFlow<String?> = _chatUsername.asStateFlow()

    

    fun loadNews() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isNewsLoading.value = true
                val url = URL("https://news-ar.ysscores.com/api/News/all_news_page")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.getInputStream().bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val dataObj = jsonResponse.optJSONObject("data")
                    
                    val parsedNews = mutableListOf<NewsItem>()
                    if (dataObj != null) {
                        // 1. Parse main news object if exists
                        val mainObj = dataObj.optJSONObject("main")
                        if (mainObj != null) {
                            val item = parseNewsItemJson(mainObj)
                            if (item != null) {
                                parsedNews.add(item)
                            }
                        }
                        
                        // 2. Parse important news array
                        val importantArray = dataObj.optJSONArray("important")
                        if (importantArray != null) {
                            for (i in 0 until importantArray.length()) {
                                val obj = importantArray.optJSONObject(i) ?: continue
                                val item = parseNewsItemJson(obj)
                                if (item != null) {
                                    parsedNews.add(item)
                                }
                            }
                        }

                        // 3. Parse last_news array
                        val lastNewsArray = dataObj.optJSONArray("last_news")
                        if (lastNewsArray != null) {
                            for (i in 0 until lastNewsArray.length()) {
                                val obj = lastNewsArray.optJSONObject(i) ?: continue
                                val item = parseNewsItemJson(obj)
                                if (item != null) {
                                    parsedNews.add(item)
                                }
                            }
                        }
                    }
                    _newsItems.value = parsedNews.distinctBy { it.id }
                } else {
                    Log.e("MainViewModel", "Failed to fetch news, response code: $code")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching news: ${e.message}", e)
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    private fun parseNewsItemJson(item: JSONObject): NewsItem? {
        try {
            val id = item.optLong("id", 0)
            if (id == 0L) return null
            val title = item.optString("title", "")
            val desc = item.optString("news_desc", "")
            val image = item.optString("image", "")
            val createdAt = item.optJSONObject("created_at")
            val dateStr = createdAt?.optString("date", "") ?: ""
            
            val fullImageUrl = if (image.isNotBlank()) {
                if (image.startsWith("http")) image else "https://imgs.ysscores.com/news/$image"
            } else {
                ""
            }
            
            return NewsItem(
                id = id,
                title = title,
                description = desc,
                imageUrl = fullImageUrl,
                dateStr = dateStr
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun finishContentUpdate() {
        _isContentUpdating.value = false
    }

    private val _chatUnreadUsersCount = MutableStateFlow(0)
    val chatUnreadUsersCount: StateFlow<Int> = _chatUnreadUsersCount.asStateFlow()

    fun updateChatUnreadUsersCount(count: Int) {
        _chatUnreadUsersCount.value = count
    }

    private var configMonitorJob: Job? = null

    private fun startConfigMonitor(code: String) {
        configMonitorJob?.cancel()
        configMonitorJob = viewModelScope.launch {
            while (true) {
                delay(15000L) // Check every 15 seconds for changes
                try {
                    val result = FirebaseService.verifyActivationCode(code)
                    if (result != null) {
                        val newHost: String
                        val newUser: String
                        val newPass: String
                        
                        if (result is VipCode) {
                            newHost = result.host
                            newUser = result.username
                            newPass = result.password
                        } else if (result is ActivationCode) {
                            newHost = result.host
                            newUser = result.username
                            newPass = result.password
                        } else {
                            continue
                        }
                        
                        val currentAccount = _activeAccount.value
                        if (currentAccount != null && 
                            (currentAccount.host != newHost || 
                             currentAccount.username != newUser || 
                             currentAccount.password != newPass)) {
                            _codeUpdateRequiredMessage.value = "توفرت بيانات جديدة من المطور، يرجى إعادة إدخال الكود"
                        }
                    }
                } catch (e: Exception) {
                    // Silent fail for monitor
                }
            }
        }
    }

    fun applyCodeUpdate() {
        logout()
        _codeUpdateRequiredMessage.value = null
    }

    fun clearCodeUpdateRequiredMessage() {
        _codeUpdateRequiredMessage.value = null
    }

    private fun checkCodeCredentialChanges(code: String, host: String, user: String, pass: String) {
        val lastHost = prefs.getString("last_host_$code", null)
        val lastUser = prefs.getString("last_username_$code", null)
        val lastPass = prefs.getString("last_password_$code", null)

        if (lastHost != null && (lastHost != host || lastUser != user || lastPass != pass)) {
            _codeUpdateRequiredMessage.value = "توفرت بيانات جديدة من المطور، يرجى إعادة إدخال الكود"
        }

        prefs.edit()
            .putString("last_host_$code", host)
            .putString("last_username_$code", user)
            .putString("last_password_$code", pass)
            .apply()
            
        // Start real-time monitoring for this code
        startConfigMonitor(code)
    }

    private val _chatName = MutableStateFlow<String?>(null)
    val chatName: StateFlow<String?> = _chatName.asStateFlow()

    private val _chatAvatar = MutableStateFlow<String>("neon_wolf")
    val chatAvatar: StateFlow<String> = _chatAvatar.asStateFlow()

    fun getChatPassword(): String = prefs.getString("chat_password", "") ?: ""

    private val _userProfiles = MutableStateFlow<Map<String, ChatUserProfile>>(emptyMap())
    val userProfiles: StateFlow<Map<String, ChatUserProfile>> = _userProfiles.asStateFlow()

    private val _allUsers = MutableStateFlow<Map<String, String>>(emptyMap())
    val allUsers: StateFlow<Map<String, String>> = _allUsers.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<String>>(emptyList())
    val blockedUsers: StateFlow<List<String>> = _blockedUsers.asStateFlow()

    private val _messagedUsers = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val messagedUsers: StateFlow<List<Pair<String, String>>> = _messagedUsers.asStateFlow()

    init {
        _chatUsername.value = prefs.getString("chat_username", null)
        _chatName.value = prefs.getString("chat_name", null)
        _chatAvatar.value = prefs.getString("chat_avatar", "neon_wolf") ?: "neon_wolf"

        // Periodic check for messaged users and blocked list (continuous update)
        viewModelScope.launch {
            while (true) {
                if (!_chatUsername.value.isNullOrBlank()) {
                    fetchMessagedUsers()
                }
                delay(3000L)
            }
        }

        // Periodic check for secondary DB chat enablement online
        viewModelScope.launch {
            while (true) {
                try {
                    _isChatEnabledOnline.value = ChatService.isChatEnabled()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to check chat enabled", e)
                }
                delay(10000L)
            }
        }

        // Periodic check for Chat, Main Channels, Notification Icon and Share button online state from RTDB
        viewModelScope.launch {
            while (true) {
                try {
                    val config = FirebaseService.getAppConfigMap()
                    if (config != null) {
                        _showChatTab.value = (config["showChatTab"] as? Boolean) ?: true
                        val evMode = config["eventsVisibilityMode"] as? String
                        if (evMode != null) {
                            _eventsVisibilityMode.value = evMode
                        } else {
                            val legacyShow = (config["showEventsTab"] as? Boolean) ?: true
                            _eventsVisibilityMode.value = if (legacyShow) "ALL" else "HIDDEN"
                        }
                        _showEventsTab.value = (config["showEventsTab"] as? Boolean) ?: true
                        _showXtreamDirectLogin.value = (config["showXtreamDirectLogin"] as? Boolean) ?: prefs.getBoolean("show_xtream_direct_login", true)
                        _showNotificationIcon.value = (config["showNotificationIcon"] as? Boolean) ?: true
                        _isShareButtonVisible.value = (config["is_share_enabled"] as? Boolean) ?: true
                        _showPackagesSection.value = (config["showPackagesSection"] as? Boolean) ?: true
                        _showAiSectionInDrawer.value = (config["showAiSectionInDrawer"] as? Boolean) ?: true
                        _showReelsSection.value = (config["showReelsSection"] as? Boolean) ?: true
                        _isVpnBlockEnabled.value = (config["isVpnBlockEnabled"] as? Boolean) ?: prefs.getBoolean("is_vpn_block_enabled", true)
                    }

                    // Periodic sync of Home and Online Users visibility from Cloud RTDB
                    val homeCfg = FirebaseService.getHomeConfig()
                    _homeConfig.value = homeCfg
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Periodic app_config check notice: ${e.message}")
                }
                delay(8000L)
            }
        }

        // Periodic sync for Reels Settings and Reels List
        viewModelScope.launch {
            while (true) {
                try {
                    val settings = ReelsService.fetchSettings()
                    _reelsSettings.value = settings
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to load Reels Settings", e)
                }
                delay(15000L)
            }
        }

        viewModelScope.launch {
            while (true) {
                try {
                    loadReels()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to load Reels periodically", e)
                }
                delay(30000L) // every 30s reload feed
            }
        }

        // Background loop for Realtime Direct Messages and Replies Notifications (simulating FCM)
        viewModelScope.launch {
            delay(5000L)
            while (true) {
                try {
                    val myUser = _chatUsername.value
                    if (!myUser.isNullOrBlank()) {
                        val allChats = ChatService.getAllChats()
                        if (allChats != null) {
                            for ((chanId, messagesMap) in allChats) {
                                if (chanId.startsWith("private_")) {
                                    val parts = chanId.split("_")
                                    if (parts.size >= 3) {
                                        val userA = parts[1]
                                        val userB = parts[2]
                                        if (userA == myUser || userB == myUser) {
                                            for ((msgId, msgData) in messagesMap) {
                                                if (msgData is Map<*, *>) {
                                                    val sender = msgData["sender"] as? String ?: ""
                                                    val text = msgData["text"] as? String ?: ""
                                                    val timestamp = (msgData["timestamp"] as? Number)?.toLong() ?: 0L

                                                    if (sender != myUser && sender.isNotBlank()) {
                                                        if (!notifiedMessageIds.contains(msgId)) {
                                                            val isFresh = (System.currentTimeMillis() - timestamp) < 180_000L // 3 minutes
                                                            if (isFresh) {
                                                                showLocalNotification(sender, text, chanId)
                                                            }
                                                            notifiedMessageIds.add(msgId)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error in private notification loop", e)
                }
                delay(4000L)
            }
        }

        // Automatically evaluate AppVersionGate on startup, seed data if allowed, and recover active code
        viewModelScope.launch {
            _isLoading.value = true

            // 1. First priority: Remote Version and Operational Gate check
            try {
                val config = FirebaseService.getAppVersionConfig()
                appVersionConfig.value = config
                val status = AppVersionManager.evaluate(config, AppVersionManager.getEffectiveAppVersion())
                _appVersionStatus.value = status
                if (status is AppVersionStatus.Maintenance || status is AppVersionStatus.UpdateRequired) {
                    _isLoading.value = false
                    Log.w("MainViewModel", "Startup blocked by AppVersionGate: status=$status")
                    return@launch
                }
            } catch (t: Throwable) {
                Log.w("MainViewModel", "Notice on startup version verification: ${t.message}")
            }

            loadEventChannels()
            loadNews()
            try {
                val savedCode = prefs.getString("saved_access_code", null)
                if (savedCode == "DIRECT_XTREAM") {
                    val h = prefs.getString("saved_direct_host", "") ?: ""
                    val u = prefs.getString("saved_direct_user", "") ?: ""
                    val p = prefs.getString("saved_direct_pass", "") ?: ""
                    val f = prefs.getString("saved_direct_format", "") ?: ""
                    if (h.isNotEmpty() && u.isNotEmpty() && p.isNotEmpty()) {
                        loginWithXtreamDirect(h, u, p, f)
                    } else {
                        _isLoading.value = false
                    }
                } else if (savedCode != null) {
                    loginWithCode(savedCode)
                } else {
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                Log.e("MainViewModel", "Error restoring saved code", t)
                _isLoading.value = false
            }
        }
    }

    private fun getSessionId(): String {
        var sid = prefs.getString("vip_session_id", null)
        if (sid == null) {
            sid = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("vip_session_id", sid).apply()
        }
        return sid
    }

    private fun startVipHeartbeat(code: String) {
        val sessionId = getSessionId()
        vipHeartbeatJob?.cancel()
        vipHeartbeatJob = viewModelScope.launch {
            while (isActive) {
                FirebaseService.updateVipCodeLastActive(code, sessionId)
                delay(30000)
            }
        }
    }

    fun loadEventChannels() {
        viewModelScope.launch {
            try {
                val fresh = FirebaseService.getEventChannels()
                _events.value = fresh
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading main channels", e)
            }
        }
        loadYssMatches()
        loadEspnMatches()
    }

    fun loadEspnMatches(forceRefresh: Boolean = false, date: String? = null, leagueId: String? = null) {
        val targetDate = date ?: _selectedEspnDate.value
        val targetLeague = leagueId ?: _selectedEspnLeague.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isEspnMatchesLoading.value = true
                val leaguesToFetch = if (targetLeague == "all") {
                    listOf("esp.1", "eng.1", "uefa.champions", "ita.1", "ger.1", "fra.1", "ksa.1", "afc.champions")
                } else {
                    listOf(targetLeague)
                }

                val matches = EspnSoccerService.fetchAllScoreboards(leaguesToFetch, targetDate)
                _espnMatches.value = matches
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching ESPN matches", e)
            } finally {
                _isEspnMatchesLoading.value = false
            }
        }
    }

    private val _selectedEspnStandings = MutableStateFlow<List<com.example.data.EspnStandingEntry>>(emptyList())
    val selectedEspnStandings: StateFlow<List<com.example.data.EspnStandingEntry>> = _selectedEspnStandings.asStateFlow()

    private val _isEspnStandingsLoading = MutableStateFlow(false)
    val isEspnStandingsLoading: StateFlow<Boolean> = _isEspnStandingsLoading.asStateFlow()

    private val _ysNews = MutableStateFlow<List<com.example.data.YsNewsItem>>(emptyList())
    val ysNews: StateFlow<List<com.example.data.YsNewsItem>> = _ysNews.asStateFlow()

    private val _isYsNewsLoading = MutableStateFlow(false)
    val isYsNewsLoading: StateFlow<Boolean> = _isYsNewsLoading.asStateFlow()

    private val _subscribedMatches = MutableStateFlow<Set<String>>(
        prefs.getStringSet("subscribed_matches_set", emptySet()) ?: emptySet()
    )
    val subscribedMatches: StateFlow<Set<String>> = _subscribedMatches.asStateFlow()

    fun toggleMatchSubscription(matchId: String) {
        val current = _subscribedMatches.value.toMutableSet()
        val isAdded: Boolean
        if (current.contains(matchId)) {
            current.remove(matchId)
            isAdded = false
        } else {
            current.add(matchId)
            isAdded = true
        }
        _subscribedMatches.value = current
        prefs.edit().putStringSet("subscribed_matches_set", current).apply()

        viewModelScope.launch {
            val m = _selectedEspnMatch.value ?: _espnMatches.value.find { it.id == matchId }
            if (m != null) {
                if (isAdded) {
                    com.example.utils.NotificationHelper.showSystemNotification(
                        getApplication(),
                        "🔔 تم تفعيل التنبيهات",
                        "سنقوم بتنبيهك عند بدء مباراة ${m.homeTeam.arabicName} ضد ${m.awayTeam.arabicName} وعند تسجيل أي هدف!"
                    )
                } else {
                    com.example.utils.NotificationHelper.showSystemNotification(
                        getApplication(),
                        "🔕 تم إيقاف التنبيهات",
                        "تم إلغاء تفعيل تنبيهات مباراة ${m.homeTeam.arabicName} ضد ${m.awayTeam.arabicName}."
                    )
                }
            }
        }
    }

    fun selectEspnMatch(match: EspnMatch) {
        _selectedEspnMatch.value = match
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isEspnSummaryLoading.value = true
                _selectedEspnSummary.value = null
                val summary = EspnSoccerService.fetchMatchSummary(match.leagueId, match.id)
                _selectedEspnSummary.value = summary
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching match summary", e)
            } finally {
                _isEspnSummaryLoading.value = false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isEspnStandingsLoading.value = true
                _selectedEspnStandings.value = emptyList()
                val standings = EspnSoccerService.fetchLeagueStandings(match.leagueId)
                _selectedEspnStandings.value = standings
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching standings", e)
            } finally {
                _isEspnStandingsLoading.value = false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isYsNewsLoading.value = true
                _ysNews.value = emptyList()
                val news = EspnSoccerService.fetchYsNews()
                _ysNews.value = news
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching news", e)
            } finally {
                _isYsNewsLoading.value = false
            }
        }
    }

    fun clearSelectedEspnMatch() {
        _selectedEspnMatch.value = null
        _selectedEspnSummary.value = null
        _selectedEspnStandings.value = emptyList()
        _ysNews.value = emptyList()
    }

    fun setEspnDate(dateYYYYMMDD: String) {
        _selectedEspnDate.value = dateYYYYMMDD
        loadEspnMatches(date = dateYYYYMMDD)
    }

    fun setEspnLeague(leagueId: String) {
        _selectedEspnLeague.value = leagueId
        loadEspnMatches(leagueId = leagueId)
    }

    fun toggleFavoriteTeam(teamName: String) {
        val current = _favoriteTeamNames.value.toMutableSet()
        if (current.contains(teamName)) {
            current.remove(teamName)
        } else {
            current.add(teamName)
        }
        _favoriteTeamNames.value = current
        prefs.edit().putStringSet("favorite_teams_set", current).apply()
    }

    fun loadYssMatches(forceRefresh: Boolean = false, targetDate: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isMatchesLoading.value = true
                
                // 1. Fetch real 100% football match data from API-Sports
                val apiMatches = FootballApiService.getTodayMatches(forceRefresh = forceRefresh, targetDate = targetDate)
                
                // 2. Filter matches to only show Spanish League, English Premier League, and UEFA Champions League
                val allowedLeagues = listOf("إسباني", "إنجليزي", "أبطال أوروبا")
                _yssMatches.value = apiMatches.filter { match ->
                    allowedLeagues.any { match.leagueName.contains(it, ignoreCase = true) }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching football matches", e)
            } finally {
                _isMatchesLoading.value = false
            }
        }
    }

    private fun parseYssMatch(obj: JSONObject, league: League): YssMatch {
        val homeObj = obj.optJSONObject("home_team")
        val awayObj = obj.optJSONObject("away_team")
        val homeTeamName = homeObj?.optString("name", "") ?: ""
        val awayTeamName = awayObj?.optString("name", "") ?: ""
        val homeLogo = homeObj?.optString("logo", "") ?: ""
        val awayLogo = awayObj?.optString("logo", "") ?: ""
        val statusStr = obj.optString("match_status", "")
        val isLive = statusStr.contains("مباشر") || obj.optInt("is_live", 0) == 1
        val minuteStr = obj.optString("match_time", "")
        val timeStr = obj.optString("time", "")
        
        return YssMatch(
            id = obj.optString("id", ""),
            homeTeam = Team(homeTeamName, homeLogo),
            awayTeam = Team(awayTeamName, awayLogo),
            team1 = homeTeamName,
            team2 = awayTeamName,
            team1Logo = homeLogo,
            team2Logo = awayLogo,
            homeScore = obj.optString("home_score", "0"),
            awayScore = obj.optString("away_score", "0"),
            score1 = obj.optString("home_score", "0").toIntOrNull(),
            score2 = obj.optString("away_score", "0").toIntOrNull(),
            status = if (isLive) MatchStatus.LIVE else MatchStatus.UPCOMING,
            statusStr = statusStr,
            minute = minuteStr.filter { it.isDigit() }.toIntOrNull(),
            minuteStr = minuteStr,
            time = timeStr,
            timeFormatted = timeStr,
            league = league,
            leagueName = league.name,
            leagueLogo = league.badge,
            isLive = isLive
        )
    }

    fun loadYssMatchDetails(matchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isMatchDetailsLoading.value = true
                _selectedMatchDetails.value = null
                
                // 1. Try real details from API-Sports (Timeline events, Lineups, Stats)
                val apiDetails = FootballApiService.getMatchDetails(matchId)
                if (apiDetails != null) {
                    _selectedMatchDetails.value = apiDetails
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching match details", e)
            } finally {
                _isMatchDetailsLoading.value = false
            }
        }
    }

    /**
     * Logic for logging in with an IPTV Access code (e.g., A1).
     */
    fun loginWithCode(code: String) {
        viewModelScope.launch {
            if (!isAppAccessAllowed()) {
                _errorMessage.value = "التطبيق متوقف مؤقتاً للصيانة أو يتطلب تحديثاً إلزامياً."
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            _errorMessage.value = null
            try {
                // 1. Try dynamic activation_codes / VIP codes first
                val result = FirebaseService.verifyActivationCode(code)
                if (result != null) {
                    if (result is VipCode) {
                        // Check user limit with presence
                        val now = System.currentTimeMillis()
                        val sessionId = getSessionId()
                        val isPresent = (now - result.lastActive) < 60000 // Active within last 60 seconds
                        val isSameSession = result.lastActiveSessionId == sessionId
                        
                        if (result.currentUsers >= result.maxUsers && isPresent && !isSameSession) {
                            _errorMessage.value = "الكود الخاص بك مستخدم من قبل أشخاص آخرين، هذه مسؤوليتك، ربما قمت بنشره أو فعلت شيئاً آخر. تواصل مع المطورين على تليجرام للتأكد من ذلك."
                            _isLoading.value = false
                            return@launch
                        }

                        // If same session, we don't increment, just ensure heartbeat starts
                        if (isSameSession) {
                            FirebaseService.updateVipCodeLastActive(result.code, sessionId)
                        } else {
                            // If not present but count is high, reset count to 1 because clearly they left
                            if (result.currentUsers >= result.maxUsers && !isPresent) {
                                 FirebaseService.setVipCodeUserCount(result.code, 1, sessionId)
                            } else {
                                 FirebaseService.updateVipCodeUserCount(result.code, 1)
                                 FirebaseService.updateVipCodeLastActive(result.code, sessionId)
                            }
                        }
                        
                        startVipHeartbeat(result.code)
                        
                        val accountResult = IPTVAccount(
                            id = result.code,
                            name = "VIP Account",
                            allowedTypes = result.allowedTypes,
                            expiryDate = "مفتوح VIP",
                            host = result.host,
                            username = result.username,
                            password = result.password,
                            forcedFormat = result.forcedFormat,
                            allowVpn = result.allowVpn
                        )
                        checkCodeCredentialChanges(result.code, result.host, result.username, result.password)
                        _accessCode.value = result.code
                        _activeAccount.value = accountResult
                        prefs.edit().putString("saved_access_code", result.code).apply()
                        loadIPTVContent(emptyList())
                    } else if (result is ActivationCode) {
                        val accountResult = IPTVAccount(
                            id = result.code,
                            name = if (result.m3uUrl.isNotEmpty()) "اشتراك M3U المفعّل" else "حساب Xtream المفعّل",
                            allowedCategories = emptyList(),
                            allowedTypes = result.allowedTypes,
                            expiryDate = "مفتوح تلقائياً",
                            host = result.host,
                            username = result.username,
                            password = result.password,
                            forcedFormat = result.forcedFormat,
                            m3uUrl = result.m3uUrl
                        )
                        checkCodeCredentialChanges(result.code, result.host, result.username, result.password)
                        _accessCode.value = result.code
                        _activeAccount.value = accountResult
                        // Save to Shared Preferences
                        prefs.edit().putString("saved_access_code", result.code).apply()
 
                        // Load content dynamically via Xtream API
                        loadIPTVContent(emptyList())
                    }
                } else {
                    // 2. Fall back to old access_codes
                    val codeResult = FirebaseService.verifyAccessCode(code)
                    if (codeResult != null && codeResult.isActive) {
                        val accountResult = FirebaseService.getAccount(codeResult.accountId)
                        if (accountResult != null) {
                            checkCodeCredentialChanges(codeResult.code, accountResult.host, accountResult.username, accountResult.password)
                            _accessCode.value = codeResult.code
                            _activeAccount.value = accountResult
                            prefs.edit().putString("saved_access_code", codeResult.code).apply()
                            loadIPTVContent(accountResult.allowedCategories)
                        } else {
                            _errorMessage.value = "الكود صحيح ولكن الحساب المرتبط به غير موجود!"
                        }
                    } else {
                        _errorMessage.value = "كود التفعيل غير صحيح أو تم إلغاء تنشيطه!"
                    }
                }
            } catch (e: Throwable) {
                _errorMessage.value = "حدث خطأ أثناء فحص الكود: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Direct login with Xtream credentials (Host, Username, Password, Forced format)
     */
    fun loginWithXtreamDirect(hostInput: String, usernameInput: String, passwordInput: String, forcedFormat: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                var formattedHost = hostInput.trim()
                if (!formattedHost.startsWith("http://") && !formattedHost.startsWith("https://")) {
                    formattedHost = "http://$formattedHost"
                }
                while (formattedHost.endsWith("/")) {
                    formattedHost = formattedHost.substring(0, formattedHost.length - 1)
                }
                val u = usernameInput.trim()
                val p = passwordInput.trim()

                if (formattedHost.isEmpty() || u.isEmpty() || p.isEmpty()) {
                    _errorMessage.value = "يرجى إدخال عنوان السيرفر واسم المستخدم وكلمة المرور كاملة!"
                    _isLoading.value = false
                    return@launch
                }

                // Verify credentials via API
                val isValid = FirebaseService.verifyXtreamCredentials(formattedHost, u, p)
                if (isValid) {
                    val accountResult = IPTVAccount(
                        id = "xtream_${System.currentTimeMillis()}",
                        name = "حساب Xtream المباشر",
                        allowedCategories = emptyList(),
                        allowedTypes = listOf("live", "movies", "series"),
                        expiryDate = "مباشر",
                        host = formattedHost,
                        username = u,
                        password = p,
                        forcedFormat = forcedFormat
                    )
                    _accessCode.value = "DIRECT_XTREAM"
                    _activeAccount.value = accountResult

                    prefs.edit()
                        .putString("saved_access_code", "DIRECT_XTREAM")
                        .putString("saved_direct_host", formattedHost)
                        .putString("saved_direct_user", u)
                        .putString("saved_direct_pass", p)
                        .putString("saved_direct_format", forcedFormat)
                        .apply()

                    loadIPTVContent(emptyList())
                } else {
                    _errorMessage.value = "تعذر الاتصال بسيرفر Xtream! يرجى التحقق من صحة رابط السيرفر وبيانات الدخول."
                }
            } catch (e: Throwable) {
                _errorMessage.value = "خطأ في الاتصال بالسيرفر: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logout and delete local cache.
     */
    fun logout() {
        val currentCode = _accessCode.value
        if (currentCode != null) {
            vipHeartbeatJob?.cancel()
            vipHeartbeatJob = null
            viewModelScope.launch {
                FirebaseService.updateVipCodeUserCount(currentCode, -1)
            }
        }

        configMonitorJob?.cancel()
        configMonitorJob = null

        val editor = prefs.edit()
        editor.remove("saved_access_code")
            .remove("saved_direct_host")
            .remove("saved_direct_user")
            .remove("saved_direct_pass")
            .remove("saved_direct_format")

        val allKeys = prefs.all.keys
        for (k in allKeys) {
            if (k.startsWith("cache_") ||
                k.startsWith("last_host_") ||
                k.startsWith("last_username_") ||
                k.startsWith("last_password_")) {
                editor.remove(k)
            }
        }
        editor.apply()

        FirebaseService.clearCache()
        _accessCode.value = null
        _activeAccount.value = null
        _channels.value = emptyList()
        _movies.value = emptyList()
        _series.value = emptyList()
        _categories.value = emptyList()
        _selectedCategory.value = "all"
        _selectedMovie.value = null
        _selectedSeries.value = null
        _playingStreamUrl.value = null
        _searchQuery.value = ""
        _isAdminAuthenticated.value = false
        _codeUpdateRequiredMessage.value = null
        _isContentUpdating.value = false
    }

    /**
     * Set selected video category.
     */
    fun selectCategory(categoryId: String) {
        _selectedCategory.value = categoryId
    }

    /**
     * Toggle item in favorites.
     */
    fun toggleFavorite(itemId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("saved_favorites", current).apply()
    }

    /**
     * Set movie to continue watching queue.
     */
    fun addToContinueWatching(movie: Movie) {
        val currentList = _continueWatching.value.toMutableList()
        if (currentList.none { it.id == movie.id }) {
            currentList.add(0, movie)
            if (currentList.size > 5) currentList.removeAt(5)
            _continueWatching.value = currentList
        }
    }

    /**
     * Update search query.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Load authenticated IPTV Account's assigned contents at maximum speed.
     */
    private suspend fun loadIPTVContent(allowedCategories: List<String>) = coroutineScope {
        _isLoading.value = true
        _isChannelsLoading.value = true
        _isMoviesLoading.value = true
        _isSeriesLoading.value = true

        try {
            val account = _activeAccount.value
            val m3uLink = if (account != null) {
                when {
                    account.m3uUrl.isNotBlank() -> account.m3uUrl.trim()
                    account.host.contains(".m3u", ignoreCase = true) -> account.host.trim()
                    account.host.contains("get.php", ignoreCase = true) -> account.host.trim()
                    else -> ""
                }
            } else ""

            if (m3uLink.isNotEmpty()) {
                Log.d("MainViewModel", "Loading channels from direct M3U link: $m3uLink")
                try {
                    val forcedFormat = account?.forcedFormat ?: "ts"
                    val (cats, chans) = FirebaseService.fetchChannelsAndCategoriesFromM3uUrl(m3uLink, forcedFormat)
                    _categories.value = cats
                    _channels.value = chans
                } catch (e: Throwable) {
                    Log.e("MainViewModel", "Error loading M3U content", e)
                } finally {
                    _isChannelsLoading.value = false
                    _isLoading.value = false
                    _isMoviesLoading.value = false
                    _isSeriesLoading.value = false
                }
                return@coroutineScope
            }

            if (account != null && account.host.isNotEmpty()) {
                val host = account.host
                val username = account.username
                val password = account.password

                // Fetch Categories, Channels, Movies, and Series ALL fully in parallel at maximum speed
                val categoriesDeferred = async(Dispatchers.IO) {
                    try {
                        val cats = FirebaseService.fetchXtreamCategories(host, username, password)
                        _categories.value = cats
                        cats
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading categories", e)
                        emptyList<IPTVCategory>()
                    }
                }

                val channelsDeferred = async(Dispatchers.IO) {
                    try {
                        val forcedFormat = account.forcedFormat
                        val chans = FirebaseService.fetchXtreamChannels(host, username, password, forcedFormat)
                        _channels.value = chans
                        chans
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading channels", e)
                        emptyList<Channel>()
                    } finally {
                        _isChannelsLoading.value = false
                        _isLoading.value = false
                    }
                }

                val moviesDeferred = async(Dispatchers.IO) {
                    try {
                        val movs = FirebaseService.fetchXtreamMovies(host, username, password)
                        _movies.value = movs
                        movs
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading movies", e)
                        emptyList<Movie>()
                    } finally {
                        _isMoviesLoading.value = false
                    }
                }

                val seriesDeferred = async(Dispatchers.IO) {
                    try {
                        val sers = FirebaseService.fetchXtreamSeries(host, username, password)
                        _series.value = sers
                        sers
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading series", e)
                        emptyList<Series>()
                    } finally {
                        _isSeriesLoading.value = false
                    }
                }

                // Await all parallel jobs
                categoriesDeferred.await()
                channelsDeferred.await()
                moviesDeferred.await()
                seriesDeferred.await()

                // Dynamically organize categories if missing from server response
                enrichCategoriesFromContent()
            } else {
                // Parallel loaders for direct Firebase / local channels
                val categoriesDeferred = async(Dispatchers.IO) {
                    try {
                        val categoriesJob = FirebaseService.getCategories()
                        val filteredCats = categoriesJob.filter { cat ->
                            allowedCategories.isEmpty() || allowedCategories.contains(cat.id)
                        }
                        _categories.value = filteredCats
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading categories", e)
                    }
                }

                val channelsDeferred = async(Dispatchers.IO) {
                    try {
                        val chans = FirebaseService.getChannels(allowedCategories)
                        _channels.value = chans
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading channels", e)
                    } finally {
                        _isChannelsLoading.value = false
                        _isLoading.value = false
                    }
                }

                val moviesDeferred = async(Dispatchers.IO) {
                    try {
                        val movs = FirebaseService.getMovies(allowedCategories)
                        _movies.value = movs
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading movies", e)
                    } finally {
                        _isMoviesLoading.value = false
                    }
                }

                val seriesDeferred = async(Dispatchers.IO) {
                    try {
                        val sers = FirebaseService.getSeries(allowedCategories)
                        _series.value = sers
                    } catch (e: Throwable) {
                        Log.e("MainViewModel", "Error loading series", e)
                    } finally {
                        _isSeriesLoading.value = false
                    }
                }

                categoriesDeferred.await()
                channelsDeferred.await()
                moviesDeferred.await()
                seriesDeferred.await()

                enrichCategoriesFromContent()
            }
        } catch (e: Throwable) {
            Log.e("MainViewModel", "Error loading IPTV content", e)
        } finally {
            _isLoading.value = false
            _isChannelsLoading.value = false
            _isMoviesLoading.value = false
            _isSeriesLoading.value = false
        }
    }

    private fun enrichCategoriesFromContent() {
        try {
            val existing = _categories.value.toMutableList()
            val existingIds = existing.map { it.id.trim() }.toSet()
            val newCategories = mutableListOf<IPTVCategory>()

            val channelCats = _channels.value
                .map { it.category.trim() }
                .filter { it.isNotBlank() && !existingIds.contains(it) && it != "all" }
                .distinct()
                .map { IPTVCategory(id = it, name = it, type = "channel") }
            newCategories.addAll(channelCats)

            val movieCats = _movies.value
                .map { it.category.trim() }
                .filter { it.isNotBlank() && !existingIds.contains(it) && it != "all" && !channelCats.any { c -> c.id == it } }
                .distinct()
                .map { IPTVCategory(id = it, name = it, type = "movie") }
            newCategories.addAll(movieCats)

            val seriesCats = _series.value
                .map { it.category.trim() }
                .filter { it.isNotBlank() && !existingIds.contains(it) && it != "all" && !channelCats.any { c -> c.id == it } && !movieCats.any { m -> m.id == it } }
                .distinct()
                .map { IPTVCategory(id = it, name = it, type = "series") }
            newCategories.addAll(seriesCats)

            if (newCategories.isNotEmpty()) {
                _categories.value = existing + newCategories
            }
        } catch (t: Throwable) {
            Log.w("MainViewModel", "Category enrichment notice: ${t.message}")
        }
    }

    /**
     * Load movies on demand to guarantee maximum speed and split queries
     */
    fun loadMoviesOnDemand() {
        val account = _activeAccount.value ?: return
        if (_movies.value.isNotEmpty() || _isMoviesLoading.value) return

        viewModelScope.launch {
            _isMoviesLoading.value = true
            try {
                if (account.host.isNotEmpty()) {
                    _movies.value = FirebaseService.fetchXtreamMovies(account.host, account.username, account.password)
                } else {
                    _movies.value = FirebaseService.getMovies(account.allowedCategories)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading movies on demand", e)
            } finally {
                _isMoviesLoading.value = false
            }
        }
    }

    /**
     * Load series on demand to guarantee maximum speed and split queries
     */
    fun loadSeriesOnDemand() {
        val account = _activeAccount.value ?: return
        if (_series.value.isNotEmpty() || _isSeriesLoading.value) return

        viewModelScope.launch {
            _isSeriesLoading.value = true
            try {
                if (account.host.isNotEmpty()) {
                    _series.value = FirebaseService.fetchXtreamSeries(account.host, account.username, account.password)
                } else {
                    _series.value = FirebaseService.getSeries(account.allowedCategories)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading series on demand", e)
            } finally {
                _isSeriesLoading.value = false
            }
        }
    }

    /**
     * Select a movie to show details.
     */
    fun selectMovie(movie: Movie?) {
        _selectedMovie.value = movie
        if (movie != null) {
            _selectedSeries.value = null // clear other detail open state
            viewModelScope.launch {
                val account = _activeAccount.value
                if (account != null && account.host.isNotEmpty()) {
                    val enriched = FirebaseService.fetchMovieDetails(account.host, account.username, account.password, movie)
                    if (_selectedMovie.value?.id == movie.id) {
                        _selectedMovie.value = enriched
                    }
                }
            }
        }
    }

    /**
     * Select a series to expand seasons and episodes.
     */
    fun selectSeries(series: Series?) {
        _selectedSeries.value = series
        if (series != null) {
            _selectedMovie.value = null
            if (_codeUpdateRequiredMessage.value != null) {
                _errorMessage.value = "تنبيه: توفرت تعديلات جديدة، يرجى إعادة تسجيل الدخول"
            }
            // Fetch seasons and rich series metadata
            viewModelScope.launch {
                _isLoading.value = true
                val account = _activeAccount.value
                if (account != null && account.host.isNotEmpty()) {
                    val fullData = FirebaseService.fetchFullSeriesData(account.host, account.username, account.password, series)
                    if (_selectedSeries.value?.id == series.id) {
                        _selectedSeries.value = fullData.series
                    }
                    _seasons.value = fullData.seasons
                    val firstSeasonId = fullData.seasons.firstOrNull()?.id ?: "1"
                    val initialEpisodes = fullData.episodesBySeason[firstSeasonId]
                        ?: fullData.episodesBySeason.values.firstOrNull()
                        ?: fullData.allEpisodes
                    _episodes.value = initialEpisodes
                } else {
                    val listSeasons = FirebaseService.getSeasons(series.id)
                    _seasons.value = listSeasons
                    if (listSeasons.isNotEmpty()) {
                        loadEpisodes(series.id, listSeasons.first().id)
                    } else {
                        _episodes.value = emptyList()
                    }
                }
                _isLoading.value = false
            }
        }
    }

    /**
     * Load specific season episodes.
     */
    fun loadEpisodes(seriesId: String, seasonId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val account = _activeAccount.value
            val listEpisodes = if (account != null && account.host.isNotEmpty()) {
                FirebaseService.fetchXtreamEpisodes(account.host, account.username, account.password, seriesId, seasonId)
            } else {
                FirebaseService.getEpisodes(seriesId, seasonId)
            }
            _episodes.value = listEpisodes
            _isLoading.value = false
        }
    }

    private val _playingEventChannels = MutableStateFlow<List<EventChannel>>(emptyList())
    val playingEventChannels: StateFlow<List<EventChannel>> = _playingEventChannels.asStateFlow()

    private val _currentEventChannelIndex = MutableStateFlow(0)
    val currentEventChannelIndex: StateFlow<Int> = _currentEventChannelIndex.asStateFlow()

    fun startPlayingEvent(eventChannels: List<EventChannel>, startIndex: Int = 0) {
        if (eventChannels.isEmpty()) return
        
        _playingEventChannels.value = eventChannels
        _currentEventChannelIndex.value = startIndex
        
        val initialChannel = eventChannels[startIndex]
        startPlaying(
            url = initialChannel.streamUrl,
            title = initialChannel.name,
            isLive = true,
            forcedFormat = initialChannel.forcedFormat,
            servers = initialChannel.servers
        )
    }

    fun switchEventChannel(index: Int) {
        val channels = _playingEventChannels.value
        if (index in channels.indices) {
            _currentEventChannelIndex.value = index
            val channel = channels[index]
            startPlaying(
                url = channel.streamUrl,
                title = channel.name,
                isLive = true,
                forcedFormat = channel.forcedFormat,
                servers = channel.servers
            )
        }
    }

    /**
     * Route Stream Url to Media Player screen with multi-server support.
     */
    fun startPlaying(
        url: String,
        title: String,
        isLive: Boolean,
        forceRawUrl: Boolean = false,
        userAgent: String = "",
        forcedFormat: String = "",
        servers: List<EventServer> = emptyList()
    ) {
        val effectiveServers = if (servers.isNotEmpty()) {
            servers
        } else if (url.isNotBlank()) {
            listOf(EventServer(id = "srv_main", name = "السيرفر الرئيسي", url = url, forcedFormat = forcedFormat.ifEmpty { "m3u8" }))
        } else {
            emptyList()
        }
        _playingServers.value = effectiveServers
        _currentServerIndex.value = 0

        val targetUrl = if (effectiveServers.isNotEmpty()) effectiveServers[0].url else url
        val targetFormat = if (effectiveServers.isNotEmpty() && effectiveServers[0].forcedFormat.isNotBlank()) {
            effectiveServers[0].forcedFormat
        } else {
            forcedFormat
        }

        playStreamInternal(
            url = targetUrl,
            title = title,
            isLive = isLive,
            forceRawUrl = forceRawUrl || (servers.isNotEmpty()),
            userAgent = userAgent,
            forcedFormat = targetFormat
        )
    }

    fun switchServer(index: Int) {
        val servers = _playingServers.value
        if (index in servers.indices) {
            _currentServerIndex.value = index
            val srv = servers[index]
            playStreamInternal(
                url = srv.url,
                title = _playingTitle.value,
                isLive = _playingIsLive.value,
                forceRawUrl = srv.forcedFormat.isEmpty(),
                userAgent = _playingUserAgent.value,
                forcedFormat = srv.forcedFormat
            )
        }
    }

    private fun playStreamInternal(
        url: String,
        title: String,
        isLive: Boolean,
        forceRawUrl: Boolean = false,
        userAgent: String = "",
        forcedFormat: String = ""
    ) {
        if (!isAppAccessAllowed()) {
            Log.w("MainViewModel", "startPlaying blocked by AppVersionGate")
            return
        }

        var processedUrl = url
        val accountForcedFormat = (_activeAccount.value?.forcedFormat ?: "").lowercase()
        val finalForcedFormat = if (forcedFormat.isNotBlank()) {
            forcedFormat.lowercase()
        } else if (accountForcedFormat.isNotBlank()) {
            accountForcedFormat
        } else {
            ""
        }
        val currentMode = if (finalForcedFormat.isNotBlank()) finalForcedFormat else _playerMode.value.lowercase()

        val shouldBypassExtensionChange = (currentMode == "normal" || currentMode == "direct" || currentMode == "raw" || finalForcedFormat == "normal" || finalForcedFormat == "direct" || finalForcedFormat == "raw" || forceRawUrl)

        if (_codeUpdateRequiredMessage.value != null) {
            _errorMessage.value = "تنبيه: توفرت تعديلات جديدة، يرجى إعادة تسجيل الدخول"
        }

        val isExplicitTs = (currentMode == "ts" || finalForcedFormat == "ts")
        val isExplicitM3u8 = (currentMode == "m3u8" || finalForcedFormat == "m3u8")
        val isDirectOrNormal = (currentMode == "normal" || currentMode == "direct" || currentMode == "raw" || finalForcedFormat == "normal" || finalForcedFormat == "direct" || finalForcedFormat == "raw")

        if (isDirectOrNormal) {
            processedUrl = url
            if (isLive || url.contains("/live/")) {
                try {
                    val queryIndex = url.indexOf('?')
                    val base = if (queryIndex != -1) url.substring(0, queryIndex) else url
                    val query = if (queryIndex != -1) url.substring(queryIndex) else ""
                    val lastSlash = base.lastIndexOf('/')
                    val dotIndex = base.lastIndexOf('.')
                    if (dotIndex > lastSlash) {
                        val ext = base.substring(dotIndex).lowercase()
                        if (ext == ".ts" || ext == ".m3u8" || ext == ".pdf") {
                            processedUrl = base.substring(0, dotIndex) + query
                        }
                    }
                } catch (e: Exception) {
                    processedUrl = url
                }
            }
        } else if (isExplicitTs) {
            try {
                val queryIndex = url.indexOf('?')
                val base = if (queryIndex != -1) url.substring(0, queryIndex) else url
                val query = if (queryIndex != -1) url.substring(queryIndex) else ""
                val lastSlash = base.lastIndexOf('/')
                val dotIndex = base.lastIndexOf('.')
                if (dotIndex > lastSlash) {
                    val ext = base.substring(dotIndex).lowercase()
                    if (ext in listOf(".m3u8", ".ts", ".mp4", ".mkv", ".flv")) {
                        processedUrl = base.substring(0, dotIndex) + ".ts" + query
                    } else {
                        processedUrl = url
                    }
                } else if (!base.lowercase().endsWith(".ts")) {
                    processedUrl = "$base.ts$query"
                } else {
                    processedUrl = url
                }
            } catch (e: Exception) {
                processedUrl = url
            }
        } else if (isExplicitM3u8) {
            try {
                val queryIndex = url.indexOf('?')
                val base = if (queryIndex != -1) url.substring(0, queryIndex) else url
                val query = if (queryIndex != -1) url.substring(queryIndex) else ""
                val lastSlash = base.lastIndexOf('/')
                val dotIndex = base.lastIndexOf('.')
                if (dotIndex > lastSlash) {
                    val ext = base.substring(dotIndex).lowercase()
                    if (ext in listOf(".m3u8", ".ts", ".mp4", ".mkv", ".flv")) {
                        processedUrl = base.substring(0, dotIndex) + ".m3u8" + query
                    } else {
                        processedUrl = url
                    }
                } else if (!base.lowercase().endsWith(".m3u8")) {
                    processedUrl = "$base.m3u8$query"
                } else {
                    processedUrl = url
                }
            } catch (e: Exception) {
                processedUrl = url
            }
        } else if (!shouldBypassExtensionChange) {
            try {
                if (isLive || url.contains("/live/")) {
                    val lastSlash = url.lastIndexOf('/')
                    val dotIndex = url.lastIndexOf('.')
                    if (lastSlash != -1) {
                        val base = if (dotIndex > lastSlash) {
                            url.substring(0, dotIndex)
                        } else {
                            url
                        }
                        
                        val cleanBase = if (base.endsWith(".ts") || base.endsWith(".m3u8") || base.endsWith(".pdf") || base.endsWith(".mp4") || base.endsWith(".mkv")) {
                            base.substring(0, base.lastIndexOf('.'))
                        } else {
                            base
                        }
                        
                        processedUrl = when (currentMode) {
                            "ts" -> "$cleanBase.ts"
                            "m3u8" -> "$cleanBase.m3u8"
                            "pdf" -> "$cleanBase.pdf"
                            "normal", "direct" -> url
                            else -> url
                        }
                    }
                } else {
                    // For Movies and Series (VOD), keep original container URL to avoid HLS parser errors
                    processedUrl = url
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error processing stream URL extension formatting for mode $currentMode", e)
            }
        }

        Log.d("MainViewModel", "Setting playing stream: $processedUrl (Original: $url) - Mode: $currentMode (Forced: $finalForcedFormat)")
        _playingStreamUrl.value = processedUrl
        _playingTitle.value = title
        _playingIsLive.value = isLive
        _playingUserAgent.value = userAgent
        _forcedFormat.value = finalForcedFormat
        _isLivePlayerFullScreen.value = true
        _isPackagesPlayerFullScreen.value = false
        
        if (isLive) {
            viewModelScope.launch {
                com.example.data.FirebaseService.joinChannelPresence(title) { count ->
                    _currentViewerCount.value = count
                }
            }
        }
    }

    fun stopPlaying() {
        val title = _playingTitle.value
        if (_playingIsLive.value && title != null) {
            viewModelScope.launch {
                com.example.data.FirebaseService.leaveChannelPresence(title)
            }
        }
        _playingStreamUrl.value = null
        _isLivePlayerFullScreen.value = false
        _isPackagesPlayerFullScreen.value = false
    }

    /**
     * ADMIN PANEL: Verify admin secret code.
     */
    fun authenticateAdmin(password: String): Boolean {
        if (password == "ali2008#$1") {
            _isAdminAuthenticated.value = true
            loadAdminData()
            return true
        }
        return false
    }

    fun exitAdmin() {
        _isAdminAuthenticated.value = false
    }

    /**
     * Load admin panel reference content.
     */
    fun loadAdminData() {
        viewModelScope.launch {
            try {
                _adminCodesList.value = FirebaseService.getAccessCodes()
            } catch (e: Throwable) {
                Log.e("MainViewModel", "Failed to load admin code list", e)
            }
        }
    }

    /**
     * ADMIN CODE OPERATIONS: Create/Activate IPTV activations.
     */
    fun adminSaveAccessCode(code: String, isActive: Boolean, accountId: String) {
        viewModelScope.launch {
            _adminActionStatus.value = "جاري الحفظ..."
            val model = AccessCode(code.trim().uppercase(), isActive, accountId.ifEmpty { "premium_account_a1" })
            val success = FirebaseService.adminSaveAccessCode(model)
            if (success) {
                _adminActionStatus.value = "تم حفظ كود التفعيل بنجاح!"
                loadAdminData()
            } else {
                _adminActionStatus.value = "فشلت العملية!"
            }
        }
    }

    /**
     * ADMIN CODE OPERATIONS: Delete activation codes.
     */
    fun adminDeleteAccessCode(code: String) {
        viewModelScope.launch {
            _adminActionStatus.value = "جاري الحذف..."
            val success = FirebaseService.adminDeleteAccessCode(code)
            if (success) {
                _adminActionStatus.value = "تم الحذف بنجاح!"
                loadAdminData()
            } else {
                _adminActionStatus.value = "فصلت عملية الحذف!"
            }
        }
    }

    /**
     * ADMIN CONTENT OPERATIONS: Upload rapid channels.
     */
    fun adminAddChannel(name: String, cat: String, playUrl: String, logoUrl: String) {
        viewModelScope.launch {
            _adminActionStatus.value = "جاري إضافة القناة بث مباشر..."
            val ch = Channel(
                id = "chan_" + System.currentTimeMillis(),
                name = name,
                category = cat,
                streamUrl = playUrl,
                logoUrl = logoUrl.ifEmpty { "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=150&q=80" }
            )
            val success = FirebaseService.adminSaveChannel(ch)
            if (success) {
                _adminActionStatus.value = "تمت إضافة القناة البث المباشر بنجاح!"
                // Refresh client data if already authenticated
                _activeAccount.value?.let { loadIPTVContent(it.allowedCategories) }
            } else {
                _adminActionStatus.value = "فشلت إضافة القناة!"
            }
        }
    }

    /**
     * ADMIN CONTENT OPERATIONS: Upload rapid movies.
     */
    fun adminAddMovie(title: String, desc: String, playUrl: String, posterUrl: String, duration: String, cat: String, rating: Double, year: Int) {
        viewModelScope.launch {
            _adminActionStatus.value = "جاري إضافة الفيلم..."
            val movie = Movie(
                id = "movie_" + System.currentTimeMillis(),
                title = title,
                description = desc,
                posterUrl = posterUrl.ifEmpty { "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=400&q=80" },
                streamUrl = playUrl,
                category = cat,
                duration = duration.ifEmpty { "02:00:00" },
                rating = rating,
                year = year
            )
            val success = FirebaseService.adminSaveMovie(movie)
            if (success) {
                _adminActionStatus.value = "تمت إضافة الفيلم بنجاح!"
                _activeAccount.value?.let { loadIPTVContent(it.allowedCategories) }
            } else {
                _adminActionStatus.value = "فشلت إضافة الفيلم!"
            }
        }
    }

    fun clearAdminStatus() {
        _adminActionStatus.value = null
    }

    fun refreshDownloads() {
        _downloads.value = loadDownloadsInternal()
    }

    private var activeDownloadJob: Job? = null

    fun isItemDownloaded(id: String): Boolean {
        return _downloads.value.any { it.id == id && it.isCompleted }
    }

    fun isItemDownloading(id: String): Boolean {
        val currentItem = _downloadingItem.value
        val phase = _downloadPhase.value
        return currentItem?.id == id && (phase == "analyzing" || phase == "downloading" || phase == "merging")
    }

    fun getDownloadedItem(id: String): LocalDownload? {
        return _downloads.value.firstOrNull { it.id == id && it.isCompleted }
    }

    fun getTotalDownloadsSizeFormatted(): String {
        var totalBytes = 0L
        for (item in _downloads.value) {
            if (item.fileSizeBytes > 0) {
                totalBytes += item.fileSizeBytes
            } else {
                try {
                    val uri = Uri.parse(item.videoUrl)
                    val path = uri.path
                    if (path != null) {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            totalBytes += file.length()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        return FastDownloaderEngine.formatBytes(totalBytes)
    }

    fun cancelDownload() {
        val currentId = _downloadingItem.value?.id
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _downloadPhase.value = "failed"
        _downloadingItem.value = null
        _activeDownloadState.value = null
        if (currentId != null) {
            val notifId = (currentId.hashCode() and 0x7FFFFFFF)
            NotificationHelper.cancelNotification(getApplication<Application>(), notifId)
        }
    }

    fun startDownload(downloadItem: LocalDownload) {
        activeDownloadJob?.cancel()
        _downloadingItem.value = downloadItem
        _downloadProgress.value = 0.05f
        _downloadPhase.value = "analyzing"
        
        activeDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            val userAgentValue = if (playingUserAgent.value.isNotEmpty()) playingUserAgent.value else "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            
            val result = FastDownloaderEngine.executeDownload(
                context = getApplication<Application>(),
                downloadItem = downloadItem,
                userAgent = userAgentValue,
                onProgressUpdate = { state ->
                    _activeDownloadState.value = state
                    _downloadProgress.value = state.progress
                    _downloadPhase.value = state.phase
                }
            )

            if (result.isSuccess && isActive) {
                val downloadedFile = result.getOrNull()
                val localFileUri = if (downloadedFile != null) Uri.fromFile(downloadedFile).toString() else downloadItem.videoUrl
                val fileSize = downloadedFile?.length() ?: 0L

                val completedItem = downloadItem.copy(
                    videoUrl = localFileUri,
                    fileSizeBytes = fileSize,
                    downloadedBytes = fileSize,
                    isCompleted = true,
                    timestamp = System.currentTimeMillis()
                )
                saveDownload(completedItem)
                _downloadProgress.value = 1.0f
                _downloadPhase.value = "completed"
                delay(1000L)
                _downloadingItem.value = null
            } else if (!isActive) {
                _downloadPhase.value = "failed"
                _downloadingItem.value = null
            } else {
                _downloadPhase.value = "failed"
                _downloadingItem.value = null
            }
        }
    }

    private fun loadDownloadsInternal(): List<LocalDownload> {
        val prefs = getApplication<Application>().getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("saved_downloads_v2", null)
            ?: prefs.getString("saved_downloads", "[]") ?: "[]"
        val list = mutableListOf<LocalDownload>()
        try {
            val regex = java.util.regex.Pattern.compile("\\{([^\\}]+)\\}")
            val matcher = regex.matcher(rawJson)
            while (matcher.find()) {
                val content = matcher.group(1) ?: continue
                val id = extractJsonField(content, "id")
                val title = extractJsonField(content, "title")
                val posterUrl = extractJsonField(content, "posterUrl")
                val videoUrl = extractJsonField(content, "videoUrl")
                val quality = extractJsonField(content, "quality").ifEmpty { "1080p FHD" }
                val type = extractJsonField(content, "type").ifEmpty { "movie" }
                val seriesTitle = extractJsonField(content, "seriesTitle")
                val duration = extractJsonField(content, "duration")
                val episodeNumber = content.substringAfter("\"episodeNumber\":", "0").substringBefore(",").trim().filter { it.isDigit() }.toIntOrNull() ?: 0
                val seasonNumber = content.substringAfter("\"seasonNumber\":", "0").substringBefore(",").trim().filter { it.isDigit() }.toIntOrNull() ?: 0
                val fileSizeBytes = content.substringAfter("\"fileSizeBytes\":", "0").substringBefore(",").trim().filter { it.isDigit() }.toLongOrNull() ?: 0L
                val timestamp = content.substringAfter("\"timestamp\":", "0").substringBefore(",").trim().filter { it.isDigit() }.toLongOrNull() ?: System.currentTimeMillis()
                val isCompleted = !content.contains("\"isCompleted\":false")

                if (id.isNotEmpty() && title.isNotEmpty()) {
                    list.add(
                        LocalDownload(
                            id = id,
                            title = title,
                            posterUrl = posterUrl,
                            videoUrl = videoUrl,
                            quality = quality,
                            timestamp = timestamp,
                            fileSizeBytes = fileSizeBytes,
                            downloadedBytes = fileSizeBytes,
                            type = type,
                            seriesTitle = seriesTitle,
                            episodeNumber = episodeNumber,
                            seasonNumber = seasonNumber,
                            isCompleted = isCompleted,
                            duration = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("Downloads", "Error parsing local downloads", e)
        }
        return list
    }

    private fun extractJsonField(content: String, field: String): String {
        val key = "\"$field\":\""
        if (!content.contains(key)) return ""
        return content.substringAfter(key).substringBefore("\"")
    }

    fun saveDownload(download: LocalDownload) {
        val current = loadDownloadsInternal().toMutableList()
        current.removeAll { it.id == download.id }
        current.add(0, download)
        saveDownloadsInternal(current)
        _downloads.value = current
    }

    fun deleteDownload(downloadId: String) {
        val current = loadDownloadsInternal()
        val toDelete = current.find { it.id == downloadId }
        if (toDelete != null) {
            try {
                val uri = Uri.parse(toDelete.videoUrl)
                val path = uri.path
                if (path != null) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w("Downloads", "Failed to delete file from disk: ${e.message}")
            }
        }
        val updated = current.filter { it.id != downloadId }
        saveDownloadsInternal(updated)
        _downloads.value = updated
    }

    fun clearAllDownloads() {
        val current = loadDownloadsInternal()
        for (item in current) {
            try {
                val uri = Uri.parse(item.videoUrl)
                val path = uri.path
                if (path != null) {
                    val file = java.io.File(path)
                    if (file.exists()) file.delete()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        saveDownloadsInternal(emptyList())
        _downloads.value = emptyList()
    }

    private fun saveDownloadsInternal(list: List<LocalDownload>) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("MainViewModel", Context.MODE_PRIVATE)
        val json = StringBuilder("[")
        list.forEachIndexed { index, download ->
            json.append("{")
                .append("\"id\":\"").append(download.id.replace("\"", "\\\"")).append("\",")
                .append("\"title\":\"").append(download.title.replace("\"", "\\\"")).append("\",")
                .append("\"posterUrl\":\"").append(download.posterUrl.replace("\"", "\\\"")).append("\",")
                .append("\"videoUrl\":\"").append(download.videoUrl.replace("\"", "\\\"")).append("\",")
                .append("\"quality\":\"").append(download.quality.replace("\"", "\\\"")).append("\",")
                .append("\"type\":\"").append(download.type.replace("\"", "\\\"")).append("\",")
                .append("\"seriesTitle\":\"").append(download.seriesTitle.replace("\"", "\\\"")).append("\",")
                .append("\"duration\":\"").append(download.duration.replace("\"", "\\\"")).append("\",")
                .append("\"episodeNumber\":").append(download.episodeNumber).append(",")
                .append("\"seasonNumber\":").append(download.seasonNumber).append(",")
                .append("\"fileSizeBytes\":").append(download.fileSizeBytes).append(",")
                .append("\"isCompleted\":").append(download.isCompleted).append(",")
                .append("\"timestamp\":").append(download.timestamp)
                .append("}")
            if (index < list.size - 1) json.append(",")
        }
        json.append("]")
        prefs.edit()
            .putString("saved_downloads_v2", json.toString())
            .putString("saved_downloads", json.toString())
            .apply()
    }

    fun loadActivationCodes() {
        viewModelScope.launch {
            try {
                _activationCodes.value = FirebaseService.adminGetActivationCodes()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading activation codes", e)
            }
        }
    }

    fun adminSaveActivationCode(code: String, host: String, username: String, password: String, forcedFormat: String, allowedTypes: List<String> = listOf("live", "movies", "series"), m3uUrl: String = "") {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري الحفظ والرفع..."
            try {
                val model = ActivationCode(
                    code = code.trim().uppercase(),
                    host = host.trim(),
                    username = username.trim(),
                    password = password.trim(),
                    forcedFormat = forcedFormat.trim(),
                    allowedTypes = allowedTypes,
                    m3uUrl = m3uUrl.trim()
                )
                val success = FirebaseService.adminSaveActivationCode(model)
                if (success) {
                    _developerActionStatus.value = "تم نشر كود التفعيل $code بنجاح!"
                } else {
                    _developerActionStatus.value = "تم النشر بنجاح على قاعدة البيانات."
                }
                loadActivationCodes()
            } catch (e: Exception) {
                _developerActionStatus.value = "فشل في الحفظ: ${e.message}"
            }
        }
    }

    fun adminDeleteActivationCode(code: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري الحذف..."
            try {
                val success = FirebaseService.adminDeleteActivationCode(code)
                if (success) {
                    _developerActionStatus.value = "تم حذف الكود $code بنجاح!"
                } else {
                    _developerActionStatus.value = "تم الحذف بنجاح."
                }
                loadActivationCodes()
            } catch (e: Exception) {
                _developerActionStatus.value = "فشل في الحذف: ${e.message}"
            }
        }
    }

    fun authenticateDeveloper(password: String): Boolean {
        if (password == "ali2008#$1") {
            _isDeveloperAuthenticated.value = true
            _developerActionStatus.value = "مرحباً بك يا مطور في لوحة التحكم الفاخرة!"
            loadActivationCodes()
            return true
        }
        _developerActionStatus.value = "كلمة المرور خاطئة!"
        return false
    }

    fun logoutDeveloper() {
        _isDeveloperAuthenticated.value = false
        _developerActionStatus.value = null
    }

    fun setShowPackagesSection(show: Boolean) {
        _showPackagesSection.value = show
        viewModelScope.launch {
            FirebaseService.saveAppConfig("showPackagesSection", show)
        }
    }

    fun publishDeveloperCategory(id: String, name: String, type: String = "exclusive", imageUrl: String = "") {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري نشر القسم الجديد أونلاين..."
            val finalId = id.trim().ifEmpty { "cat_" + System.currentTimeMillis() }
            val category = IPTVCategory(
                id = finalId,
                name = name.trim(),
                type = type.trim(),
                imageUrl = imageUrl.trim()
            )
            val success = FirebaseService.adminSaveCategory(category)
            if (success) {
                _developerActionStatus.value = "تم نشر وحفظ القسم (${name}) بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
                loadEventChannels()
            loadNews()
            } else {
                _developerActionStatus.value = "خطأ: فشل نشر القسم أونلاين!"
            }
        }
    }

    fun deleteDeveloperCategory(catId: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف القسم..."
            val success = FirebaseService.adminDeleteContent("categories", catId)
            if (success) {
                _developerActionStatus.value = "تم حذف القسم بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
                loadEventChannels()
            loadNews()
            } else {
                _developerActionStatus.value = "فشل حذف القسم!"
            }
        }
    }

    fun publishDeveloperEventChannel(
        id: String,
        name: String,
        imageUrl: String,
        streamUrl: String,
        forcedFormat: String = "m3u8",
        servers: List<EventServer> = emptyList(),
        eventType: String = "stream",
        targetChannelId: String = "",
        targetChannelIds: List<String> = emptyList(),
        startTimeMs: Long = 0L
    ) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري نشر الحدث..."
            val eventId = id.ifEmpty { "event_" + System.currentTimeMillis() }
            
            var targetChUrl = ""
            var targetChFormat = forcedFormat
            var targetChImage = imageUrl.trim()

            if (eventType == "match" && (targetChannelId.isNotBlank() || targetChannelIds.isNotEmpty())) {
                val firstLinkedId = targetChannelId.ifBlank { targetChannelIds.firstOrNull() ?: "" }
                if (firstLinkedId.isNotBlank()) {
                    val matchedPub = _events.value.find { it.id == firstLinkedId }
                    val matchedIptv = _channels.value.find { it.id == firstLinkedId }
                    targetChUrl = matchedPub?.streamUrl ?: matchedIptv?.streamUrl ?: ""
                    if (targetChFormat.isBlank()) {
                        targetChFormat = matchedPub?.forcedFormat ?: matchedIptv?.forcedFormat ?: "m3u8"
                    }
                    if (targetChImage.isBlank()) {
                        targetChImage = matchedPub?.imageUrl ?: matchedIptv?.logoUrl ?: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=500&q=80"
                    }
                }
            }

            val finalImageUrl = targetChImage.ifEmpty {
                if (eventType == "match") "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=500&q=80"
                else "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=300&q=80"
            }

            val effectiveServers = if (servers.isNotEmpty()) {
                servers
            } else if (streamUrl.isNotBlank()) {
                listOf(EventServer(id = "srv_1", name = "السيرفر الرئيسي", url = streamUrl.trim(), forcedFormat = forcedFormat.ifEmpty { "m3u8" }))
            } else {
                emptyList()
            }
            val effectiveUrl = streamUrl.trim().ifBlank { 
                effectiveServers.firstOrNull()?.url ?: targetChUrl 
            }
            val ch = EventChannel(
                id = eventId,
                name = name.trim(),
                imageUrl = finalImageUrl,
                streamUrl = effectiveUrl,
                forcedFormat = forcedFormat.ifEmpty { effectiveServers.firstOrNull()?.forcedFormat ?: targetChFormat.ifEmpty { "m3u8" } },
                servers = effectiveServers,
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                targetChannelId = targetChannelId,
                targetChannelIds = targetChannelIds,
                startTimeMs = startTimeMs
            )
            // Immediately update local state so user sees it right away
            _events.value = (listOf(ch) + _events.value.filter { it.id != eventId }).distinctBy { it.id }
            val success = FirebaseService.adminSaveEventChannel(ch)
            if (success) {
                _developerActionStatus.value = "تم نشر الحدث (${name}) بنجاح!"
                loadEventChannels()
            loadNews()
            } else {
                _developerActionStatus.value = "فشل نشر الحدث أونلاين!"
            }
        }
    }

    fun deleteDeveloperEventChannel(chanId: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف الحدث..."
            _events.value = _events.value.filter { it.id != chanId }
            val success = FirebaseService.deleteEventChannel(chanId) || FirebaseService.adminDeleteContent("events", chanId)
            if (success) {
                _developerActionStatus.value = "تم حذف الحدث بنجاح!"
                loadEventChannels()
            loadNews()
            } else {
                _developerActionStatus.value = "فشل حذف الحدث!"
            }
        }
    }

    fun publishDeveloperChannel(id: String, name: String, logoUrl: String, streamUrl: String, categoryId: String, userAgent: String, forcedFormat: String = "m3u8") {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري نشر قناة البث المباشر..."
            val ch = Channel(
                id = id.ifEmpty { "chan_" + System.currentTimeMillis() },
                name = name.trim(),
                logoUrl = logoUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=150&q=80" },
                streamUrl = streamUrl.trim(),
                category = categoryId.trim(),
                userAgent = userAgent.trim(),
                forcedFormat = forcedFormat
            )
            val success = FirebaseService.adminSaveChannel(ch)
            if (success) {
                _developerActionStatus.value = "تم نشر قناة البث المباشر (${name}) بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل نشر قناة البث المباشر أونلاين!"
            }
        }
    }

    fun deleteDeveloperChannel(chanId: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف قناة البث المباشر..."
            val success = FirebaseService.adminDeleteContent("channels", chanId)
            if (success) {
                _developerActionStatus.value = "تم حذف قناة البث المباشر بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل حذف قناة البث المباشر!"
            }
        }
    }

    fun publishDeveloperMovie(id: String, title: String, description: String, posterUrl: String, streamUrl: String, categoryId: String, duration: String, rating: Double, year: Int) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري نشر الفيلم للمكتبة..."
            val movie = Movie(
                id = id.ifEmpty { "movie_" + System.currentTimeMillis() },
                title = title.trim(),
                description = description.trim().ifEmpty { "فيلم سينمائي حائز على جوائز متاح الآن للمشاهدة." },
                posterUrl = posterUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80" },
                streamUrl = streamUrl.trim(),
                category = categoryId.trim(),
                duration = duration.trim().ifEmpty { "02:00:00" },
                rating = rating,
                year = year
            )
            val success = FirebaseService.adminSaveMovie(movie)
            if (success) {
                _developerActionStatus.value = "تم نشر فيلم (${title}) للمكتبة بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل نشر الفيلم أونلاين!"
            }
        }
    }

    fun deleteDeveloperMovie(movieId: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف الفيلم..."
            val success = FirebaseService.adminDeleteContent("movies", movieId)
            if (success) {
                _developerActionStatus.value = "تم حذف الفيلم بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل حذف الفيلم!"
            }
        }
    }

    fun publishDeveloperSeries(id: String, title: String, description: String, posterUrl: String, categoryId: String, rating: Double, year: Int) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري نشر المسلسل للمكتبة..."
            val s = Series(
                id = id.ifEmpty { "series_" + System.currentTimeMillis() },
                title = title.trim(),
                description = description.trim().ifEmpty { "مسلسل درامي حصري متاح للمشاهدة الآن." },
                posterUrl = posterUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&q=80" },
                category = categoryId.trim(),
                rating = rating,
                year = year
            )
            val success = FirebaseService.adminSaveSeries(s)
            if (success) {
                _developerActionStatus.value = "تم نشر مسلسل (${title}) للمكتبة بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل نشر المسلسل أونلاين!"
            }
        }
    }

    fun deleteDeveloperSeries(seriesId: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف المسلسل..."
            val success = FirebaseService.adminDeleteContent("series", seriesId)
            if (success) {
                _developerActionStatus.value = "تم حذف المسلسل بنجاح!"
                val allowed = _activeAccount.value?.allowedCategories ?: emptyList()
                loadIPTVContent(allowed)
            } else {
                _developerActionStatus.value = "فشل حذف المسلسل!"
            }
        }
    }

    private val notifiedMessageIds = mutableSetOf<String>()

    private fun showLocalNotification(sender: String, messageText: String, channelIdForIntent: String) {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("channelId", channelIdForIntent)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                sender.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, "private_chats")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("رسالة جديدة من @$sender")
                .setContentText(messageText)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(sender.hashCode(), builder.build())
            Log.d("MainViewModel", "Local notification sent for sender: $sender")
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to send local notification", e)
        }
    }

    fun setChatTabVisibilityOnline(visible: Boolean) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات الدردشة..."
            val success = FirebaseService.saveAppConfig("showChatTab", visible)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات الدردشة بنجاح!"
                _showChatTab.value = visible
                delay(3000)
                _developerActionStatus.value = null
            } else {
                _developerActionStatus.value = "فشل التحديث: حدث خطأ أثناء الاتصال بقاعدة البيانات"
            }
        }
    }

    fun setEventsVisibilityMode(mode: String) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات قسم الأحداث..."
            val success = FirebaseService.saveAppConfig("eventsVisibilityMode", mode)
            if (success) {
                FirebaseService.saveAppConfig("showEventsTab", mode != "HIDDEN")
                _eventsVisibilityMode.value = mode
                _showEventsTab.value = (mode != "HIDDEN")
                _developerActionStatus.value = "تم تحديث إعدادات قسم الأحداث: " + when (mode) {
                    "VIP_ONLY" -> "متاح لأكواد VIP فقط 👑"
                    "HIDDEN" -> "مخفي عن الجميع 🚫"
                    else -> "ظاهر للجميع 🟢"
                }
                delay(3000)
                _developerActionStatus.value = null
            } else {
                _developerActionStatus.value = "فشل التحديث: حدث خطأ أثناء الاتصال بقاعدة البيانات"
            }
        }
    }

    fun setEventChannelsTabVisibilityOnline(visible: Boolean) {
        setEventsVisibilityMode(if (visible) "ALL" else "HIDDEN")
    }

    fun setShowAiSectionInDrawer(visible: Boolean) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث إعدادات قسم الذكاء الاصطناعي..."
            val success = FirebaseService.saveAppConfig("showAiSectionInDrawer", visible)
            if (success) {
                _developerActionStatus.value = "تم تحديث إعدادات قسم الذكاء الاصطناعي بنجاح!"
                _showAiSectionInDrawer.value = visible
            } else {
                _developerActionStatus.value = "فشل التحديث: حدث خطأ أثناء الاتصال بقاعدة البيانات"
            }
        }
    }

    fun fetchChatMessages(channelId: String) {
        viewModelScope.launch {
            try {
                val msgs = ChatService.getMessages(channelId)
                _chatMessages.value = msgs
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch chat messages for $channelId", e)
            }
        }
    }

    fun sendChatMessage(
        channelId: String,
        sender: String,
        text: String,
        replyToId: String? = null,
        replyToSender: String? = null,
        replyToText: String? = null
    ) {
        viewModelScope.launch {
            try {
                val success = ChatService.postMessage(
                    channelId = channelId,
                    sender = sender,
                    text = text,
                    replyToId = replyToId,
                    replyToSender = replyToSender,
                    replyToText = replyToText
                )
                if (success) {
                    val msgs = ChatService.getMessages(channelId)
                    _chatMessages.value = msgs
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to send chat message", e)
            }
        }
    }

    fun registerChatUser(username: String, name: String, password: String, avatar: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                var finalAvatar = avatar
                if (finalAvatar.startsWith("content://") || finalAvatar.startsWith("file://")) {
                    val uploadedUrl = FirebaseStorageUtil.uploadFile(android.net.Uri.parse(finalAvatar), "avatars")
                    if (uploadedUrl != null) {
                        finalAvatar = uploadedUrl
                    } else {
                        onResult(false, "فشل رفع الصورة الشخصية، يرجى المحاولة لاحقاً")
                        return@launch
                    }
                }

                val cleanedUsername = username.trim().lowercase()
                val cleanedName = name.trim()
                val cleanedPassword = password.trim()

                if (cleanedUsername.isEmpty() || cleanedName.isEmpty() || cleanedPassword.isEmpty()) {
                    onResult(false, "يرجى تعبئة جميع الحقول المطلوبة!")
                    return@launch
                }
                
                // Check if user profile exists
                val existingProfile = ChatService.getUserProfile(cleanedUsername)
                if (existingProfile != null) {
                    onResult(false, "اسم المستخدم محجوز مسبقاً! يرجى اختيار اسم مستخدم آخر أو تسجيل الدخول.")
                    return@launch
                }

                // New user registration!
                val success = ChatService.registerUser(cleanedUsername, cleanedName, cleanedPassword, finalAvatar)
                if (success) {
                    prefs.edit()
                        .putString("chat_username", cleanedUsername)
                        .putString("chat_name", cleanedName)
                        .putString("chat_avatar", finalAvatar)
                        .putString("chat_password", cleanedPassword)
                        .apply()
                    _chatUsername.value = cleanedUsername
                    _chatName.value = cleanedName
                    _chatAvatar.value = finalAvatar
                    fetchAllUsers()
                    onResult(true, null)
                } else {
                    onResult(false, "حدث خطأ أثناء إنشاء الحساب، يرجى المحاولة لاحقاً.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to register chat user", e)
                onResult(false, "فشل الاتصال بخادم الدردشة!")
            }
        }
    }

    fun loginChatUser(username: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanedUsername = username.trim().lowercase()
                val cleanedPassword = password.trim()

                if (cleanedUsername.isEmpty() || cleanedPassword.isEmpty()) {
                    onResult(false, "يرجى إدخال اسم المستخدم وكلمة المرور!")
                    return@launch
                }

                val existingProfile = ChatService.getUserProfile(cleanedUsername)
                if (existingProfile == null) {
                    onResult(false, "اسم المستخدم غير مسجل في النظام! يرجى إنشاء حساب أولاً.")
                    return@launch
                }

                val registeredPassword = existingProfile.optString("password", "")
                if (registeredPassword.isNotEmpty() && registeredPassword != cleanedPassword) {
                    onResult(false, "كلمة المرور غير صحيحة! يرجى التأكد وإعادة المحاولة.")
                    return@launch
                }

                val existingName = existingProfile.optString("name", cleanedUsername)
                val existingAvatar = existingProfile.optString("avatar", "neon_wolf")

                prefs.edit()
                    .putString("chat_username", cleanedUsername)
                    .putString("chat_name", existingName)
                    .putString("chat_avatar", existingAvatar)
                    .putString("chat_password", cleanedPassword)
                    .apply()

                _chatUsername.value = cleanedUsername
                _chatName.value = existingName
                _chatAvatar.value = existingAvatar
                fetchAllUsers()
                fetchMessagedUsers()
                onResult(true, null)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to login chat user", e)
                onResult(false, "فشل تسجيل الدخول: خطأ في الاتصال بالشبكة!")
            }
        }
    }

    fun logoutChat() {
        prefs.edit()
            .remove("chat_username")
            .remove("chat_name")
            .remove("chat_avatar")
            .remove("chat_password")
            .apply()
        _chatUsername.value = null
        _chatName.value = null
        _chatAvatar.value = "neon_wolf"
    }

    fun updateChatProfile(newName: String, newPassword: String?, onResult: (Boolean, String?) -> Unit) {
        val currentUsername = _chatUsername.value ?: return
        val currentAvatar = _chatAvatar.value ?: "neon_wolf"
        val cleanedName = newName.trim()
        val cleanedPassword = newPassword?.trim()

        if (cleanedName.isEmpty()) {
            onResult(false, "يجب إدخال الاسم الشخصي!")
            return
        }

        viewModelScope.launch {
            try {
                val success = ChatService.registerUser(
                    username = currentUsername,
                    name = cleanedName,
                    password = if (!cleanedPassword.isNullOrEmpty()) cleanedPassword else null,
                    avatar = currentAvatar
                )
                if (success) {
                    _chatName.value = cleanedName
                    prefs.edit()
                        .putString("chat_name", cleanedName)
                        .apply()
                    if (!cleanedPassword.isNullOrEmpty()) {
                        prefs.edit().putString("chat_password", cleanedPassword).apply()
                    }
                    fetchAllUsers()
                    onResult(true, null)
                } else {
                    onResult(false, "فشل تحديث بيانات الحساب!")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update chat profile", e)
                onResult(false, "حدث خطأ أثناء التحديث!")
            }
        }
    }

    fun fetchAllUsers() {
        viewModelScope.launch {
            try {
                val profilesMap = ChatService.getAllUserProfiles()
                val parsedProfiles = profilesMap.mapValues { (username, json) ->
                    ChatUserProfile(
                        username = username,
                        name = json.optString("name", username),
                        avatar = json.optString("avatar", "neon_wolf"),
                        role = json.optString("role", "user")
                    )
                }
                _userProfiles.value = parsedProfiles
                // Update legacy _allUsers mapping for backward-compatibility
                _allUsers.value = parsedProfiles.mapValues { it.value.name }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch all users profiles", e)
            }
        }
    }

    fun addChatReaction(channelId: String, messageId: String, username: String, emoji: String) {
        viewModelScope.launch {
            try {
                val success = ChatService.addReaction(channelId, messageId, username, emoji)
                if (success) {
                    val msgs = ChatService.getMessages(channelId)
                    _chatMessages.value = msgs
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to add reaction", e)
            }
        }
    }

    fun removeChatReaction(channelId: String, messageId: String, username: String) {
        viewModelScope.launch {
            try {
                val success = ChatService.removeReaction(channelId, messageId, username)
                if (success) {
                    val msgs = ChatService.getMessages(channelId)
                    _chatMessages.value = msgs
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to remove reaction", e)
            }
        }
    }

    fun setChatEnabledOnline(enabled: Boolean) {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري تحديث حالة الدردشة أونلاين..."
            val success = ChatService.setChatEnabled(enabled)
            if (success) {
                _isChatEnabledOnline.value = enabled
                _developerActionStatus.value = "تم تحديث حالة الدردشة أونلاين بنجاح!"
            } else {
                _developerActionStatus.value = "خطأ: فشل تحديث حالة الدردشة أونلاين!"
            }
        }
    }

    fun wipeAllChatMessages() {
        viewModelScope.launch {
            _developerActionStatus.value = "جاري حذف جميع رسائل الدردشة..."
            val success = ChatService.deleteAllMessages()
            if (success) {
                _chatMessages.value = emptyList()
                _developerActionStatus.value = "تم حذف جميع الرسائل من قاعدة البيانات بنجاح!"
            } else {
                _developerActionStatus.value = "خطأ: فشل حذف رسائل قاعدة البيانات!"
            }
        }
    }

    fun clearDeveloperStatus() {
        _developerActionStatus.value = null
    }

    fun fetchMessagedUsers() {
        val myUser = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val allChats = ChatService.getAllChats()
                val usersMap = ChatService.getAllUsers()
                val tempMessaged = mutableSetOf<String>()
                if (allChats != null) {
                    for ((chanId, messagesMap) in allChats) {
                        if (chanId.startsWith("private_")) {
                            val parts = chanId.split("_")
                            if (parts.size >= 3) {
                                val userA = parts[1]
                                val userB = parts[2]
                                if (messagesMap.isNotEmpty()) {
                                    if (userA == myUser) {
                                        tempMessaged.add(userB)
                                    } else if (userB == myUser) {
                                        tempMessaged.add(userA)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Fetch blocked list as well
                val blocked = ChatService.getBlockedUsers(myUser)
                _blockedUsers.value = blocked

                // Filter out any user who is blocked from the messaged list if desired,
                // but let's keep them and let the UI/user handle blocks/unblocks.
                val list = tempMessaged.map { username ->
                    val displayName = usersMap[username] ?: username
                    Pair(username, displayName)
                }
                _messagedUsers.value = list
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to fetch messaged users", e)
            }
        }
    }

    fun updateChatUser(newUsername: String, newName: String, newPassword: String, newAvatar: String, onResult: (Boolean, String?) -> Unit) {
        val oldUsername = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val cleanedNewUsername = newUsername.trim().lowercase()
                val cleanedNewName = newName.trim()
                
                if (cleanedNewUsername.isEmpty() || cleanedNewName.isEmpty() || newPassword.isEmpty()) {
                    onResult(false, "يجب ملء جميع الحقول!")
                    return@launch
                }
                
                // If they changed the username, check if the new one is taken
                if (cleanedNewUsername != oldUsername) {
                    if (ChatService.isUsernameTaken(cleanedNewUsername)) {
                        onResult(false, "اسم المستخدم هذا مستخدم بالفعل من قبل شخص آخر!")
                        return@launch
                    }
                }
                
                // Save new user info
                val success = ChatService.registerUser(cleanedNewUsername, cleanedNewName, newPassword, newAvatar)
                if (success) {
                    // If username changed, delete the old database node
                    if (cleanedNewUsername != oldUsername) {
                        val deleteUrl = "https://loop-7e3d9-default-rtdb.firebaseio.com/users/$oldUsername.json"
                        try {
                            val request = okhttp3.Request.Builder().url(deleteUrl).delete().build()
                            ChatService.client.newCall(request).execute().close()
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to delete old user node", e)
                        }
                    }
                    
                    // Save in preferences
                    prefs.edit()
                        .putString("chat_username", cleanedNewUsername)
                        .putString("chat_name", cleanedNewName)
                        .putString("chat_avatar", newAvatar)
                        .putString("chat_password", newPassword)
                        .apply()
                        
                    _chatUsername.value = cleanedNewUsername
                    _chatName.value = cleanedNewName
                    _chatAvatar.value = newAvatar
                    fetchAllUsers()
                    fetchMessagedUsers()
                    onResult(true, null)
                } else {
                    onResult(false, "حدث خطأ أثناء حفظ التحديثات.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to update chat user", e)
                onResult(false, "حدث خطأ غير متوقع.")
            }
        }
    }

    fun blockUser(otherUsername: String) {
        val myUser = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val success = ChatService.blockUser(myUser, otherUsername)
                if (success) {
                    fetchMessagedUsers()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to block user", e)
            }
        }
    }

    fun unblockUser(otherUsername: String) {
        val myUser = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val success = ChatService.unblockUser(myUser, otherUsername)
                if (success) {
                    fetchMessagedUsers()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to unblock user", e)
            }
        }
    }

    fun deleteConversation(channelId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.deleteConversation(channelId)
                if (success) {
                    fetchMessagedUsers()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to delete conversation", e)
                onResult(false)
            }
        }
    }

    // --- Global Chat Developer Moderation (Firebase Web RTDB) ---

    private val _globalChatMessages = MutableStateFlow<List<GlobalChatMessageItem>>(emptyList())
    val globalChatMessages: StateFlow<List<GlobalChatMessageItem>> = _globalChatMessages.asStateFlow()

    private val _globalChatUsers = MutableStateFlow<List<GlobalChatUserItem>>(emptyList())
    val globalChatUsers: StateFlow<List<GlobalChatUserItem>> = _globalChatUsers.asStateFlow()

    private val _isGlobalChatLoading = MutableStateFlow(false)
    val isGlobalChatLoading: StateFlow<Boolean> = _isGlobalChatLoading.asStateFlow()

    fun fetchGlobalChatModerationData() {
        viewModelScope.launch {
            _isGlobalChatLoading.value = true
            try {
                _globalChatMessages.value = ChatService.fetchAllGlobalChatMessages()
                _globalChatUsers.value = ChatService.fetchAllGlobalChatUsers()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching global chat data", e)
            } finally {
                _isGlobalChatLoading.value = false
            }
        }
    }

    fun deleteGlobalChatMessage(messageId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.deleteGlobalChatMessage(messageId)
                if (success) {
                    _globalChatMessages.value = _globalChatMessages.value.filter { it.id != messageId }
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting global message", e)
                onResult(false)
            }
        }
    }

    fun resetAndWipeAllGlobalChat(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.resetAndWipeAllGlobalChatMessages()
                if (success) {
                    _globalChatMessages.value = emptyList()
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error wiping global chat", e)
                onResult(false)
            }
        }
    }

    fun postGlobalAdminBroadcast(text: String, adminName: String = "إدارة +90", onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.postGlobalAdminBroadcast(text, adminName)
                if (success) {
                    fetchGlobalChatModerationData()
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error posting admin message", e)
                onResult(false)
            }
        }
    }

    fun deleteGlobalChatUser(uid: String, username: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.deleteGlobalChatUser(uid, username)
                if (success) {
                    _globalChatUsers.value = _globalChatUsers.value.filter { it.uid != uid }
                }
                onResult(success)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting chat user", e)
                onResult(false)
            }
        }
    }

    fun wipeAllPrivateConversations(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = ChatService.wipeAllPrivateConversations()
                onResult(success)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error wiping private conversations", e)
                onResult(false)
            }
        }
    }

    // --- Notification System ---

    fun fetchNotifications() {
        viewModelScope.launch {
            try {
                _notifications.value = FirebaseService.getNotifications()
                updateUnreadCount()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching notifications: ${e.message}")
            }
        }
    }

    fun sendAppNotification(title: String, subtitle: String, body: String, imageUrl: String?, linkUrl: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = System.currentTimeMillis().toString()
                val notification = AppNotification(
                    id = id,
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    body = body.trim(),
                    imageUrl = if (imageUrl.isNullOrBlank()) null else imageUrl.trim(),
                    linkUrl = if (linkUrl.isNullOrBlank()) null else linkUrl.trim(),
                    timestamp = System.currentTimeMillis()
                )
                val success = FirebaseService.saveNotification(notification)
                if (success) {
                    _notifications.value = listOf(notification) + _notifications.value.filter { it.id != id }
                    updateUnreadCount()
                    // Trigger system notification locally as well
                    NotificationHelper.showSystemNotification(
                        context = getApplication(),
                        title = notification.title,
                        body = notification.body,
                        imageUrl = notification.imageUrl
                    )
                    Log.d("MainViewModel", "Notification saved & dispatched successfully: $id")
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sending notification: ${e.message}")
                onResult(false)
            }
        }
    }

    fun deleteAppNotification(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val success = FirebaseService.deleteNotification(id)
                if (success) {
                    _notifications.value = _notifications.value.filter { it.id != id }
                    updateUnreadCount()
                    Log.d("MainViewModel", "Notification deleted successfully: $id")
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting notification: ${e.message}")
                onResult(false)
            }
        }
    }

    // --- Secure Sharing System ---

    fun generateShareLink(title: String, streamUrl: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val shareId = java.util.UUID.randomUUID().toString().substring(0, 8)
                val expiry = System.currentTimeMillis() + (10 * 60 * 60 * 1000) // 10 hours
                
                val shareData = mapOf(
                    "id" to shareId,
                    "title" to title,
                    "url" to streamUrl,
                    "expiry" to expiry,
                    "app" to "+90"
                )
                
                val success = FirebaseService.saveShareLink(shareId, shareData)
                
                if (success) {
                    val finalLink = "https://plus90.app/play?id=$shareId"
                    onResult(finalLink)
                } else {
                    onResult("")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating share link: ${e.message}")
                onResult("")
            }
        }
    }

    fun handleSharedLink(shareId: String, onPlay: (String, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = FirebaseService.getShareLink(shareId)
                if (data != null) {
                    val expiry = (data["expiry"] as? Number)?.toLong() ?: 0L
                    if (System.currentTimeMillis() < expiry) {
                        val url = data["url"] as? String ?: ""
                        val title = data["title"] as? String ?: "بث مشترك"
                        if (url.isNotEmpty()) {
                            onPlay(url, title)
                        }
                    } else {
                        Log.w("MainViewModel", "Share link expired")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error handling shared link: ${e.message}")
            }
        }
    }

    private val _showAiHelpChat = MutableStateFlow(false)
    val showAiHelpChat: StateFlow<Boolean> = _showAiHelpChat.asStateFlow()

    fun setAiHelpChatOpen(open: Boolean) {
        _showAiHelpChat.value = open
    }

    // --- REELS (ريلزات) SYSTEM FUNCTIONS ---

    fun setReelsTabVisibilityOnline(visible: Boolean) {
        _showReelsSection.value = visible
        viewModelScope.launch {
            FirebaseService.saveAppConfig("showReelsSection", visible)
        }
    }

    fun loadReels() {
        viewModelScope.launch {
            _isReelsLoading.value = true
            try {
                val rawList = ReelsService.fetchReels()
                // Smart feed logic: Filter out any duplicates and ensure stable ordering
                val distinctReels = rawList.distinctBy { it.id }
                
                // Smart ordering: Prioritize followed users, then latest
                val myUser = _chatUsername.value
                val sortedReels = if (myUser != null) {
                    distinctReels.sortedWith(compareByDescending<Reel> { 
                        _followingUsers.value.contains(it.username) 
                    }.thenByDescending { it.timestamp })
                } else {
                    distinctReels
                }
                _reelsList.value = sortedReels
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading Reels list", e)
            } finally {
                _isReelsLoading.value = false
            }
        }
    }

    fun postReel(
        videoUrl: String,
        description: String,
        hashtags: List<String>,
        durationSeconds: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        val username = _chatUsername.value
        val name = _chatName.value ?: username
        val avatar = _chatAvatar.value
        
        if (username.isNullOrBlank()) {
            onResult(false, "يجب تسجيل الدخول أولاً لتتمكن من النشر!")
            return
        }

        val maxDuration = _reelsSettings.value.maxVideoDuration
        if (durationSeconds > maxDuration) {
            onResult(false, "عذراً، الحد الأقصى لطول الفيديو هو $maxDuration ثانية!")
            return
        }

        viewModelScope.launch {
            try {
                var finalUrl = videoUrl.trim()
                if (finalUrl.startsWith("content://") || finalUrl.startsWith("file://")) {
                    val uploadedUrl = FirebaseStorageUtil.uploadFile(android.net.Uri.parse(finalUrl), "reels_videos")
                    if (uploadedUrl != null) {
                        finalUrl = uploadedUrl
                    } else {
                        onResult(false, "فشل رفع الفيديو، يرجى التحقق من اتصالك بالإنترنت")
                        return@launch
                    }
                }

                val success = ReelsService.postReel(
                    username = username,
                    name = name ?: "مستخدم",
                    avatar = avatar,
                    videoUrl = finalUrl,
                    description = description.trim(),
                    hashtags = hashtags.map { it.trim().removePrefix("#") },
                    durationSeconds = durationSeconds
                )
                if (success) {
                    loadReels()
                    onResult(true, "تم نشر الريلز بنجاح!")
                } else {
                    onResult(false, "فشل نشر الريلز، يرجى المحاولة لاحقاً")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error posting reel", e)
                onResult(false, "حدث خطأ غير متوقع أثناء النشر")
            }
        }
    }

    fun toggleLikeReel(reelId: String) {
        val username = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val currentList = _reelsList.value.toMutableList()
                val index = currentList.indexOfFirst { it.id == reelId }
                if (index != -1) {
                    val reel = currentList[index]
                    val wasLiked = reel.likedBy[username] == true
                    val isLikedNow = !wasLiked
                    
                    // Optimistic update
                    val newLikedBy = reel.likedBy.toMutableMap()
                    if (isLikedNow) {
                        newLikedBy[username] = true
                    } else {
                        newLikedBy.remove(username)
                    }
                    val updatedReel = reel.copy(
                        likedBy = newLikedBy,
                        likesCount = if (isLikedNow) reel.likesCount + 1 else maxOf(0, reel.likesCount - 1)
                    )
                    currentList[index] = updatedReel
                    _reelsList.value = currentList

                    // Push to server
                    ReelsService.toggleLikeReel(reelId, username, isLikedNow)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error toggling like", e)
            }
        }
    }

    fun loadReelComments(reelId: String) {
        viewModelScope.launch {
            try {
                val comments = ReelsService.fetchComments(reelId)
                _currentReelComments.value = comments
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading comments", e)
            }
        }
    }

    fun commentOnReel(reelId: String, text: String, onResult: (Boolean) -> Unit) {
        val username = _chatUsername.value
        val name = _chatName.value ?: username
        val avatar = _chatAvatar.value
        if (username.isNullOrBlank() || text.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val success = ReelsService.postComment(
                    reelId = reelId,
                    username = username,
                    name = name ?: "مستخدم",
                    avatar = avatar,
                    text = text.trim()
                )
                if (success) {
                    loadReelComments(reelId)
                    // Refresh count in reelsList
                    val currentList = _reelsList.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.id == reelId }
                    if (idx != -1) {
                        currentList[idx] = currentList[idx].copy(commentsCount = currentList[idx].commentsCount + 1)
                        _reelsList.value = currentList
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error commenting on reel", e)
                onResult(false)
            }
        }
    }

    fun deleteReelComment(reelId: String, commentId: String, commentOwner: String, reelOwner: String) {
        val myUser = _chatUsername.value ?: return
        // Allow deletion if comment belongs to user or user owns the reel
        if (myUser != commentOwner && myUser != reelOwner) return

        viewModelScope.launch {
            try {
                val success = ReelsService.deleteComment(reelId, commentId)
                if (success) {
                    loadReelComments(reelId)
                    val currentList = _reelsList.value.toMutableList()
                    val idx = currentList.indexOfFirst { it.id == reelId }
                    if (idx != -1) {
                        currentList[idx] = currentList[idx].copy(commentsCount = maxOf(0, currentList[idx].commentsCount - 1))
                        _reelsList.value = currentList
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting comment", e)
            }
        }
    }

    fun replyToComment(reelId: String, commentId: String, text: String, onResult: (Boolean) -> Unit) {
        val username = _chatUsername.value
        val name = _chatName.value ?: username
        val avatar = _chatAvatar.value
        if (username.isNullOrBlank() || text.isBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val success = ReelsService.postReply(
                    reelId = reelId,
                    commentId = commentId,
                    username = username,
                    name = name ?: "مستخدم",
                    avatar = avatar,
                    text = text.trim()
                )
                if (success) {
                    loadReelComments(reelId)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error replying to comment", e)
                onResult(false)
            }
        }
    }

    fun toggleFollowUser(otherUsername: String) {
        val myUser = _chatUsername.value ?: return
        if (myUser == otherUsername) return

        viewModelScope.launch {
            try {
                val isFollowingNow = !_followingUsers.value.contains(otherUsername)
                val success = ReelsService.toggleFollow(myUser, otherUsername, isFollowingNow)
                if (success) {
                    val current = _followingUsers.value.toMutableSet()
                    if (isFollowingNow) current.add(otherUsername) else current.remove(otherUsername)
                    _followingUsers.value = current
                    // Reload reels to apply smart sorting
                    loadReels()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error toggling follow status", e)
            }
        }
    }

    fun loadCreatorProfile(username: String, onResult: (followers: Int, following: Int, posts: List<Reel>) -> Unit) {
        viewModelScope.launch {
            try {
                val followers = ReelsService.getFollowersCount(username)
                val following = ReelsService.getFollowingCount(username)
                val all = ReelsService.fetchReels()
                val userReels = all.filter { it.username == username }
                onResult(followers, following, userReels)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading profile details for $username", e)
                onResult(0, 0, emptyList())
            }
        }
    }

    fun incrementShareCopyCount(reelId: String) {
        viewModelScope.launch {
            try {
                val newCount = ReelsService.incrementShareCopyCount(reelId)
                val currentList = _reelsList.value.toMutableList()
                val idx = currentList.indexOfFirst { it.id == reelId }
                if (idx != -1) {
                    currentList[idx] = currentList[idx].copy(shareCopiesCount = newCount)
                    _reelsList.value = currentList
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error incrementing copy count", e)
            }
        }
    }

    fun saveReelsSettingsAdmin(maxDuration: Int, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val current = _reelsSettings.value.copy(
                    maxVideoDuration = maxDuration,
                    isReelsEnabled = enabled
                )
                val success = ReelsService.updateSettings(current)
                if (success) {
                    _reelsSettings.value = current
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving Reels Settings", e)
            }
        }
    }

    fun deleteReelAdmin(reelId: String) {
        viewModelScope.launch {
            try {
                val success = ReelsService.deleteReel(reelId)
                if (success) {
                    loadReels()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting Reel as admin", e)
            }
        }
    }

    fun initFollowingList() {
        val myUser = _chatUsername.value ?: return
        viewModelScope.launch {
            try {
                val reels = ReelsService.fetchReels()
                val following = mutableSetOf<String>()
                reels.forEach { reel ->
                    if (ReelsService.isFollowingUser(myUser, reel.username)) {
                        following.add(reel.username)
                    }
                }
                _followingUsers.value = following
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error syncing following users list", e)
            }
        }
    }

    fun registerReelsUserFromChat(username: String, name: String, pass: String, avatar: String) {
        // Automatically sync chat user registration with the secondary database if they use the alternative login/signup
        _chatUsername.value = username
        _chatName.value = name
        _chatAvatar.value = avatar
        
        prefs.edit().apply {
            putString("chat_username", username)
            putString("chat_name", name)
            putString("chat_password", pass)
            putString("chat_avatar", avatar)
            apply()
        }

        viewModelScope.launch {
            try {
                ChatService.registerUser(username, name, pass, avatar)
                initFollowingList()
                loadReels()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error sync registering reels/chat user", e)
            }
        }
    }

    // --- Home Hub & Promotional Ads Data Management ---

    fun loadHomeData() {
        viewModelScope.launch {
            try {
                val ads = FirebaseService.getPromoAds()
                if (ads.isNotEmpty()) {
                    _promoAds.value = ads
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading promo ads: ${e.message}")
            }

            try {
                val banners = FirebaseService.getHomeHeroBanners()
                if (banners.isNotEmpty()) {
                    _homeHeroBanners.value = banners
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading home hero banners: ${e.message}")
            }

            try {
                val config = FirebaseService.getHomeConfig()
                _homeConfig.value = config
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading home config: ${e.message}")
            }
        }
    }

    fun savePromoAd(ad: PromoAd, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = ad.id.ifEmpty { "ad_${System.currentTimeMillis()}" }
            val finalAd = ad.copy(id = id)
            val success = FirebaseService.savePromoAd(finalAd)
            if (success) {
                _promoAds.value = listOf(finalAd) + _promoAds.value.filter { it.id != id }
            } else {
                // Keep locally updated as fallback
                _promoAds.value = listOf(finalAd) + _promoAds.value.filter { it.id != id }
            }
            onComplete(true)
        }
    }

    fun deletePromoAd(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = FirebaseService.deletePromoAd(id)
            _promoAds.value = _promoAds.value.filter { it.id != id }
            onComplete(success)
        }
    }

    fun saveHomeHeroBanner(banner: HomeHeroBanner, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = banner.id.ifEmpty { "banner_${System.currentTimeMillis()}" }
            val finalBanner = banner.copy(id = id)
            val success = FirebaseService.saveHomeHeroBanner(finalBanner)
            if (success) {
                _homeHeroBanners.value = (listOf(finalBanner) + _homeHeroBanners.value.filter { it.id != id }).sortedBy { it.order }
            } else {
                _homeHeroBanners.value = (listOf(finalBanner) + _homeHeroBanners.value.filter { it.id != id }).sortedBy { it.order }
            }
            onComplete(true)
        }
    }

    fun deleteHomeHeroBanner(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = FirebaseService.deleteHomeHeroBanner(id)
            _homeHeroBanners.value = _homeHeroBanners.value.filter { it.id != id }
            onComplete(success)
        }
    }

    fun updateHomeConfig(config: HomeSectionConfig, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = FirebaseService.saveHomeConfig(config)
            _homeConfig.value = config
            onComplete(success)
        }
    }
}

