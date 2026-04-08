// src/main.jsx
// ─────────────────────────────────────────────────────────────────────────────
// React 18 entry point.
//
// ReactDOM.createRoot() is the React 18 API for mounting the app.
// It replaces the older ReactDOM.render() (React 17 and earlier).
//
// StrictMode: wraps the app in double-render checks in development to surface
// bugs. It has NO effect in production builds.
// Real-world analogy: like a spell-checker that runs while you type (dev),
// but is turned off when the document is published (prod).
//
// Toaster: global toast notification container (from react-hot-toast).
// Any component can call toast("message") and the notification appears here.
// ─────────────────────────────────────────────────────────────────────────────
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Toaster } from 'react-hot-toast'
import App from './App.jsx'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
    {/* Global toast notification system */}
    <Toaster
      position="top-right"
      toastOptions={{
        duration: 4000,
        style: {
          background: '#1E1E2A',
          color: '#fff',
          border: '1px solid #38383F',
          borderRadius: '8px',
          fontSize: '14px',
        },
        success: {
          iconTheme: { primary: '#22C55E', secondary: '#fff' },
        },
        error: {
          iconTheme: { primary: '#E10600', secondary: '#fff' },
        },
      }}
    />
  </StrictMode>,
)
