import re

with open("app/src/main/java/com/example/ui/components/YouTubeScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
if "import androidx.compose.ui.window.DialogWindowProvider" not in content:
    content = content.replace("import androidx.compose.ui.window.DialogProperties", "import androidx.compose.ui.window.DialogProperties\nimport androidx.compose.ui.window.DialogWindowProvider\nimport androidx.compose.ui.platform.LocalView")

# Remove setLayerType
content = content.replace("                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)\n", "")

# Update YouTubeVideoJsFullscreenPlayer definition to use Dialog
old_player_start = """@Composable
fun YouTubeVideoJsFullscreenPlayer(
    video: YouTubeVideoItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()"""

new_player_start = """@Composable
fun YouTubeVideoJsFullscreenPlayer(
    video: YouTubeVideoItem,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val currentView = LocalView.current
        val dialogWindow = remember(currentView) {
            (currentView.parent as? DialogWindowProvider)?.window ?: activity?.window
        }"""

content = content.replace(old_player_start, new_player_start)

# Update DisposableEffect & LaunchedEffect inside YouTubeVideoJsFullscreenPlayer
old_disposable = """    // Enforce Immediate Landscape & Strict Immersive Mode when entering
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Continuously keep system bars hidden
    LaunchedEffect(showControls) {
        activity?.window?.let { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }"""

new_disposable = """    // Enforce Immediate Landscape & Strict Immersive Mode on both Dialog & Activity Windows
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val windows = listOfNotNull(dialogWindow, activity?.window)
        windows.forEach { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                window.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            windows.forEach { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Continuously keep system bars hidden
    LaunchedEffect(showControls) {
        val windows = listOfNotNull(dialogWindow, activity?.window)
        windows.forEach { window ->
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }"""

content = content.replace(old_disposable, new_disposable)

# Close the Dialog block at the end of YouTubeVideoJsFullscreenPlayer (before LegendaryTopButton)
target_dialog_end = """            // 9. Quality Selection Dialog
            if (showQualityDialog) {"""

replacement_dialog_end = """            // 9. Quality Selection Dialog
            if (showQualityDialog) {"""

# We need to close the `Dialog` lambda block right before `LegendaryTopButton`
top_btn_str = "@Composable\nfun LegendaryTopButton"
parts = content.split(top_btn_str)
if len(parts) == 2:
    content = parts[0].rstrip() + "\n    }\n}\n\n" + top_btn_str + parts[1]

with open("app/src/main/java/com/example/ui/components/YouTubeScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("Updated YouTubeScreen successfully!")
