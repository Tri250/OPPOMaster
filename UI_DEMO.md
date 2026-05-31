# OMaster 应用UI功能展示

## 项目概述
- **应用**: OMaster - 哈苏大师预设库
- **版本**: 1.2.1
- **架构**: Jetpack Compose + Material 3

---

## 屏幕导航

### 导航配置
- **MainActivity.kt** - 路由管理
- **OMasterScreen.kt** - 屏幕定义

#### 主要路由
- `/home` - 首页（预设列表）
- `/detail/{presetId}` - 预设详情
- `/settings` - 设置页面
- `/scene-detection` - 场景检测
- `/ai-fine-tune` - AI微调
- `/camera-config` - 相机配置
- `/profile` - 用户资料

---

## 1. 首页 - ProHomeScreenV2

### 界面布局
```
┌─────────────────────────────────┐
│  🔍 Search        ⚙️  +  🏠     │  ← 顶部栏
├─────────────────────────────────┤
│  全部  收藏  HNCS  Find X  Reno │  ← 筛选芯片
│  最新  热门                     │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────┐            │
│  │   预设卡片1     │            │  ← 预设列表
│  ├─────────────────┤            │
│  │   预设卡片2     │            │
│  ├─────────────────┤            │
│  │      ...        │            │
│  └─────────────────┘            │
├─────────────────────────────────┤
│  🏠  🔍  🎨  ⚙️  👤            │  ← 底部导航
└─────────────────────────────────┘
```

### 核心功能
- ✅ 搜索栏 - 实时搜索预设（名称/设备/标签）
- ✅ 筛选芯片 - 7种筛选选项（全部/收藏/HNCS/Find X/Reno/最新/热门）
- ✅ 预设列表 - 懒加载卡片展示
- ✅ 加载状态 - 骨架屏动画
- ✅ 空状态 - 友好提示

### 主要文件
- [ProHomeScreenV2.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/ProHomeScreenV2.kt)
- [GlassPresetCard.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/components/GlassPresetCard.kt)
- [GlassFilterChips.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/components/GlassFilterChips.kt)

---

## 2. 预设详情 - ProDetailScreen

### 界面布局
```
┌─────────────────────────────────┐
│ ←  ♥  ⋮                         │  ← 顶部栏
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐ │
│  │                           │ │
│  │      预设封面图片         │ │  ← 大图预览
│  │                           │ │
│  └───────────────────────────┘ │
│                                 │
│  哈苏人像经典                    │
│  OPPO Find X8 Ultra            │  ← 标题信息
│  ★★★★☆ 10,234 次下载           │
├─────────────────────────────────┤
│  📷 Mode: Portrait              │
│  🔍 ISO: 100                    │  ← 参数展示
│  ⏱️ Shutter: 1/125s            │
│  ☀️ EV: +0.3                   │
│  🎨 WB: Auto                   │
│  📏 Focal: 50mm                │
│  ◎ Aperture: f/1.7             │
│  HNCS: ✅ Certified             │
├─────────────────────────────────┤
│  🖼️ 样张展示                    │
│  [图1] [图2] [图3]             │  ← 样张网格
├─────────────────────────────────┤
│                                 │
│  使用说明: ...                  │  ← 详细说明
├─────────────────────────────────┤
│        应用预设                 │  ← 底部按钮
└─────────────────────────────────┘
```

### 核心功能
- ✅ 全屏封面图预览
- ✅ 完整相机参数展示
- ✅ HNCS认证标识（哈苏认证）
- ✅ 收藏/取消收藏
- ✅ 样张展示网格
- ✅ 使用说明文本
- ✅ 应用预设按钮
- ✅ 分享功能

### 主要文件
- [ProDetailScreen.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/ProDetailScreen.kt)

---

## 3. 设置页面 - ProSettingsScreenV2

### 界面布局
```
┌─────────────────────────────────┐
│ ← 设置                          │
├─────────────────────────────────┤
│  🎨 外观                        │
│  ┌───────────────────────────┐ │
│  │  🌞 浅色  ●               │ │
│  │  🌙 深色  ○               │ │  ← 主题切换
│  │  🔄 跟随系统 ○           │ │
│  └───────────────────────────┘ │
├─────────────────────────────────┤
│  ⚙️ 功能                        │
│  ┌───────────────────────────┐ │
│  │  ☁️  流体云胶囊    [✓]    │ │
│  │  🔄  网络同步      [✓]    │ │  ← 功能开关
│  │  📌  悬浮窗        [✓]    │ │
│  └───────────────────────────┘ │
├─────────────────────────────────┤
│  ℹ️ 关于                        │
│  ┌───────────────────────────┐ │
│  │  OMaster v1.2.1           │ │  ← 版本信息
│  │  检查更新...              │ │
│  │  隐私政策                 │ │
│  └───────────────────────────┘ │
└─────────────────────────────────┘
```

### 核心功能
- ✅ 主题模式切换（浅色/深色/跟随系统）
- ✅ 流体云胶囊开关
- ✅ 网络同步开关（默认开启）
- ✅ 悬浮窗开关
- ✅ 版本信息展示
- ✅ 检查更新
- ✅ 隐私政策

### 主要文件
- [ProSettingsScreenV2.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/ProSettingsScreenV2.kt)

---

## 4. 场景检测 - SceneDetectionScreenV2

### 界面布局
```
┌─────────────────────────────────┐
│ ← 场景检测                      │
├─────────────────────────────────┤
│                                 │
│  ┌───────────────────────────┐ │
│  │                           │ │
│  │     📷 相机预览           │ │
│  │                           │ │
│  └───────────────────────────┘ │
│                                 │
│  检测到: 🌅 日落场景           │  ← 识别结果
│  置信度: 92%                   │
├─────────────────────────────────┤
│  推荐预设:                      │
│  ┌─────────────────┐            │
│  │ 哈苏风景大师    │            │
│  ├─────────────────┤            │
│  │ 海岛风情        │            │
│  └─────────────────┘            │
└─────────────────────────────────┘
```

