package com.omaster.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaster.app.data.CameraConfigRepository
import com.omaster.app.ui.theme.Colors
import com.omaster.app.ui.theme.Spacing
import com.omaster.app.ui.theme.Typography
import com.omaster.app.ui.theme.hasselbladOrange
import dagger.hilt.android.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 用户个人资料页面 - ColorOS 16 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,
    onCameraConfigClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val systemStats by viewModel.systemStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // 用户信息卡片
            ProfileHeaderCard(
                userProfile = userProfile,
                onEditProfile = { /* 编辑个人信息 */ }
            )

            // 统计数据卡片
            StatsCard(
                stats = systemStats,
                modifier = Modifier.fillMaxWidth()
            )

            // 功能菜单列表
            ProfileMenuSection(
                onSettingsClick = onSettingsClick,
                onCameraConfigClick = onCameraConfigClick
            )

            // 关于我们
            AboutSection()
        }
    }
}

/**
 * 用户信息卡片
 */
@Composable
fun ProfileHeaderCard(
    userProfile: UserProfile,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 用户头像
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = hasselbladOrange.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = hasselbladOrange,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 用户名称
            Text(
                text = userProfile.displayName,
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Spacing.tiny))

            // 用户描述
            Text(
                text = userProfile.bio,
                style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 编辑按钮
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("编辑个人信息")
            }
        }
    }
}

/**
 * 统计数据卡片
 */
@Composable
fun StatsCard(
    stats: SystemStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large)
        ) {
            Text(
                text = "使用统计",
                style = Typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalPresets.toString(),
                    label = "总预设",
                    icon = Icons.Default.Collections
                )
                StatItem(
                    value = stats.favoritePresets.toString(),
                    label = "收藏",
                    icon = Icons.Default.Favorite
                )
                StatItem(
                    value = stats.configsCount.toString(),
                    label = "配置",
                    icon = Icons.Default.Tune
                )
            }
        }
    }
}

/**
 * 统计数据项
 */
@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = hasselbladOrange.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = hasselbladOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = Typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = hasselbladOrange
        )

        Text(
            text = label,
            style = Typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 功能菜单区域
 */
@Composable
fun ProfileMenuSection(
    onSettingsClick: () -> Unit,
    onCameraConfigClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.small)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Tune,
                title = "相机配置",
                description = "管理相机参数配置",
                onClick = onCameraConfigClick
            )

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "设置",
                description = "应用设置与偏好",
                onClick = onSettingsClick
            )

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            ProfileMenuItem(
                icon = Icons.Default.PrivacyTip,
                title = "隐私设置",
                description = "数据与隐私管理",
                onClick = { /* 隐私设置 */ }
            )

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            ProfileMenuItem(
                icon = Icons.Default.HelpOutline,
                title = "帮助与反馈",
                description = "获取帮助和提交反馈",
                onClick = { /* 帮助与反馈 */ }
            )
        }
    }
}

/**
 * 菜单项
 */
@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // 图标
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = hasselbladOrange.copy(alpha = 0.1f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = hasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 文字内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = Typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 右箭头
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 关于我们区域
 */
@Composable
fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = hasselbladOrange
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "OPPO Master",
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = "Version 3.0.0",
                style = Typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                TextButton(onClick = { /* 用户协议 */ }) {
                    Text(
                        text = "用户协议",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { /* 隐私政策 */ }) {
                    Text(
                        text = "隐私政策",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 用户个人资料数据类
 */
data class UserProfile(
    val displayName: String = "哈苏摄影大师",
    val bio: String = "用镜头记录美好瞬间",
    val avatarUrl: String? = null
)

/**
 * 系统统计数据类
 */
data class SystemStats(
    val totalPresets: Int = 128,
    val favoritePresets: Int = 12,
    val configsCount: Int = 8
)

/**
 * Profile ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val cameraConfigRepository: CameraConfigRepository
) : androidx.lifecycle.ViewModel() {
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _systemStats = MutableStateFlow(SystemStats())
    val systemStats: StateFlow<SystemStats> = _systemStats.asStateFlow()

    init {
        // 加载配置数量
        updateStats()
    }

    private fun updateStats() {
        _systemStats.value = SystemStats(
            configsCount = cameraConfigRepository.configs.value.size
        )
    }
}
