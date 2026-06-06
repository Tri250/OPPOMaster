package com.omaster.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omaster.app.domain.model.Holiday
import com.omaster.app.domain.model.HolidayPresets
import kotlinx.coroutines.delay

/**
 * 节日问候弹窗
 */
@Composable
fun HolidayGreetingDialog(
    holiday: Holiday,
    onDismiss: () -> Unit,
    onViewPresets: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut()
        ) {
            HolidayGreetingContent(
                holiday = holiday,
                onDismiss = onDismiss,
                onViewPresets = onViewPresets
            )
        }
    }
}

@Composable
private fun HolidayGreetingContent(
    holiday: Holiday,
    onDismiss: () -> Unit,
    onViewPresets: () -> Unit
) {
    val primaryColor = Color(android.graphics.Color.parseColor(holiday.theme.primaryColor))
    val secondaryColor = Color(android.graphics.Color.parseColor(holiday.theme.secondaryColor))
    val accentColor = Color(android.graphics.Color.parseColor(holiday.theme.accentColor))
    
    val gradientColors = holiday.theme.backgroundGradient.map {
        Color(android.graphics.Color.parseColor(it))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 关闭按钮
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // 节日图标动画
                HolidayIconAnimation(
                    icon = holiday.theme.icon,
                    accentColor = accentColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 节日名称
                Text(
                    text = holiday.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 问候语
                Text(
                    text = holiday.greeting,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 节日主题预设提示
                if (holiday.presetIds.isNotEmpty()) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🎁 专属${holiday.presetIds.size}款节日预设已准备",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 查看预设按钮
                Button(
                    onClick = {
                        onDismiss()
                        onViewPresets()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "查看节日预设",
                        color = primaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 稍后再看
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "稍后再看",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HolidayIconAnimation(
    icon: String,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // 发光背景
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = accentColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(40.dp)
                )
        )
        
        // 图标
        Text(
            text = icon,
            fontSize = 48.sp,
            modifier = Modifier.scale(scale)
        )
    }
}

/**
 * 节日问候管理器
 */
object HolidayGreetingManager {
    private const val PREFS_NAME = "holiday_greeting_prefs"
    private const val KEY_LAST_SHOWN = "last_shown_holiday"
    
    /**
     * 检查是否应该显示节日问候
     */
    fun shouldShowGreeting(context: android.content.Context): Boolean {
        val holiday = HolidayPresets.getCurrentHoliday() ?: return false
        
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val lastShown = prefs.getString(KEY_LAST_SHOWN, null)
        
        // 如果今天已经显示过，不再显示
        return lastShown != holiday.id
    }
    
    /**
     * 标记已显示
     */
    fun markAsShown(context: android.content.Context, holidayId: String) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SHOWN, holidayId)
            .apply()
    }
    
    /**
     * 获取当前节日
     */
    fun getCurrentHoliday(): Holiday? = HolidayPresets.getCurrentHoliday()
}