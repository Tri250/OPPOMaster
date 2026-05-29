/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌核心色彩 - 严格遵循规范
        'oppo-orange': '#FF6B35',
        'oppo-orange-dark': '#E55A2B',
        'oppo-orange-light': '#FFB399',
        'hasselblad-orange': '#D4A574',
        'hasselblad-gold': '#D4A574',
        'oppo-black': '#0F0F0F',
        'oppo-white': '#FFFFFF',
        
        // 中性灰系列 - 信息层级
        'neutral-50': '#F5F5F5',
        'neutral-100': '#E5E5E5',
        'neutral-200': '#D4D4D4',
        'neutral-300': '#A3A3A3',
        'neutral-400': '#737373',
        'neutral-500': '#525252',
        'neutral-600': '#404040',
        'neutral-700': '#262626',
        'neutral-800': '#1A1A1A',
        'neutral-900': '#0F0F0F',
        
        // 功能色彩
        'success': '#22C55E',
        'warning': '#F59E0B',
        'error': '#EF4444',
        'info': '#3B82F6',
        
        // 深色主题 - ColorOS 16 规范
        'bg-primary': '#0F0F0F',
        'bg-secondary': '#1A1A1A',
        'bg-tertiary': '#222222',
        'bg-elevated': '#1C1C1E',
        
        // 浅色主题
        'bg-light-primary': '#FFFFFF',
        'bg-light-secondary': '#F5F5F5',
        
        // 文字颜色 - 深色模式
        'text-primary': '#FFFFFF',
        'text-secondary': '#B3B3B3',
        'text-tertiary': '#737373',
        
        // 文字颜色 - 浅色模式
        'text-light-primary': '#1A1A1A',
        'text-light-secondary': '#666666',
        'text-light-tertiary': '#999999',
        
        // 边框 - 深色模式
        'border-default': '#2A2A2A',
        'border-light': '#404040',
        
        // 边框 - 浅色模式
        'border-light-default': '#E0E0E0',
        
        // 旧有别名保持兼容
        'accent-primary': '#FF6B35',
        'accent-secondary': '#E55C2E',
        'accent-tertiary': '#CC5529',
        'hasselblad': '#D4A574',
        'hasselblad-pro': '#E5A84A',
        'oppo-green': '#2DB47A',
        'deep-space': '#0F0F0F',
        'card-surface': '#1A1A1A',
        'card-pressed': '#222222',
        'elevated': '#1C1C1E',
        'light-bg': '#FFFFFF',
        'light-surface': '#F5F5F5',
        'light-text-primary': '#1A1A1A',
        'light-text-secondary': '#666666',
        'light-text-tertiary': '#999999',
        'oppo-border': '#2A2A2A',
        'oppo-border-light': '#404040',
        'light-border': '#E0E0E0',
        'oppo-sunrise-gold': '#FF6B35',
      },
      fontFamily: {
        'sans': ['OPPO Sans 3.0', 'OPPO Sans', 'Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      fontSize: {
        // OPPO Sans 3.0 字体层级系统 - 严格遵循规范
        'h1': ['1.5rem', { lineHeight: '2rem', fontWeight: '700', letterSpacing: '0em' }],      // 24sp - 页面大标题
        'h2': ['1.25rem', { lineHeight: '1.75rem', fontWeight: '700', letterSpacing: '0em' }],  // 20sp - 导航栏标题
        'h3': ['1.125rem', { lineHeight: '1.5rem', fontWeight: '600', letterSpacing: '0em' }],  // 18sp - 卡片标题
        'body1': ['1rem', { lineHeight: '1.375rem', fontWeight: '400', letterSpacing: '0.01em' }],  // 16sp - 正文
        'body2': ['0.875rem', { lineHeight: '1.25rem', fontWeight: '400', letterSpacing: '0.01em' }], // 14sp - 辅助文字
        'caption': ['0.75rem', { lineHeight: '1rem', fontWeight: '400', letterSpacing: '0.02em' }],  // 12sp - 标签
        'number': ['1.125rem', { lineHeight: '1.5rem', fontWeight: '600', letterSpacing: '0em' }],  // 18sp - 参数数值
      },
      fontWeight: {
        'light': '300',
        'normal': '400',
        'medium': '500',
        'semibold': '600',
        'bold': '700',
      },
      spacing: {
        // 8dp网格系统 - ColorOS 16规范
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
        '14': '56px',
        '16': '64px',
        '18': '72px',
        '20': '80px',
        '24': '96px',
      },
      borderRadius: {
        // ColorOS 16 规范圆角
        'xs': '8px',
        'sm': '12px',
        'md': '16px',
        'lg': '20px',
        'xl': '24px',
        'oppo': '16px',
        'oppo-lg': '20px',
        'oppo-md': '16px',
        'oppo-sm': '12px',
        'oppo-xs': '8px',
      },
      boxShadow: {
        // OPPO软阴影系统
        'oppo-elevation-1': '0 2px 8px rgba(0, 0, 0, 0.1)',
        'oppo-elevation-2': '0 4px 16px rgba(0, 0, 0, 0.15)',
        'oppo-elevation-3': '0 8px 24px rgba(0, 0, 0, 0.2)',
        'oppo': '0 4px 8px rgba(0, 0, 0, 0.1)',
        'oppo-hover': '0 6px 20px rgba(0, 0, 0, 0.2)',
        'oppo-card': '0 4px 16px rgba(0, 0, 0, 0.15)',
      },
      animation: {
        // ColorOS 16 动效
        'float': 'float 6s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'breathing': 'breathing 1.8s ease-in-out infinite',
        'fade-in': 'fadeIn 300ms ease-out-cubic forwards',
        'slide-in-right': 'slideInRight 300ms ease-out-cubic forwards',
        'slide-out-left': 'slideOutLeft 300ms ease-in-cubic forwards',
        'scale-in': 'scaleIn 200ms ease-in-out-cubic forwards',
        'modal-in': 'modalIn 250ms ease-out-cubic forwards',
        'modal-out': 'modalOut 200ms ease-in-cubic forwards',
        'stagger-in': 'staggerIn 300ms ease-out-cubic forwards',
        'bounce-scale': 'bounceScale 300ms ease-out-bounce forwards',
        'shimmer': 'shimmer 1.5s ease-in-out infinite',
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
        slideInRight: {
          '0%': { opacity: '0', transform: 'translateX(32px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        slideOutLeft: {
          '0%': { opacity: '1', transform: 'translateX(0)' },
          '100%': { opacity: '0', transform: 'translateX(32px)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.98)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        modalIn: {
          '0%': { opacity: '0', transform: 'translateY(100%) scale(0.9)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        modalOut: {
          '0%': { opacity: '1', transform: 'translateY(0) scale(1)' },
          '100%': { opacity: '0', transform: 'translateY(100%) scale(0.9)' },
        },
        staggerIn: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        bounceScale: {
          '0%': { transform: 'scale(1)' },
          '50%': { transform: 'scale(1.2)' },
          '100%': { transform: 'scale(1)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '200% 0' },
          '100%': { backgroundPosition: '-200% 0' },
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
        'ease-out-cubic': 'cubic-bezier(0.22, 0.61, 0.36, 1)',
        'ease-in-cubic': 'cubic-bezier(0.55, 0.06, 0.68, 0.19)',
        'ease-in-out-cubic': 'cubic-bezier(0.65, 0, 0.35, 1)',
        'ease-out-bounce': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
      },
    },
  },
  plugins: [],
}
