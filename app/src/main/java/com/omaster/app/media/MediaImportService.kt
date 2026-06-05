package com.omaster.app.media

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MediaImportService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: Flow<ImportState> = _importState.asStateFlow()

    private val imageLoader = ImageLoader.Builder(context).build()

    sealed class ImportState {
        object Idle : ImportState()
        data class Loading(val progress: Int, val message: String) : ImportState()
        data class Success(val uri: Uri, val mediaInfo: MediaInfo) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    data class MediaInfo(
        val uri: Uri,
        val type: MediaType,
        val width: Int,
        val height: Int,
        val duration: Long = 0L,
        val size: Long = 0L,
        val mimeType: String,
        val thumbnailUri: Uri? = null,
        val frameCount: Int = 0,
        val rotation: Int = 0,
        val bitrate: Int = 0
    )

    enum class MediaType {
        IMAGE,
        VIDEO,
        GIF,
        UNKNOWN
    }

    suspend fun importMedia(uri: Uri): Result<MediaInfo> = withContext(Dispatchers.IO) {
        try {
            _importState.value = ImportState.Loading(0, "正在识别文件...")

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val mediaType = detectMediaType(mimeType)

            _importState.value = ImportState.Loading(20, "正在获取媒体信息...")

            val mediaInfo = when (mediaType) {
                MediaType.IMAGE -> getImageInfo(uri, mimeType)
                MediaType.VIDEO -> getVideoInfo(uri, mimeType)
                MediaType.GIF -> getGifInfo(uri, mimeType)
                MediaType.UNKNOWN -> getUnknownInfo(uri, mimeType)
            }

            _importState.value = ImportState.Loading(80, "正在生成缩略图...")

            val thumbnailUri = generateThumbnail(uri, mediaType)

            _importState.value = ImportState.Loading(100, "导入完成")

            val finalInfo = mediaInfo.copy(thumbnailUri = thumbnailUri)
            _importState.value = ImportState.Success(uri, finalInfo)

            Result.success(finalInfo)
        } catch (e: Exception) {
            Timber.e(e, "Import failed for uri: $uri")
            _importState.value = ImportState.Error(e.message ?: "导入失败")
            Result.failure(e)
        }
    }

    private fun detectMediaType(mimeType: String): MediaType {
        return when {
            mimeType.startsWith("image/gif") -> MediaType.GIF
            mimeType.startsWith("video/") -> MediaType.VIDEO
            mimeType.startsWith("image/") -> MediaType.IMAGE
            else -> MediaType.UNKNOWN
        }
    }

    private suspend fun getImageInfo(uri: Uri, mimeType: String): MediaInfo = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        val size = getFileSize(uri)

        MediaInfo(
            uri = uri,
            type = MediaType.IMAGE,
            width = width,
            height = height,
            size = size,
            mimeType = mimeType
        )
    }

    private suspend fun getVideoInfo(uri: Uri, mimeType: String): MediaInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull() ?: 0
            val size = getFileSize(uri)

            MediaInfo(
                uri = uri,
                type = MediaType.VIDEO,
                width = width,
                height = height,
                duration = duration,
                size = size,
                mimeType = mimeType,
                frameCount = frameCount,
                rotation = rotation,
                bitrate = bitrate
            )
        } finally {
            retriever.release()
        }
    }

    private suspend fun getGifInfo(uri: Uri, mimeType: String): MediaInfo = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        val size = getFileSize(uri)

        MediaInfo(
            uri = uri,
            type = MediaType.GIF,
            width = width,
            height = height,
            size = size,
            mimeType = mimeType
        )
    }

    private suspend fun getUnknownInfo(uri: Uri, mimeType: String): MediaInfo = withContext(Dispatchers.IO) {
        MediaInfo(
            uri = uri,
            type = MediaType.UNKNOWN,
            width = 0,
            height = 0,
            size = getFileSize(uri),
            mimeType = mimeType
        )
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun generateThumbnail(uri: Uri, mediaType: MediaType): Uri? = withContext(Dispatchers.IO) {
        try {
            when (mediaType) {
                MediaType.VIDEO -> generateVideoThumbnail(uri)
                MediaType.IMAGE, MediaType.GIF -> generateImageThumbnail(uri)
                MediaType.UNKNOWN -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Thumbnail generation failed")
            null
        }
    }

    private fun generateVideoThumbnail(videoUri: Uri): Uri {
        val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.extractThumbnail(
                MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver,
                    android.content.ContentUris.parseId(videoUri),
                    MediaStore.Video.Thumbnails.MINI_KIND,
                    null
                ),
                300,
                300
            )
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(
                videoUri.path ?: "",
                MediaStore.Video.Thumbnails.MINI_KIND
            )
        }

        return thumbnail?.let { saveThumbnailToCache(it) } ?: videoUri
    }

    private suspend fun generateImageThumbnail(imageUri: Uri): Uri = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .size(300, 300)
            .build()

        val result = imageLoader.execute(request)
        val bitmap = if (result is SuccessResult) {
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } else null

        bitmap?.let { saveThumbnailToCache(it) } ?: imageUri
    }

    private fun saveThumbnailToCache(bitmap: Bitmap): Uri {
        val thumbnailFile = File(context.cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
        thumbnailFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return Uri.fromFile(thumbnailFile)
    }

    suspend fun extractVideoFrame(videoUri: Uri, timeMs: Long = 0): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Timber.e(e, "Frame extraction failed")
            null
        } finally {
            retriever.release()
        }
    }

    fun validateMediaFile(uri: Uri, maxSizeBytes: Long = 100 * 1024 * 1024): Boolean {
        val size = getFileSize(uri)
        if (size > maxSizeBytes) {
            Timber.w("File size exceeds limit: $size > $maxSizeBytes")
            return false
        }

        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == null) {
            Timber.w("Cannot determine MIME type")
            return false
        }

        val supportedTypes = listOf(
            "image/jpeg", "image/png", "image/webp", "image/heic",
            "image/heif", "image/gif", "image/bmp", "image/tiff",
            "video/mp4", "video/3gpp", "video/avi", "video/mkv",
            "video/mov", "video/webm"
        )

        return mimeType in supportedTypes || mimeType.startsWith("image/") || mimeType.startsWith("video/")
    }
}

