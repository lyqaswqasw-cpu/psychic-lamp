with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'r') as f:
    text = f.read()

import re

replacement = '''    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            com.example.ui.HomeLiveOnlineCounterWidget(viewModel = viewModel)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Header'''

text = re.sub(r'    LazyColumn\(\s*modifier = modifier\s*\.fillMaxSize\(\)\s*\.padding\(16\.dp\),\s*verticalArrangement = Arrangement\.spacedBy\(14\.dp\)\s*\)\s*\{\s*// Header', replacement, text)

with open('app/src/main/java/com/example/ui/AdminHomeAndAdsScreens.kt', 'w') as f:
    f.write(text)
