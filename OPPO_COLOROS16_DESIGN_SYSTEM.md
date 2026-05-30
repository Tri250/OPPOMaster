# OPPOMaster V2.0 - ColorOS 16 Premium Photography Design System

**版本**: V2026.2
**分支**: trae/solo-agent-g4xAg3
**设计标准**: ColorOS 16 Aqua Design + OPPO Premium Photography
**编制日期**: 2026年5月30日

---

## 一、设计理念

### 1.1 ColorOS 16 Aqua Design核心原则

**水生设计语言 (Aqua Design Language)**
- 轻盈 (Lightness) - 如同水般的轻盈质感
- 流动 (Flow) - 自然流畅的交互体验  
- 包容 (Inclusive) - 包容万物的和谐之美
- 纯净 (Pure) - 简洁纯粹的视觉表达

**OPPO品牌核心**
- 影像至上 (Imaging First)
- 专业哈苏 (Hasselblad Partnership)
- AI智能 (Intelligence)
- 用户体验 (User-Centric)

### 1.2 设计愿景

为专业摄影师和摄影爱好者打造一个：
- **专业感** - 哈苏影像级别的视觉体验
- **简洁感** - ColorOS 16的轻盈优雅
- **智能感** - AI赋能的摄影工作流
- **沉浸感** - 全屏沉浸式摄影体验

---

## 二、色彩系统 (ColorOS 16 Palette)

### 2.1 主色调

#### OPPO品牌色
```css
--oppo-coral: #FF6B5B;          /* OPPO珊瑚橙 - 主品牌色 */
--oppo-coral-light: #FF8A7A;     /* 浅珊瑚橙 */
--oppo-coral-dark: #E85A4A;     /* 深珊瑚橙 */
```

#### 哈苏品牌色
```css
--hasselblad-gold: #C9A86C;      /* 哈苏金 - 专业影像 */
--hasselblad-gold-light: #D4B87A;/* 浅哈苏金 */
--hasselblad-gold-dark: #B89858; /* 深哈苏金 */
```

#### Aqua Design主色
```css
--aqua-primary: #007AFF;         /* 水蓝 - 主要交互色 */
--aqua-primary-light: #3395FF;   /* 浅水蓝 */
--aqua-primary-dark: #0062D6;     /* 深水蓝 */
```

### 2.2 功能色

#### 成功色 (OPPO Green)
```css
--success: #00C853;               /* 成功绿 */
--success-light: #69F0AE;        /* 浅成功绿 */
--success-dark: #00A844;         /* 深成功绿 */
```

#### 警告色 (OPPO Yellow)
```css
--warning: #FFB300;              /* 警告黄 */
--warning-light: #FFD54F;        /* 浅警告黄 */
--warning-dark: #FF8F00;         /* 深警告黄 */
```

#### 错误色 (OPPO Red)
```css
--error: #FF3D71;                /* 错误红 */
--error-light: #FF6B8A;          /* 浅错误红 */
--error-dark: #D32F4F;           /* 深错误红 */
```

#### 信息色 (OPPO Blue)
```css
--info: #2196F3;                 /* 信息蓝 */
--info-light: #64B5F6;           /* 浅信息蓝 */
--info-dark: #1976D2;            /* 深信息蓝 */
```

### 2.3 中性色 (Neutral Colors)

#### 深色主题
```css
--surface-900: #000000;          /* 纯黑背景 */
--surface-800: #0A0A0F;         /* 深层背景 */
--surface-700: #121218;         /* 卡片背景 */
--surface-600: #1A1A24;         /* 卡片悬浮 */
--surface-500: #242432;         /* 边框分隔 */
--surface-400: #2E2E3D;         /* 次要边框 */
--surface-300: #404050;         /* 禁用状态 */
--surface-200: #6B6B80;         /* 次要文本 */
--surface-100: #9090A5;         /* 次要文本2 */
--surface-50: #C8C8D8;          /* 主要文本2 */
--surface-0: #FFFFFF;           /* 主要文本 */
```

#### 浅色主题
```css
--light-surface-900: #FFFFFF;   /* 纯白背景 */
--light-surface-800: #F8F9FB;   /* 浅层背景 */
--light-surface-700: #F0F2F5;   /* 卡片背景 */
--light-surface-600: #E8EBF0;   /* 卡片悬浮 */
--light-surface-500: #DDE1E8;   /* 边框分隔 */
--light-surface-400: #C5CAD4;   /* 次要边框 */
--light-surface-300: #9BA5B0;   /* 禁用状态 */
--light-surface-200: #6B7280;    /* 次要文本 */
--light-surface-100: #4B5563;    /* 次要文本2 */
--light-surface-50: #1F2937;    /* 主要文本2 */
--light-surface-0: #111827;      /* 主要文本 */
```

