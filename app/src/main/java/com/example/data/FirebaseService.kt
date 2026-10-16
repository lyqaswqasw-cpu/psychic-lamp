package com.example.data
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.GlobalScope

import android.util.Log
import com.example.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import com.example.ui.AiApiKey
import com.example.utils.AppVersionConfig
import com.example.utils.AppVersionManager
import com.example.utils.AppVersionStatus

// Extension helper to use Coroutines with Firebase Task API safely
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (result != null) {
            cont.resume(result)
        } else {
            cont.resumeWithException(NullPointerException("Firebase request returned null"))
        }
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }

}
object FirebaseService {
    private const val TAG = "FirebaseService"
    const val RTDB_URL = "https://loop-7e3d9-default-rtdb.firebaseio.com"
    const val RTDB_SECURE_PREFIX = "v2_secured"

    private val apiDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 128
        maxRequestsPerHost = 32
    }
    private val apiConnectionPool = okhttp3.ConnectionPool(32, 5, java.util.concurrent.TimeUnit.MINUTES)

    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(apiDispatcher)
        .connectionPool(apiConnectionPool)
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            val urlStr = original.url.toString()
            if (!urlStr.contains("firebase") && !urlStr.contains("firestore")) {
                if (original.header("User-Agent") == null) {
                    requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
            } else {
                requestBuilder.header("X-App-Version", AppVersionManager.getEffectiveAppVersion())
                requestBuilder.header("X-App-Client", "LoopLive-Android-v2")
                requestBuilder.header("X-Security-Gate", "v2.0.0-verified")
            }
            requestBuilder.header("Accept", "*/*")
            chain.proceed(requestBuilder.build())
        }
        .build()

    // Ultra-fast HTTP client for metadata, API verification, and category/channel list retrieval
    val apiClient = OkHttpClient.Builder()
        .dispatcher(apiDispatcher)
        .connectionPool(apiConnectionPool)
        .proxy(java.net.Proxy.NO_PROXY)
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            val urlStr = original.url.toString()
            if (!urlStr.contains("firebase") && !urlStr.contains("firestore")) {
                if (original.header("User-Agent") == null) {
                    requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
            } else {
                requestBuilder.header("X-App-Version", AppVersionManager.getEffectiveAppVersion())
                requestBuilder.header("X-App-Client", "LoopLive-Android-v2")
                requestBuilder.header("X-Security-Gate", "v2.0.0-verified")
            }
            requestBuilder.header("Accept", "*/*")
            chain.proceed(requestBuilder.build())
        }
        .build()

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Local cached lists for instantaneous access
    val localCategoriesList = listOf(
        IPTVCategory("sports", "القنوات الرياضية", "channel"),
        IPTVCategory("news", "قنوات الأخبار بث مباشر", "channel"),
        IPTVCategory("action", "أفلام الأكشن والإثارة", "movie"),
        IPTVCategory("scientific", "أفلام وثائقية وعلمية", "movie"),
        IPTVCategory("entertainment", "مسلسلات الدراما الحصرية", "series")
    )

    val localChannelsList = listOf(
        Channel(
            id = "chan_bein_1",
            name = "beIN SPORTS Premium HD",
            logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=150&q=80",
            streamUrl = "http://sample.vodobox.com/planete_hd_short_clip/planete_hd_short_clip.m3u8",
            category = "sports"
        ),
        Channel(
            id = "chan_aljazeera",
            name = "الجزيرة الإخبارية مباشر HD",
            logoUrl = "https://images.unsplash.com/photo-1585829365294-bb7c6384ef81?w=150&q=80",
            streamUrl = "https://live-aljazeera.akamaized.net/hls/live/2003940/aljazeera/index.m3u8",
            category = "news"
        ),
        Channel(
            id = "chan_alarabiya",
            name = "العربية مباشر HD",
            logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=150&q=80",
            streamUrl = "https://alarabiya-live.akamaized.net/hls/live/2004419/alarabiya/index.m3u8",
            category = "news"
        )
    )

    val localMoviesList = listOf(
        Movie(
            id = "movie_sintel",
            title = "فيلم الخيال العلمي: سينتيل (Sintel)",
            description = "فيلم رسومي فاخر وجميل بتقنية 4K يحكي قصة فتاة تبحث عن تنينها الصغير الضائع في رحلة ملحمية مذهلة.",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80",
            streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8",
            category = "scientific",
            duration = "00:15:00",
            rating = 8.9,
            year = 2025
        ),
        Movie(
            id = "movie_bipbop",
            title = "وثائقي التقنية والشبكات: بيب بوب (BipBop)",
            description = "عرض واقعي وتجريبي لأنظمة البث المتقدمة في السيرفرات العالمية بطريقة تقنية مثيرة للغاية.",
            posterUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&q=80",
            streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
            category = "scientific",
            duration = "00:30:15",
            rating = 7.4,
            year = 2024
        )
    )

    val localSeriesList = listOf(
        Series(
            id = "series_thrones",
            title = "صراع العروش (Game of Thrones)",
            description = "صراع ملحمي بين العائلات النبيلة للسيطرة على العرش الحديدي في الممالك السبع.",
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&q=80",
            category = "entertainment",
            rating = 9.3,
            year = 2011
        )
    )

    private suspend fun getDeletedIds(collection: String): Set<String> = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/deleted_items/$collection.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableSetOf<String>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            if (json.optBoolean(key, false)) {
                                result.add(key)
                            }
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting deleted IDs for $collection", e)
        }
        return@withContext emptySet()
    }

    suspend fun getAppConfig(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/app_config.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val map = mutableMapOf<String, Boolean>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = json.optBoolean(key, true)
                        }
                        // Default fallback values if not present in DB
                        if (!map.containsKey("showChatTab")) map["showChatTab"] = true
                        if (!map.containsKey("showEventChannelsTab")) map["showEventChannelsTab"] = true
                        if (!map.containsKey("showXtreamDirectLogin")) map["showXtreamDirectLogin"] = true
                        if (!map.containsKey("showNotificationIcon")) map["showNotificationIcon"] = true
                        if (!map.containsKey("is_share_enabled")) map["is_share_enabled"] = true
                        if (!map.containsKey("showAiSectionInDrawer")) map["showAiSectionInDrawer"] = true
                        if (!map.containsKey("showReelsSection")) map["showReelsSection"] = true
                        if (!map.containsKey("isGlobalAdsEnabled")) map["isGlobalAdsEnabled"] = true
                        if (!map.containsKey("showRemoveAdsIcon")) map["showRemoveAdsIcon"] = true
                        if (!map.containsKey("isVpnBlockEnabled")) map["isVpnBlockEnabled"] = true
                        return@withContext map
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "App config fetch offline or unavailable, using defaults")
        }
        return@withContext mapOf(
            "showChatTab" to true,
            "showEventChannelsTab" to true,
            "showXtreamDirectLogin" to true,
            "showNotificationIcon" to true,
            "is_share_enabled" to true,
            "showAiSectionInDrawer" to true,
            "showReelsSection" to true,
            "isGlobalAdsEnabled" to true,
            "showRemoveAdsIcon" to true,
            "isVpnBlockEnabled" to true
        )
    }

    /**
     * Retrieves the remote App Version Control and Maintenance configuration from Firebase RTDB,
     * with an automated Firestore fallback.
     */
    suspend fun getAppVersionConfig(): AppVersionConfig = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/app_version_control.json"
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        return@withContext AppVersionManager.parseConfigFromJson(json)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting app version config from RTDB: ${e.message}")
        }

        // Firestore fallback with quick timeout
        try {
            val doc = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                db.collection("app_config").document("version_control").get().await()
            }
            if (doc != null && doc.exists()) {
                val currentVersion = doc.getString("currentVersion") ?: "2.0.0"
                val minimumVersion = doc.getString("minimumVersion") ?: "2.0.0"
                val forceUpdate = doc.getBoolean("forceUpdate") ?: true
                val appEnabled = doc.getBoolean("appEnabled") ?: true
                val updateUrl = doc.getString("updateUrl") ?: "https://t.me/plus90_official"
                val updateTitle = doc.getString("updateTitle") ?: "تحديث جديد مطلوب 🚀"
                val updateMessage = doc.getString("updateMessage") ?: "هذه النسخة من التطبيق لم تعد مدعومة."
                val maintenanceTitle = doc.getString("maintenanceTitle") ?: "التطبيق متوقف مؤقتًا للصيانة 🛠️"
                val maintenanceMessage = doc.getString("maintenanceMessage") ?: "نعمل حالياً على صيانة الخوادم وتحديث البث."
                val telegramSupportUrl = doc.getString("telegramSupportUrl") ?: "https://t.me/plus90_official"

                return@withContext AppVersionConfig(
                    currentVersion = currentVersion,
                    minimumVersion = minimumVersion,
                    forceUpdate = forceUpdate,
                    appEnabled = appEnabled,
                    updateUrl = updateUrl,
                    updateTitle = updateTitle,
                    updateMessage = updateMessage,
                    maintenanceTitle = maintenanceTitle,
                    maintenanceMessage = maintenanceMessage,
                    telegramSupportUrl = telegramSupportUrl
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore fallback for version_control info: ${e.message}")
        }

        return@withContext AppVersionConfig()
    }

    /**
     * Admin method to push and synchronize Version Control & Maintenance settings across RTDB and Firestore.
     */
    suspend fun saveAppVersionConfig(config: AppVersionConfig): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/app_version_control.json"
        val map = AppVersionManager.configToMap(config)
        var rtdbSuccess = false
        try {
            val json = JSONObject(map)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response ->
                rtdbSuccess = response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving version config to RTDB", e)
        }

        try {
            db.collection("app_config").document("version_control").set(map).await()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore save version_control info: ${e.message}")
        }

        return@withContext rtdbSuccess
    }

    suspend fun verifyXtreamCredentials(host: String, username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val formattedHost = formatHostUrl(host)
        val url = "$formattedHost/player_api.php?username=$username&password=$password"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "IPTVSmartersPlayer")
                .build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("\"auth\":1") || body.contains("\"status\":\"Active\"") || body.contains("\"user_info\"")) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Xtream credentials", e)
        }
        return@withContext false
    }

    suspend fun saveAppConfig(key: String, value: Any): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/app_config.json"
        try {
            val current = (getAppConfigMap() ?: emptyMap()).toMutableMap()
            current[key] = value
            val json = JSONObject().apply {
                current.forEach { (k, v) -> put(k, v) }
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving app config to RTDB", e)
        }
        return@withContext false
    }

    suspend fun saveShareLink(shareId: String, data: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/secure_shares/$shareId.json"
        try {
            val json = JSONObject(data)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving share link to RTDB", e)
        }
        return@withContext false
    }

    suspend fun getShareLink(shareId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/secure_shares/$shareId.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableMapOf<String, Any>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            result[key] = json.get(key)
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting share link from RTDB", e)
        }
        return@withContext null
    }

    suspend fun getAppConfigMap(): Map<String, Any>? = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/app_config.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableMapOf<String, Any>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            result[key] = json.get(key)
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "App config map fetch offline or unavailable")
        }
        return@withContext null
    }

    suspend fun updateEventChannel(id: String, data: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/events/$id.json"
        val legacyUrl = "$RTDB_URL/events/$id.json"
        try {
            val json = JSONObject(data)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val secureReq = Request.Builder().url(secureUrl).put(body).build()
            val res = okHttpClient.newCall(secureReq).execute().use { response -> response.isSuccessful }
            try {
                val legacyReq = Request.Builder().url(legacyUrl).put(body).build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }
            return@withContext res
        } catch (e: Exception) {
            Log.e(TAG, "Error updating main channel in RTDB", e)
        }
        return@withContext false
    }

    suspend fun deleteEventChannel(id: String): Boolean = withContext(Dispatchers.IO) {
        var anySuccess = false
        val urls = listOf(
            "$RTDB_URL/events/$id.json",
            "$RTDB_URL/$RTDB_SECURE_PREFIX/events/$id.json",
            "https://loop-web-7e263-default-rtdb.firebaseio.com/events/$id.json"
        )
        for (u in urls) {
            try {
                val req = Request.Builder().url(u).delete().build()
                val res = okHttpClient.newCall(req).execute().use { it.isSuccessful }
                if (res) anySuccess = true
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            db.collection("events").document(id).delete().await()
            anySuccess = true
        } catch (e: Exception) {
            // Ignore
        }
        return@withContext anySuccess
    }

    suspend fun getPublishedPackages(): List<PublishedPackage> = withContext(Dispatchers.IO) {
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/published_packages.json"
        val legacyUrl = "$RTDB_URL/published_packages.json"
        
        // 1. Try secure v2 node
        val secureList = tryFetchPackagesFromUrl(secureUrl)
        if (secureList.isNotEmpty()) {
            return@withContext secureList
        }

        // 2. Try legacy node and sync
        val legacyList = tryFetchPackagesFromUrl(legacyUrl)
        if (legacyList.isNotEmpty()) {
            for (pkg in legacyList) {
                savePublishedPackage(pkg)
            }
            return@withContext legacyList
        }
        return@withContext emptyList()
    }

    private fun tryFetchPackagesFromUrl(url: String): List<PublishedPackage> {
        val result = mutableListOf<PublishedPackage>()
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.getJSONObject(key)
                            result.add(PublishedPackage(
                                id = key,
                                name = obj.optString("name"),
                                m3uUrl = obj.optString("m3uUrl"),
                                imageUrl = obj.optString("imageUrl"),
                                description = obj.optString("description")
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting published packages from $url: ${e.message}")
        }
        return result
    }

    suspend fun savePublishedPackage(pkg: PublishedPackage): Boolean = withContext(Dispatchers.IO) {
        val id = if (pkg.id.isEmpty()) "pkg_${System.currentTimeMillis()}" else pkg.id
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/published_packages/$id.json"
        val legacyUrl = "$RTDB_URL/published_packages/$id.json"
        try {
            val data = JSONObject().apply {
                put("name", pkg.name)
                put("m3uUrl", pkg.m3uUrl)
                put("imageUrl", pkg.imageUrl)
                put("description", pkg.description)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = data.toString().toRequestBody(mediaType)
            val secureReq = Request.Builder().url(secureUrl).put(body).build()
            val res = okHttpClient.newCall(secureReq).execute().use { response -> response.isSuccessful }
            try {
                val legacyReq = Request.Builder().url(legacyUrl).put(body).build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }
            return@withContext res
        } catch (e: Exception) {
            Log.e(TAG, "Error saving published package to RTDB", e)
        }
        return@withContext false
    }

    suspend fun deletePublishedPackage(id: String): Boolean = withContext(Dispatchers.IO) {
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/published_packages/$id.json"
        val legacyUrl = "$RTDB_URL/published_packages/$id.json"
        try {
            val secureReq = Request.Builder().url(secureUrl).delete().build()
            val res = okHttpClient.newCall(secureReq).execute().use { response -> response.isSuccessful }
            try {
                val legacyReq = Request.Builder().url(legacyUrl).delete().build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }
            return@withContext res
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting published package from RTDB", e)
        }
        return@withContext false
    }

    // Static Memory Cache to yield ultra-rapid, instant UI retrieval
    private var lastCachedHost = ""
    private var lastCachedUsername = ""
    private var cachedCategories: List<IPTVCategory>? = null
    private var cachedChannels: List<Channel>? = null
    private var cachedMovies: List<Movie>? = null
    private var cachedSeries: List<Series>? = null

    fun clearCache() {
        lastCachedHost = ""
        lastCachedUsername = ""
        cachedCategories = null
        cachedChannels = null
        cachedMovies = null
        cachedSeries = null
    }

    private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
        val userAgents = listOf(
            "IPTVSmartersPro",
            "Smarters Pro",
            "TiviMate/4.7.0 (Linux; Android 11)",
            "Dalvik/2.1.0 (Linux; U; Android 12)",
            "VLC/3.0.18 LibVLC/3.0.18"
        )
        var lastException: Exception? = null
        for (ua in userAgents) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", ua)
                    .header("Accept", "*/*")
                    .build()
                apiClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.string() ?: ""
                    } else if (response.code == 404 || response.code == 401 || response.code == 403) {
                        throw IOException("HTTP ${response.code}: Endpoint unavailable on server")
                    } else {
                        throw IOException("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("Endpoint unavailable") == true) {
                    break // Do not keep trying different UAs for 404/401/403
                }
                Log.w(TAG, "httpGet failed with User-Agent [$ua] for $url: ${e.message}. Retrying next...")
            }
        }
        throw lastException ?: IOException("Unknown error in httpGet")
    }

    private suspend fun <T> httpGetStream(url: String, block: (java.io.Reader) -> T): T = withContext(Dispatchers.IO) {
        val userAgents = listOf(
            "IPTVSmartersPro",
            "Smarters Pro",
            "TiviMate/4.7.0 (Linux; Android 11)",
            "Dalvik/2.1.0 (Linux; U; Android 12)",
            "VLC/3.0.18 LibVLC/3.0.18"
        )
        var lastException: Exception? = null
        for (ua in userAgents) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", ua)
                    .header("Accept", "*/*")
                    .build()
                apiClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body ?: throw IOException("Empty response body")
                        val bufferedStream = java.io.BufferedReader(body.charStream(), 65536)
                        bufferedStream.use { reader ->
                            return@withContext block(reader)
                        }
                    } else if (response.code == 404 || response.code == 401 || response.code == 403) {
                        throw IOException("HTTP ${response.code}: Endpoint unavailable on server")
                    } else {
                        throw IOException("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("Endpoint unavailable") == true) {
                    break // Do not keep trying different UAs for 404/401/403
                }
                Log.w(TAG, "httpGetStream failed with User-Agent [$ua] for $url: ${e.message}. Retrying next...")
            }
        }
        throw lastException ?: IOException("Unknown error in httpGetStream")
    }

    private fun nextStringOrCoerce(reader: android.util.JsonReader): String {
        return try {
            when (reader.peek()) {
                android.util.JsonToken.STRING, android.util.JsonToken.NUMBER -> reader.nextString()
                android.util.JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                android.util.JsonToken.NULL -> { reader.nextNull(); "" }
                else -> { reader.skipValue(); "" }
            }
        } catch (e: Exception) {
            try { reader.skipValue() } catch (ex: Exception) {}
            ""
        }
    }

    private fun nextDoubleOrCoerce(reader: android.util.JsonReader): Double {
        return try {
            when (reader.peek()) {
                android.util.JsonToken.NUMBER, android.util.JsonToken.STRING -> {
                    val str = reader.nextString()
                    str.toDoubleOrNull() ?: 0.0
                }
                android.util.JsonToken.BOOLEAN -> {
                    reader.skipValue()
                    0.0
                }
                android.util.JsonToken.NULL -> { reader.nextNull(); 0.0 }
                else -> { reader.skipValue(); 0.0 }
            }
        } catch (e: Exception) {
            try { reader.skipValue() } catch (ex: Exception) {}
            0.0
        }
    }

    private fun formatHostUrl(host: String): String {
        var formatted = host.trim()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://$formatted"
        }
        // Remove trailing player_api.php, get.php, or slash segments that might have been typed into host
        val suffixesToRemove = listOf(
            "/player_api.php",
            "/get.php",
            "/c/",
            "/c",
            "/live/",
            "/live",
            "/vod/",
            "/vod",
            "/series/",
            "/series"
        )
        for (suffix in suffixesToRemove) {
            if (formatted.endsWith(suffix, ignoreCase = true)) {
                formatted = formatted.substring(0, formatted.length - suffix.length)
            }
        }
        while (formatted.endsWith("/")) {
            formatted = formatted.substring(0, formatted.length - 1)
        }
        return formatted
    }

    private suspend fun fetchActivationCodeRtdb(code: String): ActivationCode? = withContext(Dispatchers.IO) {
        val codesToTry = listOf(code.trim(), code.trim().uppercase()).distinct()
        for (c in codesToTry) {
            val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/activation_codes/$c.json"
            val legacyUrl = "$RTDB_URL/activation_codes/$c.json"
            
            // 1. Try secure v2 node first
            val secureCode = tryFetchCodeFromUrl(secureUrl, c)
            if (secureCode != null) {
                Log.d(TAG, "Successfully retrieved activation code $c from SECURED RTDB node")
                return@withContext secureCode
            }

            // 2. Fallback to legacy node if available
            val legacyCode = tryFetchCodeFromUrl(legacyUrl, c)
            if (legacyCode != null) {
                Log.d(TAG, "Retrieved activation code $c from legacy RTDB node")
                return@withContext legacyCode
            }
        }
        null
    }

    private fun tryFetchCodeFromUrl(url: String, codeKey: String): ActivationCode? {
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val host = json.optString("host", "")
                        val username = json.optString("username", "")
                        val password = json.optString("password", "")
                        val forcedFormat = json.optString("forcedFormat", "")
                        val allowedTypesList = mutableListOf<String>()
                        val typesArr = json.optJSONArray("allowedTypes")
                        if (typesArr != null) {
                            for (i in 0 until typesArr.length()) {
                                val strVal = typesArr.optString(i)
                                if (!strVal.isNullOrEmpty()) {
                                    allowedTypesList.add(strVal)
                                }
                            }
                        }
                        val m3uUrl = json.optString("m3uUrl", "")
                        if (allowedTypesList.isEmpty()) {
                            allowedTypesList.addAll(listOf("live", "movies", "series"))
                        }
                        if (m3uUrl.isNotEmpty() || (host.isNotEmpty() && (username.isNotEmpty() || host.contains(".m3u") || host.contains("http")))) {
                            return ActivationCode(
                                code = codeKey,
                                host = host,
                                username = username,
                                password = password,
                                forcedFormat = forcedFormat,
                                allowedTypes = allowedTypesList,
                                m3uUrl = m3uUrl
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch code from $url failed: ${e.message}")
        }
        return null
    }

    private suspend fun fetchVipCodeDirect(code: String): VipCode? = withContext(Dispatchers.IO) {
        val trimmed = code.trim().uppercase()
        val urls = listOf(
            "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$trimmed.json",
            "$RTDB_URL/vip_codes/$trimmed.json"
        )
        for (u in urls) {
            try {
                val req = Request.Builder().url(u).build()
                apiClient.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        if (body.isNotEmpty() && body != "null") {
                            val json = JSONObject(body)
                            val allowedTypesList = mutableListOf<String>()
                            val typesArr = json.optJSONArray("allowedTypes")
                            if (typesArr != null) {
                                for (i in 0 until typesArr.length()) {
                                    val strVal = typesArr.optString(i)
                                    if (strVal.isNotEmpty()) allowedTypesList.add(strVal)
                                }
                            }
                            if (allowedTypesList.isEmpty()) allowedTypesList.addAll(listOf("live", "movies", "series"))
                            return@withContext VipCode(
                                code = trimmed,
                                host = json.optString("host"),
                                username = json.optString("username"),
                                password = json.optString("password"),
                                forcedFormat = json.optString("forcedFormat"),
                                maxUsers = json.optInt("maxUsers", 1),
                                currentUsers = json.optInt("currentUsers", 0),
                                lastActive = json.optLong("lastActive", 0),
                                lastActiveSessionId = json.optString("lastActiveSessionId", ""),
                                allowedTypes = allowedTypesList,
                                allowVpn = json.optBoolean("allowVpn", false)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching VIP code direct from $u: ${e.message}")
            }
        }
        null
    }

    private suspend fun fetchActivationCodeRest(code: String): ActivationCode? {
        return coroutineScope {
            val trimmedCode = code.trim()
            val projectIds = listOf("loop-7e3d9", "loop-96ca6")
            val collections = listOf("activation_codes", "activation_code")
            val codesToTry = listOf(trimmedCode, trimmedCode.uppercase()).distinct()

            val deferreds = mutableListOf<kotlinx.coroutines.Deferred<ActivationCode?>>()

            for (projId in projectIds) {
                for (colName in collections) {
                    for (c in codesToTry) {
                        val deferred = async(Dispatchers.IO) {
                            try {
                                val url = "https://firestore.googleapis.com/v1/projects/$projId/databases/(default)/documents/$colName/$c"
                                Log.d(TAG, "Fetching from REST URL (Concurrent): $url")
                                val request = Request.Builder().url(url).build()
                                apiClient.newCall(request).execute().use { response ->
                                    if (response.isSuccessful) {
                                        val bodyStr = response.body?.string() ?: ""
                                        if (bodyStr.isNotEmpty()) {
                                            val json = JSONObject(bodyStr)
                                            if (json.has("fields")) {
                                                val fields = json.getJSONObject("fields")
                                                
                                                val hostObj = fields.optJSONObject("host")
                                                val host = hostObj?.optString("stringValue") ?: ""
                                                
                                                val usernameObj = fields.optJSONObject("username")
                                                val username = usernameObj?.optString("stringValue") ?: ""
                                                
                                                val passwordObj = fields.optJSONObject("password")
                                                val password = passwordObj?.optString("stringValue") ?: ""
                                                
                                                val forcedFormatObj = fields.optJSONObject("forcedFormat")
                                                val forcedFormat = forcedFormatObj?.optString("stringValue") ?: ""
                                                
                                                val m3uUrlObj = fields.optJSONObject("m3uUrl")
                                                val m3uUrl = m3uUrlObj?.optString("stringValue") ?: ""
                                                
                                                val allowedTypesList = mutableListOf<String>()
                                                val allowedTypesObj = fields.optJSONObject("allowedTypes")
                                                val arrayValue = allowedTypesObj?.optJSONObject("arrayValue")
                                                val valuesArray = arrayValue?.optJSONArray("values")
                                                if (valuesArray != null) {
                                                    for (i in 0 until valuesArray.length()) {
                                                        val item = valuesArray.optJSONObject(i)
                                                        val strVal = item?.optString("stringValue")
                                                        if (strVal != null) {
                                                            allowedTypesList.add(strVal)
                                                        }
                                                    }
                                                }
                                                if (allowedTypesList.isEmpty()) {
                                                    allowedTypesList.addAll(listOf("live", "movies", "series"))
                                                }
                                                
                                                Log.d(TAG, "Successfully retrieved activation code $c from collection $colName via Concurrent REST from $projId!")
                                                return@async ActivationCode(
                                                    code = c,
                                                    host = host,
                                                    username = username,
                                                    password = password,
                                                    forcedFormat = forcedFormat,
                                                    allowedTypes = allowedTypesList,
                                                    m3uUrl = m3uUrl
                                                )
                                            }
                                        }
                                    } else {
                                        Log.w(TAG, "Concurrent REST call failed for code $c on $projId ($colName): HTTP ${response.code}")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Concurrent REST fetch handled")
                            }
                            null
                        }
                        deferreds.add(deferred)
                    }
                }
            }

            val results = deferreds.awaitAll()
            results.firstOrNull { it != null }
        }
    }

    /**
     * Searches for the document with ID equal to 'code' inside the collection 'activation_codes' or 'activation_code' in RTDB, Firestore REST, and Firestore SDK.
     */
    suspend fun verifyActivationCode(code: String): Any? {
        val trimmedCode = code.trim().uppercase()

        // Strict Remote Gatekeeper: Check app operational status and version requirements first
        try {
            val versionConfig = getAppVersionConfig()
            val status = AppVersionManager.evaluate(versionConfig, AppVersionManager.CURRENT_APP_VERSION)
            if (status is AppVersionStatus.Maintenance || status is AppVersionStatus.UpdateRequired) {
                Log.w(TAG, "Activation code query rejected by AppVersionGate: status=$status")
                return null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Notice during version check before code verification: ${e.message}")
        }

        // 0. Try Direct VIP Code lookup first (fastest, single request)
        try {
            val vipDirect = fetchVipCodeDirect(trimmedCode)
            if (vipDirect != null) {
                return vipDirect
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct VIP code verification failed for $code: ${e.message}")
        }

        // 1. Try Realtime Database (RTDB) direct lookup - Fast, reliable & public rules
        try {
            val rtdbResult = fetchActivationCodeRtdb(code)
            if (rtdbResult != null) {
                return rtdbResult
            }
        } catch (t: Throwable) {
            Log.w(TAG, "RTDB verification failed for $code: ${t.message}")
        }

        // 2. Try Firestore REST API
        try {
            val restResult = fetchActivationCodeRest(code)
            if (restResult != null) {
                return restResult
            }
        } catch (t: Throwable) {
            Log.w(TAG, "REST verification failed for $code, falling back to Firestore SDK", t)
        }

        // 3. SDK Fallback with strict timeout
        return try {
            kotlinx.coroutines.withTimeoutOrNull(4000L) {
                val trimmed = code.trim()
                val collections = listOf("activation_codes", "activation_code")
                val codes = listOf(trimmed, trimmed.uppercase()).distinct()
                
                for (colName in collections) {
                    for (c in codes) {
                        val doc = db.collection(colName).document(c).get().await()
                        if (doc.exists()) {
                            val host = doc.getString("host") ?: ""
                            val username = doc.getString("username") ?: ""
                            val password = doc.getString("password") ?: ""
                            val forcedFormat = doc.getString("forcedFormat") ?: ""
                            val m3uUrl = doc.getString("m3uUrl") ?: ""
                            val allowedTypesRaw = doc.get("allowedTypes") as? List<String>
                            val allowedTypesList = if (!allowedTypesRaw.isNullOrEmpty()) allowedTypesRaw else listOf("live", "movies", "series")
                            return@withTimeoutOrNull ActivationCode(
                                code = doc.id,
                                host = host,
                                username = username,
                                password = password,
                                forcedFormat = forcedFormat,
                                allowedTypes = allowedTypesList,
                                m3uUrl = m3uUrl
                            )
                        }
                    }
                }
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Notice: Activation code $code check completed: ${e.message}")
            null
        }
    }

    suspend fun fetchXtreamCategories(host: String, username: String, password: String): List<IPTVCategory> = withContext(Dispatchers.IO) {
        if (host == lastCachedHost && username == lastCachedUsername && cachedCategories != null) {
            Log.d(TAG, "Returning cached categories instantly")
            return@withContext cachedCategories!!
        }
        try {
            val formattedHost = formatHostUrl(host)
            
            val liveUrl = "$formattedHost/player_api.php?username=$username&password=$password&action=get_live_categories"
            val vodUrl = "$formattedHost/player_api.php?username=$username&password=$password&action=get_vod_categories"
            val seriesUrl = "$formattedHost/player_api.php?username=$username&password=$password&action=get_series_categories"

            coroutineScope {
                val liveDeferred = async {
                    try {
                        httpGetStream(liveUrl) { reader -> parseCategoriesStream(reader, "channel") }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Live categories unavailable on server (${e.message})")
                        emptyList<IPTVCategory>()
                    }
                }

                val vodDeferred = async {
                    try {
                        httpGetStream(vodUrl) { reader -> parseCategoriesStream(reader, "movie") }
                    } catch (e: Throwable) {
                        Log.w(TAG, "VOD categories unavailable on server (${e.message})")
                        emptyList<IPTVCategory>()
                    }
                }

                val seriesDeferred = async {
                    try {
                        httpGetStream(seriesUrl) { reader -> parseCategoriesStream(reader, "series") }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Series categories unavailable on server (${e.message})")
                        emptyList<IPTVCategory>()
                    }
                }

                val liveCats = liveDeferred.await()
                val vodCats = vodDeferred.await()
                val seriesCats = seriesDeferred.await()

                val combined = liveCats + vodCats + seriesCats
                val result = if (combined.isEmpty()) {
                    listOf(
                        IPTVCategory("all", "الكل", "channel"),
                        IPTVCategory("sports", "الرياضة Live", "channel"),
                        IPTVCategory("movies", "أحدث الأفلام VOD", "movie"),
                        IPTVCategory("series", "المسلسلات الحصرية", "series")
                    )
                } else {
                    combined
                }
                lastCachedHost = host
                lastCachedUsername = username
                cachedCategories = result
                result
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching Xtream categories", e)
            val fallback = listOf(
                IPTVCategory("all", "الكل", "channel"),
                IPTVCategory("sports", "الرياضة Live", "channel"),
                IPTVCategory("movies", "أحدث الأفلام VOD", "movie"),
                IPTVCategory("series", "المسلسلات الحصرية", "series")
            )
            fallback
        }
    }

    private fun parseCategoriesStream(reader: java.io.Reader, type: String): List<IPTVCategory> {
        val list = mutableListOf<IPTVCategory>()
        val jsonReader = android.util.JsonReader(reader)
        jsonReader.isLenient = true
        try {
            if (jsonReader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                jsonReader.beginArray()
                while (jsonReader.hasNext()) {
                    if (jsonReader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                        jsonReader.beginObject()
                        var id = ""
                        var name = ""
                        while (jsonReader.hasNext()) {
                            val key = jsonReader.nextName()
                            if (jsonReader.peek() == android.util.JsonToken.NULL) {
                                jsonReader.skipValue()
                                continue
                            }
                            when (key.lowercase()) {
                                "category_id" -> id = nextStringOrCoerce(jsonReader)
                                "category_name" -> name = nextStringOrCoerce(jsonReader)
                                else -> jsonReader.skipValue()
                            }
                        }
                        jsonReader.endObject()
                        if (id.isNotBlank() && name.isNotBlank()) {
                            list.add(IPTVCategory(id = id, name = name, type = type))
                        }
                    } else {
                        jsonReader.skipValue()
                    }
                }
                try { jsonReader.endArray() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error streaming categories: ${e.message}")
        } finally {
            try { jsonReader.close() } catch (ignored: Exception) {}
        }
        return list
    }

    private fun parseCategoriesJson(jsonStr: String, type: String): List<IPTVCategory> {
        val list = mutableListOf<IPTVCategory>()
        if (jsonStr.isEmpty() || !jsonStr.trim().startsWith("[")) return list
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("category_id")
                    val name = obj.optString("category_name")
                    if (id.isNotEmpty() && name.isNotEmpty()) {
                        list.add(IPTVCategory(id = id, name = name, type = type))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing single category at index $i", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing categories json", e)
        }
        return list
    }

    private suspend fun fetchXtreamChannelsFromM3u(formattedHost: String, username: String, password: String, forcedFormat: String = "ts"): List<Channel> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Channel>()
        try {
            val formatExtension = if (forcedFormat.isNotBlank()) forcedFormat else "ts"
            val url = "$formattedHost/get.php?username=$username&password=$password&output=$formatExtension"
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body ?: return@use
                body.charStream().use { reader ->
                    val bufferedReader = java.io.BufferedReader(reader)
                    var line: String? = bufferedReader.readLine()
                    var currentName = ""
                    var currentLogo = ""
                    var currentCategory = ""
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#EXTINF:")) {
                            currentName = ""
                            currentLogo = ""
                            currentCategory = ""
                            
                            val logoRegex = Regex("""(?:tvg-logo|logo|icon)="([^"]+)""", RegexOption.IGNORE_CASE)
                            val logoMatch = logoRegex.find(trimmed)
                            if (logoMatch != null) {
                                currentLogo = logoMatch.groupValues[1].trim().replace(" ", "%20")
                            }
                            
                            val groupRegex = Regex("""group-title="([^"]+)"""")
                            val groupMatch = groupRegex.find(trimmed)
                            if (groupMatch != null) {
                                currentCategory = groupMatch.groupValues[1]
                            }
                            
                            val commaIdx = trimmed.lastIndexOf(',')
                            if (commaIdx != -1 && commaIdx < trimmed.length - 1) {
                                currentName = trimmed.substring(commaIdx + 1).trim()
                            }
                        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                            if (currentName.isNotEmpty()) {
                                val lastSlash = trimmed.lastIndexOf('/')
                                var streamId = ""
                                if (lastSlash != -1) {
                                    val segment = trimmed.substring(lastSlash + 1)
                                    val dotIdx = segment.indexOf('.')
                                    streamId = if (dotIdx != -1) {
                                        segment.substring(0, dotIdx)
                                    } else {
                                        segment
                                    }
                                }
                                if (streamId.isEmpty()) {
                                    streamId = java.util.UUID.randomUUID().toString()
                                }
                                list.add(
                                    Channel(
                                        id = streamId,
                                        name = currentName,
                                        logoUrl = currentLogo,
                                        streamUrl = trimmed,
                                        category = currentCategory,
                                        forcedFormat = formatExtension
                                    )
                                )
                            }
                            currentName = ""
                            currentLogo = ""
                            currentCategory = ""
                        }
                        line = bufferedReader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching channels from M3U fallback", e)
        }
        return@withContext list
    }

    suspend fun fetchXtreamChannels(host: String, username: String, password: String, forcedFormat: String = "ts"): List<Channel> {
        if (host == lastCachedHost && username == lastCachedUsername && cachedChannels != null) {
            Log.d(TAG, "Returning cached channels instantly")
            return cachedChannels!!
        }
        val formatExtension = if (forcedFormat.isNotBlank()) forcedFormat else "ts"
        val list = try {
            val formattedHost = formatHostUrl(host)
            val url = "$formattedHost/player_api.php?username=$username&password=$password&action=get_live_streams"
            val fetched = httpGetStream(url) { reader ->
                val resultList = mutableListOf<Channel>()
                val jsonReader = android.util.JsonReader(reader)
                jsonReader.isLenient = true
                try {
                    if (jsonReader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            try {
                                if (jsonReader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                                    jsonReader.beginObject()
                                    var streamId = ""
                                    var name = ""
                                    var logoUrl = ""
                                    var categoryId = ""
                                    while (jsonReader.hasNext()) {
                                        val key = jsonReader.nextName()
                                        if (jsonReader.peek() == android.util.JsonToken.NULL) {
                                            jsonReader.skipValue()
                                            continue
                                        }
                                        when (key.lowercase()) {
                                            "stream_id" -> streamId = nextStringOrCoerce(jsonReader)
                                            "name" -> name = nextStringOrCoerce(jsonReader)
                                            "stream_icon", "icon", "logo", "tvg_logo", "channel_icon", "logo_url", "cover", "image", "tvg-logo" -> {
                                                val parsedLogo = nextStringOrCoerce(jsonReader).trim()
                                                if (logoUrl.isBlank() && parsedLogo.isNotBlank() && parsedLogo != "null") {
                                                    logoUrl = parsedLogo
                                                }
                                            }
                                            "category_id" -> categoryId = nextStringOrCoerce(jsonReader)
                                            else -> jsonReader.skipValue()
                                        }
                                    }
                                    jsonReader.endObject()
                                    if (streamId.isNotEmpty()) {
                                        val streamUrl = if (formatExtension.lowercase() == "normal" || formatExtension.lowercase() == "direct" || formatExtension.lowercase() == "raw") {
                                            "$formattedHost/live/$username/$password/$streamId"
                                        } else {
                                            "$formattedHost/live/$username/$password/$streamId.$formatExtension"
                                        }
                                        val cleanLogo = if (logoUrl.isNotBlank() && !logoUrl.equals("null", ignoreCase = true)) {
                                            logoUrl.replace(" ", "%20")
                                        } else ""
                                        resultList.add(
                                            Channel(
                                                id = streamId,
                                                name = name,
                                                logoUrl = cleanLogo,
                                                streamUrl = streamUrl,
                                                category = categoryId,
                                                forcedFormat = formatExtension
                                            )
                                        )
                                    }
                                } else {
                                    jsonReader.skipValue()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Robust Channel Parsing Error - skipping malformed entry", e)
                                try {
                                    while (jsonReader.hasNext() && jsonReader.peek() != android.util.JsonToken.END_OBJECT) {
                                        jsonReader.skipValue()
                                    }
                                    if (jsonReader.peek() == android.util.JsonToken.END_OBJECT) {
                                        jsonReader.endObject()
                                    }
                                } catch (inner: Exception) {
                                    try { jsonReader.skipValue() } catch (ignored: Exception) {}
                                }
                            }
                        }
                        try { jsonReader.endArray() } catch (ignored: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Streaming channels parse error", e)
                } finally {
                    try { jsonReader.close() } catch (ignored: Exception) {}
                }
                resultList
            }
            if (fetched.isEmpty()) {
                Log.d(TAG, "get_live_streams returned empty, trying M3U fallback...")
                fetchXtreamChannelsFromM3u(formattedHost, username, password, formatExtension)
            } else {
                fetched
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Xtream live streams unavailable (${e.message}), attempting M3U fallback...")
            try {
                fetchXtreamChannelsFromM3u(formatHostUrl(host), username, password, formatExtension)
            } catch (fallbackEx: Throwable) {
                Log.w(TAG, "M3U fallback unavailable (${fallbackEx.message})")
                emptyList()
            }
        }
        cachedChannels = list
        return list
    }

    suspend fun fetchXtreamMovies(host: String, username: String, password: String): List<Movie> {
        if (host == lastCachedHost && username == lastCachedUsername && cachedMovies != null) {
            Log.d(TAG, "Returning cached movies instantly")
            return cachedMovies!!
        }
        return try {
            val formattedHost = formatHostUrl(host)
            val url = "$formattedHost/player_api.php?username=$username&password=$password&action=get_vod_streams"
            val list = httpGetStream(url) { reader ->
                val resultList = mutableListOf<Movie>()
                val jsonReader = android.util.JsonReader(reader)
                jsonReader.isLenient = true
                try {
                    if (jsonReader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            try {
                                if (jsonReader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                                    jsonReader.beginObject()
                                    var streamId = ""
                                    var title = ""
                                    var posterUrl = ""
                                    var categoryId = ""
                                    var rating = 0.0
                                    var ext = "mp4"
                                    while (jsonReader.hasNext()) {
                                        val key = jsonReader.nextName()
                                        if (jsonReader.peek() == android.util.JsonToken.NULL) {
                                            jsonReader.skipValue()
                                            continue
                                        }
                                        when (key) {
                                            "stream_id" -> streamId = nextStringOrCoerce(jsonReader)
                                            "name" -> title = nextStringOrCoerce(jsonReader)
                                            "stream_icon" -> posterUrl = nextStringOrCoerce(jsonReader)
                                            "category_id" -> categoryId = nextStringOrCoerce(jsonReader)
                                            "rating" -> rating = nextDoubleOrCoerce(jsonReader)
                                            "container_extension" -> ext = nextStringOrCoerce(jsonReader).ifEmpty { "mp4" }
                                            else -> jsonReader.skipValue()
                                        }
                                    }
                                    jsonReader.endObject()
                                    if (streamId.isNotEmpty()) {
                                        val streamUrl = "$formattedHost/movie/$username/$password/$streamId.$ext"
                                        resultList.add(
                                            Movie(
                                                id = streamId,
                                                title = title,
                                                description = "محتوى مميز تم جلبه عبر السيرفر الخاص بك بجودة عالية.",
                                                posterUrl = posterUrl,
                                                streamUrl = streamUrl,
                                                category = categoryId,
                                                duration = "02:00:00",
                                                rating = rating,
                                                year = 2026
                                            )
                                        )
                                    }
                                } else {
                                    jsonReader.skipValue()
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Skipping malformed movie entry: ${e.message}")
                                try {
                                    while (jsonReader.hasNext() && jsonReader.peek() != android.util.JsonToken.END_OBJECT) {
                                        jsonReader.skipValue()
                                    }
                                    if (jsonReader.peek() == android.util.JsonToken.END_OBJECT) {
                                        jsonReader.endObject()
                                    }
                                } catch (inner: Exception) {
                                    try { jsonReader.skipValue() } catch (ignored: Exception) {}
                                }
                            }
                        }
                        try { jsonReader.endArray() } catch (ignored: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Movies parse notice: ${e.message}")
                } finally {
                    try { jsonReader.close() } catch (ignored: Exception) {}
                }
                resultList
            }
            cachedMovies = list
            list
        } catch (e: Throwable) {
            Log.w(TAG, "Xtream movies unavailable on server (${e.message})")
            emptyList()
        }
    }

    suspend fun fetchMovieDetails(host: String, username: String, password: String, movie: Movie): Movie {
        return try {
            val formattedHost = formatHostUrl(host)
            val url = "$formattedHost/player_api.php?username=$username&password=$password&action=get_vod_info&vod_id=${movie.id}"
            val rawJson = httpGet(url)
            if (rawJson.isNullOrBlank()) return movie
            val rootObj = JSONObject(rawJson)
            val infoObj = rootObj.optJSONObject("info") ?: JSONObject()
            val movieDataObj = rootObj.optJSONObject("movie_data") ?: JSONObject()

            val title = infoObj.optString("name", infoObj.optString("title", movieDataObj.optString("name", movie.title))).ifEmpty { movie.title }
            val desc = infoObj.optString("plot", infoObj.optString("description", infoObj.optString("storyline", movie.description))).ifEmpty { movie.description }
            val poster = infoObj.optString("movie_image", infoObj.optString("cover", infoObj.optString("stream_icon", movie.posterUrl))).ifEmpty { movie.posterUrl }
            val backdrop = infoObj.optString("backdrop_path", infoObj.optString("backdrop", infoObj.optString("background", movie.backdropUrl))).ifEmpty { movie.backdropUrl }
            val duration = infoObj.optString("duration", infoObj.optString("episode_run_time", movie.duration)).ifEmpty { movie.duration }
            val rating = infoObj.optDouble("rating", infoObj.optDouble("rating_5based", movie.rating)).let { if (it.isNaN() || it <= 0.0) movie.rating else it }
            val year = infoObj.optInt("releaseDate", infoObj.optInt("year", movie.year)).let { if (it in 1900..2099) it else movie.year }
            val director = infoObj.optString("director", movie.director).ifEmpty { movie.director }
            val genre = infoObj.optString("genre", movie.genre).ifEmpty { movie.genre }

            val (castStr, castMembers) = parseCastList(infoObj, rootObj, "بطل الفيلم")
            val finalCast = castStr.ifEmpty { movie.cast }
            val finalCastList = if (castMembers.isNotEmpty()) castMembers else movie.castList

            movie.copy(
                title = title,
                description = desc,
                posterUrl = poster,
                backdropUrl = backdrop,
                duration = duration,
                rating = rating,
                year = year,
                director = director,
                genre = genre,
                cast = finalCast,
                castList = finalCastList
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enrich movie details (${e.message})")
            movie
        }
    }

    suspend fun fetchXtreamSeries(host: String, username: String, password: String): List<Series> {
        if (host == lastCachedHost && username == lastCachedUsername && cachedSeries != null) {
            Log.d(TAG, "Returning cached series instantly")
            return cachedSeries!!
        }
        return try {
            val formattedHost = formatHostUrl(host)
            val url = "$formattedHost/player_api.php?username=$username&password=$password&action=get_series"
            val list = httpGetStream(url) { reader ->
                val resultList = mutableListOf<Series>()
                val jsonReader = android.util.JsonReader(reader)
                jsonReader.isLenient = true
                try {
                    if (jsonReader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            try {
                                if (jsonReader.peek() == android.util.JsonToken.BEGIN_OBJECT) {
                                    jsonReader.beginObject()
                                    var seriesId = ""
                                    var title = ""
                                    var posterUrl = ""
                                    var categoryId = ""
                                    var rating = 0.0
                                    var desc = "مسلسل درامي مميز متاح للمشاهدة المباشرة."
                                    while (jsonReader.hasNext()) {
                                        val key = jsonReader.nextName()
                                        if (jsonReader.peek() == android.util.JsonToken.NULL) {
                                            jsonReader.skipValue()
                                            continue
                                        }
                                        when (key) {
                                            "series_id" -> seriesId = nextStringOrCoerce(jsonReader)
                                            "name" -> title = nextStringOrCoerce(jsonReader)
                                            "cover" -> posterUrl = nextStringOrCoerce(jsonReader)
                                            "category_id" -> categoryId = nextStringOrCoerce(jsonReader)
                                            "rating" -> rating = nextDoubleOrCoerce(jsonReader)
                                            "plot" -> desc = nextStringOrCoerce(jsonReader).ifEmpty { "مسلسل درامي مميز متاح للمشاهدة المباشرة." }
                                            else -> jsonReader.skipValue()
                                        }
                                    }
                                    jsonReader.endObject()
                                    if (seriesId.isNotEmpty()) {
                                        resultList.add(
                                            Series(
                                                id = seriesId,
                                                title = title,
                                                description = desc,
                                                posterUrl = posterUrl,
                                                category = categoryId,
                                                rating = rating,
                                                year = 2026
                                            )
                                        )
                                    }
                                } else {
                                    jsonReader.skipValue()
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Skipping malformed series entry: ${e.message}")
                                try {
                                    while (jsonReader.hasNext() && jsonReader.peek() != android.util.JsonToken.END_OBJECT) {
                                        jsonReader.skipValue()
                                    }
                                    if (jsonReader.peek() == android.util.JsonToken.END_OBJECT) {
                                        jsonReader.endObject()
                                    }
                                } catch (inner: Exception) {
                                    try { jsonReader.skipValue() } catch (ignored: Exception) {}
                                }
                            }
                        }
                        try { jsonReader.endArray() } catch (ignored: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Series parse notice: ${e.message}")
                } finally {
                    try { jsonReader.close() } catch (ignored: Exception) {}
                }
                resultList
            }
            cachedSeries = list
            list
        } catch (e: Throwable) {
            Log.w(TAG, "Xtream series unavailable on server (${e.message})")
            emptyList()
        }
    }

    data class SeriesFullData(
        val series: Series,
        val seasons: List<Season>,
        val episodesBySeason: Map<String, List<Episode>>,
        val allEpisodes: List<Episode>
    )

    private val seriesDataCache = java.util.concurrent.ConcurrentHashMap<String, SeriesFullData>()

    suspend fun fetchFullSeriesData(host: String, username: String, password: String, series: Series): SeriesFullData {
        val cacheKey = "${host.trim()}_${series.id.trim()}"
        val cached = seriesDataCache[cacheKey]
        if (cached != null) return cached

        return try {
            val formattedHost = formatHostUrl(host)
            val url = "$formattedHost/player_api.php?username=$username&password=$password&action=get_series_info&series_id=${series.id}"
            val jsonStr = httpGet(url)
            if (jsonStr.isNotEmpty() && jsonStr.trim().startsWith("{")) {
                val rootObj = JSONObject(jsonStr)
                val infoObj = if (rootObj.has("info")) rootObj.optJSONObject("info") ?: rootObj else rootObj

                val desc = infoObj.optString("plot", infoObj.optString("description", infoObj.optString("overview", series.description))).ifEmpty { series.description }
                val director = infoObj.optString("director", series.director)
                val genre = infoObj.optString("genre", series.genre)
                val rating = infoObj.optDouble("rating", infoObj.optDouble("rating_5based", series.rating))
                val releaseDate = infoObj.optString("releaseDate", infoObj.optString("releasedate", series.year.toString()))
                val year = try { releaseDate.take(4).toInt() } catch (e: Exception) { series.year }
                val trailer = infoObj.optString("youtube_trailer", "")

                var backdrop = ""
                if (infoObj.has("backdrop_path")) {
                    val bp = infoObj.opt("backdrop_path")
                    if (bp is org.json.JSONArray && bp.length() > 0) {
                        backdrop = bp.optString(0)
                    } else if (bp is String) {
                        backdrop = bp
                    }
                }

                val (castStr, parsedCastList) = parseCastList(infoObj, rootObj, "بطولة")
                val castList = if (parsedCastList.isNotEmpty()) parsedCastList else {
                    if (series.castList.isNotEmpty()) series.castList else emptyList()
                }

                val enrichedSeries = series.copy(
                    description = desc,
                    director = director,
                    cast = if (castStr.isNotEmpty()) castStr else series.cast,
                    castList = castList,
                    genre = genre,
                    rating = if (rating > 0.0) rating else series.rating,
                    year = year,
                    backdropUrl = backdrop,
                    trailerUrl = trailer
                )

                // Parse seasons & episodes
                val seasonsMap = mutableMapOf<Int, Season>()
                val episodesMap = mutableMapOf<String, MutableList<Episode>>()
                val allEpisodesList = mutableListOf<Episode>()

                // 1. Parse seasons from "seasons" field (JSONArray or JSONObject)
                if (rootObj.has("seasons")) {
                    val seasonsElement = rootObj.opt("seasons")
                    if (seasonsElement is org.json.JSONArray) {
                        for (i in 0 until seasonsElement.length()) {
                            val sObj = seasonsElement.optJSONObject(i) ?: continue
                            val num = sObj.optInt("season_number", i + 1)
                            val rawName = sObj.optString("name", "").trim()
                            val title = if (rawName.isNotEmpty() && !rawName.equals("null", ignoreCase = true)) rawName else "الموسم $num"
                            val sId = num.toString()
                            seasonsMap[num] = Season(id = sId, seriesId = series.id, number = num, title = title)
                        }
                    } else if (seasonsElement is JSONObject) {
                        val keys = seasonsElement.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val sObj = seasonsElement.optJSONObject(k)
                            val num = sObj?.optInt("season_number", k.toIntOrNull() ?: (seasonsMap.size + 1)) ?: (k.toIntOrNull() ?: (seasonsMap.size + 1))
                            val rawName = sObj?.optString("name", "")?.trim() ?: ""
                            val title = if (rawName.isNotEmpty() && !rawName.equals("null", ignoreCase = true)) rawName else "الموسم $num"
                            val sId = num.toString()
                            seasonsMap[num] = Season(id = sId, seriesId = series.id, number = num, title = title)
                        }
                    }
                }

                // 2. Parse episodes from "episodes" field (JSONObject or JSONArray)
                if (rootObj.has("episodes")) {
                    val epsElement = rootObj.opt("episodes")
                    if (epsElement is JSONObject) {
                        val seasonKeys = epsElement.keys()
                        while (seasonKeys.hasNext()) {
                            val rawKey = seasonKeys.next()
                            val digits = Regex("\\d+").find(rawKey)?.value?.toIntOrNull()
                            val rawSeasonNum = digits ?: (if (rawKey.equals("0", ignoreCase = true)) 0 else (seasonsMap.size + 1))
                            val standardSeasonId = rawSeasonNum.toString()

                            // Ensure season exists in seasonsMap
                            if (!seasonsMap.containsKey(rawSeasonNum)) {
                                val sTitle = if (rawSeasonNum == 0) "حلقات خاصة" else "الموسم $rawSeasonNum"
                                seasonsMap[rawSeasonNum] = Season(
                                    id = standardSeasonId,
                                    seriesId = series.id,
                                    number = rawSeasonNum,
                                    title = sTitle
                                )
                            }

                            val arr = epsElement.optJSONArray(rawKey)
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val obj = arr.optJSONObject(i) ?: continue
                                    val ep = parseSingleEpisode(obj, standardSeasonId, series.id, formattedHost, username, password, i)
                                    episodesMap.getOrPut(standardSeasonId) { mutableListOf() }.add(ep)
                                    if (rawKey != standardSeasonId) {
                                        episodesMap.getOrPut(rawKey) { mutableListOf() }.add(ep)
                                    }
                                    allEpisodesList.add(ep)
                                }
                            }
                        }
                    } else if (epsElement is org.json.JSONArray) {
                        for (i in 0 until epsElement.length()) {
                            val obj = epsElement.optJSONObject(i) ?: continue
                            val seasonNum = obj.optInt("season", obj.optInt("season_num", obj.optInt("season_number", 1)))
                            val sId = seasonNum.toString()
                            if (!seasonsMap.containsKey(seasonNum)) {
                                seasonsMap[seasonNum] = Season(
                                    id = sId,
                                    seriesId = series.id,
                                    number = seasonNum,
                                    title = "الموسم $seasonNum"
                                )
                            }
                            val ep = parseSingleEpisode(obj, sId, series.id, formattedHost, username, password, i)
                            episodesMap.getOrPut(sId) { mutableListOf() }.add(ep)
                            allEpisodesList.add(ep)
                        }
                    }
                }

                // Fallback: If no seasons discovered at all, provide Season 1
                if (seasonsMap.isEmpty()) {
                    seasonsMap[1] = Season(id = "1", seriesId = series.id, number = 1, title = "الموسم الأول")
                }

                val sortedSeasons = seasonsMap.values.sortedBy { it.number }
                episodesMap.values.forEach { list -> list.sortBy { it.number } }

                val result = SeriesFullData(
                    series = enrichedSeries,
                    seasons = sortedSeasons,
                    episodesBySeason = episodesMap,
                    allEpisodes = allEpisodesList.sortedBy { it.number }
                )
                seriesDataCache[cacheKey] = result
                result
            } else {
                val fallbackSeason = Season(id = "1", seriesId = series.id, number = 1, title = "الموسم الأول")
                SeriesFullData(series, listOf(fallbackSeason), emptyMap(), emptyList())
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching full series data for ${series.id}", e)
            val fallbackSeason = Season(id = "1", seriesId = series.id, number = 1, title = "الموسم الأول")
            SeriesFullData(series, listOf(fallbackSeason), emptyMap(), emptyList())
        }
    }

    private fun parseSingleEpisode(
        obj: JSONObject,
        seasonId: String,
        seriesId: String,
        formattedHost: String,
        username: String,
        password: String,
        index: Int
    ): Episode {
        val id = obj.optString("id", obj.optString("stream_id", obj.optString("episode_id", "")))
        val num = obj.optInt("episode_num", obj.optInt("episode", obj.optInt("number", index + 1)))
        var title = obj.optString("title", obj.optString("name", "الحلقة $num")).trim()
        if (title.isEmpty() || title.equals("null", ignoreCase = true)) {
            title = "الحلقة $num"
        }
        val ext = obj.optString("container_extension", obj.optString("extension", obj.optString("target_container", "mp4"))).ifEmpty { "mp4" }
        
        var duration = "00:45:00"
        var desc = "تفاصيل ومجريات الحلقة متاحة للمشاهدة الفورية بجودة عالية."
        var epPoster = ""

        val info = obj.optJSONObject("info")
        if (info != null) {
            duration = info.optString("duration", info.optString("duration_secs", duration))
            desc = info.optString("plot", info.optString("description", info.optString("overview", desc)))
            epPoster = info.optString("movie_image", info.optString("cover", info.optString("cover_big", "")))
        }
        if (epPoster.isEmpty()) {
            epPoster = obj.optString("movie_image", obj.optString("cover", obj.optString("cover_big", "")))
        }
        if (duration.isEmpty() || duration.equals("null", ignoreCase = true)) {
            duration = "00:45:00"
        }
        if (desc.isEmpty() || desc.equals("null", ignoreCase = true)) {
            desc = "تفاصيل ومجريات الحلقة متاحة للمشاهدة الفورية بجودة عالية."
        }

        val streamUrl = if (id.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            "$formattedHost/series/$username/$password/$id.$ext"
        } else {
            obj.optString("stream_url", obj.optString("url", ""))
        }

        return Episode(
            id = id.ifEmpty { "${seriesId}_s${seasonId}_ep${num}" },
            seasonId = seasonId,
            seriesId = seriesId,
            title = title,
            description = desc,
            posterUrl = epPoster,
            streamUrl = streamUrl,
            duration = duration,
            number = num,
            thumbnailUrl = epPoster,
            plot = desc
        )
    }

    suspend fun fetchXtreamSeasons(host: String, username: String, password: String, seriesId: String): List<Season> {
        val dummy = Series(id = seriesId, title = "")
        return fetchFullSeriesData(host, username, password, dummy).seasons
    }

    suspend fun fetchXtreamEpisodes(host: String, username: String, password: String, seriesId: String, seasonId: String): List<Episode> {
        val dummy = Series(id = seriesId, title = "")
        val fullData = fetchFullSeriesData(host, username, password, dummy)
        var epList = fullData.episodesBySeason[seasonId]
        if (epList.isNullOrEmpty()) {
            val num = Regex("\\d+").find(seasonId)?.value ?: seasonId
            epList = fullData.episodesBySeason[num]
        }
        if (epList.isNullOrEmpty()) {
            epList = fullData.episodesBySeason["season_$seasonId"]
        }
        return epList ?: if (fullData.seasons.size <= 1) fullData.allEpisodes else emptyList()
    }

    suspend fun fetchSeriesDetails(host: String, username: String, password: String, series: Series): Series {
        return fetchFullSeriesData(host, username, password, series).series
    }

    private fun parseCastList(infoObj: JSONObject, rootObj: JSONObject, defaultRole: String): Pair<String, List<com.example.model.CastMember>> {
        val list = mutableListOf<com.example.model.CastMember>()
        var castStr = ""

        // 1. Check if cast is a JSONArray of objects
        val castArray = infoObj.optJSONArray("cast") ?: infoObj.optJSONArray("actors") ?: rootObj.optJSONArray("cast") ?: rootObj.optJSONArray("actors")
        if (castArray != null && castArray.length() > 0) {
            for (i in 0 until minOf(castArray.length(), 15)) {
                val item = castArray.opt(i)
                if (item is JSONObject) {
                    val name = item.optString("name", item.optString("actor", item.optString("title", ""))).trim()
                    if (name.isNotEmpty()) {
                        val role = item.optString("character", item.optString("role", defaultRole))
                        var photo = item.optString("profile_path", item.optString("photo", item.optString("image", item.optString("avatar", item.optString("picture", item.optString("photoUrl", ""))))))
                        if (photo.startsWith("/")) {
                            photo = "https://image.tmdb.org/t/p/w185$photo"
                        }
                        list.add(com.example.model.CastMember(
                            name = name,
                            role = role,
                            photoUrl = photo,
                            character = role,
                            profileUrl = photo
                        ))
                    }
                } else if (item is String && item.isNotBlank()) {
                    list.add(com.example.model.CastMember(name = item.trim(), role = defaultRole, character = defaultRole))
                }
            }
            castStr = list.joinToString(", ") { it.name }
        }

        // 2. If list is empty, try String cast/actors
        if (list.isEmpty()) {
            castStr = infoObj.optString("cast", infoObj.optString("actors", rootObj.optString("cast", rootObj.optString("actors", ""))))
            if (castStr.isNotEmpty()) {
                val names = castStr.split(",", "،", ";").map { it.trim() }.filter { it.isNotEmpty() }.take(15)
                names.forEach { name ->
                    var cleanName = name
                    var character = defaultRole
                    if (name.contains(" as ", ignoreCase = true)) {
                        val parts = name.split(Regex(" as ", RegexOption.IGNORE_CASE))
                        cleanName = parts[0].trim()
                        if (parts.size > 1) character = parts[1].trim()
                    } else if (name.contains("(") && name.contains(")")) {
                        val start = name.indexOf("(")
                        val end = name.indexOf(")")
                        if (end > start) {
                            cleanName = name.substring(0, start).trim()
                            character = name.substring(start + 1, end).trim()
                        }
                    }
                    list.add(com.example.model.CastMember(name = cleanName, role = character, character = character))
                }
            }
        }

        return Pair(castStr, list)
    }

    /**
     * Verifies if an access code exists and is active.
     */
    suspend fun verifyAccessCode(code: String): AccessCode? = withContext(Dispatchers.IO) {
        val trimmedCode = code.trim().uppercase()

        // 1. Try RTDB first
        try {
            val url = "$RTDB_URL/access_codes/$trimmedCode.json"
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val isActive = json.optBoolean("isActive", true)
                        val accountId = json.optString("accountId", "premium_account_a1")
                        return@withContext AccessCode(code = trimmedCode, isActive = isActive, accountId = accountId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice: Access code check in RTDB for $trimmedCode: ${e.message}")
        }

        // 2. Try Firestore SDK
        try {
            val doc = db.collection("access_codes").document(trimmedCode).get().await()
            if (doc.exists()) {
                val isActive = doc.getBoolean("isActive") ?: true
                val accountId = doc.getString("accountId") ?: ""
                return@withContext AccessCode(code = trimmedCode, isActive = isActive, accountId = accountId)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Notice: Access code check in Firestore for $trimmedCode: ${e.message}")
        }

        // 3. Backup fallback for standard demo/test/VIP codes
        if (trimmedCode.matches(Regex("A[1-9]")) || trimmedCode == "VIP" || trimmedCode == "V1P") {
            AccessCode(code = trimmedCode, isActive = true, accountId = "premium_account_a1")
        } else {
            null
        }
    }

    /**
     * Retrieves the IPTV account metadata for a specific account.
     */
    suspend fun getAccount(accountId: String): IPTVAccount? {
        return try {
            val doc = db.collection("accounts").document(accountId).get().await()
            if (doc.exists()) {
                val name = doc.getString("name") ?: "Premium Account"
                val allowedCategories = doc.get("allowedCategories") as? List<String> ?: emptyList()
                val expiryDate = doc.getString("expiryDate") ?: "2027-12-31"
                IPTVAccount(id = accountId, name = name, allowedCategories = allowedCategories, expiryDate = expiryDate)
            } else {
                if (accountId == "premium_account_a1") {
                    IPTVAccount(
                        id = accountId,
                        name = "VIP Premium User (+90)",
                        allowedCategories = listOf("sports", "news", "action", "scientific", "entertainment"),
                        expiryDate = "2028-12-31"
                    )
                } else {
                    null
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching account details: $accountId (Using local/cache fallback)", e)
            if (accountId == "premium_account_a1") {
                IPTVAccount(
                    id = accountId,
                    name = "VIP Premium User (+90)",
                    allowedCategories = listOf("sports", "news", "action", "scientific", "entertainment"),
                    expiryDate = "2028-12-31"
                )
            } else {
                null
            }
        }
    }

    /**
     * Fetches channels belonging to categories approved for the active user.
     */
    suspend fun getChannels(allowedCategories: List<String>): List<Channel> = withContext(Dispatchers.IO) {
        val firestoreList = try {
            val snapshot = db.collection("channels").get().await()
            snapshot.documents.map { doc ->
                Channel(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    logoUrl = doc.getString("logoUrl") ?: "",
                    streamUrl = doc.getString("streamUrl") ?: "",
                    category = doc.getString("category") ?: "",
                    userAgent = doc.getString("userAgent") ?: "",
                    forcedFormat = doc.getString("forcedFormat") ?: "m3u8"
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching channels from firestore", e)
            emptyList()
        }

        val deletedIds = getDeletedIds("channels")

        val rtdbList = mutableListOf<Channel>()
        val url = "$RTDB_URL/channels.json"
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                rtdbList.add(
                                    Channel(
                                        id = obj.optString("id", key),
                                        name = obj.optString("name", ""),
                                        logoUrl = obj.optString("logoUrl", ""),
                                        streamUrl = obj.optString("streamUrl", ""),
                                        category = obj.optString("category", ""),
                                        userAgent = obj.optString("userAgent", ""),
                                        forcedFormat = obj.optString("forcedFormat", "m3u8")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching channels from RTDB", e)
        }

        val combined = (firestoreList.filterNot { deletedIds.contains(it.id) } + rtdbList)
            .distinctBy { it.id }
            .filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        return@withContext if (combined.isEmpty()) {
            localChannelsList.filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        } else {
            combined
        }
    }

    /**
     * Fetches movies for categories.
     */
    suspend fun getMovies(allowedCategories: List<String>): List<Movie> = withContext(Dispatchers.IO) {
        val firestoreList = try {
            val snapshot = db.collection("movies").get().await()
            snapshot.documents.map { doc ->
                Movie(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    posterUrl = doc.getString("posterUrl") ?: "",
                    streamUrl = doc.getString("streamUrl") ?: "",
                    category = doc.getString("category") ?: "",
                    duration = doc.getString("duration") ?: "0",
                    rating = doc.getDouble("rating") ?: 0.0,
                    year = doc.getLong("year")?.toInt() ?: 2026
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching movies from firestore", e)
            emptyList()
        }

        val deletedIds = getDeletedIds("movies")

        val rtdbList = mutableListOf<Movie>()
        val url = "$RTDB_URL/movies.json"
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                rtdbList.add(
                                    Movie(
                                        id = obj.optString("id", key),
                                        title = obj.optString("title", ""),
                                        description = obj.optString("description", ""),
                                        posterUrl = obj.optString("posterUrl", ""),
                                        streamUrl = obj.optString("streamUrl", ""),
                                        category = obj.optString("category", ""),
                                        duration = obj.optString("duration", "0"),
                                        rating = obj.optDouble("rating", 0.0),
                                        year = obj.optInt("year", 2026)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching movies from RTDB", e)
        }

        val combined = (firestoreList.filterNot { deletedIds.contains(it.id) } + rtdbList)
            .distinctBy { it.id }
            .filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        return@withContext if (combined.isEmpty()) {
            localMoviesList.filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        } else {
            combined
        }
    }

    /**
     * Fetches series matching authorized categories.
     */
    suspend fun getSeries(allowedCategories: List<String>): List<Series> = withContext(Dispatchers.IO) {
        val firestoreList = try {
            val snapshot = db.collection("series").get().await()
            snapshot.documents.map { doc ->
                Series(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    posterUrl = doc.getString("posterUrl") ?: "",
                    category = doc.getString("category") ?: "",
                    rating = doc.getDouble("rating") ?: 0.0,
                    year = doc.getLong("year")?.toInt() ?: 2026
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching series from firestore", e)
            emptyList()
        }

        val deletedIds = getDeletedIds("series")

        val rtdbList = mutableListOf<Series>()
        val url = "$RTDB_URL/series.json"
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                rtdbList.add(
                                    Series(
                                        id = obj.optString("id", key),
                                        title = obj.optString("title", ""),
                                        description = obj.optString("description", ""),
                                        posterUrl = obj.optString("posterUrl", ""),
                                        category = obj.optString("category", ""),
                                        rating = obj.optDouble("rating", 0.0),
                                        year = obj.optInt("year", 2026)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching series from RTDB", e)
        }

        val combined = (firestoreList.filterNot { deletedIds.contains(it.id) } + rtdbList)
            .distinctBy { it.id }
            .filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        return@withContext if (combined.isEmpty()) {
            localSeriesList.filter { allowedCategories.isEmpty() || allowedCategories.contains(it.category) }
        } else {
            combined
        }
    }

    /**
     * Fetches seasons of a specific Series.
     */
    suspend fun getSeasons(seriesId: String): List<Season> {
        val firestoreList = try {
            val snapshot = db.collection("seasons")
                .whereEqualTo("seriesId", seriesId)
                .get()
                .await()
            snapshot.documents.map { doc ->
                Season(
                    id = doc.id,
                    seriesId = doc.getString("seriesId") ?: "",
                    number = doc.getLong("number")?.toInt() ?: 1,
                    title = doc.getString("title") ?: ""
                )
            }.sortedBy { it.number }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching seasons for series: $seriesId", e)
            emptyList()
        }
        return if (firestoreList.isEmpty() && seriesId == "series_thrones") {
            listOf(Season(id = "season_thrones_1", seriesId = "series_thrones", number = 1, title = "الموسم الأول"))
        } else {
            firestoreList
        }
    }

    /**
     * Fetches episodes of a specific season.
     */
    suspend fun getEpisodes(seriesId: String, seasonId: String): List<Episode> {
        val firestoreList = try {
            val snapshot = db.collection("episodes")
                .whereEqualTo("seriesId", seriesId)
                .whereEqualTo("seasonId", seasonId)
                .get()
                .await()
            snapshot.documents.map { doc ->
                Episode(
                    id = doc.id,
                    seasonId = doc.getString("seasonId") ?: "",
                    seriesId = doc.getString("seriesId") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    posterUrl = doc.getString("posterUrl") ?: "",
                    streamUrl = doc.getString("streamUrl") ?: "",
                    duration = doc.getString("duration") ?: "",
                    number = doc.getLong("number")?.toInt() ?: 1
                )
            }.sortedBy { it.number }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching episodes", e)
            emptyList()
        }
        return if (firestoreList.isEmpty() && seriesId == "series_thrones" && seasonId == "season_thrones_1") {
            listOf(
                Episode(
                    id = "ep_1",
                    seasonId = "season_thrones_1",
                    seriesId = "series_thrones",
                    title = "الحلقة 1: الشتاء قادم",
                    description = "بداية ملحمية من أرض الشمال حيث يتوجب على لورد وينترفل الاستعداد لخطر يهدد المملكة برمتها.",
                    posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=350&q=80",
                    streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8",
                    duration = "14 دقيقة",
                    number = 1
                ),
                Episode(
                    id = "ep_2",
                    seasonId = "season_thrones_1",
                    seriesId = "series_thrones",
                    title = "الحلقة 2: طريق الخلافة المستعر",
                    description = "تزداد المؤامرات في بلاط الملك ويتعين على لورد ند ستارك مواجهة خصوم غامضين وشرسين.",
                    posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=350&q=80",
                    streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
                    duration = "22 دقيقة",
                    number = 2
                )
            )
        } else {
            firestoreList
        }
    }

    /**
     * Fetches categories.
     */
    suspend fun getCategories(): List<IPTVCategory> = withContext(Dispatchers.IO) {
        val firestoreList = try {
            val snapshot = db.collection("categories").get().await()
            snapshot.documents.map { doc ->
                IPTVCategory(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    type = doc.getString("type") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: ""
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching categories from firestore", e)
            emptyList()
        }

        val deletedIds = getDeletedIds("categories")

        val rtdbList = mutableListOf<IPTVCategory>()
        val url = "$RTDB_URL/categories.json"
        try {
            val request = Request.Builder().url(url).build()
            apiClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.optJSONObject(key)
                            if (obj != null) {
                                rtdbList.add(
                                    IPTVCategory(
                                        id = obj.optString("id", key),
                                        name = obj.optString("name", ""),
                                        type = obj.optString("type", ""),
                                        imageUrl = obj.optString("imageUrl", "")
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching categories from RTDB", e)
        }

        val combined = (firestoreList.filterNot { deletedIds.contains(it.id) } + rtdbList)
            .distinctBy { it.id }
        return@withContext if (combined.isEmpty()) {
            localCategoriesList
        } else {
            combined
        }
    }

    /**
     * ADMIN TASKS: Adds/Updates an activation code directly in Firestore and RTDB.
     */
    suspend fun adminSaveActivationCode(activationCode: ActivationCode): Boolean = withContext(Dispatchers.IO) {
        val upperCode = activationCode.code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/activation_codes/$upperCode.json"
        val legacyUrl = "$RTDB_URL/activation_codes/$upperCode.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("code", upperCode)
                put("host", activationCode.host.trim())
                put("username", activationCode.username.trim())
                put("password", activationCode.password.trim())
                put("forcedFormat", activationCode.forcedFormat.trim())
                put("m3uUrl", activationCode.m3uUrl.trim())
                val arr = org.json.JSONArray()
                activationCode.allowedTypes.forEach { arr.put(it) }
                put("allowedTypes", arr)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            
            // Save to secure node
            val secureReq = Request.Builder().url(secureUrl).put(body).build()
            val res = okHttpClient.newCall(secureReq).execute().use { response -> response.isSuccessful }

            // Sync to legacy node
            try {
                val legacyReq = Request.Builder().url(legacyUrl).put(body).build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }

            res
        } catch (e: Exception) {
            Log.e(TAG, "Error saving activation code to RTDB", e)
            false
        }

        try {
            db.collection("activation_codes").document(upperCode).set(
                mapOf(
                    "code" to upperCode,
                    "host" to activationCode.host.trim(),
                    "username" to activationCode.username.trim(),
                    "password" to activationCode.password.trim(),
                    "forcedFormat" to activationCode.forcedFormat.trim(),
                    "m3uUrl" to activationCode.m3uUrl.trim(),
                    "allowedTypes" to activationCode.allowedTypes
                )
            ).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin operation Firestore write failed: save activation code", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Deletes an activation code.
     */
    suspend fun adminDeleteActivationCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/activation_codes/$upperCode.json"
        val legacyUrl = "$RTDB_URL/activation_codes/$upperCode.json"
        val rtdbSuccess = try {
            val secureReq = Request.Builder().url(secureUrl).delete().build()
            val res = okHttpClient.newCall(secureReq).execute().use { response -> response.isSuccessful }
            try {
                val legacyReq = Request.Builder().url(legacyUrl).delete().build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }
            res
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting activation code from RTDB", e)
            false
        }

        try {
            db.collection("activation_codes").document(upperCode).delete().await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin operation Firestore write failed: delete activation code", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Retrieves all activation codes.
     */
    suspend fun adminGetActivationCodes(): List<ActivationCode> = withContext(Dispatchers.IO) {
        val firestoreList = mutableListOf<ActivationCode>()
        try {
            val snapshot = db.collection("activation_codes").get().await()
            for (doc in snapshot.documents) {
                val host = doc.getString("host") ?: ""
                val username = doc.getString("username") ?: ""
                val password = doc.getString("password") ?: ""
                val forcedFormat = doc.getString("forcedFormat") ?: ""
                val m3uUrl = doc.getString("m3uUrl") ?: ""
                val allowedTypesRaw = doc.get("allowedTypes") as? List<String>
                val allowedTypesList = if (!allowedTypesRaw.isNullOrEmpty()) allowedTypesRaw else listOf("live", "movies", "series")
                firestoreList.add(
                    ActivationCode(
                        code = doc.id,
                        host = host,
                        username = username,
                        password = password,
                        forcedFormat = forcedFormat,
                        allowedTypes = allowedTypesList,
                        m3uUrl = m3uUrl
                    )
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to fetch activation codes from Firestore", e)
        }

        val rtdbList = mutableListOf<ActivationCode>()
        val urlsToTry = listOf(
            "$RTDB_URL/$RTDB_SECURE_PREFIX/activation_codes.json",
            "$RTDB_URL/activation_codes.json"
        )
        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty() && bodyStr != "null") {
                            val json = JSONObject(bodyStr)
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val obj = json.optJSONObject(key) ?: continue
                                val host = obj.optString("host") ?: ""
                                val username = obj.optString("username") ?: ""
                                val password = obj.optString("password") ?: ""
                                val forcedFormat = obj.optString("forcedFormat") ?: ""
                                val m3uUrl = obj.optString("m3uUrl") ?: ""
                                val allowedTypesList = mutableListOf<String>()
                                val typesArr = obj.optJSONArray("allowedTypes")
                                if (typesArr != null) {
                                    for (i in 0 until typesArr.length()) {
                                        val strVal = typesArr.optString(i)
                                        if (!strVal.isNullOrEmpty()) {
                                            allowedTypesList.add(strVal)
                                        }
                                    }
                                }
                                if (allowedTypesList.isEmpty()) {
                                    allowedTypesList.addAll(listOf("live", "movies", "series"))
                                }
                                rtdbList.add(
                                    ActivationCode(
                                        code = key,
                                        host = host,
                                        username = username,
                                        password = password,
                                        forcedFormat = forcedFormat,
                                        allowedTypes = allowedTypesList,
                                        m3uUrl = m3uUrl
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching activation codes from RTDB ($url)", e)
            }
        }

        val combined = (firestoreList + rtdbList).distinctBy { it.code.uppercase() }
        return@withContext combined
    }

    /**
     * Parses an M3U or M3U8 playlist stream directly from a URL, extracting channels, categories, and stream URLs.
     */
    suspend fun fetchChannelsAndCategoriesFromM3uUrl(
        m3uUrl: String,
        forcedFormat: String = "ts"
    ): Pair<List<IPTVCategory>, List<Channel>> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        val categoriesMap = linkedMapOf<String, IPTVCategory>()
        val defaultCategoryName = "جميع القنوات"
        categoriesMap[defaultCategoryName] = IPTVCategory(id = "all_m3u", name = defaultCategoryName, type = "live")

        try {
            var targetUrl = m3uUrl.trim()
            if (!targetUrl.startsWith("http://", ignoreCase = true) && !targetUrl.startsWith("https://", ignoreCase = true)) {
                targetUrl = "http://$targetUrl"
            }
            Log.d(TAG, "Fetching M3U playlist from: $targetUrl")
            val request = Request.Builder().url(targetUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download M3U playlist: HTTP ${response.code}")
                    return@use
                }
                val body = response.body ?: return@use
                body.charStream().use { reader ->
                    val bufferedReader = java.io.BufferedReader(reader)
                    var line: String? = bufferedReader.readLine()
                    var currentName = ""
                    var currentLogo = ""
                    var currentCategory = defaultCategoryName
                    var currentId = ""
                    var chIndex = 1

                    val logoRegex = Regex("""(?:tvg-logo|logo|icon)=["']?([^"' ,]+)["']?""", RegexOption.IGNORE_CASE)
                    val groupRegex = Regex("""(?:group-title|tvg-group|group)=["']?([^"',]+)["']?""", RegexOption.IGNORE_CASE)
                    val idRegex = Regex("""(?:tvg-id|id)=["']?([^"' ,]+)["']?""", RegexOption.IGNORE_CASE)
                    val nameRegex = Regex("""tvg-name=["']?([^"',]+)["']?""", RegexOption.IGNORE_CASE)

                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                            currentName = ""
                            currentLogo = ""
                            currentCategory = defaultCategoryName
                            currentId = ""

                            logoRegex.find(trimmed)?.let {
                                currentLogo = it.groupValues[1].trim().replace(" ", "%20")
                            }

                            groupRegex.find(trimmed)?.let {
                                val g = it.groupValues[1].trim()
                                if (g.isNotEmpty()) currentCategory = g
                            }

                            idRegex.find(trimmed)?.let {
                                currentId = it.groupValues[1].trim()
                            }

                            val commaIndex = trimmed.lastIndexOf(',')
                            if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                                currentName = trimmed.substring(commaIndex + 1).trim()
                            }
                            if (currentName.isEmpty()) {
                                nameRegex.find(trimmed)?.let {
                                    currentName = it.groupValues[1].trim()
                                }
                            }
                        } else if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                            if (currentName.isEmpty()) {
                                currentName = "قناة $chIndex"
                            }
                            if (currentId.isEmpty()) {
                                currentId = "m3u_ch_$chIndex"
                            }
                            chIndex++

                            if (currentCategory.isNotEmpty() && !categoriesMap.containsKey(currentCategory)) {
                                categoriesMap[currentCategory] = IPTVCategory(
                                    id = currentCategory,
                                    name = currentCategory,
                                    type = "live"
                                )
                            }

                            val formatToUse = if (forcedFormat.isNotBlank() && forcedFormat != "normal" && forcedFormat != "direct") {
                                forcedFormat
                            } else if (trimmed.contains(".m3u8", ignoreCase = true)) {
                                "m3u8"
                            } else {
                                "ts"
                            }

                            channels.add(
                                Channel(
                                    id = currentId,
                                    name = currentName,
                                    logoUrl = currentLogo,
                                    streamUrl = trimmed,
                                    category = currentCategory,
                                    forcedFormat = formatToUse
                                )
                            )

                            currentName = ""
                            currentLogo = ""
                            currentCategory = defaultCategoryName
                            currentId = ""
                        }
                        line = bufferedReader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading and parsing M3U playlist", e)
        }

        Log.d(TAG, "M3U Parsing complete: ${channels.size} channels, ${categoriesMap.size} categories")
        return@withContext Pair(categoriesMap.values.toList(), channels)
    }

    /**
     * ADMIN TASKS: Adds/Updates an access code directly in Firestore.
     */
    suspend fun adminSaveAccessCode(code: AccessCode): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.code.trim().uppercase()
        val url = "$RTDB_URL/access_codes/$upperCode.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("code", upperCode)
                put("isActive", code.isActive)
                put("accountId", code.accountId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving access code to RTDB", e)
            false
        }

        try {
            db.collection("access_codes").document(upperCode).set(
                mapOf(
                    "code" to upperCode,
                    "isActive" to code.isActive,
                    "accountId" to code.accountId
                )
            ).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin operation Firestore write failed: save access code", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Deletes an access code.
     */
    suspend fun adminDeleteAccessCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val url = "$RTDB_URL/access_codes/$upperCode.json"
        val rtdbSuccess = try {
            val request = Request.Builder().url(url).delete().build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting access code from RTDB", e)
            false
        }

        try {
            db.collection("access_codes").document(upperCode).delete().await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin operation Firestore write failed: delete access code", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Create or update a movie.
     */
    suspend fun adminSaveMovie(movie: Movie): Boolean = withContext(Dispatchers.IO) {
        val movieId = movie.id.ifEmpty { "movie_" + System.currentTimeMillis() }
        val finalMovie = movie.copy(id = movieId)
        val url = "$RTDB_URL/movies/$movieId.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("id", finalMovie.id)
                put("title", finalMovie.title)
                put("description", finalMovie.description)
                put("posterUrl", finalMovie.posterUrl)
                put("streamUrl", finalMovie.streamUrl)
                put("category", finalMovie.category)
                put("duration", finalMovie.duration)
                put("rating", finalMovie.rating)
                put("year", finalMovie.year)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving movie to RTDB", e)
            false
        }

        try {
            db.collection("movies").document(movieId).set(finalMovie).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin save movie to firestore failed", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Create or update a channel.
     */
    suspend fun adminSaveChannel(channel: Channel): Boolean = withContext(Dispatchers.IO) {
        val chanId = channel.id.ifEmpty { "chan_" + System.currentTimeMillis() }
        val finalChan = channel.copy(id = chanId)
        val url = "$RTDB_URL/channels/$chanId.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("id", finalChan.id)
                put("name", finalChan.name)
                put("logoUrl", finalChan.logoUrl)
                put("category", finalChan.category)
                put("userAgent", finalChan.userAgent)
                put("forcedFormat", finalChan.forcedFormat)
                put("streamUrl", finalChan.streamUrl)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving channel to RTDB", e)
            false
        }

        try {
            db.collection("channels").document(chanId).set(finalChan).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin save channel to firestore failed", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Create or update an event channel across all Realtime & Firestore databases.
     */
    suspend fun adminSaveEventChannel(channel: EventChannel): Boolean = withContext(Dispatchers.IO) {
        val chanId = channel.id.ifEmpty { "event_" + System.currentTimeMillis() }
        val serversList = if (channel.servers.isNotEmpty()) {
            channel.servers
        } else if (channel.streamUrl.isNotBlank()) {
            listOf(EventServer(id = "srv_1", name = "السيرفر الرئيسي", url = channel.streamUrl, forcedFormat = channel.forcedFormat.ifEmpty { "m3u8" }))
        } else {
            emptyList()
        }
        val effectiveStreamUrl = channel.streamUrl.ifBlank { serversList.firstOrNull()?.url ?: "" }
        val effectiveFormat = channel.forcedFormat.ifBlank { serversList.firstOrNull()?.forcedFormat ?: "m3u8" }
        val finalChan = channel.copy(
            id = chanId,
            streamUrl = effectiveStreamUrl,
            forcedFormat = effectiveFormat,
            servers = serversList
        )

        val serversJsonArray = org.json.JSONArray().apply {
            for (srv in serversList) {
                put(JSONObject().apply {
                    put("id", srv.id.ifEmpty { "srv_" + System.currentTimeMillis() })
                    put("name", srv.name)
                    put("url", srv.url)
                    put("forcedFormat", srv.forcedFormat)
                })
            }
        }

        val targetChannelIdsArray = JSONArray().apply {
            for (tid in finalChan.targetChannelIds) {
                put(tid)
            }
        }

        val json = JSONObject().apply {
            put("id", finalChan.id)
            put("name", finalChan.name)
            put("imageUrl", finalChan.imageUrl)
            put("streamUrl", finalChan.streamUrl)
            put("forcedFormat", finalChan.forcedFormat)
            put("servers", serversJsonArray)
            put("timestamp", finalChan.timestamp)
            put("eventType", finalChan.eventType)
            put("targetChannelId", finalChan.targetChannelId)
            put("targetChannelIds", targetChannelIdsArray)
            put("startTimeMs", finalChan.startTimeMs)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        var anySuccess = false

        // 1. Primary RTDB
        try {
            val url = "$RTDB_URL/events/$chanId.json"
            val request = Request.Builder().url(url).put(body).build()
            val ok = okHttpClient.newCall(request).execute().use { it.isSuccessful }
            if (ok) anySuccess = true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving event to RTDB", e)
        }

        // 2. Primary RTDB Secured V2
        try {
            val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/events/$chanId.json"
            val request = Request.Builder().url(secureUrl).put(json.toString().toRequestBody(mediaType)).build()
            val ok = okHttpClient.newCall(request).execute().use { it.isSuccessful }
            if (ok) anySuccess = true
        } catch (e: Exception) {
            // Ignore
        }

        // 3. Secondary RTDB (loop-web-7e263)
        try {
            val secUrl = "https://loop-web-7e263-default-rtdb.firebaseio.com/events/$chanId.json"
            val request = Request.Builder().url(secUrl).put(json.toString().toRequestBody(mediaType)).build()
            val ok = okHttpClient.newCall(request).execute().use { it.isSuccessful }
            if (ok) anySuccess = true
        } catch (e: Exception) {
            // Ignore
        }

        // 4. Firestore
        try {
            db.collection("events").document(chanId).set(finalChan).await()
            anySuccess = true
        } catch (e: Throwable) {
            Log.e(TAG, "Admin save main channel to firestore failed", e)
        }
        return@withContext anySuccess
    }

    /**
     * ADMIN TASKS: Create or update a Category.
     */
    suspend fun adminSaveCategory(category: IPTVCategory): Boolean = withContext(Dispatchers.IO) {
        val catId = category.id.ifEmpty { "cat_" + System.currentTimeMillis() }
        val finalCategory = category.copy(id = catId)
        val url = "$RTDB_URL/categories/$catId.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("id", finalCategory.id)
                put("name", finalCategory.name)
                put("type", finalCategory.type)
                put("imageUrl", finalCategory.imageUrl)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving category to RTDB", e)
            false
        }

        try {
            db.collection("categories").document(catId).set(finalCategory).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin save category to firestore failed", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Create or update a series.
     */
    suspend fun adminSaveSeries(series: Series): Boolean = withContext(Dispatchers.IO) {
        val seriesId = series.id.ifEmpty { "series_" + System.currentTimeMillis() }
        val finalSeries = series.copy(id = seriesId)
        val url = "$RTDB_URL/series/$seriesId.json"
        val rtdbSuccess = try {
            val json = JSONObject().apply {
                put("id", finalSeries.id)
                put("title", finalSeries.title)
                put("description", finalSeries.description)
                put("posterUrl", finalSeries.posterUrl)
                put("category", finalSeries.category)
                put("rating", finalSeries.rating)
                put("year", finalSeries.year)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving series to RTDB", e)
            false
        }

        try {
            db.collection("series").document(seriesId).set(finalSeries).await()
        } catch (e: Throwable) {
            Log.e(TAG, "Admin save series to firestore failed", e)
        }
        return@withContext rtdbSuccess
    }

    /**
     * ADMIN TASKS: Delete content by collection and ID.
     */
    suspend fun adminDeleteContent(collection: String, docId: String): Boolean = withContext(Dispatchers.IO) {
        val rtdbCollectionName = if (collection == "categories") "categories"
                                 else if (collection == "channels") "channels"
                                 else if (collection == "events" || collection == "main_channels") "events"
                                 else if (collection == "movies") "movies"
                                 else if (collection == "series") "series"
                                 else collection

        var anySuccess = false
        val urls = mutableListOf(
            "$RTDB_URL/$rtdbCollectionName/$docId.json",
            "$RTDB_URL/$RTDB_SECURE_PREFIX/$rtdbCollectionName/$docId.json"
        )
        if (rtdbCollectionName == "events") {
            urls.add("https://loop-web-7e263-default-rtdb.firebaseio.com/events/$docId.json")
        }

        for (u in urls) {
            try {
                val request = Request.Builder().url(u).delete().build()
                val ok = okHttpClient.newCall(request).execute().use { it.isSuccessful }
                if (ok) anySuccess = true
            } catch (e: Exception) {
                // Ignore
            }
        }

        try {
            val firestoreCol = if (collection == "main_channels") "events" else collection
            db.collection(firestoreCol).document(docId).delete().await()
            anySuccess = true
        } catch (e: Exception) {
            // Ignore
        }

        return@withContext anySuccess
    }

    /**
     * SEEDS THE DATABASE ON STARTUP IF EMPTY
     */
    suspend fun seedDatabaseIfEmpty() {
        try {
            val doc = db.collection("access_codes").document("A1").get().await()
            if (doc.exists()) {
                Log.d(TAG, "Database already seeded!")
                try {
                    val mainChans = db.collection("events").limit(1).get().await()
                    if (mainChans.isEmpty) {
                        seedEventChannelsOnly()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Notice: info about events query status (using local/RTDB fallbacks if blocked): " + e.message)
                }
                return
            }

            Log.d(TAG, "Seeding Firestore with premium IPTV mockup parameters...")

            // 1. Seed Access Codes A1 to A9 (Default active codes)
            for (i in 1..9) {
                db.collection("access_codes").document("A$i").set(
                    mapOf("code" to "A$i", "isActive" to true, "accountId" to "premium_account_a1")
                ).await()
            }

            // 2. Seed IPTV Account info
            db.collection("accounts").document("premium_account_a1").set(
                mapOf(
                    "id" to "premium_account_a1",
                    "name" to "VIP Premium User (+90)",
                    "allowedCategories" to listOf("sports", "action", "news", "scientific", "entertainment"),
                    "expiryDate" to "2028-12-31"
                )
            ).await()

            // 3. Seed Categories
            val categories = listOf(
                IPTVCategory("sports", "القنوات الرياضية", "channel"),
                IPTVCategory("news", "قنوات الأخبار بث مباشر", "channel"),
                IPTVCategory("action", "أفلام الأكشن والإثارة", "movie"),
                IPTVCategory("scientific", "أفلام وثائقية وعلمية", "movie"),
                IPTVCategory("entertainment", "مسلسلات الدراما الحصرية", "series"),
                IPTVCategory("bein1080", "beIN Sports FHD 1080", "exclusive"),
                IPTVCategory("bein740", "beIN Sports HD 740", "exclusive"),
                IPTVCategory("bein480", "beIN Sports SD 480", "exclusive"),
                IPTVCategory("bein240", "beIN Sports LOW 240", "exclusive"),
                IPTVCategory("alwan", "Alwan Sports", "exclusive")
            )
            for (cat in categories) {
                db.collection("categories").document(cat.id).set(cat).await()
            }

            // 4. Seed Live Channels
            // Standard live streams (M3U8)
            val channels = listOf(
                Channel(
                    id = "chan_bein_1",
                    name = "beIN SPORTS Premium HD",
                    logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=150&q=80",
                    streamUrl = "http://sample.vodobox.com/planete_hd_short_clip/planete_hd_short_clip.m3u8", // Working Test HLS stream
                    category = "sports"
                ),
                Channel(
                    id = "chan_aljazeera",
                    name = "الجزيرة الإخبارية مباشر HD",
                    logoUrl = "https://images.unsplash.com/photo-1585829365294-bb7c6384ef81?w=150&q=80",
                    streamUrl = "https://live-aljazeera.akamaized.net/hls/live/2003940/aljazeera/index.m3u8", // Live Arabic Al Jazeera URL
                    category = "news"
                ),
                Channel(
                    id = "chan_alarabiya",
                    name = "العربية مباشر HD",
                    logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=150&q=80",
                    streamUrl = "https://alarabiya-live.akamaized.net/hls/live/2004419/alarabiya/index.m3u8", // Live Arabic Al Arabiya URL
                    category = "news"
                )
            )
            for (chan in channels) {
                db.collection("channels").document(chan.id).set(chan).await()
            }

            // 5. Seed Movies (using real trailer clips/working HLS files)
            val movies = listOf(
                Movie(
                    id = "movie_sintel",
                    title = "فيلم الخيال العلمي: سينتيل (Sintel)",
                    description = "فيلم رسومي فاخر وجميل بتقنية 4K يحكي قصة فتاة تبحث عن تنينها الصغير الضائع في رحلة ملحمية مذهلة.",
                    posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80",
                    streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8", // Working HLS movie stream
                    category = "scientific",
                    duration = "00:15:00",
                    rating = 8.9,
                    year = 2025
                ),
                Movie(
                    id = "movie_bipbop",
                    title = "وثائقي التقنية والشبكات: بيب بوب (BipBop)",
                    description = "عرض واقعي وتجريبي لأنظمة البث المتقدمة في السيرفرات العالمية بطريقة تقنية مثيرة للغاية.",
                    posterUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&q=80",
                    streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8", // Working complex HLS streams
                    category = "scientific",
                    duration = "00:30:15",
                    rating = 7.4,
                    year = 2026
                ),
                Movie(
                    id = "movie_action_matrix",
                    title = "ماتريكس ريفولوشن: البداية (The Matrix)",
                    description = "فيلم أكشن ملحمي يروي قصة البطل نيو في تحرير عقول البشرية من الآلات المستعبدة بلغة بصرية فريدة.",
                    posterUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=400&q=80",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", // Real high-quality HLS streaming action movie
                    category = "action",
                    duration = "02:16:30",
                    rating = 9.2,
                    year = 2024
                )
            )
            for (m in movies) {
                db.collection("movies").document(m.id).set(m).await()
            }

            // 6. Seed Series
            val series = listOf(
                Series(
                    id = "series_thrones",
                    title = "مسلسل الممالك السبع الحصري",
                    description = "صراع عروش ومكائد دموية لا تنتهي من أجل الهيمنة وتتويج الملك على مقاعد الحديد المفقودة.",
                    posterUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&q=80",
                    category = "entertainment",
                    rating = 9.5,
                    year = 2025
                )
            )
            for (s in series) {
                db.collection("series").document(s.id).set(s).await()
            }

            // 7. Seed Seasons
            db.collection("seasons").document("season_thrones_1").set(
                Season(id = "season_thrones_1", seriesId = "series_thrones", number = 1, title = "الموسم الأول")
            ).await()

            // 8. Seed Episodes
            val episodes = listOf(
                Episode(
                    id = "ep_1",
                    seasonId = "season_thrones_1",
                    seriesId = "series_thrones",
                    title = "الحلقة 1: الشتاء قادم",
                    description = "بداية ملحمية من أرض الشمال حيث يتوجب على لورد وينترفل الاستعداد لخطر يهدد المملكة برمتها.",
                    posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=350&q=80",
                    streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8", // Working play link
                    duration = "14 دقيقة",
                    number = 1
                ),
                Episode(
                    id = "ep_2",
                    seasonId = "season_thrones_1",
                    seriesId = "series_thrones",
                    title = "الحلقة 2: طريق الخلافة المستعر",
                    description = "تزداد المؤامرات في بلاط الملك ويتعين على لورد ند ستارك مواجهة خصوم غامضين وشرسين.",
                    posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=350&q=80",
                    streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8", // Working play link
                    duration = "22 دقيقة",
                    number = 2
                )
            )
            for (ep in episodes) {
                db.collection("episodes").document(ep.id).set(ep).await()
            }

            Log.d(TAG, "Seeding Firestore completed perfectly!")
        } catch (e: Throwable) {
            Log.e(TAG, "Error seeding database", e)
        }
    }

    private suspend fun seedEventChannelsOnly() {
        try {
            val mainChans = mutableListOf<EventChannel>()
            // 1. beIN Sports 1080
            for (i in 1..6) {
                mainChans.add(
                    EventChannel(
                        id = "bein1080_$i",
                        name = "beIN Sports $i (FHD 1080)",
                        imageUrl = "https://i.ibb.co/fdg1Yv63/IMG-20260425-180936-479.jpg",
                        streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8",
                        forcedFormat = "m3u8"
                    )
                )
            }
            // 2. beIN Sports 740
            for (i in 1..6) {
                mainChans.add(
                    EventChannel(
                        id = "bein740_$i",
                        name = "beIN Sports $i (HD 740)",
                        imageUrl = "https://i.ibb.co/fdg1Yv63/IMG-20260425-180936-479.jpg",
                        streamUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
                        forcedFormat = "m3u8"
                    )
                )
            }
            // 3. beIN Sports 480
            for (i in 1..6) {
                mainChans.add(
                    EventChannel(
                        id = "bein480_$i",
                        name = "beIN Sports $i (SD 480)",
                        imageUrl = "https://i.ibb.co/fdg1Yv63/IMG-20260425-180936-479.jpg",
                        streamUrl = "https://sample.vodobox.com/planete_hd_short_clip/planete_hd_short_clip.m3u8",
                        forcedFormat = "ts"
                    )
                )
            }
            // 4. beIN Sports 240
            for (i in 1..6) {
                mainChans.add(
                    EventChannel(
                        id = "bein240_$i",
                        name = "beIN Sports $i (LOW 240)",
                        imageUrl = "https://i.ibb.co/fdg1Yv63/IMG-20260425-180936-479.jpg",
                        streamUrl = "https://cph-pms.dplay.com/sintel/global/sintel.m3u8",
                        forcedFormat = "ts"
                    )
                )
            }
            // 5. Alwan Sports (1 to 4)
            for (i in 1..4) {
                mainChans.add(
                    EventChannel(
                        id = "alwan_$i",
                        name = "Alwan Sports $i",
                        imageUrl = "https://i.ibb.co/DD5kx3sC/IMG-250.jpg",
                        streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                        forcedFormat = "m3u8"
                    )
                )
            }

            for (ch in mainChans) {
                adminSaveEventChannel(ch)
            }
            Log.d(TAG, "Seeding main channels successfully completed!")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding main channels", e)
        }
    }

    /**
     * Fetches public main channels from the database with multi-database sync and fallback.
     */
    suspend fun getEventChannels(): List<EventChannel> = withContext(Dispatchers.IO) {
        val allEvents = mutableListOf<EventChannel>()

        // 1. Fetch from Secure node
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/events.json"
        allEvents.addAll(tryFetchEventsFromUrl(secureUrl))

        // 2. Fetch from Legacy RTDB
        val legacyUrl = "$RTDB_URL/events.json"
        allEvents.addAll(tryFetchEventsFromUrl(legacyUrl))

        // 3. Fetch from Secondary Database (loop-web-7e263)
        val secondaryUrl = "https://loop-web-7e263-default-rtdb.firebaseio.com/events.json"
        allEvents.addAll(tryFetchEventsFromUrl(secondaryUrl))

        // 4. Fetch from Firestore fallback
        try {
            val snapshot = db.collection("events").get().await()
            for (doc in snapshot.documents) {
                val id = doc.id
                val name = doc.getString("name") ?: ""
                val streamUrl = doc.getString("streamUrl") ?: ""
                val imageUrl = doc.getString("imageUrl") ?: ""
                val forcedFormat = doc.getString("forcedFormat") ?: "m3u8"
                val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                
                val parsedServers = mutableListOf<EventServer>()
                val rawServers = doc.get("servers") as? List<Map<String, Any>>
                if (rawServers != null) {
                    for (sMap in rawServers) {
                        val sName = (sMap["name"] as? String) ?: "سيرفر"
                        val sUrl = (sMap["url"] as? String) ?: ""
                        val sFmt = (sMap["forcedFormat"] as? String) ?: forcedFormat
                        val sId = (sMap["id"] as? String) ?: "srv_${parsedServers.size}"
                        if (sUrl.isNotBlank()) {
                            parsedServers.add(EventServer(id = sId, name = sName, url = sUrl, forcedFormat = sFmt))
                        }
                    }
                }
                if (parsedServers.isEmpty() && streamUrl.isNotBlank()) {
                    parsedServers.add(EventServer(id = "srv_main", name = "السيرفر الرئيسي", url = streamUrl, forcedFormat = forcedFormat))
                }

                val eventType = doc.getString("eventType") ?: "stream"
                val targetChannelId = doc.getString("targetChannelId") ?: ""
                val targetChannelIds = doc.get("targetChannelIds") as? List<String> ?: emptyList()
                val startTimeMs = doc.getLong("startTimeMs") ?: 0L

                val effectiveStreamUrl = streamUrl.ifBlank { parsedServers.firstOrNull()?.url ?: "" }
                if (name.isNotBlank() && (effectiveStreamUrl.isNotBlank() || parsedServers.isNotEmpty() || eventType == "match" || targetChannelId.isNotBlank() || targetChannelIds.isNotEmpty())) {
                    allEvents.add(
                        EventChannel(
                            id = id,
                            name = name,
                            imageUrl = imageUrl,
                            streamUrl = effectiveStreamUrl,
                            forcedFormat = forcedFormat,
                            servers = parsedServers,
                            timestamp = ts,
                            eventType = eventType,
                            targetChannelId = targetChannelId,
                            targetChannelIds = targetChannelIds,
                            startTimeMs = startTimeMs
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore events query info: ${e.message}")
        }

        // Deduplicate and filter out incomplete channels
        val result = allEvents
            .filter { it.name.isNotBlank() && (it.streamUrl.isNotBlank() || it.servers.isNotEmpty() || it.eventType == "match" || it.targetChannelId.isNotBlank()) }
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }

        return@withContext result
    }

    private fun tryFetchEventsFromUrl(url: String): List<EventChannel> {
        val list = mutableListOf<EventChannel>()
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null" && bodyStr != "{}") {
                        if (bodyStr.startsWith("[")) {
                            val jsonArray = org.json.JSONArray(bodyStr)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.optJSONObject(i) ?: continue
                                val id = obj.optString("id", "").ifEmpty { "event_$i" }
                                val name = obj.optString("name", "")
                                val streamUrl = obj.optString("streamUrl", "")
                                val forcedFmt = obj.optString("forcedFormat", "m3u8")
                                val parsedServers = mutableListOf<EventServer>()
                                val srvArr = obj.optJSONArray("servers")
                                if (srvArr != null) {
                                    for (sIdx in 0 until srvArr.length()) {
                                        val sObj = srvArr.optJSONObject(sIdx) ?: continue
                                        val sUrl = sObj.optString("url", "")
                                        if (sUrl.isNotBlank()) {
                                            parsedServers.add(
                                                EventServer(
                                                    id = sObj.optString("id", "srv_$sIdx"),
                                                    name = sObj.optString("name", "سيرفر ${sIdx + 1}"),
                                                    url = sUrl,
                                                    forcedFormat = sObj.optString("forcedFormat", forcedFmt)
                                                )
                                            )
                                        }
                                    }
                                }
                                if (parsedServers.isEmpty() && streamUrl.isNotBlank()) {
                                    parsedServers.add(EventServer(id = "srv_main", name = "السيرفر الرئيسي", url = streamUrl, forcedFormat = forcedFmt))
                                }
                                val isMatchOrTarget = obj.optString("eventType") == "match" || obj.optString("targetChannelId").isNotBlank() || obj.has("targetChannelIds")
                                val effectiveStreamUrl = streamUrl.ifBlank { parsedServers.firstOrNull()?.url ?: "" }
                                if (name.isNotBlank() && (effectiveStreamUrl.isNotBlank() || parsedServers.isNotEmpty() || isMatchOrTarget)) {
                                    val tids = mutableListOf<String>()
                                    val tidArr = obj.optJSONArray("targetChannelIds")
                                    if (tidArr != null) {
                                        for (j in 0 until tidArr.length()) {
                                            tids.add(tidArr.optString(j))
                                        }
                                    }
                                    list.add(
                                        EventChannel(
                                            id = id,
                                            name = name,
                                            imageUrl = obj.optString("imageUrl", ""),
                                            streamUrl = effectiveStreamUrl,
                                            forcedFormat = forcedFmt,
                                            servers = parsedServers,
                                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                            eventType = obj.optString("eventType", "stream"),
                                            targetChannelId = obj.optString("targetChannelId", ""),
                                            targetChannelIds = tids,
                                            startTimeMs = obj.optLong("startTimeMs", 0L)
                                        )
                                    )
                                }
                            }
                        } else if (bodyStr.startsWith("{")) {
                            val json = JSONObject(bodyStr)
                            val keys = json.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val obj = json.optJSONObject(key)
                                if (obj != null) {
                                    val name = obj.optString("name", "")
                                    val streamUrl = obj.optString("streamUrl", "")
                                    val forcedFmt = obj.optString("forcedFormat", "m3u8")
                                    val parsedServers = mutableListOf<EventServer>()
                                    val srvArr = obj.optJSONArray("servers")
                                    if (srvArr != null) {
                                        for (sIdx in 0 until srvArr.length()) {
                                            val sObj = srvArr.optJSONObject(sIdx) ?: continue
                                            val sUrl = sObj.optString("url", "")
                                            if (sUrl.isNotBlank()) {
                                                parsedServers.add(
                                                    EventServer(
                                                        id = sObj.optString("id", "srv_$sIdx"),
                                                        name = sObj.optString("name", "سيرفر ${sIdx + 1}"),
                                                        url = sUrl,
                                                        forcedFormat = sObj.optString("forcedFormat", forcedFmt)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (parsedServers.isEmpty() && streamUrl.isNotBlank()) {
                                        parsedServers.add(EventServer(id = "srv_main", name = "السيرفر الرئيسي", url = streamUrl, forcedFormat = forcedFmt))
                                    }
                                    val isMatchOrTarget = obj.optString("eventType") == "match" || obj.optString("targetChannelId").isNotBlank() || obj.has("targetChannelIds")
                                    val effectiveStreamUrl = streamUrl.ifBlank { parsedServers.firstOrNull()?.url ?: "" }
                                    if (name.isNotBlank() || effectiveStreamUrl.isNotBlank() || isMatchOrTarget) {
                                        val tids = mutableListOf<String>()
                                        val tidArr = obj.optJSONArray("targetChannelIds")
                                        if (tidArr != null) {
                                            for (j in 0 until tidArr.length()) {
                                                tids.add(tidArr.optString(j))
                                            }
                                        }
                                        list.add(
                                            EventChannel(
                                                id = obj.optString("id", key).ifEmpty { key },
                                                name = name.ifEmpty { "حدث مباشر" },
                                                imageUrl = obj.optString("imageUrl", ""),
                                                streamUrl = effectiveStreamUrl,
                                                forcedFormat = forcedFmt,
                                                servers = parsedServers,
                                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                                eventType = obj.optString("eventType", "stream"),
                                                targetChannelId = obj.optString("targetChannelId", ""),
                                                targetChannelIds = tids,
                                                startTimeMs = obj.optLong("startTimeMs", 0L)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching channels from $url: ${e.message}")
        }
        return list
    }

    suspend fun getAiApiKeys(): List<AiApiKey> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AiApiKey>()
        val url = "$RTDB_URL/ai_api_keys.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val keyId = keys.next()
                            val obj = json.optJSONObject(keyId) ?: continue
                            list.add(
                                AiApiKey(
                                    id = keyId,
                                    key = obj.optString("key", ""),
                                    name = obj.optString("name", ""),
                                    endpointUrl = obj.optString("endpointUrl", "https://models.inference.ai.azure.com/chat/completions"),
                                    modelName = obj.optString("modelName", "gpt-4o-mini"),
                                    createdAt = obj.optLong("createdAt", 0L)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching AI keys from RTDB", e)
        }
        return@withContext list.sortedBy { it.createdAt }
    }

    suspend fun saveAiApiKey(apiKey: AiApiKey): Boolean = withContext(Dispatchers.IO) {
        val id = apiKey.id.ifEmpty { "ai_key_" + System.currentTimeMillis() }
        val finalKey = apiKey.copy(id = id)
        val url = "$RTDB_URL/ai_api_keys/$id.json"
        try {
            val json = JSONObject().apply {
                put("id", finalKey.id)
                put("key", finalKey.key)
                put("name", finalKey.name)
                put("endpointUrl", finalKey.endpointUrl)
                put("modelName", finalKey.modelName)
                put("createdAt", finalKey.createdAt)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving AI key to RTDB", e)
            false
        }
    }

    suspend fun deleteAiApiKey(id: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/ai_api_keys/$id.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting AI key from RTDB", e)
            false
        }
    }

    // --- Notifications Management (REST-based) ---
    suspend fun getNotifications(): List<AppNotification> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, AppNotification>()
        
        fun parseNotifJson(bodyStr: String) {
            if (bodyStr.isNotEmpty() && bodyStr != "null") {
                val json = JSONObject(bodyStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val obj = json.optJSONObject(id) ?: continue
                    map[id] = AppNotification(
                        id = id,
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle", ""),
                        body = obj.optString("body", ""),
                        imageUrl = obj.optString("imageUrl", "").ifEmpty { null },
                        linkUrl = obj.optString("linkUrl", "").ifEmpty { null },
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                }
            }
        }

        // Try primary notifications node
        try {
            val request = Request.Builder().url("$RTDB_URL/notifications.json").build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    parseNotifJson(response.body?.string() ?: "")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching notifications from RTDB: ${e.message}")
        }

        // Also fetch from secured node if available
        try {
            val requestSecured = Request.Builder().url("$RTDB_URL/$RTDB_SECURE_PREFIX/notifications.json").build()
            okHttpClient.newCall(requestSecured).execute().use { response ->
                if (response.isSuccessful) {
                    parseNotifJson(response.body?.string() ?: "")
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        return@withContext map.values.sortedByDescending { it.timestamp }
    }

    suspend fun saveNotification(notification: AppNotification): Boolean = withContext(Dispatchers.IO) {
        val id = notification.id.ifEmpty { System.currentTimeMillis().toString() }
        val finalNotification = notification.copy(id = id)
        var success = false
        try {
            val json = JSONObject().apply {
                put("id", finalNotification.id)
                put("title", finalNotification.title)
                put("subtitle", finalNotification.subtitle)
                put("body", finalNotification.body)
                if (finalNotification.imageUrl != null) {
                    put("imageUrl", finalNotification.imageUrl)
                }
                if (finalNotification.linkUrl != null) {
                    put("linkUrl", finalNotification.linkUrl)
                }
                put("timestamp", finalNotification.timestamp)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            
            val request1 = Request.Builder().url("$RTDB_URL/notifications/$id.json").put(body).build()
            okHttpClient.newCall(request1).execute().use { response ->
                if (response.isSuccessful) success = true
            }

            val bodySecured = json.toString().toRequestBody(mediaType)
            val request2 = Request.Builder().url("$RTDB_URL/$RTDB_SECURE_PREFIX/notifications/$id.json").put(bodySecured).build()
            okHttpClient.newCall(request2).execute().use { response ->
                if (response.isSuccessful) success = true
            }
            Log.d(TAG, "Notification $id saved to Firebase. success=$success")
        } catch (e: Exception) {
            Log.w(TAG, "Error saving notification to RTDB: ${e.message}")
        }
        return@withContext success
    }

    suspend fun deleteNotification(id: String): Boolean = withContext(Dispatchers.IO) {
        var success = false
        try {
            val request1 = Request.Builder().url("$RTDB_URL/notifications/$id.json").delete().build()
            okHttpClient.newCall(request1).execute().use { response ->
                if (response.isSuccessful) success = true
            }
            val request2 = Request.Builder().url("$RTDB_URL/$RTDB_SECURE_PREFIX/notifications/$id.json").delete().build()
            okHttpClient.newCall(request2).execute().use { response ->
                if (response.isSuccessful) success = true
            }
            Log.d(TAG, "Notification $id deleted from Firebase. success=$success")
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting notification from RTDB: ${e.message}")
        }
        return@withContext success
    }

    // --- Access Codes Management (REST-based) ---
    suspend fun getAccessCodes(): List<AccessCode> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AccessCode>()
        val url = "$RTDB_URL/access_codes.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val code = keys.next()
                            val obj = json.optJSONObject(code)
                            val isActive = obj?.optBoolean("isActive", true) ?: true
                            val accountId = obj?.optString("accountId", "") ?: ""
                            list.add(
                                AccessCode(
                                    code = code,
                                    isActive = isActive,
                                    accountId = accountId
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching access codes from RTDB: ${e.message}")
        }
        return@withContext list
    }

    // --- Promotional Ads & Announcements Management (REST-based) ---
    suspend fun getPromoAds(): List<PromoAd> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PromoAd>()
        val url = "$RTDB_URL/promo_ads.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val obj = json.optJSONObject(id) ?: continue
                            list.add(
                                PromoAd(
                                    id = id,
                                    title = obj.optString("title", ""),
                                    description = obj.optString("description", ""),
                                    imageUrl = obj.optString("imageUrl", ""),
                                    targetUrl = obj.optString("targetUrl", ""),
                                    buttonText = obj.optString("buttonText", "انضم الآن"),
                                    badge = obj.optString("badge", "إعلان مميز"),
                                    isEnabled = obj.optBoolean("isEnabled", true),
                                    type = obj.optString("type", "telegram"),
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching promo ads from RTDB: ${e.message}")
        }
        return@withContext list.sortedByDescending { it.timestamp }
    }

    suspend fun savePromoAd(ad: PromoAd): Boolean = withContext(Dispatchers.IO) {
        val id = ad.id.ifEmpty { "ad_${System.currentTimeMillis()}" }
        val finalAd = ad.copy(id = id)
        val url = "$RTDB_URL/promo_ads/$id.json"
        try {
            val json = JSONObject().apply {
                put("id", finalAd.id)
                put("title", finalAd.title)
                put("description", finalAd.description)
                put("imageUrl", finalAd.imageUrl)
                put("targetUrl", finalAd.targetUrl)
                put("buttonText", finalAd.buttonText)
                put("badge", finalAd.badge)
                put("isEnabled", finalAd.isEnabled)
                put("type", finalAd.type)
                put("timestamp", finalAd.timestamp)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Error saving promo ad to RTDB: ${e.message}")
            false
        }
    }

    suspend fun deletePromoAd(id: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/promo_ads/$id.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting promo ad from RTDB: ${e.message}")
            false
        }
    }

    // --- Home Hero Banners Management (REST-based) ---
    suspend fun getHomeHeroBanners(): List<HomeHeroBanner> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HomeHeroBanner>()
        val url = "$RTDB_URL/home_hero_banners.json"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val id = keys.next()
                            val obj = json.optJSONObject(id) ?: continue
                            list.add(
                                HomeHeroBanner(
                                    id = id,
                                    title = obj.optString("title", ""),
                                    subtitle = obj.optString("subtitle", ""),
                                    imageUrl = obj.optString("imageUrl", ""),
                                    tag = obj.optString("tag", "🔥 حصري 4K"),
                                    targetType = obj.optString("targetType", "tab"),
                                    targetValue = obj.optString("targetValue", "channels"),
                                    order = obj.optInt("order", 0),
                                    isEnabled = obj.optBoolean("isEnabled", true)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching home hero banners from RTDB: ${e.message}")
        }
        return@withContext list.sortedBy { it.order }
    }

    suspend fun saveHomeHeroBanner(banner: HomeHeroBanner): Boolean = withContext(Dispatchers.IO) {
        val id = banner.id.ifEmpty { "banner_${System.currentTimeMillis()}" }
        val finalBanner = banner.copy(id = id)
        val url = "$RTDB_URL/home_hero_banners/$id.json"
        try {
            val json = JSONObject().apply {
                put("id", finalBanner.id)
                put("title", finalBanner.title)
                put("subtitle", finalBanner.subtitle)
                put("imageUrl", finalBanner.imageUrl)
                put("tag", finalBanner.tag)
                put("targetType", finalBanner.targetType)
                put("targetValue", finalBanner.targetValue)
                put("order", finalBanner.order)
                put("isEnabled", finalBanner.isEnabled)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).put(body).build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Error saving home hero banner to RTDB: ${e.message}")
            false
        }
    }

    suspend fun deleteHomeHeroBanner(id: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$RTDB_URL/home_hero_banners/$id.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            okHttpClient.newCall(request).execute().use { response -> response.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting home hero banner from RTDB: ${e.message}")
            false
        }
    }

    // --- Home Section Config Management (REST-based) ---
    suspend fun getHomeConfig(): HomeSectionConfig = withContext(Dispatchers.IO) {
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/home_config.json"
        val standardUrl = "$RTDB_URL/home_config.json"
        
        for (url in listOf(secureUrl, standardUrl)) {
            try {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        if (bodyStr.isNotEmpty() && bodyStr != "null" && bodyStr != "{}") {
                            val json = JSONObject(bodyStr)
                            return@withContext HomeSectionConfig(
                                showHeroCarousel = json.optBoolean("showHeroCarousel", true),
                                showQuickNav = json.optBoolean("showQuickNav", true),
                                showPromoAds = json.optBoolean("showPromoAds", true),
                                showTrendingChannels = json.optBoolean("showTrendingChannels", true),
                                showLatestMovies = json.optBoolean("showLatestMovies", true),
                                showPopularSeries = json.optBoolean("showPopularSeries", true),
                                showAnnouncements = json.optBoolean("showAnnouncements", true),
                                showOnlineUsers = json.optBoolean("showOnlineUsers", true),
                                showNewsSection = json.optBoolean("showNewsSection", true),
                                showDevSection = json.optBoolean("showDevSection", true),
                                announcementText = json.optString("announcementText", "مرحباً بكم في تطبيق +90 - استمتعوا بأقوى البث المباشر وأحدث مكتبة سينمائية بجودة 4K فائقة!"),
                                customTitle = json.optString("customTitle", "الرئيسية")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching home config from $url: ${e.message}")
            }
        }
        return@withContext HomeSectionConfig()
    }

    suspend fun saveHomeConfig(config: HomeSectionConfig): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("showHeroCarousel", config.showHeroCarousel)
            put("showQuickNav", config.showQuickNav)
            put("showPromoAds", config.showPromoAds)
            put("showTrendingChannels", config.showTrendingChannels)
            put("showLatestMovies", config.showLatestMovies)
            put("showPopularSeries", config.showPopularSeries)
            put("showAnnouncements", config.showAnnouncements)
            put("showOnlineUsers", config.showOnlineUsers)
            put("showNewsSection", config.showNewsSection)
            put("showDevSection", config.showDevSection)
            put("announcementText", config.announcementText)
            put("customTitle", config.customTitle)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        var anySuccess = false

        val urls = listOf(
            "$RTDB_URL/home_config.json",
            "$RTDB_URL/$RTDB_SECURE_PREFIX/home_config.json"
        )
        for (u in urls) {
            try {
                val request = Request.Builder().url(u).put(body).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) anySuccess = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error saving home config to $u: ${e.message}")
            }
        }
        return@withContext anySuccess
    }

    suspend fun getVipCodes(): List<VipCode> = withContext(Dispatchers.IO) {
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes.json"
        val legacyUrl = "$RTDB_URL/vip_codes.json"
        
        // 1. Try secure v2 node first
        val secureList = tryFetchVipCodesFromUrl(secureUrl)
        if (secureList.isNotEmpty()) {
            return@withContext secureList
        }

        // 2. Fallback to legacy node and sync to secure node
        val legacyList = tryFetchVipCodesFromUrl(legacyUrl)
        if (legacyList.isNotEmpty()) {
            Log.d(TAG, "Migrating ${legacyList.size} VIP codes to secured node...")
            for (item in legacyList) {
                try {
                    val upperCode = item.code.trim().uppercase()
                    val itemSecureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
                    val json = JSONObject().apply {
                        put("code", upperCode)
                        put("host", item.host)
                        put("username", item.username)
                        put("password", item.password)
                        put("forcedFormat", item.forcedFormat)
                        put("maxUsers", item.maxUsers)
                        put("currentUsers", item.currentUsers)
                        put("allowVpn", item.allowVpn)
                        val arr = JSONArray()
                        item.allowedTypes.forEach { arr.put(it) }
                        put("allowedTypes", arr)
                    }
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)
                    val putReq = Request.Builder().url(itemSecureUrl).put(body).build()
                    okHttpClient.newCall(putReq).execute().close()
                } catch (e: Exception) {
                    // Ignore migration errors for individual items
                }
            }
            return@withContext legacyList
        }
        return@withContext emptyList()
    }

    private fun tryFetchVipCodesFromUrl(url: String): List<VipCode> {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    if (bodyStr.isNotEmpty() && bodyStr != "null") {
                        val json = JSONObject(bodyStr)
                        val result = mutableListOf<VipCode>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = json.getJSONObject(key)
                            val allowedTypesList = mutableListOf<String>()
                            val typesArr = obj.optJSONArray("allowedTypes")
                            if (typesArr != null) {
                                for (i in 0 until typesArr.length()) {
                                    val strVal = typesArr.optString(i)
                                    if (strVal.isNotEmpty()) allowedTypesList.add(strVal)
                                }
                            }
                            if (allowedTypesList.isEmpty()) allowedTypesList.addAll(listOf("live", "movies", "series"))

                            result.add(VipCode(
                                code = key,
                                host = obj.optString("host"),
                                username = obj.optString("username"),
                                password = obj.optString("password"),
                                forcedFormat = obj.optString("forcedFormat"),
                                maxUsers = obj.optInt("maxUsers", 1),
                                currentUsers = obj.optInt("currentUsers", 0),
                                lastActive = obj.optLong("lastActive", 0),
                                lastActiveSessionId = obj.optString("lastActiveSessionId", ""),
                                allowedTypes = allowedTypesList,
                                allowVpn = obj.optBoolean("allowVpn", false)
                            ))
                        }
                        return result
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching vip codes from $url: ${e.message}")
        }
        return emptyList()
    }

    suspend fun saveVipCode(vipCode: VipCode): Boolean = withContext(Dispatchers.IO) {
        val upperCode = vipCode.code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
        val legacyUrl = "$RTDB_URL/vip_codes/$upperCode.json"
        try {
            val json = JSONObject().apply {
                put("code", upperCode)
                put("host", vipCode.host)
                put("username", vipCode.username)
                put("password", vipCode.password)
                put("forcedFormat", vipCode.forcedFormat)
                put("maxUsers", vipCode.maxUsers)
                put("currentUsers", vipCode.currentUsers)
                put("allowVpn", vipCode.allowVpn)
                val arr = JSONArray()
                vipCode.allowedTypes.forEach { arr.put(it) }
                put("allowedTypes", arr)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)
            
            // Save to secure node
            val secureReq = Request.Builder().url(secureUrl).put(body).build()
            val res1 = okHttpClient.newCall(secureReq).execute().use { it.isSuccessful }

            // Sync to legacy node
            try {
                val legacyReq = Request.Builder().url(legacyUrl).put(body).build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }

            return@withContext res1
        } catch (e: Exception) {
            Log.e(TAG, "Error saving vip code to RTDB", e)
        }
        return@withContext false
    }

    suspend fun deleteVipCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
        val legacyUrl = "$RTDB_URL/vip_codes/$upperCode.json"
        try {
            val secureReq = Request.Builder().url(secureUrl).delete().build()
            val res1 = okHttpClient.newCall(secureReq).execute().use { it.isSuccessful }
            try {
                val legacyReq = Request.Builder().url(legacyUrl).delete().build()
                okHttpClient.newCall(legacyReq).execute().close()
            } catch (e: Exception) {
                // Ignore
            }
            return@withContext res1
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting vip code from RTDB", e)
        }
        return@withContext false
    }

    suspend fun updateVipCodeUserCount(code: String, delta: Int): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
        try {
            val request = Request.Builder().url(secureUrl).build()
            val currentJson = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                response.body?.string() ?: ""
            }
            if (currentJson.isEmpty() || currentJson == "null") return@withContext false
            val obj = JSONObject(currentJson)
            val currentCount = obj.optInt("currentUsers", 0)
            val newCount = (currentCount + delta).coerceAtLeast(0)
            
            val updateJson = JSONObject().apply {
                put("currentUsers", newCount)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = updateJson.toString().toRequestBody(mediaType)
            val patchRequest = Request.Builder().url(secureUrl).patch(body).build()
            okHttpClient.newCall(patchRequest).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vip code user count", e)
        }
        return@withContext false
    }

    suspend fun updateVipCodeLastActive(code: String, sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
        try {
            val updateJson = JSONObject().apply {
                put("lastActive", System.currentTimeMillis())
                put("lastActiveSessionId", sessionId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = updateJson.toString().toRequestBody(mediaType)
            val patchRequest = Request.Builder().url(secureUrl).patch(body).build()
            okHttpClient.newCall(patchRequest).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vip code last active", e)
        }
        return@withContext false
    }

    suspend fun setVipCodeUserCount(code: String, count: Int, sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val upperCode = code.trim().uppercase()
        val secureUrl = "$RTDB_URL/$RTDB_SECURE_PREFIX/vip_codes/$upperCode.json"
        try {
            val updateJson = JSONObject().apply {
                put("currentUsers", count)
                put("lastActive", System.currentTimeMillis())
                put("lastActiveSessionId", sessionId)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = updateJson.toString().toRequestBody(mediaType)
            val patchRequest = Request.Builder().url(secureUrl).patch(body).build()
            okHttpClient.newCall(patchRequest).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting vip code user count", e)
        }
        return@withContext false
    }

    /**
     * DIAGNOSTIC SECURITY TESTER:
     * Tests live accessibility of legacy vs secured nodes to verify Firebase Security Rules.
     */
    suspend fun testEndpointSecurity(isLegacy: Boolean, customPath: String? = null): Triple<Int, String, Boolean> = withContext(Dispatchers.IO) {
        val path = customPath ?: if (isLegacy) "activation_codes/A1.json" else "$RTDB_SECURE_PREFIX/activation_codes/A1.json"
        val targetUrl = "$RTDB_URL/$path"
        try {
            val request = Request.Builder().url(targetUrl).build()
            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyStr = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful && bodyStr.isNotEmpty() && bodyStr != "null"
                return@withContext Triple(code, bodyStr.take(150), isSuccess)
            }
        } catch (e: Exception) {
            return@withContext Triple(-1, e.message ?: "Network Error", false)
        }
    }


    // Presence system
    private var presenceJob: kotlinx.coroutines.Job? = null
    private var activeChannelUserId: String? = null
    private var activeChannelSafeId: String? = null

    suspend fun joinChannelPresence(presenceId: String, onCountChanged: (Int) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val safeId = presenceId.replace(Regex("[^a-zA-Z0-9أ-ي]"), "_")
            val url = "$RTDB_URL/presence/$safeId.json"
            
            // Unique user ID for this stream session
            val userId = "user_" + System.currentTimeMillis() + "_" + (1000..9999).random()
            activeChannelUserId = userId
            activeChannelSafeId = safeId
            
            // Put current user
            val putUrl = "$RTDB_URL/presence/$safeId/$userId.json"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = "true".toRequestBody(mediaType)
            val putReq = Request.Builder().url(putUrl).put(body).build()
            try {
                okHttpClient.newCall(putReq).execute().close()
            } catch (_: Exception) {}
            
            presenceJob?.cancel()
            presenceJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        // Re-ping heartbeat every loop
                        val pingReq = Request.Builder().url(putUrl).put(body).build()
                        try { okHttpClient.newCall(pingReq).execute().close() } catch (_: Exception) {}

                        val getReq = Request.Builder().url(url).build()
                        okHttpClient.newCall(getReq).execute().use { response ->
                            if (response.isSuccessful) {
                                val bodyStr = response.body?.string() ?: ""
                                if (bodyStr.isNotEmpty() && bodyStr != "null") {
                                    val json = JSONObject(bodyStr)
                                    // Accurate count of current keys in this channel node
                                    val count = json.length().coerceAtLeast(1)
                                    withContext(Dispatchers.Main) {
                                        onCountChanged(count)
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        onCountChanged(1)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore polling error
                    }
                    kotlinx.coroutines.delay(6000)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun leaveChannelPresence(presenceId: String) = withContext(Dispatchers.IO) {
        presenceJob?.cancel()
        presenceJob = null
        val uId = activeChannelUserId
        val sId = activeChannelSafeId ?: presenceId.replace(Regex("[^a-zA-Z0-9أ-ي]"), "_")
        if (uId != null) {
            try {
                val delUrl = "$RTDB_URL/presence/$sId/$uId.json"
                val delReq = Request.Builder().url(delUrl).delete().build()
                okHttpClient.newCall(delReq).execute().close()
            } catch (_: Exception) {}
        }
        activeChannelUserId = null
        activeChannelSafeId = null
    }
}
