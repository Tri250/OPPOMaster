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
        
        // 深色主题
        'deep-space': '#0D0D0D',
        'card-surface': '#141414',
        'elevated': '#1C1C1E',
        
        // 浅色主题
        'light-bg': '#F8F8F8',
        'light-surface': '#FFFFFF',
        
        // 文字颜色
        'text-primary': '#F5F5F5',
        'text-secondary': '#A3A3A3',
        'text-tertiary': '#737373',
        'light-text-primary': '#1A1A1A',
        'light-text-secondary': '#6B6B6B',
        
        // 边框
        'oppo-border': '#272727',
        'oppo-border-light': '#404040',
        'light-border': '#E5E5E5',
      },
      fontFamily: {
        'sans': ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'oppo': '28px',
        'oppo-lg': '24px',
        'oppo-md': '20px',
        'oppo-sm': '16px',
      },
      boxShadow: {
        'oppo': '0 4px 24px rgba(255, 179, 71, 0.15)',
        'oppo-hover': '0 8px 32px rgba(255, 179, 71, 0.25)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'breathing': 'breathing 1.8s ease-in-out infinite',
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
      },
    },
  },
  plugins: [],
}