### 2.4 渐变色 (Gradients)

#### 哈苏影像渐变
```css
--gradient-hasselblad: linear-gradient(135deg, #C9A86C 0%, #8B7355 100%);
--gradient-hasselblad-light: linear-gradient(135deg, #D4B87A 0%, #A08565 100%);
--gradient-hasselblad-glow: linear-gradient(135deg, rgba(201, 168, 108, 0.4) 0%, rgba(139, 115, 85, 0.2) 100%);
```

#### Aqua水波渐变
```css
--gradient-aqua: linear-gradient(180deg, #007AFF 0%, #00C4E8 100%);
--gradient-aqua-light: linear-gradient(180deg, #3395FF 0%, #33D4FF 100%);
```

#### OPPO品牌渐变
```css
--gradient-oppo: linear-gradient(135deg, #FF6B5B 0%, #FF9A7A 100%);
--gradient-sunset: linear-gradient(180deg, #FF6B5B 0%, #FFB347 50%, #FFD93D 100%);
```

#### 深空渐变（摄影主题）
```css
--gradient-deep-space: linear-gradient(180deg, #0A0A0F 0%, #1A1A24 50%, #242432 100%);
--gradient-night: linear-gradient(180deg, #121218 0%, #1E1E2E 50%, #2A2A3C 100%);
--gradient-blue-hour: linear-gradient(180deg, #0D1B2A 0%, #1B3A4B 50%, #2E5266 100%);
```

---

## 三、字体系统 (Typography)

### 3.1 字体家族

**中文**
```css
font-family: 'OPPO Sans', 'PingFang SC', -apple-system, BlinkMacSystemFont, sans-serif;
```

**英文/数字**
```css
font-family: 'SF Pro Display', 'DIN Pro', -apple-system, BlinkMacSystemFont, sans-serif;
```

**代码/参数**
```css
font-family: 'SF Mono', 'Fira Code', 'JetBrains Mono', monospace;
```

### 3.2 字体层级 (Type Scale)

#### Display - 超大标题
```css
.text-display-xl {
  font-size: 3.75rem;    /* 60px */
  font-weight: 700;      /* Bold */
  line-height: 1.1;     /* 紧凑 */
  letter-spacing: -0.025em;
}

.text-display-lg {
  font-size: 3rem;       /* 48px */
  font-weight: 700;      /* Bold */
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.text-display-md {
  font-size: 2.25rem;    /* 36px */
  font-weight: 700;      /* Bold */
  line-height: 1.2;
  letter-spacing: -0.015em;
}
```

#### Headline - 大标题
```css
.text-headline-xl {
  font-size: 1.875rem;   /* 30px */
  font-weight: 600;      /* Semibold */
  line-height: 1.3;
  letter-spacing: -0.01em;
}

.text-headline-lg {
  font-size: 1.5rem;     /* 24px */
  font-weight: 600;      /* Semibold */
  line-height: 1.35;
  letter-spacing: -0.008em;
}

.text-headline-md {
  font-size: 1.25rem;    /* 20px */
  font-weight: 600;      /* Semibold */
  line-height: 1.4;
}
```

#### Title - 标题
```css
.text-title-xl {
  font-size: 1.125rem;   /* 18px */
  font-weight: 600;      /* Semibold */
  line-height: 1.45;
}

.text-title-lg {
  font-size: 1rem;       /* 16px */
  font-weight: 600;      /* Semibold */
  line-height: 1.5;
}

.text-title-md {
  font-size: 0.9375rem;  /* 15px */
  font-weight: 500;      /* Medium */
  line-height: 1.5;
}
```

#### Body - 正文
```css
.text-body-xl {
  font-size: 1rem;       /* 16px */
  font-weight: 400;      /* Regular */
  line-height: 1.6;
}

.text-body-lg {
  font-size: 0.9375rem;  /* 15px */
  font-weight: 400;      /* Regular */
  line-height: 1.6;
}

.text-body-md {
  font-size: 0.875rem;   /* 14px */
  font-weight: 400;      /* Regular */
  line-height: 1.6;
}

.text-body-sm {
  font-size: 0.8125rem;  /* 13px */
  font-weight: 400;      /* Regular */
  line-height: 1.5;
}
```

