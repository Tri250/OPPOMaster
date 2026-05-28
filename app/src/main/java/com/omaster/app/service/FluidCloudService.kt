package com.omaster.app.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import com.omaster.app.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FluidCloudService : Service() {
    
    companion object {
        const val ACTION_SHOW_CAPSULE = "com.omaster.app.action.SHOW_CAPSULE"
        const val ACTION_HIDE_CAPSULE = "com.omaster.app.action.HIDE_CAPSULE"
        const val ACTION_UPDATE_PRESET = "com.omaster.app.action.UPDATE_PRESET"
        const val EXTRA_PRESET_ID = "preset_id"
        const val EXTRA_PRESET_NAME = "preset_name"
        const val EXTRA_PRESET_CATEGORY = "preset_category"
        const val EXTRA_HAS_HNCS = "has_hncs"
    }

    private var windowManager: WindowManager? = null
    private var fluidCloudView: View? = null
    private var currentPresetId: String? = null
    private var currentPresetName: String? = null
    private var currentCategory: String? = null
    private var hasHncs: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("FluidCloudService created")
        initFluidCloudView()
    }

    private fun initFluidCloudView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        fluidCloudView = LayoutInflater.from(this).inflate(R.layout.fluid_cloud_capsule, null)
        setupCapsuleView()
    }

    private fun setupCapsuleView() {
        fluidCloudView?.apply {
            findViewById<View>(R.id.capsule_container)?.setOnClickListener {
                handleCapsuleClick()
            }
            
            findViewById<View>(R.id.capsule_close)?.setOnClickListener {
                hideFluidCloudCapsule()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("FluidCloudService onStartCommand with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SHOW_CAPSULE -> {
                currentPresetId = intent.getStringExtra(EXTRA_PRESET_ID)
                currentPresetName = intent.getStringExtra(EXTRA_PRESET_NAME)
                currentCategory = intent.getStringExtra(EXTRA_PRESET_CATEGORY)
                hasHncs = intent.getBooleanExtra(EXTRA_HAS_HNCS, false)
                showFluidCloudCapsule()
            }
            ACTION_UPDATE_PRESET -> {
                currentPresetId = intent.getStringExtra(EXTRA_PRESET_ID)
                currentPresetName = intent.getStringExtra(EXTRA_PRESET_NAME)
                currentCategory = intent.getStringExtra(EXTRA_PRESET_CATEGORY)
                hasHncs = intent.getBooleanExtra(EXTRA_HAS_HNCS, false)
                updateCapsuleContent()
            }
            ACTION_HIDE_CAPSULE -> {
                hideFluidCloudCapsule()
            }
        }
        
        return START_STICKY
    }

    private fun showFluidCloudCapsule() {
        Timber.d("Showing fluid cloud capsule for preset: $currentPresetName")
        
        try {
            val layoutParams = createLayoutParams()
            
            if (fluidCloudView?.parent == null) {
                windowManager?.addView(fluidCloudView, layoutParams)
                animateCapsuleIn()
            }
            
            updateCapsuleContent()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show fluid cloud capsule")
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 64
        }
    }

    private fun updateCapsuleContent() {
        fluidCloudView?.apply {
            findViewById<TextView>(R.id.capsule_preset_name)?.text = currentPresetName ?: "小O帮帮"
            findViewById<TextView>(R.id.capsule_category)?.text = currentCategory ?: "哈苏预设"
            
            val hncsIcon = findViewById<ImageView>(R.id.capsule_hncs_icon)
            if (hasHncs) {
                hncsIcon?.visibility = View.VISIBLE
            } else {
                hncsIcon?.visibility = View.GONE
            }
        }
    }

    private fun animateCapsuleIn() {
        fluidCloudView?.apply {
            val slideIn = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_in)
            startAnimation(slideIn)
        }
    }

    private fun animateCapsuleOut() {
        fluidCloudView?.apply {
            val slideOut = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_out)
            slideOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    try {
                        windowManager?.removeView(fluidCloudView)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to remove fluid cloud view")
                    }
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            })
            startAnimation(slideOut)
        }
    }

    private fun handleCapsuleClick() {
        Timber.d("Fluid cloud capsule clicked")
        // 点击胶囊可以打开应用或者显示更多选项
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch app from capsule")
        }
    }

    private fun hideFluidCloudCapsule() {
        Timber.d("Hiding fluid cloud capsule")
        
        try {
            if (fluidCloudView?.parent != null) {
                animateCapsuleOut()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide fluid cloud capsule")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (fluidCloudView?.parent != null) {
                windowManager?.removeView(fluidCloudView)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing fluid cloud view")
        }
        Timber.d("FluidCloudService destroyed")
    }
}