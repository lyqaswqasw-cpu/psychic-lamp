package com.example.ui

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.HomeHeroBanner
import com.example.model.HomeSectionConfig
import com.example.model.PromoAd

@Composable
fun AdsAdminManager(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val promoAds by viewModel.promoAds.collectAsState()

    var isAddingAd by remember { mutableStateOf(false) }
    var editingAdId by remember { mutableStateOf<String?>(null) }

    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var targetUrlInput by remember { mutableStateOf("") }
    var buttonTextInput by remember { mutableStateOf("انضم الآن") }
    var badgeInput by remember { mutableStateOf("إعلان مميز") }
    var typeInput by remember { mutableStateOf("telegram") }
    var isEnabledInput by remember { mutableStateOf(true) }

    fun resetForm() {
        titleInput = ""
        descInput = ""
        imageUrlInput = ""
        targetUrlInput = ""
        buttonTextInput = "انضم الآن"
        badgeInput = "إعلان مميز"
        typeInput = "telegram"
        isEnabledInput = true
        editingAdId = null
        isAddingAd = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            com.example.ui.components.HomeLiveOnlineCounterWidget(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Header info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11162A)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF229ED9).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF229ED9).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF229ED9))
                        }
                        Column {
                            Text("إدارة محتوى الإعلانات والترويج", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("انشر إعلانات قنوات التيليجرام والعروض الحصرية مباشرة في الرئيسية", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val isGlobalAdsEnabled by viewModel.rawGlobalAdsEnabled.collectAsState()
                    Button(
                        onClick = { viewModel.setGlobalAdsEnabled(!isGlobalAdsEnabled) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isGlobalAdsEnabled) Color(0xFFE50914) else Color(0xFF00E676)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Icon(
                            if (isGlobalAdsEnabled) Icons.Default.Block else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isGlobalAdsEnabled) "إزالة جميع الإعلانات من التطبيق (إيقاف)" else "السماح بظهور الإعلانات في التطبيق (تشغيل)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                resetForm()
                                isAddingAd = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة إعلان جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Quick Telegram Template Button
                        OutlinedButton(
                            onClick = {
                                titleInput = "انضم إلى قناة التلغرام الرسمية"
                                descInput = "تابع أقوى التحديثات، سيرفرات VIP، وجدول المباريات اليومية"
                                imageUrlInput = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80"
                                targetUrlInput = "https://t.me/jdj_q"
                                buttonTextInput = "انضم لقناة التيلجرام 🚀"
                                badgeInput = "⚡ قناة التليجرام"
                                typeInput = "telegram"
                                isEnabledInput = true
                                editingAdId = null
                                isAddingAd = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF229ED9)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF229ED9).copy(alpha = 0.5f))
                        ) {
                            Text("قالب تليجرام جاهز", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Form (Add or Edit)
        if (isAddingAd) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingAdId != null) "تعديل بيانات الإعلان" else "إضافة إعلان ترويجي جديد",
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("عنوان الإعلان الرئيسي", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("تفاصيل ووصف الإعلان", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = targetUrlInput,
                            onValueChange = { targetUrlInput = it },
                            label = { Text("رابط التوجيه (مثال: https://t.me/jdj_q)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = imageUrlInput,
                            onValueChange = { imageUrlInput = it },
                            label = { Text("رابط صورة/بنر الإعلان (اختياري)", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = buttonTextInput,
                                onValueChange = { buttonTextInput = it },
                                label = { Text("نص الزر", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            OutlinedTextField(
                                value = badgeInput,
                                onValueChange = { badgeInput = it },
                                label = { Text("شارة التمييز", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // Type selector
                        Text("نوع الإعلان:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("telegram" to "تيليجرام", "vip" to "اشتراك VIP", "general" to "عام").forEach { (typeKey, typeLabel) ->
                                val isSelected = typeInput == typeKey
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f))
                                        .clickable { typeInput = typeKey }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تفعيل الإعلان ونشره فوراً", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = isEnabledInput,
                                onCheckedChange = { isEnabledInput = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (titleInput.isBlank()) {
                                        Toast.makeText(context, "الرجاء كتابة عنوان الإعلان", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val ad = PromoAd(
                                        id = editingAdId ?: "ad_${System.currentTimeMillis()}",
                                        title = titleInput.trim(),
                                        description = descInput.trim(),
                                        imageUrl = imageUrlInput.trim(),
                                        targetUrl = targetUrlInput.trim(),
                                        buttonText = buttonTextInput.trim().ifEmpty { "انضم الآن" },
                                        badge = badgeInput.trim().ifEmpty { "إعلان مميز" },
                                        type = typeInput,
                                        isEnabled = isEnabledInput,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    viewModel.savePromoAd(ad) {
                                        Toast.makeText(context, "تم حفظ ونشر الإعلان بنجاح!", Toast.LENGTH_SHORT).show()
                                        resetForm()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حفظ ونشر أونلاين", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { resetForm() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("إلغاء")
                            }
                        }
                    }
                }
            }
        }

        // List of existing ads
        item {
            Text("الإعلانات المنشورة حالياً (${promoAds.size}):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        if (promoAds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111526))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد إعلانات منشورة بعد. اضغط على 'إضافة إعلان جديد' للبدء.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(promoAds) { ad ->
                AdminPromoAdItemCard(
                    ad = ad,
                    onToggle = { isEnabled ->
                        viewModel.savePromoAd(ad.copy(isEnabled = isEnabled))
                    },
                    onEdit = {
                        titleInput = ad.title
                        descInput = ad.description
                        imageUrlInput = ad.imageUrl
                        targetUrlInput = ad.targetUrl
                        buttonTextInput = ad.buttonText
                        badgeInput = ad.badge
                        typeInput = ad.type
                        isEnabledInput = ad.isEnabled
                        editingAdId = ad.id
                        isAddingAd = true
                    },
                    onDelete = {
                        viewModel.deletePromoAd(ad.id) {
                            Toast.makeText(context, "تم حذف الإعلان", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminPromoAdItemCard(
    ad: PromoAd,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11162A)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (ad.isEnabled) Color(0xFF229ED9).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (ad.isEnabled) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (ad.isEnabled) "نشط ومفعل" else "معطل",
                            color = if (ad.isEnabled) Color(0xFF00E676) else Color.Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = ad.badge,
                        color = Color(0xFFFFD700),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = ad.isEnabled,
                        onCheckedChange = { onToggle(it) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Text(text = ad.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            if (ad.description.isNotBlank()) {
                Text(text = ad.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            if (ad.targetUrl.isNotBlank()) {
                Text(text = "الرابط: ${ad.targetUrl}", color = Color(0xFF229ED9), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onEdit() }) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete() }) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE50914), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun HomeAdminManager(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homeConfig by viewModel.homeConfig.collectAsState()
    val banners by viewModel.homeHeroBanners.collectAsState()

    var announcementInput by remember { mutableStateOf(homeConfig.announcementText) }
    var isAddingBanner by remember { mutableStateOf(false) }
    var editingBannerId by remember { mutableStateOf<String?>(null) }

    var bannerTitle by remember { mutableStateOf("") }
    var bannerSubtitle by remember { mutableStateOf("") }
    var bannerImageUrl by remember { mutableStateOf("") }
    var bannerTag by remember { mutableStateOf("🔥 حصري 4K") }
    var bannerTargetType by remember { mutableStateOf("tab") }
    var bannerTargetValue by remember { mutableStateOf("channels") }

    LaunchedEffect(homeConfig.announcementText) {
        announcementInput = homeConfig.announcementText
    }

    fun resetBannerForm() {
        bannerTitle = ""
        bannerSubtitle = ""
        bannerImageUrl = ""
        bannerTag = "🔥 حصري 4K"
        bannerTargetType = "tab"
        bannerTargetValue = "channels"
        editingBannerId = null
        isAddingBanner = false
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            com.example.ui.components.HomeLiveOnlineCounterWidget(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11162A)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = Color(0xFFFFD700))
                        }
                        Column {
                            Text("إدارة وتخصيص قسم الرئيسية", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("تحكم شامل في البنرات السينمائية والأقسام التفاعلية للرئيسية", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Announcement Bar Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نص الشريط الترحيبي / الإعلاني المتحرك:", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = announcementInput,
                        onValueChange = { announcementInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Button(
                        onClick = {
                            viewModel.updateHomeConfig(homeConfig.copy(announcementText = announcementInput.trim())) {
                                Toast.makeText(context, "تم تحديث نص الشريط الترحيبي بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("حفظ التغيير", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("التحكم في ظهور عناصر وأقسام الرئيسية:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    AdminSectionToggleRow(
                        title = "المعرض السينمائي التفاعلي (Hero Carousel)",
                        isChecked = homeConfig.showHeroCarousel,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showHeroCarousel = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "بوابة الوصول السريع للأقسام (Quick Navigation)",
                        isChecked = homeConfig.showQuickNav,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showQuickNav = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "قسم الإعلانات والترويج الحصري (Promo Ads)",
                        isChecked = homeConfig.showPromoAds,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showPromoAds = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "أبرز القنوات المباشرة (Trending Channels)",
                        isChecked = homeConfig.showTrendingChannels,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showTrendingChannels = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "أحدث الأفلام المضافة (Latest Movies)",
                        isChecked = homeConfig.showLatestMovies,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showLatestMovies = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "أشهر المسلسلات (Popular Series)",
                        isChecked = homeConfig.showPopularSeries,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showPopularSeries = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "الشريط الإعلاني والترحيب (Announcements)",
                        isChecked = homeConfig.showAnnouncements,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showAnnouncements = it)) }
                    )

                    AdminSectionToggleRow(
                        title = "قسم الأخبار (News Section)",
                        isChecked = homeConfig.showNewsSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showNewsSection = it)) }
                    )
                    AdminSectionToggleRow(
                        title = "زر لوحة المطور (Developer Button)",
                        isChecked = homeConfig.showDevSection,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showDevSection = it)) }
                    )
                    AdminSectionToggleRow(
                        title = "عداد المتواجدين الآن (Online Users)",
                        isChecked = homeConfig.showOnlineUsers,
                        onCheckedChange = { viewModel.updateHomeConfig(homeConfig.copy(showOnlineUsers = it)) }
                    )
                }
            }
        }

        // Hero Banners Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("البنرات السينمائية للرئيسية (${banners.size}):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        resetBannerForm()
                        isAddingBanner = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة بنر", fontSize = 11.sp)
                }
            }
        }

        if (isAddingBanner) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B30)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE50914).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (editingBannerId != null) "تعديل البنر السينمائي" else "إضافة بنر سينمائي جديد",
                            color = Color(0xFFE50914),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = bannerTitle,
                            onValueChange = { bannerTitle = it },
                            label = { Text("عنوان البنر", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE50914),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = bannerSubtitle,
                            onValueChange = { bannerSubtitle = it },
                            label = { Text("الوصف الفرعي للبنر", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE50914),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        OutlinedTextField(
                            value = bannerImageUrl,
                            onValueChange = { bannerImageUrl = it },
                            label = { Text("رابط الصورة الخلفية للبنر", color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE50914),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bannerTag,
                                onValueChange = { bannerTag = it },
                                label = { Text("شارة البنر (مثال: 🔥 حصري 4K)", color = Color.White.copy(alpha = 0.6f)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFE50914),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            // Target Tab Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("القسم الموجه إليه:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("channels" to "بث مباشر", "movies" to "أفلام", "series" to "مسلسلات").forEach { (tabKey, tabName) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (bannerTargetValue == tabKey) Color(0xFFE50914) else Color.White.copy(alpha = 0.1f))
                                                .clickable { bannerTargetValue = tabKey }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(tabName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (bannerTitle.isBlank()) {
                                        Toast.makeText(context, "الرجاء إدخال عنوان للبنر", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val b = HomeHeroBanner(
                                        id = editingBannerId ?: "banner_${System.currentTimeMillis()}",
                                        title = bannerTitle.trim(),
                                        subtitle = bannerSubtitle.trim(),
                                        imageUrl = bannerImageUrl.trim(),
                                        tag = bannerTag.trim().ifEmpty { "🔥 حصري 4K" },
                                        targetType = bannerTargetType,
                                        targetValue = bannerTargetValue,
                                        isEnabled = true
                                    )
                                    viewModel.saveHomeHeroBanner(b) {
                                        Toast.makeText(context, "تم حفظ البنر بنجاح!", Toast.LENGTH_SHORT).show()
                                        resetBannerForm()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حفظ ونشر البنر", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(onClick = { resetBannerForm() }, shape = RoundedCornerShape(8.dp)) {
                                Text("إلغاء", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Banners List
        items(banners) { banner ->
            AdminHeroBannerItemCard(
                banner = banner,
                onEdit = {
                    bannerTitle = banner.title
                    bannerSubtitle = banner.subtitle
                    bannerImageUrl = banner.imageUrl
                    bannerTag = banner.tag
                    bannerTargetType = banner.targetType
                    bannerTargetValue = banner.targetValue
                    editingBannerId = banner.id
                    isAddingBanner = true
                },
                onDelete = {
                    viewModel.deleteHomeHeroBanner(banner.id) {
                        Toast.makeText(context, "تم حذف البنر", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun AdminSectionToggleRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
        )
    }
}

@Composable
fun AdminHeroBannerItemCard(
    banner: HomeHeroBanner,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11162A)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(banner.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=300&auto=format&fit=crop&q=80" })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 60.dp, height = 40.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(text = banner.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = "شارة: ${banner.tag} | التوجيه: ${banner.targetValue}", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
            }
        }
    }
}
