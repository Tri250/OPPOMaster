package com.omaster.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.data.AppLanguage
import com.omaster.app.presentation.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 语言设置页面
 * 用于切换应用显示语言
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val supportedLanguages = remember { AppLanguage.entries.toList() }
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingLanguage by remember { mutableStateOf<AppLanguage?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "语言设置",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前语言显示
            CurrentLanguageCard(selectedLanguage = selectedLanguage)

            Spacer(modifier = Modifier.height(8.dp))

            // 语言列表标题
            Text(
                text = "选择语言",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 语言列表
            supportedLanguages.forEach { language ->
                LanguageOptionCard(
                    language = language,
                    isSelected = selectedLanguage == language,
                    onClick = {
                        if (selectedLanguage != language) {
                            pendingLanguage = language
                            showRestartDialog = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 预览示例
            Text(
                text = "预览示例",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PreviewCard(selectedLanguage = selectedLanguage)

            Spacer(modifier = Modifier.height(16.dp))

            // 重启提示
            RestartHintCard()
        }
    }

    // 重启确认对话框
    if (showRestartDialog && pendingLanguage != null) {
        RestartConfirmDialog(
            targetLanguage = pendingLanguage!!,
            onConfirm = {
                viewModel.setLanguage(pendingLanguage!!)
                showRestartDialog = false
                pendingLanguage = null
            },
            onDismiss = {
                showRestartDialog = false
                pendingLanguage = null
            }
        )
    }
}

/**
 * 当前语言卡片
 */
@Composable
private fun CurrentLanguageCard(selectedLanguage: AppLanguage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "当前语言",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${selectedLanguage.displayName} (${selectedLanguage.nativeName})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 语言选项卡片
 */
@Composable
private fun LanguageOptionCard(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 语言图标/标识
                LanguageIcon(language = language)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = language.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 语言图标
 */
@Composable
private fun LanguageIcon(language: AppLanguage) {
    val backgroundColor = when (language) {
        AppLanguage.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
        AppLanguage.CHINESE -> MaterialTheme.colorScheme.errorContainer
        AppLanguage.ENGLISH -> MaterialTheme.colorScheme.tertiaryContainer
        AppLanguage.JAPANESE -> MaterialTheme.colorScheme.secondaryContainer
        AppLanguage.KOREAN -> MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = when (language) {
        AppLanguage.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
        AppLanguage.CHINESE -> MaterialTheme.colorScheme.onErrorContainer
        AppLanguage.ENGLISH -> MaterialTheme.colorScheme.onTertiaryContainer
        AppLanguage.JAPANESE -> MaterialTheme.colorScheme.onSecondaryContainer
        AppLanguage.KOREAN -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (language) {
                    AppLanguage.SYSTEM -> "A"
                    AppLanguage.CHINESE -> "中"
                    AppLanguage.ENGLISH -> "En"
                    AppLanguage.JAPANESE -> "日"
                    AppLanguage.KOREAN -> "한"
                },
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

/**
 * 预览卡片
 */
@Composable
private fun PreviewCard(selectedLanguage: AppLanguage) {
    val locale = when (selectedLanguage) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.CHINESE -> Locale.CHINESE
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.JAPANESE -> Locale.JAPANESE
        AppLanguage.KOREAN -> Locale.KOREAN
    }

    val dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, locale)
    val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, locale)
    val numberFormat = java.text.NumberFormat.getNumberInstance(locale)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 示例文本
            PreviewItem(
                label = "示例文本",
                value = getSampleText(selectedLanguage)
            )

            Divider()

            // 日期格式示例
            PreviewItem(
                label = "日期格式",
                value = dateFormat.format(Date())
            )

            // 时间格式示例
            PreviewItem(
                label = "时间格式",
                value = timeFormat.format(Date())
            )

            // 数字格式示例
            PreviewItem(
                label = "数字格式",
                value = numberFormat.format(1234567.89)
            )
        }
    }
}

/**
 * 预览项
 */
@Composable
private fun PreviewItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 获取示例文本
 */
private fun getSampleText(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> "欢迎使用 OMaster"
        AppLanguage.CHINESE -> "欢迎使用 OMaster"
        AppLanguage.ENGLISH -> "Welcome to OMaster"
        AppLanguage.JAPANESE -> "OMaster へようこそ"
        AppLanguage.KOREAN -> "OMaster에 오신 것을 환영합니다"
    }
}

/**
 * 重启提示卡片
 */
@Composable
private fun RestartHintCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "切换语言后需要重启应用才能完全生效",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * 重启确认对话框
 */
@Composable
private fun RestartConfirmDialog(
    targetLanguage: AppLanguage,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换语言") },
        text = {
            Column {
                Text(
                    text = "确定要切换到 ${targetLanguage.displayName} (${targetLanguage.nativeName}) 吗？",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "切换后需要重启应用才能完全生效。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
