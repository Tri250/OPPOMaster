package com.omaster.app.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed interface ValidationResult {
    object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

@Serializable
@Parcelize
data class CameraParams(
    val mode: String = "哈苏大师",
    val filter: String = "",
    val iso: Int = 100,
    val shutter: String = "1/200",
    val ev: String = "0",
    val wb: String = "5500K",
    val focal_length: String = "24mm",
    val aperture: String = "f/1.8",
    val hdr: Boolean = false,
    val night_mode: Boolean = false,
    val portrait_mode: Boolean = false,
    val macro_mode: Boolean = false,
    val sports_mode: Boolean = false,
    val ai_optimization: Boolean = true,
    val hasselblad_hncs: Boolean = true,
    val hasselblad_natural_color: Boolean = true,
    val hasselblad_master_style: String = "",
    val color_profile: String = "Natural",
    val sharpness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50,
    val master_tonemap: Boolean = true,
    val vignette: Float = 0.15f,
    val softness: Int = 0,
    val film_simulation: FilmSimulation = FilmSimulation.NONE,
    val exposure_compensation: Float = 0.0f,
    val shutter_priority: Boolean = false,
    val aperture_priority: Boolean = false
) : Parcelable {
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (iso < 50 || iso > 204800) {
            errors.add("ISO 超出范围（应在 50-204800 之间）")
        }
        
        if (!isValidShutterSpeed(shutter)) {
            errors.add("快门格式无效")
        }
        
        if (!isValidExposureValue(ev)) {
            errors.add("EV 格式无效")
        }
        
        if (vignette < 0f || vignette > 1f) {
            errors.add("暗角值超出范围（应在 0-1 之间）")
        }
        
        if (softness < 0 || softness > 100) {
            errors.add("柔焦值超出范围（应在 0-100 之间）")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
    
    private fun isValidShutterSpeed(shutter: String): Boolean {
        val fractionRegex = Regex("^1/\\d+$")
        val numberRegex = Regex("^\\d+(\\.\\d+)?$")
        return fractionRegex.matches(shutter) || numberRegex.matches(shutter)
    }
    
    private fun isValidExposureValue(ev: String): Boolean {
        val evRegex = Regex("^[+-]?\\d+(\\.\\d+)?$")
        return evRegex.matches(ev)
    }
    
    fun formatParamsForDisplay(): String {
        return buildString {
            append("ISO $iso")
            append(" · $shutter")
            if (ev != "0") append(" · EV $ev")
            append(" · $wb")
            if (hasselblad_hncs) append(" · HNCS")
        }
    }
    
    companion object {
        fun createHasselbladNatural(): CameraParams {
            return CameraParams(
                mode = "哈苏大师",
                iso = 100,
                shutter = "1/200",
                ev = "0",
                wb = "5500K",
                focal_length = "24mm",
                aperture = "f/1.8",
                hasselblad_hncs = true,
                hasselblad_natural_color = true,
                color_profile = "Natural",
                sharpness = 45,
                contrast = 50,
                saturation = 45,
                vignette = 0.1f,
                softness = 10,
                film_simulation = FilmSimulation.NONE
            )
        }
        
        fun createHasselbladPortrait(): CameraParams {
            return CameraParams(
                mode = "哈苏大师",
                portrait_mode = true,
                iso = 200,
                shutter = "1/200",
                ev = "+0.3",
                wb = "5000K",
                focal_length = "50mm",
                aperture = "f/1.8",
                hasselblad_hncs = true,
                hasselblad_natural_color = true,
                color_profile = "Portrait",
                sharpness = 40,
                contrast = 48,
                saturation = 48,
                vignette = 0.15f,
                softness = 30,
                film_simulation = FilmSimulation.PORTRAIT
            )
        }
        
        fun createHasselbladNight(): CameraParams {
            return CameraParams(
                mode = "哈苏大师",
                night_mode = true,
                iso = 800,
                shutter = "1/60",
                ev = "-0.3",
                wb = "4000K",
                focal_length = "24mm",
                aperture = "f/1.8",
                hasselblad_hncs = true,
                hasselblad_natural_color = true,
                color_profile = "Night",
                sharpness = 50,
                contrast = 55,
                saturation = 40,
                vignette = 0.2f,
                softness = 5,
                film_simulation = FilmSimulation.NONE
            )
        }
    }
}

@Serializable
enum class FilmSimulation(val displayName: String) {
    NONE("无"),
    PORTAL("Portal"),
    XPRO_V("X-Pro V"),
    CLASSIC_NEG("Classic Neg"),
    ACROS("Acros"),
    VELVIA("Velvia"),
    PROVIA("Provia"),
    ASTIA("Astia"),
    PORTRAIT("Portrait"),
    CINE("Cine"),
    B_W("B&W"),
    SEPIA("Sepia")
}
