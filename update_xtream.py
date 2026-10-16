import re

with open('app/src/main/java/com/example/ui/components/LoginScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I will replace the STEP 1 and STEP 2 sections with a unified beautiful design.
# It starts around "// ---------------------------------------------------------\n                        // STEP 1: HOST URL & PROTOCOL SELECTOR"
# And ends before "// ---------------------------------------------------------\n                        // STEP 3: STREAM FORMAT & ENGINE"

pattern = r"(// ---------------------------------------------------------\n\s*// STEP 1: HOST URL & PROTOCOL SELECTOR.*?)(?=// ---------------------------------------------------------\n\s*// STEP 3: STREAM FORMAT & ENGINE)"

replacement = """// ---------------------------------------------------------
                        // UNIFIED XTREAM CREDENTIALS CARD
                        // ---------------------------------------------------------
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF04060E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 1. HOST INPUT
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = hostInput,
                                        onValueChange = { hostInput = it },
                                        placeholder = { Text("رابط السيرفر (Host URL)...", fontSize = 12.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Protocol chips or paste btn
                                    if (hostInput.isEmpty()) {
                                        IconButton(onClick = { 
                                            val clip = clipboardManager.getText()?.text?.trim()
                                            if (!clip.isNullOrEmpty()) hostInput = clip
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(onClick = { hostInput = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = SoftGreyText, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                                // 2. USERNAME INPUT
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = usernameInput,
                                        onValueChange = { usernameInput = it },
                                        placeholder = { Text("اسم المستخدم (Username)...", fontSize = 12.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                                // 3. PASSWORD INPUT
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        placeholder = { Text("كلمة المرور (Password)...", fontSize = 12.sp, color = SoftGreyText.copy(alpha = 0.5f)) },
                                        singleLine = true,
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                if (hostInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                                    viewModel.loginWithXtreamDirect(hostInput.trim(), usernameInput.trim(), passwordInput.trim(), selectedFormat)
                                                }
                                            }
                                        ),
                                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (passwordVisible) Color(0xFF38BDF8) else SoftGreyText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        """

new_content, count = re.subn(pattern, replacement, content, flags=re.DOTALL)

print(f"Replaced {count} occurrences in Xtream form.")

# Now for the Activation Code form
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
                                )
                                .shadow(if (isCodeFocused.value || inputCode.isNotEmpty()) 12.dp else 0.dp, RoundedCornerShape(20.dp), spotColor = LoopRed.copy(alpha = 0.4f)),
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
                                                        Toast.makeText(context, "تم لصق كود التفعيل: $filtered", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "الحافظة فارغة!", Toast.LENGTH_SHORT).show()
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

                                BasicTextField(
                                    value = inputCode,
                                    onValueChange = { newVal ->
                                        val sanitized = newVal.filter { it.isLetterOrDigit() }.uppercase()
                                        inputCode = sanitized
                                    },
                                    textStyle = TextStyle(
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
                                            } else {
                                                Toast.makeText(context, "الرجاء إدخال كود تفعيل صالح!", Toast.LENGTH_SHORT).show()
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

new_content, count2 = re.subn(pattern2, replacement2, new_content, flags=re.DOTALL)

print(f"Replaced {count2} occurrences in Code form.")

with open('app/src/main/java/com/example/ui/components/LoginScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)
