package com.omaster.app.accessibility

object DeviceMappingTable {
    
    data class CameraAppInfo(
        val packageName: String,
        val brand: String,
        val model: String,
        val paramMapping: ParamMapping
    )
    
    data class ParamMapping(
        val isoIds: List<String>,
        val shutterIds: List<String>,
        val evIds: List<String>,
        val wbIds: List<String>,
        val focusIds: List<String>,
        val additionalParams: Map<String, List<String>>
    )
    
    val cameraApps = listOf(
        
        // ==================== OPPO 系列 ====================
        CameraAppInfo(
            packageName = "com.oppo.camera",
            brand = "OPPO",
            model = "Find X系列/Reno系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.oppo.camera:id/iso_value",
                    "com.oppo.camera:id/iso",
                    "iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.oppo.camera:id/shutter_value",
                    "com.oppo.camera:id/shutter",
                    "shutter_value",
                    "shutter",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.oppo.camera:id/ev_value",
                    "com.oppo.camera:id/ev",
                    "ev_value",
                    "ev",
                    "曝光补偿",
                    "EV"
                ),
                wbIds = listOf(
                    "com.oppo.camera:id/wb_value",
                    "com.oppo.camera:id/wb",
                    "wb_value",
                    "wb",
                    "白平衡",
                    "WB"
                ),
                focusIds = listOf(
                    "com.oppo.camera:id/focus_value",
                    "com.oppo.camera:id/focus",
                    "focus_value",
                    "focus",
                    "对焦",
                    "AF"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast_value", "contrast", "对比度"),
                    "saturation" to listOf("saturation_value", "saturation", "饱和度"),
                    "sharpness" to listOf("sharpness_value", "sharpness", "锐度"),
                    "vignette" to listOf("vignette_value", "vignette", "暗角")
                )
            )
        ),
        
        // ==================== 一加 系列 ====================
        CameraAppInfo(
            packageName = "com.oneplus.camera",
            brand = "OnePlus",
            model = "一加12/一加11/一加Ace系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.oneplus.camera:id/iso_value",
                    "com.oneplus.camera:id/iso",
                    "iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.oneplus.camera:id/shutter_value",
                    "com.oneplus.camera:id/shutter",
                    "shutter_value",
                    "shutter",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.oneplus.camera:id/ev_value",
                    "com.oneplus.camera:id/ev",
                    "ev_value",
                    "ev",
                    "曝光补偿",
                    "EV"
                ),
                wbIds = listOf(
                    "com.oneplus.camera:id/wb_value",
                    "com.oneplus.camera:id/wb",
                    "wb_value",
                    "wb",
                    "白平衡",
                    "WB"
                ),
                focusIds = listOf(
                    "com.oneplus.camera:id/focus_value",
                    "com.oneplus.camera:id/focus",
                    "focus_value",
                    "focus",
                    "对焦",
                    "AF"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast_value", "contrast", "对比度"),
                    "saturation" to listOf("saturation_value", "saturation", "饱和度"),
                    "sharpness" to listOf("sharpness_value", "sharpness", "锐度")
                )
            )
        ),
        
        // ==================== realme 系列 ====================
        CameraAppInfo(
            packageName = "com.realme.camera",
            brand = "realme",
            model = "真我GT/真我数字系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.realme.camera:id/iso_value",
                    "com.realme.camera:id/iso",
                    "iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.realme.camera:id/shutter_value",
                    "com.realme.camera:id/shutter",
                    "shutter_value",
                    "shutter",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.realme.camera:id/ev_value",
                    "com.realme.camera:id/ev",
                    "ev_value",
                    "ev",
                    "曝光补偿",
                    "EV"
                ),
                wbIds = listOf(
                    "com.realme.camera:id/wb_value",
                    "com.realme.camera:id/wb",
                    "wb_value",
                    "wb",
                    "白平衡",
                    "WB"
                ),
                focusIds = listOf(
                    "com.realme.camera:id/focus_value",
                    "com.realme.camera:id/focus",
                    "focus_value",
                    "focus",
                    "对焦",
                    "AF"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast_value", "contrast", "对比度"),
                    "saturation" to listOf("saturation_value", "saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 小米/红米 系列 ====================
        CameraAppInfo(
            packageName = "com.android.camera",
            brand = "Xiaomi",
            model = "小米14/小米13/红米K系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.android.camera:id/iso",
                    "com.android.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度",
                    "感光度值"
                ),
                shutterIds = listOf(
                    "com.android.camera:id/shutter",
                    "com.android.camera:id/shutter_speed",
                    "com.android.camera:id/shutter_value",
                    "shutter",
                    "shutter_speed",
                    "快门速度",
                    "快门",
                    "S",
                    "s"
                ),
                evIds = listOf(
                    "com.android.camera:id/ev",
                    "com.android.camera:id/ev_value",
                    "ev",
                    "EV",
                    "曝光补偿",
                    "曝光"
                ),
                wbIds = listOf(
                    "com.android.camera:id/wb",
                    "com.android.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡",
                    "白平衡值"
                ),
                focusIds = listOf(
                    "com.android.camera:id/focus",
                    "com.android.camera:id/focus_value",
                    "com.android.camera:id/af",
                    "focus",
                    "AF",
                    "对焦",
                    "对焦模式"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "contrast_value", "对比度"),
                    "saturation" to listOf("saturation", "saturation_value", "饱和度"),
                    "sharpness" to listOf("sharpness", "sharpness_value", "锐度"),
                    "exposure" to listOf("exposure", "exposure_value", "曝光")
                )
            )
        ),
        
        CameraAppInfo(
            packageName = "com.miui.camera",
            brand = "Xiaomi",
            model = "小米/红米 MIUI系统",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.miui.camera:id/iso",
                    "com.miui.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.miui.camera:id/shutter",
                    "com.miui.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.miui.camera:id/ev",
                    "com.miui.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.miui.camera:id/wb",
                    "com.miui.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.miui.camera:id/focus",
                    "com.miui.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== vivo/iQOO 系列 ====================
        CameraAppInfo(
            packageName = "com.vivo.camera",
            brand = "vivo",
            model = "X系列/S系列/iQOO系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.vivo.camera:id/iso",
                    "com.vivo.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.vivo.camera:id/shutter",
                    "com.vivo.camera:id/shutter_value",
                    "com.vivo.camera:id/shutter_speed",
                    "shutter",
                    "shutter_speed",
                    "快门速度",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.vivo.camera:id/ev",
                    "com.vivo.camera:id/ev_value",
                    "ev",
                    "EV",
                    "曝光补偿",
                    "曝光"
                ),
                wbIds = listOf(
                    "com.vivo.camera:id/wb",
                    "com.vivo.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡",
                    "白平衡值"
                ),
                focusIds = listOf(
                    "com.vivo.camera:id/focus",
                    "com.vivo.camera:id/focus_value",
                    "com.vivo.camera:id/af",
                    "focus",
                    "AF",
                    "对焦",
                    "对焦模式"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "contrast_value", "对比度"),
                    "saturation" to listOf("saturation", "saturation_value", "饱和度"),
                    "sharpness" to listOf("sharpness", "锐度")
                )
            )
        ),
        
        CameraAppInfo(
            packageName = "com.iqoo.camera",
            brand = "iQOO",
            model = "iQOO 12/iQOO 11/iQOO Neo系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.iqoo.camera:id/iso",
                    "com.iqoo.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.iqoo.camera:id/shutter",
                    "com.iqoo.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.iqoo.camera:id/ev",
                    "com.iqoo.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.iqoo.camera:id/wb",
                    "com.iqoo.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.iqoo.camera:id/focus",
                    "com.iqoo.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 华为 系列 ====================
        CameraAppInfo(
            packageName = "com.huawei.camera",
            brand = "Huawei",
            model = "Mate系列/P系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.huawei.camera:id/iso",
                    "com.huawei.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度",
                    "感光度值"
                ),
                shutterIds = listOf(
                    "com.huawei.camera:id/shutter",
                    "com.huawei.camera:id/shutter_speed",
                    "com.huawei.camera:id/shutter_value",
                    "shutter",
                    "shutter_speed",
                    "快门速度",
                    "快门",
                    "S",
                    "s"
                ),
                evIds = listOf(
                    "com.huawei.camera:id/ev",
                    "com.huawei.camera:id/ev_value",
                    "com.huawei.camera:id/exposure",
                    "ev",
                    "EV",
                    "曝光补偿",
                    "曝光"
                ),
                wbIds = listOf(
                    "com.huawei.camera:id/wb",
                    "com.huawei.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡",
                    "白平衡值"
                ),
                focusIds = listOf(
                    "com.huawei.camera:id/focus",
                    "com.huawei.camera:id/focus_value",
                    "com.huawei.camera:id/af",
                    "focus",
                    "AF",
                    "对焦",
                    "对焦模式"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "contrast_value", "对比度"),
                    "saturation" to listOf("saturation", "saturation_value", "饱和度"),
                    "sharpness" to listOf("sharpness", "sharpness_value", "锐度")
                )
            )
        ),
        
        CameraAppInfo(
            packageName = "com.huawei.systemcamera",
            brand = "Huawei",
            model = "华为 EMUI 13+",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.huawei.systemcamera:id/iso",
                    "com.huawei.systemcamera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.huawei.systemcamera:id/shutter",
                    "com.huawei.systemcamera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.huawei.systemcamera:id/ev",
                    "com.huawei.systemcamera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.huawei.systemcamera:id/wb",
                    "com.huawei.systemcamera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.huawei.systemcamera:id/focus",
                    "com.huawei.systemcamera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 荣耀 系列 ====================
        CameraAppInfo(
            packageName = "com.hihonor.camera",
            brand = "HONOR",
            model = "荣耀Magic/荣耀数字系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.hihonor.camera:id/iso",
                    "com.hihonor.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.hihonor.camera:id/shutter",
                    "com.hihonor.camera:id/shutter_speed",
                    "com.hihonor.camera:id/shutter_value",
                    "shutter",
                    "shutter_speed",
                    "快门速度",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.hihonor.camera:id/ev",
                    "com.hihonor.camera:id/ev_value",
                    "ev",
                    "EV",
                    "曝光补偿",
                    "曝光"
                ),
                wbIds = listOf(
                    "com.hihonor.camera:id/wb",
                    "com.hihonor.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.hihonor.camera:id/focus",
                    "com.hihonor.camera:id/focus_value",
                    "com.hihonor.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "contrast_value", "对比度"),
                    "saturation" to listOf("saturation", "saturation_value", "饱和度"),
                    "sharpness" to listOf("sharpness", "锐度")
                )
            )
        ),
        
        // ==================== 魅族 系列 ====================
        CameraAppInfo(
            packageName = "com.meizu.camera",
            brand = "Meizu",
            model = "魅族20/魅族18/魅族17系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.meizu.camera:id/iso",
                    "com.meizu.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.meizu.camera:id/shutter",
                    "com.meizu.camera:id/shutter_speed",
                    "com.meizu.camera:id/shutter_value",
                    "shutter",
                    "快门速度",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.meizu.camera:id/ev",
                    "com.meizu.camera:id/ev_value",
                    "ev",
                    "EV",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.meizu.camera:id/wb",
                    "com.meizu.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.meizu.camera:id/focus",
                    "com.meizu.camera:id/focus_value",
                    "com.meizu.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 中兴/努比亚 系列 ====================
        CameraAppInfo(
            packageName = "com.zte.camera",
            brand = "ZTE",
            model = "中兴Axon/努比亚Z系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.zte.camera:id/iso",
                    "com.zte.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.zte.camera:id/shutter",
                    "com.zte.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.zte.camera:id/ev",
                    "com.zte.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.zte.camera:id/wb",
                    "com.zte.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.zte.camera:id/focus",
                    "com.zte.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        CameraAppInfo(
            packageName = "com.nubia.camera",
            brand = "Nubia",
            model = "努比亚红魔/努比亚Z系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.nubia.camera:id/iso",
                    "com.nubia.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.nubia.camera:id/shutter",
                    "com.nubia.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.nubia.camera:id/ev",
                    "com.nubia.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.nubia.camera:id/wb",
                    "com.nubia.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.nubia.camera:id/focus",
                    "com.nubia.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 联想/摩托罗拉 系列 ====================
        CameraAppInfo(
            packageName = "com.lenovo.camera",
            brand = "Lenovo",
            model = "联想拯救者/联想Pro系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.lenovo.camera:id/iso",
                    "com.lenovo.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.lenovo.camera:id/shutter",
                    "com.lenovo.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.lenovo.camera:id/ev",
                    "com.lenovo.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.lenovo.camera:id/wb",
                    "com.lenovo.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.lenovo.camera:id/focus",
                    "com.lenovo.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        CameraAppInfo(
            packageName = "com.motorola.camera",
            brand = "Motorola",
            model = "Motorola Edge/razr系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.motorola.camera:id/iso",
                    "com.motorola.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.motorola.camera:id/shutter",
                    "com.motorola.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.motorola.camera:id/ev",
                    "com.motorola.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.motorola.camera:id/wb",
                    "com.motorola.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.motorola.camera:id/focus",
                    "com.motorola.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 三星 系列 ====================
        CameraAppInfo(
            packageName = "com.samsung.android.camera",
            brand = "Samsung",
            model = "Galaxy S系列/Galaxy Z系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.samsung.android.camera:id/iso",
                    "com.samsung.android.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "ISO值",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.samsung.android.camera:id/shutter",
                    "com.samsung.android.camera:id/shutter_speed",
                    "com.samsung.android.camera:id/shutter_value",
                    "shutter",
                    "shutter_speed",
                    "快门速度",
                    "快门",
                    "S"
                ),
                evIds = listOf(
                    "com.samsung.android.camera:id/ev",
                    "com.samsung.android.camera:id/ev_value",
                    "com.samsung.android.camera:id/exposure",
                    "ev",
                    "EV",
                    "曝光补偿",
                    "曝光"
                ),
                wbIds = listOf(
                    "com.samsung.android.camera:id/wb",
                    "com.samsung.android.camera:id/wb_value",
                    "wb",
                    "WB",
                    "白平衡",
                    "白平衡值"
                ),
                focusIds = listOf(
                    "com.samsung.android.camera:id/focus",
                    "com.samsung.android.camera:id/focus_value",
                    "com.samsung.android.camera:id/af",
                    "focus",
                    "AF",
                    "对焦",
                    "对焦模式"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "contrast_value", "对比度"),
                    "saturation" to listOf("saturation", "saturation_value", "饱和度"),
                    "sharpness" to listOf("sharpness", "sharpness_value", "锐度")
                )
            )
        ),
        
        // ==================== TCL 系列 ====================
        CameraAppInfo(
            packageName = "com.tcl.camera",
            brand = "TCL",
            model = "TCL 20/TCL Plex系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.tcl.camera:id/iso",
                    "com.tcl.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.tcl.camera:id/shutter",
                    "com.tcl.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.tcl.camera:id/ev",
                    "com.tcl.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.tcl.camera:id/wb",
                    "com.tcl.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.tcl.camera:id/focus",
                    "com.tcl.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 海信 系列 ====================
        CameraAppInfo(
            packageName = "com.hisense.camera",
            brand = "Hisense",
            model = "海信金刚系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.hisense.camera:id/iso",
                    "com.hisense.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.hisense.camera:id/shutter",
                    "com.hisense.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.hisense.camera:id/ev",
                    "com.hisense.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.hisense.camera:id/wb",
                    "com.hisense.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.hisense.camera:id/focus",
                    "com.hisense.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 酷派 系列 ====================
        CameraAppInfo(
            packageName = "com.coolpad.camera",
            brand = "Coolpad",
            model = "酷派Cool系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.coolpad.camera:id/iso",
                    "com.coolpad.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.coolpad.camera:id/shutter",
                    "com.coolpad.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.coolpad.camera:id/ev",
                    "com.coolpad.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.coolpad.camera:id/wb",
                    "com.coolpad.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.coolpad.camera:id/focus",
                    "com.coolpad.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        ),
        
        // ==================== 锤子/坚果 系列 ====================
        CameraAppInfo(
            packageName = "com.smartisan.camera",
            brand = "Smartisan",
            model = "坚果Pro/坚果R系列",
            paramMapping = ParamMapping(
                isoIds = listOf(
                    "com.smartisan.camera:id/iso",
                    "com.smartisan.camera:id/iso_value",
                    "iso",
                    "ISO",
                    "感光度"
                ),
                shutterIds = listOf(
                    "com.smartisan.camera:id/shutter",
                    "com.smartisan.camera:id/shutter_speed",
                    "shutter",
                    "快门速度",
                    "S"
                ),
                evIds = listOf(
                    "com.smartisan.camera:id/ev",
                    "com.smartisan.camera:id/ev_value",
                    "ev",
                    "曝光补偿"
                ),
                wbIds = listOf(
                    "com.smartisan.camera:id/wb",
                    "com.smartisan.camera:id/wb_value",
                    "wb",
                    "白平衡"
                ),
                focusIds = listOf(
                    "com.smartisan.camera:id/focus",
                    "com.smartisan.camera:id/af",
                    "focus",
                    "AF",
                    "对焦"
                ),
                additionalParams = mapOf(
                    "contrast" to listOf("contrast", "对比度"),
                    "saturation" to listOf("saturation", "饱和度")
                )
            )
        )
    )
    
    fun getPackageNames(): Set<String> {
        return cameraApps.map { it.packageName }.toSet()
    }
    
    fun getCameraApp(packageName: String): CameraAppInfo? {
        return cameraApps.find { it.packageName == packageName }
    }
    
    fun getCameraAppByBrand(brand: String): List<CameraAppInfo> {
        return cameraApps.filter { it.brand.equals(brand, ignoreCase = true) }
    }
}
