/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          lavender: '#7966D8',
          'lavender-hover': '#6853C7',
          'lavender-light': '#EDE8FF',
          'lavender-subtle': '#F7F5FF',
          indigo: '#30275F',
          'indigo-dark': '#24213D',
        },
        surface: {
          DEFAULT: 'var(--color-surface)',
          elevated: 'var(--color-surface-elevated)',
          subtle: 'var(--color-surface-subtle)',
          border: 'var(--color-border)',
        },
        text: {
          primary: 'var(--color-text-primary)',
          secondary: 'var(--color-text-secondary)',
          muted: 'var(--color-text-muted)',
        },
        primary: {
          DEFAULT: 'var(--color-primary)',
          hover: 'var(--color-primary-hover)',
          surface: 'var(--color-primary-surface)',
          contrast: 'var(--color-primary-contrast)',
        },
        status: {
          success: 'var(--color-success)',
          'success-surface': 'var(--color-success-surface)',
          warning: 'var(--color-warning)',
          'warning-surface': 'var(--color-warning-surface)',
          error: 'var(--color-error)',
          'error-surface': 'var(--color-error-surface)',
          info: 'var(--color-info)',
          'info-surface': 'var(--color-info-surface)',
        }
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'system-ui', 'sans-serif'],
        display: ['Outfit', 'Plus Jakarta Sans', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        'soft-sm': '0 1px 3px rgba(48, 39, 95, 0.05), 0 1px 2px rgba(48, 39, 95, 0.03)',
        'soft-md': '0 4px 14px rgba(48, 39, 95, 0.07), 0 2px 6px rgba(48, 39, 95, 0.04)',
        'soft-lg': '0 12px 28px rgba(48, 39, 95, 0.09), 0 4px 10px rgba(48, 39, 95, 0.05)',
        'glow-lavender': '0 0 20px rgba(121, 102, 216, 0.25)',
      },
      borderRadius: {
        'xl': '0.875rem',
        '2xl': '1.125rem',
      }
    },
  },
  plugins: [],
};
