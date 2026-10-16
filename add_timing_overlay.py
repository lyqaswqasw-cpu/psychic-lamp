import re

with open('app/src/main/java/com/example/ui/components/EventsGridView.kt', 'r') as f:
    content = f.read()

target = """                        AsyncImage(
                            model = match.imageUrl.trim(),
                            contentDescription = match.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Footer Bar for Custom Banner (no extra padding around)"""

replacement = """                        AsyncImage(
                            model = match.imageUrl.trim(),
                            contentDescription = match.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Overlay Timing & Status Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Status Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLiveNow) LoopRed.copy(alpha = 0.95f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, if (isLiveNow) LoopRed else Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isLiveNow) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = GoldGlow,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = remainingTimeText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Match Start Time (e.g. 09:00 PM)
                            if (matchTimeFormatted.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, GoldGlow.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "⏰ $matchTimeFormatted",
                                        color = GoldGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Footer Bar for Custom Banner (no extra padding around)"""

if target in content:
    with open('app/src/main/java/com/example/ui/components/EventsGridView.kt', 'w') as f:
        f.write(content.replace(target, replacement))
    print("Overlay added successfully!")
else:
    print("Could not find target!")

