/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌色
        'oppo-coral': '#FF6B5B',
        'oppo-coral-light': '#FF8A7A',
        'oppo-coral-dark': '#E85A4A',
        
        // 哈苏品牌色
        'hasselblad': '#C9A86C',
        'hasselblad-light': '#D4B87A',
        'hasselblad-dark': '#B89858',
        'hasselblad-gold': '#C9A86C',
        
        // Aqua Design主色
        'aqua-primary': '#007AFF',
        'aqua-primary-light': '#3395FF',
        'aqua-primary-dark': '#0062D6',
        
        // OPPO功能色
        'oppo-green': '#00C853',
        'oppo-yellow': '#FFB300',
        'oppo-red': '#FF3D71',
        'oppo-blue': '#2196F3',
        
        // 深色主题表面色
        'surface-900': '#000000',
        'surface-800': '#0A0A0F',
        'surface-700': '#121218',
        'surface-600': '#1A1A24',
        'surface-500': '#242432',
        'surface-400': '#2E2E3D',
        'surface-300': '#404050',
        'surface-200': '#6B6B80',
        'surface-100': '#9090A5',
        'surface-50': '#C8C8D8',
        
        // 深空主题
        'deep-space': '#0A0A0F',
        'deep-space-light': '#1A1A24',
        
        // 摄影主题渐变色终点
        'night-sky': '#0D1B2A',
        'blue-hour': '#1B3A4B',
      },
      fontFamily: {
        'sans': ['OPPO Sans', 'PingFang SC', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        'display': ['SF Pro Display', 'DIN Pro', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        'mono': ['SF Mono', 'Fira Code', 'JetBrains Mono', 'monospace'],
      },
      fontSize: {
        'display-xl': ['3.75rem', { lineHeight: '1.1', letterSpacing: '-0.025em', fontWeight: '700' }],
        'display-lg': ['3rem', { lineHeight: '1.15', letterSpacing: '-0.02em', fontWeight: '700' }],
        'display-md': ['2.25rem', { lineHeight: '1.2', letterSpacing: '-0.015em', fontWeight: '700' }],
        'headline-xl': ['1.875rem', { lineHeight: '1.3', letterSpacing: '-0.01em', fontWeight: '600' }],
        'headline-lg': ['1.5rem', { lineHeight: '1.35', letterSpacing: '-0.008em', fontWeight: '600' }],
        'headline-md': ['1.25rem', { lineHeight: '1.4', fontWeight: '600' }],
        'title-xl': ['1.125rem', { lineHeight: '1.45', fontWeight: '600' }],
        'title-lg': ['1rem', { lineHeight: '1.5', fontWeight: '600' }],
        'title-md': ['0.9375rem', { lineHeight: '1.5', fontWeight: '500' }],
        'body-xl': ['1rem', { lineHeight: '1.6', fontWeight: '400' }],
        'body-lg': ['0.9375rem', { lineHeight: '1.6', fontWeight: '400' }],
        'body-md': ['0.875rem', { lineHeight: '1.6', fontWeight: '400' }],
        'body-sm': ['0.8125rem', { lineHeight: '1.5', fontWeight: '400' }],
        'caption-xl': ['0.75rem', { lineHeight: '1.5', letterSpacing: '0.01em', fontWeight: '500' }],
        'caption-lg': ['0.6875rem', { lineHeight: '1.4', letterSpacing: '0.02em', fontWeight: '500' }],
      },
      spacing: {
        'space-1': '4px',
        'space-2': '8px',
        'space-3': '12px',
        'space-4': '16px',
        'space-5': '20px',
        'space-6': '24px',
        'space-8': '32px',
        'space-10': '40px',
        'space-12': '48px',
        'space-16': '64px',
        'space-20': '80px',
        'space-24': '96px',
      },
      borderRadius: {
        'radius-xs': '4px',
        'radius-sm': '8px',
        'radius-md': '12px',
        'radius-lg': '16px',
        'radius-xl': '20px',
        'radius-2xl': '24px',
        'radius-3xl': '28px',
      },
      boxShadow: {
        'glow-gold': '0 0 20px rgba(201, 168, 108, 0.4), 0 0 40px rgba(201, 168, 108, 0.2)',
        'glow-blue': '0 0 20px rgba(0, 122, 255, 0.4), 0 0 40px rgba(0, 122, 255, 0.2)',
        'glow-coral': '0 0 20px rgba(255, 107, 91, 0.4), 0 0 40px rgba(255, 107, 91, 0.2)',
        'card-hover': '0 8px 32px rgba(201, 168, 108, 0.2)',
      },
      animation: {
        'ripple': 'ripple 0.6s ease-out',
        'wave-expand': 'wave-expand 1.2s ease-out infinite',
        'glow-pulse': 'glow-pulse 2s ease-in-out infinite',
        'float': 'float 6s ease-in-out infinite',
        'pulse-glow': 'pulse-glow 2s ease-in-out infinite',
        'shimmer': 'shimmer 1.5s infinite',
        'slide-up': 'slide-up 0.3s ease-out',
        'slide-down': 'slide-down 0.3s ease-out',
        'fade-in': 'fade-in 0.2s ease-out',
        'scale-in': 'scale-in 0.25s ease-out',
        'bounce-soft': 'bounce-soft 2s ease-in-out infinite',
      },
      keyframes: {
        ripple: {
          '0%': { transform: 'scale(0)', opacity: '0.6' },
          '100%': { transform: 'scale(4)', opacity: '0' },
        },
        'wave-expand': {
          '0%': { transform: 'scale(0.8)', opacity: '0.8' },
          '100%': { transform: 'scale(1.5)', opacity: '0' },
        },
        'glow-pulse': {
          '0%, 100%': { boxShadow: '0 0 20px rgba(201, 168, 108, 0.3), 0 0 40px rgba(201, 168, 108, 0.2)' },
          '50%': { boxShadow: '0 0 30px rgba(201, 168, 108, 0.5), 0 0 60px rgba(201, 168, 108, 0.3)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-20px)' },
        },
        'pulse-glow': {
          '0%, 100%': { boxShadow: '0 0 20px rgba(212, 165, 116, 0.3)' },
          '50%': { boxShadow: '0 0 40px rgba(212, 165, 116, 0.6)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        'slide-up': {
          from: { opacity: '0', transform: 'translateY(20px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'slide-down': {
          from: { opacity: '0', transform: 'translateY(-20px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'scale(0.9)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
        'bounce-soft': {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-5px)' },
        },
      },
      transitionTimingFunction: {
        'ease-standard': 'cubic-bezier(0.4, 0, 0.2, 1)',
        'ease-enter': 'cubic-bezier(0, 0, 0.2, 1)',
        'ease-exit': 'cubic-bezier(0.4, 0, 1, 1)',
        'ease-bounce': 'cubic-bezier(0.34, 1.56, 0.64, 1)',
      },
      transitionDuration: {
        'duration-instant': '100ms',
        'duration-fast': '150ms',
        'duration-normal': '200ms',
        'duration-slow': '300ms',
        'duration-page': '400ms',
      },
    },
  },
  plugins: [],
}
