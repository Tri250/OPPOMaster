package com.omaster.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrParameterRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    data class OcrResult(
        val rawText: String,
        val confidence: Float,
        val textBlocks: List<TextBlock>
    )

    data class TextBlock(
        val text: String,
        val boundingBox: android.graphics.Rect?,
        val lines: List<String>
    )

    sealed class OcrState {
        object Idle : OcrState()
        object Recognizing : OcrState()
        data class Success(val result: OcrResult) : OcrState()
        data class Error(val message: String) : OcrState()
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage)
            
            result.addOnSuccessListener { visionText ->
                Timber.d("OCR recognition successful: ${visionText.text}")
            }.addOnFailureListener { e ->
                Timber.e(e, "OCR recognition failed")
            }

            val visionText = com.google.android.gms.tasks.Tasks.await(result)
            val ocrResult = OcrResult(
                rawText = visionText.text,
                confidence = calculateConfidence(visionText),
                textBlocks = visionText.textBlocks.map { block ->
                    TextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { it.text }
                    )
                }
            )
            Result.success(ocrResult)
        } catch (e: Exception) {
            Timber.e(e, "Error recognizing text from bitmap")
            Result.failure(e)
        }
    }

    suspend fun recognizeFromUri(uri: Uri): Result<OcrResult> {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val result = textRecognizer.process(inputImage)
            val visionText = com.google.android.gms.tasks.Tasks.await(result)
            
            val ocrResult = OcrResult(
                rawText = visionText.text,
                confidence = calculateConfidence(visionText),
                textBlocks = visionText.textBlocks.map { block ->
                    TextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { it.text }
                    )
                }
            )
            Result.success(ocrResult)
        } catch (e: Exception) {
            Timber.e(e, "Error recognizing text from URI")
            Result.failure(e)
        }
    }

    fun recognizeFromCameraX(): Flow<OcrState> = callbackFlow {
        trySend(OcrState.Idle)
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        this as LifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    trySend(OcrState.Recognizing)
                    
                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                val inputImage = InputImage.fromBitmap(bitmap, image.imageInfo.rotationDegrees)
                                
                                textRecognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val ocrResult = OcrResult(
                                            rawText = visionText.text,
                                            confidence = calculateConfidence(visionText),
                                            textBlocks = visionText.textBlocks.map { block ->
                                                TextBlock(
                                                    text = block.text,
                                                    boundingBox = block.boundingBox,
                                                    lines = block.lines.map { it.text }
                                                )
                                            }
                                        )
                                        trySend(OcrState.Success(ocrResult))
                                    }
                                    .addOnFailureListener { e ->
                                        trySend(OcrState.Error(e.message ?: "Recognition failed"))
                                    }
                                    .addOnCompleteListener {
                                        image.close()
                                    }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                trySend(OcrState.Error(exception.message ?: "Capture failed"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    trySend(OcrState.Error(e.message ?: "Camera binding failed"))
                }
            } catch (e: Exception) {
                trySend(OcrState.Error(e.message ?: "Camera initialization failed"))
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            cameraExecutor.shutdown()
        }
    }

    private fun calculateConfidence(visionText: com.google.mlkit.vision.text.Text): Float {
        var totalConfidence = 0f
        var count = 0
        
        visionText.textBlocks.forEach { block ->
            block.lines.forEach { line ->
                line.elements.forEach { element ->
                    totalConfidence += element.confidence ?: 0f
                    count++
                }
            }
        }
        
        return if (count > 0) totalConfidence / count else 0f
    }

    fun close() {
        textRecognizer.close()
        cameraExecutor.shutdown()
    }
}