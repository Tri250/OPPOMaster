package com.omaster.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片工具类 - 防OOM、统一预处理
 */
object ImageUtils {

    private const val MAX_BITMAP_WIDTH = 1080
    private const val MAX_BITMAP_HEIGHT = 1080

    /**
     * Uri解码并下采样为Bitmap
     * 防OOM，适合AI推理输入
     */
    @WorkerThread
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        targetWidth: Int = MAX_BITMAP_WIDTH,
        targetHeight: Int = MAX_BITMAP_HEIGHT
    ): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                options.inSampleSize = calculateInSampleSize(
                    options.outWidth,
                    options.outHeight,
                    targetWidth,
                    targetHeight
                )

                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565
                options.inMutable = false

                context.contentResolver.openInputStream(uri)?.use { newInputStream ->
                    BitmapFactory.decodeStream(newInputStream, null, options)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 协程版本的图片解码
     */
    suspend fun decodeSampledBitmapSuspend(
        context: Context,
        uri: Uri,
        targetWidth: Int = MAX_BITMAP_WIDTH,
        targetHeight: Int = MAX_BITMAP_HEIGHT
    ): Bitmap? = withContext(Dispatchers.IO) {
        decodeSampledBitmap(context, uri, targetWidth, targetHeight)
    }

    /**
     * 计算BitmapFactory的采样率
     */
    private fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (originalWidth > reqWidth || originalHeight > reqHeight) {
            val halfWidth = originalWidth / 2
            val halfHeight = originalHeight / 2

            while (halfWidth / inSampleSize >= reqWidth &&
                halfHeight / inSampleSize >= reqHeight
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 获取图片尺寸信息
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                Pair(options.outWidth, options.outHeight)
            } ?: Pair(0, 0)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    /**
     * 批量图片处理 - 带并发控制
     */
    suspend fun batchDecodeSampledBitmaps(
        context: Context,
        uris: List<Uri>,
        maxConcurrency: Int = 2
    ): List<Bitmap?> = withContext(Dispatchers.IO) {
        uris.map { uri ->
            decodeSampledBitmap(context, uri)
        }
    }
}
