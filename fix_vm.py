with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    text = f.read()

# Fix function declaration
text = text.replace('    fun loadEventChannels()\n            loadNews() {', '    fun loadEventChannels() {')

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(text)
