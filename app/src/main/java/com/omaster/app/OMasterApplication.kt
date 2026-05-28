package com.omaster.app

import android.app.Application
import com.omaster.app.util.SecureLogManager
import dagger.hilt.android.HiltAndroidApp

/**
 * OPPOMaster应用类 - 安全加固版本
 * 
 * 安全改进：
 * 1. 使用SecureLogManager统一管理日志
 * 2. Release构建只记录非敏感信息
 * 3. 自动初始化安全组件
 * 
 * 作者：带娃的小陈工
 * 版本：2.0（安全加固版）
 */
@HiltAndroidApp
class OMasterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 使用安全日志管理器
        SecureLogManager.initialize(this)
        
        SecureLogManager.d("OMasterApplication started")
    }
}
