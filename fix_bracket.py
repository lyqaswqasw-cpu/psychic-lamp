import re

with open('app/src/main/java/com/example/ui/MainApp.kt', 'r') as f:
    text = f.read()

# Replace the closing brace logic for `if (homeConfig.showDevSection) {`
target = '''                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 180f))
                        }'''
replacement = target + "\n                        }"
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/MainApp.kt', 'w') as f:
    f.write(text)
