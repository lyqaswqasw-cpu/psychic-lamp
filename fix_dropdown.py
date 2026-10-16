with open('app/src/main/java/com/example/ui/components/DeveloperSettingsScreen.kt', 'r') as f:
    text = f.read()

import re
dropdown_pattern = re.compile(r'var channelDropdownExpanded by remember.*?// Allow typing a direct stream url fallback in events\? No, they asked to pick a channel\.\s*\}\s*\}', re.DOTALL)
match = dropdown_pattern.search(text)
if match:
    print("Found dropdown!")
else:
    print("Not found")
