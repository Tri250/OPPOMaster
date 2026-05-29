# OPPO Master 技术提升路线图

## 概述

本文档基于对 iCurrer/OMaster 等影像工具的分析，为 OPPO Master 提供专业的技术提升方案。

**核心策略：去伪存真，夯实底座，适度前瞻。**

## 一、架构治理（近期）

### 1.1 状态管理与数据流标准化 ✨ 高优先级

**现状**：当前项目已有一定基础，需要完善。

**目标架构**：
- MVI (Model-View-Intent) + Kotlin Flow
- 使用 StateFlow 暴露 UI State
- 定义清晰的 UiState (Loading/Success/Error) 和 UiEvent

**实现要点**：
```kotlin
// UiState 示例
sealed interface PresetUiState {
    object Loading : PresetUiState
    data class Success(val presets: List<Preset>) : PresetUiState
    data class Error(val message: String) : PresetUiState
}

// ViewModel 示例
@HiltViewModel
class PresetViewModel @Inject constructor(
    private val presetRepository: PresetRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PresetUiState>(PresetUiState.Loading)
    val uiState: StateFlow<PresetUiState> = _uiState.asStateFlow()
    
    fun loadPresets() {
        viewModelScope.launch {
            presetRepository.getPresets().collect { result ->
                _uiState.value = when (result) {
                    is Result.Success -> PresetUiState.Success(result.data)
                    is Result.Error -> PresetUiState.Error(result.exception.message ?: "Error")
                }
            }
        }
    }
}
```

### 1.2 模块化拆分（中期）

**目标架构**：
```
:app                    (壳 + Navigation)
:core-ui               (Compose 基础组件、Theme、Preview)
:core-data             (DataStore, Network, Preset Repo)
:feature-presets       (预设列表/详情/收藏)
:feature-floating      (悬浮窗业务逻辑，隔离独立)
:feature-capture       (未来可能的相机交互)
```

**收益**：
- 编译加速
- 按需加载
- 便于多团队/多 PR 并行开发

## 二、核心功能技术深化

### 2.1 悬浮窗技术加固 🎯 P0 稳定性

**现状**：已具备基础功能，需要加固。

**权限策略**：
1. **检测 OverlayPermission**
2. **针对国产 ROM 专属引导**：
   - ColorOS（OPPO/OnePlus）
   - MIUI（小米）
   - HarmonyOS（华为）
   
**引导文案**不仅是跳转设置，还要告诉用户勾选哪个开关。

**渲染优化**：
- 使用 SurfaceView 或 TextureView 承载复杂动画（如水印预览）
- 避免主线程绘制阻塞
- 悬浮窗 View 采用 Compose in View，利用 GraphicsLayer 做半透明硬件加速

### 2.2 预设引擎：从文本升级为 DSL（中期）

**现状**：JSON 格式。

**升级方案**：定义 Preset DSL (Domain Specific Language)

```kotlin
// DSL 示例
preset("Hasselblad Natural") {
    metadata {
        author = "OPPO Master"
        tags = listOf("Portrait", "Natural", "Hasselblad")
        description = "哈苏自然色彩，适合人像和风景"
    }
    
    exposure {
        iso = 100
        shutter = "1/200"
        ev = 0.0
        metering = MeteringMode.SPOT
    }
    
    color {
        wb = 5600
        saturation = -0.1
        contrast = -0.05
    }
    
    camera {
        mode = CameraMode.MASTER
        hasselbladNaturalColor = true
        hasselbladHNCS = true
    }
    
    conditions {
        scene(Scene.PORTRAIT) { recommend() }
        scene(Scene.LANDSCAPE) { recommend() }
    }
}
```

**收益**：
- 支持更复杂的逻辑（条件判断）
- 类型安全，编译器检查
- 更易扩展和维护

### 2.3 高性能图片与资源管理 ✨ 已部分实现

**图片加载**：Coil（对 Compose 支持最好）✅ 已引入

