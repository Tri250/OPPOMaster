/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // OPPO品牌主色
        'oppo-primary': '#FF6B35',
        // 哈苏橙（一级标题用）
        'hasselblad': '#D4A574',
        // OPPO绿
        'oppo-green': '#00C853',
        // 背景色
        'page-bg': '#0F0F0F',
        'card-bg': '#1A1A1A',
        // 文字色
        'text-primary': '#FFFFFF',
        'text-secondary': '#CCCCCC',
        'text-tertiary': '#999999',
      },
      fontFamily: {
        'sans': ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'card': '16px',
        'button': '12px',
        'small': '8px',
      },
      spacing: {
        'page': '16px',
        'component': '12px',
        'inner': '8px',
        'card-y': '24px',
        'card-x': '20px',
        'nav-content': '24px',
        'feature-card': '20px',
        'tool-card': '16px',
      },
    },
  },
  plugins: [],
}