#### Caption - 辅助文字
```css
.text-caption-xl {
  font-size: 0.75rem;    /* 12px */
  font-weight: 500;      /* Medium */
  line-height: 1.5;
  letter-spacing: 0.01em;
}

.text-caption-lg {
  font-size: 0.6875rem;  /* 11px */
  font-weight: 500;      /* Medium */
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.text-caption-md {
  font-size: 0.625rem;   /* 10px */
  font-weight: 500;      /* Medium */
  line-height: 1.4;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}
```

---

## 四、间距系统 (Spacing)

### 4.1 基础间距单位
ColorOS 16使用8px作为基础网格单位。

```css
--space-0: 0px;
--space-1: 4px;     /* 0.5x */
--space-2: 8px;     /* 1x - 基础单位 */
--space-3: 12px;    /* 1.5x */
--space-4: 16px;    /* 2x */
--space-5: 20px;    /* 2.5x */
--space-6: 24px;    /* 3x */
--space-8: 32px;    /* 4x */
--space-10: 40px;   /* 5x */
--space-12: 48px;   /* 6x */
--space-16: 64px;   /* 8x */
--space-20: 80px;   /* 10x */
--space-24: 96px;   /* 12x */
--space-32: 128px;  /* 16x */
```

### 4.2 组件间距规范

```css
/* 页面内边距 */
--page-padding-x: 20px;           /* 移动端水平边距 */
--page-padding-y: 24px;            /* 页面顶部/底部边距 */

/* 卡片内边距 */
--card-padding: 16px;              /* 小卡片 */
--card-padding-lg: 20px;          /* 中卡片 */
--card-padding-xl: 24px;          /* 大卡片 */

/* 元素间距 */
--element-gap-xs: 4px;             /* 极小间距 */
--element-gap-sm: 8px;            /* 小间距 */
--element-gap-md: 12px;           /* 中间距 */
--element-gap-lg: 16px;           /* 大间距 */
--element-gap-xl: 20px;            /* 特大间距 */

/* 列表项间距 */
--list-gap: 12px;                  /* 列表项之间 */
--list-gap-dense: 8px;            /* 紧凑列表 */

/* 网格间距 */
--grid-gap: 16px;                 /* 卡片网格间距 */
--grid-gap-lg: 24px;              /* 大卡片网格 */
```

---

## 五、圆角系统 (Border Radius)

### 5.1 圆角层级

```css
/* 微圆角 - 小元素 */
--radius-none: 0px;
--radius-xs: 4px;
--radius-sm: 8px;

/* 中圆角 - 主要组件 */
--radius-md: 12px;                /* 按钮、输入框 */
--radius-lg: 16px;                /* 卡片、面板 */
--radius-xl: 20px;                /* 大卡片 */

/* 大圆角 - 全屏元素 */
--radius-2xl: 24px;              /* 模态框 */
--radius-3xl: 28px;              /* 底部抽屉 */
--radius-full: 9999px;            /* 圆形、胶囊按钮 */
```

### 5.2 组件圆角规范

```css
/* 按钮 */
--btn-radius: var(--radius-md);   /* 12px */

/* 卡片 */
--card-radius: var(--radius-xl);   /* 20px */

/* 输入框 */
--input-radius: var(--radius-md); /* 12px */

/* 标签 */
--tag-radius: var(--radius-sm);   /* 8px */

/* 图片 */
--image-radius: var(--radius-lg); /* 16px */
--avatar-radius: var(--radius-full); /* 圆形 */
```

---

## 六、阴影系统 (Shadows)

### 6.1 深色主题阴影

```css
/* 柔和阴影 - 轻微浮起 */
--shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.08);
--shadow-sm-colored: 0 2px 8px rgba(201, 168, 108, 0.15);

/* 中等阴影 - 卡片悬浮 */
--shadow-md: 0 4px 16px rgba(0, 0, 0, 0.12);
--shadow-md-colored: 0 4px 16px rgba(201, 168, 108, 0.2);

/* 强阴影 - 模态框 */
--shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.16);
--shadow-lg-colored: 0 8px 32px rgba(201, 168, 108, 0.25);

/* 夸张阴影 - 悬浮窗 */
--shadow-xl: 0 16px 48px rgba(0, 0, 0, 0.2);
--shadow-xl-glow: 0 16px 48px rgba(201, 168, 108, 0.3);
```

### 6.2 浅色主题阴影

