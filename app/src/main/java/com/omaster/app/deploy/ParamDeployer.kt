package com.omaster.app.deploy

import com.omaster.app.model.CameraParams

/**
 * 参数部署结果
 */
sealed interface DeployResult {
    /**
     * 成功部署到相机
     */
    data class Success(val message: String = "参数已成功应用") : DeployResult

    /**
     * 需要用户手动操作
     */
    data class GuideUser(
        val steps: List<GuideStep>,
        val message: String = "请按照以下步骤手动设置参数"
    ) : DeployResult

    /**
     * 部署失败
     */
    data class Failure(val reason: String, val exception: Throwable? = null) : DeployResult
}

/**
 * 引导步骤
 */
data class GuideStep(
    val order: Int,
    val title: String,
    val description: String,
    val actionHint: String? = null
)

/**
 * 参数部署器接口
 *
 * 设计原则：
 * - 不承诺能直接写入系统相机（Android限制）
 * - 插件式架构，支持不同部署方式
 * - 用户辅助操作是主要方式
 */
interface ParamDeployer {
    /**
     * 部署器名称
     */
    val name: String

    /**
     * 是否可用
     */
    val isAvailable: Boolean

    /**
     * 优先级（越高越优先）
     */
    val priority: Int

    /**
     * 部署参数
     */
    suspend fun deploy(params: CameraParams): DeployResult
}

/**
 * 部署器优先级
 */
object DeployerPriority {
    const val SYSTEM = 1000
    const val SHIZUKU = 500
    const val MANUAL = 0
}

/**
 * 手动引导部署器（最安全的默认方式）
 *
 * 不需要任何特殊权限，只是给用户清晰的操作指引
 */
class ManualGuideDeployer : ParamDeployer {
    override val name: String = "手动引导"
    override val isAvailable: Boolean = true
    override val priority: Int = DeployerPriority.MANUAL

    override suspend fun deploy(params: CameraParams): DeployResult {
        val steps = buildGuideSteps(params)
        return DeployResult.GuideUser(steps)
    }

    private fun buildGuideSteps(params: CameraParams): List<GuideStep> {
        val steps = mutableListOf<GuideStep>()

        // 步骤1: 打开相机应用
        steps.add(
            GuideStep(
                order = 1,
                title = "打开相机",
                description = "在手机上打开系统相机应用",
                actionHint = "点击打开相机"
            )
        )

        // 步骤2: 进入哈苏大师模式
        steps.add(
            GuideStep(
                order = 2,
                title = "进入哈苏大师模式",
                description = "在相机顶部或侧边找到「哈苏大师」或「专业模式」",
                actionHint = "切换到${params.mode}"
            )
        )

        // 步骤3: 设置ISO
        steps.add(
            GuideStep(
                order = 3,
                title = "设置 ISO",
                description = "在相机参数中设置 ISO 为 ${params.iso}",
                actionHint = "ISO: ${params.iso}"
            )
        )

        // 步骤4: 设置快门速度
        steps.add(
            GuideStep(
                order = 4,
                title = "设置快门速度",
                description = "调整快门速度为 ${params.shutter}",
                actionHint = "快门: ${params.shutter}"
            )
        )

        // 步骤5: 设置白平衡
        steps.add(
            GuideStep(
                order = 5,
                title = "设置白平衡",
                description = "设置白平衡为 ${params.wb}",
                actionHint = "WB: ${params.wb}"
            )
        )

        // 步骤6: 设置色彩风格
        if (params.colorStyle.isNotEmpty()) {
            steps.add(
                GuideStep(
                    order = 6,
                    title = "设置色彩风格",
                    description = "选择色彩风格为 ${params.colorStyle}",
                    actionHint = "色彩: ${params.colorStyle}"
                )
            )
        }

        // 步骤7: 应用哈苏色彩
        if (params.hasselblad_hncs) {
            steps.add(
                GuideStep(
                    order = 7,
                    title = "启用哈苏色彩",
                    description = "打开哈苏自然色彩解决方案 (HNCS)",
                    actionHint = "HNCS: 开启"
                )
            )
        }

        return steps
    }
}

/**
 * 复制到剪贴板部署器
 *
 * 将参数格式化为可读文本，让用户参考
 */
class ClipboardDeployer : ParamDeployer {
    override val name: String = "复制参数"
    override val isAvailable: Boolean = true
    override val priority: Int = DeployerPriority.MANUAL + 50

    override suspend fun deploy(params: CameraParams): DeployResult {
        val formattedParams = formatParams(params)
        return DeployResult.GuideUser(
            steps = listOf(
                GuideStep(
                    order = 1,
                    title = "复制参数",
                    description = "点击按钮复制所有参数到剪贴板",
                    actionHint = formattedParams
                ),
                GuideStep(
                    order = 2,
                    title = "参考设置",
                    description = "打开相机，按照复制的参数手动设置",
                    actionHint = "粘贴参考"
                )
            )
        )
    }

    private fun formatParams(params: CameraParams): String {
        return buildString {
            appendLine("📷 OPPO 哈苏大师模式参数")
            appendLine("──────────────────────")
            appendLine("模式: ${params.mode}")
            appendLine("ISO: ${params.iso}")
            appendLine("快门: ${params.shutter}")
            appendLine("EV: ${params.ev}")
            appendLine("白平衡: ${params.wb}")
            appendLine("焦距: ${params.focalLength}")
            appendLine("光圈: ${params.aperture}")
            appendLine("色彩风格: ${params.colorStyle}")
            appendLine("HNCS: ${if (params.hasselblad_hncs) "开启" else "关闭"}")
            appendLine("色彩科学: ${params.hasselbladColorScience}")
            appendLine("清晰度: ${params.sharpness}%")
            appendLine("对比度: ${params.contrast}%")
            appendLine("饱和度: ${params.saturation}%")
        }
    }
}
