package com.omaster.app.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.ColorOSLightBackground
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 分享组件 - 支持S003, S011-S020测试用例
 */
@Composable
fun ShareDialog(
    imageUri: Uri?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQualityDialog by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    
    if (showQualityDialog && imageUri != null) {
        QualitySelectionDialog(
            onQualitySelected = { quality ->
                showQualityDialog = false
                isSharing = true
                scope.launch {
                    shareImage(context, imageUri, quality)
                    isSharing = false
                    onDismiss()
                }
            },
            onDismiss = { showQualityDialog = false }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = ColorOSBlack,
        title = {
            Text(
                text = "分享到",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSharing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在准备分享...",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    SocialAppGrid(
                        onAppClick = { app ->
                            when (app) {
                                ShareApp.WECHAT -> shareToWeChat(context, imageUri)
                                ShareApp.WECHAT_MOMENTS -> shareToWeChatMoments(context, imageUri)
                                ShareApp.QQ -> shareToQQ(context, imageUri)
                                ShareApp.QQ_ZONE -> shareToQQZone(context, imageUri)
                                ShareApp.WEIBO -> shareToWeibo(context, imageUri)
                                ShareApp.DOUYIN -> shareToDouyin(context, imageUri)
                                ShareApp.XIAOHONGSHU -> shareToXiaohongshu(context, imageUri)
                                ShareApp.MORE -> {
                                    showQualityDialog = true
                                }
                            }
                            if (app != ShareApp.MORE) {
                                onDismiss()
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun SocialAppGrid(
    onAppClick: (ShareApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps = listOf(
        listOf(ShareApp.WECHAT, ShareApp.WECHAT_MOMENTS, ShareApp.QQ, ShareApp.QQ_ZONE),
        listOf(ShareApp.WEIBO, ShareApp.DOUYIN, ShareApp.XIAOHONGSHU, ShareApp.MORE)
    )
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        apps.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { app ->
                    SocialAppItem(
                        app = app,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
    }
}

@Composable
fun SocialAppItem(
    app: ShareApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = app.color.copy(alpha = 0.15f),
            onClick = onClick
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = app.icon,
                    contentDescription = app.name,
                    tint = app.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 11.sp
        )
    }
}

@Composable
fun QualitySelectionDialog(
    onQualitySelected: (ShareQuality) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorOSBlack,
        title = {
            Text(
                text = "选择分享质量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareQuality.entries.forEach { quality ->
                    QualityOption(
                        quality = quality,
                        onClick = { onQualitySelected(quality) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun QualityOption(
    quality: ShareQuality,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ColorOSLightBackground.copy(alpha = 0.1f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = quality.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = quality.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

enum class ShareApp(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val packageName: String
) {
    WECHAT("微信", Icons.Default.Chat, Color(0xFF07C160)),
    WECHAT_MOMENTS("朋友圈", Icons.Default.People, Color(0xFF07C160)),
    QQ("QQ", Icons.Default.Forum, Color(0xFF12B7F5)),
    QQ_ZONE("QQ空间", Icons.Default.Photo, Color(0xFFFACC15)),
    WEIBO("微博", Icons.Default.Language, Color(0xFFDC4228)),
    DOUYIN("抖音", Icons.Default.PlayCircle, Color(0xFF000000)),
    XIAOHONGSHU("小红书", Icons.Default.Bookmark, Color(0xFFFF2442)),
    MORE("更多", Icons.Default.MoreHoriz, HasselbladOrange)
}

enum class ShareQuality(
    val name: String,
    val description: String,
    val compressionQuality: Int
) {
    ORIGINAL("原图", "保持原始分辨率和质量", 100),
    HIGH("高质量", "高分辨率，推荐分享", 90),
    MEDIUM("中等质量", "平衡质量和大小", 70),
    LOW("低质量", "小文件，快速分享", 50)
}

suspend fun shareImage(
    context: Context,
    imageUri: Uri,
    quality: ShareQuality
) {
    withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "share_${System.currentTimeMillis()}.jpg")
            
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                try {
                    FileOutputStream(tempFile).use { output ->
                        bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            quality.compressionQuality,
                            output
                        )
                    }
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            withContext(Dispatchers.Main) {
                shareUri(shareUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun shareUri(uri: Uri) {}

fun shareToWeChat(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.tencent.mm")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到微信"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.addCategory(Intent.CATEGORY_DEFAULT))
        }
    }
}

fun shareToWeChatMoments(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.tencent.mm")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra("Ksnsuploadimgusestamp", 1)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到朋友圈"))
        } catch (e: Exception) {
            shareToWeChat(context, imageUri)
        }
    }
}

fun shareToQQ(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.tencent.mobileqq")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到QQ"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.addCategory(Intent.CATEGORY_DEFAULT))
        }
    }
}

fun shareToQQZone(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            component = ComponentName(
                "com.qzone",
                "com.qzone.ui.module.home.QZoneFeedActivity"
            )
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到QQ空间"))
        } catch (e: Exception) {
            shareToQQ(context, imageUri)
        }
    }
}

fun shareToWeibo(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.sina.weibo")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到微博"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.addCategory(Intent.CATEGORY_DEFAULT))
        }
    }
}

fun shareToDouyin(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.ss.android.ugc.aweme")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到抖音"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.addCategory(Intent.CATEGORY_DEFAULT))
        }
    }
}

fun shareToXiaohongshu(context: Context, imageUri: Uri?) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.xingin.xhs")
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "分享到小红书"))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }.addCategory(Intent.CATEGORY_DEFAULT))
        }
    }
}

fun shareToSystem(context: Context, imageUri: Uri?, quality: ShareQuality = ShareQuality.HIGH) {
    imageUri?.let { uri ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "分享"))
    }
}
