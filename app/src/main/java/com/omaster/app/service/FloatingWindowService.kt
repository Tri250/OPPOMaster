package com.omaster.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.omaster.app.MainActivity
import com.omaster.app.R
import com.omaster.app.model.Preset
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.ui.theme.HasselbladOrange
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs

class FloatingWindowService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var expandedView: View? = null
    
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var savedStateRegistryController: SavedStateRegistryController
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentPreset: Preset? = null
    private var presetList: List<Preset> = emptyList()
    private var currentIndex: Int = 0
    private var isExpanded: Boolean = false
    private var currentAlpha: Float = 1f
    private var currentCategory: String = "全部"
    
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var isDragging: Boolean = false
    private var isSnapToEdgeEnabled: Boolean = true
    
    private var expandedLayoutParams: WindowManager.LayoutParams? = null
    private var collapsedLayoutParams: WindowManager.LayoutParams? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "omaster_floating_window"
        private const val ACTION_SHOW_OVERLAY = "com.omaster.app.SHOW_OVERLAY"
        private const val ACTION_HIDE_OVERLAY = "com.omaster.app.HIDE_OVERLAY"
        private const val ACTION_TOGGLE_OVERLAY = "com.omaster.app.TOGGLE_OVERLAY"
        private const val ACTION_NEXT_PRESET = "com.omaster.app.NEXT_PRESET"
        private const val ACTION_PREV_PRESET = "com.omaster.app.PREV_PRESET"
        private const val ACTION_COLLAPSE = "com.omaster.app.COLLAPSE"
        private const val ACTION_CLOSE = "com.omaster.app.CLOSE"
        
        private const val DRAG_THRESHOLD = 10f
        private const val SNAP_MARGIN = 16
        private const val SNAP_ANIMATION_DURATION = 300L
        
        private var instance: FloatingWindowService? = null
        
        fun getInstance(): FloatingWindowService? = instance
        
        fun showOverlay(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun hideOverlay(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            context.startService(intent)
        }
        
        fun toggleOverlay(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_TOGGLE_OVERLAY
            }
            context.startService(intent)
        }
        
        fun updatePresets(presets: List<Preset>) {
            instance?.apply {
                presetList = presets
                currentIndex = 0
                currentPreset = presets.firstOrNull()
                updateFloatingWindowContent()
            }
        }
        
        fun setCurrentPreset(preset: Preset) {
            instance?.apply {
                currentPreset = preset
                currentIndex = presetList.indexOfFirst { it.id == preset.id }.coerceAtLeast(0)
                updateFloatingWindowContent()
            }
        }
        
        fun nextPreset() {
            instance?.apply {
                if (currentIndex < presetList.size - 1) {
                    currentIndex++
                    currentPreset = presetList[currentIndex]
                    updateFloatingWindowContent()
                }
            }
        }
        
        fun prevPreset() {
            instance?.apply {
                if (currentIndex > 0) {
                    currentIndex--
                    currentPreset = presetList[currentIndex]
                    updateFloatingWindowContent()
                }
            }
        }
        
        fun collapse() {
            instance?.apply {
                isExpanded = false
                updateFloatingWindowContent()
            }
        }
        
        fun expand() {
            instance?.apply {
                isExpanded = true
                updateFloatingWindowContent()
            }
        }
        
        fun setAlpha(alpha: Float) {
            instance?.apply {
                currentAlpha = alpha.coerceIn(0.3f, 1f)
                floatingView?.alpha = currentAlpha
                expandedView?.alpha = currentAlpha
            }
        }
        
        fun setCategory(category: String) {
            instance?.apply {
                currentCategory = category
                val filteredPresets = if (category == "全部") {
                    presetList
                } else if (category == "收藏") {
                    presetList.filter { it.isFavorite }
                } else {
                    presetList.filter { it.cameraParams?.filter == category }
                }
                if (filteredPresets.isNotEmpty()) {
                    currentIndex = 0
                    currentPreset = filteredPresets[currentIndex]
                    updateFloatingWindowContent()
                }
            }
        }
    }
    
    override val lifecycleRegistry: LifecycleRegistry
        get() = lifecycleRegistry
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        lifecycleRegistry = LifecycleRegistry(this)
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        
        Timber.d("FloatingWindowService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showFloatingWindow()
            ACTION_HIDE_OVERLAY -> hideFloatingWindow()
            ACTION_TOGGLE_OVERLAY -> toggleFloatingWindow()
            ACTION_NEXT_PRESET -> nextPreset()
            ACTION_PREV_PRESET -> prevPreset()
            ACTION_COLLAPSE -> collapse()
            ACTION_CLOSE -> {
                hideFloatingWindow()
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        instance = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        hideFloatingWindow()
        serviceScope.cancel()
        super.onDestroy()
        Timber.d("FloatingWindowService destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OMaster 悬浮窗控制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供悬浮窗快捷控制功能"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val collapseIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_COLLAPSE
        }
        val collapsePendingIntent = PendingIntent.getService(
            this, 1, collapseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val prevIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_PREV_PRESET
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 2, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val nextIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_NEXT_PRESET
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val closeIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_CLOSE
        }
        val closePendingIntent = PendingIntent.getService(
            this, 4, closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OMaster 悬浮窗已开启")
            .setContentText(currentPreset?.name ?: "点击打开 OMaster")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_launcher_foreground, "收起", collapsePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "上一个", prevPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "下一个", nextPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "关闭", closePendingIntent)
            .build()
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingWindow() {
        if (floatingView != null || expandedView != null) return
        
        val layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        
        // 展开窗口参数
        expandedLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = SNAP_MARGIN
            y = 200
        }
        
        try {
            expandedView = createExpandedFloatingView()
            val density = resources.displayMetrics.density
            expandedLayoutParams?.width = (280 * density).toInt()
            expandedLayoutParams?.height = (360 * density).toInt()
            windowManager?.addView(expandedView, expandedLayoutParams)
            
            collapsedLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = SNAP_MARGIN
                y = 200
            }
            floatingView = createCollapsedFloatingView()
            windowManager?.addView(floatingView, collapsedLayoutParams)
            
            setupDragListener()
            
            updateFloatingWindowContent()
            updateNotification()
            
            Timber.d("Floating window created successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create floating window")
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.rawX - lastX)
                    val dy = abs(event.rawY - lastY)
                    if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        val newX = (view.layoutParams as WindowManager.LayoutParams).x + (event.rawX - lastX).toInt()
                        val newY = (view.layoutParams as WindowManager.LayoutParams).y + (event.rawY - lastY).toInt()
                        
                        (view.layoutParams as WindowManager.LayoutParams).x = newX
                        (view.layoutParams as WindowManager.LayoutParams).y = newY
                        
                        windowManager?.updateViewLayout(view, view.layoutParams)
                        
                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    isDragging
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging && isSnapToEdgeEnabled) {
                        snapToEdge(view)
                    }
                    false
                }
                else -> false
            }
        }
    }
    
    private fun snapToEdge(view: View) {
        val params = view.layoutParams as WindowManager.LayoutParams
        val screenWidth = resources.displayMetrics.widthPixels
        
        val targetX = if (params.x < screenWidth / 2) {
            SNAP_MARGIN
        } else {
            screenWidth - SNAP_MARGIN - (56 * resources.displayMetrics.density).toInt()
        }
        
        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = SNAP_ANIMATION_DURATION
        animator.interpolator = android.view.animation.OvershootInterpolator(1.2f)
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update view layout during snap")
            }
        }
        animator.start()
    }
    
    private fun hideFloatingWindow() {
        try {
            floatingView?.let {
                windowManager?.removeView(it)
                floatingView = null
            }
            expandedView?.let {
                windowManager?.removeView(it)
                expandedView = null
            }
            Timber.d("Floating window removed")
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove floating window")
        }
    }
    
    private fun toggleFloatingWindow() {
        if (floatingView != null) {
            hideFloatingWindow()
        } else {
            showFloatingWindow()
        }
    }
    
    private fun createCollapsedFloatingView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
        }
        
        composeView.setContent {
            MaterialTheme {
                CollapsedFloatingContent(
                    currentPreset = currentPreset,
                    isExpanded = isExpanded,
                    onExpand = {
                        isExpanded = true
                        updateFloatingWindowContent()
                    },
                    onDoubleTap = {
                        hideFloatingWindow()
                        stopSelf()
                    }
                )
            }
        }
        
        return composeView
    }
    
    private fun createExpandedFloatingView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
        }
        
        composeView.setContent {
            MaterialTheme {
                ExpandedFloatingContent(
                    currentPreset = currentPreset,
                    presetList = presetList,
                    currentIndex = currentIndex,
                    currentCategory = currentCategory,
                    onCollapse = {
                        isExpanded = false
                        updateFloatingWindowContent()
                    },
                    onNext = { nextPreset() },
                    onPrev = { prevPreset() },
                    onCategoryChange = { setCategory(it) },
                    onAlphaChange = { setAlpha(it) },
                    onClose = {
                        hideFloatingWindow()
                        stopSelf()
                    }
                )
            }
        }
        
        return composeView
    }
    
    private fun updateFloatingWindowContent() {
        (floatingView as? ComposeView)?.setContent {
            MaterialTheme {
                CollapsedFloatingContent(
                    currentPreset = currentPreset,
                    isExpanded = isExpanded,
                    onExpand = {
                        isExpanded = true
                        updateFloatingWindowContent()
                    },
                    onDoubleTap = {
                        hideFloatingWindow()
                        stopSelf()
                    }
                )
            }
        }
        
        (expandedView as? ComposeView)?.setContent {
            MaterialTheme {
                ExpandedFloatingContent(
                    currentPreset = currentPreset,
                    presetList = presetList,
                    currentIndex = currentIndex,
                    currentCategory = currentCategory,
                    onCollapse = {
                        isExpanded = false
                        updateFloatingWindowContent()
                    },
                    onNext = { nextPreset() },
                    onPrev = { prevPreset() },
                    onCategoryChange = { setCategory(it) },
                    onAlphaChange = { setAlpha(it) },
                    onClose = {
                        hideFloatingWindow()
                        stopSelf()
                    }
                )
            }
        }
        
        floatingView?.alpha = currentAlpha
        expandedView?.alpha = currentAlpha
        
        floatingView?.visibility = if (isExpanded) View.GONE else View.VISIBLE
        expandedView?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        updateNotification()
    }
}