**缓存策略**：
- 双层缓存（内存 LRU + 磁盘）
- 强制使用 WebP 格式减少体积和内存

## 三、数据与后端架构（务实版）

### 3.1 预设分发：GitOps 模式

**方案**：不搭建后端服务器，直接用 GitHub Repository 作为数据库。

**实现**：
1. `presets.json` 放在独立的 Community 仓库
2. App 启动时拉取 Raw URL
3. 利用 GitHub Release 的 tag 做版本控制和增量更新

**优点**：
- 免费、CDN 快
- 天然支持 PR 审核
- 无需运维

### 3.2 本地数据库升级（中期）

**方案**：从 SharedPreferences 迁移到 Room

**用途**：
- 存储用户的收藏夹
- 存储浏览历史
- 缓存云端预设（离线可用）

## 四、质量保障体系（区分 Demo 与产品）

### 4.1 自动化测试金字塔（近期）

**单元测试 (JUnit)**：
- 测试预设解析逻辑
- 测试业务计算

**集成测试**：
- 测试 DataStore 读写

**UI 测试 (Compose Test)**：
- 验证核心路径：打开 App -> 点击预设 -> 弹出悬浮窗 -> 切换预设
- 防止 Compose 重组导致的内存泄漏

### 4.2 CI/CD 流水线 ✨ 已创建

**GitHub Actions** 已配置：
- ✅ PR 检查：构建、测试、Lint
- ✅ 自动构建：Push Tag 自动打包 Release APK

## 五、进阶技术储备（谨慎引入）

### 5.1 AI 场景识别（低成本实现）

**拒绝**：本地跑大模型（耗电、慢）

**推荐**：ML Kit Image Labeling
- 调用 Google 的轻量级模型
- 识别画面中有"Food"、"Building"、"Person"
- 仅用于推荐预设，绝不修改系统相机

### 5.2 Camera2 集成（仅限"读数"）

**边界**：只做 Preview 流分析，不做拍照

**用途**：实时显示当前的 EV/WB 数值（作为教学辅助），而不是控制相机

**警告**：绝对不要尝试绕过 OPPO 的私有 API 去强行改参数，会被系统风控

## 六、技术债务清理清单

- [x] CI/CD: GitHub Actions 配置
- [ ] 架构: 逐步规划模块化
- [ ] DI: 完善 Hilt 使用，规范依赖图
- [ ] UI: 全面检查 Material 3 适配
- [ ] 性能: 使用 Baseline Profiles 提升启动速度
- [ ] 存储: 规划 Room 迁移路径
- [x] 安全: 已启用 R8 混淆
- [ ] 适配: 检查 Android 14/15 的 FGS 权限
- [ ] 监控: 评估 Firebase Crashlytics（如面向海外）

## 七、技术维度评分修正

| 维度 | 当前 | 目标 | 提升项 |
|------|------|------|--------|
| 架构规范性 | 6.5 | 8.0 | MVI 标准化、模块化 |
| 稳定性/健壮性 | 7.0 | 9.0 | 悬浮窗加固、测试覆盖 |
| 扩展性 | 6.0 | 8.5 | 预设 DSL、模块化架构 |
| 技术前瞻性 | 5.0 | 7.0 | 务实 AI、性能优化 |
| **综合技术分** | **6.1** | **8.3** | - |

## 八、执行优先级

### Phase 1（1-2周）：夯实基础
- [x] CI/CD 配置
- [ ] 完善悬浮窗权限检测
- [ ] 优化预设加载性能
- [ ] 完善状态管理

### Phase 2（1个月）：架构优化
- [ ] Room 数据库集成
- [ ] 预设 DSL 设计
- [ ] 完善测试覆盖

### Phase 3（2-3个月）：进阶功能
- [ ] 低成本 AI 场景识别
- [ ] Camera2 参数读取
- [ ] GitOps 预设分发

---

**结论**：OPPO Master 项目已经有很好的基础，通过上述技术提升，可以从"可使用的工具"转变为"技术优秀的产品"。
