package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.*
import com.example.utils.VpnSecurityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VpnBlockedScreen(
    modifier: Modifier = Modifier,
    onVpnResolved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }

    // Intercept back button: when pressed on VPN block screen, exit the app
    BackHandler(enabled = true) {
        activity?.finishAffinity()
    }

    // Glowing animation for the warning shield
    val infiniteTransition = rememberInfiniteTransition(label = "vpn_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(99999f)
            .background(Color(0xFF0A0A0E))
            .testTag("vpn_blocked_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Background Decorative Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x33E50914),
                            Color(0x10000000),
                            Color(0xFF0A0A0E)
                        )
                    )
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(vertical = 24.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141A)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE50914).copy(alpha = glowAlpha))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pulsating Shield / VPN Slashed Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .background(Color(0x22E50914), CircleShape)
                        .border(2.dp, Color(0xFFE50914).copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE50914), Color(0xFF8B0000))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = "VPN Blocked",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Alert Badge
                Surface(
                    color = Color(0x33E50914),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE50914), CircleShape)
                        )
                        Text(
                            text = "تم اكتشاف اتصال VPN نشط",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title
                Text(
                    text = "استخدام VPN غير مسموح به",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                // Explanation
                Text(
                    text = "عذراً، يمنع تشغيل التطبيق أو مشاهدة القنوات والبث المباشر أثناء تفعيل برامج الـ VPN أو البروكسي، وذلك لحماية حقوق البث والأمان وجودة الخدمة.",
                    color = Color(0xFFB0B0B8),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                // Instructions Box
                Surface(
                    color = Color(0xFF1C1C24),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(18.dp))
                            Text("كيفية حل المشكلة:", color = PlayerAccentGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "1. قم بفتح تطبيق الـ VPN المثبت على جهازك (مثل 1.1.1.1، Turbo VPN، SuperVPN وغيرها).\n2. قم بإيقاف تشغيل أو قطع الاتصال (Disconnect).\n3. اضغط على زر 'إعادة الفحص' بالأسفل لمتابعة المشاهدة فوراً.",
                            color = Color(0xFFD0D0D8),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Re-check Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isChecking = true
                            delay(600L)
                            val stillConnected = VpnSecurityManager.forceCheck(context)
                            isChecking = false
                            if (!stillConnected) {
                                Toast.makeText(context, "تم إيقاف الـ VPN بنجاح! أهلاً بك.", Toast.LENGTH_SHORT).show()
                                onVpnResolved?.invoke()
                            } else {
                                Toast.makeText(context, "لا يزال الـ VPN قيد التشغيل! يرجى إيقافه أولاً.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerAccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("جاري الفحص السريع...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إعادة الفحص والتحقق 🔄", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }

                // Exit App Button
                OutlinedButton(
                    onClick = {
                        activity?.finishAffinity()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إغلاق التطبيق فوراً", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
