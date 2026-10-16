import re

with open('app/src/main/java/com/example/ui/components/LoginScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

pattern2 = r"(// Luxury Code Box with Integrated Paste / Clear buttons.*?)(?=Spacer\(modifier = Modifier\.height\(24\.dp\)\)\n\n\s*// ---------------------------------------------------------\n\s*// RECENT CODES VAULT)"

replacement2 = """// Luxury Code Box with Integrated Paste / Clear buttons
                        val isCodeFocused = remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isCodeFocused.value || inputCode.isNotEmpty()) 1.8.dp else 1.2.dp,
                                    color = if (isCodeFocused.value) LoopRed else if (inputCode.isNotEmpty()) LoopRed.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF04060C)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = if (inputCode.isNotEmpty()) LoopRed else SoftGreyText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "رمز التفعيل (كود الاشتراك)",
                                            color = SoftGreyText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Fast Actions: Paste from Clipboard & Clear
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text?.trim()
                                                if (!clip.isNullOrEmpty()) {
                                                    val filtered = clip.filter { it.isLetterOrDigit() }.uppercase()
                                                    if (filtered.isNotEmpty()) {
                                                        inputCode = filtered
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF161D30),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("لصق", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (inputCode.isNotEmpty()) {
                                            Surface(
                                                onClick = { inputCode = "" },
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF2D1616),
                                                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = LoopRed, modifier = Modifier.size(12.dp))
                                                    Text("مسح", color = LoopRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputCode,
                                    onValueChange = { newVal ->
                                        val sanitized = newVal.filter { it.isLetterOrDigit() }.uppercase()
                                        inputCode = sanitized
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 6.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (inputCode.trim().isNotEmpty()) {
                                                viewModel.loginWithCode(inputCode.trim())
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("activation_code_input")
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown && 
                                                (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.NumPadEnter)) {
                                                if (inputCode.trim().isNotEmpty()) {
                                                    viewModel.loginWithCode(inputCode.trim())
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF090D1A), RoundedCornerShape(12.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                                .padding(vertical = 18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (inputCode.isEmpty()) {
                                                Text(
                                                    text = "أدخل الكود هنا...",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = SoftGreyText.copy(alpha = 0.4f),
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                        
                        """

new_content, count2 = re.subn(pattern2, replacement2, content, flags=re.DOTALL)

print(f"Replaced {count2} occurrences in Code form.")

with open('app/src/main/java/com/example/ui/components/LoginScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)
