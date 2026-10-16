package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

@UnstableApi
object ExoPlayerDataSourceUtils {
    val DEFAULT_USER_AGENTS = listOf(
        "IPTVSmartersPro",
        "VLC/3.0.18 LibVLC/3.0.18",
        "GSE-SMART-IPTV",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "ExoPlayer/2.19.1 (Linux;Android 13) AndroidXMedia3/1.2.0"
    )
    val DEFAULT_USER_AGENT = DEFAULT_USER_AGENTS[0]

    /**
     * Builds a production OkHttpClient with robust TCP keep-alive, redirect handling,
     * and non-intrusive request handling (no artificial header spoofing or fake referers).
     */
    fun createOkHttpClient(userAgent: String? = null): OkHttpClient {
        val selectedUserAgent = if (!userAgent.isNullOrBlank()) {
            userAgent
        } else {
            DEFAULT_USER_AGENT
        }

        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(8, 60, TimeUnit.SECONDS))
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", selectedUserAgent)
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                    .header("Icy-MetaData", "1")
                
                val request = requestBuilder.build()
                val response: Response
                val startTime = System.currentTimeMillis()
                try {
                    response = chain.proceed(request)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    val safeUrl = request.url.toString().substringBefore("?")
                    LiveStreamLogger.e("NETWORK_ERROR", "Request failed ($duration ms) for $safeUrl: ${e.javaClass.simpleName} - ${e.message}")
                    throw e
                }
                
                val duration = System.currentTimeMillis() - startTime
                LiveStreamLogger.lastKnownHttpCode = response.code

                if (!response.isSuccessful) {
                    val safeUrl = request.url.toString().substringBefore("?")
                    when (response.code) {
                        401, 403 -> LiveStreamLogger.log("HTTP_403", "Auth/Session expired or forbidden ($duration ms) for $safeUrl")
                        404 -> LiveStreamLogger.log("HTTP_404", "Resource not found on server ($duration ms) for $safeUrl")
                        429 -> LiveStreamLogger.log("HTTP_429", "Rate limited by server ($duration ms) for $safeUrl")
                        in 500..599 -> LiveStreamLogger.log("HTTP_5XX", "Server internal error (${response.code}) for $safeUrl")
                        else -> LiveStreamLogger.log("HTTP_ERROR", "HTTP ${response.code} ($duration ms) for $safeUrl")
                    }
                }
                
                response
            }
            .build()
    }

    /**
     * Balanced LoadControl for Live HLS and MPEG-TS streams:
     * - minBufferMs: 15,000ms (15s) guarantees 2-3 standard segments buffered to survive network jitters.
     * - maxBufferMs: 50,000ms (50s) avoids memory pressure while preventing buffer starvation.
     * - bufferForPlaybackMs: 2,500ms (2.5s) fast startup.
     * - bufferForPlaybackAfterRebufferMs: 5,000ms (5s) prevents immediate re-buffering loops after stall.
     * - backBufferDurationMs: 10,000ms (10s) keeps past keyframes for smooth seeking.
     */
    fun createLiveLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10000, // minBufferMs
                50000, // maxBufferMs
                1000,  // bufferForPlaybackMs - Start almost instantly
                2000   // bufferForPlaybackAfterRebufferMs - Recover quickly
            )
            .setBackBuffer(10000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Ultra-Fast & High-Stability LoadControl for VOD (Movies & Series):
     * - bufferForPlaybackMs: 500ms (0.5s) - Instant startup and reception without delay!
     * - bufferForPlaybackAfterRebufferMs: 1000ms (1s) - Super quick recovery.
     * - minBufferMs: 30,000ms (30s) - Robust forward buffer prevents stalling and black screens.
     * - maxBufferMs: 120,000ms (120s) - Generous buffer for continuous uninterrupted playback.
     * - backBufferDurationMs: 30,000ms (30s) - Instant rewinding without re-buffering.
     */
    fun createVodLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // minBufferMs
                120000, // maxBufferMs
                250,   // bufferForPlaybackMs (Extreme Instant initial playback!)
                1000   // bufferForPlaybackAfterRebufferMs
            )
            .setBackBuffer(30000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * LoadErrorHandlingPolicy with retries for live manifests and media segments before declaring fatal error.
     */
    fun createLiveErrorPolicy(): DefaultLoadErrorHandlingPolicy {
        return object : DefaultLoadErrorHandlingPolicy(1000) {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return when (dataType) {
                    C.DATA_TYPE_MANIFEST -> 5
                    C.DATA_TYPE_MEDIA -> 4
                    else -> 3
                }
            }
        }
    }

    /**
     * Fast Error Handling Policy for VOD Candidate format probing (MKV -> MP4 -> M3U8 -> TS):
     * Fails quickly on 404/403/invalid formats so the player can switch candidates instantly.
     */
    fun createVodErrorPolicy(): DefaultLoadErrorHandlingPolicy {
        return object : DefaultLoadErrorHandlingPolicy(300) {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 1
            }
        }
    }

    /**
     * Creates a DefaultMediaSourceFactory configured with:
     * 1. TS Extractors that support Non-IDR keyframes and access unit detection (essential for IPTV TS streams).
     * 2. HLS Extractor factory with CEA-608 declarations enabled.
     * 3. OkHttp Data Source for resilient HTTP streaming.
     */
    fun createMediaSourceFactory(context: Context, userAgent: String? = null): DefaultMediaSourceFactory {
        val client = createOkHttpClient(userAgent)
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(client)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setConstantBitrateSeekingEnabled(true)

        val hlsExtractorFactory = DefaultHlsExtractorFactory(
            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
            DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS,
            true
        )

        return DefaultMediaSourceFactory(defaultDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(createLiveErrorPolicy())
    }

    /**
     * Specialized MediaSourceFactory for Movies and Series (VOD) with instant candidate failover.
     */
    fun createVodMediaSourceFactory(context: Context, userAgent: String? = null): DefaultMediaSourceFactory {
        val client = createOkHttpClient(userAgent)
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(client)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
            )
            .setConstantBitrateSeekingEnabled(true)

        return DefaultMediaSourceFactory(defaultDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(createVodErrorPolicy())
    }

    /**
     * Builds a MediaItem with proper LiveConfiguration.
     * Letting ExoPlayer dynamically compute target live offset based on target duration
     * prevents requesting segments that are not yet published on the server.
     */
    fun createLiveMediaItem(uriString: String, mimeType: String? = null): MediaItem {
        val liveConfig = MediaItem.LiveConfiguration.Builder()
            // Conservative speeds prevent micro-stutters and buffer starvation
            .setMinPlaybackSpeed(0.98f)
            .setMaxPlaybackSpeed(1.02f)
            .build()

        return MediaItem.Builder()
            .setUri(Uri.parse(uriString))
            .apply {
                if (!mimeType.isNullOrEmpty()) {
                    setMimeType(mimeType)
                }
            }
            .setLiveConfiguration(liveConfig)
            .build()
    }
}
