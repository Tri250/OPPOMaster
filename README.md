# OPPO Master - 哈苏影像系统级参数库

**版本**: v0.1.0-alpha  
**状态**: 开发中 - Web 展示可用，Android 端功能正在完善

---

## 📸 项目简介

OPPO Master 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，采用 ColorOS 16 设计语言。

### 功能状态

| 功能 | Web 端 | Android 端 | 状态 |
|-----|--------|-----------|------|
| 📸 预设浏览展示 | ✅ 可用 | ✅ 基础实现 | 可用 |
| 🎨 ColorOS 风格组件库 | ✅ 完整 | ✅ 完整 | 可用 |
| 🪟 悬浮窗 | 📝 展示页 | 🔧 开发中 | 部分实现 |
| 🎭 预设编辑器 | 📝 展示页 | 🔧 开发中 | 部分实现 |
| 📸 AI 场景识别 | 📝 演示页 | 📋 规划中 | 演示阶段 |
| 💧 水印生成器 | 📝 展示页 | 🔧 开发中 | 部分实现 |
| 📤 云同步 | 📝 展示页 | 📋 规划中 | 演示阶段 |
| 📱 原生相机参数填入 | ❌ 不适用 | 📋 规划中 | 规划中 |

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

---

## 🚀 快速开始

### Web 端

#### 环境要求
- Node.js 18+
- npm 9+ 或 yarn 1.22+

#### 安装依赖
```bash
cd opmaster-web
npm install
```

#### 开发模式
```bash
npm run dev
```

应用将在 `http://localhost:5173` 启动。

#### 构建生产版本
```bash
npm run build
```

### Android 端

#### 环境要求
- JDK 17+
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK API 34

#### 编译项目
```bash
./gradlew assembleDebug
```

---

## 📁 项目结构

```
OPPOMaster/
├── app/                            # Android 端应用
│   ├── src/main/
│   │   ├── java/com/omaster/app/
│   │   │   ├── ui/               # UI 层（Jetpack Compose）
│   │   │   │   ├── screens/      # 页面
│   │   │   │   ├── components/   # 组件
│   │   │   │   └── theme/        # 主题
│   │   │   ├── model/            # 数据模型
│   │   │   ├── service/          # 服务层
│   │   │   └── MainActivity.kt   # 主 Activity
│   │   └── res/                  # 资源文件
│   └── build.gradle.kts
├── opmaster-web/                  # Web 端应用
│   ├── src/
│   │   ├── components/            # 组件目录
│   │   ├── pages/                # 页面目录
│   │   ├── App.tsx               # 应用主组件
│   │   └── main.tsx             # 入口文件
│   └── package.json
├── docs/                          # 项目文档
│   └── reports/                  # 历史报告文件
└── README.md                      # 项目说明
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
| 更多组件... | 详见代码 |

---

## 🔧 技术栈

### Web 端

| 领域 | 技术 | 版本 |
|-----|------|------|
| 框架 | React | 19.2+ |
| 构建 | Vite | 8.0+ |
| 样式 | Tailwind CSS | 3.4+ |
| 动画 | Framer Motion | 12.40+ |
| 图标 | Lucide React | 1.16+ |
| 路由 | React Router | 7.15+ |
| 状态 | Zustand | 5.0+ |
| 语言 | TypeScript | 5.5+ |

### Android 端

| 领域 | 技术 |
|-----|------|
| UI | Jetpack Compose |
| DI | Hilt |
| 语言 | Kotlin |
| 异步 | Coroutines + Flow |

---

## 📝 开发规范

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
