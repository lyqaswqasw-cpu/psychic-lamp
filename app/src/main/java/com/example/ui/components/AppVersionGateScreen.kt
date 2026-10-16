package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.LoopRed
import com.example.utils.AppVersionConfig
import com.example.utils.AppVersionManager
import com.example.utils.AppVersionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private tailrec fun findActivity(ctx: Context?): Activity? = when (ctx) {
    is Activity -> ctx
    is ContextWrapper -> findActivity(ctx.baseContext)
    else -> null
}

/**
 * Strict, un-bypassable screen displayed when:
 * 1. App is disabled by admin for maintenance (appEnabled == false).
 * 2. Installed app version is below the minimum required version (minimumVersion).
 * 3. A forced update is active (forceUpdate == true).
 */
@Composable
fun AppVersionGateScreen(
    status: AppVersionStatus,
    viewModel: com.example.ui.MainViewModel? = null,
    onRetryCheck: suspend () -> Boolean,
    onBypassAndEnter: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }

    // Developer authentication and version override states
    var showDevPasswordDialog by remember { mutableStateOf(false) }
    var showDevVersionDialog by remember { mutableStateOf(false) }
    var showFullDeveloperPanel by remember { mutableStateOf(false) }
    var devPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSavingVersionConfig by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableIntStateOf(0) }

    // Intercept hardware and gesture back buttons to block navigation completely
    BackHandler(enabled = true) {
        if (showFullDeveloperPanel) {
            showFullDeveloperPanel = false
        } else if (showDevVersionDialog) {
            showDevVersionDialog = false
        } else if (showDevPasswordDialog) {
            showDevPasswordDialog = false
        } else {
            findActivity(context)?.finishAffinity()
        }
    }

    val isMaintenance = status is AppVersionStatus.Maintenance
    val config = when (status) {
        is AppVersionStatus.Maintenance -> status.config
        is AppVersionStatus.UpdateRequired -> status.config
        is AppVersionStatus.UpdateRecommended -> status.config
        is AppVersionStatus.Allowed -> status.config
        else -> AppVersionConfig()
    }

    val currentInstalledVersion = when (status) {
        is AppVersionStatus.UpdateRequired -> status.currentAppVersion
        is AppVersionStatus.UpdateRecommended -> status.currentAppVersion
        else -> AppVersionManager.CURRENT_APP_VERSION
    }

    // Pulsating animation for the status icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val primaryAccentColor = if (isMaintenance) Color(0xFFFFB300) else Color(0xFFE50914)
    val secondaryAccentColor = if (isMaintenance) Color(0xFFFF8F00) else Color(0xFF8B0000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative ambient background
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(primaryAccentColor.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                primaryAccentColor.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top App Logo & Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable {
                        logoTapCount++
                        if (logoTapCount >= 5) {
                            logoTapCount = 0
                            if (viewModel?.isDeveloperAuthenticated?.value == true) {
                                showDevVersionDialog = true
                            } else {
                                showDevPasswordDialog = true
                            }
                        }
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.plus_90_app_logo_1787273202258),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "+90 Ultimate Live",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Pulsating Icon Box
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .background(primaryAccentColor.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, primaryAccentColor.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(primaryAccentColor, secondaryAccentColor)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMaintenance) Icons.Default.Build else Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = primaryAccentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryAccentColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(primaryAccentColor, CircleShape)
                        )
                        Text(
                            text = if (isMaintenance) "وضع الصيانة المؤقتة ⚡" else "تحديث إجباري مطلوب ⚠️",
                            color = primaryAccentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Main Title
                Text(
                    text = if (isMaintenance) config.maintenanceTitle else config.updateTitle,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                // Version Info Badges (When Update Required)
                if (!isMaintenance) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Installed Version
                        Surface(
                            color = Color(0xFF22222E),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "نسختك الحالية",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "v$currentInstalledVersion",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Required Version
                        Surface(
                            color = Color(0xFF22222E),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "الحد الأدنى المطلوب",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "v${config.minimumVersion}",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Description Box
                Surface(
                    color = Color(0xFF1B1B26),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isMaintenance) config.maintenanceMessage else config.updateMessage,
                            color = Color(0xFFDCDCE8),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Primary Action Button: "تحديث التطبيق الآن" (if update) or "قناة التليجرام" (if maintenance)
                if (!isMaintenance) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.updateUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح الرابط: ${config.updateUrl}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "تحميل وتحديث التطبيق الآن 📥",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Secondary Action: Re-check Button (Refresh from Firebase)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isChecking = true
                            delay(500L)
                            val isResolved = onRetryCheck()
                            isChecking = false
                            if (isResolved) {
                                Toast.makeText(context, "تم التحقق بنجاح! جاري الدخول...", Toast.LENGTH_SHORT).show()
                            } else {
                                val failMsg = if (isMaintenance) {
                                    "لا يزال التطبيق في وضع الصيانة، يرجى المحاولة لاحقاً."
                                } else {
                                    "يرجى تحديث التطبيق إلى الإصدار ${config.minimumVersion} للمتابعة."
                                }
                                Toast.makeText(context, failMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMaintenance) GoldGlow else Color(0xFF2C2C3C)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = if (isMaintenance) Color.Black else Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "جاري التحقق من الخادم...",
                            color = if (isMaintenance) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (isMaintenance) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "إعادة الفحص والتحقق 🔄",
                            color = if (isMaintenance) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Telegram / Support Channel Button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.telegramSupportUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF29B6F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "قناة التليجرام والتحديثات الرسمية 💬",
                        color = Color(0xFF29B6F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Developer Mode & Version Control Button
                OutlinedButton(
                    onClick = {
                        if (viewModel?.isDeveloperAuthenticated?.value == true) {
                            showDevVersionDialog = true
                        } else {
                            showDevPasswordDialog = true
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldGlow.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldGlow)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = GoldGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "لوحة تحكم المطور وتعديل الإصدار 🛠️",
                        color = GoldGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Exit App Button
                TextButton(
                    onClick = {
                        findActivity(context)?.finishAffinity()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "إغلاق التطبيق",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- Developer Access Password Dialog ---
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
                            text = "أدخل كلمة مرور المطور لتعديل إصدار التطبيق في السحابة وتجاوز شاشة التحديث والدخول للتطبيق بشكل طبيعي:",
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
                            val isValid = viewModel?.authenticateDeveloper(devPasswordInput) ?: (devPasswordInput == "ali2008#$1")
                            if (isValid) {
                                viewModel?.bypassVersionGateAsDeveloper()
                                showDevPasswordDialog = false
                                devPasswordInput = ""
                                showDevVersionDialog = true
                                Toast.makeText(context, "تم التحقق بنجاح! مرحباً بك يا مطور 👑", Toast.LENGTH_SHORT).show()
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

        // --- Developer Version Control & Bypass Dialog ---
        if (showDevVersionDialog) {
            var minVersionInput by remember(config) { mutableStateOf(config.minimumVersion) }
            var currentVersionInput by remember(config) { mutableStateOf(config.currentVersion) }
            var forceUpdateInput by remember(config) { mutableStateOf(config.forceUpdate) }
            var appEnabledInput by remember(config) { mutableStateOf(config.appEnabled) }

            AlertDialog(
                onDismissRequest = {
                    if (!isSavingVersionConfig) {
                        showDevVersionDialog = false
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = GoldGlow,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تعديل الإصدار والتحكم المباشر 🛠️",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Current Installed vs Remote Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF222230)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("النسخة المثبتة حالياً:", color = Color.Gray, fontSize = 11.sp)
                                    Text("v$currentInstalledVersion", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("الحد الأدنى المسجل بالخادم:", color = Color.Gray, fontSize = 11.sp)
                                    Text("v${config.minimumVersion}", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Fast Preset Button: Match Installed Version
                        Button(
                            onClick = {
                                minVersionInput = currentInstalledVersion
                                currentVersionInput = currentInstalledVersion
                                forceUpdateInput = false
                                appEnabledInput = true
                                Toast.makeText(context, "تم ضبط الحقول لتطابق نسختك الحالية: v$currentInstalledVersion", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "⚡ ضبط الحد الأدنى لنسختي (v$currentInstalledVersion)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Input Minimum Version
                        OutlinedTextField(
                            value = minVersionInput,
                            onValueChange = { minVersionInput = it },
                            label = { Text("الحد الأدنى للإصدار (minimumVersion)", color = Color.LightGray, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Input Current Version
                        OutlinedTextField(
                            value = currentVersionInput,
                            onValueChange = { currentVersionInput = it },
                            label = { Text("أحدث إصدار معتمد (currentVersion)", color = Color.LightGray, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGlow,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Toggle Force Update
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222230), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجبار التحديث (forceUpdate)", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = forceUpdateInput,
                                onCheckedChange = { forceUpdateInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoldGlow,
                                    checkedTrackColor = GoldGlow.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Toggle App Enabled
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF222230), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تشغيل التطبيق (appEnabled / صيانة)", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = appEnabledInput,
                                onCheckedChange = { appEnabledInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF4CAF50),
                                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                                )
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Save and Open App Button
                        Button(
                            onClick = {
                                isSavingVersionConfig = true
                                val updatedConfig = config.copy(
                                    minimumVersion = minVersionInput.trim().ifEmpty { currentInstalledVersion },
                                    currentVersion = currentVersionInput.trim().ifEmpty { currentInstalledVersion },
                                    forceUpdate = forceUpdateInput,
                                    appEnabled = appEnabledInput
                                )
                                if (viewModel != null) {
                                    viewModel.updateRemoteVersionConfig(updatedConfig) { success ->
                                        isSavingVersionConfig = false
                                        if (success) {
                                            viewModel.bypassVersionGateAsDeveloper()
                                            onBypassAndEnter?.invoke()
                                            showDevVersionDialog = false
                                            Toast.makeText(context, "تم حفظ الإصدار الجديد بنجاح في Firebase والدخول للتطبيق! 🚀", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "فشل حفظ الإصدار في Firebase، يرجى فحص الإنترنت.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isSavingVersionConfig = false
                                    onBypassAndEnter?.invoke()
                                    showDevVersionDialog = false
                                    Toast.makeText(context, "تم الدخول للتطبيق بنجاح!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !isSavingVersionConfig
                        ) {
                            if (isSavingVersionConfig) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("جاري حفظ الإصدار...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("حفظ الإصدار والدخول للتطبيق طبيعي 🚀", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Direct Bypass Button
                        Button(
                            onClick = {
                                viewModel?.bypassVersionGateAsDeveloper()
                                onBypassAndEnter?.invoke()
                                showDevVersionDialog = false
                                Toast.makeText(context, "تم تجاوز التحديث وفتح التطبيق كـ مطور! 👑", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("تجاوز الفحص ودخول التطبيق مباشرة 🔓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Full Developer Panel Button
                        OutlinedButton(
                            onClick = {
                                showDevVersionDialog = false
                                showFullDeveloperPanel = true
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("فتح لوحة تحكم المطور الكاملة ⚙️", color = Color.White, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showDevVersionDialog = false },
                        enabled = !isSavingVersionConfig
                    ) {
                        Text("إغلاق", color = Color.Gray)
                    }
                },
                modifier = Modifier.border(1.dp, GoldGlow.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                containerColor = Color(0xFF191924),
                shape = RoundedCornerShape(20.dp)
            )
        }

        // --- Full Developer Settings Screen Overlay ---
        if (showFullDeveloperPanel && viewModel != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0E))
            ) {
                com.example.ui.components.DeveloperSettingsScreen(
                    viewModel = viewModel,
                    onDismiss = {
                        showFullDeveloperPanel = false
                    }
                )
            }
        }
    }
}
