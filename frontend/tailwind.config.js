/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#162334',
        muted: '#667587',
        paper: '#f5f7f8',
        brand: {
          50: '#eef7fb',
          100: '#d9edf5',
          500: '#257fa5',
          600: '#1e6888',
          700: '#1b536d',
          900: '#123447',
        },
        accent: '#e5a84b',
      },
      boxShadow: {
        card: '0 12px 40px rgba(22, 35, 52, 0.08)',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['DM Sans', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
