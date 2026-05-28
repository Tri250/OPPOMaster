package com.omaster.app.feature.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicColorMatrix
import android.renderscript.Matrix4f
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

data class ImageAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val blur: Float = 0f
)

class ImageProcessingPipeline(private val context: Context) {

    private val renderScript: RenderScript by lazy {
        RenderScript.create(context)
    }

    suspend fun applyPresetRealtime(
        bitmap: Bitmap,
        adjustments: ImageAdjustments
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            var result = bitmap.copy(bitmap.config, true)
            
            if (adjustments.brightness != 0f || adjustments.contrast != 1f) {
                result = adjustBrightnessContrast(result, adjustments.brightness, adjustments.contrast)
            }
            
            if (adjustments.saturation != 1f) {
                result = adjustSaturation(result, adjustments.saturation)
            }
            
            if (adjustments.warmth != 0f) {
                result = adjustWarmth(result, adjustments.warmth)
            }
            
            if (adjustments.vignette > 0f) {
                result = applyVignette(result, adjustments.vignette)
            }
            
            if (adjustments.blur > 0f) {
                result = applyBlur(result, adjustments.blur)
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "图像处理失败")
            bitmap
        }
    }

    private fun adjustBrightnessContrast(
        bitmap: Bitmap,
        brightness: Float,
        contrast: Float
    ): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            var r = Color.red(color)
            var g = Color.green(color)
            var b = Color.blue(color)

            r = ((r - 128) * contrast + 128 + brightness * 255).toInt()
            g = ((g - 128) * contrast + 128 + brightness * 255).toInt()
            b = ((b - 128) * contrast + 128 + brightness * 255).toInt()

            r = max(0, min(255, r))
            g = max(0, min(255, g))
            b = max(0, min(255, b))

            pixels[i] = Color.rgb(r, g, b)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun adjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            val newR = (gray + saturation * (r - gray)).toInt()
            val newG = (gray + saturation * (g - gray)).toInt()
            val newB = (gray + saturation * (b - gray)).toInt()

            pixels[i] = Color.rgb(
                max(0, min(255, newR)),
                max(0, min(255, newG)),
                max(0, min(255, newB))
            )
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun adjustWarmth(bitmap: Bitmap, warmth: Float): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            var r = Color.red(color)
            var g = Color.green(color)
            var b = Color.blue(color)

            val adjustment = (warmth * 30).toInt()
            r = max(0, min(255, r + adjustment))
            b = max(0, min(255, b - adjustment))

            pixels[i] = Color.rgb(r, g, b)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float): Bitmap {
        val result = bitmap.copy(bitmap.config, true)
        val width = bitmap.width
        val height = bitmap.height
        val centerX = width / 2f
        val centerY = height / 2f
        val maxDistance = Math.sqrt((centerX * centerX + centerY * centerY).toDouble()).toFloat()

        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val distanceX = x - centerX
                val distanceY = y - centerY
                val distance = Math.sqrt((distanceX * distanceX + distanceY * distanceY).toDouble()).toFloat()
                val normalizedDistance = distance / maxDistance

                val vignetteFactor = 1f - (normalizedDistance * strength * 0.8f)
                val factor = max(0.2f, min(1f, vignetteFactor))

                val color = pixels[index]
                val r = (Color.red(color) * factor).toInt()
                val g = (Color.green(color) * factor).toInt()
                val b = (Color.blue(color) * factor).toInt()

                pixels[index] = Color.rgb(r, g, b)
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun applyBlur(bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0) return bitmap
        
        val input = Allocation.createFromBitmap(renderScript, bitmap)
        val output = Allocation.createTyped(renderScript, input.type)
        
        val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        blurScript.setRadius(min(25f, radius * 25f))
        blurScript.setInput(input)
        blurScript.forEach(output)
        
        val result = bitmap.copy(bitmap.config, true)
        output.copyTo(result)
        
        input.destroy()
        output.destroy()
        blurScript.destroy()
        
        return result
    }

    fun release() {
        renderScript.release()
    }
}
