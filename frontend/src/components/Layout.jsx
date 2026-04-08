// src/components/Layout.jsx
import { Outlet, NavLink, useLocation } from 'react-router-dom'
import { Flag, BarChart2, FileText, Zap } from 'lucide-react'
import clsx from 'clsx'

// ─────────────────────────────────────────────────────────────────────────────
// Layout – the persistent shell around all pages.
// Contains: top navigation bar + main content area + footer.
//
// <Outlet /> is where React Router renders the matched child route.
// Real-world analogy: the navbar/footer are picture frames that stay fixed.
// <Outlet /> is the picture that changes when you navigate between pages.
// ─────────────────────────────────────────────────────────────────────────────

const navItems = [
  { to: '/',        label: 'Home',    icon: Flag,     exact: true  },
  { to: '/results', label: 'Results', icon: BarChart2, exact: false },
  { to: '/reports', label: 'Reports', icon: FileText,  exact: false },
]

export default function Layout() {
  const location = useLocation()

  return (
    <div className="min-h-screen bg-f1-darker flex flex-col">

      {/* ── Top Navigation Bar ─────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 bg-f1-dark/95 backdrop-blur-md border-b border-f1-gray">
        <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">

            {/* Logo */}
            <NavLink to="/" className="flex items-center gap-3 group">
              {/* F1 red stripe logo mark */}
              <div className="flex gap-1">
                <div className="w-1.5 h-7 bg-f1-red rounded-full" />
                <div className="w-1.5 h-7 bg-f1-red/60 rounded-full" />
                <div className="w-1.5 h-7 bg-f1-red/30 rounded-full" />
              </div>
              <div>
                <span className="text-white font-black text-lg tracking-tight">
                  F1 RACE
                </span>
                <span className="text-f1-red font-black text-lg tracking-tight ml-1">
                  REPORT
                </span>
                <div className="text-gray-500 text-xs font-medium tracking-widest uppercase">
                  AI-Powered Analysis
                </div>
              </div>
            </NavLink>

            {/* Nav Links */}
            <div className="flex items-center gap-1">
              {navItems.map(({ to, label, icon: Icon, exact }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={exact}
                  className={({ isActive }) => clsx(
                    'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200',
                    isActive
                      ? 'bg-f1-red text-white shadow-f1'
                      : 'text-gray-400 hover:text-white hover:bg-white/5'
                  )}
                >
                  <Icon size={15} />
                  <span className="hidden sm:inline">{label}</span>
                </NavLink>
              ))}
            </div>

            {/* Status indicator */}
            <div className="hidden md:flex items-center gap-2 text-xs text-gray-500">
              <Zap size={12} className="text-green-400" />
              <span>Powered by Groq AI</span>
            </div>
          </div>
        </nav>

        {/* Red speed stripe under navbar */}
        <div className="h-0.5 bg-gradient-to-r from-f1-red via-red-500 to-transparent" />
      </header>

      {/* ── Main Content ───────────────────────────────────────────────── */}
      <main className="flex-1 page-enter">
        <Outlet />
      </main>

      {/* ── Footer ─────────────────────────────────────────────────────── */}
      <footer className="bg-f1-dark border-t border-f1-gray mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <p className="text-gray-500 text-sm">
              F1 Race Report Tool · Data from{' '}
              <a
                href="https://api.jolpi.ca"
                target="_blank"
                rel="noopener noreferrer"
                className="text-f1-red hover:underline"
              >
                Jolpica F1 API
              </a>
              {' '}· AI by{' '}
              <a
                href="https://groq.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-f1-red hover:underline"
              >
                Groq
              </a>
            </p>
            <p className="text-gray-600 text-xs">
              Not affiliated with Formula 1 or the FIA
            </p>
          </div>
        </div>
      </footer>
    </div>
  )
}
