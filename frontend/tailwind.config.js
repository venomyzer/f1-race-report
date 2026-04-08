/** @type {import('tailwindcss').Config} */

// ─────────────────────────────────────────────────────────────────────────────
// tailwind.config.js
//
// Tailwind CSS works by scanning your JSX/JS files for class names at build
// time, then generating ONLY the CSS for classes you actually use.
//
// The `theme.extend` block adds our custom F1 design system ON TOP OF
// Tailwind's defaults (so we still have blue-500, gray-100, etc.).
//
// Real-world analogy: Tailwind's defaults are a full paint store.
// Our `extend` block adds a custom "F1 Racing" paint collection to the
// existing store – we don't replace the store, we add a new aisle.
// ─────────────────────────────────────────────────────────────────────────────
export default {
  // `content` tells Tailwind which files to scan for class names.
  // Any class not found here gets purged from the production CSS bundle.
  content: [
    './index.html',
    './src/**/*.{js,jsx,ts,tsx}',
  ],

  darkMode: 'class', // Dark mode toggled by adding `dark` class to <html>

  theme: {
    extend: {
      // ── F1 Brand Colors ────────────────────────────────────────────────────
      colors: {
        // Primary brand
        f1: {
          red:    '#E10600',  // Official F1 red (used in logo, header, accents)
          dark:   '#15151E',  // F1 dark background (from official website)
          darker: '#0A0A0F',  // Even darker background for cards
          white:  '#FFFFFF',
          gray:   '#38383F',  // F1 mid-grey (borders, dividers)
          silver: '#C0C0C0',  // P2 silver
        },
        // Podium colours
        podium: {
          gold:   '#FFD700',
          silver: '#C0C0C0',
          bronze: '#CD7F32',
        },
        // Team colours (for driver cards and chart lines)
        team: {
          redbull:    '#3671C6',
          ferrari:    '#E8002D',
          mercedes:   '#27F4D2',
          mclaren:    '#FF8000',
          aston:      '#358C75',
          alpine:     '#FF87BC',
          williams:   '#64C4FF',
          haas:       '#B6BABD',
          sauber:     '#52E252',
          racing:     '#6692FF',
        },
        // Status colours
        status: {
          finished: '#22C55E',  // green
          dnf:      '#EF4444',  // red
          lapped:   '#F59E0B',  // amber
          fastest:  '#A855F7',  // purple (F1 fastest lap convention)
        }
      },

      // ── Typography ─────────────────────────────────────────────────────────
      fontFamily: {
        // F1 uses a custom font; we approximate with system sans-serif
        f1: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },

      // ── Animations ─────────────────────────────────────────────────────────
      animation: {
        'fade-in':    'fadeIn 0.4s ease-in-out',
        'slide-up':   'slideUp 0.4s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'spin-slow':  'spin 3s linear infinite',
      },
      keyframes: {
        fadeIn: {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%':   { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%':   { opacity: '0', transform: 'translateY(-12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },

      // ── Box shadows (glows for F1 dramatic effect) ─────────────────────────
      boxShadow: {
        'f1':       '0 0 20px rgba(225, 6, 0, 0.3)',
        'f1-lg':    '0 0 40px rgba(225, 6, 0, 0.4)',
        'card':     '0 4px 24px rgba(0, 0, 0, 0.4)',
        'card-hover': '0 8px 32px rgba(0, 0, 0, 0.6)',
      },

      // ── Background images ──────────────────────────────────────────────────
      backgroundImage: {
        'f1-gradient':     'linear-gradient(135deg, #15151E 0%, #0A0A0F 100%)',
        'red-gradient':    'linear-gradient(90deg, #E10600 0%, #FF4444 100%)',
        'card-gradient':   'linear-gradient(145deg, #1E1E2A 0%, #15151E 100%)',
        'podium-gradient': 'linear-gradient(180deg, #2A1F00 0%, #15151E 100%)',
      },
    },
  },

  plugins: [],
}
