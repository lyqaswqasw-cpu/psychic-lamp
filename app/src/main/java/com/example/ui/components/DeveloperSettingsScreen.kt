package com.example.ui.components

import android.widget.Toast


import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.FirebaseService
import com.example.ui.MainViewModel
import com.example.ui.AiApiKey
import com.example.model.ActivationCode
import com.example.model.AppNotification
import com.example.model.VipCode
import com.example.model.EventChannel
import com.example.model.EventServer
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.utils.NotificationHelper
import com.example.utils.AppVersionConfig
import com.example.utils.AppVersionManager
import com.example.utils.AppVersionStatus
import com.example.ui.theme.*
import com.example.R
import androidx.compose.foundation.lazy.items
import com.example.ui.utils.tvFocusable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeveloperSettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    val isAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()
    
    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_settings_banner),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))
            
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = LoopRed, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("منطقة المطورين المحمية", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("يرجى إدخال كلمة المرور للوصول إلى الإعدادات المتقدمة", color = SoftGreyText, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("كلمة المرor الماستر") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoopRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedLabelColor = LoopRed,
                        unfocusedLabelColor = SoftGreyText,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.authenticateDeveloper(password) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("فتح الصلاحيات", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("إلغاء والرجوع", color = SoftGreyText)
                }
            }
        }
    } else {
        var selectedTab by remember { mutableIntStateOf(0) }
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 16.dp
                ) {
                    val tabs = listOf(
                        "الرئيسية" to Icons.Default.Home,
                        "الإشعارات" to Icons.Default.NotificationsActive,
                        "إعدادات الدردشة" to Icons.Default.Settings,
                        "إدارة الدردشة" to Icons.Default.Forum,
                        "الإصدارات" to Icons.Default.SystemUpdate,
                        "أكواد عادية" to Icons.Default.Key,
                        "أكواد VIP" to Icons.Default.VpnKey,
                        "إدارة الأحداث" to Icons.Default.Event
                    )
                    
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.first, fontSize = 13.sp) },
                            icon = { Icon(tab.second, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            selectedContentColor = if (index == 2 || index == 5 || index == 6 || index == 7) GoldGlow else LoopRed,
                            unselectedContentColor = SoftGreyText
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> DevMainSettingsTab(viewModel, onDismiss)
                    1 -> DevNotificationsTab(viewModel)
                    2 -> DevChatSettingsTab(viewModel)
                    3 -> DevGlobalChatTab(viewModel)
                    4 -> DevVersionControlTab(viewModel)
                    5 -> DevNormalCodesTab(viewModel)
                    6 -> DevVipSettingsTab(viewModel)
                    7 -> DevEventChannelsTab(viewModel)
                    else -> DevMainSettingsTab(viewModel, onDismiss)
                }
            }
        }
    }
}

