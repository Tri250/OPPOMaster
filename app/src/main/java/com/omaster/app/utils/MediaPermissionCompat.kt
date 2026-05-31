package com.omaster.app.utils

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 统一媒体权限兼容层
 * 适配 Android 13+ 细粒度媒体权限
 */
object MediaPermissionCompat {

    /**
     * 获取图片访问所需的权限列表
     */
    fun getRequiredImagePermissions(context: Context): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取视频访问所需的权限列表
     */
    fun getRequiredVideoPermissions(context: Context): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * 获取音频访问所需的权限列表
     */
    fun getRequiredAudioPermissions(context: Context): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * 检查是否使用了 Android 13+ 权限模型
     */
    fun isUsingMediaPermissions(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * 获取权限说明文本（根据版本不同）
     */
    fun getPermissionExplanation(type: MediaType, context: Context): String {
        return when (type) {
            MediaType.IMAGE -> {
                if (isUsingMediaPermissions(context)) {
                    "需要访问您的照片，用于AI场景识别。不会上传照片到云端。"
                } else {
                    "需要访问您的存储，用于选择照片进行AI场景识别。不会上传照片到云端。"
                }
            }
            MediaType.VIDEO -> {
                if (isUsingMediaPermissions(context)) {
                    "需要访问您的视频，用于AI场景识别。不会上传视频到云端。"
                } else {
                    "需要访问您的存储，用于选择视频进行AI场景识别。不会上传视频到云端。"
                }
            }
            MediaType.AUDIO -> {
                if (isUsingMediaPermissions(context)) {
                    "需要访问您的音频。"
                } else {
                    "需要访问您的存储，用于选择音频。"
                }
            }
        }
    }
}

/**
 * 媒体类型
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}
