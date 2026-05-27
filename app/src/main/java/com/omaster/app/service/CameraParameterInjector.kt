package com.omaster.app.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.omaster.app.model.CameraParams
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CameraParameterInjector(
    private val context: Context
) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    suspend fun injectPreset(preset: Preset): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val params = preset.cameraParams ?: return@withContext Result.failure(
                IllegalStateException("Camera parameters not found")
            )

            val validation = validateParameters(params)
            if (!validation.isValid) {
                return@withContext Result.failure(
                    IllegalStateException(validation.message ?: "Invalid parameters")
                )
            }

            try {
                if (isOppoCameraSdkAvailable()) {
                    injectViaOppoSdk(params)
                } else {
                    injectViaCamera2(params)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun isOppoCameraSdkAvailable(): Boolean {
        return false
    }

    private fun injectViaOppoSdk(params: CameraParams) {
    }

    private fun injectViaCamera2(params: CameraParams) {
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null
    )

    private fun validateParameters(params: CameraParams): ValidationResult {
        if (params.iso !in 50..6400) {
            return ValidationResult(false, "ISO must be between 50 and 6400")
        }

        val shutterNs = parseShutter(params.shutter)
        if (shutterNs !in 1_000_000L..30_000_000_000L) {
            return ValidationResult(false, "Shutter speed out of range")
        }

        val ev = parseEV(params.ev)
        if (ev !in -4..4) {
            return ValidationResult(false, "EV must be between -4 and 4")
        }

        if (params.contrast !in 0.5f..2.0f) {
            return ValidationResult(false, "Contrast must be between 0.5 and 2.0")
        }

        if (params.saturation !in 0.0f..2.0f) {
            return ValidationResult(false, "Saturation must be between 0.0 and 2.0")
        }

        if (params.vignette !in 0.0f..1.0f) {
            return ValidationResult(false, "Vignette must be between 0.0 and 1.0")
        }

        return ValidationResult(true)
    }

    private fun parseShutter(shutter: String): Long {
        return when {
            shutter.contains("/") -> {
                val parts = shutter.split("/")
                val numerator = parts[0].toLongOrNull() ?: 1
                val denominator = parts[1].toLongOrNull() ?: 125
                (1_000_000_000L * numerator) / denominator
            }
            shutter.endsWith("s") -> {
                val seconds = shutter.removeSuffix("s").toDoubleOrNull() ?: 0.125
                (seconds * 1_000_000_000L).toLong()
            }
            else -> {
                shutter.toDoubleOrNull()?.let { (it * 1_000_000_000L).toLong() } ?: 8_000_000L
            }
        }
    }

    private fun parseEV(ev: String): Int {
        return ev.toIntOrNull() ?: 0
    }

    private fun parseWhiteBalance(wb: String): Int {
        return wb.removeSuffix("K").toIntOrNull() ?: 5500
    }
}
