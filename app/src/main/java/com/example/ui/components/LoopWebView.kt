package com.example.ui.components

import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun LoopWebView(tabName: String = "") {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(false)
            WebView(context).apply {
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }
                }
                webViewClient = object : WebViewClient() {
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
                        return true // Quiet console messages
                    }
                }
                val url = if (tabName.isNotEmpty()) {
                    "file:///android_asset/loop.html#$tabName"
                } else {
                    "file:///android_asset/loop.html"
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            val targetUrl = if (tabName.isNotEmpty()) {
                "file:///android_asset/loop.html#$tabName"
            } else {
                "file:///android_asset/loop.html"
            }
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
            webView.evaluateJavascript("window.handleHashChange && window.handleHashChange();", null)
        }
    )
}
