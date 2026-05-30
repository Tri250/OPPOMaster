/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌主色 #FF6B35
        'oppo-primary': '#FF6B35',
        'oppo-primary-light': '#FF8F6B',
        'oppo-primary-dark': '#E55A2D',
        
        // 哈苏橙 #D4A574（一级标题用）
        'hasselblad': '#D4A574',
        'hasselblad-light': '#E0B894',
        'hasselblad-dark': '#B88A54',
        
        // OPPO绿 #00C853
        'oppo-green': '#00C853',
        'oppo-green-light': '#33D976',
        
        // 背景色 - #0F0F0F页面背景，#1A1A1A卡片背景（比页面背景浅7%）
        'page-bg': '#0F0F0F',
        'card-bg': '#1A1A1A',
        'card-bg-alpha': 'rgba(26, 26, 26, 0.95)',
        
        // 文字色
        'text-primary': '#FFFFFF',      // 一级文字
        'text-secondary': '#CCCCCC',    // 二级文字
        'text-tertiary': '#999999',     // 三级文字
        
        // 功能色
        'oppo-blue': '#2962FF',
        'oppo-yellow': '#FF9800',
        'oppo-pink': '#E91E63',
        'oppo-purple': '#9C27B0',
        
        // 错误色
        'error': '#FF4444',
        
        // 分隔线颜色
        'divider': '#333333',
      },
      fontFamily: {
        'sans': ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'card': '16px',    // 卡片圆角
        'button': '12px',   // 按钮圆角
        'small': '8px',    // 小元素圆角
        'icon': '12px',    // 图标容器圆角
      },
      spacing: {
        'page': '16px',           // 页面左右边距
        'component': '12px',      // 组件间距
        'inner': '8px',           // 内边距
        'card-y': '24px',         // 卡片内上下边距
        'card-x': '20px',         // 卡片内左右边距
        'nav-content': '24px',    // 导航栏与内容区域间距
        'feature-card': '20px',    // 功能卡片之间垂直间距
        'tool-card': '16px',      // 工具卡片之间垂直间距
        
        // 固定高度
        'nav-height': '64px',     // 导航栏高度
        'icon-size': '64px',      // 图标尺寸
        'icon-size-small': '32px', // 小图标尺寸
      },
      fontSize: {
        'card-title': ['20px', { lineHeight: '1.3', fontWeight: '700', letterSpacing: '0.5px' }],
        'card-description': ['14px', { lineHeight: '1.5', fontWeight: '400', letterSpacing: '0.3px' }],
        'card-list': ['13px', { lineHeight: '1.5', fontWeight: '400' }],
        'card-subtitle': ['12px', { lineHeight: '1.5', fontWeight: '400' }],
      },
      boxShadow: {
        'card': '0 4px 8px rgba(0, 0, 0, 0.1)',
        'card-hover': '0 8px 16px rgba(0, 0, 0, 0.15)',
      }
    },
  },
  plugins: [],
}
