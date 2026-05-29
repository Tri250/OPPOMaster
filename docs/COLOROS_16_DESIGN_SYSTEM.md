# ColorOS 16 设计系统 - 专家级设计文档

## 🎨 设计理念

ColorOS 16 设计系统采用「灵动 · 自由 · 生态化设计理念，结合OPPO品牌色系统，打造专业摄影体验。

---

## 🎯 核心设计原则

### 1. 品牌一致性
- **OPPO品牌色系统：从-OPPO橙为主色，体现年轻、温暖、活力
- **哈苏专业色系统：哈苏金色为专业摄影色彩
- **视觉一致性：从Android端、Web端统一

### 2. 简洁高效
- **内容为先：让内容成为视觉焦点，减少视觉噪音
- **层次清晰：通过层级关系明确
- **操作直观：操作路径简短明了

### 3. 细腻精致
- **动效流畅：采用ColorOS 16标准动效曲线
- **反馈及时：给予用户即时、及时的反馈

---

## 🎨 色彩系统

### OPPO品牌色彩系统

| 色彩名称 | 色值 | 使用场景 |
|---------|------|---------|
| OPPO橙 | #FF6B35 | 主要按钮、强调、标签 |
| OPPO橙暗 | #E55A2B | 悬停/按压 |
| OPPO绿 | #2DB47A | 成功、状态、环保 |
| OPPO蓝 | #3B82F6 | 信息、链接 |
| OPPO紫 | #8B5CF6 | 高级、渐变 |
| OPPO粉 | #EC4899 | 警告、温馨 |

### 哈苏专业色彩

| 色彩名称 | 色值 | 使用场景 |
|---------|------|---------|
| 哈苏橙 | #D4A574 | 哈苏认证、专业标签 |
| 哈苏金 | #E5C07B | 渐变强调 |

### 中性灰度系统

| 层级 | 色值 | 用途 |
|-----|------|------|
| 50 | #F9F9F9 | 亮背景 |
| 100 | #F0F0F0 | 卡片背景 |
| 200 | #E0E0E0 | 分割线 |
| 300 | #C4C4C4 | 禁用 |
| 400 | #9E9E9E | 次要文字 |
| 500 | #757575 | 禁用文字 |
| 600 | #525252 | 禁用状态 |
| 700 | #303030 | 卡片背景 |
| 800 | #1A1A1A | 次要背景 |
| 900 | #0F0F0F | 主背景 |

### 功能色彩系统

| 色彩 | 色值 | 用途 |
|-----|------|-----|
| 成功 | #2DB47A | 成功、完成 |
| 警告 | #F59E0B | 提示、警告 |
| 错误 | #EF4444 | 错误、危险 |
| 信息 | #3B82F6 | 信息提示 |

### 深色模式

| 层级 | 用途 |
|-----|------|
| bg-primary | #0F0F0F | 主背景 |
| bg-secondary | #1A1A1A | 次背景 |
| bg-tertiary | #1F1F1F | 三级背景 |
| bg-elevated | #1C1C1E | 卡片背景 |
| bg-glass | rgba(15, 15, 15, 0.72) | 毛玻璃效果 |

---

## 📝 文字系统

### 字体层级系统

| 层级 | 字号 | 行高 | 字重 | 使用场景 |
|-----|-----|-----|-----|
| display | 2.25rem | 2.75rem | 700 | 超大标题、Hero区域 |
| h1 | 1.75rem | 2.25rem | 700 | 页面标题 |
| h2 | 1.5rem | 2rem | 700 | 大标题 |
| h3 | 1.25rem | 1.75rem | 600 | 卡片标题、模块标题 |
| body1 | 1rem | 1.5rem | 400 | 正文、主要内容 |
| body2 | 0.875rem | 1.25rem | 400 | 辅助文字、描述 |
| caption | 0.75rem | 1rem | 400 | 标签、小字 |
| micro | 0.6875rem | 0.9375rem | 400 | 极小文字 |
| number | 1.25rem | 1.75rem | 600 | 数值显示 |
| number-lg | 1.5rem | 2rem | 700 | 大数值显示 |

### 字重系统

| 字重 | 数值 | 用途 |
|-----|-----|
| light | 300 | 细体 |
| normal | 400 | 常规 |
| medium | 500 | 中等 |
| semibold | 600 | 半粗 |
| bold | 700 | 粗体 |

---

## 📏 间距系统

### 8dp网格系统

