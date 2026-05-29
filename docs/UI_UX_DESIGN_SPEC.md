# 小O帮帮 UI/UX设计规范
**设计版本**：v2.0.0  
**更新日期**：2026年5月29日  
**设计风格**：OPPO简约科技风  
**响应式断点**：Mobile < 768px | Tablet 768px-1024px | Desktop > 1024px

---

## 一、设计理念

### 1.1 品牌定位
- **核心价值**：智能、简约、专业
- **品牌色彩**：OPPO绿 + 科技蓝 + 中性灰
- **设计关键词**：轻量化、一致性、可触达

### 1.2 设计原则
1. **一致性**：全平台视觉语言统一
2. **可用性**：符合用户直觉操作
3. **性能**：关注首屏加载与交互响应
4. **包容性**：考虑不同设备与用户群体

---

## 二、色彩系统

### 2.1 品牌色

```css
:root {
    /* OPPO品牌主色 */
    --color-primary: #00A06D;        /* OPPO绿 - 主色 */
    --color-primary-dark: #007A52;    /* 深绿 - 强调 */
    --color-primary-light: #4DBF95;   /* 浅绿 - 次级 */
    
    /* 科技蓝 */
    --color-secondary: #0066FF;       /* 科技蓝 - 次色 */
    --color-secondary-dark: #0052CC;  /* 深蓝 - 强调 */
    --color-secondary-light: #3385FF; /* 浅蓝 - 次级 */
    
    /* 影像系统色彩 */
    --color-hasselblad: #8B6914;      /* 哈苏金 */
    --color-leica: #C0C0C0;          /* 徕卡银 */
    --color-zeiss: #1A1A1A;          /* 蔡司黑 */
    --color-xmage: #FF6B35;           /* XMAGE橙 */
}
```

### 2.2 功能色

```css
:root {
    /* 功能色 */
    --color-success: #00A06D;         /* 成功 - 绿色 */
    --color-warning: #FF9500;        /* 警告 - 橙色 */
    --color-error: #FF3B30;          /* 错误 - 红色 */
    --color-info: #007AFF;           /* 信息 - 蓝色 */
    
    /* 状态色 */
    --color-ai-detecting: #5856D6;   /* AI识别中 - 紫色 */
    --color-ai-success: #00A06D;      /* AI成功 - 绿色 */
    --color-ai-failed: #FF3B30;       /* AI失败 - 红色 */
}
```

### 2.3 中性色

```css
:root {
    /* 文字色 */
    --color-text-primary: #1A1A1A;    /* 主文字 */
    --color-text-secondary: #666666;  /* 次级文字 */
    --color-text-tertiary: #999999;  /* 辅助文字 */
    --color-text-inverse: #FFFFFF;   /* 反色文字 */
    
    /* 背景色 */
    --color-bg-primary: #FFFFFF;      /* 主背景 */
    --color-bg-secondary: #F5F5F7;    /* 次级背景 */
    --color-bg-tertiary: #F0F0F0;    /* 辅助背景 */
    --color-bg-card: #FFFFFF;        /* 卡片背景 */
    --color-bg-overlay: rgba(0,0,0,0.5); /* 遮罩背景 */
    
    /* 边框色 */
    --color-border: #E5E5E5;        /* 默认边框 */
    --color-border-hover: #CCCCCC;   /* 悬停边框 */
    --color-border-active: #00A06D;   /* 激活边框 */
}
```

---

## 三、字体系统

### 3.1 字体家族

```css
:root {
    /* 手机端 - ColorOS字体 */
    --font-family-mobile: 
        -apple-system, 
        "ColorOS", 
        "Helvetica Neue", 
        "PingFang SC", 
        "Hiragino Sans GB", 
        "Microsoft YaHei", 
        sans-serif;
    
    /* Web端 - 系统字体 */
    --font-family-web: 
        -apple-system, 
        BlinkMacSystemFont, 
        "Segoe UI", 
        "PingFang SC", 
        "Hiragino Sans GB", 
        "Microsoft YaHei", 
        sans-serif;
}
```

