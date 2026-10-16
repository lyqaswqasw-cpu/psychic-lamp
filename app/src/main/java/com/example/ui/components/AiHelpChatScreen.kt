package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.theme.LoopRed
import com.example.ui.theme.SoftGreyText
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun AiHelpChatScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("مرحباً بك! أنا المساعد الذكي لتطبيق Loop Live. كيف يمكنني مساعدتك اليوم؟" to false)) }
    var isThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LoopRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = LoopRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("المساعد الذكي Loop AI", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("مساعدك الفوري لحل أي مشكلة", color = SoftGreyText, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.08f))

                // Chat Messages
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { (text, isUser) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isUser) LoopRed else MaterialTheme.colorScheme.background)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = text,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    if (isThinking) {
                        item {
                            Text("جاري الكتابة...", color = SoftGreyText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اكتب سؤالك هنا...", color = SoftGreyText, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoopRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val userText = query.trim()
                            if (userText.isNotEmpty()) {
                                query = ""
                                messages = messages + (userText to true)
                                isThinking = true
                                coroutineScope.launch {
                                    delay(1000)
                                    val reply = if (userText.contains("كود") || userText.contains("دخول")) {
                                        "إذا واجهت مشكلة في الكود، تأكد من إدخاله بدقة أو اطلب كوداً جديداً من المطور عبر قسم الدعم."
                                    } else if (userText.contains("قنوات") || userText.contains("بث")) {
                                        "يمكنك التبديل بين مشغلات البث عبر زر الإعدادات واختيار النمط المباشر لتشغيل سريع وسلس."
                                    } else {
                                        "شكراً لتواصلك! يمكنك تصفح القنوات والأفلام بكل سهولة، أو مراجعة قسم الإعدادات لتخصيص تجربتك."
                                    }
                                    messages = messages + (reply to false)
                                    isThinking = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LoopRed)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
