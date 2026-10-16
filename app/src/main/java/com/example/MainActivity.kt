package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.MainApp
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.VpnSecurityManager
import com.example.ads.UnityAdsManager

class MainActivity : ComponentActivity() {
  private var viewModelInstance: MainViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Unity Ads with official App Key and preload units
    UnityAdsManager.initialize(this)
    
    enableEdgeToEdge()
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Start proactive VPN security monitoring immediately
    VpnSecurityManager.startMonitoring(this)

    setContent {
      val mainViewModel: MainViewModel = viewModel()
      viewModelInstance = mainViewModel
      val selectedTheme by mainViewModel.selectedTheme.collectAsState()
      val isDarkMode by mainViewModel.isDarkMode.collectAsState()
      
      // Handle Deep Link
      LaunchedEffect(intent) {
          handleIntent(intent, mainViewModel)
      }

      MyApplicationTheme(themeName = selectedTheme, isDark = isDarkMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainApp(viewModel = mainViewModel)
        }
      }
    }
  }

  override fun onResume() {
      super.onResume()
      VpnSecurityManager.forceCheck(this)
      viewModelInstance?.checkAppVersionStatus()
  }

  override fun onDestroy() {
      super.onDestroy()
      VpnSecurityManager.stopMonitoring(this)
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      // Note: In a real app, you'd pass this to a State or directly to ViewModel
  }

  private fun handleIntent(intent: Intent?, viewModel: MainViewModel) {
      intent?.data?.let { uri ->
          val isAppLink = (uri.host == "plus90.app" || uri.host == "plus-90.app" || uri.host == "25-live.app") && uri.path?.startsWith("/play") == true
          val isCustomScheme = (uri.scheme == "plus90" || uri.scheme == "looplive" || uri.scheme == "loolive") && uri.host == "play"
          
          if (isAppLink || isCustomScheme) {
              val shareId = uri.getQueryParameter("id")
              if (shareId != null) {
                  viewModel.handleSharedLink(shareId) { url, title ->
                      viewModel.startPlaying(url, title, isLive = true)
                  }
              }
          }
      }
  }
}
