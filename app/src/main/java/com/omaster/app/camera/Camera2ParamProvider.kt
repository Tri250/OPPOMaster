package com.omaster.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class Camera2ParamProvider(private val context: Context) : CameraParamProvider {

    private val _params = MutableLiveData(RealTimeCameraParams())
    override val params: LiveData<RealTimeCameraParams> = _params

    private val _status = MutableLiveData<CameraCompatibilityStatus>(CameraCompatibilityStatus.NotSupported)
    override val status: LiveData<CameraCompatibilityStatus> = _status

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var monitorJob: Job? = null

    private var currentLensType = "wide"

    override fun startMonitor() {
        if (!checkCameraSupport()) {
            _status.value = CameraCompatibilityStatus.NotSupported
            return
        }

        if (!checkPermissions()) {
            _status.value = CameraCompatibilityStatus.PermissionRequired
            return
        }

        _status.value = CameraCompatibilityStatus.Available

        monitorJob = scope.launch {
            while (true) {
                try {
                    updateCameraParams()
                } catch (e: Exception) {
                    Timber.e(e, "Error updating camera params")
                }
                delay(300) // Update every 300ms
            }
        }
    }

    override fun stopMonitor() {
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun switchCamera(lensType: String) {
        currentLensType = lensType
        updateCameraParams()
    }

    override fun release() {
        stopMonitor()
    }

    private fun checkCameraSupport(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            false
        } else {
            try {
                val cameraIdList = cameraManager.cameraIdList
                cameraIdList.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateCameraParams() {
        try {
            val cameraId = findCameraIdForLensType() ?: return
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            
            val newParams = RealTimeCameraParams(
                iso = readIso(characteristics),
                shutterSpeed = readShutterSpeed(characteristics),
                ev = "0",
                whiteBalance = readWhiteBalance(characteristics),
                lensType = currentLensType
            )
            
            _params.postValue(newParams)
            
        } catch (e: Exception) {
            Timber.e(e, "Error reading camera params")
        }
    }

    private fun findCameraIdForLensType(): String? {
        val lensFacing = when (currentLensType)
        val cameraIdList = cameraManager.cameraIdList
        
        val desiredFacing = when (lensFacing) {
            "wide" -> CameraMetadata.LENS_FACING_BACK
            "ultra" -> CameraMetadata.LENS_FACING_BACK
            "tele" -> CameraMetadata.LENS_FACING_BACK
            "front" -> CameraMetadata.LENS_FACING_FRONT
            else -> CameraMetadata.LENS_FACING_BACK
        }

        return cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == desiredFacing
        }
    }

    private fun readIso(characteristics: CameraCharacteristics): Int {
        return try {
            val isoRange = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
            )
            isoRange?.lower ?: 100
        } catch (e: Exception) {
            100
        }
    }

    private fun readShutterSpeed(characteristics: CameraCharacteristics): String {
        return try {
            val exposureTimeRange = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
            )
            val exposureTime = exposureTimeRange?.lower ?: 1000000L
            formatShutterSpeed(exposureTime)
        } catch (e: Exception) {
            "auto"
        }
    }

    private fun formatShutterSpeed(nanos: Long): String {
        return if (nanos >= 1000000L) {
            val seconds = nanos.toDouble() / 1000000000.0
            String.format("%.1fs", seconds)
        } else {
            val fraction = 1000000000.0 / nanos.toDouble()
            "1/${Math.round(fraction)}s"
        }
    }

    private fun readWhiteBalance(characteristics: CameraCharacteristics): String {
        return try {
            val availableModes = characteristics.get(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES
            )
            when {
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT) == true -> "daylight"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_AUTO) == true -> "auto"
                else -> "auto"
            }
        } catch (e: Exception) {
            "auto"
        }
    }
}
