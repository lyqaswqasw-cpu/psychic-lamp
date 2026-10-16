package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    // Unity Ads & Unit IDs provided by user
    const val APP_KEY = "27b46bb1d"
    const val INTERSTITIAL_UNIT_ID = "0guf5xa00hisr805"
    const val REWARDED_UNIT_ID = "1yh48y27ldxhczle"

    private var isInitializedLocally = false
    private var isInitializing = false
    private var isInterstitialLoaded = false
    private var isRewardedLoaded = false
    private var lastInterstitialShowTime = 0L
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 25000L // 25 seconds smart interval

    fun initialize(context: Context, testMode: Boolean = false) {
        if (isInitializedLocally || isInitializing) return
        isInitializing = true

        try {
            UnityAds.initialize(
                context.applicationContext,
                APP_KEY,
                testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isInitializedLocally = true
                        isInitializing = false
                        Log.i(TAG, "Unity Ads initialized successfully with App Key: $APP_KEY")
                        preloadAds(context.applicationContext)
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        isInitializing = false
                        Log.w(TAG, "Unity Ads initialization failed: $error, message: $message")
                    }
                }
            )
        } catch (e: Throwable) {
            isInitializing = false
            Log.e(TAG, "Exception during Unity Ads init", e)
        }
    }

    fun preloadAds(context: Context) {
        if (!UnityAds.isInitialized) {
            initialize(context)
            return
        }
        loadInterstitial(context)
        loadRewardedVideo(context)
    }

    fun loadInterstitial(context: Context, onLoaded: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (!UnityAds.isInitialized) {
            initialize(context)
        }
        try {
            UnityAds.load(INTERSTITIAL_UNIT_ID, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    isInterstitialLoaded = true
                    Log.i(TAG, "Unity Interstitial Loaded: $placementId")
                    onLoaded?.invoke()
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    isInterstitialLoaded = false
                    Log.w(TAG, "Unity Interstitial Failed to load: $error, message: $message")
                    onError?.invoke(message ?: "Failed to load interstitial ad")
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading interstitial ad", e)
            onError?.invoke(e.message ?: "Exception")
        }
    }

    fun showInterstitial(
        activity: Activity,
        force: Boolean = false,
        onAdDismissed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastInterstitialShowTime < INTERSTITIAL_MIN_INTERVAL_MS)) {
            Log.d(TAG, "Interstitial throttled by smart frequency cap")
            onAdDismissed()
            return
        }

        if (!UnityAds.isInitialized) {
            initialize(activity)
            onAdFailed("Unity Ads not initialized yet")
            return
        }

        try {
            val showOptions = UnityAdsShowOptions()
            UnityAds.show(activity, INTERSTITIAL_UNIT_ID, showOptions, object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    Log.w(TAG, "Unity Interstitial Show Failure: $error, msg: $message")
                    isInterstitialLoaded = false
                    loadInterstitial(activity)
                    onAdFailed(message ?: "Ad show failure")
                }

                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.i(TAG, "Unity Interstitial Show Started: $placementId")
                    lastInterstitialShowTime = System.currentTimeMillis()
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.i(TAG, "Unity Interstitial Clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    Log.i(TAG, "Unity Interstitial Complete: $placementId, state: $state")
                    isInterstitialLoaded = false
                    loadInterstitial(activity)
                    onAdDismissed()
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Exception showing interstitial ad", e)
            onAdFailed(e.message ?: "Exception")
        }
    }

    fun loadRewardedVideo(context: Context, onLoaded: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (!UnityAds.isInitialized) {
            initialize(context)
        }
        try {
            UnityAds.load(REWARDED_UNIT_ID, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    isRewardedLoaded = true
                    Log.i(TAG, "Unity Rewarded Video Loaded: $placementId")
                    onLoaded?.invoke()
                }

                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    isRewardedLoaded = false
                    Log.w(TAG, "Unity Rewarded Video Failed to load: $error, message: $message")
                    onError?.invoke(message ?: "Failed to load rewarded video")
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading rewarded video", e)
            onError?.invoke(e.message ?: "Exception")
        }
    }

    fun showRewardedVideo(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        if (!UnityAds.isInitialized) {
            initialize(activity)
            onAdFailed("Unity Ads not initialized")
            return
        }

        try {
            val showOptions = UnityAdsShowOptions()
            UnityAds.show(activity, REWARDED_UNIT_ID, showOptions, object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    Log.w(TAG, "Unity Rewarded Video Show Failure: $error, msg: $message")
                    isRewardedLoaded = false
                    loadRewardedVideo(activity)
                    onAdFailed(message ?: "Failed to show rewarded ad")
                }

                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.i(TAG, "Unity Rewarded Video Show Started: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.i(TAG, "Unity Rewarded Video Clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    Log.i(TAG, "Unity Rewarded Video Complete: $placementId, state: $state")
                    isRewardedLoaded = false
                    loadRewardedVideo(activity)
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onRewardEarned()
                    }
                    onAdDismissed()
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Exception showing rewarded video", e)
            onAdFailed(e.message ?: "Exception")
        }
    }
}