### 核心功能
- ✅ AI场景识别
- ✅ 实时相机预览
- ✅ 推荐相关预设
- ✅ 置信度显示

### 主要文件
- [SceneDetectionScreenV2.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreenV2.kt)

---

## 5. AI微调 - AiFineTuneScreen

### 界面布局
```
┌─────────────────────────────────┐
│ ← AI微调                        │
├─────────────────────────────────┤
│  选择预设:                      │
│  [下拉菜单选择...]              │
├─────────────────────────────────┤
│  🎨 参数调节:                   │
│                                 │
│  曝光补偿: ────────○─────      │  ← 滑动条
│  对比度:   ───────○──────      │
│  饱和度:   ────○────────      │
│  锐度:     ──────○──────      │
├─────────────────────────────────┤
│  实时预览:                      │
│  ┌───────────────────────────┐ │
│  │  原图  ↔  调节后          │ │
│  └───────────────────────────┘ │
├─────────────────────────────────┤
│      [保存自定义预设]          │
└─────────────────────────────────┘
```

### 核心功能
- ✅ 预设参数微调
- ✅ 实时对比预览
- ✅ 保存自定义预设
- ✅ 曝光/对比度/饱和度/锐度调节

### 主要文件
- [AiFineTuneScreen.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt)

---

## 6. 相机配置 - CameraConfigScreen

### 界面布局
```
┌─────────────────────────────────┐
│ ← 相机配置                      │
├─────────────────────────────────┤
│  📷 设备信息:                   │
│  设备: OPPO Find X8 Ultra       │
│  相机: IMX989 (主摄)            │
│  最大分辨率: 50MP               │
├─────────────────────────────────┤
│  ⚙️ 高级设置:                   │
│  ┌───────────────────────────┐ │
│  │ RAW 格式: [ON/OFF]        │ │
│  │ HDR+:     [ON/OFF]        │ │  ← 开关
│  │ AI 场景:   [ON/OFF]        │ │
│  └───────────────────────────┘ │
├─────────────────────────────────┤
│  📋 预设导入/导出:              │
│  [导入预设]  [导出备份]        │
└─────────────────────────────────┘
```

### 核心功能
- ✅ 设备信息展示
- ✅ 相机参数配置
- ✅ 预设导入/导出

### 主要文件
- [CameraConfigScreen.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/CameraConfigScreen.kt)

---

## 7. 用户资料 - ProfileScreen

### 界面布局
```
┌─────────────────────────────────┐
│ ← 个人中心                      │
├─────────────────────────────────┤
│                                 │
│       👤                       │  ← 用户头像
│    用户名称                    │
│    @username                   │
│                                 │
├─────────────────────────────────┤
│  📊 统计:                       │
│  收藏预设: 12                  │
│  使用次数: 1,234               │
│  下载预设: 45                  │
├─────────────────────────────────┤
│  我的收藏:                      │
│  [预设网格展示]                 │
└─────────────────────────────────┘
```

### 核心功能
- ✅ 用户信息展示
- ✅ 使用统计
- ✅ 收藏列表

### 主要文件
- [ProfileScreen.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/screens/ProfileScreen.kt)

---

## 8. 底部导航 - OMasterBottomBar

### 导航栏布局
```
┌─────────────────────────────────┐
│  🏠    🔍    🎨    ⚙️    👤    │
│  首页  搜索  AI  设置  我的    │
└─────────────────────────────────┘
```

### 导航项
1. 首页 - 预设浏览
2. 搜索 - 发现页面
3. AI - 场景检测
4. 设置 - 应用设置
5. 我的 - 个人中心

### 主要文件
- [OMasterBottomBar.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/components/OMasterBottomBar.kt)

---

## UI组件库

### 玻璃态组件 (Glass Morphism)
- `GlassPresetCard` - 预设卡片
- `GlassTopAppBar` - 顶部栏
- `GlassFilterChips` - 筛选芯片
- `GlassActionButton` - 操作按钮

### 颜色系统
- `ColorOSTheme` - ColorOS 16 主题
- `Material 3` 设计语言
- 支持深色/浅色模式

### 动画系统
- `ColorOSAnimationDuration` - 动画时长
- `ColorOSEasing` - 缓动曲线
- `ColorOSScale` - 缩放动画

### 主题文件
- [Theme.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/theme/Theme.kt)
- [Color.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/theme/Color.kt)
- [Type.kt](file:///workspace/app/src/main/java/com/omaster/app/ui/theme/Type.kt)

---

## 预设库内容

### 6个哈苏认证预设
1. **哈苏人像经典** - OPPO Find X8 Ultra
2. **哈苏风景大师** - OPPO Find X8 Ultra
3. **哈苏夜景大师** - OPPO Find X8 Ultra
4. **哈苏美食摄影** - OPPO Find X8 Ultra
5. **哈苏街拍模式** - OnePlus 13 Pro
6. **海岛风情** - realme GT7 Pro

### 每个预设包含
- 封面图片
- 样张展示
- 完整相机参数
- HNCS认证标识
- 使用说明

---

## 构建指南

### Debug APK构建
```bash
cd /workspace/app
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"

cd /workspace
./gradlew clean assembleDebug
```

### 输出文件
- `app/build/outputs/apk/debug/app-debug.apk`

---

## 仓库链接
- **主仓库**: https://github.com/Tri250/OPPOMaster
- **最新提交**: https://github.com/Tri250/OPPOMaster/commit/4265a96