| 尺寸 | 像素 | 间距名 | 用途 |
|-----|------|-----|
| xs | 4px | xs | 小间距、紧凑 |
| sm | 8px | 2 | 小内边距、紧凑 |
| md | 12px | 3 | 标准间距 |
| lg | 16px | 4 | 常规内边距 |
| xl | 20px | 5 | 较大间距 |
| 2xl | 24px | 6 | 大间距、章节 |
| 3xl | 32px | 8 | 超大间距 |
| 4xl | 40px | 10 | 特殊间距 |

---

## 🔲 圆角系统

| 圆角 | 像素 | 用途 |
|-----|------|-----|
| xs | 8px | 小按钮、小卡片 |
| sm | 12px | 标准按钮、小卡片 |
| md | 16px | 标准卡片、中等按钮 |
| lg | 20px | 大卡片、大按钮 |
| xl | 24px | 超大卡片、容器 |
| 2xl | 28px | 特殊卡片、展示 |
| pill | 9999px | 胶囊按钮、标签 |

---

## ✨ 阴影系统

### OPPO Elevation System

| 层级 | 阴影参数 | 用途 |
|-----|---------|-----|
| elevation-1 | 0 2px 8px rgba(0, 0, 0, 0.08) | 小卡片、标签 |
| elevation-2 | 0 4px 16px rgba(0, 0, 0, 0.12) | 标准卡片、按钮悬停 |
| elevation-3 | 0 8px 24px rgba(0, 0, 0, 0.16) | 大卡片、悬浮元素 |
| elevation-4 | 0 12px 32px rgba(0, 0, 0, 0.20) | 弹窗、重要元素 |

### 发光阴影

| 名称 | 参数 | 用途 |
|-----|------|
| glow-orange | 0 0 30px rgba(255, 107, 53, 0.3) | OPPO橙发光效果 |
| glow-green | 0 0 30px rgba(45, 180, 122, 0.3) | OPPO绿发光效果 |

---

## 🎬 动效系统

### 缓动曲线系统

```typescript
// ColorOS 16 标准缓动曲线

oppoEnter: [0.05, 0.7, 0.1, 1.0]  // 进入
oppoExit: [0.3, 0.0, 0.8, 0.15]     // 退出
oppoBounce: [0.175, 0.885, 0.32, 1.275]  // 弹性
easeOutElastic: [0.18, 0.89, 0.32, 1.28]  // 弹性弹性
```

### 动画时长

| 类型 | 时长 | 用途 |
|-----|------|-----|
| fast | 120ms | 状态切换、快速反馈 |
| normal | 200ms | 标准动画、基础交互 |
| slow | 300ms | 页面切换、动效强调 |
| slower | 400ms | 过渡动画、复杂动效 |
| slowest | 600ms | 特殊动画、缓动过渡 |

### 预设动画

| 动画名 | 效果 | 参数 |
|-------|-----|
| float | 漂浮 | 0 0 20px 无限 |
| breathing | 呼吸 | scale 1-1.03, opacity 0.75-1 |
| pulse-glow | 脉冲发光 | shadow 发光 |
| fade-in | 淡入 | opacity 0-1 |
| fade-in-up | 上滑淡入 | translateY 30px-0 |
| scale-in | 缩放淡入 | scale 0.95-1 |
| slide-in-right | 右滑淡入 | translateX 40px-0 |
| slide-in-left | 左滑淡入 | translateX -40px-0 |
| modal-in | 弹窗进入 | scale 0.92-1, translateY 100%-0 |

---

## 📦 组件系统

### 按钮组件

#### 主按钮 (Primary Button)
```tsx
// Primary Button
<Button variant="primary" size="lg">
  主要操作
</Button>
```

- **样式：背景渐变-OPPO橙-哈苏金
- **交互：悬停-发光效果，悬停-上移
- **点击-缩放
#### 次要按钮 (Secondary Button)
```tsx
<Button variant="secondary">
  次要操作
</Button>
```

#### 边框按钮 (Outline Button)
```tsx
<Button variant="outline">
  边框按钮
</Button>
```

#### 图标按钮 (Icon Button)
```tsx
<IconButton>
  <Heart />
</IconButton>
```

### 卡片组件

#### 标准卡片 (Standard Card)
```tsx
<Card variant="default">
  卡片内容
</Card>
```

#### 互动卡片 (Interactive Card)
```tsx
<Card variant="interactive">
  卡片内容
</Card>
```

#### 玻璃卡片 (Glass Card)
```tsx
<Card variant="glass">
  卡片内容
</Card>
```

#### 哈苏认证卡片 (HNCS Card)
```tsx
<Card variant="hncs">
  卡片内容
</Card>
```

### 标签组件