### 3.2 字体层级

| 层级 | 字号(手机) | 字号(Web) | 字重 | 行高 | 使用场景 |
|------|-------------|-----------|------|------|----------|
| H1 | 28px | 32px | 700 | 1.3 | 页面标题 |
| H2 | 24px | 28px | 600 | 1.3 | 区块标题 |
| H3 | 20px | 24px | 600 | 1.4 | 卡片标题 |
| H4 | 18px | 20px | 500 | 1.4 | 副标题 |
| Body | 16px | 16px | 400 | 1.5 | 正文内容 |
| Caption | 14px | 14px | 400 | 1.5 | 辅助说明 |
| Small | 12px | 12px | 400 | 1.5 | 标签/徽章 |

### 3.3 字体使用规范

```css
/* 标题字体 */
h1, h2, h3, h4 {
    font-family: var(--font-family);
    font-weight: 600;
    color: var(--color-text-primary);
}

/* 正文字体 */
body, p {
    font-family: var(--font-family);
    font-size: 16px;
    line-height: 1.5;
    color: var(--color-text-primary);
}

/* 辅助字体 */
.caption, .hint {
    font-size: 14px;
    color: var(--color-text-secondary);
}
```

---

## 四、间距系统

### 4.1 基础间距单位

```css
:root {
    --spacing-unit: 4px;      /* 基础单位 */
    --spacing-xs: 4px;        /* 4px */
    --spacing-sm: 8px;        /* 8px */
    --spacing-md: 16px;       /* 16px */
    --spacing-lg: 24px;       /* 24px */
    --spacing-xl: 32px;       /* 32px */
    --spacing-xxl: 48px;     /* 48px */
}
```

### 4.2 组件间距规范

| 组件类型 | 组件内间距 | 组件间间距 |
|----------|------------|------------|
| 卡片 | 16px | 12px |
| 按钮组 | 12px | 8px |
| 表单项 | 16px | 16px |
| 列表项 | 12px | 0px |
| 区块 | 24px | 24px |

---

## 五、组件设计

### 5.1 按钮组件

```css
/* 主按钮 */
.btn-primary {
    background: var(--color-primary);
    color: var(--color-text-inverse);
    border-radius: 12px;
    padding: 12px 24px;
    font-size: 16px;
    font-weight: 500;
    transition: all 0.2s ease;
}

.btn-primary:hover {
    background: var(--color-primary-dark);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 160, 109, 0.3);
}

.btn-primary:active {
    transform: translateY(0);
    box-shadow: none;
}

/* 次级按钮 */
.btn-secondary {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
    border: 1px solid var(--color-border);
}

/* 文字按钮 */
.btn-text {
    background: transparent;
    color: var(--color-primary);
    padding: 8px 16px;
}
```

**按钮尺寸**：

| 尺寸 | 高度 | 内边距 | 字号 | 圆角 |
|------|------|--------|------|------|
| 大 | 48px | 16px 24px | 16px | 12px |
| 中 | 40px | 12px 20px | 14px | 10px |
| 小 | 32px | 8px 16px | 12px | 8px |

### 5.2 卡片组件

```css
/* 基础卡片 */
.card {
    background: var(--color-bg-card);
    border-radius: 16px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;
}

.card:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
}

/* 场景卡片 */
.scene-card {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 12px;
    overflow: hidden;
    position: relative;
}

.scene-card__overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px;
    background: linear-gradient(transparent, rgba(0,0,0,0.7));
    color: white;
}

/* 设备卡片 */
.device-card {
    display: flex;
    align-items: center;
    padding: 16px;
    background: var(--color-bg-card);
    border-radius: 12px;
    border: 1px solid var(--color-border);
}

.device-card__icon {
    width: 48px;
    height: 48px;
    margin-right: 12px;
    border-radius: 8px;
    background: var(--color-bg-secondary);
}
```

