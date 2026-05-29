/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO Brand Colors
        'oppo-primary': '#FF6700',
        'oppo-orange': '#FF6700',
        'oppo-gold': '#FFB347',
        'hasselblad': '#D4A574',
        
        // OPPO Green
        'oppo-green': '#2DB47A',
        'oppo-green-light': '#68D391',
        'oppo-green-dark': '#1E8E5A',
        
        // Deep Space
        'deep-space': '#0D0D0D',
        'deep-space-100': '#141414',
        'deep-space-200': '#1A1A1A',
        'deep-space-300': '#262626',
        'deep-space-400': '#333333',
        
        // Surface
        'surface': '#141414',
        'surface-elevated': '#1A1A1A',
        'surface-hover': '#202020',
        
        // Text
        'text-primary': '#FFFFFF',
        'text-secondary': '#A3A3A3',
        'text-tertiary': '#737373',
        'text-disabled': '#525252',
        
        // Border
        'border-subtle': '#262626',
        'border-light': '#333333',
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
        'oppo': '0 4px 24px rgba(255, 103, 0, 0.15)',
        'oppo-hover': '0 8px 32px rgba(255, 103, 0, 0.25)',
        'card': '0 2px 12px rgba(0, 0, 0, 0.4)',
      },
      animation: {
        'float': 'float 6s ease-in-out infinite',
        'breathing': 'breathing 2.5s ease-in-out infinite',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        breathing: {
          '0%, 100%': { opacity: '0.8', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.02)' },
        },
      },
    },
  },
  plugins: [],
}
