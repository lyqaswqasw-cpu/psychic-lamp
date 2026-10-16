with open('app/src/main/java/com/example/model/IPTVModels.kt', 'r') as f:
    text = f.read()

import re

# Add showNewsSection to HomeSectionConfig
text = re.sub(r'val showOnlineUsers: Boolean = true,', 'val showOnlineUsers: Boolean = true,\n    val showNewsSection: Boolean = true,', text)

# Add NewsItem models
news_models = '''
data class NewsItem(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val dateStr: String = ""
)
'''

text = text + "\n" + news_models

with open('app/src/main/java/com/example/model/IPTVModels.kt', 'w') as f:
    f.write(text)
