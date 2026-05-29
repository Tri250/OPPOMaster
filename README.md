# OPPO Master - 哈苏影像系统级参数库

**版本**: V1.5.0  
**技术栈**: React 18 + TypeScript + Tailwind CSS + Framer Motion  
**设计规范**: ColorOS 16 Aqua Design 水生设计语言  

---

## 📸 项目简介

OPPO Master 是一个为 OPPO 哈苏影像系统设计的专业调色参数库 Web 应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

### 核心功能

- 📸 **AI 场景识别** - 智能识别拍摄场景，自动优化参数
- 🎨 **原生相机参数** - 自动填入最佳相机参数设置
- 🪟 **悬浮窗** - 便捷悬浮窗，实时预览滤镜效果
- 🔍 **预设分类搜索** - 快速找到所需的滤镜预设
- ⚡ **预设生态** - 丰富的预设社区，分享与下载
- 📤 **多格式导入导出** - 支持多种格式的预设文件
- 💧 **水印生成器** - 专业水印制作工具
- 🎭 **预设编辑器** - 自定义滤镜参数编辑

---

## ✨ 技术亮点

- 🎯 **ColorOS 16 设计语言** - 遵循 Aqua Design 水生设计语言
- 🚀 **高性能** - 流畅动画与响应式交互（60fps）
- 🧩 **组件化设计** - 14+ 可复用组件
- 📱 **响应式适配** - 完美适配移动端和桌面端
- ♿ **无障碍支持** - 符合 WCAG 2.1 AAA 级标准
- 🧪 **全面测试** - 69 个测试用例 100% 通过
- 🔒 **TypeScript** - 类型安全，代码质量保证

---

## 🎨 设计规范

### 色彩系统

| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| Accent Primary | #FF6B35 | 主色、强调色 |
| Hasselblad Orange | #D4A574 | 哈苏品牌色 |
| Deep Space | #0F0F0F | 深色主题背景 |
| Card Surface | #1A1A1A | 卡片背景 |
| OPPO Green | #2DB47A | 成功状态 |

### 字体系统

| 层级 | 字号 | 字重 | 用途 |
|-----|------|------|------|
| 大标题 | 32sp | 700 | 首页大标题 |
| 页面标题 | 24sp | 600 | 二级页面标题 |
| 卡片标题 | 18sp | 500 | 卡片标题 |
| 正文 | 16sp | 400 | 正文描述 |
| 辅助文字 | 14sp | 400 | 辅助说明 |
| 小字提示 | 12sp | 300 | 标签、提示 |

### 圆角系统

| 元素 | 圆角值 | 用途 |
|-----|--------|------|
| 卡片 | 16dp | 功能卡片、内容卡片 |
| 按钮 | 12dp | 按钮组件 |
| 小元素 | 8dp | 图标、标签、输入框 |

### 间距系统

| 元素 | 间距值 | 用途 |
|-----|--------|------|
| 页面边距 | 16dp | 页面左右边距 |
| 组件间距 | 12dp | 组件之间间距 |
| 卡片间距 | 20dp | 首页卡片垂直间距 |
| 内边距 | 16dp | 卡片内边距 |

---

## 🚀 快速开始

### 环境要求

- Node.js 18+
- npm 9+ 或 yarn 1.22+

### 安装依赖

```bash
cd opmaster-web
npm install
```

### 开发模式

```bash
npm run dev
```

应用将在 `http://localhost:5173` 启动。

### 构建生产版本

```bash
npm run build
```

### 代码检查

```bash
npm run lint
```

---

## 📁 项目结构

```
OPPOMaster/
├── opmaster-web/                    # Web 端应用
│   ├── src/
│   │   ├── components/             # 组件目录
│   │   │   └── common/
│   │   │       ├── ColorOSComponents.tsx  # ColorOS 组件库
│   │   │       └── NavigationBar.tsx      # 导航栏组件
│   │   ├── pages/                  # 页面目录
│   │   │   ├── AppShowcase.tsx     # 应用展示
│   │   │   ├── AiFineTunePage.tsx  # AI 微调
│   │   │   ├── CloudSyncPage.tsx   # 云同步
│   │   │   ├── FilterLibraryPage.tsx    # 滤镜库
│   │   │   ├── FloatingWindowPage.tsx   # 悬浮窗
│   │   │   ├── LutManagerPage.tsx   # LUT 管理
│   │   │   ├── MasterParamsPage.tsx # 大师参数
│   │   │   ├── NativeCameraPage.tsx # 原生相机
│   │   │   ├── OcrDemoPage.tsx      # OCR 演示
│   │   │   ├── P0Overview.tsx      # P0 概览
│   │   │   ├── PresetEcosystemPage.tsx  # 预设生态
│   │   │   ├── SceneDetectionPage.tsx    # 场景检测
│   │   │   ├── SettingsPage.tsx     # 设置
│   │   │   └── XiaoOHelpPage.tsx   # 小O帮帮
│   │   ├── App.tsx                 # 应用主组件
│   │   ├── main.tsx               # 入口文件
│   │   └── index.css               # 全局样式
│   ├── tailwind.config.js          # Tailwind 配置
│   ├── vite.config.ts              # Vite 配置
│   └── package.json                # 项目依赖
├── 文档/                            # 项目文档
│   ├── AI_SCENE_DETECTION_COMPLETE_ACCEPTANCE_REPORT.md
│   ├── OPPO_MASTER_COMPLETE_TEST_REPORT.md
│   ├── OPPO_MASTER_MERGE_SUMMARY_REPORT.md
│   ├── OPPO_MASTER_P0-P1-P2_REDESIGN_SPEC.md
│   ├── OPPO_MASTER_WEB_UX_TEST_REPORT.md
│   └── COLOROS_16_EXPERT_REDESIGN_REPORT.md
└── README.md                       # 项目说明
```

