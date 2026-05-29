/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌核心色彩 - ColorOS 16 规范
        'oppo-orange': '#FF6B35',
        'oppo-orange-dark': '#E55A2B',
        'oppo-orange-light': '#FFB399',
        'oppo-green': '#2DB47A',
        'oppo-blue': '#3B82F6',
        'oppo-purple': '#8B5CF6',
        'oppo-pink': '#EC4899',
        'oppo-black': '#0F0F0F',
        
        // 哈苏专业色彩
        'hasselblad-orange': '#D4A574',
        'hasselblad-gold': '#E5C07B',
        
        // ============ 中性灰系列 - 信息层级 ============
        'neutral-50': '#F9F9F9',
        'neutral-100': '#F0F0F0',
        'neutral-200': '#E0E0E0',
        'neutral-300': '#C4C4C4',
        'neutral-400': '#9E9E9E',
        'neutral-500': '#757575',
        'neutral-600': '#525252',
        'neutral-700': '#303030',
        'neutral-800': '#1A1A1A',
        'neutral-900': '#0F0F0F',
        
        // ============ 功能色彩 - ColorOS 16 ============
        'success': '#2DB47A',
        'success-light': '#A7F3D0',
        'warning': '#F59E0B',
        'warning-light': '#FDE68A',
        'error': '#EF4444',
        'error-light': '#FECACA',
        'info': '#3B82F6',
        'info-light': '#BFDBFE',
        
        // ============ 深色模式 - ColorOS 16 规范 ============
        'bg-primary': '#0F0F0F',
        'bg-secondary': '#171717',
        'bg-tertiary': '#1F1F1F',
        'bg-elevated': '#1C1C1E',
        'bg-glass': 'rgba(15, 15, 15, 0.72)',
        
        // 浅色模式
        'bg-light-primary': '#FFFFFF',
        'bg-light-secondary': '#F5F5F5',
        'bg-light-tertiary': '#EEEEEE',
        
        // ============ 文字颜色 - 深色模式 ============
        'text-primary': '#FFFFFF',
        'text-secondary': '#B3B3B3',
        'text-tertiary': '#757575',
        'text-disabled': '#525252',
        
        // 文字颜色 - 浅色模式
        'text-light-primary': '#1A1A1A',
        'text-light-secondary': '#666666',
        'text-light-tertiary': '#999999',
        
        // ============ 边框 - 深色模式 ============
        'border-default': '#2A2A2A',
        'border-light': '#383838',
        'border-strong': '#4A4A4A',
        
        // 边框 - 浅色模式
        'border-light-default': '#E0E0E0',
        
        // ============ 兼容别名 ============
        'accent-primary': '#FF6B35',
        'accent-secondary': '#E55A2B',
        'accent-tertiary': '#CC5529',
        'hasselblad': '#D4A574',
        'hasselblad-pro': '#E5C07B',
        'deep-space': '#0F0F0F',
        'card-surface': '#171717',
        'card-pressed': '#222222',
        'elevated': '#1C1C1E',
        'light-bg': '#FFFFFF',
        'light-surface': '#F5F5F5',
        'light-text-primary': '#1A1A1A',
        'light-text-secondary': '#666666',
        'light-text-tertiary': '#999999',
        'oppo-border': '#2A2A2A',
        'oppo-border-light': '#383838',
        'light-border': '#E0E0E0',
        'oppo-sunrise-gold': '#FF6B35',
        'ocean-blue': '#3B82F6',
        'rose-gold': '#EC4899',
        'pure-green': '#2DB47A',
        
        // 半透明白色
        'white/3': 'rgba(255, 255, 255, 0.03)',
        'white/5': 'rgba(255, 255, 255, 0.05)',
        'white/6': 'rgba(255, 255, 255, 0.06)',
        'white/8': 'rgba(255, 255, 255, 0.08)',
        'white/10': 'rgba(255, 255, 255, 0.1)',
        'white/15': 'rgba(255, 255, 255, 0.15)',
        'white/20': 'rgba(255, 255, 255, 0.2)',
        'white/30': 'rgba(255, 255, 255, 0.3)',
        'white/40': 'rgba(255, 255, 255, 0.4)',
        'white/50': 'rgba(255, 255, 255, 0.5)',
        'white/60': 'rgba(255, 255, 255, 0.6)',
        'white/70': 'rgba(255, 255, 255, 0.7)',
        'white/80': 'rgba(255, 255, 255, 0.8)',
        'white/85': 'rgba(255, 255, 255, 0.85)',
        'white/90': 'rgba(255, 255, 255, 0.9)',
        'white/95': 'rgba(255, 255, 255, 0.95)',
        
        // 半透明背景色
        'bg-primary/85': 'rgba(15, 15, 15, 0.85)',
        'bg-elevated/85': 'rgba(28, 28, 30, 0.85)',
        
        // 半透明黑色
        'black/5': 'rgba(0, 0, 0, 0.05)',
        'black/8': 'rgba(0, 0, 0, 0.08)',
        'black/10': 'rgba(0, 0, 0, 0.1)',
        'black/20': 'rgba(0, 0, 0, 0.2)',
        'black/30': 'rgba(0, 0, 0, 0.3)',
        'black/40': 'rgba(0, 0, 0, 0.4)',
        'black/50': 'rgba(0, 0, 0, 0.5)',
        'black/60': 'rgba(0, 0, 0, 0.6)',
        
        // 半透明边框
        'border-light-default/50': 'rgba(224, 224, 224, 0.5)',
        'border-oppo-orange/50': 'rgba(255, 107, 53, 0.5)',
        'border-oppo-orange/30': 'rgba(255, 107, 53, 0.3)',
        'border-hasselblad-orange/30': 'rgba(212, 165, 116, 0.3)',
      },
      fontFamily: {
        'sans': ['OPPO Sans 3.0', 'OPPO Sans', 'Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      fontSize: {
        // ============ OPPO Sans 3.0 字体层级系统 - ColorOS 16 严格规范 ============
        'display': ['2.25rem', { lineHeight: '2.75rem', fontWeight: '700', letterSpacing: '0em' }],    // 36sp - 超大标题
        'h1': ['1.75rem', { lineHeight: '2.25rem', fontWeight: '700', letterSpacing: '0em' }],      // 28sp - 页面大标题
        'h2': ['1.5rem', { lineHeight: '2rem', fontWeight: '700', letterSpacing: '0em' }],         // 24sp - 导航栏标题
        'h3': ['1.25rem', { lineHeight: '1.75rem', fontWeight: '600', letterSpacing: '0em' }],     // 20sp - 卡片标题
        'body1': ['1rem', { lineHeight: '1.5rem', fontWeight: '400', letterSpacing: '0.01em' }],    // 16sp - 正文
        'body2': ['0.875rem', { lineHeight: '1.25rem', fontWeight: '400', letterSpacing: '0.01em' }], // 14sp - 辅助文字
        'caption': ['0.75rem', { lineHeight: '1rem', fontWeight: '400', letterSpacing: '0.02em' }],  // 12sp - 标签
        'micro': ['0.6875rem', { lineHeight: '0.9375rem', fontWeight: '400', letterSpacing: '0.02em' }], // 11sp - 极小文字
        'number': ['1.25rem', { lineHeight: '1.75rem', fontWeight: '600', letterSpacing: '0em' }],  // 20sp - 参数数值
        'number-lg': ['1.5rem', { lineHeight: '2rem', fontWeight: '700', letterSpacing: '0em' }],  // 24sp - 大数值
      },
      fontWeight: {
        'light': '300',
        'normal': '400',
        'medium': '500',
        'semibold': '600',
        'bold': '700',
      },
      spacing: {
        // ============ 8dp网格系统 - ColorOS 16规范 ============
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
        '11': '44px',
        '12': '48px',
        '13': '52px',
        '14': '56px',
        '15': '60px',
        '16': '64px',
        '18': '72px',
        '20': '80px',
        '24': '96px',
      },
      borderRadius: {
        // ============ ColorOS 16 规范圆角系统 ============
        'xs': '8px',
        'sm': '12px',
        'md': '16px',
        'lg': '20px',
        'xl': '24px',
        '2xl': '28px',
        '3xl': '32px',
        'oppo': '16px',
        'oppo-lg': '20px',
        'oppo-md': '16px',
        'oppo-sm': '12px',
        'oppo-xs': '8px',
        'pill': '9999px',
      },
      boxShadow: {
        // ============ ColorOS 16 专业阴影系统 ============
        'oppo-elevation-1': '0 2px 8px rgba(0, 0, 0, 0.08)',
        'oppo-elevation-2': '0 4px 16px rgba(0, 0, 0, 0.12)',
        'oppo-elevation-3': '0 8px 24px rgba(0, 0, 0, 0.16)',
        'oppo-elevation-4': '0 12px 32px rgba(0, 0, 0, 0.20)',
        'oppo': '0 4px 8px rgba(0, 0, 0, 0.1)',
        'oppo-hover': '0 8px 24px rgba(0, 0, 0, 0.16)',
        'oppo-card': '0 4px 16px rgba(0, 0, 0, 0.12)',
        'oppo-glow-orange': '0 0 30px rgba(255, 107, 53, 0.3)',
        'oppo-glow-green': '0 0 30px rgba(45, 180, 122, 0.3)',
      },
      animation: {
        // ============ ColorOS 16 动效系统 ============
        'float': 'float 8s ease-in-out infinite',
        'float-slow': 'float 12s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2.5s ease-in-out infinite',
        'breathing': 'breathing 2s ease-in-out infinite',
        'fade-in': 'fadeIn 300ms ease-out-cubic forwards',
        'fade-in-up': 'fadeInUp 400ms ease-out-cubic forwards',
        'fade-in-down': 'fadeInDown 400ms ease-out-cubic forwards',
        'slide-in-right': 'slideInRight 300ms ease-out-cubic forwards',
        'slide-in-left': 'slideInLeft 300ms ease-out-cubic forwards',
        'slide-out-left': 'slideOutLeft 300ms ease-in-cubic forwards',
        'slide-out-right': 'slideOutRight 300ms ease-in-cubic forwards',
        'scale-in': 'scaleIn 200ms ease-in-out-cubic forwards',
        'scale-in-up': 'scaleInUp 300ms ease-out-cubic forwards',
        'modal-in': 'modalIn 250ms ease-out-cubic forwards',
        'modal-out': 'modalOut 200ms ease-in-cubic forwards',
        'stagger-in': 'staggerIn 300ms ease-out-cubic forwards',
        'bounce-scale': 'bounceScale 300ms ease-out-bounce forwards',
        'shimmer': 'shimmer 2s ease-in-out infinite',
        'ripple': 'ripple 600ms ease-out forwards',
        'swipe-right': 'swipeRight 300ms ease-out forwards',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-24px)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 24px rgba(255, 107, 53, 0.25)', opacity: '1' },
          '50%': { boxShadow: '0 0 48px rgba(255, 107, 53, 0.5)', opacity: '0.9' },
        },
        breathing: {
          '0%, 100%': { opacity: '0.75', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.03)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        fadeInDown: {
          '0%': { opacity: '0', transform: 'translateY(-24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideInRight: {
          '0%': { opacity: '0', transform: 'translateX(40px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        slideInLeft: {
          '0%': { opacity: '0', transform: 'translateX(-40px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        slideOutLeft: {
          '0%': { opacity: '1', transform: 'translateX(0)' },
          '100%': { opacity: '0', transform: 'translateX(-40px)' },
        },
        slideOutRight: {
          '0%': { opacity: '1', transform: 'translateX(0)' },
          '100%': { opacity: '0', transform: 'translateX(40px)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.95)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        scaleInUp: {
          '0%': { opacity: '0', transform: 'scale(0.9) translateY(20px)' },
          '100%': { opacity: '1', transform: 'scale(1) translateY(0)' },
        },
        modalIn: {
          '0%': { opacity: '0', transform: 'translateY(100%) scale(0.92)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' },
        },
        modalOut: {
          '0%': { opacity: '1', transform: 'translateY(0) scale(1)' },
          '100%': { opacity: '0', transform: 'translateY(100%) scale(0.92)' },
        },
        staggerIn: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
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
        ripple: {
          '0%': { transform: 'scale(0)', opacity: '0.4' },
          '100%': { transform: 'scale(2.5)', opacity: '0' },
        },
        swipeRight: {
          '0%': { transform: 'translateX(0)' },
          '50%': { transform: 'translateX(8px)' },
          '100%': { transform: 'translateX(0)' },
        },
      },
      transitionDuration: {
        'fast': '120ms',
        'normal': '200ms',
        'slow': '300ms',
        'slower': '400ms',
        'slowest': '600ms',
      },
      transitionTimingFunction: {
        'oppo-enter': 'cubic-bezier(0.05, 0.7, 0.1, 1.0)',
        'oppo-exit': 'cubic-bezier(0.3, 0.0, 0.8, 0.15)',
        'oppo-bounce': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
        'ease-out-cubic': 'cubic-bezier(0.22, 0.61, 0.36, 1)',
        'ease-in-cubic': 'cubic-bezier(0.55, 0.06, 0.68, 0.19)',
        'ease-in-out-cubic': 'cubic-bezier(0.65, 0, 0.35, 1)',
        'ease-out-bounce': 'cubic-bezier(0.175, 0.885, 0.32, 1.275)',
        'ease-out-elastic': 'cubic-bezier(0.18, 0.89, 0.32, 1.28)',
      },
      backdropBlur: {
        xs: '2px',
      },
    },
  },
  plugins: [],
}
