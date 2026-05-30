package com.omaster.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream

object BitmapUtils {

    /**
     * 从Uri解码Bitmap，优化内存使用
     */
    fun decodeUriToBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                
                options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565
                
                context.contentResolver.openInputStream(uri)?.use { decodedStream ->
                    BitmapFactory.decodeStream(decodedStream, null, options)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 计算合适的采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (width, height) = options.outWidth to options.outHeight
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 获取图片的文件名（如果可用）
     */
    fun getFileName(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            it.getString(nameIndex)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            "file" -> uri.path?.let { path -> File(path).name }
            else -> null
        }
    }
}
