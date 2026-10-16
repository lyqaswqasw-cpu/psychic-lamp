import re
with open('app/src/main/java/com/example/ui/MainApp.kt', 'r') as f:
    text = f.read()

text = text.replace('fun SettingsPanel(viewModel: MainViewModel) {', 'fun SettingsPanel(viewModel: MainViewModel) {\n    val homeConfig by viewModel.homeConfig.collectAsState()')

with open('app/src/main/java/com/example/ui/MainApp.kt', 'w') as f:
    f.write(text)
