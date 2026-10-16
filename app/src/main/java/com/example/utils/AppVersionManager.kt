package com.example.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Data model for remote app version configuration retrieved from Firebase.
 */
data class AppVersionConfig(
    val currentVersion: String = "2.0.0",
    val minimumVersion: String = "2.0.0",
    val forceUpdate: Boolean = true,
    val appEnabled: Boolean = true,
    val updateUrl: String = "https://t.me/plus90_official",
    val updateTitle: String = "تحديث جديد مطلوب 🚀",
    val updateMessage: String = "هذه النسخة من التطبيق لم تعد مدعومة.\nيرجى تحديث التطبيق إلى أحدث إصدار للاستمرار في المشاهدة ومتابعة البث.",
    val maintenanceTitle: String = "التطبيق متوقف مؤقتًا للصيانة 🛠️",
    val maintenanceMessage: String = "نعمل حالياً على صيانة وتطوير خوادم البث لضمان أعلى جودة وسرعة.\nسنعود للعمل قريباً، شكراً لصبركم.",
    val telegramSupportUrl: String = "https://t.me/plus90_official",
    val serverTimestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the evaluation result of the app's version and operational status.
 */
sealed class AppVersionStatus {
    object Loading : AppVersionStatus()
    data class Allowed(val config: AppVersionConfig) : AppVersionStatus()
    data class Maintenance(val config: AppVersionConfig) : AppVersionStatus()
    data class UpdateRequired(val config: AppVersionConfig, val currentAppVersion: String) : AppVersionStatus()
    data class UpdateRecommended(val config: AppVersionConfig, val currentAppVersion: String) : AppVersionStatus()
}

/**
 * Professional App Version Management & Semantic Version Comparison utility.
 */
object AppVersionManager {
    private const val TAG = "AppVersionManager"

    // Hardcoded authoritative version of the current release in code
    const val CURRENT_APP_VERSION = "2.0.0"

    // Developer Test Mode - Simulated Version for testing old versions / updates / maintenance
    private val _simulatedVersion = MutableStateFlow<String?>(null)
    val simulatedVersion: StateFlow<String?> = _simulatedVersion.asStateFlow()

    fun getEffectiveAppVersion(): String {
        return _simulatedVersion.value ?: CURRENT_APP_VERSION
    }

    fun setSimulatedVersion(version: String?) {
        _simulatedVersion.value = version
        evaluate(_latestConfig.value, getEffectiveAppVersion())
    }

    private val _versionStatus = MutableStateFlow<AppVersionStatus>(AppVersionStatus.Loading)
    val versionStatus: StateFlow<AppVersionStatus> = _versionStatus.asStateFlow()

    private val _latestConfig = MutableStateFlow(AppVersionConfig())
    val latestConfig: StateFlow<AppVersionConfig> = _latestConfig.asStateFlow()

    /**
     * Robust Semantic Version Comparison.
     * Compares version strings like "2.0.0", "2.1.0", "10.0.0", "v2.0.0.1".
     *
     * Returns:
     *   < 0 if v1 < v2
     *   0   if v1 == v2
     *   > 0 if v1 > v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = cleanAndSplitVersion(v1)
        val parts2 = cleanAndSplitVersion(v2)

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0L }
            val num2 = parts2.getOrElse(i) { 0L }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    private fun cleanAndSplitVersion(version: String): List<Long> {
        val clean = version.trim().lowercase()
            .removePrefix("v")
            .split("-")[0] // Strip build tags like -beta, -rc1
            .split("+")[0]

        return clean.split(".")
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty { listOf(0L) }
    }

    /**
     * True if installedVersion is strictly less than minRequiredVersion.
     */
    fun isVersionOutdated(installedVersion: String, minRequiredVersion: String): Boolean {
        return compareVersions(installedVersion, minRequiredVersion) < 0
    }

    /**
     * True if installedVersion is strictly less than latestVersion.
     */
    fun isNewerVersionAvailable(installedVersion: String, latestVersion: String): Boolean {
        return compareVersions(installedVersion, latestVersion) < 0
    }

    /**
     * Evaluates the application status against the given remote configuration.
     */
    fun evaluate(config: AppVersionConfig, installedVersion: String = getEffectiveAppVersion()): AppVersionStatus {
        _latestConfig.value = config

        val status = when {
            !config.appEnabled -> {
                AppVersionStatus.Maintenance(config)
            }
            isVersionOutdated(installedVersion, config.minimumVersion) -> {
                AppVersionStatus.UpdateRequired(config, installedVersion)
            }
            config.forceUpdate && isNewerVersionAvailable(installedVersion, config.currentVersion) -> {
                AppVersionStatus.UpdateRequired(config, installedVersion)
            }
            isNewerVersionAvailable(installedVersion, config.currentVersion) -> {
                AppVersionStatus.UpdateRecommended(config, installedVersion)
            }
            else -> {
                AppVersionStatus.Allowed(config)
            }
        }

        _versionStatus.value = status
        return status
    }

    /**
     * Parses a JSON object from Firebase into [AppVersionConfig].
     */
    fun parseConfigFromJson(json: JSONObject): AppVersionConfig {
        val currentVersion = json.optString("currentVersion", "2.0.0").ifBlank { "2.0.0" }
        val minimumVersion = json.optString("minimumVersion", "2.0.0").ifBlank { "2.0.0" }
        val forceUpdate = json.optBoolean("forceUpdate", true)
        val appEnabled = json.optBoolean("appEnabled", true)
        val updateUrl = json.optString("updateUrl", "https://t.me/plus90_official").ifBlank { "https://t.me/plus90_official" }
        val updateTitle = json.optString("updateTitle", "تحديث جديد مطلوب 🚀").ifBlank { "تحديث جديد مطلوب 🚀" }
        val updateMessage = json.optString("updateMessage", "هذه النسخة من التطبيق لم تعد مدعومة.\nيرجى تحديث التطبيق إلى أحدث إصدار للاستمرار في المشاهدة ومتابعة البث.").ifBlank {
            "هذه النسخة من التطبيق لم تعد مدعومة.\nيرجى تحديث التطبيق إلى أحدث إصدار للاستمرار في المشاهدة ومتابعة البث."
        }
        val maintenanceTitle = json.optString("maintenanceTitle", "التطبيق متوقف مؤقتًا للصيانة 🛠️").ifBlank { "التطبيق متوقف مؤقتًا للصيانة 🛠️" }
        val maintenanceMessage = json.optString("maintenanceMessage", "نعمل حالياً على صيانة وتطوير خوادم البث لضمان أعلى جودة وسرعة.\nسنعود للعمل قريباً، شكراً لصبركم.").ifBlank {
            "نعمل حالياً على صيانة وتطوير خوادم البث لضمان أعلى جودة وسرعة.\nسنعود للعمل قريباً، شكراً لصبركم."
        }
        val telegramSupportUrl = json.optString("telegramSupportUrl", "https://t.me/plus90_official").ifBlank { "https://t.me/plus90_official" }
        val serverTimestamp = json.optLong("serverTimestamp", System.currentTimeMillis())

        return AppVersionConfig(
            currentVersion = currentVersion,
            minimumVersion = minimumVersion,
            forceUpdate = forceUpdate,
            appEnabled = appEnabled,
            updateUrl = updateUrl,
            updateTitle = updateTitle,
            updateMessage = updateMessage,
            maintenanceTitle = maintenanceTitle,
            maintenanceMessage = maintenanceMessage,
            telegramSupportUrl = telegramSupportUrl,
            serverTimestamp = serverTimestamp
        )
    }

    /**
     * Converts [AppVersionConfig] to a Map for saving to Firebase.
     */
    fun configToMap(config: AppVersionConfig): Map<String, Any> {
        return mapOf(
            "currentVersion" to config.currentVersion,
            "minimumVersion" to config.minimumVersion,
            "forceUpdate" to config.forceUpdate,
            "appEnabled" to config.appEnabled,
            "updateUrl" to config.updateUrl,
            "updateTitle" to config.updateTitle,
            "updateMessage" to config.updateMessage,
            "maintenanceTitle" to config.maintenanceTitle,
            "maintenanceMessage" to config.maintenanceMessage,
            "telegramSupportUrl" to config.telegramSupportUrl,
            "serverTimestamp" to System.currentTimeMillis()
        )
    }
}
