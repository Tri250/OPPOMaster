package com.omaster.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun recognizeFromBitmap(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = processImage(inputImage)
            
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
            Timber.d("OCR recognition successful: ${visionText.text}")
            Result.success(ocrResult)
        } catch (e: Exception) {
            Timber.e(e, "OCR recognition failed")
            Result.failure(e)
        }
    }

    suspend fun recognizeFromUri(uri: Uri): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val visionText = processImage(inputImage)
            
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
            Timber.e(e, "OCR recognition failed from URI")
            Result.failure(e)
        }
    }

    private suspend fun processImage(inputImage: InputImage): com.google.mlkit.vision.text.Text {
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (continuation.isActive) {
                        continuation.resume(visionText)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
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
