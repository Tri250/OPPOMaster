package com.omaster.app.service

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.omaster.app.R
import com.omaster.app.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CapsuleHandle(val id: String)

data class FluidCloudData(
    val presetId: String,
    val title: String,
    val subtitle: String,
    val isoText: String,
    val shutterText: String,
    val evText: String,
    val contrastText: String,
    val saturationText: String,
    val wbText: String,
    val vignetteText: String,
    val ratingText: String,
    val usageText: String,
    val deviceText: String,
    val iconPath: String,
    val coverPath: String,
    val bgGradient: String,
    val borderColor: String,
    val showApplyBtn: Boolean = true,
    val showDetailBtn: Boolean = true,
    val showParams: Boolean = true
)

class FluidCloudCapsuleManager(private val context: Context) {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var currentCapsule: View? = null
    private var currentPreset: Preset? = null
    private var currentData: FluidCloudData? = null
    private val capsules = mutableListOf<Pair<Preset, View>>()
    private var isExpanded = false

    fun createFluidCloudData(preset: Preset): FluidCloudData {
        val params = preset.cameraParams
        return FluidCloudData(
            presetId = preset.id,
            title = preset.name,
            subtitle = "ISO ${params?.iso} | ${params?.shutter}",
            isoText = "${params?.iso ?: 100}",
            shutterText = params?.shutter ?: "1/125",
            evText = params?.ev ?: "0",
            contrastText = String.format("%.1f", params?.contrast ?: 1.0f),
            saturationText = String.format("%.1f", params?.saturation ?: 1.0f),
            wbText = params?.wb ?: "5500K",
            vignetteText = String.format("%.2f", params?.vignette ?: 0.0f),
            ratingText = String.format("%.1f", preset.rating),
            usageText = "${preset.usageCount}次使用",
            deviceText = preset.deviceModel,
            iconPath = preset.coverPath,
            coverPath = preset.coverPath,
            bgGradient = "linear-gradient(180deg,#6366F1,#8B5CF6)",
            borderColor = "#8B5CF6",
            showApplyBtn = true,
            showDetailBtn = true,
            showParams = true
        )
    }

    suspend fun createCapsule(preset: Preset): Result<CapsuleHandle> {
        return withContext(Dispatchers.Main) {
            try {
                removeCurrentCapsule()

                val capsuleData = createFluidCloudData(preset)
                currentData = capsuleData
                val capsuleView = createCapsuleView(preset)

                val params = WindowManager.LayoutParams().apply {
                    type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                    format = android.graphics.PixelFormat.TRANSLUCENT
                    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    width = WindowManager.LayoutParams.WRAP_CONTENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    gravity = Gravity.TOP or Gravity.END
                    x = 20
                    y = 100
                }

                windowManager.addView(capsuleView, params)

                currentCapsule = capsuleView
                currentPreset = preset
                capsules.add(preset to capsuleView)

                Result.success(CapsuleHandle(preset.id))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun createCapsuleView(preset: Preset): View {
        val view = LayoutInflater.from(context).inflate(R.layout.fluid_cloud_capsule, null)
        val data = currentData ?: createFluidCloudData(preset)

        view.findViewById<TextView>(R.id.capsule_title).text = data.title
        view.findViewById<TextView>(R.id.capsule_subtitle).text = data.subtitle

        val bgColor = parseGradientColor(data.bgGradient)
        view.setBackgroundColor(bgColor)

        view.setOnClickListener {
            expandWithAnimation(preset)
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0f
            private var initialY = 0f
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: android.view.MotionEvent?): Boolean {
                when (event?.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = (v?.layoutParams as? WindowManager.LayoutParams)?.x?.toFloat() ?: 0f
                        initialY = (v?.layoutParams as? WindowManager.LayoutParams)?.y?.toFloat() ?: 0f
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val params = v?.layoutParams as? WindowManager.LayoutParams
                        params?.let {
                            it.x = (initialX + (event.rawX - initialTouchX)).toInt()
                            it.y = (initialY + (event.rawY - initialTouchY)).toInt()
                            windowManager.updateViewLayout(v, it)
                        }
                        return true
                    }
                }
                return false
            }
        })

        return view
    }

    private fun parseGradientColor(gradient: String): Int {
        return try {
            if (gradient.startsWith("linear-gradient")) {
                val colors = gradient
                    .replace("linear-gradient(", "")
                    .replace(")", "")
                    .split(",")
                    .map { it.trim() }

                if (colors.size >= 2) {
                    Color.parseColor(colors[1].trim())
                } else {
                    getDefaultColor()
                }
            } else {
                Color.parseColor(gradient)
            }
        } catch (e: Exception) {
            getDefaultColor()
        }
    }

    private fun getDefaultColor(): Int {
        return Color.parseColor("#6366F1")
    }

    private fun expandWithAnimation(preset: Preset) {
        isExpanded = !isExpanded

        currentData?.let { data ->
            if (isExpanded) {
                showExpandedInfo(preset, data)
            }
        }
    }

    private fun showExpandedInfo(preset: Preset, data: FluidCloudData) {
        currentCapsule?.let { view ->
            view.findViewById<TextView>(R.id.capsule_subtitle)?.apply {
                text = buildString {
                    append("ISO: ${data.isoText} | ")
                    append("对比度: ${data.contrastText} | ")
                    append("饱和度: ${data.saturationText}")
                }
            }
        }
    }

    private fun switchPreset(direction: Int) {
    }

    private fun removeCurrentCapsule() {
        currentCapsule?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
            }
        }
        currentCapsule = null
    }

    fun updateCapsule(preset: Preset) {
        currentPreset = preset
        currentData = createFluidCloudData(preset)

        currentCapsule?.let { view ->
            view.findViewById<TextView>(R.id.capsule_title)?.text = preset.name
            val params = preset.cameraParams
            view.findViewById<TextView>(R.id.capsule_subtitle)?.text =
                "ISO ${params?.iso} | ${params?.shutter}"

            val bgColor = parseGradientColor(currentData?.bgGradient ?: "")
            view.setBackgroundColor(bgColor)
        }
    }

    fun updateCapsuleWithData(data: FluidCloudData) {
        currentData = data

        currentCapsule?.let { view ->
            view.findViewById<TextView>(R.id.capsule_title)?.text = data.title
            view.findViewById<TextView>(R.id.capsule_subtitle)?.text = data.subtitle

            val bgColor = parseGradientColor(data.bgGradient)
            view.setBackgroundColor(bgColor)
        }
    }

    fun hideCapsule() {
        currentCapsule?.visibility = View.GONE
    }

    fun showCapsule() {
        currentCapsule?.visibility = View.VISIBLE
    }

    fun isExpanded(): Boolean = isExpanded

    fun getCurrentData(): FluidCloudData? = currentData

    fun destroy() {
        removeCurrentCapsule()
        capsules.clear()
        currentPreset = null
        currentData = null
    }
}
