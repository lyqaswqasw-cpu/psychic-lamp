                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تحليل وتعبئة البيانات فوراً", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ---------------------------------------------------------
                        // STEP 1: HOST URL & PROTOCOL SELECTOR
                        // ---------------------------------------------------------
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1. عنوان السيرفر والمنفذ (Host & Port)",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Protocol Helpers: http:// and https://
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ProtocolChip(
                                        label = "http://",
                                        onClick = {
                                            var clean = hostInput.removePrefix("https://").removePrefix("http://")
                                            hostInput = "http://$clean"
                                        }
                                    )
                                    ProtocolChip(
                                        label = "https://",
                                        onClick = {
                                            var clean = hostInput.removePrefix("https://").removePrefix("http://")
                                            hostInput = "https://$clean"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = hostInput,
                                onValueChange = { hostInput = it },
                                placeholder = {
                                    Text(
                                        text = "مثال: http://iptv-server.com:8080",
                                        fontSize = 12.sp,
                                        color = SoftGreyText.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    Row {
                                        if (hostInput.isNotEmpty()) {
                                            IconButton(onClick = { hostInput = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = SoftGreyText, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text?.trim()
                                                if (!clip.isNullOrEmpty()) {
                                                    hostInput = clip
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedContainerColor = Color(0xFF04060E),
                                    unfocusedContainerColor = Color(0xFF04060E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ---------------------------------------------------------
                        // STEP 2: USERNAME & PASSWORD CREDENTIALS
                        // ---------------------------------------------------------
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "2. بيانات الاعتماد (الحساب وكلمة المرور)",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Username
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                placeholder = {
                                    Text(
                                        text = "اسم المستخدم (Username)...",
                                        fontSize = 12.sp,
                                        color = SoftGreyText.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (usernameInput.isNotEmpty()) {
                                        IconButton(onClick = { usernameInput = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = SoftGreyText, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedContainerColor = Color(0xFF04060E),
                                    unfocusedContainerColor = Color(0xFF04060E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Password
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                placeholder = {
                                    Text(
                                        text = "كلمة المرور (Password)...",
                                        fontSize = 12.sp,
                                        color = SoftGreyText.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                            tint = if (passwordVisible) Color(0xFF38BDF8) else SoftGreyText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.5.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                    focusedContainerColor = Color(0xFF04060E),
                                    unfocusedContainerColor = Color(0xFF04060E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ---------------------------------------------------------
                        // STEP 3: STREAM FORMAT & ENGINE
