/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        accent: {
          primary: "#00D9FF",
          secondary: "#FF6B00",
          hasselblad: "#FF6B00"
        },
        deep: {
          space: "#0F0F23",
          dark: "#1A1A2E"
        }
      },
      fontFamily: {
        display: ['"Space Grotesk"', '"SF Pro Display"', 'system-ui', 'sans-serif'],
        body: ['"Inter"', '"SF Pro Text"', 'system-ui', 'sans-serif']
      }
    },
  },
  plugins: [],
}

