package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.*
import com.omaster.app.model.UserProfile
import com.omaster.app.model.Work
import com.omaster.app.ui.theme.*

// ============================================
// 个人中心 - 用户粘性核心
// ============================================

@Composable
fun ProfileScreen(
    onMyWorksClick: () -> Unit,
    onMyPresetsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile = remember { getMockUserProfile() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // 头部信息
        ProfileHeader(userProfile)
        
        // 数据统计
        ProfileStats(userProfile)
        
        // 菜单列表
        ProfileMenu(
            onMyWorksClick = onMyWorksClick,
            onMyPresetsClick = onMyPresetsClick,
            onSettingsClick = onSettingsClick
        )
        
        // 成就区域
        AchievementSection()
    }
}

@Composable
fun ProfileHeader(profile: UserProfile) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = LightFieldPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = profile.name.first().toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = LightFieldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall,
                color = LightFieldOnSurfaceDark,
                fontWeight = FontWeight.Bold
            )
            
            if (profile.isVerified) {
                Spacer(modifier = Modifier.width(8.dp)
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "认证",
                    tint = OppoGreen,
                    modifier = Modifier.size(20.dp)
            }
        }
        
        if (profile.isHncsCreator) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(100),
                color = HasselbladOrange.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "哈苏认证创作者",
                    style = MaterialTheme.typography.labelMedium,
                    color = HasselbladOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        if (profile.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = LightFieldOnSurfaceVariantDark,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileStats(profile: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassMediumDark)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("作品", profile.worksCount)
            StatItem("粉丝", profile.followers)
            StatItem("预设", profile.presetsCount)
            StatItem("获赞", 12847)
        }
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = LightFieldPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LightFieldOnSurfaceVariantDark
        )
    }
}

@Composable
fun ProfileMenu(
    onMyWorksClick: () -> Unit,
    onMyPresetsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        MenuItem(
            icon = Icons.Default.Collections,
            title = "我的作品",
            subtitle = "查看你发布的所有作品",
            onClick = onMyWorksClick
        )
        
        MenuItem(
            icon = Icons.Default.Palette,
            title = "我的预设",
            subtitle = "管理你收藏和创作的预设",
            onClick = onMyPresetsClick
        )
        
        MenuItem(
            icon = Icons.Default.Star,
            title = "我的收藏",
            subtitle = "你收藏的预设和作品",
            onClick = { }
        )
        
        MenuItem(
            icon = Icons.Default.Settings,
            title = "设置",
            subtitle = "个性化你的OMaster",
            onClick = onSettingsClick
        )
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GlassMediumDark,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = LightFieldPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = LightFieldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LightFieldOnSurfaceDark,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LightFieldOnSurfaceVariantDark
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "更多",
                tint = LightFieldOnSurfaceVariantDark
            )
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun AchievementSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "成就",
            style = MaterialTheme.typography.titleLarge,
            color = LightFieldOnSurfaceDark,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassMediumDark
        )
    ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AchievementBadge(
                    icon = Icons.Default.Lightbulb,
                    title = "新手创作者",
                    description = "发布你的第一个作品",
                    isEarned = true
                )
                
                AchievementBadge(
                    icon = Icons.Default.EmojiEvents,
                    title = "哈苏大师",
                    description = "使用HNCS预设100次",
                    isEarned = false
                )
            }
        }
    }
}

@Composable
fun AchievementBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isEarned: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (isEarned) LightFieldPrimary.copy(alpha = 0.2f) 
            else LightFieldOnSurfaceVariantDark.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isEarned) LightFieldPrimary else LightFieldOnSurfaceVariantDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = LightFieldOnSurfaceDark,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = LightFieldOnSurfaceVariantDark
        )
    }
}

private fun getMockUserProfile(): UserProfile = UserProfile(
    id = "me",
    name = "摄影爱好者",
    bio = "用Find X用户 · 哈苏色彩探索者",
    followers = 1280,
    following = 256,
    worksCount = 48,
    presetsCount = 6,
    isVerified = false,
    isHncsCreator = false
)
