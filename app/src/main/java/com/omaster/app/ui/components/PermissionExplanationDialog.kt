package com.omaster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omaster.app.ui.theme.ColorOSBlack
import com.omaster.app.ui.theme.HasselbladOrange
import com.omaster.app.ui.theme.ColorOSLightTextPrimary
import com.omaster.app.ui.theme.ColorOSLightBackground
import com.omaster.app.ui.animation.clickableWithColorOSFeedback

/**
 * 权限说明对话框
 * 符合 Android 官方最佳实践：请求前给出解释
 */
@Composable
fun PermissionExplanationDialog(
    title: String,
    explanation: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isDarkTheme: Boolean = false
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) ColorOSBlack else ColorOSLightBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图标
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HasselbladOrange.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.padding(16.dp).size(48.dp)
                    )
                }

                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else ColorOSLightTextPrimary,
                    textAlign = TextAlign.Center
                )

                // 说明文本
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f)
                            else ColorOSLightTextPrimary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange
                        )
                    ) {
                        Text(
                            text = "授权",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDarkTheme) ColorOSBlack else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 权限被拒引导对话框
 * 引导用户到设置页手动授权
 */
@Composable
fun PermissionDeniedDialog(
    title: String,
    explanation: String,
    settingHint: String = "请在设置中开启此权限",
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
    isDarkTheme: Boolean = false
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) ColorOSBlack else ColorOSLightBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 警告图标
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorOSRed.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ColorOSRed,
                        modifier = Modifier.padding(16.dp).size(48.dp)
                    )
                }

                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else ColorOSLightTextPrimary,
                    textAlign = TextAlign.Center
                )

                // 说明文本
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f)
                            else ColorOSLightTextPrimary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                // 设置提示
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = (if (isDarkTheme) ColorOSGray else ColorOSLightGray).copy(alpha = 0.3f)
                ) {
                    Text(
                        text = settingHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                                else ColorOSLightTextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isDarkTheme) ColorOSBlack else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "打开设置",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDarkTheme) ColorOSBlack else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * ColorOS 红色
 */
private val ColorOSRed = Color(0xFFFF4D4F)
private val ColorOSGray = Color(0xFF3A3A3C)
private val ColorOSLightGray = Color(0xFFE5E5EA)
