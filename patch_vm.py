import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    text = f.read()

# Add imports
imports = '''
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.NewsItem
'''
text = text.replace('import com.example.model.IPTVAccount', imports + '\nimport com.example.model.IPTVAccount')

# Add state flow
state_flow = '''
    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()
'''
text = re.sub(r'private val _codeUpdateRequiredMessage', state_flow + '\n    private val _codeUpdateRequiredMessage', text)

# Add fetch function
fetch_func = '''
    fun loadNews() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isNewsLoading.value = true
                val url = URL("https://news-ar.ysscores.com/api/News/all_news_page")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val dataObj = jsonResponse.optJSONObject("data")
                    val dataArray = dataObj?.optJSONArray("data")
                    
                    val parsedNews = mutableListOf<NewsItem>()
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.optJSONObject(i) ?: continue
                            val id = item.optLong("id", 0)
                            val title = item.optString("title", "")
                            val desc = item.optString("news_desc", "")
                            val image = item.optString("image", "")
                            val createdAt = item.optJSONObject("created_at")
                            val dateStr = createdAt?.optString("date", "") ?: ""
                            
                            val fullImageUrl = if (image.isNotBlank()) "https://imgs.ysscores.com/news/$image" else ""
                            
                            parsedNews.add(
                                NewsItem(
                                    id = id,
                                    title = title,
                                    description = desc,
                                    imageUrl = fullImageUrl,
                                    dateStr = dateStr
                                )
                            )
                        }
                    }
                    _newsItems.value = parsedNews
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isNewsLoading.value = false
            }
        }
    }
'''

# Put fetch function near finishContentUpdate
text = text.replace('    fun finishContentUpdate() {', fetch_func + '\n    fun finishContentUpdate() {')

# Call loadNews in init or near content load
text = text.replace('loadEventChannels()', 'loadEventChannels()\n            loadNews()')

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(text)
