package com.omasterpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import androidpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStylepackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  //package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMALpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String =package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate:package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrandpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        valpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
        bitmap: Bitmap,
        config: WatermarkConfig = WatermarkConfig()
    ):package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
        bitmap: Bitmap,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_888package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
        bitmap: Bitmap,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        when (config.style) {package com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
        bitmap: Bitmap,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        when (config.style) {
            WatermarkStyle.SIMPLE -> drawSimpleWatermark(canvas, config, bitmap.widthpackage com.omaster.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 水印工具类
 * 提供多种水印模板和品牌标识功能
 */
@Singleton
class WatermarkUtil @Inject constructor() {

    enum class WatermarkStyle {
        SIMPLE,        // 简单文字水印
        PROFESSIONAL,  // 专业摄影水印
        BRANDED,       // 品牌标识水印
        MINIMAL,       // 极简风格
        DATE_TIME      // 日期时间水印
    }

    data class WatermarkConfig(
        val style: WatermarkStyle = WatermarkStyle.SIMPLE,
        val text: String = "OMaster",
        val showDate: Boolean = true,
        val showCameraParams: Boolean = false,
        val showBrand: Boolean = true,
        val position: Position = Position.BOTTOM_RIGHT,
        val opacity: Int = 180,
        val textSize: Float = 36f
    )

    enum class Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 为图片添加水印
     */
    fun addWatermark(
        bitmap: Bitmap,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        when (config.style) {
            WatermarkStyle.SIMPLE -> drawSimpleWatermark(canvas, config, bitmap.width, bitmap.height)
            WatermarkStyle.PROFESSIONAL -> drawProfessionalWatermark(