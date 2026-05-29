/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // ColorOS 16 主色调 - 日出金
        'oppo-sunrise-gold': '#FFB347',
        'oppo-sunrise-gold-light': '#FFD89A',
        'oppo-sunrise-gold-dark': '#E09420',
        'oppo-sunrise-gold-glow': 'rgba(255, 179, 71, 0.4)',
        
        // 哈苏色彩系统
        'hasselblad': '#D4A574',
        'hasselblad-pro': '#E5A84A',
        'hasselblad-vibrant': '#E8B163',
        'hasselblad-dark': '#9C6D30',
        'hasselblad-gold': '#C9A227',
        
        // OPPO 品牌绿
        'oppo-green': '#2DB47A',
        'oppo-green-light': '#68D391',
        'oppo-green-dark': '#249D6B',
        
        // 辅助色彩 - ColorOS 16 扩展色板
        'ocean-blue': '#3B82F6',
        'ocean-blue-light': '#60A5FA',
        'ocean-blue-dark': '#2563EB',
        'sakura-pink': '#EC4899',
        'sakura-pink-light': '#F472B6',
        'aurora-purple': '#8B5CF6',
        'aurora-purple-light': '#A78BFA',
        'cyber-teal': '#14B8A6',
        'warm-orange': '#FB923C',
        
        // 功能色彩
        'success-vital': '#22C55E',
        'warning-vital': '#F59E0B',
        'error-vital': '#EF4444',
        'info-vital': '#3B82F6',
        
        // 深色主题色彩系统 - ColorOS 16 深空主题
        'deep-space': '#0D0D0D',
        'deep-space-light': '#121212',
        'card-surface': '#141414',
        'card-surface-light': '#1A1A1A',
        'elevated': '#1C1C1E',
        'elevated-light': '#212121',
        
        // 浅色主题
        'light-bg': '#F8F8F8',
        'light-surface': '#FFFFFF',
        'light-card': '#F5F5F5',
        
        // 文字颜色系统
        'text-primary': '#F5F5F5',
        'text-secondary': '#A3A3A3',
        'text-tertiary': '#737373',
        'text-disabled': '#525252',
        'light-text-primary': '#1A1A1A',
        'light-text-secondary': '#6B6B6B',
        'light-text-tertiary': '#A0A0A0',
        
        // 边框颜色
        'oppo-border': '#272727',
        'oppo-border-light': '#404040',
        'oppo-border-gold': 'rgba(255, 179, 71, 0.3)',
        'light-border': '#E5E5E5',
        'light-border-light': '#F0F0F0',
        
        // 毛玻璃效果颜色
        'glass-white': 'rgba(255, 255, 255, 0.08)',
        'glass-white-strong': 'rgba(255, 255, 255, 0.12)',
        'glass-gold': 'rgba(255, 179, 71, 0.1)',
      },
      fontFamily: {
        'sans': ['Inter', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'system-ui', 'sans-serif'],
        'display': ['Inter', 'PingFang SC', 'sans-serif'],
      },
      fontWeight: {
        'medium': 500,
        'semibold': 600,
        'bold': 700,
        'extrabold': 800,
      },
      borderRadius: {
        'oppo': '28px',
        'oppo-lg': '24px',
        'oppo-md': '20px',
        'oppo-sm': '16px',
        'oppo-xs': '12px',
        'oppo-pill': '9999px',
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '96': '24rem',
        '128': '32rem',
      },
      boxShadow: {
        'oppo': '0 4px 24px rgba(255, 179, 71, 0.15)',
        'oppo-hover': '0 8px 32px rgba(255, 179, 71, 0.25)',
        'oppo-glow': '0 0 40px rgba(255, 179, 71, 0.3)',
        'card': '0 2px 8px rgba(0, 0, 0, 0.15)',
        'card-hover': '0 8px 24px rgba(0, 0, 0, 0.25)',
        'glass': '0 8px 32px rgba(0, 0, 0, 0.3)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'float-slow': 'float 8s ease-in-out infinite',
        'float-fast': 'float 4s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'pulse-glow-slow': 'pulse-glow 3s ease-in-out infinite',
        'breathing': 'breathing 1.8s ease-in-out infinite',
        'slide-up': 'slide-up 0.6s ease-out forwards',
        'slide-down': 'slide-down 0.6s ease-out forwards',
        'slide-left': 'slide-left 0.6s ease-out forwards',
        'slide-right': 'slide-right 0.6s ease-out forwards',
        'fade-in': 'fade-in 0.5s ease-out forwards',
        'scale-in': 'scale-in 0.4s ease-out forwards',
        'bounce-in': 'bounce-in 0.6s ease-out forwards',
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
        'slide-up': {
          '0%': { opacity: '0', transform: 'translateY(30px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-down': {
          '0%': { opacity: '0', transform: 'translateY(-30px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-left': {
          '0%': { opacity: '0', transform: 'translateX(30px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        'slide-right': {
          '0%': { opacity: '0', transform: 'translateX(-30px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        'fade-in': {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'scale-in': {
          '0%': { opacity: '0', transform: 'scale(0.9)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
        'bounce-in': {
          '0%': { opacity: '0', transform: 'scale(0.8)' },
          '50%': { transform: 'scale(1.05)' },
          '100%': { opacity: '1', transform: 'scale(1)' },
        },
      },
      backdropBlur: {
        xs: '2px',
        sm: '4px',
        md: '8px',
        lg: '12px',
        xl: '20px',
        '2xl': '40px',
      },
    },
  },
  plugins: [],
}