### 5.3 输入组件

```css
/* 文本输入框 */
.input {
    width: 100%;
    height: 48px;
    padding: 0 16px;
    border: 1px solid var(--color-border);
    border-radius: 12px;
    font-size: 16px;
    background: var(--color-bg-primary);
    transition: all 0.2s ease;
}

.input:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(0, 160, 109, 0.1);
    outline: none;
}

.input--error {
    border-color: var(--color-error);
}

/* 搜索框 */
.search-input {
    position: relative;
}

.search-input__icon {
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    color: var(--color-text-tertiary);
}

.search-input input {
    padding-left: 48px;
}
```

### 5.4 标签组件

```css
/* 场景标签 */
.tag {
    display: inline-flex;
    align-items: center;
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 12px;
    font-weight: 500;
    background: var(--color-bg-secondary);
    color: var(--color-text-secondary);
}

.tag--primary {
    background: var(--color-primary);
    color: white;
}

.tag--hasselblad {
    background: var(--color-hasselblad);
    color: white;
}

.tag--leica {
    background: var(--color-leica);
    color: #333;
}

.tag--zeiss {
    background: var(--color-zeiss);
    color: white;
}

.tag--xmage {
    background: var(--color-xmage);
    color: white;
}
```

---

## 六、页面布局

### 6.1 手机端布局

```
┌─────────────────────────────┐
│        状态栏 44px          │
├─────────────────────────────┤
│      顶部导航栏 56px         │
│  [←] 标题      [设置] [分享] │
├─────────────────────────────┤
│                             │
│                             │
│       主内容区域              │
│       (可滚动)               │
│                             │
│                             │
│                             │
├─────────────────────────────┤
│       底部标签栏 56px         │
│  [首页] [场景] [设备] [我的]   │
└─────────────────────────────┘
```

**布局规范**：
- 安全区域：20px（刘海屏/水滴屏）
- 顶部导航栏：56px
- 底部标签栏：56px + 安全区域
- 内容边距：16px

### 6.2 Web端布局

```
┌─────────────────────────────────────────────────────────┐
│                      顶部导航栏 64px                     │
│  [Logo]    [首页] [场景库] [设备库] [关于]      [登录]   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐  ┌─────────────────────────────────┐   │
│  │   侧边栏    │  │                                 │   │
│  │   240px    │  │         主内容区域               │   │
│  │            │  │         (flex: 1)               │   │
│  │  分类导航   │  │                                 │   │
│  │            │  │                                 │   │
│  │  筛选器    │  │                                 │   │
│  │            │  │                                 │   │
│  └─────────────┘  └─────────────────────────────────┘   │
│                                                         │
├─────────────────────────────────────────────────────────┤
│                      底部 80px                           │
│              © 2026 小O帮帮 | 服务条款 | 隐私政策        │
└─────────────────────────────────────────────────────────┘
```

**布局规范**：
- 最大宽度：1280px（居中）
- 侧边栏宽度：240px
- 主内容内边距：32px
- 网格间距：24px

### 6.3 响应式断点

```css
/* 手机端 < 768px */
@media (max-width: 767px) {
    .container {
        padding: 0 16px;
    }
    
    .grid {
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
    }
}

/* 平板端 768px - 1024px */
@media (min-width: 768px) and (max-width: 1024px) {
    .container {
        padding: 0 24px;
    }
    
    .grid {
        grid-template-columns: repeat(3, 1fr);
        gap: 16px;
    }
}

/* 桌面端 > 1024px */
@media (min-width: 1025px) {
    .container {
        padding: 0 32px;
    }
    
    .grid {
        grid-template-columns: repeat(4, 1fr);
        gap: 24px;
    }
}
```

---

## 七、动画规范

### 7.1 过渡动画

