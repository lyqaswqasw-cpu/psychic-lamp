with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    text = f.read()

imports = '''
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.NewsItem
'''

text = text.replace('import android.app.Application', imports + 'import android.app.Application')

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(text)
