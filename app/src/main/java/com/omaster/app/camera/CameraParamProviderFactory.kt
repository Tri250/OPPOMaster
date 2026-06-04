package com.omaster.app.camera

import android.content.Context
import timber.log.Timber

/**
 * Camera参数提供者工厂
 * 
 * 注意：由于应用的minSdk是26（Android 8.0），Camera2 API在API 21+已可用，
 * 所以总是使用Camera2ParamProvider，不需要版本检查。
 */
object CameraParamProviderFactory {

    fun create(context: Context): CameraParamProvider {
        // minSdk是26，Camera2 API总是可用
        Timber.d("Using Camera2ParamProvider (minSdk=26, Camera2 always available)")
        return Camera2ParamProvider(context)
    }
}