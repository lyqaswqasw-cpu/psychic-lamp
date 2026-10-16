import re

with open('app/src/main/java/com/example/ui/components/EventsGridView.kt', 'r') as f:
    content = f.read()

start_match = re.search(r'    Card\(\n\s+modifier = Modifier\n\s+\.fillMaxWidth\(\)\n\s+\.tvFocusable\(shape = RoundedCornerShape\(16\.dp\), onClick = onPlay\)', content)
end_str = "}\n\n/**\n * Event Channel Card without the \"مباشر\" badge"
end_match = content.find(end_str)

if start_match and end_match != -1:
    old_card = content[start_match.start():end_match]
    
    new_card = """    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onPlay)
            .clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldGlow.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f), LoopRed.copy(alpha = 0.35f)))),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasCustomBanner) {
                // Completely custom layout for custom banner (fills entirely, no top badges)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(21f / 9f)
                            .background(Color(0xFF0D111A))
                    ) {
                        AsyncImage(
                            model = match.imageUrl.trim(),
                            contentDescription = match.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Footer Bar for Custom Banner (no extra padding around)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D111A))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = GoldGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (targetChannelName.isNotBlank()) "القناة: $targetChannelName" else "بث مباشر عبر السيرفر",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldGlow,
                            modifier = Modifier.clickable { onPlay() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مشاهدة الآن",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Background Stadium Glow Gradient
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A2232),
                                    Color(0xFF0F131C)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header Bar (Match Timing & Status)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLiveNow) LoopRed.copy(alpha = 0.95f) else Color(0xFF1E293B),
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
                                color = GoldGlow.copy(alpha = 0.15f),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (team2.isNotBlank()) {
                        // Teams VS Arena Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team 1 (Right in RTL)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(54.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.5.dp, GoldGlow.copy(alpha = 0.6f)),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(6.dp)
                                    ) {
                                        if (!team1LogoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = team1LogoUrl,
                                                contentDescription = team1,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.SportsSoccer,
                                                contentDescription = team1,
                                                tint = GoldGlow,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = team1,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // VS Center Badge
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LoopRed.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "VS",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Team 2 (Left in RTL)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(54.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(6.dp)
                                    ) {
                                        if (!team2LogoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = team2LogoUrl,
                                                contentDescription = team2,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.SportsSoccer,
                                                contentDescription = team2,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = team2,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Single Match Banner / Poster Title
                        if (match.imageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0D111A))
                            ) {
                                AsyncImage(
                                    model = match.imageUrl.trim(),
                                    contentDescription = match.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF0F131C).copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )
                                Text(
                                    text = match.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = null,
                                    tint = GoldGlow,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = match.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Footer Bar: Broadcasting Channel & Watch Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D111A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = GoldGlow,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (targetChannelName.isNotBlank()) "القناة: $targetChannelName" else "بث مباشر عبر السيرفر",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Watch Match Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldGlow,
                            modifier = Modifier.clickable { onPlay() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مشاهدة الآن",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }"""
    
    with open('app/src/main/java/com/example/ui/components/EventsGridView.kt', 'w') as f:
        f.write(content.replace(old_card, new_card))
    print("Replaced successfully!")
else:
    print("Could not find start or end")

