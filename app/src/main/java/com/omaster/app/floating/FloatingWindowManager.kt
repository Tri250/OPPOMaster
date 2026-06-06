package com.omaster.app.floating

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * 悬浮窗位置数据类
 */
data class FloatingWindowPosition(
    val x: Int,
    val y: Int,
    val isRightSide: Boolean = true
)

/**
 * 悬浮窗设置数据类
 */
data class FloatingWindowSettings(
    val opacity: Float = 1.0f,
    val position: FloatingWindowPosition = FloatingWindowPosition(20, 200),
    val isExpanded: Boolean = true,
    val lastPresetId: String? = null
)

/**
 * 自定义 LifecycleOwner 用于悬浮窗
 */
private class FloatingLifecycleOwner : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    fun onCreate() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

/**
 * 主题管理器 - 监听系统主题变化
 */
class FloatingThemeManager(private val context: Context) {
    private val _isDarkTheme = MutableStateFlow(isSystemInDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val configurationChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                updateTheme()
            }
        }
    }

    init {
        updateTheme()
    }

    private fun isSystemInDarkTheme(): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    fun updateTheme() {
        _isDarkTheme.value = isSystemInDarkTheme()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _useDynamicColor.value = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}

object FloatingWindowManager {
    private const val PREFS_NAME = "floating_window_prefs"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_POS_X = "pos_x"
    private const val KEY_POS_Y = "pos_y"
    private const val KEY_IS_RIGHT_SIDE = "is_right_side"
    private const val KEY_IS_EXPANDED = "is_expanded"
    private const val KEY_LAST_PRESET_ID = "last_preset_id"

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var isShowing = false
    private var lifecycleOwner: FloatingLifecycleOwner? = null
    private var themeManager: FloatingThemeManager? = null

    private var currentPresetName: String = "预设参数"
    private var currentParams: Map<String, String> = emptyMap()
    private var currentPresetId: String? = null

    // 状态流
    private val _settings = MutableStateFlow(FloatingWindowSettings())
    val settings: StateFlow<FloatingWindowSettings> = _settings.asStateFlow()

    private val _isExpanded = MutableStateFlow(true)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // 回调接口
    interface FloatingWindowCallback {
        fun onFavoriteToggle(presetId: String, isFavorite: Boolean)
        fun onSharePreset(presetId: String)
        fun onCopyParams(params: Map<String, String>)
        fun onPresetSelect(presetId: String)
        fun onClose()
    }

    private var callback: FloatingWindowCallback? = null

    fun setCallback(cb: FloatingWindowCallback) {
        callback = cb
    }

    /**
     * 获取 SharedPreferences
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 加载设置
     */
    fun loadSettings(context: Context) {
        val prefs = getPrefs(context)
        val opacity = prefs.getFloat(KEY_OPACITY, 1.0f).coerceIn(0.3f, 1.0f)
        val posX = prefs.getInt(KEY_POS_X, 20)
        val posY = prefs.getInt(KEY_POS_Y, 200)
        val isRightSide = prefs.getBoolean(KEY_IS_RIGHT_SIDE, true)
        val isExpanded = prefs.getBoolean(KEY_IS_EXPANDED, true)
        val lastPresetId = prefs.getString(KEY_LAST_PRESET_ID, null)

        _settings.value = FloatingWindowSettings(
            opacity = opacity,
            position = FloatingWindowPosition(posX, posY, isRightSide),
            isExpanded = isExpanded,
            lastPresetId = lastPresetId
        )
        _isExpanded.value = isExpanded
    }

    /**
     * 保存设置
     */
    fun saveSettings(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().apply {
            putFloat(KEY_OPACITY, _settings.value.opacity)
            putInt(KEY_POS_X, _settings.value.position.x)
            putInt(KEY_POS_Y, _settings.value.position.y)
            putBoolean(KEY_IS_RIGHT_SIDE, _settings.value.position.isRightSide)
            putBoolean(KEY_IS_EXPANDED, _isExpanded.value)
            putString(KEY_LAST_PRESET_ID, currentPresetId)
            apply()
        }
    }

    /**
     * 设置透明度
     */
    fun setOpacity(opacity: Float) {
        _settings.value = _settings.value.copy(opacity = opacity.coerceIn(0.3f, 1.0f))
        updateWindowAlpha()
    }

    /**
     * 更新窗口透明度
     */
    private fun updateWindowAlpha() {
        floatingView?.alpha = _settings.value.opacity
    }

