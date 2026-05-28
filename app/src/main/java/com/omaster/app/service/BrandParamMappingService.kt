package com.omaster.app.service

import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrandParamMappingService @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) {

    fun getDeviceBrand(): DeviceBrand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()

        return when {
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> DeviceBrand.OPPO
            manufacturer.contains("oneplus") -> DeviceBrand.ONEPLUS
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> DeviceBrand.XIAOMI
            manufacturer.contains("vivo") -> DeviceBrand.VIVO
            manufacturer.contains("huawei") -> DeviceBrand.HUAWEI
            manufacturer.contains("honor") -> DeviceBrand.HONOR
            manufacturer.contains("samsung") -> DeviceBrand.SAMSUNG
            else -> DeviceBrand.OTHER
        }
    }

    fun getBrandCameraPackage(): String {
        return when (getDeviceBrand()) {
            DeviceBrand.OPPO -> "com.oplus.camera"
            DeviceBrand.ONEPLUS -> "com.oneplus.camera"
            DeviceBrand.XIAOMI -> "com.miui.camera"
            DeviceBrand.VIVO -> "com.vivo.camera"
            DeviceBrand.HUAWEI -> "com.huawei.camera"
            DeviceBrand.HONOR -> "com.honor.camera"
            DeviceBrand.SAMSUNG -> "com.samsung.android.camera"
            DeviceBrand.OTHER -> "com.android.camera"
        }
    }

    fun getParamMapping(): Map<String, String> {
        return when (getDeviceBrand()) {
            DeviceBrand.OPPO -> oppoParamMap
            DeviceBrand.ONEPLUS -> oneplusParamMap
            DeviceBrand.XIAOMI -> xiaomiParamMap
            DeviceBrand.VIVO -> vivoParamMap
            DeviceBrand.HUAWEI -> huaweiParamMap
            DeviceBrand.HONOR -> honorParamMap
            DeviceBrand.SAMSUNG -> samsungParamMap
            DeviceBrand.OTHER -> defaultParamMap
        }
    }

    fun getParamRange(paramKey: String): ParamRange {
        val brand = getDeviceBrand()
        
        return when (paramKey) {
            "iso" -> when (brand) {
                DeviceBrand.OPPO, DeviceBrand.ONEPLUS -> ParamRange(50, 25600, 100)
                DeviceBrand.XIAOMI -> ParamRange(100, 6400, 100)
                DeviceBrand.VIVO -> ParamRange(50, 12800, 100)
                DeviceBrand.HUAWEI, DeviceBrand.HONOR -> ParamRange(50, 51200, 100)
                DeviceBrand.SAMSUNG -> ParamRange(50, 25600, 100)
                else -> ParamRange(100, 3200, 100)
            }
            "shutter" -> ParamRange(1, 30000, 1)
            "ev" -> ParamRange(-3, 3, 0.3f)
            "wb" -> when (brand) {
                DeviceBrand.OPPO, DeviceBrand.ONEPLUS -> ParamRange(2000, 10000, 5500)
                DeviceBrand.XIAOMI -> ParamRange(2700, 10000, 5200)
                DeviceBrand.VIVO -> ParamRange(2000, 8000, 5500)
                DeviceBrand.HUAWEI, DeviceBrand.HONOR -> ParamRange(2000, 10000, 5300)
                DeviceBrand.SAMSUNG -> ParamRange(2800, 10000, 5500)
                else -> ParamRange(2000, 10000, 5500)
            }
            else -> ParamRange(0, 100, 50)
        }
    }

    fun isMasterModeSupported(): Boolean {
        return when (getDeviceBrand()) {
            DeviceBrand.OPPO, DeviceBrand.ONEPLUS, DeviceBrand.XIAOMI, 
            DeviceBrand.VIVO, DeviceBrand.HUAWEI, DeviceBrand.HONOR, DeviceBrand.SAMSUNG -> true
            else -> false
        }
    }

    fun getMasterModeName(): String {
        return when (getDeviceBrand()) {
            DeviceBrand.OPPO -> "大师模式"
            DeviceBrand.ONEPLUS -> "哈苏模式"
            DeviceBrand.XIAOMI -> "徕卡模式"
            DeviceBrand.VIVO -> "蔡司模式"
            DeviceBrand.HUAWEI -> "XMAGE模式"
            DeviceBrand.HONOR -> "鹰眼模式"
            DeviceBrand.SAMSUNG -> "专业模式"
            else -> "专业模式"
        }
    }

    fun mapUniversalParam(universalParam: UniversalParam): BrandSpecificParam {
        val brand = getDeviceBrand()
        val mappedWb = when {
            universalParam.wb >= 6000 -> "cool"
            universalParam.wb <= 4500 -> "warm"
            else -> "auto"
        }

        return BrandSpecificParam(
            iso = universalParam.iso,
            shutter = formatShutter(universalParam.shutter),
            ev = universalParam.ev,
            wb = when (brand) {
                DeviceBrand.XIAOMI -> mappedWb
                else -> universalParam.wb.toString() + "K"
            },
            filter = mapFilter(universalParam.filter)
        )
    }

    private fun formatShutter(shutter: Float): String {
        return if (shutter < 1) {
            "1/${(1 / shutter).toInt()}"
        } else {
            shutter.toString()
        }
    }

    private fun mapFilter(universalFilter: String): String {
        return when (universalFilter.lowercase()) {
            "vintage", "retro", "复古" -> "复古"
            "film", "胶片" -> "胶片"
            "fresh", "清新" -> "清新"
            "blackwhite", "黑白" -> "黑白"
            "warm", "暖调" -> "暖色"
            "cool", "冷调" -> "冷色"
            "vibrant", "鲜艳" -> "鲜艳"
            "natural", "自然" -> "标准"
            else -> universalFilter
        }
    }

    private val oppoParamMap = mapOf(
        "iso" to "com.oplus.camera:id/iso_value",
        "shutter" to "com.oplus.camera:id/shutter_speed_value",
        "wb" to "com.oplus.camera:id/wb_value",
        "ev" to "com.oplus.camera:id/ev_value",
        "filter" to "com.oplus.camera:id/filter_value"
    )

    private val oneplusParamMap = mapOf(
        "iso" to "com.oneplus.camera:id/iso_value",
        "shutter" to "com.oneplus.camera:id/shutter_speed_value",
        "wb" to "com.oneplus.camera:id/wb_value",
        "ev" to "com.oneplus.camera:id/ev_value",
        "filter" to "com.oneplus.camera:id/filter_value"
    )

    private val xiaomiParamMap = mapOf(
        "iso" to "com.miui.camera:id/iso_selector",
        "shutter" to "com.miui.camera:id/shutter_selector",
        "wb" to "com.miui.camera:id/wb_selector",
        "ev" to "com.miui.camera:id/ev_selector",
        "filter" to "com.miui.camera:id/filter_selector"
    )

    private val vivoParamMap = mapOf(
        "iso" to "com.vivo.camera:id/iso_value",
        "shutter" to "com.vivo.camera:id/shutter_speed_value",
        "wb" to "com.vivo.camera:id/wb_value",
        "ev" to "com.vivo.camera:id/ev_value",
        "filter" to "com.vivo.camera:id/filter_value"
    )

    private val huaweiParamMap = mapOf(
        "iso" to "com.huawei.camera:id/iso_value",
        "shutter" to "com.huawei.camera:id/shutter_speed_value",
        "wb" to "com.huawei.camera:id/wb_value",
        "ev" to "com.huawei.camera:id/ev_value",
        "filter" to "com.huawei.camera:id/filter_value"
    )

    private val honorParamMap = mapOf(
        "iso" to "com.honor.camera:id/iso_value",
        "shutter" to "com.honor.camera:id/shutter_speed_value",
        "wb" to "com.honor.camera:id/wb_value",
        "ev" to "com.honor.camera:id/ev_value",
        "filter" to "com.honor.camera:id/filter_value"
    )

    private val samsungParamMap = mapOf(
        "iso" to "com.samsung.android.camera:id/iso_value",
        "shutter" to "com.samsung.android.camera:id/shutter_speed_value",
        "wb" to "com.samsung.android.camera:id/wb_value",
        "ev" to "com.samsung.android.camera:id/ev_value",
        "filter" to "com.samsung.android.camera:id/filter_value"
    )

    private val defaultParamMap = mapOf(
        "iso" to "iso",
        "shutter" to "shutter",
        "wb" to "wb",
        "ev" to "ev",
        "filter" to "filter"
    )

    enum class DeviceBrand {
        OPPO,
        ONEPLUS,
        XIAOMI,
        VIVO,
        HUAWEI,
        HONOR,
        SAMSUNG,
        OTHER
    }

    data class ParamRange(
        val min: Number,
        val max: Number,
        val default: Number
    )

    data class UniversalParam(
        val iso: Int,
        val shutter: Float,
        val ev: Float,
        val wb: Int,
        val filter: String
    )

    data class BrandSpecificParam(
        val iso: Int,
        val shutter: String,
        val ev: Float,
        val wb: String,
        val filter: String
    )
}