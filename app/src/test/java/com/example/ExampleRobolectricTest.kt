package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AppVersionConfig
import com.example.utils.AppVersionManager
import com.example.utils.AppVersionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("+90", appName)
  }

  @Test
  fun `version manager compares semantic versions correctly`() {
    assertTrue(AppVersionManager.compareVersions("2.0.0", "1.9.9") > 0)
    assertTrue(AppVersionManager.compareVersions("1.0.0", "2.0.0") < 0)
    assertEquals(0, AppVersionManager.compareVersions("2.0.0", "2.0.0"))
  }

  @Test
  fun `version manager blocks legacy versions`() {
    val config = AppVersionConfig(
      currentVersion = "2.0.0",
      minimumVersion = "2.0.0",
      forceUpdate = true,
      appEnabled = true
    )
    val legacyStatus = AppVersionManager.evaluate(config, "1.0.0")
    assertTrue(legacyStatus is AppVersionStatus.UpdateRequired)

    val validStatus = AppVersionManager.evaluate(config, "2.0.0")
    assertTrue(validStatus is AppVersionStatus.Allowed)
  }

  @Test
  fun `version manager triggers maintenance when app is disabled`() {
    val config = AppVersionConfig(
      appEnabled = false,
      maintenanceTitle = "صيانة الخوادم"
    )
    val status = AppVersionManager.evaluate(config, "2.0.0")
    assertTrue(status is AppVersionStatus.Maintenance)
  }
}
