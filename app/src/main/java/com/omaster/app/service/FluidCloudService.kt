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
    private var isViewAttached: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("FluidCloudService created")
        initFluidCloudView()
    }

    private fun initFluidCloudView() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            fluidCloudView = LayoutInflater.from(this).inflate(R.layout.fluid_cloud_capsule, null)
            setupCapsuleView()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize fluid cloud view")
        }
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
        
        try {
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
        } catch (e: Exception) {
            Timber.e(e, "Error handling service command")
        }
        
        return START_NOT_STICKY
    }

    private fun showFluidCloudCapsule() {
        Timber.d("Showing fluid cloud capsule for preset: $currentPresetName")
        
        try {
            val layoutParams = createLayoutParams()
            
            if (fluidCloudView?.parent == null && !isViewAttached) {
                windowManager?.addView(fluidCloudView, layoutParams)
                isViewAttached = true
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
        try {
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to update capsule content")
        }
    }

    private fun animateCapsuleIn() {
        try {
            fluidCloudView?.apply {
                val slideIn = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_in)
                startAnimation(slideIn)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to animate capsule in")
        }
    }

    private fun animateCapsuleOut() {
        try {
            fluidCloudView?.apply {
                val slideOut = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_out)
                slideOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        try {
                            if (isViewAttached && fluidCloudView?.parent != null) {
                                windowManager?.removeView(fluidCloudView)
                                isViewAttached = false
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to remove fluid cloud view")
                        }
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                })
                startAnimation(slideOut)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to animate capsule out")
        }
    }

    private fun handleCapsuleClick() {
        Timber.d("Fluid cloud capsule clicked")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch app from capsule")
        }
    }

    private fun hideFluidCloudCapsule() {
        Timber.d("Hiding fluid cloud capsule")
        
        try {
            if (isViewAttached && fluidCloudView?.parent != null) {
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
            if (isViewAttached && fluidCloudView?.parent != null) {
                windowManager?.removeView(fluidCloudView)
                isViewAttached = false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing fluid cloud view")
        }
        Timber.d("FluidCloudService destroyed")
    }
}
