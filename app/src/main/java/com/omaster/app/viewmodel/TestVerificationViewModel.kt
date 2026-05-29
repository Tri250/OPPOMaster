package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import com.omaster.app.ui.screens.TestCategory
import com.omaster.app.ui.screens.TestItem
import com.omaster.app.ui.screens.TestStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TestVerificationViewModel @Inject constructor() : ViewModel() {
    
    private val _testItems = MutableStateFlow(generateTestItems())
    val testItems: StateFlow<List<TestItem>> = _testItems.asStateFlow()
    
    fun updateTestStatus(testId: Int, newStatus: TestStatus) {
        _testItems.value = _testItems.value.map { item ->
            if (item.id == testId) {
                item.copy(status = newStatus)
            } else {
                item
            }
        }
        Timber.d("Test item $testId status changed to $newStatus")
    }
    
    private fun generateTestItems(): List<TestItem> {
        return listOf(
            // 场景识别准确性
            TestItem(
                id = 1,
                name = "自然风光场景识别",
                category = TestCategory.SCENE_RECOGNITION,
                steps = listOf(
                    "拍摄天空、山水、树木等自然风景照片",
                    "打开小O帮帮应用",
                    "点击场景识别按钮",
                    "等待AI识别完成"
                ),
                expectedResult = "系统能够准确识别出'风景'场景，并推荐相应的哈苏预设",
                acceptanceCriteria = "识别准确率≥95%，推荐相关度≥90%"
            ),
            TestItem(
                id = 2,
                name = "人像场景识别",
                category = TestCategory.SCENE_RECOGNITION,
                steps = listOf(
                    "拍摄包含人脸的证件照或生活照",
                    "打开小O帮帮应用",
                    "点击场景识别按钮",
                    "验证识别结果"
                ),
                expectedResult = "系统准确识别出'人像'场景，并推荐适合人像的预设",
                acceptanceCriteria = "人像识别准确率≥98%"
            ),
            TestItem(
                id = 3,
                name = "夜景场景识别",
                category = TestCategory.SCENE_RECOGNITION,
                steps = listOf(
                    "在低光环境下拍摄夜景照片",
                    "打开小O帮帮应用",
                    "点击场景识别按钮",
                    "检查识别结果"
                ),
                expectedResult = "系统准确识别出'夜景'场景，推荐低光优化预设",
                acceptanceCriteria = "夜景识别准确率≥92%"
            ),
            
            // 特殊场景测试
            TestItem(
                id = 4,
                name = "逆光场景处理",
                category = TestCategory.SPECIAL_SCENES,
                steps = listOf(
                    "在强逆光环境下拍摄照片",
                    "打开小O帮帮应用",
                    "应用推荐的预设",
                    "检查照片效果"
                ),
                expectedResult = "预设能够自动调整曝光，保留高光和阴影细节",
                acceptanceCriteria = "高光不过曝，暗部有细节"
            ),
            TestItem(
                id = 5,
                name = "运动模糊场景",
                category = TestCategory.SPECIAL_SCENES,
                steps = listOf(
                    "拍摄运动中的物体（如奔跑的人）",
                    "打开小O帮帮应用",
                    "应用推荐的预设",
                    "评估运动模糊处理效果"
                ),
                expectedResult = "系统检测运动场景并提供防抖建议",
                acceptanceCriteria = "提供清晰的拍摄建议或运动模式预设"
            ),
            
            // 参数自动填入
            TestItem(
                id = 6,
                name = "相机参数读取",
                category = TestCategory.AUTO_FILL,
                steps = listOf(
                    "打开小O帮帮应用",
                    "连接支持的OPPO/一加设备",
                    "拍摄一张照片",
                    "检查参数是否自动填入"
                ),
                expectedResult = "系统自动读取并填入ISO、白平衡、曝光补偿等参数",
                acceptanceCriteria = "参数读取成功率≥99%"
            ),
            TestItem(
                id = 7,
                name = "参数同步延迟",
                category = TestCategory.AUTO_FILL,
                steps = listOf(
                    "在应用中调整相机参数",
                    "立即在实际相机中查看",
                    "记录同步延迟时间",
                    "重复测试5次取平均值"
                ),
                expectedResult = "参数同步延迟≤500ms",
                acceptanceCriteria = "平均延迟<500ms，最大延迟<1s"
            ),
            
            // 悬浮窗功能
            TestItem(
                id = 8,
                name = "悬浮窗显示",
                category = TestCategory.FLOATING_WINDOW,
                steps = listOf(
                    "打开小O帮帮应用",
                    "启用悬浮窗功能",
                    "切换到其他应用",
                    "观察悬浮窗是否显示"
                ),
                expectedResult = "悬浮窗能够正常显示在屏幕上，可以拖动位置",
                acceptanceCriteria = "悬浮窗响应<100ms，支持多点触控"
            ),
            TestItem(
                id = 9,
                name = "悬浮窗快捷操作",
                category = TestCategory.FLOATING_WINDOW,
                steps = listOf(
                    "显示悬浮窗",
                    "点击悬浮窗上的快捷按钮",
                    "验证操作响应",
                    "检查功能是否正常"
                ),
                expectedResult = "悬浮窗提供场景识别、参数调整等快捷操作",
                acceptanceCriteria = "所有快捷按钮功能正常，无崩溃"
            ),
            
            // 分类搜索
            TestItem(
                id = 10,
                name = "关键词搜索",
                category = TestCategory.CATEGORY_SEARCH,
                steps = listOf(
                    "打开小O帮帮应用",
                    "点击搜索框",
                    "输入关键词（如'哈苏'、'人像'）",
                    "观察搜索结果"
                ),
                expectedResult = "搜索结果准确，支持模糊匹配",
                acceptanceCriteria = "搜索响应时间<500ms，结果相关性≥90%"
            ),
            TestItem(
                id = 11,
                name = "分类筛选",
                category = TestCategory.CATEGORY_SEARCH,
                steps = listOf(
                    "打开小O帮帮应用",
                    "点击分类标签",
                    "切换不同分类",
                    "验证筛选结果"
                ),
                expectedResult = "分类筛选准确，显示对应分类的预设",
                acceptanceCriteria = "分类显示正确，无遗漏或错误"
            ),
            
            // 预设生态
            TestItem(
                id = 12,
                name = "预设下载",
                category = TestCategory.PRESET_ECOSYSTEM,
                steps = listOf(
                    "打开小O帮帮应用",
                    "浏览在线预设库",
                    "点击下载按钮",
                    "等待下载完成"
                ),
                expectedResult = "预设能够成功下载并保存到本地",
                acceptanceCriteria = "下载成功率≥99%，失败重试机制正常"
            ),
            TestItem(
                id = 13,
                name = "预设收藏",
                category = TestCategory.PRESET_ECOSYSTEM,
                steps = listOf(
                    "打开小O帮帮应用",
                    "浏览预设列表",
                    "点击收藏按钮",
                    "验证收藏列表"
                ),
                expectedResult = "预设成功添加到收藏列表",
                acceptanceCriteria = "收藏操作响应<200ms，数据持久化正常"
            ),
            
            // 多格式导入导出
            TestItem(
                id = 14,
                name = "DNG格式导入",
                category = TestCategory.MULTI_FORMAT,
                steps = listOf(
                    "准备DNG格式的样片",
                    "打开小O帮帮应用",
                    "导入DNG文件",
                    "验证参数读取"
                ),
                expectedResult = "成功导入DNG格式，保留所有RAW数据",
                acceptanceCriteria = "DNG识别成功率100%，参数完整"
            ),
            TestItem(
                id = 15,
                name = "JPEG格式导出",
                category = TestCategory.MULTI_FORMAT,
                steps = listOf(
                    "应用预设到照片",
                    "选择JPEG格式导出",
                    "检查导出质量",
                    "验证文件兼容性"
                ),
                expectedResult = "成功导出JPEG格式，画质无损",
                acceptanceCriteria = "JPEG质量≥95%，色彩准确"
            ),
            
            // 性能测试
            TestItem(
                id = 16,
                name = "冷启动时间",
                category = TestCategory.PERFORMANCE,
                steps = listOf(
                    "完全关闭应用",
                    "记录起始时间",
                    "启动小O帮帮应用",
                    "记录应用完全加载时间"
                ),
                expectedResult = "应用冷启动时间≤3秒",
                acceptanceCriteria = "启动时间≤3s，无白屏或卡顿"
            ),
            TestItem(
                id = 17,
                name = "界面响应速度",
                category = TestCategory.PERFORMANCE,
                steps = listOf(
                    "打开小O帮帮应用",
                    "执行各种操作（搜索、筛选、收藏等）",
                    "记录每次操作的响应时间",
                    "统计平均响应时间"
                ),
                expectedResult = "所有界面操作响应时间≤2秒",
                acceptanceCriteria = "平均响应时间≤2s，最大响应≤3s"
            ),
            TestItem(
                id = 18,
                name = "内存占用",
                category = TestCategory.PERFORMANCE,
                steps = listOf(
                    "打开小O帮帮应用",
                    "正常使用10分钟",
                    "检查内存占用",
                    "验证内存泄漏"
                ),
                expectedResult = "应用内存占用稳定，无内存泄漏",
                acceptanceCriteria = "内存占用≤200MB，无持续增长"
            ),
            
            // 安全性测试
            TestItem(
                id = 19,
                name = "数据加密",
                category = TestCategory.SECURITY,
                steps = listOf(
                    "检查应用数据存储",
                    "验证加密方式",
                    "测试数据保护",
                    "检查隐私政策"
                ),
                expectedResult = "敏感数据使用加密存储",
                acceptanceCriteria = "所有敏感数据加密，无明文存储"
            ),
            TestItem(
                id = 20,
                name = "权限管理",
                category = TestCategory.SECURITY,
                steps = listOf(
                    "检查应用权限申请",
                    "验证权限必要性",
                    "测试权限拒绝处理",
                    "检查权限说明"
                ),
                expectedResult = "权限申请合理，有明确说明",
                acceptanceCriteria = "权限最小化，有清晰的隐私政策"
            )
        )
    }
}