```css
:root {
    /* 动画时长 */
    --duration-fast: 150ms;     /* 快：150ms */
    --duration-normal: 250ms;   /* 正常：250ms */
    --duration-slow: 400ms;     /* 慢：400ms */
    
    /* 缓动函数 */
    --ease-default: cubic-bezier(0.4, 0, 0.2, 1);     /* 默认 */
    --ease-in: cubic-bezier(0.4, 0, 1, 1);            /* 进入 */
    --ease-out: cubic-bezier(0, 0, 0.2, 1);           /* 退出 */
    --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55); /* 弹性 */
}

/* 通用过渡 */
.transition {
    transition: all var(--duration-normal) var(--ease-default);
}

/* 按钮悬停 */
.btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 卡片悬停 */
.card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
```

### 7.2 页面动画

```css
/* 页面进入 */
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.page-enter {
    animation: fadeIn var(--duration-slow) var(--ease-out);
}

/* 列表项依次进入 */
@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(16px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.list-item {
    animation: slideUp var(--duration-normal) var(--ease-out) forwards;
    opacity: 0;
}

.list-item:nth-child(1) { animation-delay: 0ms; }
.list-item:nth-child(2) { animation-delay: 50ms; }
.list-item:nth-child(3) { animation-delay: 100ms; }
.list-item:nth-child(4) { animation-delay: 150ms; }
.list-item:nth-child(5) { animation-delay: 200ms; }
```

### 7.3 交互动效

```css
/* 按钮点击反馈 */
@keyframes buttonTap {
    0% { transform: scale(1); }
    50% { transform: scale(0.95); }
    100% { transform: scale(1); }
}

.btn:active {
    animation: buttonTap 150ms ease;
}

/* 加载动画 */
@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.spinner {
    animation: spin 1s linear infinite;
}

/* AI识别动画 */
@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

.ai-detecting {
    animation: pulse 1.5s ease-in-out infinite;
}
```

### 7.4 手机端专用动效

```css
@media (max-width: 767px) {
    /* 下拉刷新 */
    @keyframes pullRefresh {
        0% { transform: translateY(0); }
        100% { transform: translateY(-60px); }
    }
    
    /* 页面切换 */
    .page-enter {
        animation: slideInRight var(--duration-normal) var(--ease-out);
    }
    
    @keyframes slideInRight {
        from {
            opacity: 0;
            transform: translateX(20px);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
    
    /* 底部弹出 */
    .sheet-enter {
        animation: slideUp var(--duration-normal) var(--ease-out);
    }
}
```

---

## 八、图标系统

### 8.1 图标风格

- **图标类型**：线性图标（Line Icons）
- **图标线宽**：2px
- **图标尺寸**：24px（标准）/ 20px（小）/ 32px（大）
- **图标颜色**：跟随文字色或使用品牌色

### 8.2 常用图标

| 图标 | 含义 | 使用场景 |
|------|------|----------|
| 🏠 首页 | 首页 | 底部导航 |
| 📷 场景 | 场景库 | 底部导航 |
| 📱 设备 | 设备库 | 底部导航 |
| 👤 我的 | 个人中心 | 底部导航 |
| 🔍 搜索 | 搜索 | 搜索框 |
| ⚙️ 设置 | 设置 | 导航栏 |
| ✨ AI | AI识别 | 功能按钮 |
| 📷 相机 | 相机 | 功能按钮 |
| ❤️ 收藏 | 收藏 | 操作按钮 |
| 📤 分享 | 分享 | 操作按钮 |
| ⬇️ 下载 | 下载 | 操作按钮 |
| 🗑️ 删除 | 删除 | 操作按钮 |
| ✏️ 编辑 | 编辑 | 操作按钮 |
| 🔙 返回 | 返回 | 导航栏 |
| ✓ 确认 | 确认 | 按钮 |
| ✕ 关闭 | 关闭 | 弹窗 |

---

## 九、阴影系统

