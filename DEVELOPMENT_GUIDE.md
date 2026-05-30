# OPPOMaster 开发指南

> **版本**: v2.0.0  
> **分支**: `trae/solo-agent-g4xAg3`  
> **最后更新**: 2026年5月30日

---

## 📖 目录

1. [项目概述](#1-项目概述)
2. [快速开始](#2-快速开始)
3. [开发环境](#3-开发环境)
4. [代码规范](#4-代码规范)
5. [功能开发](#5-功能开发)
6. [测试指南](#6-测试指南)
7. [发布流程](#7-发布流程)

---

## 1. 项目概述

### 1.1 项目简介

OPPOMaster（小O帮帮）是一款专业的影像参数管理应用，集成了DeepSeek AI智能场景识别技术，为用户提供哈苏大师级影像参数推荐。

### 1.2 技术栈

#### Android端
- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + Hilt
- **最低版本**: Android 16 (API 36)

#### Web端
- **框架**: React 19
- **语言**: TypeScript
- **构建**: Vite 8.0.14
- **样式**: Tailwind CSS

---

## 2. 快速开始

### 2.1 克隆项目

```bash
# 克隆仓库
git clone https://github.com/Tri250/OPPOMaster.git

# 切换到开发分支
cd OPPOMaster
git checkout trae/solo-agent-g4xAg3
```

### 2.2 Android端启动

```bash
# 使用Gradle构建
./gradlew assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2.3 Web端启动

```bash
# 进入Web目录
cd opmaster-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173/
```

---

## 3. 开发环境

### 3.1 环境要求

#### Android
- Java 17+
- Android Studio Ladybug+
- Gradle 8.0+
- Android SDK 36

#### Web
- Node.js 18+
- npm 9+
- VS Code（推荐）

### 3.2 环境变量

#### Android
```properties
# local.properties
sdk.dir=/path/to/android/sdk
```

#### Web
```bash
# .env
VITE_DEEPSEEK_API_KEY=your_api_key
```

### 3.3 依赖安装

```bash
# Android
./gradlew dependencies

# Web
cd opmaster-web
npm install
```

---

## 4. 代码规范

### 4.1 Kotlin规范

#### 命名规范
- 类名：`UpperCamelCase`（如 `HomeScreen`）
- 函数名：`lowerCamelCase`（如 `onPresetClick`）
- 常量：`SCREAMING_SNAKE_CASE`（如 `MAX_RETRY_COUNT`）

#### 架构规范
- 使用MVVM架构
- 依赖注入使用Hilt
- UI状态使用StateFlow
- 协程处理异步操作

#### 示例
```kotlin
@Composable
fun HomeScreen(
    onPresetClick: (Preset) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    // ...
}
```

### 4.2 TypeScript规范

#### 命名规范
- 组件：`PascalCase`（如 `HomePage`）
- 函数：`camelCase`（如 `handleClick`）
- 常量：`UPPER_SNAKE_CASE`（如 `MAX_ITEMS`）

#### 组件规范
- 使用函数组件
- 使用Hooks管理状态
- Props使用TypeScript类型

#### 示例
```typescript
interface PresetCardProps {
  preset: Preset;
  index: number;
}

export default function PresetCard({ preset, index }: PresetCardProps) {
  // ...
}
```

### 4.3 Git提交规范

#### 提交格式
```
<type>: <subject>

<body>
```

#### Type类型
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

#### 示例
```bash
git commit -m "feat: 添加新的预设分类功能"
git commit -m "fix: 修复AI识别准确率问题"
git commit -m "docs: 更新README文档"
```

---

## 5. 功能开发

### 5.1 开发流程

#### 创建功能分支
```bash
# 从开发分支创建
git checkout -b feature/new-feature-name

# 或从问题修复分支
git checkout -b fix/bug-description
```

#### 开发步骤
1. 编写代码
2. 本地测试
3. 提交代码
4. 推送到远程
5. 创建Pull Request

#### 合并到开发分支
```bash
# 切换到开发分支
git checkout trae/solo-agent-g4xAg3

# 合并功能分支
git merge feature/new-feature-name

# 推送更新
git push origin trae/solo-agent-g4xAg3

# 删除功能分支
git branch -d feature/new-feature-name
```

### 5.2 Android开发指南

#### 添加新屏幕
1. 在 `ui/screens/` 创建新文件
2. 定义Screen组件
3. 在 `navigation/Screen.kt` 添加路由
4. 在 `MainActivity.kt` 添加导航

#### 示例
```kotlin
// ui/screens/NewScreen.kt
@Composable
fun NewScreen(
    onBack: () -> Unit
) {
    // 实现屏幕
}

// navigation/Screen.kt
data object NewScreen : Screen(
    route = "new_screen",
    title = "新功能"
)

// MainActivity.kt
composable(Screen.NewScreen.route) {
    NewScreen(onBack = { navController.popBackStack() })
}
```

### 5.3 Web开发指南

#### 添加新页面
1. 在 `pages/` 创建新文件
2. 定义页面组件
3. 在 `App.tsx` 添加路由

#### 示例
```typescript
// pages/NewPage.tsx
export default function NewPage() {
  return <div>新页面</div>;
}

// App.tsx
import NewPage from './pages/NewPage';

<Route path="/new-page" element={<NewPage />} />
```

### 5.4 API集成

#### Android DeepSeek API
```kotlin
// service/DeepSeekService.kt
class DeepSeekService {
    suspend fun detectScene(bitmap: Bitmap): SceneResult {
        // 实现API调用
    }
}
```

#### Web DeepSeek API
```typescript
// services/deepseek.ts
export async function detectScene(imageFile: File) {
  // 实现API调用
}
```

---

## 6. 测试指南

### 6.1 Android测试

#### 单元测试
```bash
./gradlew test
```

#### UI测试
```bash
./gradlew connectedAndroidTest
```

#### 构建验证
```bash
./gradlew assembleDebug
./gradlew lint
```

### 6.2 Web测试

#### 开发模式
```bash
npm run dev
# 访问 http://localhost:5173/
```

#### 构建测试
```bash
npm run build
npm run preview
```

#### 代码检查
```bash
npm run lint
npm run type-check
```

### 6.3 AI功能测试

#### 测试场景识别
1. 准备测试图片（人像、风景、夜景等）
2. 上传到AI识别功能
3. 验证识别结果
4. 检查推荐预设准确性

#### 边界场景测试
- 全黑图片
- 全白图片
- 模糊图片
- 快速切换场景

---

## 7. 发布流程

### 7.1 版本号规范

采用语义化版本（SemVer）：
```
主版本.次版本.修订号
v2.0.0
  │  │  └─ 修订号：日常bug修复
  │  └─ 次版本：新功能（向后兼容）
  └─ 主版本：重大变更（不向后兼容）
```

### 7.2 发布步骤

#### 1. 准备发布
```bash
# 确保在开发分支
git checkout trae/solo-agent-g4xAg3

# 更新版本号
# Android: 修改 build.gradle.kts
# Web: 修改 package.json
```

#### 2. 创建发布分支
```bash
git checkout -b release/v2.1.0
```

#### 3. 测试验证
```bash
# Android
./gradlew assembleRelease
./gradlew test

# Web
npm run build
npm run type-check
```

#### 4. 创建标签
```bash
git tag -a v2.1.0 -m "版本 v2.1.0 发布"
git push origin v2.1.0
```

#### 5. 合并到main
```bash
git checkout main
git merge release/v2.1.0
git push origin main
```

#### 6. 清理
```bash
git checkout trae/solo-agent-g4xAg3
git merge main
git branch -d release/v2.1.0
git push origin trae/solo-agent-g4xAg3
```

---

## 📞 获取帮助

### 文档资源
- [VERSION_LOCK.md](VERSION_LOCK.md) - 版本锁定信息
- [RELEASE_NOTES.md](RELEASE_NOTES.md) - 发布说明
- [EXPERT_VERIFICATION_REPORT.md](EXPERT_VERIFICATION_REPORT.md) - 验证报告

### 问题反馈
- 提交GitHub Issue
- 查看现有Issue
- 参与讨论

---

## ✅ 开发检查清单

### 开始开发前
- [ ] 克隆最新代码
- [ ] 安装所有依赖
- [ ] 运行开发服务器
- [ ] 验证基本功能

### 开发过程中
- [ ] 遵循代码规范
- [ ] 编写清晰注释
- [ ] 定期提交代码
- [ ] 测试新功能

### 提交代码前
- [ ] 运行所有测试
- [ ] 检查代码规范
- [ ] 更新相关文档
- [ ] 创建清晰的提交信息

---

**开发愉快！** 🚀

---

> 📌 **提示**  
> 遇到问题？请查看 [EXPERT_VERIFICATION_REPORT.md](EXPERT_VERIFICATION_REPORT.md) 中的常见问题解答。  
> 或提交Issue获取帮助。

**最后更新**: 2026年5月30日  
**维护团队**: OPPOMaster 开发团队
