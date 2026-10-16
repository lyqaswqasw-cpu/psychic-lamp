package com.example.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel

class ChatJsBridge(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val onBackAction: () -> Unit
) {
    @JavascriptInterface
    fun goBack() {
        Handler(Looper.getMainLooper()).post {
            onBackAction()
        }
    }

    @JavascriptInterface
    fun getCredentials(): String {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("chat_username", "") ?: ""
        val password = sharedPrefs.getString("chat_password", "") ?: ""
        val json = org.json.JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        return json.toString()
    }

    @JavascriptInterface
    fun updateUnreadCount(count: Int) {
        Handler(Looper.getMainLooper()).post {
            viewModel.updateChatUnreadUsersCount(count)
        }
    }
}

@Composable
fun LoopChatWebView(
    viewModel: MainViewModel,
    onBackToChannels: () -> Unit = {}
) {
    val context = LocalContext.current
    var webViewRef: WebView? = remember { null }

    BackHandler {
        onBackToChannels()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView.setWebContentsDebuggingEnabled(false)
            WebView(ctx).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                addJavascriptInterface(ChatJsBridge(context, viewModel, onBackToChannels), "AndroidBridge")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        view?.let {
                            (it.parent as? ViewGroup)?.removeView(it)
                            it.destroy()
                        }
                        return true
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        return true
                    }
                }
                loadUrl("file:///android_asset/chat.html")
                webViewRef = this
            }
        },
        update = { webView ->
            if (webView.url != "file:///android_asset/chat.html") {
                webView.loadUrl("file:///android_asset/chat.html")
            }
        }
    )
}
