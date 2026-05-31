package com.omaster.app.deploy

import android.content.Context
import com.omaster.app.model.CameraParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 参数部署管理器
 *
 * 负责：
 * - 注册和管理所有部署器
 * - 根据可用性选择最优部署器
 * - 执行部署操作
 */
@Singleton
class ParamDeployerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val deployers = mutableListOf<ParamDeployer>()

    init {
        // 注册部署器（按优先级从低到高）
        registerDeployer(ManualGuideDeployer())
        registerDeployer(ClipboardDeployer())
        // 可以在此添加更多部署器，如 ShizukuDeployer 等
    }

    /**
     * 注册部署器
     */
    fun registerDeployer(deployer: ParamDeployer) {
        deployers.add(deployer)
        // 按优先级降序排序
        deployers.sortByDescending { it.priority }
        Timber.d("已注册部署器: ${deployer.name}")
    }

    /**
     * 获取可用的部署器列表（按优先级排序）
     */
    fun getAvailableDeployers(): List<ParamDeployer> {
        return deployers.filter { it.isAvailable }
    }

    /**
     * 获取最佳部署器（优先级最高且可用）
     */
    fun getBestDeployer(): ParamDeployer? {
        return getAvailableDeployers().firstOrNull()
    }

    /**
     * 执行部署操作
     */
    suspend fun deploy(params: CameraParams, deployer: ParamDeployer? = null): DeployResult {
        return withContext(Dispatchers.IO) {
            val selectedDeployer = deployer ?: getBestDeployer()

            if (selectedDeployer == null) {
                Timber.e("没有可用的部署器")
                return@withContext DeployResult.Failure("没有可用的参数部署方式")
            }

            try {
                Timber.d("使用部署器: ${selectedDeployer.name}")
                selectedDeployer.deploy(params)
            } catch (e: Exception) {
                Timber.e(e, "部署失败")
                DeployResult.Failure(e.message ?: "部署失败", e)
            }
        }
    }

    /**
     * 尝试所有部署器直到成功
     */
    suspend fun tryAllDeployers(params: CameraParams): DeployResult {
        val availableDeployers = getAvailableDeployers()

        if (availableDeployers.isEmpty()) {
            return DeployResult.Failure("没有可用的部署方式")
        }

        for (deployer in availableDeployers) {
            try {
                Timber.d("尝试部署器: ${deployer.name}")
                val result = deployer.deploy(params)
                if (result !is DeployResult.Failure) {
                    return result
                }
            } catch (e: Exception) {
                Timber.e(e, "部署器 ${deployer.name} 执行失败")
            }
        }

        return DeployResult.Failure("所有部署方式都失败了")
    }
}