```css
:root {
    /* 轻微阴影 - 卡片默认 */
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
    
    /* 标准阴影 - 卡片悬停 */
    --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.07);
    
    /* 中等阴影 - 浮层 */
    --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
    
    /* 大阴影 - 弹窗 */
    --shadow-xl: 0 20px 25px rgba(0, 0, 0, 0.15);
    
    /* 品牌阴影 */
    --shadow-primary: 0 4px 14px rgba(0, 160, 109, 0.25);
}

/* 应用示例 */
.card {
    box-shadow: var(--shadow-sm);
}

.card:hover {
    box-shadow: var(--shadow-md);
}

.floating-button {
    box-shadow: var(--shadow-lg);
}

.modal {
    box-shadow: var(--shadow-xl);
}
```

---

## 十、圆角系统

```css
:root {
    /* 圆角尺寸 */
    --radius-xs: 4px;      /* 微圆角 - 标签 */
    --radius-sm: 8px;      /* 小圆角 - 按钮 */
    --radius-md: 12px;     /* 中圆角 - 输入框 */
    --radius-lg: 16px;     /* 大圆角 - 卡片 */
    --radius-xl: 24px;     /* 超大圆角 - 弹窗 */
    --radius-full: 9999px; /* 全圆角 - 头像/标签 */
}

/* 应用示例 */
.tag {
    border-radius: var(--radius-full);
}

.btn {
    border-radius: var(--radius-sm);
}

.card {
    border-radius: var(--radius-lg);
}

.modal {
    border-radius: var(--radius-xl);
}
```

---

## 十一、无障碍设计

### 11.1 色彩对比度

```css
/* AAA级对比度（推荐） */
.text-primary {
    color: #1A1A1A on #FFFFFF; /* 16:1 */
}

.text-secondary {
    color: #666666 on #FFFFFF; /* 5.7:1 */
}

/* AA级对比度（最低要求） */
.text-hint {
    color: #999999 on #FFFFFF; /* 4.5:1 */
}
```

### 11.2 触控区域

```css
/* 最小触控区域：44x44px */
.touch-target {
    min-width: 44px;
    min-height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
}
```

### 11.3 焦点状态

```css
/* 键盘焦点指示器 */
:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

/* 跳过导航链接 */
.skip-link {
    position: absolute;
    top: -40px;
    left: 0;
    background: var(--color-primary);
    color: white;
    padding: 8px;
    z-index: 100;
}

.skip-link:focus {
    top: 0;
}
```

---

## 十二、设计资源

### 12.1 Sketch/Figma组件库

| 组件 | 路径 |
|------|------|
| 按钮组件 | Components/Buttons |
| 卡片组件 | Components/Cards |
| 表单组件 | Components/Forms |
| 导航组件 | Components/Navigation |
| 图标库 | Icons/Line |
| 品牌素材 | Brand/Assets |

### 12.2 设计令牌

所有设计令牌已上传至Figma Variables，可通过以下方式使用：
- **Figma**：Variables面板
- **代码**：CSS Variables / Design Tokens JSON

---

## 十三、设计验收清单

### 13.1 手机端验收

| 检查项 | 标准 | 状态 |
|--------|------|------|
| 状态栏高度 | 44px（刘海屏考虑安全区） | □ |
| 导航栏高度 | 56px | □ |
| 底部栏高度 | 56px + 安全区 | □ |
| 触控区域 | ≥44x44px | □ |
| 字体可读性 | 最小12px | □ |
| 色彩对比度 | ≥4.5:1 | □ |
| 动画流畅度 | ≥60fps | □ |

### 13.2 Web端验收

| 检查项 | 标准 | 状态 |
|--------|------|------|
| 最大宽度 | 1280px | □ |
| 侧边栏宽度 | 240px | □ |
| 内容区内边距 | 32px | □ |
| 网格间距 | 24px | □ |
| 字体层级 | 清晰可辨 | □ |
| 色彩对比度 | ≥4.5:1 | □ |
| 响应式断点 | 768px / 1024px | □ |

---

**文档版本**：v2.0.0  
**下次更新**：每次设计迭代后  
**维护人**：设计团队