@Singleton
class VideoPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    data class VideoCaptureConfig(
        val quality: Quality = Quality.HD,
        val frameRate: Int = 30,
        val bitrate: Int = 10_000_000,
        val audioEnabled: Boolean = true
    )

    suspend fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        config: VideoCaptureConfig = VideoCaptureConfig()
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(config.quality))
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                continuation.resume(true)
            } catch (e: Exception) {
                Timber.e(e, "Camera setup failed")
                continuation.resume(false)
            }
        }, ContextCompat.getMainExecutor(context))

        continuation.invokeOnCancellation {
            cameraProviderFuture.get().unbindAll()
        }
    }

    fun startRecording(
        outputFile: File,
        onRecordingEvent: (RecordingEvent) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: return

        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onRecordingEvent(RecordingEvent.Started)
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            onRecordingEvent(RecordingEvent.Error(event.error))
                        } else {
                            onRecordingEvent(RecordingEvent.Finished(event.outputResults.outputUri))
                        }
                    }
                    is VideoRecordEvent.Status -> {
                        onRecordingEvent(RecordingEvent.Status(
                            event.recordingStats.recordedDurationNanos / 1_000_000
                        ))
                    }
                    is VideoRecordEvent.Pause -> onRecordingEvent(RecordingEvent.Paused)
                    is VideoRecordEvent.Resume -> onRecordingEvent(RecordingEvent.Resumed)
                }
            }
    }

    fun pauseRecording() {
        recording?.pause()
    }

    fun resumeRecording() {
        recording?.resume()
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    sealed class RecordingEvent {
        object Started : RecordingEvent()
        object Paused : RecordingEvent()
        object Resumed : RecordingEvent()
        data class Status(val durationMs: Long) : RecordingEvent()
        data class Finished(val outputUri: Uri) : RecordingEvent()
        data class Error(val error: Int) : RecordingEvent()
    }
}
