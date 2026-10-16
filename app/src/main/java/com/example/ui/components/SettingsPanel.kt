package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun SettingsPanel(viewModel: MainViewModel) {
    val autoLandscape by viewModel.autoLandscape.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val accessCode by viewModel.accessCode.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الإعدادات والتحكم",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Account Info Card
        SettingsCard(title = "معلومات الحساب والاشتراك", icon = Icons.Default.Person) {
            Column(modifier = Modifier.padding(14.dp)) {
                val isXtream = accessCode == "DIRECT_XTREAM" || (activeAccount?.host?.isNotEmpty() == true && activeAccount?.username?.isNotEmpty() == true)

                if (isXtream) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Text("متصل بسيرفر Xtream Codes API", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!activeAccount?.host.isNullOrEmpty()) {
                        Text(text = "عنوان السيرفر:", color = SoftGreyText, fontSize = 10.5.sp)
                        Text(text = activeAccount?.host ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (!activeAccount?.username.isNullOrEmpty()) {
                        Text(text = "اسم المستخدم:", color = SoftGreyText, fontSize = 10.5.sp)
                        Text(text = activeAccount?.username ?: "", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(text = "صلاحية الحساب:", color = SoftGreyText, fontSize = 10.5.sp)
                    Text(text = activeAccount?.expiryDate ?: "نشط ومفعل", color = Color(0xFF10B981), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LoopRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = LoopRed, modifier = Modifier.size(14.dp))
                        Text("متصل عبر كود تفعيل VIP", color = LoopRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "كود التفعيل النشط:", color = SoftGreyText, fontSize = 10.5.sp)
                    Text(text = accessCode ?: activeAccount?.id ?: "غير معروف", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "حالة الاشتراك:", color = SoftGreyText, fontSize = 10.5.sp)
                    Text(text = activeAccount?.name ?: "اشتراك مفعل", color = GoldGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الخروج والتبديل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // App Settings
        SettingsCard(title = "تفضيلات التطبيق", icon = Icons.Default.Settings) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "وضع العرض الأفقي التلقائي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "تدوير الشاشة تلقائياً عند تشغيل الفيديو", color = SoftGreyText, fontSize = 10.sp)
                    }
                    Switch(
                        checked = autoLandscape,
                        onCheckedChange = { viewModel.setAutoLandscape(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = LoopRed, checkedTrackColor = LoopRed.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Support
        SettingsCard(title = "الدعم الفني والاشتراكات", icon = Icons.Default.HeadsetMic) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "تواجه مشكلة أو ترغب في تجديد أو إضافة اشتراك جديد؟ نحن جاهزون للمساعدة فوراً", color = SoftGreyText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تليجرام حالياً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تواصل مع الدعم المباشر عبر تليجرام", color = GoldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "إصدار المنصة 4.0.0 (+90 Build Pro)",
            color = SoftGreyText.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
    }
}

@Composable
fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
