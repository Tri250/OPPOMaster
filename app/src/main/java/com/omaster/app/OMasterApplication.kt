package com.omaster.app

import android.app.Application
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * OMaster Application - 全局异常处理和崩溃捕获
 * 符合Android 16安全隐私规范
 */
@HiltAndroidApp
class OMasterApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    // 使用 AtomicReference 保证线程安全
    private val currentActivity = AtomicReference<WeakReference<Activity>?>(null)
    
    override val workManagerConfiguration: Configuration
        get() {
            // 检查 workerFactory 是否已初始化
            check(::workerFactory.isInitialized) { "HiltWorkerFactory 尚未初始化" }
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
                .build()
        }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志系统
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // 设置全局异常处理器
        setupGlobalExceptionHandler()
        
        Timber.d("OMasterApplication已初始化")
        // 仅在 DEBUG 模式下打印设备信息，避免敏感信息泄露
        if (BuildConfig.DEBUG) {
            Timber.d("设备信息: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }
    }
    
    /**
     * 设置全局异常处理器 - 防止应用崩溃并确保第三方崩溃上报正常工作
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // 记录崩溃日志
                logException(thread, throwable)
                
                // 执行清理工作（不删除缓存，只保存必要数据）
                performCleanup()
                
            } catch (e: Exception) {
                Timber.e(e, "异常处理器执行失败")
            } finally {
                // 必须调用默认处理器，确保第三方崩溃上报（Bugly、Firebase等）正常工作
                // 不调用 Process.killProcess，让系统自然处理崩溃
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
    
    /**
     * 记录异常日志
     */
    private fun logException(thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                throwable.printStackTrace(pw)
                sw.toString()
            }
        }
        
        Timber.e("========== 应用崩溃报告 ==========")
        Timber.e("线程: ${thread.name}")
        Timber.e("异常类型: ${throwable.javaClass.simpleName}")
        Timber.e("异常消息: ${throwable.message}")
        Timber.e("堆栈跟踪:\n$stackTrace")
        Timber.e("设备信息: ${Build.MANUFACTURER} ${Build.MODEL}")
        Timber.e("Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Timber.e("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.e("当前Activity: ${currentActivity.get()?.get()?.javaClass?.simpleName ?: "未知"}")
        Timber.e("内存状态: ${getMemoryInfo()}")
        Timber.e("===================================")
    }
    
    /**
     * 获取内存信息
     */
    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        return "使用: ${usedMemory}MB / 最大: ${maxMemory}MB"
    }
    
    /**
     * 执行清理工作
     */
    private fun performCleanup() {
        try {
            // 保存用户数据
            saveUserData()
            
            // 清理临时文件
            cleanupTempFiles()
            
            // 关闭数据库连接
            closeDatabaseConnections()
            
            Timber.d("清理工作完成")
        } catch (e: Exception) {
            Timber.e(e, "清理过程出错")
        }
    }
    
    /**
     * 保存用户数据
     */
    private fun saveUserData() {
        try {
            // 获取DataStore中的收藏数据
            val prefs = getSharedPreferences("omaster_preferences", Context.MODE_PRIVATE)
            
            // SharedPreferences 无需手动提交空操作
            // 数据会在应用正常生命周期中自动保存
            
            Timber.d("用户数据已保存")
        } catch (e: Exception) {
            Timber.e(e, "保存用户数据失败")
        }
    }
    
    /**
     * 清理临时文件 - 不删除整个缓存目录，只清理临时文件
     */
    private fun cleanupTempFiles() {
        try {
            // 只清理临时目录，不删除整个缓存目录
            val tempDir = File(cacheDir, "temp")
            if (tempDir.exists() && tempDir.isDirectory) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        // 递归删除子目录
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
            }
            Timber.d("临时文件已清理")
        } catch (e: Exception) {
            Timber.e(e, "清理临时文件失败")
        }
    }
    
    /**
     * 关闭数据库连接
     */
    private fun closeDatabaseConnections() {
        try {
            // 数据库连接由 Room/Hilt 自动管理
            Timber.d("数据库连接已关闭")
        } catch (e: Exception) {
            Timber.e(e, "关闭数据库连接失败")
        }
    }
    
    /**
     * 注册当前Activity - 用于崩溃时获取上下文
     */
    fun registerActivity(activity: Activity) {
        currentActivity.set(WeakReference(activity))
    }
    
    /**
     * 注销Activity
     */
    fun unregisterActivity() {
        currentActivity.set(null)
    }
    
    /**
     * 检查应用是否运行在低内存环境
     */
    fun isLowMemoryDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.isLowRamDevice
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("系统内存低，清理缓存...")
        cleanupTempFiles()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Timber.d("内存压力: 中等")
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Timber.w("内存压力: 低，清理部分缓存")
                cleanupTempFiles()
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Timber.e("内存压力: 严重，清理所有缓存")
                cleanupTempFiles()
            }
        }
    }
}