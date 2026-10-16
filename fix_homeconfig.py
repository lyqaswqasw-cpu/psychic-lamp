import re

with open('app/src/main/java/com/example/ui/MainApp.kt', 'r') as f:
    text = f.read()

text = text.replace('    val isContentUpdating by viewModel.isContentUpdating.collectAsState()', '    val isContentUpdating by viewModel.isContentUpdating.collectAsState()\n    val homeConfig by viewModel.homeConfig.collectAsState()')

with open('app/src/main/java/com/example/ui/MainApp.kt', 'w') as f:
    f.write(text)
