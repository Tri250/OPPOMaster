package com.omaster.app.data.camera

import android.content.Context
import android.os.Build
import timber.log.Timber

object CameraParamProviderFactory {

    fun create(context: Context): CameraParamProvider {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                Timber.d("Using Camera2ParamProvider")
                Camera2ParamProvider(context)
            }
            else -> {
                Timber.d("Using fallback CameraParamProvider")
                FallbackCameraParamProvider()
            }
        }
    }
}

class FallbackCameraParamProvider : CameraParamProvider {
    private val _params = androidx.lifecycle.MutableLiveData(RealTimeCameraParams())
    override val params: androidx.lifecycle.LiveData<RealTimeCameraParams> = _params

    private val _status = androidx.lifecycle.MutableLiveData<CameraCompatibilityStatus>(
        CameraCompatibilityStatus.NotSupported
    )
    override val status: androidx.lifecycle.LiveData<CameraCompatibilityStatus> = _status

    override fun startMonitor() {
        _status.value = CameraCompatibilityStatus.NotSupported
    }

    override fun stopMonitor() {
        // No-op
    }

    override fun switchCamera(lensType: String) {
        // No-op
    }

    override fun release() {
        // No-op
    }
}