@Composable
fun CollapsedFloatingContent(
    currentPreset: Preset?,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(AccentPrimary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExpand()
                    },
                    onDoubleTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDoubleTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (currentPreset != null) {
            Text(
                text = currentPreset.cameraParams?.filter?.take(2) ?: "预设",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedFloatingContent(
    currentPreset: Preset?,
    presetList: List<Preset>,
    currentIndex: Int,
    currentCategory: String,
    onCollapse: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val categories = listOf("全部", "收藏", "复古", "清新", "夜景", "自然")
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var showAlphaSlider by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(offsetX) > 50) {
                            if (offsetX < 0) onNext() else onPrev()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OMaster",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showAlphaSlider = !showAlphaSlider },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "调节透明度",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(visible = showAlphaSlider) {
                Slider(
                    value = 1f,
                    onValueChange = { onAlphaChange(it) },
                    valueRange = 0.3f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = currentCategory == category,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCategoryChange(category)
                        },
                        label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (currentPreset != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPreset.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    currentPreset.cameraParams?.let { params ->
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "ISO ${params.iso}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = params.shutter ?: "auto",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = params.ev ?: "0",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = params.wb ?: "auto",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = params.filter ?: "标准",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AccentPrimary
                                    )
                                    if (params.hasselblad_hncs) {
                                        Text(
                                            text = "HNCS",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = HasselbladOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPrev()
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "上一个",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Text(
                        text = "${currentIndex + 1} / ${presetList.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNext()
                        },
                        enabled = currentIndex < presetList.size - 1,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "下一个",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无预设",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
