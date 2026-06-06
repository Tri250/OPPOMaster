package com.omaster.app.data.camera

import androidx.lifecycle.LiveData

sealed class CameraCompatibilityStatus {
    object Available : CameraCompatibilityStatus()
    object NotSupported : CameraCompatibilityStatus()
    object PermissionRequired : CameraCompatibilityStatus()
    data class Error(val message: String) : CameraCompatibilityStatus()
}

data class RealTimeCameraParams(
    val iso: Int = 0,
    val shutterSpeed: String = "auto",
    val ev: String = "0",
    val whiteBalance: String = "auto",
    val lensType: String = "wide"
)

interface CameraParamProvider {
    val params: LiveData<RealTimeCameraParams>
    val status: LiveData<CameraCompatibilityStatus>
    fun startMonitor()
    fun stopMonitor()
    fun switchCamera(lensType: String)
    fun release()
}
