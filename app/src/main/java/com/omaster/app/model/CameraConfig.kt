package com.omaster.app.model

import kotlinx.serialization.Serializable

/**
 * 相机配置数据类 - 企业级实现
 * 所有数据来自真实用户输入或远程服务器，不使用模拟数据
 */
@Serializable
data class CameraConfig(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val brand: String = "",
    val model: String = "",
    val iso: ISO = ISO.AUTO,
    val shutterSpeed: ShutterSpeed = ShutterSpeed.AUTO,
    val aperture: Aperture = Aperture.AUTO,
    val ev: EV = EV.ZERO,
    val wb: WB = WB.AUTO,
    val focusMode: FocusMode = FocusMode.AUTO,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false,
    val source: String = "user", // user, remote, import
    val appVersion: String = "小O帮帮 3.0",
    val author: String = "",
    val tags: List<String> = emptyList()
) {
    /**
     * 获取格式化创建时间
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(createdAt))
    }

    /**
     * 获取配置摘要
     */
    fun getSummary(): String {
        return buildString {
            append("ISO: ${iso.value}, ")
            append("快门: ${shutterSpeed.value}, ")
            append("光圈: ${aperture.value}, ")
            append("EV: ${ev.value}, ")
            append("WB: ${wb.value}")
        }
    }

    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && brand.isNotBlank()
    }
}

@Serializable
enum class ISO(val value: String, val description: String) {
    AUTO("AUTO", "自动"),
    ISO_50("50", "ISO 50"),
    ISO_100("100", "ISO 100"),
    ISO_200("200", "ISO 200"),
    ISO_400("400", "ISO 400"),
    ISO_800("800", "ISO 800"),
    ISO_1600("1600", "ISO 1600"),
    ISO_3200("3200", "ISO 3200"),
    ISO_6400("6400", "ISO 6400");

    companion object {
        fun fromValue(value: String): ISO {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}

@Serializable
enum class ShutterSpeed(val value: String, val description: String) {
    AUTO("AUTO", "自动"),
    S_1_8000("1/8000", "1/8000秒"),
    S_1_4000("1/4000", "1/4000秒"),
    S_1_2000("1/2000", "1/2000秒"),
    S_1_1000("1/1000", "1/1000秒"),
    S_1_500("1/500", "1/500秒"),
    S_1_250("1/250", "1/250秒"),
    S_1_125("1/125", "1/125秒"),
    S_1_60("1/60", "1/60秒"),
    S_1_30("1/30", "1/30秒"),
    S_1_15("1/15", "1/15秒"),
    S_1_8("1/8", "1/8秒"),
    S_1_4("1/4", "1/4秒"),
    S_1_2("1/2", "1/2秒"),
    S_1("1", "1秒"),
    S_2("2", "2秒"),
    S_4("4", "4秒"),
    S_8("8", "8秒"),
    S_15("15", "15秒"),
    S_30("30", "30秒");

    companion object {
        fun fromValue(value: String): ShutterSpeed {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}

@Serializable
enum class Aperture(val value: String, val description: String) {
    AUTO("AUTO", "自动"),
    F_1_4("f/1.4", "f/1.4"),
    F_1_8("f/1.8", "f/1.8"),
    F_2_0("f/2.0", "f/2.0"),
    F_2_8("f/2.8", "f/2.8"),
    F_4_0("f/4.0", "f/4.0"),
    F_5_6("f/5.6", "f/5.6"),
    F_8_0("f/8.0", "f/8.0"),
    F_11("f/11", "f/11"),
    F_16("f/16", "f/16"),
    F_22("f/22", "f/22");

    companion object {
        fun fromValue(value: String): Aperture {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}

@Serializable
enum class EV(val value: String, val description: String) {
    NEG_3("-3.0", "-3.0 EV"),
    NEG_2_7("-2.7", "-2.7 EV"),
    NEG_2_3("-2.3", "-2.3 EV"),
    NEG_2("-2.0", "-2.0 EV"),
    NEG_1_7("-1.7", "-1.7 EV"),
    NEG_1_3("-1.3", "-1.3 EV"),
    NEG_1("-1.0", "-1.0 EV"),
    NEG_0_7("-0.7", "-0.7 EV"),
    NEG_0_3("-0.3", "-0.3 EV"),
    ZERO("0.0", "0.0 EV"),
    POS_0_3("+0.3", "+0.3 EV"),
    POS_0_7("+0.7", "+0.7 EV"),
    POS_1("+1.0", "+1.0 EV"),
    POS_1_3("+1.3", "+1.3 EV"),
    POS_1_7("+1.7", "+1.7 EV"),
    POS_2("+2.0", "+2.0 EV"),
    POS_2_3("+2.3", "+2.3 EV"),
    POS_2_7("+2.7", "+2.7 EV"),
    POS_3("+3.0", "+3.0 EV");

    companion object {
        fun fromValue(value: String): EV {
            return entries.find { it.value == value } ?: ZERO
        }
    }
}

@Serializable
enum class WB(val value: String, val description: String, val temperature: Int) {
    AUTO("AUTO", "自动", 0),
    DAYLIGHT("日光", "日光 5200K", 5200),
    CLOUDY("阴天", "阴天 6000K", 6000),
    SHADE("阴影", "阴影 7000K", 7000),
    TUNGSTEN("钨丝灯", "钨丝灯 3200K", 3200),
    FLUORESCENT("荧光灯", "荧光灯 4000K", 4000),
    FLASH("闪光灯", "闪光灯 5500K", 5500),
    CUSTOM("自定义", "自定义", 0);

    companion object {
        fun fromValue(value: String): WB {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}

@Serializable
enum class FocusMode(val value: String, val description: String) {
    AUTO("AUTO", "自动对焦"),
    MANUAL("MANUAL", "手动对焦"),
    CONTINUOUS("CONTINUOUS", "连续对焦"),
    SINGLE("SINGLE", "单次对焦"),
    MACRO("MACRO", "微距对焦");

    companion object {
        fun fromValue(value: String): FocusMode {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}
