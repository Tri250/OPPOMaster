package com.omaster.app.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 重试拦截器
 * 在网络请求失败时自动重试
 */
class RetryInterceptor(
    private val maxRetryCount: Int = 3,
    private val retryInterval: Long = 1000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var retryCount = 0

        while (retryCount <= maxRetryCount) {
            try {
                val response = chain.proceed(request)
                // 如果响应成功或者是客户端错误(4xx)，不重试
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                lastException = e
                // 只对特定异常重试
                if (!shouldRetry(e)) {
                    throw e
                }
            }

            retryCount++
            if (retryCount <= maxRetryCount) {
                Thread.sleep(retryInterval * retryCount)
            }
        }

        throw lastException ?: IOException("Unknown error occurred after $maxRetryCount retries")
    }

    private fun shouldRetry(exception: IOException): Boolean {
        return exception is SocketTimeoutException ||
                exception is UnknownHostException ||
                exception.message?.contains("connection", ignoreCase = true) == true
    }
}