/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO 2026 主色调
        'oppo-sunrise-gold': '#FFB347',
        'oppo-sunrise-gold-light': '#FFD89A',
        'oppo-sunrise-gold-dark': '#E09420',
        
        // 哈苏色彩
        'hasselblad': '#D4A574',
        'hasselblad-pro': '#E5A84A',
        'hasselblad-vibrant': '#E8B163',
        'hasselblad-dark': '#9C6D30',
        
        // OPPO绿
        'oppo-green': '#2DB47A',
        'oppo-green-light': '#68D391',
        'oppo-green-dark': '#249D6B',
        
        // 辅助色彩
        'ocean-blue': '#3B82F6',
        'ocean-blue-light': '#60A5FA',
        'ocean-blue-dark': '#2563EB',
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
        'sans': ['OPPO Sans', 'Inter', 'system-ui', 'sans-serif'],
      },
      fontSize: {
        // 根据 ColorOS 16 规范调整字体大小
        'xs': ['0.75rem', { lineHeight: '1.2' }],      // 12sp
        'sm': ['0.875rem', { lineHeight: '1.3' }],     // 14sp
        'base': ['1rem', { lineHeight: '1.4' }],       // 16sp
        'lg': ['1.125rem', { lineHeight: '1.4' }],     // 18sp
        'xl': ['1.25rem', { lineHeight: '1.4' }],      // 20sp
        '2xl': ['1.5rem', { lineHeight: '1.3' }],      // 24sp
        '3xl': ['1.75rem', { lineHeight: '1.2' }],     // 28sp - 主标题
        '4xl': ['2rem', { lineHeight: '1.2' }],        // 32sp
        '5xl': ['3rem', { lineHeight: '1.1' }],        // 48sp
        '6xl': ['3.75rem', { lineHeight: '1' }],       // 60sp
        '7xl': ['4.5rem', { lineHeight: '1' }],        // 72sp
      },
      spacing: {
        // ColorOS 16 规范间距
        '1': '4px',
        '2': '8px',
        '3': '12px',
        '4': '16px',
        '5': '20px',
        '6': '24px',
        '8': '32px',
        '10': '40px',
        '12': '48px',
        '16': '64px',
        '20': '80px',
        '24': '96px',
      },
      borderRadius: {
        // ColorOS 16 规范圆角 - 统一使用 16dp
        'oppo': '16px',
        'oppo-lg': '20px',
        'oppo-md': '16px',
        'oppo-sm': '12px',
      },
      boxShadow: {
        // ColorOS 16 规范阴影
        'oppo': '0 4px 8px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(42, 42, 42, 0.8)',
        'oppo-hover': '0 8px 12px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 179, 71, 0.3)',
        'oppo-card': '0 4px 8px rgba(0, 0, 0, 0.2)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'breathing': 'breathing 1.8s ease-in-out infinite',
        'fade-in': 'fadeIn 500ms ease-out forwards',
        'slide-in': 'slideIn 300ms ease-out forwards',
        'scale-in': 'scaleIn 200ms ease-in-out forwards',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-20px)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 20px rgba(255, 179, 71, 0.3)' },
          '50%': { boxShadow: '0 0 40px rgba(255, 179, 71, 0.6)' },
        },
        breathing: {
          '0%, 100%': { opacity: '0.7', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.05)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(20px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        scaleIn: {
          '0%': { opacity: '0', transform: 'scale(0.98)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
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
      },
    },
  },
  plugins: [],
}