    /**
     * 设置展开/收起状态
     */
    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    /**
     * 切换展开/收起
     */
    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
    }

    /**
     * 设置收藏状态
     */
    fun setFavorite(isFav: Boolean) {
        _isFavorite.value = isFav
    }

    /**
     * 更新位置
     */
    fun updatePosition(x: Int, y: Int, isRightSide: Boolean) {
        _settings.value = _settings.value.copy(
            position = FloatingWindowPosition(x, y, isRightSide)
        )
    }

    fun setPresetData(name: String, params: Map<String, String>, presetId: String? = null) {
        currentPresetName = name
        currentParams = params
        currentPresetId = presetId
        if (isShowing) {
            updateFloatingView()
        }
    }

    fun showWindow(context: Context) {
        if (isShowing) return

        if (!canDrawOverlays(context)) {
            requestOverlayPermission(context)
            return
        }

        // 加载保存的设置
        loadSettings(context)

        try {
            val appContext = context.applicationContext
            windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // 初始化主题管理器
            themeManager = FloatingThemeManager(appContext)

            // 创建并初始化 LifecycleOwner
            lifecycleOwner = FloatingLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }

            floatingView = ComposeView(appContext).apply {
                // 设置 LifecycleOwner
                ViewTreeLifecycleOwner.set(this, lifecycleOwner)

                // 设置 SavedStateRegistryOwner
                val savedStateRegistryOwner = object : androidx.savedstate.SavedStateRegistryOwner {
                    override val lifecycle: Lifecycle
                        get() = lifecycleOwner!!.lifecycle
                    override val savedStateRegistry: SavedStateRegistry
                        get() = SavedStateRegistry()
                }
                ViewTreeSavedStateRegistryOwner.set(this, savedStateRegistryOwner)

                alpha = _settings.value.opacity

                setContent {
                    val isDarkTheme by themeManager!!.isDarkTheme.collectAsState()
                    val useDynamicColor by themeManager!!.useDynamicColor.collectAsState()

                    FloatingWindowThemeWrapper(
                        isDarkTheme = isDarkTheme,
                        useDynamicColor = useDynamicColor
                    ) {
                        FloatingWindowContentEnhanced(
                            presetName = currentPresetName,
                            params = currentParams,
                            presetId = currentPresetId,
                            isExpanded = _isExpanded.value,
                            isFavorite = _isFavorite.value,
                            opacity = _settings.value.opacity,
                            onClose = {
                                saveSettings(appContext)
                                hideWindow()
                                callback?.onClose()
                            },
                            onCopyParams = { copyParamsToClipboard(appContext) },
                            onExpandToggle = { toggleExpanded() },
                            onFavoriteToggle = {
                                val newState = !_isFavorite.value
                                _isFavorite.value = newState
                                currentPresetId?.let { callback?.onFavoriteToggle(it, newState) }
                            },
                            onSharePreset = {
                                currentPresetId?.let { callback?.onSharePreset(it) }
                            },
                            onOpacityChange = { setOpacity(it) },
                            onPositionUpdate = { x, y, isRight ->
                                updatePosition(x, y, isRight)
                            }
                        )
                    }
                }
            }

            val params = getWindowParams()
            windowManager?.addView(floatingView, params)
            isShowing = true
            Timber.d("悬浮窗已显示")
        } catch (e: Exception) {
            Timber.e(e, "显示悬浮窗失败")
        }
    }

    fun hideWindow() {
        if (!isShowing) return

        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
            // 清理 LifecycleOwner
            lifecycleOwner?.apply {
                onPause()
                onStop()
                onDestroy()
            }
            lifecycleOwner = null
            themeManager = null
            floatingView = null
            isShowing = false
            Timber.d("悬浮窗已隐藏")
        } catch (e: Exception) {
            Timber.e(e, "隐藏悬浮窗失败")
        }
    }

    fun toggleWindow(context: Context) {
        if (isShowing) {
            hideWindow()
        } else {
            showWindow(context)
        }
    }

    private fun updateFloatingView() {
        floatingView?.invalidate()
    }

    private fun copyParamsToClipboard(context: Context?) {
        context ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = currentParams.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val clip = android.content.ClipData.newPlainText("参数", text)
        clipboard.setPrimaryClip(clip)
        callback?.onCopyParams(currentParams)
    }

    /**
     * 获取窗口参数
     */
    fun getWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            val pos = _settings.value.position
            gravity = Gravity.TOP or if (pos.isRightSide) Gravity.END else Gravity.START
            x = pos.x
            y = pos.y
        }
    }

    /**
     * 更新窗口位置
     */
    fun updateWindowPosition(x: Int, y: Int, isRightSide: Boolean) {
        try {
            val params = getWindowParams()
            params.x = x
            params.y = y
            params.gravity = Gravity.TOP or if (isRightSide) Gravity.END else Gravity.START
            windowManager?.updateViewLayout(floatingView, params)
        } catch (e: Exception) {
            Timber.e(e, "更新窗口位置失败")
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun clearPresetData() {
        currentPresetName = "预设参数"
        currentParams = emptyMap()
        currentPresetId = null
    }

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            Pair(metrics.bounds.width(), metrics.bounds.height())
        } else {
            val display = wm.defaultDisplay
            val metrics = android.util.DisplayMetrics()
            display.getMetrics(metrics)
            Pair(metrics.widthPixels, metrics.heightPixels)
        }
    }
}