@Composable
fun DevMainSettingsTab(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SettingsSuggest, contentDescription = null, tint = LoopRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("تحكم المطور المتطور", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        item {
            val liveOnline = viewModel.liveOnlineUsersCount.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("المتواجدون الآن (Live)", color = GoldGlow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("العدد الفعلي للمستخدمين النشطين حالياً بالتطبيق", color = SoftGreyText, fontSize = 11.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF191F2D)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(text = liveOnline.value.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("إعدادات الواجهة والتحكم بالعناصر", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val showNotifIcon by viewModel.showNotificationIcon.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أيقونة الجرس في الهيدر", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار اختصار الإشعارات في الشريط العلوي", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showNotifIcon,
                            onCheckedChange = { viewModel.setShowNotificationIcon(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showXtreamLogin by viewModel.showXtreamDirectLogin.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("خيار إدخال حسابات Xtream مباشرة في صفحة الدخول", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("السماح للمستخدمين بالدخول المباشر بحساب Xtream (Host/User/Pass) بدون كود", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showXtreamLogin,
                            onCheckedChange = { viewModel.setShowXtreamDirectLogin(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val isShareVisible by viewModel.isShareButtonVisible.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("زر مشاركة التطبيق", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء زر مشاركة التطبيق في الشريط العلوي والقائمة", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isShareVisible,
                            onCheckedChange = { viewModel.setShareButtonVisibilityOnline(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showAiDrawer by viewModel.showAiSectionInDrawer.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قسم المساعد الذكي في القائمة الجانبية", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء زر المساعد الذكي في القائمة الجانبية", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showAiDrawer,
                            onCheckedChange = { viewModel.setShowAiSectionInDrawer(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showReels by viewModel.showReelsSection.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قسم ريلز الفيديوهات القصيرة (Reels)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء شريط الفيديوهات القصيرة في الرئيسية", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showReels,
                            onCheckedChange = { viewModel.setReelsTabVisibilityOnline(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showPackages by viewModel.showPackagesSection.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قسم الباقات المنشورة (Packages)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء قسم الباقات في الرئيسية", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showPackages,
                            onCheckedChange = { viewModel.setShowPackagesSection(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val eventsVisibilityMode by viewModel.eventsVisibilityMode.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التحكم في قسم الأحداث والمباريات", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("تحكم فوري أونلاين عبر قاعدة البيانات لمن يظهر له القسم", color = SoftGreyText, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("ALL", "ظاهر للجميع", "🟢"),
                                Triple("VIP_ONLY", "أكواد VIP فقط", "👑"),
                                Triple("HIDDEN", "إخفاء للكل", "🚫")
                            ).forEach { (modeKey, modeTitle, modeIcon) ->
                                val isSelected = eventsVisibilityMode == modeKey
                                Surface(
                                    color = if (isSelected) {
                                        if (modeKey == "VIP_ONLY") GoldGlow.copy(alpha = 0.25f)
                                        else if (modeKey == "HIDDEN") LoopRed.copy(alpha = 0.25f)
                                        else Color(0xFF4CAF50).copy(alpha = 0.25f)
                                    } else Color.White.copy(alpha = 0.04f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) {
                                            if (modeKey == "VIP_ONLY") GoldGlow
                                            else if (modeKey == "HIDDEN") LoopRed
                                            else Color(0xFF4CAF50)
                                        } else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setEventsVisibilityMode(modeKey) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(modeIcon, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = modeTitle,
                                            color = if (isSelected) Color.White else SoftGreyText,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (eventsVisibilityMode) {
                                "VIP_ONLY" -> "👑 الوضع الحالي: متاح حصرياً لمشتركي VIP، ومخفي عن المشتركين العاديين."
                                "HIDDEN" -> "🚫 الوضع الحالي: مخفي تماماً عن جميع المستخدمين في الصفحة الرئيسية."
                                else -> "🟢 الوضع الحالي: ظاهر ونشط لكافة المستخدمين والأكواد."
                            },
                            color = when (eventsVisibilityMode) {
                                "VIP_ONLY" -> GoldGlow
                                "HIDDEN" -> LoopRed
                                else -> Color(0xFF4CAF50)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showChatTab by viewModel.showChatTab.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تبويب الدردشة العامة", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء تبويب الدردشة العامة للمستخدمين", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showChatTab,
                            onCheckedChange = { viewModel.setChatTabVisibilityOnline(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val liveOnlineUsersCount by viewModel.liveOnlineUsersCount.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إظهار عداد المتصلين الحقيقي (Online)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (homeConfig.showOnlineUsers) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (homeConfig.showOnlineUsers) Color(0xFF00E676).copy(alpha = 0.5f) else Color.Transparent)
                                ) {
                                    Text(
                                        text = if (homeConfig.showOnlineUsers) "$liveOnlineUsersCount متصل الآن" else "مخفي عالمياً",
                                        color = if (homeConfig.showOnlineUsers) Color(0xFF00E676) else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("التحكم في إظهار أو إخفاء عداد المتصلين الحي سحابياً عبر قاعدة البيانات Firebase ليطبق على جميع المستخدمين عالمياً فوراً", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = homeConfig.showOnlineUsers,
                            onCheckedChange = { isEnabled ->
                                viewModel.updateHomeConfig(homeConfig.copy(showOnlineUsers = isEnabled))
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF00E676))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إظهار قسم الأخبار الجديد بالرئيسية", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("التحكم في تفعيل أو إخفاء قسم الأخبار الجديد كلياً في الواجهة الرئيسية فورياً", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = homeConfig.showNewsSection,
                            onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showNewsSection = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("التحكم العالمي بالإعلانات والواجهة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val isGlobalAdsEnabled by viewModel.rawGlobalAdsEnabled.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("إزالة جميع الإعلانات من التطبيق", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (!isGlobalAdsEnabled) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (!isGlobalAdsEnabled) "Online: بدون إعلانات" else "Online: إعلانات مفعلة",
                                        color = if (!isGlobalAdsEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("عند التفعيل يتم حذف وتعطيل كافة إعلانات التطبيق ومزودات الإعلانات لجميع المستخدمين فوراً", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = !isGlobalAdsEnabled,
                            onCheckedChange = { removeAds -> viewModel.setGlobalAdsEnabled(!removeAds) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.setGlobalAdsEnabled(!isGlobalAdsEnabled) },
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGlobalAdsEnabled) Color(0xFFE50914) else Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isGlobalAdsEnabled) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGlobalAdsEnabled) "إزالة جميع الإعلانات من التطبيق" else "تفعيل ظهور الإعلانات مجدداً",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val showRemoveAdsIcon by viewModel.rawShowRemoveAdsIcon.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أيقونة إزالة الإعلانات في الهيدر", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("إظهار أو إخفاء أيقونة التاج في الشريط العلوي للمستخدمين", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = showRemoveAdsIcon,
                            onCheckedChange = { viewModel.setShowRemoveAdsIcon(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    val isVpnBlockEnabled by viewModel.isVpnBlockEnabled.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حظر استخدام VPN في التطبيق (Anti-VPN)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("منع أو السماح للمستخدمين بالدخول واستخدام التطبيق عند تفعيل برامج الـ VPN", color = SoftGreyText, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isVpnBlockEnabled,
                            onCheckedChange = { viewModel.setVpnBlockEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                        )
                    }
                }
            }
        }

        item {
            val currentMode by viewModel.playerMode.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشغل الفيديو الافتراضي", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("اختر صيغة ومحرك التشغيل الافتراضي للبث المباشر والقنوات:", color = SoftGreyText, fontSize = 11.sp)

                    val playerModes = listOf(
                        "smart" to "الذكي التلقائي (Smart Auto)",
                        "ts" to "محرك TS الفائق (TS Engine)",
                        "m3u8" to "بث M3U8 المباشر (HLS)",
                        "normal" to "التشغيل المباشر (Direct)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playerModes.forEach { (modeKey, modeTitle) ->
                            val isSelected = currentMode == modeKey
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPlayerMode(modeKey) },
                                color = if (isSelected) GoldGlow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(1.dp, if (isSelected) GoldGlow else Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = modeTitle,
                                        color = if (isSelected) GoldGlow else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val apiKeys by viewModel.aiApiKeys.collectAsState()
            var showAddDialog by remember { mutableStateOf(false) }
            var editingKey by remember { mutableStateOf<AiApiKey?>(null) }
            
            var inputName by remember { mutableStateOf("") }
            var inputKey by remember { mutableStateOf("") }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MidnightGrey),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إدارة مفاتيح ذكاء اصطناعي OpenAI API", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك إضافة أكثر من مفتاح API لتوزيع الضغط بين سيرفرات المطور وتجنب انقطاع الخدمة.",
                        color = SoftGreyText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    
                    // Online Status Indicator
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حالة اتصال الموزّع الذكي: متصل ومستقر (توزيع عبء العمل نشط)",
                            color = Color.Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // List of Keys
                    if (apiKeys.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد مفاتيح مخصصة مضافة حالياً. جاري استخدام المفتاح الماستر الافتراضي للتطبيق.", color = SoftGreyText, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            apiKeys.forEach { apiKey ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(apiKey.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (apiKey.key.length > 15) apiKey.key.take(10) + "..." + apiKey.key.takeLast(5) else apiKey.key,
                                            color = SoftGreyText,
                                            fontSize = 10.sp
                                        )
                                    }
                                    
                                    // Edit Button
                                    IconButton(
                                        onClick = {
                                            editingKey = apiKey
                                            inputName = apiKey.name
                                            inputKey = apiKey.key
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldGlow, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    // Delete Button
                                    IconButton(
                                        onClick = { viewModel.deleteAiApiKey(apiKey.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = LoopRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add Button
                    Button(
                        onClick = {
                            inputName = ""
                            inputKey = ""
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianBlack, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة مفتاح API جديد", color = ObsidianBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            
            // ADD DIALOG
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("إضافة مفتاح ذكاء اصطناعي جديد", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("اسم المفتاح / السيرفر") },
                                placeholder = { Text("مثال: سيرفر المساعد الرئيسي") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldGlow, focusedLabelColor = GoldGlow)
                            )
                            OutlinedTextField(
                                value = inputKey,
                                onValueChange = { inputKey = it },
                                label = { Text("مفتاح API Key") },
                                placeholder = { Text("token_...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldGlow, focusedLabelColor = GoldGlow)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputName.isNotBlank() && inputKey.isNotBlank()) {
                                    viewModel.addAiApiKey(inputKey.trim(), inputName.trim())
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                        ) {
                            Text("إضافة", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("إلغاء", color = SoftGreyText)
                        }
                    },
                    containerColor = MidnightGrey
                )
            }
            
            // EDIT DIALOG
            if (editingKey != null) {
                AlertDialog(
                    onDismissRequest = { editingKey = null },
                    title = { Text("تعديل مفتاح الذكاء الاصطناعي", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inputName,
                                onValueChange = { inputName = it },
                                label = { Text("اسم المفتاح / السيرفر") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldGlow, focusedLabelColor = GoldGlow)
                            )
                            OutlinedTextField(
                                value = inputKey,
                                onValueChange = { inputKey = it },
                                label = { Text("مفتاح API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldGlow, focusedLabelColor = GoldGlow)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = editingKey?.id
                                if (id != null && inputName.isNotBlank() && inputKey.isNotBlank()) {
                                    viewModel.updateAiApiKey(id, inputKey.trim(), inputName.trim())
                                    editingKey = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                        ) {
                            Text("حفظ التغييرات", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingKey = null }) {
                            Text("إلغاء", color = SoftGreyText)
                        }
                    },
                    containerColor = MidnightGrey
                )
            }
        }
        
        item {
            Button(
                onClick = { viewModel.logoutDeveloper(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
            ) {
                Text("خروج من وضع المطور", color = LoopRed)
            }
        }
    }
}

@Composable
fun DevNormalCodesTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val codes by viewModel.activationCodes.collectAsState()
    val actionStatus by viewModel.developerActionStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf<String?>(null) }

    var nCode by remember { mutableStateOf("") }
    var nHost by remember { mutableStateOf("") }
    var nM3uUrl by remember { mutableStateOf("") }
    var nIsM3uMode by remember { mutableStateOf(false) }
    var nUser by remember { mutableStateOf("") }
    var nPass by remember { mutableStateOf("") }
    var nFormat by remember { mutableStateOf("ts") }
    var nAllowLive by remember { mutableStateOf(true) }
    var nAllowMovies by remember { mutableStateOf(true) }
    var nAllowSeries by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadActivationCodes()
    }

    fun parseAndApplyNormalUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.contains(".m3u", ignoreCase = true) || trimmed.contains("type=m3u", ignoreCase = true) || trimmed.contains("get.php", ignoreCase = true)) {
            nM3uUrl = trimmed
            nIsM3uMode = true
            if (trimmed.contains(".m3u8", ignoreCase = true)) {
                nFormat = "m3u8"
            }
        }

        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            try {
                val uri = Uri.parse(trimmed)
                val uParam = uri.getQueryParameter("username")
                val pParam = uri.getQueryParameter("password")
                if (!uParam.isNullOrEmpty() && !pParam.isNullOrEmpty()) {
                    nHost = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
                    nUser = uParam
                    nPass = pParam
                    nFormat = "m3u8"
                } else {
                    val pathSegments = uri.pathSegments ?: emptyList()
                    val hostPart = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
                    if (pathSegments.size >= 2) {
                        val filteredSegments = if (pathSegments.isNotEmpty() && listOf("live", "movie", "series").contains(pathSegments[0].lowercase())) {
                            pathSegments.drop(1)
                        } else {
                            pathSegments
                        }
                        if (filteredSegments.size >= 2) {
                            nHost = hostPart
                            nUser = filteredSegments[0]
                            nPass = filteredSegments[1]
                            val lastSeg = filteredSegments.lastOrNull() ?: ""
                            if (lastSeg.endsWith(".pdf", ignoreCase = true)) {
                                nFormat = "pdf"
                            } else if (lastSeg.endsWith(".m3u8", ignoreCase = true)) {
                                nFormat = "m3u8"
                            } else if (lastSeg.endsWith(".ts", ignoreCase = true)) {
                                nFormat = "ts"
                            } else {
                                nFormat = "direct"
                            }
                        } else {
                            nHost = trimmed
                        }
                    } else {
                        nHost = trimmed
                    }
                }
            } catch (e: Exception) {
                nHost = trimmed
            }
        } else {
            nHost = trimmed
        }
    }

    val filteredCodes = remember(codes, searchQuery) {
        if (searchQuery.isBlank()) codes
        else codes.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.host.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("أكواد التفعيل العادية", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { viewModel.loadActivationCodes() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = SoftGreyText)
                        }
                    }
                    Text(
                        "إمكانية نشر وإدارة أكواد التفعيل العادية لكافة المستخدمين مع تخصيص الأقسام (قنوات، أفلام، مسلسلات) ونوع البث.",
                        color = SoftGreyText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    if (!actionStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF1E2638),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = actionStatus ?: "",
                                color = GoldGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    nCode = ""
                    nHost = ""
                    nM3uUrl = ""
                    nIsM3uMode = false
                    nUser = ""
                    nPass = ""
                    nFormat = "ts"
                    nAllowLive = true
                    nAllowMovies = true
                    nAllowSeries = true
                    showAddDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("نشر كود تفعيل عادي جديد ➕", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("بحث عن كود...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftGreyText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الأكواد المنشورة (${filteredCodes.size})", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredCodes.isEmpty()) {
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد أكواد عادية منشورة حتى الآن." else "لم يتم العثور على أكواد مطابقة لبحثك.",
                            color = SoftGreyText,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filteredCodes.forEach { item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.code,
                                                    color = GoldGlow,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(LoopRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = when (item.forcedFormat.lowercase()) {
                                                            "pdf" -> "PDF"
                                                            "m3u8" -> "M3U8"
                                                            "direct", "normal" -> "مباشر"
                                                            else -> "TS"
                                                        },
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                            val clip = android.content.ClipData.newPlainText("code", item.code)
                                                            clipboard?.setPrimaryClip(clip)
                                                            Toast.makeText(context, "تم نسخ الكود: ${item.code}", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            // ignore
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = GoldGlow, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        nCode = item.code
                                                        nHost = item.host
                                                        nM3uUrl = item.m3uUrl
                                                        nIsM3uMode = item.m3uUrl.isNotBlank()
                                                        nUser = item.username
                                                        nPass = item.password
                                                        nFormat = item.forcedFormat
                                                        nAllowLive = item.allowedTypes.contains("live")
                                                        nAllowMovies = item.allowedTypes.contains("movies")
                                                        nAllowSeries = item.allowedTypes.contains("series")
                                                        showAddDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { codeToDelete = item.code },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = LoopRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (item.m3uUrl.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "📑 رابط M3U",
                                                    color = GoldGlow,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(GoldGlow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("M3U: ${item.m3uUrl}", color = SoftGreyText, fontSize = 10.sp, maxLines = 1)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        if (item.host.isNotBlank() && item.host != item.m3uUrl) {
                                            Text("Host: ${item.host}", color = SoftGreyText, fontSize = 11.sp, maxLines = 1)
                                        }
                                        if (item.username.isNotBlank()) {
                                            Text("User: ${item.username}", color = SoftGreyText.copy(alpha = 0.8f), fontSize = 10.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            val types = if (item.allowedTypes.isEmpty()) listOf("live", "movies", "series") else item.allowedTypes
                                            if (types.contains("live")) {
                                                Text("📺 قنوات", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (types.contains("movies")) {
                                                Text("🎬 أفلام", color = Color(0xFF2196F3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (types.contains("series")) {
                                                Text("🍿 مسلسلات", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    if (nIsM3uMode) "نشر كود مع رابط M3U 📑" else "نشر كود تفعيل عادي 🔑",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Type Selector: Xtream vs Direct M3U
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (!nIsM3uMode) LoopRed else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (!nIsM3uMode) LoopRed else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.weight(1f).clickable { nIsM3uMode = false }
                            ) {
                                Text(
                                    "سيرفر Xtream 🌐",
                                    color = if (!nIsM3uMode) Color.White else SoftGreyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Surface(
                                color = if (nIsM3uMode) GoldGlow.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (nIsM3uMode) GoldGlow else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.weight(1f).clickable { nIsM3uMode = true }
                            ) {
                                Text(
                                    "رابط M3U مباشر 📑",
                                    color = if (nIsM3uMode) GoldGlow else SoftGreyText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = nCode,
                            onValueChange = { nCode = it },
                            label = { Text("رمز الكود (Code)") },
                            placeholder = { Text("مثال: M3U2026 أو NORMAL1") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (nIsM3uMode) {
                        item {
                            Column {
                                OutlinedTextField(
                                    value = nM3uUrl,
                                    onValueChange = {
                                        nM3uUrl = it
                                        if (nHost.isBlank()) nHost = it
                                    },
                                    label = { Text("رابط قائمة M3U المباشر") },
                                    placeholder = { Text("http://example.com/playlist.m3u8", fontSize = 10.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💡 يعمل مباشرة وبأعلى سرعة لقنوات M3U و M3U8",
                                        color = GoldGlow,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                                if (clip.isNotBlank()) {
                                                    nM3uUrl = clip.trim()
                                                    if (nHost.isBlank()) nHost = clip.trim()
                                                }
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("لصق الرابط", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            OutlinedTextField(
                                value = nHost,
                                onValueChange = { parseAndApplyNormalUrl(it) },
                                label = { Text(if (nIsM3uMode) "رابط السيرفر الأساسي (اختياري)" else "رابط الهوست أو الرابط الكامل") },
                                placeholder = { Text("http://server.com:8080", fontSize = 10.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!nIsM3uMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💡 الصق أي رابط مباشر وسيتم ملء البيانات تلقائياً",
                                        color = GoldGlow,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                                if (clip.isNotBlank()) {
                                                    parseAndApplyNormalUrl(clip)
                                                }
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("لصق ذكي", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (!nIsM3uMode || nUser.isNotBlank() || nPass.isNotBlank()) {
                        item {
                            OutlinedTextField(
                                value = nUser,
                                onValueChange = { nUser = it },
                                label = { Text(if (nIsM3uMode) "اسم المستخدم (إن وُجد)" else "اسم المستخدم (Username)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = nPass,
                                onValueChange = { nPass = it },
                                label = { Text(if (nIsM3uMode) "كلمة المرور (إن وُجدت)" else "كلمة المرور (Password)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Text("صيغة الفيديو المفضلة:", color = SoftGreyText, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ts" to "TS (.ts)", "m3u8" to "M3U8", "direct" to "مباشر").forEach { (formatKey, formatLabel) ->
                                val isSelected = nFormat == formatKey
                                Surface(
                                    color = if (isSelected) LoopRed else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) LoopRed else Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.weight(1f).clickable { nFormat = formatKey }
                                ) {
                                    Text(
                                        text = formatLabel,
                                        color = if (isSelected) Color.White else SoftGreyText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("الأقسام المسموح بها في هذا الكود:", color = SoftGreyText, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = nAllowLive, onCheckedChange = { nAllowLive = it })
                                Text("📺 بث مباشر", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = nAllowMovies, onCheckedChange = { nAllowMovies = it })
                                Text("🎬 أفلام", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = nAllowSeries, onCheckedChange = { nAllowSeries = it })
                                Text("🍿 مسلسلات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val effectiveHost = when {
                            nHost.isNotBlank() -> nHost.trim()
                            nM3uUrl.isNotBlank() -> nM3uUrl.trim()
                            else -> ""
                        }
                        if (nCode.isNotBlank() && effectiveHost.isNotBlank()) {
                            val allowed = mutableListOf<String>()
                            if (nAllowLive) allowed.add("live")
                            if (nAllowMovies) allowed.add("movies")
                            if (nAllowSeries) allowed.add("series")
                            if (allowed.isEmpty()) allowed.addAll(listOf("live", "movies", "series"))

                            val finalM3u = if (nIsM3uMode || nM3uUrl.isNotBlank()) nM3uUrl.trim() else ""

                            viewModel.adminSaveActivationCode(
                                code = nCode.trim(),
                                host = effectiveHost,
                                username = nUser.trim(),
                                password = nPass.trim(),
                                forcedFormat = nFormat.trim(),
                                allowedTypes = allowed,
                                m3uUrl = finalM3u
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("نشر الكود الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = SoftGreyText) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (codeToDelete != null) {
        AlertDialog(
            onDismissRequest = { codeToDelete = null },
            title = { Text("تأكيد الحذف", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف كود التفعيل: ${codeToDelete}؟", color = SoftGreyText, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        codeToDelete?.let { viewModel.adminDeleteActivationCode(it) }
                        codeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { codeToDelete = null }) { Text("إلغاء", color = SoftGreyText) }
            }
        )
    }
}

@Composable
fun DevVipSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val vipCodes by viewModel.vipCodes.collectAsState()
    var showAddVipDialog by remember { mutableStateOf(false) }

    var vCode by remember { mutableStateOf("") }
    var vHost by remember { mutableStateOf("") }
    var vUser by remember { mutableStateOf("") }
    var vPass by remember { mutableStateOf("") }
    var vFormat by remember { mutableStateOf("ts") }
    var vMaxUsers by remember { mutableStateOf("1") }
    var vAllowVpn by remember { mutableStateOf(false) }

    fun parseAndApplyUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            try {
                val uri = Uri.parse(trimmed)
                val uParam = uri.getQueryParameter("username")
                val pParam = uri.getQueryParameter("password")
                if (!uParam.isNullOrEmpty() && !pParam.isNullOrEmpty()) {
                    vHost = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
                    vUser = uParam
                    vPass = pParam
                    vFormat = "m3u8"
                } else {
                    val pathSegments = uri.pathSegments ?: emptyList()
                    val hostPart = "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
                    if (pathSegments.size >= 2) {
                        val filteredSegments = if (pathSegments.isNotEmpty() && listOf("live", "movie", "series").contains(pathSegments[0].lowercase())) {
                            pathSegments.drop(1)
                        } else {
                            pathSegments
                        }
                        if (filteredSegments.size >= 2) {
                            vHost = hostPart
                            vUser = filteredSegments[0]
                            vPass = filteredSegments[1]
                            val lastSeg = filteredSegments.lastOrNull() ?: ""
                            if (lastSeg.endsWith(".pdf", ignoreCase = true)) {
                                vFormat = "pdf"
                            } else if (lastSeg.endsWith(".m3u8", ignoreCase = true)) {
                                vFormat = "m3u8"
                            } else if (lastSeg.endsWith(".ts", ignoreCase = true)) {
                                vFormat = "ts"
                            } else {
                                vFormat = "direct"
                            }
                        } else {
                            vHost = trimmed
                        }
                    } else {
                        vHost = trimmed
                    }
                }
            } catch (e: Exception) {
                vHost = trimmed
            }
        } else {
            vHost = rawInput
        }
    }

    LaunchedEffect(Unit) { viewModel.loadVipCodes() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("نشر وإدارة أكواد VIP", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("قائمة الأكواد الحالية", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (vipCodes.isEmpty()) {
                        Text("لا توجد أكواد VIP منشورة حالياً.", color = SoftGreyText, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            vipCodes.forEach { code ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(code.code, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(GoldGlow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (code.forcedFormat.lowercase()) {
                                                        "pdf" -> "PDF"
                                                        "m3u8" -> "M3U8"
                                                        "direct", "normal" -> "مباشر"
                                                        else -> "TS"
                                                    },
                                                    color = GoldGlow,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(if (code.allowVpn) Color.Green.copy(alpha = 0.15f) else LoopRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (code.allowVpn) "🔓 VPN مسموح" else "🛡️ حظر VPN",
                                                    color = if (code.allowVpn) Color.Green else LoopRed,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${code.currentUsers} / ${code.maxUsers} مستخدم متصل حالياً", color = if (code.currentUsers >= code.maxUsers) LoopRed else Color.Green, fontSize = 11.sp)
                                        Text("Host: ${code.host}", color = SoftGreyText, fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            vCode = code.code
                                            vHost = code.host
                                            vUser = code.username
                                            vPass = code.password
                                            vFormat = code.forcedFormat
                                            vMaxUsers = code.maxUsers.toString()
                                            vAllowVpn = code.allowVpn
                                            showAddVipDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteVipCode(code.code) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = LoopRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            vCode = ""
                            vHost = ""
                            vUser = ""
                            vPass = ""
                            vFormat = "ts"
                            vMaxUsers = "1"
                            vAllowVpn = false
                            showAddVipDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("نشر كود VIP جديد", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddVipDialog) {
        AlertDialog(
            onDismissRequest = { showAddVipDialog = false },
            title = { Text("إعداد ونشر كود VIP", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = vCode,
                            onValueChange = { vCode = it },
                            label = { Text("الكود (Code)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Column {
                            OutlinedTextField(
                                value = vHost,
                                onValueChange = { parseAndApplyUrl(it) },
                                label = { Text("هوست السيرفر أو الرابط المباشر الكامل") },
                                placeholder = { Text("http://host.com:8080/user/pass/id", fontSize = 10.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡 الصق أي رابط مباشر وسيتم ملء الحقول تلقائياً",
                                    color = GoldGlow,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            if (clip.isNotBlank()) {
                                                parseAndApplyUrl(clip)
                                            }
                                        } catch (e: Exception) {
                                            // ignore clipboard read error
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("لصق ذكي", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = vUser,
                            onValueChange = { vUser = it },
                            label = { Text("اسم المستخدم (Username)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = vPass,
                            onValueChange = { vPass = it },
                            label = { Text("كلمة المرور (Password)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text(
                            text = "صيغة وامتداد تشغيل الفيديو:",
                            color = SoftGreyText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val formatList = listOf(
                                "ts" to "TS (.ts)",
                                "m3u8" to "M3U8 (.m3u8)",
                                "pdf" to "PDF (.pdf)",
                                "direct" to "رابط مباشر (Direct)"
                            )
                            formatList.forEach { (formatKey, formatLabel) ->
                                val isSelected = vFormat == formatKey || (formatKey == "direct" && vFormat == "normal")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldGlow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        .clickable { vFormat = formatKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatLabel,
                                        color = if (isSelected) ObsidianBlack else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = vMaxUsers,
                            onValueChange = { vMaxUsers = it },
                            label = { Text("أقصى عدد مستخدمين متزامنين") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = if (vAllowVpn) Color.Green else LoopRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("السماح بالدخول عبر VPN لهذا الكود", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = if (vAllowVpn) "مسموح: يمكن للمستخدم تشغيل الـ VPN أثناء استخدام هذا الكود" else "محظور: سيتم حظر التطبيق إذا قام المستخدم بتشغيل الـ VPN",
                                        color = SoftGreyText,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = vAllowVpn,
                                    onCheckedChange = { vAllowVpn = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color.Green, uncheckedTrackColor = LoopRed.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (vCode.isNotBlank()) {
                        viewModel.saveVipCode(VipCode(
                            code = vCode.trim(),
                            host = vHost.trim(),
                            username = vUser.trim(),
                            password = vPass.trim(),
                            forcedFormat = vFormat.trim(),
                            maxUsers = vMaxUsers.toIntOrNull() ?: 1,
                            allowVpn = vAllowVpn
                        ))
                        showAddVipDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)) {
                    Text("نشر الآن", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVipDialog = false }) { Text("إلغاء", color = SoftGreyText) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun DevVersionControlTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.appVersionConfig.collectAsState()
    val status by viewModel.appVersionStatus.collectAsState()

    var vAppEnabled by remember(config) { mutableStateOf(config.appEnabled) }
    var vMinimumVersion by remember(config) { mutableStateOf(config.minimumVersion) }
    var vCurrentVersion by remember(config) { mutableStateOf(config.currentVersion) }
    var vForceUpdate by remember(config) { mutableStateOf(config.forceUpdate) }
    var vUpdateUrl by remember(config) { mutableStateOf(config.updateUrl) }
    var vUpdateTitle by remember(config) { mutableStateOf(config.updateTitle) }
    var vUpdateMessage by remember(config) { mutableStateOf(config.updateMessage) }
    var vMaintenanceTitle by remember(config) { mutableStateOf(config.maintenanceTitle) }
    var vMaintenanceMessage by remember(config) { mutableStateOf(config.maintenanceMessage) }
    var vTelegramUrl by remember(config) { mutableStateOf(config.telegramSupportUrl) }

    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = LoopRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("إدارة الإصدارات والتحديث الإجباري", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("تحكم بإيقاف النسخ القديمة وتفعيل وضع الصيانة فورياً عبر Firebase", color = SoftGreyText, fontSize = 12.sp)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        // Current Installed Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("معلومات هذا التطبيق المثبت", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("رقم إصدار هذا الكود (CURRENT):", color = SoftGreyText, fontSize = 13.sp)
                        Text("v${AppVersionManager.CURRENT_APP_VERSION}", color = GoldGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الحالة المطبقة على هذا الجهاز:", color = SoftGreyText, fontSize = 13.sp)
                        val statusText = when (status) {
                            is AppVersionStatus.Allowed -> "✅ مسموح وشغال بالكامل"
                            is AppVersionStatus.Maintenance -> "🛠️ وضع الصيانة نشط"
                            is AppVersionStatus.UpdateRequired -> "⚠️ محظور (يتطلب تحديث)"
                            is AppVersionStatus.UpdateRecommended -> "💡 تحديث اختياري متاح"
                            is AppVersionStatus.Loading -> "⏳ جاري التحقق..."
                        }
                        val statusColor = when (status) {
                            is AppVersionStatus.Allowed -> Color(0xFF4CAF50)
                            is AppVersionStatus.Maintenance -> Color(0xFFFFB300)
                            is AppVersionStatus.UpdateRequired -> Color(0xFFFF5252)
                            else -> SoftGreyText
                        }
                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Fast Preset Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إجراءات سريعة بنقرة واحدة (Presets)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                vAppEnabled = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔒 إيقاف للصيانة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                vAppEnabled = true
                                vMinimumVersion = "2.0.0"
                                vCurrentVersion = "2.0.0"
                                vForceUpdate = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("✅ تشغيل طبيعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            vAppEnabled = true
                            vMinimumVersion = "2.0.0"
                            vCurrentVersion = "2.0.0"
                            vForceUpdate = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("⚡ حظر كل ما قبل 2.0.0 وتشغيل هذه النسخة فقط", color = ObsidianBlack, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Version & Gate Form Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إعدادات السحابة المباشرة (Firebase Version Control)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)

                    // App Enabled Switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("حالة التطبيق العامة (appEnabled)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(if (vAppEnabled) "التطبيق يعمل لجميع المستخدمين" else "التطبيق مقفل تماماً بوضع الصيانة", color = SoftGreyText, fontSize = 11.sp)
                            }
                            Switch(
                                checked = vAppEnabled,
                                onCheckedChange = { vAppEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4CAF50))
                            )
                        }
                    }

                    // Force Update Switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إلزامية التحديث الفوري (forceUpdate)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("إجبار أي مستخدم لديه إصدار أقدم من المطلوب على التحديث فوراً", color = SoftGreyText, fontSize = 11.sp)
                            }
                            Switch(
                                checked = vForceUpdate,
                                onCheckedChange = { vForceUpdate = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = LoopRed)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = vMinimumVersion,
                        onValueChange = { vMinimumVersion = it },
                        label = { Text("الحد الأدنى للإصدار المسموح (minimumVersion)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vCurrentVersion,
                        onValueChange = { vCurrentVersion = it },
                        label = { Text("أحدث إصدار متوفر للتطبيق (currentVersion)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vUpdateUrl,
                        onValueChange = { vUpdateUrl = it },
                        label = { Text("رابط تحميل التحديث (updateUrl)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vTelegramUrl,
                        onValueChange = { vTelegramUrl = it },
                        label = { Text("رابط قناة الدعم والتليجرام (telegramSupportUrl)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = vUpdateTitle,
                        onValueChange = { vUpdateTitle = it },
                        label = { Text("عنوان شاشة التحديث (updateTitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vUpdateMessage,
                        onValueChange = { vUpdateMessage = it },
                        label = { Text("رسالة شاشة التحديث (updateMessage)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = vMaintenanceTitle,
                        onValueChange = { vMaintenanceTitle = it },
                        label = { Text("عنوان شاشة الصيانة (maintenanceTitle)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vMaintenanceMessage,
                        onValueChange = { vMaintenanceMessage = it },
                        label = { Text("رسالة شاشة الصيانة (maintenanceMessage)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isSaving = true
                            val updatedConfig = AppVersionConfig(
                                currentVersion = vCurrentVersion.trim().ifEmpty { "2.0.0" },
                                minimumVersion = vMinimumVersion.trim().ifEmpty { "2.0.0" },
                                forceUpdate = vForceUpdate,
                                appEnabled = vAppEnabled,
                                updateUrl = vUpdateUrl.trim().ifEmpty { "https://t.me/plus90_official" },
                                updateTitle = vUpdateTitle.trim().ifEmpty { "تحديث جديد مطلوب 🚀" },
                                updateMessage = vUpdateMessage.trim().ifEmpty { "هذه النسخة لم تعد مدعومة." },
                                maintenanceTitle = vMaintenanceTitle.trim().ifEmpty { "التطبيق متوقف مؤقتًا للصيانة 🛠️" },
                                maintenanceMessage = vMaintenanceMessage.trim().ifEmpty { "نعمل حالياً على صيانة الخوادم." },
                                telegramSupportUrl = vTelegramUrl.trim().ifEmpty { "https://t.me/plus90_official" }
                            )
                            viewModel.updateRemoteVersionConfig(updatedConfig) { success ->
                                isSaving = false
                                val msg = if (success) "تم حفظ وتطبيق إعدادات الإصدار في Firebase بنجاح!" else "فشل الحفظ في السحابة، يرجى التأكد من الإنترنت."
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = ObsidianBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("جاري الحفظ في السحابة...", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ObsidianBlack)
                            Spacer(Modifier.width(8.dp))
                            Text("حفظ وتطبيق على جميع الأجهزة فوراً 🚀", color = ObsidianBlack, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevNotificationsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    
    var inputTitle by remember { mutableStateOf("") }
    var inputSubtitle by remember { mutableStateOf("") }
    var inputBody by remember { mutableStateOf("") }
    var inputImageUrl by remember { mutableStateOf("") }
    var inputLinkUrl by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = LoopRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "مركز إرسال وإدارة الإشعارات أونلاين",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "إرسال إشعارات فورية متصلة بقاعدة البيانات تظهر على أجهزة جميع المستخدمين",
                        color = SoftGreyText,
                        fontSize = 12.sp
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        // Realtime DB Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(EmeraldGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "نظام الإشعارات أونلاين نشط (Firebase RTDB)",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "أي إشعار يتم إرساله هنا يُحفظ في السيرفر ويصل لجميع المستخدمين فوراً",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { viewModel.fetchNotifications() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = EmeraldGreen)
                    }
                }
            }
        }

        // Send New Notification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = LoopRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال إشعار فوري جديد لجميع المستخدمين",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Templates
                    Text(
                        text = "قوالب سريعة جاهزة للاستخدام:",
                        color = SoftGreyText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                inputTitle = "⚽ انطلاق مباراة القمة الآن!"
                                inputSubtitle = "بث مباشر وحصري"
                                inputBody = "شاهد البث المباشر بأعلى جودة وبدون تقطيع على سيرفراتنا."
                                inputLinkUrl = ""
                            },
                            label = { Text("⚽ مباراة قمة", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                inputTitle = "🚀 سيرفرات بث جديدة أضيفت الآن"
                                inputSubtitle = "تحديث السيرفرات"
                                inputBody = "تم تحديث وإضافة سيرفرات جديدة فائقة السرعة لجميع القنوات."
                                inputLinkUrl = ""
                            },
                            label = { Text("🚀 سيرفر جديد", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Title
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("عنوان الإشعار *") },
                        placeholder = { Text("مثال: ⚽ بث مباشر: ريال مدريد ضد برشلونة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            focusedLabelColor = LoopRed
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Subtitle
                    OutlinedTextField(
                        value = inputSubtitle,
                        onValueChange = { inputSubtitle = it },
                        label = { Text("الفرع الإضافي (الوصف القصير) *") },
                        placeholder = { Text("مثال: بث مباشر وحصري") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            focusedLabelColor = LoopRed
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Body
                    OutlinedTextField(
                        value = inputBody,
                        onValueChange = { inputBody = it },
                        label = { Text("نص الإشعار / تفاصيل الرسالة *") },
                        placeholder = { Text("اكتب تفاصيل الإشعار الذي سيظهر للمستخدمين...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            focusedLabelColor = LoopRed
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Image URL
                    OutlinedTextField(
                        value = inputImageUrl,
                        onValueChange = { inputImageUrl = it },
                        label = { Text("رابط صورة الإشعار (اختياري)") },
                        placeholder = { Text("https://example.com/banner.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            focusedLabelColor = LoopRed
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Link URL
                    OutlinedTextField(
                        value = inputLinkUrl,
                        onValueChange = { inputLinkUrl = it },
                        label = { Text("نشر رابط مع الإشعار (اختياري)") },
                        placeholder = { Text("https://example.com/match") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            focusedLabelColor = LoopRed
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Send Button
                    Button(
                        onClick = {
                            if (inputTitle.isBlank() || inputSubtitle.isBlank() || inputBody.isBlank()) {
                                Toast.makeText(context, "يرجى تعبئة العنوان والفرع الإضافي والنص", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSending = true
                            viewModel.sendAppNotification(
                                title = inputTitle,
                                subtitle = inputSubtitle,
                                body = inputBody,
                                imageUrl = inputImageUrl.ifBlank { null },
                                linkUrl = inputLinkUrl.ifBlank { null }
                            ) { success ->
                                isSending = false
                                if (success) {
                                    Toast.makeText(context, "✅ تم إرسال وحفظ الإشعار أونلاين بنجاح لجميع الأجهزة!", Toast.LENGTH_LONG).show()
                                    inputTitle = ""
                                    inputSubtitle = ""
                                    inputBody = ""
                                    inputImageUrl = ""
                                    inputLinkUrl = ""
                                } else {
                                    Toast.makeText(context, "⚠️ فشل إرسال الإشعار، يرجى فحص الاتصال بالإنترنت", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("جاري الإرسال والمزامنة...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("إرسال الإشعار أونلاين لجميع المستخدمين 🚀", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Local Test Button
                    OutlinedButton(
                        onClick = {
                            val title = inputTitle.ifBlank { "🔔 إشعار تجريبي من لوحة المطور" }
                            val body = inputBody.ifBlank { "هذا إشعار تجريبي للتأكد من عمل نظام الإشعارات وشريط الحالة في جهازك بنجاح!" }
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                NotificationHelper.showSystemNotification(
                                    context = context,
                                    title = title,
                                    body = body,
                                    imageUrl = inputImageUrl.ifBlank { null }
                                )
                                Toast.makeText(context, "تم إطلاق الإشعار في شريط التنبيهات بجهازك الآن!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF4E4E5E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("🔔 إشعار تجريبي محلي (على جهازك فقط)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        items(notifications, key = { it.id }) { notif ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.body,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val dateStr = try {
                                    dateFormat.format(Date(notif.timestamp))
                                } catch (e: Exception) {
                                    notif.timestamp.toString()
                                }
                                Text(
                                    text = "🕒 $dateStr",
                                    color = SoftGreyText,
                                    fontSize = 10.sp
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = {
                                    deletingId = notif.id
                                    viewModel.deleteAppNotification(notif.id) { success ->
                                        deletingId = null
                                        if (success) {
                                            Toast.makeText(context, "تم حذف الإشعار من السيرفر بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = deletingId != notif.id
                            ) {
                                if (deletingId == notif.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LoopRed)
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الإشعار", tint = LoopRed.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun DevGlobalChatTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val globalMessages by viewModel.globalChatMessages.collectAsState()
    val chatUsers by viewModel.globalChatUsers.collectAsState()
    val isLoading by viewModel.isGlobalChatLoading.collectAsState()

    var showWipeChatDialog by remember { mutableStateOf(false) }
    var showWipePrivateDialog by remember { mutableStateOf(false) }
    var isWiping by remember { mutableStateOf(false) }
    var isPostingBroadcast by remember { mutableStateOf(false) }
    var broadcastText by remember { mutableStateOf("") }
    var adminSenderName by remember { mutableStateOf("إدارة +90") }
    var searchQuery by remember { mutableStateOf("") }
    var deletingMsgId by remember { mutableStateOf<String?>(null) }
    var selectedSubSection by remember { mutableIntStateOf(0) } // 0: Messages, 1: Users, 2: Broadcast

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    val filteredUsers = remember(chatUsers, searchQuery) {
        if (searchQuery.isBlank()) chatUsers
        else chatUsers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredMessages = remember(globalMessages, searchQuery) {
        if (searchQuery.isBlank()) globalMessages
        else globalMessages.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchGlobalChatModerationData()
    }

    if (showWipeChatDialog) {
        AlertDialog(
            onDismissRequest = { showWipeChatDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LoopRed)
                    Text("تأكيد مسح وتصفير الدردشة العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف وتصفير جميع رسائل الدردشة العامة بالكامل من قاعدة بيانات Firebase؟\n\n⚠️ سيتم مسح جميع الرسائل لجميع المستخدمين نهائياً ولا يمكن استرجاعها.",
                    color = SoftGreyText,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeChatDialog = false
                        isWiping = true
                        viewModel.resetAndWipeAllGlobalChat { success ->
                            isWiping = false
                            if (success) {
                                Toast.makeText(context, "تم مسح وتصفير جميع الرسائل العامة بنجاح من السيرفر", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "حدث خطأ أثناء محاولة التصفير", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("نعم، احذف وصَفِّر الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeChatDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    if (showWipePrivateDialog) {
        AlertDialog(
            onDismissRequest = { showWipePrivateDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = GoldGlow)
                    Text("تصفير المحادثات الخاصة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل ترغب في مسح جميع المحادثات وسجلات الرسائل الخاصة (1v1) من السيرفر؟",
                    color = SoftGreyText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipePrivateDialog = false
                        viewModel.wipeAllPrivateConversations { success ->
                            if (success) {
                                Toast.makeText(context, "تم تصفير المحادثات الخاصة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                ) {
                    Text("مسح الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipePrivateDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Quick Stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(LoopRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Forum, contentDescription = null, tint = LoopRed)
                            }
                            Column {
                                Text("لوحة التحكم بالدردشة العالمية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("إدارة كاملة للمحادثات، الرسائل، والمستخدمين", color = SoftGreyText, fontSize = 11.sp)
                            }
                        }

                        IconButton(
                            onClick = { viewModel.fetchGlobalChatModerationData() },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = LoopRed, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = LoopRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${globalMessages.size}", color = LoopRed, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("رسائل عامة", color = SoftGreyText, fontSize = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${chatUsers.size}", color = GoldGlow, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("حساب مسجل", color = SoftGreyText, fontSize = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("متصل ⚡", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Firebase RTDB", color = SoftGreyText, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Danger Zone: Full Wipe Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF281114)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = LoopRed)
                            Text("تصفير قاعدة بيانات الرسائل العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "حذف جميع الرسائل الموجودة في الدردشة العامة من السيرفر وإعادة ضبط المحادثة للصفر بضغطة واحدة.",
                        color = SoftGreyText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showWipeChatDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isWiping
                    ) {
                        if (isWiping) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري تصفير الرسائل...", color = Color.White, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف وتصفير جميع الرسائل العامة فوراً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Sub-tabs switcher (Messages, Broadcast, Users)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedSubSection == 0) LoopRed else Color.Transparent)
                        .clickable { selectedSubSection = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("الرسائل (${globalMessages.size})", color = if (selectedSubSection == 0) Color.White else SoftGreyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedSubSection == 1) LoopRed else Color.Transparent)
                        .clickable { selectedSubSection = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("إرسال تعميم 📢", color = if (selectedSubSection == 1) Color.White else SoftGreyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedSubSection == 2) LoopRed else Color.Transparent)
                        .clickable { selectedSubSection = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("المستخدمون (${chatUsers.size})", color = if (selectedSubSection == 2) Color.White else SoftGreyText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // SUB-SECTION 1: BROADCAST OFFICIAL ANNOUNCEMENT
        if (selectedSubSection == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldGlow)
                            Text("إرسال رسالة رسمية للدردشة العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedTextField(
                            value = adminSenderName,
                            onValueChange = { adminSenderName = it },
                            label = { Text("اسم المرسل الرسمي", color = SoftGreyText) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGlow,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = broadcastText,
                            onValueChange = { broadcastText = it },
                            label = { Text("نص الرسالة أو التعميم لجميع المستخدمين", color = SoftGreyText) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldGlow,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                if (broadcastText.isNotBlank()) {
                                    isPostingBroadcast = true
                                    viewModel.postGlobalAdminBroadcast(broadcastText.trim(), adminSenderName.trim()) { success ->
                                        isPostingBroadcast = false
                                        if (success) {
                                            broadcastText = ""
                                            Toast.makeText(context, "تم إرسال التعميم إلى الدردشة العامة بنجاح", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشل الإرسال", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isPostingBroadcast && broadcastText.isNotBlank()
                        ) {
                            if (isPostingBroadcast) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نشر في الدردشة العامة الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("تنظيف إضافي:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("يمكنك مسح كافة المحادثات الخاصة (1v1) المسجلة في السيرفر:", color = SoftGreyText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showWipePrivateDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldGlow),
                            border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح سجل المحادثات الخاصة (1v1)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // SUB-SECTION 2: USERS LIST
        if (selectedSubSection == 2) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن مستخدم بالاسم أو المعرف...", color = SoftGreyText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftGreyText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoopRed,
                        unfocusedBorderColor = SoftGreyText.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredUsers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد مستخدمون مسجلون يطابقون البحث", color = SoftGreyText, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredUsers, key = { it.uid }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF2A2A35), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.name.take(1).ifEmpty { "U" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (user.online) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), CircleShape))
                                    }
                                }
                                Text("@${user.username}", color = LoopRed, fontSize = 11.sp)
                                if (user.bio.isNotBlank()) {
                                    Text(user.bio, color = SoftGreyText, fontSize = 11.sp, maxLines = 1)
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteGlobalChatUser(user.uid, user.username) { success ->
                                        if (success) {
                                            Toast.makeText(context, "تم حذف حساب ${user.username} بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "حذف المستخدم", tint = LoopRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // SUB-SECTION 0: MESSAGES LIST (DEFAULT)
        if (selectedSubSection == 0) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في الرسائل أو أسماء المرسلين...", color = SoftGreyText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftGreyText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoopRed,
                        unfocusedBorderColor = SoftGreyText.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            if (filteredMessages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد رسائل عامة حالياً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("الدردشة العامة فارغة أو تم تصفيرها بالكامل", color = SoftGreyText, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredMessages, key = { it.id }) { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E2E38))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(msg.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (msg.username.isNotBlank()) {
                                        Text("@${msg.username}", color = LoopRed, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    color = Color(0xFFEEEEEE),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                val dateStr = try {
                                    dateFormat.format(Date(msg.createdAt))
                                } catch (e: Exception) {
                                    msg.createdAt.toString()
                                }
                                Text("🕒 $dateStr", color = SoftGreyText, fontSize = 10.sp)
                            }

                            // Delete button
                            IconButton(
                                onClick = {
                                    deletingMsgId = msg.id
                                    viewModel.deleteGlobalChatMessage(msg.id) { success ->
                                        deletingMsgId = null
                                        if (success) {
                                            Toast.makeText(context, "تم حذف الرسالة بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp),
                                enabled = deletingMsgId != msg.id
                            ) {
                                if (deletingMsgId == msg.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LoopRed)
                                } else {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الرسالة",
                                        tint = LoopRed.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun DevChatSettingsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isChatEnabled by remember { mutableStateOf(true) }
    var broadcastText by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("إدارة +90") }
    var showWipeChatDialog by remember { mutableStateOf(false) }
    var showWipePrivateDialog by remember { mutableStateOf(false) }
    var isPosting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            isChatEnabled = com.example.data.ChatService.isChatEnabled()
        }
    }

    if (showWipeChatDialog) {
        AlertDialog(
            onDismissRequest = { showWipeChatDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LoopRed)
                    Text("تأكيد مسح وتصفير الدردشة العامة نهائياً", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف وتصفير جميع رسائل الدردشة العامة من قاعدة البيانات؟ لا يمكن التراجع عن هذا الإجراء.",
                    color = SoftGreyText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipeChatDialog = false
                        viewModel.resetAndWipeAllGlobalChat { success ->
                            if (success) {
                                Toast.makeText(context, "🗑️ تم تصفير وحذف جميع رسائل الدردشة العامة بنجاح", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "فشل تصفير الدردشة", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("نعم، احذف وصَفِّر الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeChatDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    if (showWipePrivateDialog) {
        AlertDialog(
            onDismissRequest = { showWipePrivateDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = GoldGlow)
                    Text("تصفير المحادثات الخاصة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل ترغب في مسح جميع سجلات المحادثات الخاصة (1v1) من السيرفر نهائياً؟",
                    color = SoftGreyText,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWipePrivateDialog = false
                        viewModel.wipeAllPrivateConversations { success ->
                            if (success) {
                                Toast.makeText(context, "✅ تم تصفير جميع المحادثات الخاصة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                ) {
                    Text("مسح الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipePrivateDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = LoopRed, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("إعدادات الدردشة والتحكم الشامل", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("إدارة حالة الدردشة، التصفير الشامل، وإعلانات الإدارة", color = SoftGreyText, fontSize = 12.sp)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }


        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نشر إعلان إداري رسمي في الدردشة العامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("اسم المرسل الرسمي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = { broadcastText = it },
                        label = { Text("نص الإعلان الإداري...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (broadcastText.isBlank()) {
                                Toast.makeText(context, "اكتب نص الإعلان أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isPosting = true
                            viewModel.postGlobalAdminBroadcast(broadcastText, adminName) { success ->
                                isPosting = false
                                if (success) {
                                    Toast.makeText(context, "📢 تم نشر الإعلان الإداري في الدردشة العامة بنجاح!", Toast.LENGTH_LONG).show()
                                    broadcastText = ""
                                } else {
                                    Toast.makeText(context, "فشل النشر", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isPosting
                    ) {
                        Text(if (isPosting) "جاري النشر..." else "نشر الإعلان الإداري الآن 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = LoopRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("منطقة الصيانة وتصفير قواعد البيانات", color = LoopRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showWipeChatDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصفير وحذف جميع رسائل الدردشة العامة نهائياً", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showWipePrivateDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, GoldGlow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = GoldGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصفير جميع المحادثات الخاصة (1v1)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper to convert stream URL extension based on selected format
fun convertUrlFormat(url: String, targetFormat: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    
    val queryIndex = trimmed.indexOf('?')
    val baseUrl = if (queryIndex != -1) trimmed.substring(0, queryIndex) else trimmed
    val queryString = if (queryIndex != -1) trimmed.substring(queryIndex) else ""
    
    val lastSlash = baseUrl.lastIndexOf('/')
    val dotIndex = baseUrl.lastIndexOf('.')
    
    return when (targetFormat.lowercase().trim()) {
        "ts" -> {
            if (dotIndex > lastSlash) {
                val ext = baseUrl.substring(dotIndex).lowercase()
                if (ext in listOf(".m3u8", ".ts", ".mp4", ".mkv", ".flv", ".avi", ".mov")) {
                    baseUrl.substring(0, dotIndex) + ".ts" + queryString
                } else {
                    "$baseUrl.ts$queryString"
                }
            } else {
                "$baseUrl.ts$queryString"
            }
        }
        "m3u8" -> {
            if (dotIndex > lastSlash) {
                val ext = baseUrl.substring(dotIndex).lowercase()
                if (ext in listOf(".m3u8", ".ts", ".mp4", ".mkv", ".flv", ".avi", ".mov")) {
                    baseUrl.substring(0, dotIndex) + ".m3u8" + queryString
                } else {
                    "$baseUrl.m3u8$queryString"
                }
            } else {
                "$baseUrl.m3u8$queryString"
            }
        }
        "normal", "direct" -> {
            if (dotIndex > lastSlash) {
                val ext = baseUrl.substring(dotIndex).lowercase()
                if (ext in listOf(".m3u8", ".ts")) {
                    baseUrl.substring(0, dotIndex) + queryString
                } else {
                    trimmed
                }
            } else {
                trimmed
            }
        }
        else -> trimmed
    }
}

@Composable
fun EventFormatSelector(
    selectedFormat: String,
    onFormatSelected: (format: String, newConvertedUrl: String?) -> Unit,
    currentUrl: String = "",
    label: String = "نوع وامتداد البث"
) {
    var expanded by remember { mutableStateOf(false) }

    val formatOptions = listOf(
        Triple("m3u8", "صيغة m3u8 (HLS)", "بث HLS المباشر الافتراضي (.m3u8)"),
        Triple("ts", "صيغة ts (MPEG-TS)", "بث مباشر فائق السرعة والاستقرار (.ts)"),
        Triple("normal", "رابط عادي (Direct)", "تشغيل الرابط الأصلي المباشر بدون تغيير")
    )

    val currentDisplay = when (selectedFormat.lowercase().trim()) {
        "ts" -> "صيغة ts (MPEG-TS)"
        "normal", "direct" -> "رابط عادي (Direct)"
        else -> "صيغة m3u8 (HLS)"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = SoftGreyText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        // Dropdown Clickable Trigger
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (selectedFormat.lowercase().trim()) {
                                "ts" -> Icons.Default.Bolt
                                "normal", "direct" -> Icons.Default.Link
                                else -> Icons.Default.LiveTv
                            },
                            contentDescription = null,
                            tint = when (selectedFormat.lowercase().trim()) {
                                "ts" -> Color(0xFFF59E0B)
                                "normal", "direct" -> Color(0xFF38BDF8)
                                else -> GoldGlow
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = currentDisplay,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (selectedFormat.lowercase().trim()) {
                                    "ts" -> "يتم تحويل الامتداد تلقائياً إلى .ts وتفعيل مشغل TS"
                                    "normal", "direct" -> "يتم تشغيل الرابط كما هو مباشرة بدون تعديل"
                                    else -> "يتم تحويل الامتداد تلقائياً إلى .m3u8 وتفعيل مشغل HLS"
                                },
                                color = SoftGreyText,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "قائمة الاختيارات",
                        tint = GoldGlow
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .background(Color(0xFF1E2430), RoundedCornerShape(12.dp))
                    .border(1.dp, GoldGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                formatOptions.forEach { (fmtKey, title, desc) ->
                    val isSelected = selectedFormat.lowercase().trim() == fmtKey || 
                        (fmtKey == "normal" && selectedFormat.lowercase().trim() == "direct") ||
                        (fmtKey == "m3u8" && selectedFormat.isBlank())
                    
                    DropdownMenuItem(
                        text = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) GoldGlow else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = GoldGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = desc,
                                    color = SoftGreyText,
                                    fontSize = 10.sp
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            val converted = if (currentUrl.isNotBlank()) convertUrlFormat(currentUrl, fmtKey) else null
                            onFormatSelected(fmtKey, converted)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (fmtKey) {
                                    "ts" -> Icons.Default.Bolt
                                    "normal" -> Icons.Default.Link
                                    else -> Icons.Default.LiveTv
                                },
                                contentDescription = null,
                                tint = if (isSelected) GoldGlow else SoftGreyText
                            )
                        }
                    )
                }
            }
        }

        // Quick Segmented Selection Chips (1-tap instant switch)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple("m3u8", "⚡ m3u8", GoldGlow),
                Triple("ts", "🚀 ts", Color(0xFFF59E0B)),
                Triple("normal", "🔗 رابط عادي", Color(0xFF38BDF8))
            ).forEach { (fmt, labelText, color) ->
                val isSelected = selectedFormat.lowercase().trim() == fmt || 
                    (fmt == "normal" && selectedFormat.lowercase().trim() == "direct") ||
                    (fmt == "m3u8" && selectedFormat.isBlank())
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val converted = if (currentUrl.isNotBlank()) convertUrlFormat(currentUrl, fmt) else null
                            onFormatSelected(fmt, converted)
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) color.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) color else Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            color = if (isSelected) color else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DevEventChannelsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsState()
    val allChannels by viewModel.channels.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Broadcast Channels, 1: Matches
    
    var inputName by remember { mutableStateOf("") }
    var inputImageUrl by remember { mutableStateOf("") }
    var inputStreamUrl by remember { mutableStateOf("") }
    var inputForcedFormat by remember { mutableStateOf("m3u8") }
    var inputServers by remember { mutableStateOf(listOf<EventServer>()) }
    
    var inputTargetChannelIds by remember { mutableStateOf(listOf<String>()) }
    var inputMatchDate by remember { mutableStateOf("") }
    var inputMatchTime by remember { mutableStateOf("") }
    var inputEventType by remember { mutableStateOf("stream") }

    val filteredEvents = remember(events, selectedTab) {
        events.filter { 
            if (selectedTab == 0) it.eventType == "stream" else it.eventType == "match"
        }
    }

    fun getStartTimeMs(): Long {
        if (inputMatchDate.isBlank() || inputMatchTime.isBlank()) return 0L
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ENGLISH)
            val date = formatter.parse("$inputMatchDate $inputMatchTime")
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // Sub-form for adding a server to the channel
    var newServerName by remember { mutableStateOf("") }
    var newServerUrl by remember { mutableStateOf("") }
    var newServerFormat by remember { mutableStateOf("m3u8") }

    var isPublishing by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var editingChannel by remember { mutableStateOf<EventChannel?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "مركز إدارة المحتوى الذكي",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "إدارة القنوات المباشرة والمباريات مع دعم سيرفرات متعددة وربط ذكي",
                        color = SoftGreyText,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Section Switcher
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.2f))
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .background(if (selectedTab == 0) GoldGlow.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "نشر قنوات بث مباشرة",
                            color = if (selectedTab == 0) GoldGlow else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(44.dp).background(GoldGlow.copy(alpha = 0.1f)))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .background(if (selectedTab == 1) GoldGlow.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "نشر مباراة / حدث",
                            color = if (selectedTab == 1) GoldGlow else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        }

        // Form Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (selectedTab == 0) "إعدادات القناة المباشرة" else "إعدادات المباراة والحدث",
                        color = GoldGlow,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text(if (selectedTab == 0) "اسم القناة (مثال: beIN SPORTS 1)" else "اسم المباراة (مثال: ريال مدريد vs برشلونة)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputImageUrl,
                        onValueChange = { inputImageUrl = it },
                        label = { Text("رابط شعار القناة / بوستر المباراة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (selectedTab == 0) {
                        // Channel specific fields
                        OutlinedTextField(
                            value = inputStreamUrl,
                            onValueChange = { inputStreamUrl = it },
                            label = { Text("رابط البث الرئيسي") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        EventFormatSelector(
                            selectedFormat = inputForcedFormat,
                            onFormatSelected = { fmt, conv ->
                                inputForcedFormat = fmt
                                if (conv != null) inputStreamUrl = conv
                            },
                            currentUrl = inputStreamUrl,
                            label = "نوع الرابط الرئيسي:"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.05f))
                        
                        Text("إضافة سيرفرات إضافية (اختياري):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = newServerName,
                            onValueChange = { newServerName = it },
                            label = { Text("اسم السيرفر (مثال: HD Server)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newServerUrl,
                            onValueChange = { newServerUrl = it },
                            label = { Text("رابط البث للسيرفر") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        EventFormatSelector(
                            selectedFormat = newServerFormat,
                            onFormatSelected = { fmt, conv ->
                                newServerFormat = fmt
                                if (conv != null) newServerUrl = conv
                            },
                            currentUrl = newServerUrl,
                            label = "نوع رابط السيرفر:"
                        )

                        Button(
                            onClick = {
                                if (newServerUrl.isBlank()) return@Button
                                val srv = EventServer(
                                    id = "srv_${System.currentTimeMillis()}",
                                    name = newServerName.ifBlank { "سيرفر ${inputServers.size + 1}" },
                                    url = newServerUrl,
                                    forcedFormat = newServerFormat
                                )
                                inputServers = inputServers + srv
                                newServerName = ""
                                newServerUrl = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("إضافة سيرفر (+)", fontWeight = FontWeight.Bold)
                        }

                        if (inputServers.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                inputServers.forEachIndexed { idx, srv ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${srv.name} (${srv.forcedFormat})", color = Color.White, fontSize = 12.sp)
                                        IconButton(onClick = { inputServers = inputServers.filterIndexed { i, _ -> i != idx } }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = LoopRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Match specific fields
                        Text("القنوات الناقلة للمباراة (يمكنك اختيار قنوات متعددة):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        
                        var showChannelSelector by remember { mutableStateOf(false) }
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showChannelSelector = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = GoldGlow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (inputTargetChannelIds.isEmpty()) "إضافة القنوات الناقلة..." else "تم اختيار ${inputTargetChannelIds.size} قنوات",
                                    color = if (inputTargetChannelIds.isEmpty()) Color.Gray else Color.White
                                )
                            }
                        }

                        if (inputTargetChannelIds.isNotEmpty()) {
                            androidx.compose.foundation.layout.FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                inputTargetChannelIds.forEach { id ->
                                    val name = (events.find { it.id == id }?.name ?: allChannels.find { it.id == id }?.name) ?: id
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = GoldGlow.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.5f))
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(name, color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(14.dp).clickable { inputTargetChannelIds = inputTargetChannelIds - id },
                                                tint = GoldGlow
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showChannelSelector) {
                            var search by remember { mutableStateOf("") }
                            val filtered = events.filter { it.eventType == "stream" && (search.isEmpty() || it.name.contains(search, ignoreCase = true)) }
                            
                            AlertDialog(
                                onDismissRequest = { showChannelSelector = false },
                                containerColor = Color(0xFF1E2430),
                                title = { Text("اختر القنوات الناقلة", color = Color.White) },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = search,
                                            onValueChange = { search = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("ابحث عن قناة...") }
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                            items(filtered) { ch ->
                                                val isSelected = inputTargetChannelIds.contains(ch.id)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable { 
                                                        inputTargetChannelIds = if (isSelected) inputTargetChannelIds - ch.id else inputTargetChannelIds + ch.id
                                                    }.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(checked = isSelected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = GoldGlow))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(ch.name, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showChannelSelector = false }) { Text("تم", color = GoldGlow) }
                                }
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputMatchDate,
                                onValueChange = { inputMatchDate = it },
                                label = { Text("التاريخ (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputMatchTime,
                                onValueChange = { inputMatchTime = it },
                                label = { Text("الوقت (HH:mm)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (inputName.isBlank()) return@Button
                            isPublishing = true
                            viewModel.publishDeveloperEventChannel(
                                id = editingChannel?.id ?: "",
                                name = inputName,
                                imageUrl = inputImageUrl,
                                streamUrl = inputStreamUrl,
                                forcedFormat = inputForcedFormat,
                                servers = inputServers,
                                eventType = if (selectedTab == 0) "stream" else "match",
                                targetChannelIds = inputTargetChannelIds,
                                startTimeMs = getStartTimeMs()
                            )
                            isPublishing = false
                            inputName = ""
                            inputImageUrl = ""
                            inputStreamUrl = ""
                            inputServers = emptyList()
                            inputTargetChannelIds = emptyList()
                            inputMatchDate = ""
                            inputMatchTime = ""
                            editingChannel = null
                            Toast.makeText(context, "تم النشر بنجاح أونلاين!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (editingChannel == null) "نشر وحفظ أونلاين 🚀" else "حفظ التعديلات أونلاين", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Current Events List
        item {
            Text(
                text = "الأحداث المنشورة حالياً (${events.size})",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (filteredEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedTab == 0) "لا توجد قنوات بث منشورة" else "لا توجد مباريات منشورة",
                            color = SoftGreyText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = event.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(event.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                if (event.eventType == "match") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = LoopRed.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "⚽ مباراة",
                                            color = LoopRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            
                            if (event.eventType == "match") {
                                val channelCount = event.targetChannelIds.size
                                Text("📺 $channelCount قنوات ناقلة", color = SoftGreyText, fontSize = 10.sp)
                            } else {
                                Text(event.streamUrl, color = SoftGreyText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (event.eventType != "match") {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (event.forcedFormat.lowercase().trim()) {
                                            "ts" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            "normal", "direct" -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                            else -> GoldGlow.copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = event.forcedFormat.uppercase().ifEmpty { "M3U8" },
                                            color = when (event.forcedFormat.lowercase().trim()) {
                                                "ts" -> Color(0xFFF59E0B)
                                                "normal", "direct" -> Color(0xFF38BDF8)
                                                else -> GoldGlow
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (event.servers.isNotEmpty()) {
                                    Text("• ${event.servers.size} سيرفرات", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                if (event.eventType == "match" && event.startTimeMs > 0) {
                                    val formattedTime = remember(event.startTimeMs) {
                                        com.example.data.EspnSoccerService.formatArabicMatchTime(event.startTimeMs)
                                    }
                                    if (formattedTime.isNotBlank()) {
                                        Text("• ⏰ $formattedTime", color = GoldGlow, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Row {
                            IconButton(onClick = {
                                editingChannel = event
                                selectedTab = if (event.eventType == "match") 1 else 0
                                inputName = event.name
                                inputImageUrl = event.imageUrl
                                inputStreamUrl = event.streamUrl
                                inputForcedFormat = event.forcedFormat.ifEmpty { "m3u8" }
                                inputServers = event.servers
                                inputEventType = event.eventType.ifEmpty { "stream" }
                                inputTargetChannelIds = event.targetChannelIds
                                if (event.startTimeMs > 0) {
                                    try {
                                        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
                                        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.ENGLISH)
                                        val d = java.util.Date(event.startTimeMs)
                                        inputMatchDate = sdfDate.format(d)
                                        inputMatchTime = sdfTime.format(d)
                                    } catch (e: Exception) {
                                        inputMatchDate = ""
                                        inputMatchTime = ""
                                    }
                                } else {
                                    inputMatchDate = ""
                                    inputMatchTime = ""
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldGlow, modifier = Modifier.size(20.dp))
                            }
                            
                            IconButton(
                                onClick = {
                                    deletingId = event.id
                                    viewModel.deleteDeveloperEventChannel(event.id)
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        kotlinx.coroutines.delay(800)
                                        deletingId = null
                                        Toast.makeText(context, "تم حذف الحدث بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = deletingId != event.id
                            ) {
                                if (deletingId == event.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LoopRed, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = LoopRed.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
