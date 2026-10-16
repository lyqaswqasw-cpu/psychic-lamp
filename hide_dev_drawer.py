import re

with open('app/src/main/java/com/example/ui/MainApp.kt', 'r') as f:
    text = f.read()

# We need to wrap the developer mode Row in `if (homeConfig.showDevSection) { ... }`
# Wait, `homeConfig` is already a variable in `MainApp.kt`? Let's check.
# Yes, `val homeConfig by viewModel.homeConfig.collectAsState()` is likely there.

pattern = re.compile(r'(Row\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.clickable \{\s*if \(isDeveloperAuthenticated\))', re.MULTILINE)
text = pattern.sub(r'if (homeConfig.showDevSection) {\n                                \1', text)

# Now we need to close the brace after that section. It's followed by a Divider usually.
# Let's find the text right after the developer mode section.
after_pattern = r'                                    Text\(text = "إدارة الأقسام ونشر قنوات بث أونلاين على السيرفر", color = SoftGreyText, fontSize = 10\.sp\)\n                                \}\n                            \}\n                            Icon\(Icons\.Default\.ChevronLeft, contentDescription = null, tint = SoftGreyText\)\n                        \}\n                        Spacer\(modifier = Modifier\.height\(8\.dp\)\)\n                        HorizontalDivider\(color = Color\.White\.copy\(alpha = 0\.05f\)\)'
replacement = r'                                    Text(text = "إدارة الأقسام ونشر قنوات بث أونلاين على السيرفر", color = SoftGreyText, fontSize = 10.sp)\n                                }\n                            }\n                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = SoftGreyText)\n                        }\n                            }\n                        Spacer(modifier = Modifier.height(8.dp))\n                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))'

text = re.sub(after_pattern, replacement, text, flags=re.MULTILINE)

with open('app/src/main/java/com/example/ui/MainApp.kt', 'w') as f:
    f.write(text)
