/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#e4393c',
        'primary-hover': '#c1272d',
        'primary-light': '#fef0f0',
        accent: '#ff6b35',
      },
      borderRadius: {
        card: '12px',
      },
    },
  },
  plugins: [],
}
