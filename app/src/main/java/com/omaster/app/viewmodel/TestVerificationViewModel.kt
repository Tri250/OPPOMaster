package com.omaster.app.viewmodel

import androidx.lifecycle.ViewModel
import com.omaster.app.model.DeviceDatabase
import com.omaster.app.model.PresetDatabase
import com.omaster.app.model.SceneType
import com.omaster.app.ui.screens.TestCategory
import com.omaster.app.ui.screens.TestItem
import com.omaster.app.ui.screens.TestStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }
    
    private fun generateTestItems(): List<TestItem> {
        val testItems = mutableListOf<TestItem>()
        var id = 1
        
        // ==================== AI场景识别测试（20项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "AI识别准确率测试",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 准备100张不同场景的测试照片",
                "2. 使用AI场景识别功能分析每张照片",
                "3. 记录识别结果与实际场景的对比",
                "4. 计算准确率：识别正确数/总数×100%"
            ),
            expectedResult = "准确率≥95%",
            acceptanceCriteria = "识别准确率≥95%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "人像场景识别",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 拍摄10张人像照片（单人、多人、逆光）",
                "2. 使用AI场景识别分析",
                "3. 验证识别为人像类场景",
                "4. 检查推荐的参数是否匹配"
            ),
            expectedResult = "正确识别人像场景类型",
            acceptanceCriteria = "人像识别准确率≥98%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "风景场景识别",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 拍摄10张风景照片（山川、海洋、森林等）",
                "2. 使用AI场景识别分析",
                "3. 验证识别为风景类场景",
                "4. 检查推荐的参数是否匹配"
            ),
            expectedResult = "正确识别风景场景类型",
            acceptanceCriteria = "风景识别准确率≥97%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "夜景场景识别",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 拍摄10张夜景照片（城市、星空、霓虹）",
                "2. 使用AI场景识别分析",
                "3. 验证识别为夜景类场景",
                "4. 检查推荐的参数是否匹配"
            ),
            expectedResult = "正确识别夜景场景类型",
            acceptanceCriteria = "夜景识别准确率≥95%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "美食场景识别",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 拍摄10张美食照片（菜肴、甜点、饮品）",
                "2. 使用AI场景识别分析",
                "3. 验证识别为美食类场景",
                "4. 检查推荐的参数是否匹配"
            ),
            expectedResult = "正确识别美食场景类型",
            acceptanceCriteria = "美食识别准确率≥96%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "AI响应时间测试",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 连续测试100次AI场景识别",
                "2. 记录每次识别的响应时间",
                "3. 计算平均响应时间",
                "4. 检查是否满足性能要求"
            ),
            expectedResult = "平均响应时间≤500ms",
            acceptanceCriteria = "响应时间≤500ms",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "AI算法稳定性测试",
            category = TestCategory.SCENE_RECOGNITION,
            steps = listOf(
                "1. 同一张照片识别10次",
                "2. 记录每次识别结果",
                "3. 验证结果的一致性",
                "4. 检查算法稳定性"
            ),
            expectedResult = "相同照片识别结果一致",
            acceptanceCriteria = "识别一致性≥99%",
            status = TestStatus.PENDING
        ))
        
        // ==================== 特殊场景测试（10项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "低光照场景识别",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在极暗环境（<10 lux）下拍摄",
                "2. 使用AI场景识别分析",
                "3. 验证识别结果",
                "4. 检查推荐的夜景参数"
            ),
            expectedResult = "正确识别并推荐夜景参数",
            acceptanceCriteria = "低光照识别准确率≥95%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "逆光场景识别",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 拍摄逆光场景照片",
                "2. 使用AI场景识别分析",
                "3. 验证识别结果",
                "4. 检查是否推荐HDR或人像参数"
            ),
            expectedResult = "正确识别逆光场景",
            acceptanceCriteria = "逆光识别准确率≥90%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "运动模糊场景",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 拍摄运动模糊照片",
                "2. 使用AI场景识别分析",
                "3. 验证识别结果",
                "4. 检查是否提示拍摄建议"
            ),
            expectedResult = "正确识别并提示拍摄建议",
            acceptanceCriteria = "模糊场景处理正确",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "混合场景识别",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 拍摄包含多种元素的复杂场景",
                "2. 使用AI场景识别分析",
                "3. 验证主场景识别",
                "4. 检查推荐的参数是否合理"
            ),
            expectedResult = "正确识别主要场景",
            acceptanceCriteria = "混合场景识别准确率≥85%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "OPPO Find系列专属测试",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在Find X7 Ultra上测试所有场景",
                "2. 验证哈苏影像优化",
                "3. 检查专属预设应用",
                "4. 验证AI场景识别效果"
            ),
            expectedResult = "完美支持Find X7 Ultra",
            acceptanceCriteria = "Find系列支持率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "一加哈苏影像测试",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在OnePlus 12上测试所有场景",
                "2. 验证哈苏XPan模式",
                "3. 检查一加专属预设",
                "4. 验证色彩优化效果"
            ),
            expectedResult = "完美支持OnePlus 12",
            acceptanceCriteria = "一加系列支持率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "小米徕卡影像测试",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在Xiaomi 14 Ultra上测试",
                "2. 验证徕卡经典/生动模式",
                "3. 检查徕卡专属预设",
                "4. 验证徕卡色彩优化"
            ),
            expectedResult = "完美支持小米徕卡",
            acceptanceCriteria = "徕卡系列支持率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "vivo蔡司影像测试",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在vivo X100 Ultra上测试",
                "2. 验证蔡司T*镀膜优化",
                "3. 检查蔡司专属预设",
                "4. 验证蔡司色彩科学"
            ),
            expectedResult = "完美支持vivo蔡司",
            acceptanceCriteria = "蔡司系列支持率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "华为XMAGE影像测试",
            category = TestCategory.SPECIAL_SCENES,
            steps = listOf(
                "1. 在Mate 60 Pro+上测试",
                "2. 验证XMAGE原色/明快模式",
                "3. 检查XMAGE专属预设",
                "4. 验证华为影像优化"
            ),
            expectedResult = "完美支持华为XMAGE",
            acceptanceCriteria = "XMAGE系列支持率100%",
            status = TestStatus.PENDING
        ))
        
        // ==================== 参数自动填入测试（5项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "OPPO相机参数填入",
            category = TestCategory.AUTO_FILL,
            steps = listOf(
                "1. 打开OPPO Find X7 Ultra原生相机",
                "2. 识别场景后点击自动填入",
                "3. 验证参数是否正确填入",
                "4. 检查色彩模式、AI开关等"
            ),
            expectedResult = "参数正确填入OPPO相机",
            acceptanceCriteria = "参数填入准确率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "一加相机参数填入",
            category = TestCategory.AUTO_FILL,
            steps = listOf(
                "1. 打开OnePlus 12原生相机",
                "2. 识别场景后点击自动填入",
                "3. 验证哈苏参数是否正确填入",
                "4. 检查专业模式参数"
            ),
            expectedResult = "参数正确填入一加相机",
            acceptanceCriteria = "哈苏参数填入准确率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "六大品牌参数填入",
            category = TestCategory.AUTO_FILL,
            steps = listOf(
                "1. 测试OPPO、一加、小米、vivo、华为、realme",
                "2. 验证各品牌相机参数填入",
                "3. 记录成功和失败情况",
                "4. 优化失败场景"
            ),
            expectedResult = "支持六大品牌参数填入",
            acceptanceCriteria = "六大品牌支持率≥95%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "参数冲突处理",
            category = TestCategory.AUTO_FILL,
            steps = listOf(
                "1. 手动设置参数与AI推荐冲突",
                "2. 测试自动填入的处理逻辑",
                "3. 验证冲突解决方案",
                "4. 检查用户体验"
            ),
            expectedResult = "冲突处理逻辑正确",
            acceptanceCriteria = "冲突处理满意度≥90%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "无Root权限测试",
            category = TestCategory.AUTO_FILL,
            steps = listOf(
                "1. 在未Root设备上测试",
                "2. 验证无障碍服务功能",
                "3. 测试参数填入效果",
                "4. 检查权限申请流程"
            ),
            expectedResult = "无需Root即可使用",
            acceptanceCriteria = "无Root支持率100%",
            status = TestStatus.PENDING
        ))
        
        // ==================== 设备映射测试（5项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "OPPO Find系列设备识别",
            category = TestCategory.PRESET_ECOSYSTEM,
            steps = listOf(
                "1. 测试Find X7 Ultra识别",
                "2. 测试Find X6 Pro识别",
                "3. 测试Find N3识别",
                "4. 验证设备映射正确性"
            ),
            expectedResult = "正确识别OPPO Find系列",
            acceptanceCriteria = "Find系列识别率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "一加数字系列设备识别",
            category = TestCategory.PRESET_ECOSYSTEM,
            steps = listOf(
                "1. 测试OnePlus 12识别",
                "2. 测试OnePlus 11识别",
                "3. 测试OnePlus 10 Pro识别",
                "4. 验证哈苏影像映射"
            ),
            expectedResult = "正确识别一加数字系列",
            acceptanceCriteria = "一加系列识别率100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "六大品牌全覆盖测试",
            category = TestCategory.PRESET_ECOSYSTEM,
            steps = listOf(
                "1. 测试OPPO所有机型",
                "2. 测试一加所有机型",
                "3. 测试小米所有机型",
                "4. 测试vivo所有机型",
                "5. 测试华为所有机型",
                "6. 测试realme所有机型"
            ),
            expectedResult = "支持六大品牌全部机型",
            acceptanceCriteria = "六大品牌覆盖率≥99%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "设备搜索功能测试",
            category = TestCategory.PRESET_ECOSYSTEM,
            steps = listOf(
                "1. 测试按品牌搜索",
                "2. 测试按机型搜索",
                "3. 测试模糊搜索",
                "4. 测试搜索结果准确性"
            ),
            expectedResult = "搜索功能正常",
            acceptanceCriteria = "搜索准确率≥98%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "预设库规模测试",
            category = TestCategory.PRESET_ECOSYSTEM,
            steps = listOf(
                "1. 统计哈苏预设数量",
                "2. 统计徕卡预设数量",
                "3. 统计蔡司预设数量",
                "4. 统计XMAGE预设数量",
                "5. 统计通用预设数量",
                "6. 计算总预设数量"
            ),
            expectedResult = "预设库达到企业级规模",
            acceptanceCriteria = "预设总数≥1000个",
            status = TestStatus.PENDING
        ))
        
        // ==================== 性能测试（5项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "AI识别性能测试",
            category = TestCategory.PERFORMANCE,
            steps = listOf(
                "1. 测试100次AI场景识别",
                "2. 记录每次响应时间",
                "3. 计算平均响应时间",
                "4. 检查是否满足≤500ms要求"
            ),
            expectedResult = "平均响应时间≤500ms",
            acceptanceCriteria = "响应时间≤500ms",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "设备识别性能测试",
            category = TestCategory.PERFORMANCE,
            steps = listOf(
                "1. 测试100次设备识别",
                "2. 记录响应时间",
                "3. 验证识别速度",
                "4. 检查性能指标"
            ),
            expectedResult = "设备识别响应快",
            acceptanceCriteria = "识别时间≤200ms",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "预设加载性能测试",
            category = TestCategory.PERFORMANCE,
            steps = listOf(
                "1. 测试1000+预设加载",
                "2. 记录加载时间",
                "3. 验证加载性能",
                "4. 检查内存占用"
            ),
            expectedResult = "预设加载流畅",
            acceptanceCriteria = "加载时间≤2s",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "长时间运行稳定性",
            category = TestCategory.PERFORMANCE,
            steps = listOf(
                "1. 应用持续运行24小时",
                "2. 监控内存占用",
                "3. 监控CPU使用率",
                "4. 检查是否崩溃"
            ),
            expectedResult = "长时间运行稳定",
            acceptanceCriteria = "内存泄漏≤5%，无崩溃",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "高频操作测试",
            category = TestCategory.PERFORMANCE,
            steps = listOf(
                "1. 连续1000次AI识别操作",
                "2. 记录成功率和响应时间",
                "3. 监控系统状态",
                "4. 检查性能表现"
            ),
            expectedResult = "高频操作稳定",
            acceptanceCriteria = "操作成功率≥99.9%",
            status = TestStatus.PENDING
        ))
        
        // ==================== 兼容性测试（5项） ====================
        testItems.add(TestItem(
            id = id++,
            name = "ColorOS 14兼容性",
            category = TestCategory.FLOATING_WINDOW,
            steps = listOf(
                "1. 在ColorOS 14设备上测试",
                "2. 测试悬浮窗显示",
                "3. 测试参数填入",
                "4. 验证功能完整性"
            ),
            expectedResult = "完美兼容ColorOS 14",
            acceptanceCriteria = "兼容性100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "MIUI 14兼容性",
            category = TestCategory.FLOATING_WINDOW,
            steps = listOf(
                "1. 在MIUI 14设备上测试",
                "2. 测试悬浮窗显示",
                "3. 测试参数填入",
                "4. 验证功能完整性"
            ),
            expectedResult = "完美兼容MIUI 14",
            acceptanceCriteria = "兼容性100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "OriginOS兼容性",
            category = TestCategory.FLOATING_WINDOW,
            steps = listOf(
                "1. 在OriginOS设备上测试",
                "2. 测试悬浮窗显示",
                "3. 测试参数填入",
                "4. 验证功能完整性"
            ),
            expectedResult = "完美兼容OriginOS",
            acceptanceCriteria = "兼容性100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "HarmonyOS兼容性",
            category = TestCategory.FLOATING_WINDOW,
            steps = listOf(
                "1. 在HarmonyOS设备上测试",
                "2. 测试悬浮窗显示",
                "3. 测试参数填入",
                "4. 验证功能完整性"
            ),
            expectedResult = "完美兼容HarmonyOS",
            acceptanceCriteria = "兼容性100%",
            status = TestStatus.PENDING
        ))
        
        testItems.add(TestItem(
            id = id++,
            name = "跨品牌数据兼容性",
            category = TestCategory.FLOATING_WINDOW,
            steps = listOf(
                "1. 在OPPO设备创建预设",
                "2. 导出预设数据",
                "3. 在小米设备导入",
                "4. 验证数据兼容性"
            ),
            expectedResult = "跨品牌数据兼容",
            acceptanceCriteria = "数据兼容性≥98%",
            status = TestStatus.PENDING
        ))
        
        return testItems
    }
}