---

## 🧩 组件库

### ColorOSComponents

项目提供了一套完整的 ColorOS 风格组件库，包含以下组件：

| 组件名称 | 说明 |
|---------|------|
| `ColorOSCard` | ColorOS 风格卡片组件 |
| `ColorOSButton` | 按钮组件（支持多种变体） |
| `ColorOSSwitch` | 开关组件 |
| `ColorOSSlider` | 滑块组件 |
| `ColorOSListItem` | 列表项组件 |
| `ColorOSSectionHeader` | 区域标题组件 |
| `ColorOSChip` | 标签组件 |
| `ColorOSProgressBar` | 进度条组件 |
| `ColorOSTabs` | 标签页组件 |
| `ColorOSRadioGroup` | 单选框组件 |
| `ColorOSDialog` | 对话框组件 |
| `ColorOSBottomSheet` | 底部抽屉组件 |
| `ColorOSFilterCard` | 滤镜卡片组件 |
| `ColorOSFloatingWindow` | 悬浮窗组件 |

所有组件都支持：
- ✅ 统一的视觉风格
- ✅ 流畅的动画效果
- ✅ 响应式设计
- ✅ 无障碍支持
- ✅ TypeScript 类型安全

---

## 📊 测试验收

### 测试覆盖

| 类别 | 用例数 | 通过率 |
|-----|-------|--------|
| 字体系统 | 8 | 100% |
| 色彩系统 | 6 | 100% |
| 间距圆角 | 8 | 100% |
| 首页专项 | 7 | 100% |
| 卡片组件 | 11 | 100% |
| 链接导航 | 8 | 100% |
| 动画过渡 | 9 | 100% |
| 响应式 | 8 | 100% |
| 可访问性 | 9 | 100% |
| **总计** | **69** | **100%** |

### 验收标准

- ✅ **P0 级用例**: 100% 通过（22/22）
- ✅ **P1 级用例**: 100% 通过（25/25）
- ✅ **P2 级用例**: 100% 通过（16/16）
- ✅ **P3 级用例**: 100% 通过（6/6）

---

## 🌐 响应式设计

应用支持多种屏幕尺寸：

| 设备类型 | 屏幕宽度 | 布局 |
|---------|---------|------|
| 小屏手机 | < 360px | 单列，超紧凑布局 |
| 手机 | 360px - 480px | 单列，紧凑布局 |
| 大屏手机 | 480px - 768px | 单列，标准布局 |
| 平板 | 768px - 1024px | 双列网格 |
| 桌面 | > 1024px | 多列网格 |

---

## ♿ 无障碍特性

应用符合 WCAG 2.1 AAA 级标准：

- ✅ 完整的键盘导航支持
- ✅ 清晰的焦点指示器
- ✅ 语义化 HTML 标签
- ✅ ARIA 属性支持
- ✅ 屏幕阅读器优化
- ✅ 文本对比度 ≥7:1
- ✅ 减少动画选项支持

---

## 🔧 技术栈

| 领域 | 技术 | 版本 |
|-----|------|------|
| 框架 | React | 18.3+ |
| 构建 | Vite | 5.4+ |
| 样式 | Tailwind CSS | 3.4+ |
| 动画 | Framer Motion | 11.0+ |
| 图标 | Lucide React | 0.460+ |
| 路由 | React Router | 6.28+ |
| 状态 | Zustand | 5.0+ |
| 语言 | TypeScript | 5.5+ |

---

## 📝 开发规范

### 命名规范

- **组件名**: PascalCase（如 `ColorOSCard`）
- **文件名**: PascalCase（如 `AppShowcase.tsx`）
- **CSS 类名**: kebab-case（如 `text-primary`）

### 代码风格

- 使用 ESLint 进行代码检查
- 遵循 React Hooks 规范
- 使用 TypeScript 严格模式
- 组件采用函数式写法

### Git 提交规范

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试相关
chore: 构建或辅助工具的变动
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 开源协议

MIT License

---

## 📞 联系方式

- **项目地址**: https://github.com/Tri250/OPPOMaster
- **问题反馈**: https://github.com/Tri250/OPPOMaster/issues

---

## 🙏 致谢

- OPPO 哈苏影像系统
- Tailwind CSS 团队
- Framer Motion 团队
- ColorOS 设计团队

---

**Made with ❤️ for ColorOS 16** 📸
