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
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

        /**
         * 静态内部类 AnimationListener，使用 WeakReference 避免内存泄漏
         */
        private class CapsuleAnimationListener(
            service: FluidCloudService
        ) : android.view.animation.Animation.AnimationListener {
            private val serviceRef = WeakReference(service)
            
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                serviceRef.get()?.onCapsuleAnimationEnd()
            }
            
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
        }
    }

    private val windowManagerRef = AtomicReference<WindowManager?>(null)
    private val fluidCloudViewRef = AtomicReference<View?>(null)
    private val currentPresetId = AtomicReference<String?>(null)
    private val currentPresetName = AtomicReference<String?>(null)
    private val currentCategory = AtomicReference<String?>(null)
    private val hasHncs = AtomicBoolean(false)
    private val isViewAttached = AtomicBoolean(false)
    
    private val viewLock = Any()

    override fun onCreate() {
        super.onCreate()
        Timber.d("FluidCloudService created")
        initFluidCloudView()
    }

    private fun initFluidCloudView() {
        try {
            windowManagerRef.set(getSystemService(WINDOW_SERVICE) as WindowManager)
            fluidCloudViewRef.set(LayoutInflater.from(this).inflate(R.layout.fluid_cloud_capsule, null))
            setupCapsuleView()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize fluid cloud view")
        }
    }

    private fun setupCapsuleView() {
        fluidCloudViewRef.get()?.apply {
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
                    currentPresetId.set(intent.getStringExtra(EXTRA_PRESET_ID))
                    currentPresetName.set(intent.getStringExtra(EXTRA_PRESET_NAME))
                    currentCategory.set(intent.getStringExtra(EXTRA_PRESET_CATEGORY))
                    hasHncs.set(intent.getBooleanExtra(EXTRA_HAS_HNCS, false))
                    showFluidCloudCapsule()
                }
                ACTION_UPDATE_PRESET -> {
                    currentPresetId.set(intent.getStringExtra(EXTRA_PRESET_ID))
                    currentPresetName.set(intent.getStringExtra(EXTRA_PRESET_NAME))
                    currentCategory.set(intent.getStringExtra(EXTRA_PRESET_CATEGORY))
                    hasHncs.set(intent.getBooleanExtra(EXTRA_HAS_HNCS, false))
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
        Timber.d("Showing fluid cloud capsule for preset: ${currentPresetName.get()}")
        
        try {
            val layoutParams = createLayoutParams()
            
            synchronized(viewLock) {
                val fluidCloudView = fluidCloudViewRef.get()
                val windowManager = windowManagerRef.get()
                if (fluidCloudView?.parent == null && !isViewAttached.get()) {
                    windowManager?.addView(fluidCloudView, layoutParams)
                    isViewAttached.set(true)
                    animateCapsuleIn()
                }
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
            fluidCloudViewRef.get()?.apply {
                findViewById<TextView>(R.id.capsule_preset_name)?.text = currentPresetName.get() ?: "小O帮帮"
                findViewById<TextView>(R.id.capsule_category)?.text = currentCategory.get() ?: "哈苏预设"
                
                val hncsIcon = findViewById<ImageView>(R.id.capsule_hncs_icon)
                if (hasHncs.get()) {
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
            fluidCloudViewRef.get()?.apply {
                val slideIn = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_in)
                startAnimation(slideIn)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to animate capsule in")
        }
    }

    private fun animateCapsuleOut() {
        try {
            fluidCloudViewRef.get()?.apply {
                val slideOut = AnimationUtils.loadAnimation(context, R.anim.capsule_slide_out)
                slideOut.setAnimationListener(CapsuleAnimationListener(this@FluidCloudService))
                startAnimation(slideOut)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to animate capsule out")
            // 在 catch 块中重置状态
            isViewAttached.set(false)
        }
    }

    /**
     * 胶囊动画结束后的回调处理
     */
    private fun onCapsuleAnimationEnd() {
        synchronized(viewLock) {
            try {
                if (isViewAttached.get() && fluidCloudViewRef.get()?.parent != null) {
                    windowManagerRef.get()?.removeView(fluidCloudViewRef.get())
                    isViewAttached.set(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove fluid cloud view")
                // 在 catch 块中重置状态
                isViewAttached.set(false)
            }
        }
    }

    private fun handleCapsuleClick() {
        Timber.d("Fluid cloud capsule clicked")
        try {
            val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch app from capsule")
        }
    }

    private fun hideFluidCloudCapsule() {
        Timber.d("Hiding fluid cloud capsule")
        
        try {
            if (isViewAttached.get() && fluidCloudViewRef.get()?.parent != null) {
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
        synchronized(viewLock) {
            try {
                if (isViewAttached.get() && fluidCloudViewRef.get()?.parent != null) {
                    windowManagerRef.get()?.removeView(fluidCloudViewRef.get())
                    isViewAttached.set(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing fluid cloud view")
            }
        }
        Timber.d("FluidCloudService destroyed")
    }
}
