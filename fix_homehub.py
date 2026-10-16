with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'r') as f:
    text = f.read()

import re
# Remove the online users widget from HomeHubScreen
pattern = re.compile(r'// 3\. Dedicated Real-Time Online Users Counter Widget \([^)]+\)\s*if \(homeConfig\.showOnlineUsers\) \{\s*item \{\s*HomeLiveOnlineCounterWidget\(viewModel = viewModel\)\s*\}\s*\}', re.MULTILINE)
text = pattern.sub('', text)

with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'w') as f:
    f.write(text)
