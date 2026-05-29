package com.omaster.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omaster.app.MainActivity
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.model.PresetDatabase

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var expandedView: View
    private var isExpanded = false
    
    private var currentPresets = mutableListOf<Preset>()
    private var currentIndex = 0
    
    companion object {
        const val CHANNEL_ID = "floating_window_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_SHOW = "com.omaster.app.ACTION_SHOW_FLOATING"
        const val ACTION_HIDE = "com.omaster.app.ACTION_HIDE_FLOATING"
        const val ACTION_NEXT = "com.omaster.app.ACTION_NEXT_PRESET"
        const val ACTION_PREV = "com.omaster.app.ACTION_PREV_PRESET"
        const val ACTION_EXPAND = "com.omaster.app.ACTION_EXPAND"
        const val ACTION_COLLAPSE = "com.omaster.app.ACTION_COLLAPSE"
        
        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showFloatingWindow()
            ACTION_HIDE -> hideFloatingWindow()
            ACTION_NEXT -> nextPreset()
            ACTION_PREV -> prevPreset()
            ACTION_EXPAND -> expandWindow()
            ACTION_COLLAPSE -> collapseWindow()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "悬浮窗服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "悬浮窗服务，用于在拍照时显示预设参数"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val showIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_SHOW
        }
        val showPendingIntent = PendingIntent.getService(
            this, 0, showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 1, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 2, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hideIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_HIDE
        }
        val hidePendingIntent = PendingIntent.getService(
            this, 3, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("小O帮帮")
            .setContentText("悬浮窗已启动")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(R.drawable.ic_skip_previous, "上一个", prevPendingIntent)
            .addAction(R.drawable.ic_skip_next, "下一个", nextPendingIntent)
            .addAction(R.drawable.ic_close, "关闭", hidePendingIntent)
            .build()
    }

    private fun showFloatingWindow() {
        if (::floatingView.isInitialized) {
            floatingView.visibility = View.VISIBLE
            return
        }

        startForeground(NOTIFICATION_ID, createNotification())
        
        currentPresets = PresetDatabase.getAllPresets().take(10).toMutableList()
        
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_window, null)
        expandedView = inflater.inflate(R.layout.floating_window_expanded, null)
        
        setupFloatingView()
        setupExpandedView()
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }
        
        try {
            windowManager.addView(floatingView, params)
            updateFloatingContent()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFloatingView() {
        floatingView.findViewById<ImageButton>(R.id.btnExpand)?.setOnClickListener {
            expandWindow()
        }
        
        floatingView.findViewById<ImageButton>(R.id.btnNext)?.setOnClickListener {
            nextPreset()
        }
        
        floatingView.findViewById<ImageButton>(R.id.btnPrev)?.setOnClickListener {
            prevPreset()
        }
        
        floatingView.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
            hideFloatingWindow()
        }
        
        floatingView.setOnTouchListener(createTouchListener())
    }

    private fun setupExpandedView() {
        expandedView.findViewById<ImageButton>(R.id.btnCollapse)?.setOnClickListener {
            collapseWindow()
        }
        
        val recyclerView = expandedView.findViewById<RecyclerView>(R.id.presetList)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = PresetAdapter(currentPresets) { preset ->
            selectPreset(preset)
        }
        
        val closeBtn = expandedView.findViewById<ImageButton>(R.id.btnCloseExpanded)
        closeBtn?.setOnClickListener {
            hideFloatingWindow()
        }
        
        expandedView.visibility = View.GONE
    }

    private fun createTouchListener(): View.OnTouchListener {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var lastAction = 0
        
        return View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = (floatingView.layoutParams as WindowManager.LayoutParams).x
                    initialY = (floatingView.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    lastAction = MotionEvent.ACTION_DOWN
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val params = floatingView.layoutParams as WindowManager.LayoutParams
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    lastAction = MotionEvent.ACTION_MOVE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        expandWindow()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun expandWindow() {
        if (isExpanded || !::expandedView.isInitialized) return
        isExpanded = true
        floatingView.visibility = View.GONE
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100
        }
        
        try {
            windowManager.addView(expandedView, params)
            expandedView.findViewById<RecyclerView>(R.id.presetList)?.adapter?.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collapseWindow() {
        if (!isExpanded || !::expandedView.isInitialized) return
        isExpanded = false
        
        try {
            windowManager.removeView(expandedView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        floatingView.visibility = View.VISIBLE
    }

    private fun hideFloatingWindow() {
        try {
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
            if (::expandedView.isInitialized && isExpanded) {
                windowManager.removeView(expandedView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning = false
    }

    private fun nextPreset() {
        if (currentPresets.isEmpty()) return
        currentIndex = (currentIndex + 1) % currentPresets.size
        updateFloatingContent()
    }

    private fun prevPreset() {
        if (currentPresets.isEmpty()) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else currentPresets.size - 1
        updateFloatingContent()
    }

    private fun selectPreset(preset: Preset) {
        currentIndex = currentPresets.indexOf(preset)
        if (currentIndex < 0) {
            currentPresets.add(0, preset)
            currentIndex = 0
        }
        updateFloatingContent()
        collapseWindow()
        
        // 发送广播通知主界面应用预设
        val intent = Intent("com.omaster.app.PRESET_APPLIED")
        intent.putExtra("preset_id", preset.id)
        sendBroadcast(intent)
    }

    private fun updateFloatingContent() {
        if (!::floatingView.isInitialized || currentPresets.isEmpty()) return
        
        val preset = currentPresets[currentIndex]
        floatingView.findViewById<TextView>(R.id.tvPresetName)?.text = preset.name
        floatingView.findViewById<TextView>(R.id.tvDeviceModel)?.text = preset.deviceModel
        
        val paramsText = buildString {
            preset.cameraParams?.let { params ->
                params.hasselblad_hncs?.let { append("哈苏HNCS ") }
                params.master_hdr?.let { append("HDR ") }
                params.ai_scene?.let { append("AI ") }
                params.saturation?.let { append("饱和度:$it ") }
                params.contrast?.let { append("对比度:$it") }
            }
        }
        floatingView.findViewById<TextView>(R.id.tvParams)?.text = paramsText
        
        floatingView.findViewById<TextView>(R.id.tvIndex)?.text = 
            "${currentIndex + 1}/${currentPresets.size}"
    }

    fun updatePresets(presets: List<Preset>) {
        currentPresets = presets.take(20).toMutableList()
        currentIndex = 0
        updateFloatingContent()
        
        if (::expandedView.isInitialized) {
            expandedView.findViewById<RecyclerView>(R.id.presetList)?.adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        hideFloatingWindow()
        isRunning = false
        super.onDestroy()
    }

    inner class PresetAdapter(
        private val presets: List<Preset>,
        private val onPresetClick: (Preset) -> Unit
    ) : RecyclerView.Adapter<PresetAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: CardView = itemView.findViewById(R.id.presetCard)
            val tvName: TextView = itemView.findViewById(R.id.tvPresetName)
            val tvDevice: TextView = itemView.findViewById(R.id.tvDeviceModel)
            val tvParams: TextView = itemView.findViewById(R.id.tvParams)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_floating_preset, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val preset = presets[position]
            holder.tvName.text = preset.name
            holder.tvDevice.text = preset.deviceModel
            
            val paramsText = buildString {
                preset.cameraParams?.let { params ->
                    params.hasselblad_hncs?.let { append("HNCS ") }
                    params.saturation?.let { append("饱:$it ") }
                    params.contrast?.let { append("对:$it") }
                }
            }
            holder.tvParams.text = paramsText
            
            holder.cardView.setOnClickListener {
                onPresetClick(preset)
            }
        }

        override fun getItemCount() = presets.size
    }
}
