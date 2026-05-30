/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌色彩
        oppo: {
          coral: '#FF6B35',
          coralLight: '#FF8A5C',
          green: '#2DB47A',
          greenLight: '#45C08A',
        },
        // 哈苏品牌色彩
        hasselblad: {
          DEFAULT: '#D4A574',
          orange: '#D4A574',
          gold: '#D4A574',
        },
        // ColorOS 16深色主题表面色彩
        surface: {
          50: '#F2F2F7',
          100: '#E5E5EA',
          200: '#C7C7CC',
          300: '#AEAEB2',
          400: '#8E8E93',
          500: '#636366',
          600: '#48484A',
          700: '#2C2C2E',
          800: '#1C1C1E',
          900: '#0F0F0F',
        },
        // 深空主题背景色 - 新增简写别名
        'deep-space': '#0F0F0F',
        'deep-spaceLight': '#1A1A1A',
        // 功能状态色彩
        status: {
          success: '#2DB47A',
          warning: '#FFB020',
          error: '#FF4D4F',
          info: '#007AFF',
        },
        // 中性色
        neutral: {
          100: '#FFFFFF',
          90: '#E5E5E5',
          80: '#CCCCCC',
          70: '#B3B3B3',
          60: '#666666',
          50: '#333333',
        },
      },
      fontFamily: {
        sans: ['"OPPO Sans 3.0"', 'Inter', 'Roboto', 'sans-serif'],
        serif: ['"OPPO Serif"', 'Georgia', 'serif'],
        mono: ['"JetBrains Mono"', 'Consolas', 'monospace'],
      },
      fontSize: {
        // ColorOS 16标准字体层级
        'display-xl': ['40px', { lineHeight: '1.2', fontWeight: '700', letterSpacing: '-0.5px' }],
        'display-lg': ['28px', { lineHeight: '1.3', fontWeight: '600', letterSpacing: '-0.3px' }],
        'display-md': ['22px', { lineHeight: '1.4', fontWeight: '600', letterSpacing: '-0.2px' }],
        'display-sm': ['18px', { lineHeight: '1.4', fontWeight: '500', letterSpacing: '0px' }],
        'body-lg': ['16px', { lineHeight: '1.5', fontWeight: '400', letterSpacing: '0px' }],
        'body-md': ['14px', { lineHeight: '1.5', fontWeight: '400', letterSpacing: '0.1px' }],
        'caption': ['12px', { lineHeight: '1.6', fontWeight: '300', letterSpacing: '0.2px' }],
      },
      borderRadius: {
        // ColorOS 16超椭圆圆角系统
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
        'xl': '20px',
        '2xl': '24px',
      },
      boxShadow: {
        // ColorOS 16阴影系统
        'card': '0px 8px 32px rgba(0, 0, 0, 0.25)',
        'card-hover': '0px 12px 48px rgba(0, 0, 0, 0.32)',
        'glow-coral': '0px 0px 24px rgba(255, 107, 53, 0.3)',
        'glow-orange': '0px 0px 24px rgba(212, 165, 116, 0.3)',
      },
      backdropBlur: {
        // ColorOS 16毛玻璃模糊度
        'sm': '8px',
        'md': '12px',
        'lg': '16px',
      },
      animation: {
        // ColorOS 16动画
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)',
        'pulse-glow': 'pulseGlow 2s ease-in-out infinite',
        'float': 'float 6s ease-in-out infinite',
        'ripple': 'ripple 0.6s ease-out forwards',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(30px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        pulseGlow: {
          '0%, 100%': { boxShadow: '0 0 8px rgba(212, 165, 116, 0.3)' },
          '50%': { boxShadow: '0 0 16px rgba(212, 165, 116, 0.5)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        ripple: {
          '0%': { transform: 'scale(0)', opacity: '0.6' },
          '100%': { transform: 'scale(4)', opacity: '0' },
        },
      },
    },
  },
  plugins: [],
}
