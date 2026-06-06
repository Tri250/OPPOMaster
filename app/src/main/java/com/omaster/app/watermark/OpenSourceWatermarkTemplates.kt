package com.omaster.app.watermark

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 开源水印模板库 - 免费商用
 * 参考: Easy Watermark (MIT), Photix Mark (开源), darktable水印模板
 */
object OpenSourceWatermarkTemplates {

    /**
     * 平铺水印模板 - 防盗用
     * 灵感来源: Easy Watermark (MIT协议)
     * 特点: 水印自动重复铺满全图，不留空白死角，大幅提升去除难度
     */
    fun drawTilePatternWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 36f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val textWidth = paint.measureText(text)
        val textHeight = paint.fontMetrics.let { it.bottom - it.top }
        
        // 计算间距
        val horizontalSpacing = (textWidth + 100) * config.scale
        val verticalSpacing = (textHeight + 60) * config.scale
        
        // 旋转角度（通常30-45度效果最佳）
        val rotationAngle = 30f

        canvas.save()
        canvas.rotate(rotationAngle, width / 2f, height / 2f)
        var y = -height.toFloat()
        while (y < height * 2f) {
            var x = -width.toFloat()
            while (x < width * 2f) {
                canvas.drawText(text, x, y, paint)
                x += horizontalSpacing
            }
            y += verticalSpacing
        }
        canvas.restore()
    }

    /**
     * 对角线文字水印 - 版权保护
     * 灵感来源: Photix Mark
     * 特点: 对角线布局，覆盖主体区域，难以裁剪去除
     */
    fun drawDiagonalTextWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 48f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        // 从左下角到右上角的对角线
        val centerX = width / 2f
        val centerY = height / 2f
        val angle = -Math.toDegrees(Math.atan(height.toDouble() / width.toDouble())).toFloat()

        canvas.withRotation(angle, centerX, centerY) {
            val textWidth = paint.measureText(text)
            drawText(text, centerX - textWidth / 2, centerY, paint)
        }
    }

    /**
     * 相机参数水印 - Leica/小米风格
     * 灵感来源: Photix Mark, 小米Leica水印
     * 特点: 显示相机型号、参数、时间等专业信息
     */
    fun drawCameraInfoWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val params = config.cameraParams ?: return
        val padding = 20f * config.scale
        
        // 背景矩形
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (0.6f * 255).toInt()
            style = Paint.Style.FILL
        }
        
        val textPaint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 24f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        
        val smallTextPaint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.8f * 255).toInt()
            textSize = 18f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        // 构建参数文本
        val cameraModel = "OPPO Find X8 Ultra"
        val paramLine1 = "${params.aperture}  ${params.shutter}  ISO ${params.iso}"
        val paramLine2 = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

        // 计算背景大小
        val maxWidth = maxOf(
            textPaint.measureText(cameraModel),
            textPaint.measureText(paramLine1),
            smallTextPaint.measureText(paramLine2)
        )
        val bgWidth = maxWidth + padding * 2
        val bgHeight = 90f * config.scale

        // 绘制背景
        val left = width - bgWidth - padding
        val top = height - bgHeight - padding
        val rect = RectF(left, top, left + bgWidth, top + bgHeight)
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        // 绘制文字
        val textStartX = left + padding
        var textY = top + 30f * config.scale
        canvas.drawText(cameraModel, textStartX, textY, textPaint)
        
        textY += 28f * config.scale
        canvas.drawText(paramLine1, textStartX, textY, textPaint)
        
        textY += 24f * config.scale
        canvas.drawText(paramLine2, textStartX, textY, smallTextPaint)
    }

    /**
     * 日期印章水印 - 证件照专用
     * 灵感来源: Easy Watermark
     * 特点: "本照片仅限XX审核之用，他用无效"
     */
    fun drawDateStampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        purpose: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF4444") // 红色警示
            alpha = (config.opacity * 255).toInt()
            textSize = 28f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val date = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date())
        val text1 = "本照片仅限【$purpose】审核之用"
        val text2 = "他用无效 · $date"

        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制半透明背景
        val bgPaint = Paint().apply {
            color = Color.WHITE
            alpha = (0.9f * 255).toInt()
            style = Paint.Style.FILL
        }
        val bgRect = RectF(
            centerX - 200 * config.scale,
            centerY - 50 * config.scale,
            centerX + 200 * config.scale,
            centerY + 50 * config.scale
        )
        canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)

        // 绘制文字
        canvas.drawText(text1, centerX, centerY - 10 * config.scale, paint)
        paint.textSize = 22f * config.scale
        canvas.drawText(text2, centerX, centerY + 25 * config.scale, paint)
    }

    /**
     * 版权符号水印 - ©️风格
     * 灵感来源: darktable SVG模板
     * 特点: 简洁的版权声明
     */
    fun drawCopyrightSignWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        author: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 32f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val text = "© $year $author"

        val padding = 30f * config.scale
        val x = width - paint.measureText(text) - padding
        val y = height - padding

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 签名水印 - 手写风格
     * 灵感来源: eZy Watermark
     * 特点: 模拟手写签名效果
     */
    fun drawSignatureWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        signature: String,
        config: WatermarkConfig
    ) {
        // 使用斜体模拟手写效果
        val paint = Paint().apply {
            color = Color.parseColor("#333333")
            alpha = (config.opacity * 255).toInt()
            textSize = 40f * config.scale
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val padding = 40f * config.scale
        val x = width - paint.measureText(signature) - padding
        val y = height - padding

        // 轻微旋转模拟手写角度
        canvas.withRotation(-5f, x + paint.measureText(signature) / 2, y) {
            drawText(signature, x, y, paint)
        }
    }

    /**
     * 拼图九宫格水印
     * 灵感来源: Photix Crop
     * 特点: 在图片边缘添加九宫格参考线
     */
    fun drawCollageGridWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.5f * 255).toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        // 三等分线
        val thirdWidth = width / 3f
        val thirdHeight = height / 3f

        // 垂直线
        canvas.drawLine(thirdWidth, 0f, thirdWidth, height.toFloat(), paint)
        canvas.drawLine(thirdWidth * 2, 0f, thirdWidth * 2, height.toFloat(), paint)

        // 水平线
        canvas.drawLine(0f, thirdHeight, width.toFloat(), thirdHeight, paint)
        canvas.drawLine(0f, thirdHeight * 2, width.toFloat(), thirdHeight * 2, paint)
    }

    /**
     * 社交媒体水印
     * 灵感来源: eZy Watermark
     * 特点: 显示社交媒体账号
     */
    fun drawSocialMediaWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        platform: String,
        username: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 26f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val iconPaint = Paint().apply {
            color = when (platform.lowercase()) {
                "instagram" -> Color.parseColor("#E4405F")
                "twitter", "x" -> Color.parseColor("#1DA1F2")
                "weibo" -> Color.parseColor("#E6162D")
                "xiaohongshu" -> Color.parseColor("#FF2442")
                else -> Color.WHITE
            }
            alpha = (config.opacity * 255).toInt()
            textSize = 30f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val text = "@$username"
        val padding = 30f * config.scale
        val x = padding
        val y = height - padding

        // 绘制平台图标
        val icon = when (platform.lowercase()) {
            "instagram" -> "📷"
            "twitter", "x" -> "🐦"
            "weibo" -> "📱"
            "xiaohongshu" -> "📕"
            else -> "🔗"
        }
        canvas.drawText(icon, x, y - 5 * config.scale, iconPaint)
        canvas.drawText(text, x + 40 * config.scale, y, paint)
    }

    /**
     * 极简角标水印
     * 灵感来源: darktable minimal模板
     * 特点: 右下角极简文字
     */
    fun drawMinimalCornerWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 0.6f * 255).toInt()
            textSize = 18f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val padding = 20f * config.scale
        val x = width - paint.measureText(text) - padding
        val y = height - padding

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 邮票邮戳水印 - vivo风格
     * 灵感来源: vivo 2026邮票水印
     * 特点: 复古文艺风格
     */
    fun drawStampWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        location: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#8B4513") // 棕色邮戳色
            alpha = (config.opacity * 255).toInt()
            textSize = 24f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制圆形边框模拟邮票
        val borderPaint = Paint().apply {
            color = Color.parseColor("#8B4513")
            alpha = (config.opacity * 0.5f * 255).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * config.scale
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, 60f * config.scale, borderPaint)

        // 绘制地点和日期
        canvas.drawText(location, centerX, centerY - 10 * config.scale, paint)
        paint.textSize = 16f * config.scale
        canvas.drawText(date, centerX, centerY + 20 * config.scale, paint)
    }

    /**
     * 国风印章水印 - 水墨风格
     * 灵感来源: vivo/荣耀国风水印
     * 特点: 传统印章效果
     */
    fun drawChineseStyleWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#C41E3A") // 中国红
            alpha = (config.opacity * 255).toInt()
            textSize = 32f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val padding = 30f * config.scale
        val x = width - padding - 40 * config.scale
        val y = height - padding

        // 绘制印章方框
        val borderPaint = Paint().apply {
            color = Color.parseColor("#C41E3A")
            alpha = (config.opacity * 0.7f * 255).toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * config.scale
            isAntiAlias = true
        }
        val rect = RectF(x - 30 * config.scale, y - 40 * config.scale, x + 30 * config.scale, y + 10 * config.scale)
        canvas.drawRect(rect, borderPaint)

        // 绘制文字
        canvas.drawText(text, x, y - 10 * config.scale, paint)
    }

    /**
     * 胶片相框水印 - 小米风格
     * 灵感来源: 小米胶片水印
     * 特点: 复古胶片边框
     */
    fun drawFilmFrameWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            alpha = (config.opacity * 255).toInt()
            textSize = 14f * config.scale
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        // 绘制底部胶片信息栏
        val barHeight = 30f * config.scale
        val barPaint = Paint().apply {
            color = Color.BLACK
            alpha = (config.opacity * 0.8f * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - barHeight, width.toFloat(), height.toFloat(), barPaint)

        // 绘制胶片参数
        val params = config.cameraParams
        val text = if (params != null) {
            "f/${params.aperture}  ${params.shutter}  ISO ${params.iso}"
        } else {
            "f/1.8  1/125  ISO 100"
        }
        canvas.drawText(text, 20f * config.scale, height - 10 * config.scale, paint)
    }

    /**
     * 新春舞狮水印 - 小米非遗
     * 灵感来源: 小米17 Ultra新春水印
     * 特点: 舞狮元素，喜庆红金
     */
    fun drawNewYearWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        greeting: String,
        config: WatermarkConfig
    ) {
        val paint = Paint().apply {
            color = Color.parseColor("#FF0000") // 正红色
            alpha = (config.opacity * 255).toInt()
            textSize = 28f * config.scale
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 绘制红色底部边框
        val barHeight = 50f * config.scale
        val barPaint = Paint().apply {
            color = Color.parseColor("#FF0000")
            alpha = (config.opacity * 0.9f * 255).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - barHeight, width.toFloat(), height.toFloat(), barPaint)

        // 绘制新春祝福
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val text = "$greeting · $year"
        paint.color = Color.WHITE
        canvas.drawText(text, width / 2f, height - 15 * config.scale, paint)
    }
}

/**
 * 扩展Canvas旋转功能
 */
private inline fun Canvas.withRotation(
    degrees: Float,
    pivotX: Float = 0f,
    pivotY: Float = 0f,
    block: Canvas.() -> Unit
) {
    save()
    rotate(degrees, pivotX, pivotY)
    block()
    restore()
}
