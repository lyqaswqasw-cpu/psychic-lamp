with open('app/src/main/java/com/example/data/FirebaseService.kt', 'r') as f:
    text = f.read()

import re

# Read
text = re.sub(r'showOnlineUsers = json\.optBoolean\("showOnlineUsers", true\),', 'showOnlineUsers = json.optBoolean("showOnlineUsers", true),\n                            showNewsSection = json.optBoolean("showNewsSection", true),', text)

# Write
text = re.sub(r'put\("showOnlineUsers", config\.showOnlineUsers\)', 'put("showOnlineUsers", config.showOnlineUsers)\n                put("showNewsSection", config.showNewsSection)', text)

with open('app/src/main/java/com/example/data/FirebaseService.kt', 'w') as f:
    f.write(text)
