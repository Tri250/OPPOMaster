# 小O帮帮 - 哈苏影像系统级标定基座

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 特性

### 核心功能
- 📸 **HNCS 认证预设** - 精选哈苏自然色彩解决方案认证预设
- 🎨 **10+ 水印模板** - 支持 OPPO/一加/realme/哈苏等品牌水印
- 🤖 **AI 智能场景识别** - 自动识别场景并推荐最佳预设
- ⚡ **流体云胶囊** - ColorOS 16 系统级侧边栏集成
- 🪟 **悬浮窗** - 全局参数显示与快捷操作
- 📊 **实时相机参数** - 实时读取 Camera2 参数

### 技术亮点
- 🎯 **ColorOS 16 设计语言** - 遵循 Aqua Design 水生设计语言
- 🚀 **高性能** - 流畅动画与响应式交互
- 🧪 **自动化测试** - 全面的单元测试与 UI 测试
- 🔒 **安全隐私** - 本地数据加密存储

## 支持设备

- **OPPO** Find X7/X6/X5 系列、Reno 10/9/8 系列
- **OnePlus** 12/11/10 系列
- **realme** GT5/GT Neo5 系列

## 系统要求

- Android 8.0+ (API 26)
- ColorOS 13+ / OxygenOS 13+

## 快速开始

### 环境要求
- JDK 17+
- Android Studio Hedgehog+
- Android SDK Platform 34/35

### 构建命令
```bash
# 构建 debug 版本
./gradlew assembleDebug

# 构建 release 版本
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## 项目结构

```
app/
├── src/main/java/com/omaster/app/
│   ├── ui/                    # UI 层
│   │   ├── screens/           # 页面组件
│   │   ├── components/        # 通用组件
│   │   ├── theme/             # 主题配置
│   │   └── navigation/        # 导航路由
│   ├── data/                  # 数据层
│   │   ├── repository/        # 数据仓库
│   │   └── network/           # 网络请求
│   ├── camera/                # Camera2 相机模块
│   ├── watermark/             # 水印处理
│   ├── screenshot/            # 截图功能
│   ├── floating/              # 悬浮窗模块
│   └── accessibility/         # 无障碍服务
└── src/test/                   # 测试代码
```

## 功能模块

### 1. 预设浏览
- 网格/列表布局切换
- 搜索与筛选
- 收藏管理
- HNCS 标识

### 2. 预设详情
- 参数详情展示
- 样片预览
- 一键应用
- 分享功能

### 3. AI 场景识别
- 图片上传
- 场景分类
- 预设推荐
- 参数对比

### 4. 水印编辑器
- 10+ 模板选择
- 自定义位置
- 批量处理
- 无损输出

### 5. 设置中心
- 主题切换（浅色/深色/跟随系统）
- 系统能力开关
- 关于应用

## 设计规范

### 色彩系统
- **主色**: #FF6B35 (Accent Primary)
- **哈苏橙**: #D4A574 (Hasselblad Orange)
- **深色背景**: #0F0F0F (Deep Space)

### 圆角系统
- 卡片: 16dp
- 按钮: 12dp
- 小元素: 8dp

### 间距系统
- 页面边距: 16dp
- 组件间距: 12dp
- 内边距: 8dp

## 开源协议

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 致谢

- OPPO 哈苏影像系统
- Jetpack Compose 团队
- ColorOS 设计团队

---

**热爱摄影的小陈工** 📸
