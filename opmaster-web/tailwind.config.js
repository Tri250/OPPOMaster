/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 主色与强调色 - 符合测试用例要求
        'accent-primary': '#FF6B35',
        'accent-secondary': '#E55C2E',
        'accent-tertiary': '#CC5529',
        
        // 哈苏色彩
        'hasselblad-orange': '#D4A574',
        'hasselblad': '#D4A574',
        'hasselblad-pro': '#E5A84A',
        
        // OPPO绿
        'oppo-green': '#2DB47A',
        'oppo-green-light': '#68D391',
        
        // 辅助色彩
        'ocean-blue': '#3B82F6',
        'sakura-pink': '#EC4899',
        'aurora-purple': '#8B5CF6',
        
        // 功能色彩
        'success-vital': '#22C55E',
        'warning-vital': '#F59E0B',
        'error-vital': '#EF4444',
        'info-vital': '#3B82F6',
        
        // 深色主题 - ColorOS 16 规范
        'deep-space': '#0F0F0F',
        'card-surface': '#1A1A1A',
        'card-pressed': '#222222',
        'elevated': '#1C1C1E',
        
        // 浅色主题
        'light-bg': '#FFFFFF',
        'light-surface': '#F5F5F5',
        
        // 文字颜色 - 深色模式
        'text-primary': '#FFFFFF',
        'text-secondary': '#B3B3B3',
        'text-tertiary': '#737373',
        
        // 文字颜色 - 浅色模式
        'light-text-primary': '#1A1A1A',
        'light-text-secondary': '#666666',
        'light-text-tertiary': '#999999',
        
        // 边框 - 深色模式
        'oppo-border': '#2A2A2A',
        'oppo-border-light': '#404040',
        
        // 边框 - 浅色模式
        'light-border': '#E0E0E0',
      },
      fontFamily: {
        'sans': ['OPPO Sans', 'Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      fontSize: {
        // 根据测试用例要求的字体大小规范
        'xs': ['0.75rem', { lineHeight: '1.4', fontWeight: '300' }],    // 12sp - 辅助文字
        'sm': ['0.875rem', { lineHeight: '1.4', fontWeight: '400' }],   // 14sp - 辅助文字
        'base': ['1rem', { lineHeight: '1.5', fontWeight: '400' }],     // 16sp - 正文描述
        'lg': ['1.125rem', { lineHeight: '1.4', fontWeight: '500' }],   // 18sp - 卡片标题
        'xl': ['1.25rem', { lineHeight: '1.4', fontWeight: '600' }],    // 20sp - 三级页面标题
        '2xl': ['1.5rem', { lineHeight: '1.3', fontWeight: '600' }],    // 24sp - 二级页面标题
        '3xl': ['1.75rem', { lineHeight: '1.2', fontWeight: '700' }],   // 28sp - 页面大标题
        '4xl': ['2rem', { lineHeight: '1.2', fontWeight: '700' }],      // 32sp - 首页大标题
      },
      fontWeight: {
        'light': '300',
        'normal': '400',
        'medium': '500',
        'semibold': '600',
        'bold': '700',
      },
      spacing: {
        // ColorOS 16 规范间距 - 符合测试用例要求
        '1': '4px',
        '2': '8px',
        '3': '12px',
        '4': '16px',
        '5': '20px',
        '6': '24px',
        '7': '28px',
        '8': '32px',
        '9': '36px',
        '10': '40px',
        '12': '48px',
        '16': '64px',
      },
      borderRadius: {
        // ColorOS 16 规范圆角 - 符合测试用例要求
        'oppo': '16px',
        'oppo-lg': '20px',
        'oppo-md': '16px',
        'oppo-sm': '12px',
        'oppo-xs': '8px',
      },
      boxShadow: {
        // ColorOS 16 规范阴影
        'oppo': '0 4px 8px rgba(0, 0, 0, 0.2)',
        'oppo-hover': '0 6px 12px rgba(0, 0, 0, 0.3)',
        'oppo-card': '0 4px 8px rgba(0, 0, 0, 0.2)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'breathing': 'breathing 1.8s ease-in-out infinite',
        'fade-in': 'fadeIn 300ms ease-out forwards',
        'slide-in': 'slideIn 300ms ease-out forwards',
        'scale-in': 'scaleIn 200ms ease-in-out forwards',
        'stagger-in': 'staggerIn 300ms ease-out forwards',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-20px)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 20px rgba(255, 107, 53, 0.3)' },
          '50%': { boxShadow: '0 0 40px rgba(255, 107, 53, 0.6)' },
        },
        breathing: {
          '0%, 100%': { opacity: '0.7', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.05)' },
        },
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(20px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.98)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        staggerIn: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      transitionDuration: {
        'fast': '150ms',
        'normal': '200ms',
        'slow': '300ms',
        'slower': '500ms',
      },
      transitionTimingFunction: {
        'oppo-enter': 'cubic-bezier(0.05, 0.7, 0.1, 1.0)',
        'oppo-exit': 'cubic-bezier(0.3, 0.0, 0.8, 0.15)',
        'oppo-bounce': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
        'ease-out': 'cubic-bezier(0.0, 0.0, 0.2, 1)',
        'ease-in': 'cubic-bezier(0.4, 0.0, 1, 1)',
        'ease-in-out': 'cubic-bezier(0.4, 0.0, 0.2, 1)',
      },
    },
  },
  plugins: [],
}
