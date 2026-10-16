with open('app/src/main/java/com/example/ui/components/DeveloperSettingsScreen.kt', 'r') as f:
    text = f.read()

import re

# Match the old Box containing OutlinedTextField and DropdownMenu
pattern = re.compile(r'Box \{\s*OutlinedTextField\([\s\S]*?DropdownMenu\([\s\S]*?\}\s*\}\s*\}', re.MULTILINE)

replacement = '''var channelDialogExpanded by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { channelDialogExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.5f))
                        ) {
                            Text(selectedTargetChannel?.name ?: "اختر القناة...", color = if (selectedTargetChannel != null) Color.White else Color.Gray)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = GoldGlow)
                        }

                        if (channelDialogExpanded) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { channelDialogExpanded = false },
                                containerColor = Color(0xFF1E2430),
                                title = { Text("اختر القناة الناقلة", color = Color.White) },
                                text = {
                                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                        items(allChannels, key = { it.id }) { ch ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        inputTargetChannelId = ch.id
                                                        channelDialogExpanded = false
                                                    },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(ch.name, color = Color.White, modifier = Modifier.padding(12.dp))
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(onClick = { channelDialogExpanded = false }) {
                                        Text("إلغاء", color = GoldGlow)
                                    }
                                }
                            )
                        }'''

text = pattern.sub(replacement, text)

with open('app/src/main/java/com/example/ui/components/DeveloperSettingsScreen.kt', 'w') as f:
    f.write(text)
