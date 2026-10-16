with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import com.example.ui.theme.GoldGlow\npackage com.example.ui', 'package com.example.ui\nimport com.example.ui.theme.GoldGlow')

with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'w') as f:
    f.write(text)
