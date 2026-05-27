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
import kotlin.math.abs

data class CapsuleHandle(val id: String)

class FluidCloudCapsuleManager(private val context: Context) {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var currentCapsule: View? = null
    private var currentPreset: Preset? = null
    private val capsules = mutableListOf<Pair<Preset, View>>()
    private var isExpanded = false

    suspend fun createCapsule(preset: Preset): Result<CapsuleHandle> {
        return withContext(Dispatchers.Main) {
            try {
                removeCurrentCapsule()

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
                    width = 200
                    height = 100
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

        view.findViewById<TextView>(R.id.capsule_title).text = preset.name
        val params = preset.cameraParams
        view.findViewById<TextView>(R.id.capsule_subtitle).text =
            "ISO ${params?.iso}  ${params?.shutter}"

        view.setBackgroundColor(getDynamicColor())

        view.setOnClickListener {
            expandWithAnimation(preset)
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0f
            private var initialTouchX = 0f

            override fun onTouch(v: View?, event: android.view.MotionEvent?): Boolean {
                when (event?.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = (v?.layoutParams as WindowManager.LayoutParams).x.toFloat()
                        initialTouchX = event.rawX
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val params = v?.layoutParams as WindowManager.LayoutParams
                        params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                        windowManager.updateViewLayout(v, params)
                        return true
                    }
                }
                return false
            }
        })

        return view
    }

    private fun getDynamicColor(): Int {
        return Color.parseColor("#FF6200EE")
    }

    private fun expandWithAnimation(preset: Preset) {
        isExpanded = !isExpanded
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
    }

    fun updateCapsule(preset: Preset) {
        currentCapsule?.findViewById<TextView>(R.id.capsule_title)?.text = preset.name
        val params = preset.cameraParams
        currentCapsule?.findViewById<TextView>(R.id.capsule_subtitle)?.text =
            "ISO ${params?.iso}  ${params?.shutter}"
    }

    fun hideCapsule() {
        currentCapsule?.visibility = View.GONE
    }

    fun showCapsule() {
        currentCapsule?.visibility = View.VISIBLE
    }

    fun destroy() {
        removeCurrentCapsule()
        capsules.clear()
    }
}