```css
--shadow-light-sm: 0 2px 8px rgba(0, 0, 0, 0.04);
--shadow-light-md: 0 4px 16px rgba(0, 0, 0, 0.06);
--shadow-light-lg: 0 8px 32px rgba(0, 0, 0, 0.08);
--shadow-light-xl: 0 16px 48px rgba(0, 0, 0, 0.1);
```

### 6.3 特殊效果阴影

```css
/* 哈苏金发光 */
--shadow-glow-gold: 0 0 20px rgba(201, 168, 108, 0.4),
                    0 0 40px rgba(201, 168, 108, 0.2);

/* 水波纹发光 */
--shadow-glow-blue: 0 0 20px rgba(0, 122, 255, 0.4),
                    0 0 40px rgba(0, 122, 255, 0.2);

/* OPPO品牌发光 */
--shadow-glow-coral: 0 0 20px rgba(255, 107, 91, 0.4),
                     0 0 40px rgba(255, 107, 91, 0.2);
```

---

## 七、卡片系统 (Card System)

### 7.1 基础卡片

```css
.card {
  background: var(--surface-700);
  border-radius: var(--radius-xl);
  padding: var(--card-padding-lg);
  border: 1px solid var(--surface-500);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 悬浮状态 */
.card:hover {
  background: var(--surface-600);
  border-color: var(--hasselblad-gold);
  box-shadow: var(--shadow-md-colored);
  transform: translateY(-2px);
}
```

### 7.2 玻璃态卡片

```css
.card-glass {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-xl);
}

.card-glass-strong {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-xl);
}
```

### 7.3 摄影画廊卡片

```css
.card-gallery {
  background: var(--surface-700);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--surface-500);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-gallery:hover {
  border-color: var(--hasselblad-gold);
  box-shadow: var(--shadow-lg-colored);
  transform: scale(1.02);
}

.card-gallery-image {
  aspect-ratio: 4 / 3;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.card-gallery:hover .card-gallery-image {
  transform: scale(1.05);
}
```

---

## 八、按钮系统 (Button System)

### 8.1 主要按钮

```css
.btn-primary {
  background: var(--hasselblad-gold);
  color: #000000;
  font-weight: 600;
  border-radius: var(--radius-md);
  padding: 12px 24px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(201, 168, 108, 0.3);
}

.btn-primary:hover {
  background: var(--hasselblad-gold-light);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(201, 168, 108, 0.4);
}

.btn-primary:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(201, 168, 108, 0.3);
}
```

### 8.2 次要按钮

```css
.btn-secondary {
  background: transparent;
  color: var(--hasselblad-gold);
  border: 1.5px solid var(--hasselblad-gold);
  border-radius: var(--radius-md);
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-secondary:hover {
  background: rgba(201, 168, 108, 0.1);
}

.btn-secondary:active {
  background: rgba(201, 168, 108, 0.2);
}
```

### 8.3 图标按钮

```css
.btn-icon {
  width: 44px;
  height: 44px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-700);
  border: 1px solid var(--surface-500);
  border-radius: var(--radius-md);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-icon:hover {
  background: var(--surface-600);
  border-color: var(--hasselblad-gold);
}
```

---

## 九、动画系统 (Motion System)

### 9.1 时间轴

```css
/* 微交互 */
--duration-instant: 100ms;
--duration-fast: 150ms;
--duration-normal: 200ms;
--duration-slow: 300ms;

/* 页面过渡 */
--duration-page: 400ms;
--duration-modal: 350ms;
--duration-drawer: 300ms;

/* 特殊效果 */
--duration-entrance: 500ms;
--duration-exit: 300ms;
```

### 9.2 缓动函数

```css
/* 标准缓动 - 日常交互 */
--ease-standard: cubic-bezier(0.4, 0, 0.2, 1);

/* 进入缓动 - 元素出现 */
--ease-enter: cubic-bezier(0, 0, 0.2, 1);

/* 退出缓动 - 元素消失 */
--ease-exit: cubic-bezier(0.4, 0, 1, 1);

/* 强调缓动 - 弹性效果 */
--ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);

/* 线性 - 进度条 */
--ease-linear: linear;
```

### 9.3 特色动画

#### 水波纹效果
```css
@keyframes ripple {
  0% {
    transform: scale(0);
    opacity: 0.6;
  }
  100% {
    transform: scale(4);
    opacity: 0;
  }
}

.ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(201, 168, 108, 0.3);
  animation: ripple 0.6s ease-out;
  pointer-events: none;
}
```

#### 涟漪扩散
```css
@keyframes wave-expand {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.wave-expand {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.2) 0%, rgba(0, 196, 232, 0.1) 100%);
  animation: wave-expand 1.2s ease-out infinite;
  pointer-events: none;
}
```

