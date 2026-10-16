import re
with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'r') as f:
    text = f.read()

insert_call = '''
        if (homeConfig.showNewsSection) {
            item {
                HomeNewsSection(viewModel = viewModel)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
'''

if "HomeNewsSection(viewModel = viewModel)" not in text.replace("fun HomeNewsSection", ""):
    text = re.sub(r'(if \(homeConfig\.showLatestMovies && showMovies && movies\.isNotEmpty\(\)\) \{\s*item \{\s*HomeMoviesSection.*?\s*\}\s*\})', insert_call + r'\1', text, flags=re.DOTALL)
    
with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'w') as f:
    f.write(text)
