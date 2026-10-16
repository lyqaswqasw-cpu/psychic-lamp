import re

with open('app/src/main/java/com/example/ui/components/ViewerCount.kt', 'r') as f:
    content = f.read()

# Replace the ViewerCountOverlay in ViewerCount.kt to use an odometer style
target = """@Composable
fun ViewerCountOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val viewerCount by viewModel.currentViewerCount.collectAsState()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF0F172A).copy(alpha = 0.75f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Viewers",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$viewerCount",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}"""

replacement = """@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ViewerCountOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
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
                imageVector = Icons.Default.Visibility,
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

if target in content:
    with open('app/src/main/java/com/example/ui/components/ViewerCount.kt', 'w') as f:
        f.write(content.replace(target, replacement))
    print("Replaced ViewerCountOverlay successfully")
else:
    print("Could not find ViewerCountOverlay target")

