package com.omaster.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.omaster.app.R
import com.omaster.app.model.Preset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var params: WindowManager.LayoutParams? = null
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_overlay, null)
        setupFloatingView()
    }

    private fun setupFloatingView() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        params?.apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            x = 0
            y = 200
        }

        floatingView.findViewById<View>(R.id.floating_container).setOnTouchListener { _, event ->
            handleTouchEvent(event)
        }

        floatingView.findViewById<ImageView>(R.id.close_button).setOnClickListener {
            stopSelf()
        }

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialX = params?.x ?: 0
                initialY = params?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    isDragging = true
                }
                params?.apply {
                    x = initialX - dx
                    y = initialY + dy
                }
                windowManager.updateViewLayout(floatingView, params)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    floatingView.performClick()
                }
                return true
            }
        }
        return false
    }

    fun updatePreset(preset: Preset) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.Main) {
                floatingView.findViewById<TextView>(R.id.preset_name).text = preset.name
                floatingView.findViewById<TextView>(R.id.preset_filter).text = preset.cameraParams?.filter ?: ""
                floatingView.findViewById<TextView>(R.id.preset_iso).text = "ISO: ${preset.cameraParams?.iso}"
                floatingView.findViewById<TextView>(R.id.preset_shutter).text = "${preset.cameraParams?.shutter}"
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val presetId = it.getStringExtra("preset_id")
            if (presetId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val presetRepository = PresetRepository.getInstance(applicationContext)
                    val preset = presetRepository.getPresetById(presetId)
                    preset?.let { p -> updatePreset(p) }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(floatingView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun startService(context: Context, presetId: String? = null) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            presetId?.let { intent.putExtra("preset_id", it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}