#### 哈苏金发光
```css
@keyframes glow-pulse {
  0%, 100% {
    box-shadow: 0 0 20px rgba(201, 168, 108, 0.3),
                0 0 40px rgba(201, 168, 108, 0.2);
  }
  50% {
    box-shadow: 0 0 30px rgba(201, 168, 108, 0.5),
                0 0 60px rgba(201, 168, 108, 0.3);
  }
}

.glow-pulse {
  animation: glow-pulse 2s ease-in-out infinite;
}
```

---

## 十、交互规范

### 10.1 触摸目标

```css
/* 最小触摸目标 */
--touch-target-min: 44px;
--touch-target-lg: 56px;

/* 触摸反馈 */
.touch-feedback {
  position: relative;
  overflow: hidden;
}

.touch-feedback:active {
  background: rgba(255, 255, 255, 0.1);
}
```

### 10.2 手势支持

```css
/* 滑动删除 */
.swipe-action {
  transition: transform 0.3s var(--ease-standard);
}

.swipe-action.swiped {
  transform: translateX(-80px);
}

/* 长按菜单 */
.long-press {
  transition: transform 0.15s var(--ease-bounce);
}

.long-press.pressed {
  transform: scale(0.95);
}
```

---

## 十一、无障碍设计 (Accessibility)

### 11.1 焦点管理

```css
/* 焦点环 */
*:focus-visible {
  outline: 2px solid var(--hasselblad-gold);
  outline-offset: 2px;
}

/* 焦点内边距 */
.focus-ring {
  box-shadow: 0 0 0 4px rgba(201, 168, 108, 0.3);
}
```

### 11.2 Motion Safe

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 11.3 高对比度模式

```css
@media (prefers-contrast: high) {
  :root {
    --surface-700: #000000;
    --surface-0: #FFFFFF;
  }
}
```

---

## 十二、摄影专用组件

### 12.1 相机取景框

```css
.viewfinder {
  aspect-ratio: 3 / 4;
  border-radius: var(--radius-xl);
  border: 2px solid var(--hasselblad-gold);
  box-shadow: var(--shadow-glow-gold);
  overflow: hidden;
}

.viewfinder-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, 
    rgba(0, 0, 0, 0.3) 0%, 
    transparent 20%, 
    transparent 80%, 
    rgba(0, 0, 0, 0.3) 100%);
  pointer-events: none;
}
```

### 12.2 参数滑块

```css
.param-slider {
  height: 6px;
  background: var(--surface-500);
  border-radius: 3px;
  position: relative;
}

.param-slider-track {
  position: absolute;
  height: 100%;
  background: var(--gradient-hasselblad);
  border-radius: 3px;
  transition: width 0.15s var(--ease-standard);
}

.param-slider-thumb {
  width: 20px;
  height: 20px;
  background: var(--hasselblad-gold);
  border: 3px solid var(--surface-0);
  border-radius: 50%;
  box-shadow: var(--shadow-md);
  cursor: grab;
  transition: transform 0.2s var(--ease-bounce);
}

.param-slider-thumb:hover {
  transform: scale(1.2);
}

.param-slider-thumb:active {
  cursor: grabbing;
}
```

### 12.3 场景标签

```css
.scene-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--surface-700);
  border: 1px solid var(--surface-500);
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s var(--ease-standard);
}

.scene-tag.active {
  background: var(--gradient-hasselblad);
  color: #000000;
  border-color: transparent;
  box-shadow: var(--shadow-md-colored);
}

.scene-tag:hover:not(.active) {
  border-color: var(--hasselblad-gold);
  background: rgba(201, 168, 108, 0.1);
}
```

---

## 十三、总结

本设计系统基于ColorOS 16 Aqua Design语言，结合OPPO品牌风格和哈苏影像专业特性，为OPPOMaster打造了一套完整的视觉设计规范。

**核心特点**：
- 水生设计的轻盈流动感
- 哈苏金色的专业影像感
- OPPO珊瑚橙的活力感
- 摄影画廊的沉浸体验
- WCAG 2.2 AA级无障碍支持

**使用原则**：
1. 保持一致性 - 严格遵循本规范
2. 注重可访问性 - 所有功能需无障碍支持
3. 优化性能 - 动画需流畅（60fps）
4. 尊重用户 - 支持减少动画偏好
5. 持续迭代 - 根据用户反馈优化

---

**编制人**: OPPO Design Team
**版本**: V2026.2
**最后更新**: 2026年5月30日
