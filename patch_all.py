import re

with open('app/src/main/java/com/example/ui/components/ViewerCount.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun ViewerCountOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    if (!homeConfig.showOnlineUsers) return

    val viewerCount by viewModel.currentViewerCount.collectAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Viewers",
            tint = Color.Red,
            modifier = Modifier.size(16.dp).padding(end = 4.dp)
        )
        Text(
            text = "$viewerCount",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}"""

replacement = """@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ViewerCountOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    if (!homeConfig.showOnlineUsers) return

    val viewerCount by viewModel.currentViewerCount.collectAsState()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF0D111A).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color(0xFFE21221).copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Live dot pulsing
            val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE21221).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Viewers",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            
            // Odometer style counter
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                val countString = viewerCount.toString()
                for (char in countString) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            slideInVertically(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) { height -> height } + fadeIn() togetherWith
                            slideOutVertically(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) { height -> -height } + fadeOut()
                        },
                        label = "digit_flip"
                    ) { targetDigit ->
                        Text(
                            text = targetDigit.toString(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}"""

# Fix indentation and replace
content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/components/ViewerCount.kt', 'w') as f:
    f.write(content)
print("Updated ViewerCount.kt")

# Now update FirebaseService.kt for the video player viewer count
with open('app/src/main/java/com/example/data/FirebaseService.kt', 'r') as f:
    fs_content = f.read()

fs_target = """                                    val count = json.length() + (1..3).random()"""
fs_replacement = """                                    // Generate a massively realistic crowd based on real connections
                                    val realCount = json.length()
                                    // Base crowd of 12,500 + 400 per actual user + random fluctuation
                                    val count = 12500 + (realCount * 450) + (10..150).random()"""
if fs_target in fs_content:
    with open('app/src/main/java/com/example/data/FirebaseService.kt', 'w') as f:
        f.write(fs_content.replace(fs_target, fs_replacement))
    print("Updated FirebaseService.kt")
else:
    print("Could not find fs_target")


# Update MainViewModel.kt for the global online counter
with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    mvm_content = f.read()

mvm_target = """                    val count = PresenceService.getLiveOnlineCount()
                    _liveOnlineUsersCount.value = count"""
mvm_replacement = """                    val realCount = PresenceService.getLiveOnlineCount()
                    // Create a large app-wide realistic presence number (e.g. 24,000+)
                    val inflatedCount = 24300 + (realCount * 320) + (1..250).random()
                    _liveOnlineUsersCount.value = inflatedCount"""

if mvm_target in mvm_content:
    with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
        f.write(mvm_content.replace(mvm_target, mvm_replacement))
    print("Updated MainViewModel.kt")
else:
    print("Could not find mvm_target")

