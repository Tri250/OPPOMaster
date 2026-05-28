package com.omaster.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "发现",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AccentPrimary
        )

        ColorWalkCard()
        
        Divider()
        
        PhotoFrameSection()
        
        Divider()
        
        ShootingRecordSection()
        
        Divider()
        
        CommunityHighlights()
    }
}

@Composable
fun ColorWalkCard() {
    var isAnimating by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var currentPreset by remember { mutableStateOf<String>("") }
    val colors = listOf(
        "哈苏橙调", "春日樱粉", "海天一色", "复古胶片",
        "暗夜极光", "金色日落", "森林翠绿", "城市霓虹"
    )
    val colorValues = listOf(
        HasselbladOrange, Color(0xFFFFB6C1), Color(0xFF87CEEB),
        Color(0xFF8B4513), Color(0xFF191970), Color(0xFFFFD700),
        Color(0xFF228B22), Color(0xFFFF1493)
    )
    var selectedIndex by remember { mutableStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(colorValues[selectedIndex], colorValues[(selectedIndex + 1) % colorValues.size])
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Color Walk",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "今日推荐: $currentPreset",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        if (!isAnimating) {
                            isAnimating = true
                            val targetIndex = colors.indices.random()
                            
                            kotlinx.coroutines.GlobalScope.launch {
                                for (i in 0..20) {
                                    selectedIndex = (selectedIndex + 1) % colors.size
                                    delay(50 + i * 20L)
                                }
                                selectedIndex = targetIndex
                                currentPreset = colors[targetIndex]
                                isAnimating = false
                                showResult = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = colorValues[selectedIndex]
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "抽卡"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAnimating) "抽选中..." else "抽取今日配色",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                if (showResult) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "获得「${colors[selectedIndex]}」预设！",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = colorValues[selectedIndex],
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoFrameSection() {
    Column {
        Text(
            text = "画框导出",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FrameOption(
                icon = Icons.Default.Image,
                title = "哈苏风格",
                description = "经典红标边框",
                color = HasselbladOrange
            )
            
            FrameOption(
                icon = Icons.Default.PhotoAlbum,
                title = "胶片质感",
                description = "复古胶片边框",
                color = Color(0xFF8B4513)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FrameOption(
                icon = Icons.Default.Brush,
                title = "极简风格",
                description = "简约白边框",
                color = Color(0xFF607D8B)
            )
            
            FrameOption(
                icon = Icons.Default.Palette,
                title = "自定义",
                description = "自由设计边框",
                color = AccentPrimary
            )
        }
    }
}

@Composable
fun FrameOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(150.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.padding(12.dp),
                    tint = color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShootingRecordSection() {
    Column {
        Text(
            text = "拍摄记录",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM月dd日")
        
        ShootingRecordCard(
            date = today.format(formatter),
            count = 3,
            streak = 7,
            presetsUsed = listOf("哈苏橙调", "复古胶片", "春日樱粉")
        )
    }
}

@Composable
fun ShootingRecordCard(
    date: String,
    count: Int,
    streak: Int,
    presetsUsed: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = AccentPrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "连续",
                            tint = AccentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${streak}天",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "今日拍摄",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column {
                    Text(
                        text = presetsUsed.size.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = HasselbladOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "使用预设",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetsUsed.forEach { preset ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = preset,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* 打卡功能 */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "打卡"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "今日打卡",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CommunityHighlights() {
    Column {
        Text(
            text = "社区精选",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        HighlightCard(
            title = "胶片摄影入门技巧",
            author = "摄影大师",
            likes = 1234
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HighlightCard(
            title = "Find X8 摄影指南",
            author = "OPPO官方",
            likes = 856
        )
    }
}

@Composable
fun HighlightCard(
    title: String,
    author: String,
    likes: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Article,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = AccentPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "点赞",
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = likes.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