/**
 * 悬浮窗主题包装器
 */
@Composable
fun FloatingWindowThemeWrapper(
    isDarkTheme: Boolean,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> ColorOSDarkColorScheme
        else -> ColorOSLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ColorOSTypography,
        shapes = ColorOSShapes,
        content = content
    )
}

/**
 * 增强版悬浮窗内容
 */
@Composable
fun FloatingWindowContentEnhanced(
    presetName: String,
    params: Map<String, String>,
    presetId: String?,
    isExpanded: Boolean,
    isFavorite: Boolean,
    opacity: Float,
    onClose: () -> Unit,
    onCopyParams: () -> Unit,
    onExpandToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSharePreset: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onPositionUpdate: (Int, Int, Boolean) -> Unit
) {
    if (isExpanded) {
        ExpandedFloatingWindowEnhanced(
            presetName = presetName,
            params = params,
            presetId = presetId,
            isFavorite = isFavorite,
            opacity = opacity,
            onClose = onClose,
            onCopyParams = onCopyParams,
            onCollapse = onExpandToggle,
            onFavoriteToggle = onFavoriteToggle,
            onSharePreset = onSharePreset,
            onOpacityChange = onOpacityChange
        )
    } else {
        CollapsedFloatingBallEnhanced(
            onClick = onExpandToggle,
            onDoubleClick = onClose,
            onLongPress = { /* 显示快捷菜单 */ },
            isFavorite = isFavorite
        )
    }
}

/**
 * 展开状态的悬浮窗
 */
@Composable
fun ExpandedFloatingWindowEnhanced(
    presetName: String,
    params: Map<String, String>,
    presetId: String?,
    isFavorite: Boolean,
    opacity: Float,
    onClose: () -> Unit,
    onCopyParams: () -> Unit,
    onCollapse: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSharePreset: () -> Unit,
    onOpacityChange: (Float) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showOpacitySlider by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(300.dp)
            .padding(8.dp)
            .alpha(opacity),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = presetName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Row {
                    // 收藏按钮
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) AccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 更多菜单按钮
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 收起按钮
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 参数列表
            params.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 透明度控制
            if (showOpacitySlider) {
                Column {
                    Text(
                        text = "透明度: ${(opacity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = opacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.3f..1.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 复制参数按钮
                Button(
                    onClick = onCopyParams,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "复制",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // 透明度切换按钮
                IconButton(
                    onClick = { showOpacitySlider = !showOpacitySlider },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = "透明度",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 关闭按钮
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = ErrorPro
                    )
                }
            }
        }
    }

    // 下拉菜单
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("分享预设") },
            leadingIcon = {
                Icon(Icons.Default.Share, contentDescription = null)
            },
            onClick = {
                onSharePreset()
                showMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text(if (isFavorite) "取消收藏" else "添加收藏") },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            },
            onClick = {
                onFavoriteToggle()
                showMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text("关闭悬浮窗") },
            leadingIcon = {
                Icon(Icons.Default.Close, contentDescription = null)
            },
            onClick = {
                onClose()
                showMenu = false
            }
        )
    }
}

/**
 * 收起状态的悬浮球
 */
@Composable
fun CollapsedFloatingBallEnhanced(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongPress: () -> Unit,
    isFavorite: Boolean
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                onClickLabel = "展开悬浮窗"
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // 双击
                    onDoubleClick()
                } else {
                    // 单击
                    onClick()
                }
                lastClickTime = currentTime
            },
        shape = RoundedCornerShape(28.dp),
        color = AccentPrimary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.PhotoCamera,
                contentDescription = "悬浮球",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}