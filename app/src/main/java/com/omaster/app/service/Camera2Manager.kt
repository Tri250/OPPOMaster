package com.omaster.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import com.omaster.app.model.CameraParams
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera2 API 管理器
 * 提供相机参数查询和控制的基础框架
 */
@Singleton
class Camera2Manager @Inject constructor(
    private val context: Context
) {
    
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 获取可用相机列表
     */
    fun getAvailableCameras(): List<String> {
        return try {
            cameraManager.cameraIdList.toList()
        } catch (e: CameraAccessException) {
            Timber.e(e, "获取相机列表失败")
            emptyList()
        }
    }

    /**
     * 获取相机特性信息
     */
    fun getCameraCharacteristics(cameraId: String): CameraCharacteristics? {
        return try {
            cameraManager.getCameraCharacteristics(cameraId)
        } catch (e: CameraAccessException) {
            Timber.e(e, "获取相机特性失败")
            null
        }
    }

    /**
     * 获取相机支持的ISO范围
     */
    fun getSupportedIsoRange(cameraId: String): Pair<Int, Int>? {
        val characteristics = getCameraCharacteristics(cameraId) ?: return null
        
        val sensorInfo = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        return if (sensorInfo != null) {
            Pair(sensorInfo.lower, sensorInfo.upper)
        } else {
            null
        }
    }

    /**
     * 检查是否支持哈苏色彩系统
     */
    fun supportsHasselbladColor(): Boolean {
        // 这里可以根据设备型号或系统特性判断
        val deviceModel = Build.MODEL
        return deviceModel.contains("Find X", ignoreCase = true) || 
               deviceModel.contains("OPPO", ignoreCase = true)
    }

    /**
     * 将预设参数转换为相机可应用的配置
     * 返回参数是否可以被应用（目前是框架，实际应用需要完整的相机session）
     */
    fun applyPresetParams(cameraParams: CameraParams): Boolean {
        Timber.d("尝试应用相机参数: $cameraParams")
        
        // 目前是框架实现，记录参数信息
        // 完整的实现需要：
        // 1. 打开相机
        // 2. 创建CaptureRequest
        // 3. 应用参数
        // 4. 处理预览
        
        Timber.i("参数配置已准备: ISO=${cameraParams.iso}, 快门=${cameraParams.shutter}, EV=${cameraParams.ev}")
        
        return true
    }

    /**
     * 生成相机参数说明
     */
    fun getParamsDescription(cameraParams: CameraParams): String {
        return buildString {
            append("📷 相机参数配置\n")
            append("模式: ${cameraParams.mode}\n")
            append("滤镜: ${cameraParams.filter}\n")
            append("ISO: ${cameraParams.iso}\n")
            append("快门: ${cameraParams.shutter}\n")
            append("曝光补偿: ${cameraParams.ev}\n")
            append("白平衡: ${cameraParams.wb}\n")
            append("哈苏HNCS: ${if (cameraParams.hasselblad_hncs) "开启" else "关闭"}\n")
        }
    }

    /**
     * 获取相机硬件级别
     */
    fun getHardwareLevel(cameraId: String): Int {
        val characteristics = getCameraCharacteristics(cameraId) ?: return CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        
        return characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) 
            ?: CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

    /**
     * 获取硬件级别描述
     */
    fun getHardwareLevelDescription(level: Int): String {
        return when (level) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3 (完整功能)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full (全功能)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited (有限功能)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy (兼容模式)"
            else -> "Unknown"
        }
    }

    /**
     * 检查是否支持手动控制
     */
    fun supportsManualControl(cameraId: String): Boolean {
        val characteristics = getCameraCharacteristics(cameraId) ?: return false
        
        val availableCapabilities = characteristics.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        ) ?: return false
        
        return availableCapabilities.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        )
    }

    companion object {
        private const val TAG = "Camera2Manager"
    }
}
