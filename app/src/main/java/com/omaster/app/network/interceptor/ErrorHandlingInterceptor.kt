package com.omaster.app.network.interceptor

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.io.IOException

/**
 * 统一错误处理拦截器
 * 处理服务器返回的错误响应，统一错误格式
 */
class ErrorHandlingInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // 如果响应成功，直接返回
        if (response.isSuccessful) {
            return response
        }

        // 处理错误响应
        val errorBody = response.peekBody(Long.MAX_VALUE).string()
        val errorMessage = parseErrorMessage(errorBody, response.code)

        Timber.e("HTTP Error ${response.code}: $errorMessage for ${request.url}")

        // 创建包含错误信息的响应体
        val errorJson = JsonObject().apply {
            addProperty("code", response.code)
            addProperty("message", errorMessage)
            addProperty("success", false)
        }

        return response.newBuilder()
            .body(errorJson.toString().toResponseBody(response.body?.contentType()))
            .build()
    }

    private fun parseErrorMessage(errorBody: String?, code: Int): String {
        if (errorBody.isNullOrBlank()) {
            return getDefaultErrorMessage(code)
        }

        return try {
            val json = gson.fromJson(errorBody, JsonObject::class.java)
            json.get("message")?.asString
                ?: json.get("error")?.asString
                ?: json.get("msg")?.asString
                ?: getDefaultErrorMessage(code)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse error body")
            getDefaultErrorMessage(code)
        }
    }

    private fun getDefaultErrorMessage(code: Int): String {
        return when (code) {
            400 -> "请求参数错误"
            401 -> "未授权，请先登录"
            403 -> "禁止访问"
            404 -> "请求的资源不存在"
            405 -> "请求方法不允许"
            408 -> "请求超时"
            500 -> "服务器内部错误"
            502 -> "网关错误"
            503 -> "服务暂时不可用"
            504 -> "网关超时"
            else -> "网络请求失败($code)"
        }
    }
}