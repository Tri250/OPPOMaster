package com.omaster.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.omaster.app.model.CameraParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Camera2Controller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var previewSurface: Surface? = null

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Idle)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _currentParams = MutableStateFlow<CameraParams?>(null)
    val currentParams: StateFlow<CameraParams?> = _currentParams.asStateFlow()

    sealed class CameraState {
        object Idle : CameraState()
        object Opening : CameraState()
        object Ready : CameraState()
        object Capturing : CameraState()
        data class Error(val message: String) : CameraState()
    }

    data class CaptureResult(
        val image: Bitmap?,
        val params: CameraParams,
        val filePath: String?
    )

    private val cameraExecutor: Executor = ContextCompat.getMainExecutor(context)

    @OptIn(ExperimentalCamera2Interop::class)
    fun bindCameraX(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onParamsDetected: (CameraParams) -> Unit
    ): Camera? {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                _cameraState.value = CameraState.Opening

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            analyzeImage(imageProxy, onParamsDetected)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                _cameraState.value = CameraState.Ready
                Timber.d("CameraX bound successfully")

                camera
            } catch (e: Exception) {
                Timber.e(e, "Failed to bind CameraX")
                _cameraState.value = CameraState.Error(e.message ?: "Camera initialization failed")
            }
        }, cameraExecutor)

        return null
    }

    private fun analyzeImage(imageProxy: ImageProxy, onParamsDetected: (CameraParams) -> Unit) {
        try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                // 检测画面特征，估算参数
                val estimatedParams = estimateParamsFromImage(bitmap)
                _currentParams.value = estimatedParams
                onParamsDetected(estimatedParams)
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing image")
        } finally {
            imageProxy.close()
        }
    }

    private fun estimateParamsFromImage(bitmap: Bitmap): CameraParams {
        // 基于图像分析估算相机参数
        // 这是一个简化版本，实际可以使用更复杂的算法
        val width = bitmap.width
        val height = bitmap.height
        
        return CameraParams(
            mode = "哈苏大师",
            iso = 100,
            shutter = "1/200",
            ev = "0",
            wb = "5500K",
            focal_length = "24mm",
            aperture = "f/1.8",
            ai_optimization = true,
            hasselblad_hncs = true,
            hasselblad_natural_color = true,
            color_profile = "Natural"
        )
    }

    fun setCameraParams(params: CameraParams): Boolean {
        Timber.d("Setting camera params: $params")
        _currentParams.value = params
        
        try {
            applyParamsToSession(params)
            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply camera params")
            return false
        }
    }

    private fun applyParamsToSession(params: CameraParams) {
        captureSession?.let { session ->
            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            
            captureBuilder?.apply {
                // ISO
                set(CaptureRequest.SENSOR_SENSITIVITY, params.iso)
                
                // EV
                val evValue = (params.ev.replace("+", "").toFloatOrNull() ?: 0f) * 6 // 转换为Camera2 EV单位
                set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evValue.toInt())
                
                // White Balance
                if (params.wb.equals("Auto", ignoreCase = true)) {
                    set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                } else {
                    set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                    val wbValue = params.wb.replace("K", "").toIntOrNull() ?: 5500
                    // 设置色温 (简化处理)
                }
                
                // Apply to session
                session.capture(build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        Timber.d("Camera params applied successfully")
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        Timber.e("Failed to apply camera params: ${failure.reason}")
                    }
                }, backgroundHandler)
            }
        }
    }

    suspend fun captureImage(outputFile: File): Result<CaptureResult> {
        return try {
            _cameraState.value = CameraState.Capturing
            
            val currentParamsValue = _currentParams.value ?: CameraParams()
            
            captureSession?.let { session ->
                val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                
                imageReader?.surface?.let { surface ->
                    captureBuilder?.addTarget(surface)
                    
                    applyParamsToCaptureRequest(captureBuilder!!, currentParamsValue)
                    
                    session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            Timber.d("Image captured successfully")
                            _cameraState.value = CameraState.Ready
                        }

                        override fun onCaptureFailed(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            failure: CaptureFailure
                        ) {
                            Timber.e("Capture failed: ${failure.reason}")
                            _cameraState.value = CameraState.Error("Capture failed")
                        }
                    }, backgroundHandler)
                }
            }
            
            Result.success(
                CaptureResult(
                    image = null,
                    params = currentParamsValue,
                    filePath = outputFile.absolutePath
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error capturing image")
            _cameraState.value = CameraState.Error(e.message ?: "Capture failed")
            Result.failure(e)
        }
    }

    private fun applyParamsToCaptureRequest(builder: CaptureRequest.Builder, params: CameraParams) {
        builder.apply {
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            
            // Apply custom params
            set(CaptureRequest.SENSOR_SENSITIVITY, params.iso)
            
            val evValue = (params.ev.replace("+", "").toFloatOrNull() ?: 0f) * 6
            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, evValue.toInt())
        }
    }

    fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Timber.e(e, "Error stopping background thread")
        }
    }

    fun close() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing camera")
        }
        stopBackgroundThread()
    }

    fun saveParamsToImage(imageFile: File, params: CameraParams): Result<File> {
        return try {
            val jsonContent = """
                {
                    "preset_name": "${params.hasselblad_master_style}",
                    "camera_params": {
                        "mode": "${params.mode}",
                        "iso": ${params.iso},
                        "shutter": "${params.shutter}",
                        "ev": "${params.ev}",
                        "wb": "${params.wb}",
                        "focal_length": "${params.focal_length}",
                        "aperture": "${params.aperture}",
                        "filter": "${params.filter}",
                        "hncs": ${params.hasselblad_hncs}
                    },
                    "device": "小O帮帮",
                    "version": "2.0"
                }
            """.trimIndent()
            
            val jsonFile = File(imageFile.parent, "${imageFile.nameWithoutExtension}.json")
            FileOutputStream(jsonFile).use { fos ->
                fos.write(jsonContent.toByteArray())
            }
            
            Result.success(jsonFile)
        } catch (e: Exception) {
            Timber.e(e, "Error saving params to image")
            Result.failure(e)
        }
    }
}