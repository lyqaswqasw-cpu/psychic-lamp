package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LoopLiveApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .build()
                chain.proceed(request)
            }

        // Trust all SSL certificates to bypass secure connection issues with self-signed logo hosts
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = try {
                SSLContext.getInstance("TLS")
            } catch (e: Exception) {
                SSLContext.getInstance("SSL")
            }
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Throwable) {
            Log.e("LoopLiveApp", "Failed to setup trust-all SSL for Coil: ${e.message}", e)
        }

        val okHttpClient = try {
            builder.build()
        } catch (e: Throwable) {
            OkHttpClient()
        }

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to prevent unexpected termination from non-critical background jobs
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LoopLiveApp", "Global crash intercepted on thread ${thread.name}: ${throwable.message}", throwable)
            if (thread.name.contains("main", ignoreCase = true)) {
                try {
                    defaultHandler?.uncaughtException(thread, throwable)
                } catch (t: Throwable) {
                    Log.e("LoopLiveApp", "Secondary error in default crash handler: ${t.message}", t)
                }
            }
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:725987052105:android:0b0f98bbccb6c874bdcd98")
                    .setProjectId("loop-7e3d9")
                    .setApiKey("AIzaSyBmUJGFkIPMyN2tOVk3LFe4u7ZLky6CKkQ")
                    .setStorageBucket("loop-7e3d9.firebasestorage.app")
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.d("LoopLiveApp", "Firebase initialized successfully manually!")
            }
        } catch (e: Throwable) {
            Log.e("LoopLiveApp", "Failed to initialize Firebase", e)
        }

        // Initialize Notification Channels safely
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                
                val chatChannel = NotificationChannel(
                    "private_chats",
                    "الرسائل الخاصة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات عند تلقي رسائل جديدة في الدردشة الخاصة"
                }
                notificationManager?.createNotificationChannel(chatChannel)

                val mainNotifChannel = NotificationChannel(
                    "plus90_notifications_channel",
                    "إشعارات تطبيق +90",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة إشعارات البث المباشر والأخبار والمباريات"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager?.createNotificationChannel(mainNotifChannel)

                val downloadChannel = NotificationChannel(
                    "plus90_downloads_channel",
                    "تنزيلات الأفلام والمسلسلات",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "قناة متابعة تقدم وتنزيل الأفلام وحلقات المسلسلات"
                }
                notificationManager?.createNotificationChannel(downloadChannel)

                Log.d("LoopLiveApp", "Notification Channels initialized successfully!")
            }
        } catch (e: Throwable) {
            Log.e("LoopLiveApp", "Failed to create Notification Channels: ${e.message}", e)
        }
    }
}
