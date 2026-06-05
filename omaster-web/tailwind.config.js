/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        hasselblad: {
          50: "#FFF4E5",
          100: "#FFE3BF",
          200: "#FFCB7F",
          300: "#FFB340",
          400: "#FF8C42",
          500: "#FF6B00",
          600: "#CC5600",
          700: "#994000",
          800: "#662B00",
          900: "#331500",
        },
        ink: {
          50: "#F5F1E8",
          100: "#E8E2D2",
          200: "#C7BFA8",
          300: "#9C9382",
          400: "#6B6358",
          500: "#3D3830",
          600: "#2A2620",
          700: "#1C1C1E",
          800: "#141416",
          900: "#0A0A0A",
        },
        amber: {
          DEFAULT: "#D4A574",
        },
      },
      fontFamily: {
        display: ['"Playfair Display"', "Georgia", "serif"],
        sans: ['"Inter"', '"PingFang SC"', '"Microsoft YaHei"', "system-ui", "sans-serif"],
        mono: ['"JetBrains Mono"', '"Fira Code"', "monospace"],
      },
      animation: {
        "fade-in": "fadeIn 0.6s ease-out forwards",
        "fade-up": "fadeUp 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards",
        "scale-in": "scaleIn 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards",
        "shimmer": "shimmer 2.4s linear infinite",
        "marquee": "marquee 30s linear infinite",
        "breathe": "breathe 3s ease-in-out infinite",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        fadeUp: {
          "0%": { opacity: "0", transform: "translateY(24px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        scaleIn: {
          "0%": { opacity: "0", transform: "scale(0.92)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
        shimmer: {
          "0%": { backgroundPosition: "-200% 0" },
          "100%": { backgroundPosition: "200% 0" },
        },
        marquee: {
          "0%": { transform: "translateX(0)" },
          "100%": { transform: "translateX(-50%)" },
        },
        breathe: {
          "0%, 100%": { transform: "scale(1)", opacity: "0.85" },
          "50%": { transform: "scale(1.04)", opacity: "1" },
        },
      },
      backdropBlur: {
        xs: "2px",
      },
    },
  },
  plugins: [],
};
