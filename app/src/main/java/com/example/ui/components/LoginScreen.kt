package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.ui.utils.tvFocusable
import org.json.JSONArray
import org.json.JSONObject

// ==========================================
// 1. Data Models & Storage Helpers for Login
// ==========================================

data class SavedXtreamProfile(
    val id: String,
    val label: String,
    val host: String,
    val username: String,
    val password: String,
    val format: String
)

data class XtreamParsedData(
    val host: String,
    val username: String,
    val password: String,
    val format: String
)

object LoginPreferencesHelper {
    private const val PREFS_NAME = "loop_live_login_vault"
    private const val KEY_RECENT_CODES = "recent_activation_codes"
    private const val KEY_SAVED_PROFILES = "saved_xtream_profiles"

    fun getRecentCodes(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_RECENT_CODES, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val code = array.getString(i).trim().uppercase()
                if (code.isNotEmpty() && !list.contains(code)) {
                    list.add(code)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveRecentCode(context: Context, code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = getRecentCodes(context).toMutableList()
            current.remove(trimmed)
            current.add(0, trimmed)
            val limited = current.take(6)
            val array = JSONArray()
            limited.forEach { array.put(it) }
            prefs.edit().putString(KEY_RECENT_CODES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun removeRecentCode(context: Context, code: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = getRecentCodes(context).toMutableList()
            current.remove(code.trim().uppercase())
            val array = JSONArray()
            current.forEach { array.put(it) }
            prefs.edit().putString(KEY_RECENT_CODES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getSavedProfiles(context: Context): List<SavedXtreamProfile> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_SAVED_PROFILES, "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val list = mutableListOf<SavedXtreamProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedXtreamProfile(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        label = obj.optString("label", "سيرفر Xtream"),
                        host = obj.optString("host", ""),
                        username = obj.optString("username", ""),
                        password = obj.optString("password", ""),
                        format = obj.optString("format", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProfile(context: Context, profile: SavedXtreamProfile) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = getSavedProfiles(context)
                .filter { it.host != profile.host || it.username != profile.username }
                .toMutableList()
            current.add(0, profile)
            val limited = current.take(8)
            val array = JSONArray()
            limited.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("label", p.label)
                obj.put("host", p.host)
                obj.put("username", p.username)
                obj.put("password", p.password)
                obj.put("format", p.format)
                array.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_PROFILES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun deleteProfile(context: Context, profileId: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = getSavedProfiles(context).filter { it.id != profileId }
            val array = JSONArray()
            current.forEach { p ->
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("label", p.label)
                obj.put("host", p.host)
                obj.put("username", p.username)
                obj.put("password", p.password)
                obj.put("format", p.format)
                array.put(obj)
            }
            prefs.edit().putString(KEY_SAVED_PROFILES, array.toString()).apply()
        } catch (_: Exception) {}
    }
}

/**
 * Intelligent Smart Parser for M3U and Xtream URLs.
 * Extracts Host, Username, Password and Output Format automatically.
 */
fun parseSmartXtreamUrl(input: String): XtreamParsedData? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val uri = Uri.parse(trimmed)
        val scheme = uri.scheme ?: "http"
        val hostPart = uri.host ?: return null
        val portPart = if (uri.port != -1) ":${uri.port}" else ""
        val baseUrl = "$scheme://$hostPart$portPart"

        // 1. Query parameters (e.g. get.php?username=XXX&password=YYY&output=ts)
        val qUser = uri.getQueryParameter("username")
            ?: uri.getQueryParameter("user")
            ?: uri.getQueryParameter("u")
        val qPass = uri.getQueryParameter("password")
            ?: uri.getQueryParameter("pass")
            ?: uri.getQueryParameter("p")
        val qOutput = uri.getQueryParameter("output") ?: uri.getQueryParameter("type") ?: ""

        if (!qUser.isNullOrEmpty() && !qPass.isNullOrEmpty()) {
            val fmt = when {
                qOutput.contains("m3u8", ignoreCase = true) -> "m3u8"
                qOutput.contains("ts", ignoreCase = true) -> "ts"
                else -> ""
            }
            return XtreamParsedData(baseUrl, qUser, qPass, fmt)
        }

        // 2. Path segments: /live/user/pass/id.ts or /movie/user/pass/id.mp4
        val segments = uri.pathSegments
        if (segments != null && segments.size >= 3) {
            val head = segments[0].lowercase()
            if (head in listOf("live", "movie", "series", "xmltv.php", "player_api.php")) {
                val u = segments[1]
                val p = segments[2]
                val fmt = when {
                    segments.lastOrNull()?.endsWith(".m3u8", ignoreCase = true) == true -> "m3u8"
                    segments.lastOrNull()?.endsWith(".ts", ignoreCase = true) == true -> "ts"
                    else -> ""
                }
                return XtreamParsedData(baseUrl, u, p, fmt)
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

// ==========================================
// 2. Main LoginScreen (التصميم الكلاسيكي الأصلي)
// ==========================================

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showXtreamDirectLogin by viewModel.showXtreamDirectLogin.collectAsState()

    // 0: كود التفعيل, 1: حساب Xtream
    var selectedTab by remember { mutableStateOf(0) }

    // Activation Code State
    var activationCodeInput by remember { mutableStateOf("") }

    // Xtream Fields
    var xtreamHost by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var xtreamPassVisible by remember { mutableStateOf(false) }
    var xtreamFormat by remember { mutableStateOf("") } // "": تلقائي, "m3u8", "ts"
    var saveProfileChecked by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var recentCodes by remember { mutableStateOf(LoginPreferencesHelper.getRecentCodes(context)) }
    var savedProfiles by remember { mutableStateOf(LoginPreferencesHelper.getSavedProfiles(context)) }
    var showSavedProfiles by remember { mutableStateOf(false) }

    // Developer Panel State
    var showDeveloperPanel by remember { mutableStateOf(false) }
    var showDevPasswordDialog by remember { mutableStateOf(false) }
    var devPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableIntStateOf(0) }

    BackHandler(enabled = showDeveloperPanel || showDevPasswordDialog) {
        if (showDeveloperPanel) {
            showDeveloperPanel = false
        } else if (showDevPasswordDialog) {
            showDevPasswordDialog = false
            devPasswordInput = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A14)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LoopRed.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(2.dp, LoopRed.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .clickable {
                        logoTapCount++
                        if (logoTapCount >= 5) {
                            logoTapCount = 0
                            if (viewModel.isDeveloperAuthenticated.value) {
                                showDeveloperPanel = true
                            } else {
                                showDevPasswordDialog = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.plus_90_app_logo_1787273202258),
                    contentDescription = "+90 Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "منصة +90 Live",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = "بث مباشر للمباريات والقنوات التلفزيونية والمكتبة الشاملة",
                color = SoftGreyText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Switcher Tabs: كود التفعيل vs حساب Xtream
            if (showXtreamDirectLogin) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F1424),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Tab 0: كود التفعيل
                        val isCode = selectedTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCode) LoopRed else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (isCode) Color.White else SoftGreyText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "كود التفعيل",
                                    color = if (isCode) Color.White else SoftGreyText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Tab 1: حساب Xtream
                        val isXtream = selectedTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isXtream) LoopRed else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (isXtream) Color.White else SoftGreyText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "حساب Xtream",
                                    color = if (isXtream) Color.White else SoftGreyText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main Card Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1220)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedTab == 0 || !showXtreamDirectLogin) {
                        // ==========================================
                        // TAB 1: ACTIVATION CODE (التصميم القديم الكلاسيكي)
                        // ==========================================
                        Text(
                            text = "تسجيل الدخول عبر كود التفعيل",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "أدخل رمز الاشتراك الممنوح لك لبدء المشاهدة فوراً",
                            color = SoftGreyText,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )

                        // Classic Single OutlinedTextField for Activation Code
                        OutlinedTextField(
                            value = activationCodeInput,
                            onValueChange = { activationCodeInput = it },
                            placeholder = {
                                Text(
                                    text = "أدخل كود التفعيل هنا...",
                                    color = SoftGreyText.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = LoopRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (activationCodeInput.isNotEmpty()) {
                                        IconButton(onClick = { activationCodeInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = SoftGreyText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text?.trim()
                                            if (!clip.isNullOrEmpty()) {
                                                activationCodeInput = clip
                                                Toast.makeText(context, "تم لصق الكود!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "لصق من الحافظة",
                                            tint = GoldGlow,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (activationCodeInput.trim().isNotEmpty()) {
                                        LoginPreferencesHelper.saveRecentCode(context, activationCodeInput.trim())
                                        recentCodes = LoginPreferencesHelper.getRecentCodes(context)
                                        viewModel.loginWithCode(activationCodeInput.trim())
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF070A14),
                                unfocusedContainerColor = Color(0xFF070A14),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_code_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Error Banner if exists
                        errorMessage?.let { err ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF330D11),
                                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                                        Text(text = err, color = Color.White, fontSize = 11.5.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearErrorMessage() },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (activationCodeInput.trim().isNotEmpty()) {
                                    LoginPreferencesHelper.saveRecentCode(context, activationCodeInput.trim())
                                    recentCodes = LoginPreferencesHelper.getRecentCodes(context)
                                    viewModel.loginWithCode(activationCodeInput.trim())
                                } else {
                                    Toast.makeText(context, "الرجاء إدخال كود التفعيل!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_btn")
                                .tvFocusable(shape = RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "جاري التحقق...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "تسجيل الدخول", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Recent Codes Chips
                        if (recentCodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الأكواد السابقة:",
                                        color = SoftGreyText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "مسح السجل",
                                        color = LoopRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            recentCodes.forEach { LoginPreferencesHelper.removeRecentCode(context, it) }
                                            recentCodes = emptyList()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    recentCodes.forEach { code ->
                                        Surface(
                                            onClick = { activationCodeInput = code },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (activationCodeInput == code) LoopRed.copy(alpha = 0.25f) else Color(0xFF070A14),
                                            border = BorderStroke(
                                                1.dp,
                                                if (activationCodeInput == code) LoopRed else Color.White.copy(alpha = 0.12f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = LoopRed, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = code,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // TAB 2: XTREME ACCOUNT LOGIN (كتابة حسابات Xtreme الكلاسيكية)
                        // ==========================================
                        Text(
                            text = "تسجيل الدخول بسيرفر Xtream",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "أدخل رابط السيرفر واسم المستخدم وكلمة المرور",
                            color = SoftGreyText,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        // Quick Paste Smart Link Button
                        Surface(
                            onClick = {
                                val clip = clipboardManager.getText()?.text?.trim()
                                if (!clip.isNullOrEmpty()) {
                                    val parsed = parseSmartXtreamUrl(clip)
                                    if (parsed != null) {
                                        xtreamHost = parsed.host
                                        xtreamUser = parsed.username
                                        xtreamPass = parsed.password
                                        if (parsed.format.isNotEmpty()) xtreamFormat = parsed.format
                                        Toast.makeText(context, "تم استخراج وتعبئة البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // If plain link, paste to host
                                        xtreamHost = clip
                                        Toast.makeText(context, "تم لصق الرابط في حقل السيرفر!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "الحافظة فارغة!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131A2E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "لصق رابط كامل أو M3U وتعبئة الحقول تلقائياً",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Host URL
                        OutlinedTextField(
                            value = xtreamHost,
                            onValueChange = { xtreamHost = it },
                            label = { Text("رابط السيرفر (Host URL)") },
                            placeholder = { Text("http://example.com:8080", color = SoftGreyText.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(Icons.Default.Dns, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (xtreamHost.isNotEmpty()) {
                                    IconButton(onClick = { xtreamHost = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = SoftGreyText, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF070A14),
                                unfocusedContainerColor = Color(0xFF070A14),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = LoopRed,
                                unfocusedLabelColor = SoftGreyText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Username
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("اسم المستخدم (Username)") },
                            placeholder = { Text("Username", color = SoftGreyText.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF070A14),
                                unfocusedContainerColor = Color(0xFF070A14),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = LoopRed,
                                unfocusedLabelColor = SoftGreyText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Password
                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("كلمة المرور (Password)") },
                            placeholder = { Text("Password", color = SoftGreyText.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { xtreamPassVisible = !xtreamPassVisible }) {
                                    Icon(
                                        imageVector = if (xtreamPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SoftGreyText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (xtreamPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (xtreamHost.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()) {
                                        viewModel.loginWithXtreamDirect(xtreamHost.trim(), xtreamUser.trim(), xtreamPass.trim(), xtreamFormat)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedContainerColor = Color(0xFF070A14),
                                unfocusedContainerColor = Color(0xFF070A14),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = LoopRed,
                                unfocusedLabelColor = SoftGreyText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Format Chips (تلقائي / m3u8 / ts)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "الصيغة:", color = SoftGreyText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            listOf(
                                "" to "تلقائي",
                                "m3u8" to "m3u8 (HLS)",
                                "ts" to "ts"
                            ).forEach { (fKey, fLabel) ->
                                val isSelected = xtreamFormat == fKey
                                Surface(
                                    onClick = { xtreamFormat = fKey },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LoopRed.copy(alpha = 0.2f) else Color(0xFF070A14),
                                    border = BorderStroke(1.dp, if (isSelected) LoopRed else Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = fLabel,
                                        color = if (isSelected) Color.White else SoftGreyText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Save Server Checkbox & View Saved
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = saveProfileChecked,
                                    onCheckedChange = { saveProfileChecked = it },
                                    colors = CheckboxDefaults.colors(checkedColor = LoopRed, uncheckedColor = SoftGreyText)
                                )
                                Text("تذكر هذا السيرفر", color = Color.White, fontSize = 11.sp)
                            }

                            if (savedProfiles.isNotEmpty()) {
                                Text(
                                    text = if (showSavedProfiles) "إخفاء المحفوظة" else "السيرفرات المحفوظة (${savedProfiles.size})",
                                    color = GoldGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showSavedProfiles = !showSavedProfiles }
                                )
                            }
                        }

                        // Saved Profiles List
                        AnimatedVisibility(visible = showSavedProfiles && savedProfiles.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                savedProfiles.forEach { p ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF070A14),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = p.host, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(text = "المستخدم: ${p.username}", color = SoftGreyText, fontSize = 9.5.sp)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Button(
                                                    onClick = {
                                                        xtreamHost = p.host
                                                        xtreamUser = p.username
                                                        xtreamPass = p.password
                                                        xtreamFormat = p.format
                                                        showSavedProfiles = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("استخدام", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        LoginPreferencesHelper.deleteProfile(context, p.id)
                                                        savedProfiles = LoginPreferencesHelper.getSavedProfiles(context)
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Error Banner if exists
                        errorMessage?.let { err ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF330D11),
                                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                                        Text(text = err, color = Color.White, fontSize = 11.5.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearErrorMessage() },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Connect Xtream Button
                        Button(
                            onClick = {
                                if (xtreamHost.trim().isEmpty() || xtreamUser.trim().isEmpty() || xtreamPass.trim().isEmpty()) {
                                    Toast.makeText(context, "يرجى تعبئة كافة بيانات السيرفر!", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (saveProfileChecked) {
                                        LoginPreferencesHelper.saveProfile(
                                            context,
                                            SavedXtreamProfile(
                                                id = System.currentTimeMillis().toString(),
                                                label = "سيرفر Xtream",
                                                host = xtreamHost.trim(),
                                                username = xtreamUser.trim(),
                                                password = xtreamPass.trim(),
                                                format = xtreamFormat
                                            )
                                        )
                                        savedProfiles = LoginPreferencesHelper.getSavedProfiles(context)
                                    }
                                    viewModel.loginWithXtreamDirect(xtreamHost.trim(), xtreamUser.trim(), xtreamPass.trim(), xtreamFormat)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .tvFocusable(shape = RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "جاري الاتصال بالسيرفر...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "تسجيل الدخول", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Telegram Support Link
            Surface(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "لا يمكن فتح تليجرام حالياً", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "Telegram Support",
                        tint = GoldGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "طلب كود تفعيل أو دعم فني عبر تليجرام",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Developer Panel Entry Button
            Surface(
                onClick = {
                    if (viewModel.isDeveloperAuthenticated.value) {
                        showDeveloperPanel = true
                    } else {
                        showDevPasswordDialog = true
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131728),
                border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "لوحة تحكم المطور",
                        tint = GoldGlow,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "لوحة تحكم المطور وإدارة السيرفر ⚙️",
                        color = GoldGlow,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Developer Password Verification Dialog
        if (showDevPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDevPasswordDialog = false
                    devPasswordInput = ""
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تحقق من صلاحية المطور",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "أدخل كلمة مرور المطور لفتح لوحة التحكم وإدارة الأكواد والسيرفرات والمحتوى:",
                            color = Color(0xFFDCDCE8),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        OutlinedTextField(
                            value = devPasswordInput,
                            onValueChange = { devPasswordInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("أدخل كلمة مرور المطور", color = Color.Gray, fontSize = 12.sp) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                        tint = Color.LightGray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val isValid = viewModel.authenticateDeveloper(devPasswordInput)
                            if (isValid) {
                                showDevPasswordDialog = false
                                devPasswordInput = ""
                                showDeveloperPanel = true
                                Toast.makeText(context, "تم التحقق بنجاح! مرحباً بك في لوحة المطور 👑", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "كلمة المرور غير صحيحة ❌", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                    ) {
                        Text("دخول للوحة 🔓", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDevPasswordDialog = false
                        devPasswordInput = ""
                    }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                },
                modifier = Modifier.border(1.dp, GoldGlow.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                containerColor = Color(0xFF191924),
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Full Developer Settings Screen Overlay
        if (showDeveloperPanel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070A14))
            ) {
                DeveloperSettingsScreen(
                    viewModel = viewModel,
                    onDismiss = {
                        showDeveloperPanel = false
                    }
                )
            }
        }
    }
}