#### 标准标签
```tsx
<Tag variant="default">标签</Tag>
```

#### 哈苏认证标签
```tsx
<Tag variant="hncs">哈苏认证</Tag>
```

#### 精选标签
```tsx
<Tag variant="featured">精选</Tag>
```

---

## 🎨 毛玻璃效果系统

### 玻璃效果层级

| 效果名 | 透明度 | 模糊 | 边框 |
|-------|--------|------|
| glass-effect | rgba(15, 15, 15, 0.72) | 2xl | 1px white/8 |
| glass-effect-light | rgba(255, 255, 255, 0.05) | xl | 1px white/6 |
| glass-navigation | rgba(15, 15, 15, 0.85) | 2xl | 1px white/5 |

---

## 🌅 背景光效系统

### 光球效果

| 光球 | 渐变 | 尺寸 | 模糊 | 位置 |
|-----|------|-----|
| orb-orange | linear-gradient(135deg, #FF6B35, #D4A574) | 500px | 100px | -top-left |
| orb-blue | linear-gradient(135deg, #3B82F6, #60A5FA) | 450px | 100px | -bottom-right |
| orb-green | linear-gradient(135deg, #2DB47A, #86EFAC) | 400px | 100px | -middle |

---

## 📱 导航系统

### 顶部导航

- **高度：56dp
- **样式：玻璃效果
- **导航项间距：8dp
- **图标尺寸：24px

### 底部导航

- **高度：72dp
- **样式：玻璃效果
- **安全区域：考虑系统安全区域
- **图标尺寸：24px
- **标签：12px
- **选中：渐变-填充
- **未选中：灰色-文字

---

## 🎯 交互设计原则

### 1. 点击反馈
- **微缩放-0.98
- **颜色：0.96
- **动画-弹性

### 2. 悬停反馈
- **阴影增强
- **缩放-
- **0.5-1px

### 3. 状态切换
- **动画：200-300ms
- **缓动：oppoEnter/oppoExit

---

## 📐 栅格系统

### 断点系统

| 断点 | 设备 | 列数 | 间距 |
|-----|------|------|
| sm | 360px | 4 | 16px |
| md | 640px | 8 | 24px |
| lg | 1024px | 12 | 24px |
| xl | 1280px | 12 | 32px |

---

## 🎨 品牌元素

### OPPO品牌文字渐变

```css
gradient-text-oppo {
  background: linear-gradient(135deg, #FF6B35, #D4A574);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
```

### 哈苏认证标识

- **星形：填充哈苏橙
- **文字：哈苏橙
- **背景：半透明-玻璃效果

---

## ✅ 设计质量检查清单

### 用户体验 (UX)

- [x] 清晰的视觉层级
- [x] 直观的操作流程
- [x] 及时的反馈机制
- [x] 响应式适配
- [x] 可访问性支持
- [x] 系统安全区域
- [x] 清晰的错误处理

### 视觉设计 (UI)

- [x] 品牌一致性
- [x] 色彩系统统一
- [x] 文字层级清晰
- [x] 间距系统对齐
- [x] 圆角系统统一
- [x] 阴影层次分明
- [x] 动效流畅自然
- [x] 图标风格一致
- [x] 卡片风格统一
- [x] 按钮系统完整

### 代码质量

- [x] 组件复用
- [x] 命名规范
- [x] 性能优化
- [x] 响应式适配
- [x] 可访问性
- [x] 无障碍支持
- [x] 动画性能

---

## 📚 参考文档

### 相关文件结构

```
src/
├── components/
│   ├── common/
│   │   ├── ColorOSComponents.tsx
│   │   ├── NavigationBar.tsx
│   │   └── SearchHistory.tsx
│   └── home/
│       └── PresetCard.tsx
├── pages/
│   ├── HomePage.tsx
│   ├── CommunityPage.tsx
│   ├── SubscriptionPage.tsx
│   ├── CloudSyncPage.tsx
│   └── FloatingWindowPage.tsx
├── styles/
│   └── index.css
└── tailwind.config.js
```

### 设计资源

- **Figma文件：(待补充)
- **设计规范：本文档
- **组件库：ColorOS 16 Components
- **图标库：Lucide React (Customized)

---

## 🏷️ 版本历史

| 版本 | 日期 | 变更 |
|-----|------|-----|
| v1.0 | 2026-05-29 | 初版，完整实现 |

---

## 📞 设计支持

如有设计问题或需求，请联系设计团队。

---

*本设计系统文档遵循OPPO品牌设计规范，所有设计决策均基于ColorOS 16金标系统标准。
