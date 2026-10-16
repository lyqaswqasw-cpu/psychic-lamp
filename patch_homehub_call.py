import re

with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'r') as f:
    text = f.read()

# Make sure to import Article, Search, Clear, Schedule if not imported
imports = [
    "import androidx.compose.material.icons.filled.Article",
    "import androidx.compose.material.icons.filled.Search",
    "import androidx.compose.material.icons.filled.Clear",
    "import androidx.compose.material.icons.filled.Schedule"
]
for imp in imports:
    if imp not in text:
        text = text.replace("import androidx.compose.material.icons.filled.Refresh", imp + "\nimport androidx.compose.material.icons.filled.Refresh")

# Find a good place to insert the News section (maybe before or after Popular Movies)
insert_call = '''
        if (homeConfig.showNewsSection) {
            item {
                HomeNewsSection(viewModel = viewModel)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
'''

if "HomeNewsSection(viewModel = viewModel)" not in text.replace("fun HomeNewsSection", ""):
    text = re.sub(r'(if \(homeConfig\.showPopularMovies\)\s*\{\s*item \{\s*HomePopularMoviesSection.*?\s*\}\s*\})', r'\1' + insert_call, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'w') as f:
    f.write(text)
