with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'r') as f:
    text = f.read()

import re

# Insert the News Section UI at the end of the file
news_ui = '''
@Composable
fun HomeNewsSection(viewModel: MainViewModel) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val filteredNews = if (searchQuery.isBlank()) {
        newsItems
    } else {
        newsItems.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionHeaderTitle(
            title = "أحدث أخبار كرة القدم",
            subtitle = "تغطية شاملة ومستمرة لكل ما يخص عالم الساحرة المستديرة",
            icon = Icons.Default.Article
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Professional Search Bar
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(),
            placeholder = { Text("ابحث في الأخبار...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = GoldGlow) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldGlow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = GoldGlow,
                focusedContainerColor = Color(0xFF161B22),
                unfocusedContainerColor = Color(0xFF161B22)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isNewsLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldGlow)
            }
        } else if (filteredNews.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد أخبار متاحة حالياً.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredNews, key = { it.id }) { news ->
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .tvFocusable(shape = RoundedCornerShape(16.dp))
                            .clickable { /* Could show full article dialog */ },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2130)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column {
                            AsyncImage(
                                model = news.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = news.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = news.description,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = news.dateStr.take(10), // only YYYY-MM-DD
                                        color = GoldGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
'''
if "fun HomeNewsSection" not in text:
    text = text + "\n" + news_ui

with open('app/src/main/java/com/example/ui/HomeHubScreen.kt', 'w') as f:
    f.write(text)
