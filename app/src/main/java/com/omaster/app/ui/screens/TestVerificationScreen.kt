package com.omaster.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.ui.theme.*

enum class TestCategory(val displayName: String, val icon: String) {
    SCENE_RECOGNITION("场景识别准确性", "🎯"),
    SPECIAL_SCENES("特殊场景测试", "⚡"),
    AUTO_FILL("参数自动填入", "✨"),
    FLOATING_WINDOW("悬浮窗功能", "🪟"),
    CATEGORY_SEARCH("分类搜索", "🔍"),
    PRESET_ECOSYSTEM("预设生态", "🌿"),
    MULTI_FORMAT("多格式导入导出", "📦"),
    PERFORMANCE("性能测试", "⚙️"),
    SECURITY("安全性测试", "🔒")
}

enum class TestStatus(val displayName: String) {
    PASSED("通过"),
    FAILED("失败"),
    PENDING("待测试")
}

data class TestItem(
    val id: Int,
    val name: String,
    val category: TestCategory,
    val steps: List<String>,
    val expectedResult: String,
    val acceptanceCriteria: String,
    var status: TestStatus = TestStatus.PENDING
)

@Composable
fun TestVerificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestVerificationViewModel = hiltViewModel()
) {
    val testItems by viewModel.testItems.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<TestCategory?>(null) }
    
    val filteredItems = remember(testItems, selectedCategory) {
        if (selectedCategory == null) {
            testItems
        } else {
            testItems.filter { it.category == selectedCategory }
        }
    }
    
    val stats = remember(testItems) {
        mapOf(
            "total" to testItems.size,
            "passed" to testItems.count { it.status == TestStatus.PASSED },
            "pending" to testItems.count { it.status == TestStatus.PENDING }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TestVerificationTopBar(onBack = onBack)
        },
        containerColor = DeepSpace
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 页面标题和描述
            TestHeader()
            
            // 统计信息卡片
            TestStatsCard(stats = stats)
            
            // 测试分类标签
            TestCategoryTabs(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // 测试项列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredItems, key = { it.id }) { testItem ->
                    TestItemCard(
                        testItem = testItem,
                        onStatusChange = { newStatus ->
                            viewModel.updateTestStatus(testItem.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestVerificationTopBar(onBack: () -> Unit) {
    Surface(
        color = DeepSpace,
        tonalElevation = 0.dp
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "测试验证中心",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OppoSunriseGold,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = OppoSunriseGold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DeepSpace
            )
        )
    }
}

@Composable
fun TestHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "测试验证中心",
            style = MaterialTheme.typography.headlineMedium,
            color = OppoSunriseGold,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "小O帮帮功能测试验证 - 全面检查各项功能是否符合验收标准",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun TestStatsCard(stats: Map<String, Int>) {
    val completionRate = if (stats["total"] ?: 0 > 0) {
        ((stats["passed"] ?: 0).toFloat() / (stats["total"] ?: 1) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "总测试项",
                    value = "${stats["total"] ?: 0}",
                    color = OppoSunriseGold
                )
                StatItem(
                    label = "通过数",
                    value = "${stats["passed"] ?: 0}",
                    color = SuccessVital
                )
                StatItem(
                    label = "待测试",
                    value = "${stats["pending"] ?: 0}",
                    color = WarningVital
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 完成率进度条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "完成率",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$completionRate%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (completionRate >= 80) SuccessVital else WarningVital,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { completionRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (completionRate >= 80) SuccessVital else WarningVital,
                    trackColor = Neutral700,
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun TestCategoryTabs(
    selectedCategory: TestCategory?,
    onCategorySelected: (TestCategory?) -> Unit
) {
    var scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 全部标签
        TestCategoryChip(
            label = "全部",
            emoji = "📋",
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) }
        )
        
        // 其他分类标签
        TestCategory.values().forEach { category ->
            TestCategoryChip(
                label = category.displayName,
                emoji = category.icon,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun TestCategoryChip(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            OppoSunriseGold.copy(alpha = 0.15f)
        } else {
            BgSecondary
        },
        animationSpec = tween(durationMillis = 200),
        label = "chip_bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            OppoSunriseGold.copy(alpha = 0.5f)
        } else {
            Neutral700
        },
        animationSpec = tween(durationMillis = 200),
        label = "chip_border"
    )
    
    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) OppoSunriseGold else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TestItemCard(
    testItem: TestItem,
    onStatusChange: (TestStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val cardElevation by animateDpAsState(
        targetValue = if (expanded) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = 200),
        label = "card_elevation"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 分类标签
                    Surface(
                        color = OppoSunriseGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = testItem.category.icon,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Column {
                        Text(
                            text = testItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = testItem.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
                
                // 状态切换按钮
                StatusToggleButton(
                    currentStatus = testItem.status,
                    onStatusChange = onStatusChange
                )
            }
            
            // 展开内容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = Neutral700)
                    
                    // 测试步骤
                    TestSection(
                        title = "测试步骤",
                        icon = "📝",
                        content = {
                            testItem.steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        color = OppoSunriseGold.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = OppoSunriseGold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    )
                    
                    // 预期结果
                    TestSection(
                        title = "预期结果",
                        icon = "🎯",
                        content = {
                            Text(
                                text = testItem.expectedResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    )
                    
                    // 验收标准
                    TestSection(
                        title = "验收标准",
                        icon = "✅",
                        content = {
                            Text(
                                text = testItem.acceptanceCriteria,
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessVital
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusToggleButton(
    currentStatus: TestStatus,
    onStatusChange: (TestStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val statusColor = when (currentStatus) {
        TestStatus.PASSED -> SuccessVital
        TestStatus.FAILED -> ErrorVital
        TestStatus.PENDING -> WarningVital
    }
    
    Box {
        val scale by animateFloatAsState(
            targetValue = if (showMenu) 1.1f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "status_btn_scale"
        )
        
        Surface(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            onClick = { showMenu = true },
            color = statusColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val icon = when (currentStatus) {
                    TestStatus.PASSED -> Icons.Default.CheckCircle
                    TestStatus.FAILED -> Icons.Default.Cancel
                    TestStatus.PENDING -> Icons.Default.Schedule
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentStatus.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            TestStatus.values().forEach { status ->
                val isSelected = status == currentStatus
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    TestStatus.PASSED -> Icons.Default.CheckCircle
                                    TestStatus.FAILED -> Icons.Default.Cancel
                                    TestStatus.PENDING -> Icons.Default.Schedule
                                },
                                contentDescription = null,
                                tint = when (status) {
                                    TestStatus.PASSED -> SuccessVital
                                    TestStatus.FAILED -> ErrorVital
                                    TestStatus.PENDING -> WarningVital
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = status.displayName,
                                color = if (isSelected) OppoSunriseGold else TextPrimary
                            )
                        }
                    },
                    onClick = {
                        onStatusChange(status)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun TestSection(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = OppoSunriseGold,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.padding(start = 24.dp)) {
            content()
        }
    }
}

// ==================== 验收标准展示卡片 ====================
@Composable
fun AcceptanceCriteriaCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgTertiary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = OppoSunriseGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "验收标准",
                    style = MaterialTheme.typography.titleMedium,
                    color = OppoSunriseGold,
                    fontWeight = FontWeight.Bold
                )
            }
            
            HorizontalDivider(color = Neutral700)
            
            val criteria = listOf(
                "功能性：通过率≥99.5%" to SuccessVital,
                "稳定性：24小时无崩溃" to SuccessVital,
                "性能：启动≤3s，响应≤2s" to InfoVital,
                "兼容性：测试通过率≥98%" to InfoVital,
                "安全性：无高危漏洞" to ErrorVital,
                "用户体验：响应迅速，操作直观" to WarningVital
            )
            
            criteria.forEach { (criterion, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = criterion,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
