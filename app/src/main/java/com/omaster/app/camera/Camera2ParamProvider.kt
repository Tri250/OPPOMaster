package com.omaster.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.omaster.app.utils.AppLifecycleManager
import com.omaster.app.utils.AppLifecycleListener
import timber.log.Timber

class Camera2ParamProvider(private val context: Context) : CameraParamProvider, AppLifecycleListener {

    private val _params = MutableLiveData(RealTimeCameraParams())
    override val params: LiveData<RealTimeCameraParams> = _params

    private val _status = MutableLiveData<CameraCompatibilityStatus>(CameraCompatibilityStatus.NotSupported)
    override val status: LiveData<CameraCompatibilityStatus> = _status

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    
    private val backgroundThread: HandlerThread by lazy {
        HandlerThread("Camera2ParamProvider").apply { start() }
    }
    private val backgroundHandler: Handler by lazy { Handler(backgroundThread.looper) }
    
    private var isMonitoring = false
    private var currentLensType = "wide"
    private var currentCameraId: String? = null

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            Timber.d("Camera available: $cameraId")
            if (isMonitoring) {
                updateCameraParams()
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            Timber.d("Camera unavailable: $cameraId")
        }
    }

    init {
        AppLifecycleManager.addListener(this)
    }

    override fun startMonitor() {
        if (!checkCameraSupport()) {
            _status.postValue(CameraCompatibilityStatus.NotSupported)
            return
        }

        if (!checkPermissions()) {
            _status.postValue(CameraCompatibilityStatus.PermissionRequired)
            return
        }

        _status.postValue(CameraCompatibilityStatus.Available)
        isMonitoring = true

        try {
            cameraManager.registerAvailabilityCallback(availabilityCallback, backgroundHandler)
            updateCameraParams()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start camera monitor")
        }
    }

    override fun stopMonitor() {
        isMonitoring = false
        try {
            cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister camera callback")
        }
    }

    override fun switchCamera(lensType: String) {
        currentLensType = lensType
        currentCameraId = null
        updateCameraParams()
    }

    override fun release() {
        stopMonitor()
        AppLifecycleManager.removeListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            backgroundThread.quitSafely()
        } else {
            backgroundThread.quit()
        }
    }

    override fun onAppForeground() {
        if (isMonitoring) {
            updateCameraParams()
        }
    }

    override fun onAppBackground() {
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
            
            val iso = readIso(characteristics)
            val shutterSpeed = readShutterSpeed(characteristics)
            val ev = readExposureValue(characteristics)
            val whiteBalance = readWhiteBalance(characteristics)

            val newParams = RealTimeCameraParams(
                iso = iso,
                shutterSpeed = shutterSpeed,
                ev = ev,
                whiteBalance = whiteBalance,
                lensType = currentLensType
            )
            
            _params.postValue(newParams)
            
        } catch (e: Exception) {
            Timber.e(e, "Error reading camera params")
        }
    }

    private fun findCameraIdForLensType(): String? {
        if (currentCameraId != null) {
            return currentCameraId
        }

        val cameraIdList = cameraManager.cameraIdList
        
        val desiredFacing = when (currentLensType) {
            "front" -> CameraMetadata.LENS_FACING_FRONT
            else -> CameraMetadata.LENS_FACING_BACK
        }

        var foundId: String? = null
        
        cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            
            if (facing == desiredFacing) {
                val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                
                if (focalLength != null && focalLength.isNotEmpty()) {
                    val fl = focalLength[0]
                    when (currentLensType) {
                        "wide" -> {
                            if (fl < 2.0f) foundId = id
                        }
                        "ultra" -> {
                            if (fl < 1.5f) foundId = id
                        }
                        "tele" -> {
                            if (fl >= 3.0f) foundId = id
                        }
                        "front" -> {
                            foundId = id
                        }
                    }
                }
                
                if (foundId == null && currentLensType == "wide") {
                    foundId = id
                }
            }
        }

        currentCameraId = foundId
        return foundId
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

    private fun readExposureValue(characteristics: CameraCharacteristics): String {
        return try {
            val evRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            evRange?.lower?.toString() ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }

    private fun formatShutterSpeed(nanos: Long): String {
        return if (nanos >= 1000000L) {
            val seconds = nanos.toDouble() / 1000000000.0
            if (seconds >= 1.0) {
                String.format("%.1fs", seconds)
            } else {
                val fraction = 1.0 / seconds
                "1/${Math.round(fraction)}s"
            }
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
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT) == true -> "Daylight"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT) == true -> "Cloudy"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_TWILIGHT) == true -> "Twilight"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT) == true -> "Incandescent"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT) == true -> "Fluorescent"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT) == true -> "Warm Fluorescent"
                availableModes?.contains(CameraMetadata.CONTROL_AWB_MODE_AUTO) == true -> "Auto"
                else -> "Auto"
            }
        } catch (e: Exception) {
            "Auto"
        }
    }
}
