// src/components/ui/StateComponents.jsx
// Reusable loading, error, and empty state components.
// Having these as shared components ensures a consistent look for all
// async data states across every page.
import { AlertCircle, RefreshCw, Inbox, Loader2 } from 'lucide-react'

// ─── LoadingSpinner ───────────────────────────────────────────────────────────
// Full-section loading indicator with F1 red spinner.
export function LoadingSpinner({ message = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-4 animate-fade-in">
      {/* Outer ring */}
      <div className="relative w-14 h-14">
        <div className="absolute inset-0 rounded-full border-4 border-f1-gray" />
        <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-f1-red animate-spin" />
        {/* F1 flag icon in centre */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-4 h-4 bg-f1-red rounded-sm" />
        </div>
      </div>
      <p className="text-gray-400 text-sm font-medium animate-pulse">{message}</p>
    </div>
  )
}

// ─── InlineLoader ─────────────────────────────────────────────────────────────
// Compact inline spinner for buttons and small sections.
export function InlineLoader({ size = 16 }) {
  return <Loader2 size={size} className="animate-spin text-f1-red" />
}

// ─── ErrorCard ────────────────────────────────────────────────────────────────
// Displays an error message with an optional retry button.
export function ErrorCard({ message, onRetry }) {
  return (
    <div className="f1-card border-red-900/50 bg-red-950/20 animate-fade-in">
      <div className="flex items-start gap-3">
        <AlertCircle className="text-f1-red shrink-0 mt-0.5" size={20} />
        <div className="flex-1">
          <p className="text-white font-semibold mb-1">Something went wrong</p>
          <p className="text-gray-400 text-sm leading-relaxed">{message}</p>
        </div>
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-4 btn-secondary text-sm py-2"
        >
          <RefreshCw size={14} />
          Try Again
        </button>
      )}
    </div>
  )
}

// ─── EmptyState ───────────────────────────────────────────────────────────────
// Displayed when a data set is empty (e.g., no reports generated yet).
export function EmptyState({ icon: Icon = Inbox, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center animate-fade-in">
      <div className="w-16 h-16 rounded-full bg-f1-gray/30 flex items-center justify-center mb-4">
        <Icon size={28} className="text-gray-500" />
      </div>
      <h3 className="text-white font-semibold text-lg mb-2">{title}</h3>
      {description && (
        <p className="text-gray-400 text-sm max-w-sm leading-relaxed mb-6">{description}</p>
      )}
      {action}
    </div>
  )
}

// ─── SkeletonCard ─────────────────────────────────────────────────────────────
// Animated placeholder while content loads (better UX than a blank screen).
// Real-world analogy: like the grey box outlines you see on slow-loading
// Facebook posts before the actual content appears.
export function SkeletonCard({ rows = 3 }) {
  return (
    <div className="f1-card animate-pulse">
      <div className="skeleton h-5 w-1/3 mb-4 rounded" />
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex gap-3 mb-3">
          <div className="skeleton h-4 rounded" style={{ width: `${60 + (i % 3) * 20}%` }} />
        </div>
      ))}
    </div>
  )
}

// ─── StatBox ──────────────────────────────────────────────────────────────────
// A labelled statistic box (winner, fastest lap, retirements etc.)
export function StatBox({ label, value, subValue, accent = false }) {
  return (
    <div className={`rounded-xl p-4 border ${
      accent
        ? 'bg-f1-red/10 border-f1-red/30'
        : 'bg-f1-dark border-f1-gray'
    }`}>
      <p className="text-gray-500 text-xs font-semibold uppercase tracking-wider mb-1">
        {label}
      </p>
      <p className={`font-bold text-lg leading-tight ${accent ? 'text-f1-red' : 'text-white'}`}>
        {value || '–'}
      </p>
      {subValue && (
        <p className="text-gray-400 text-xs mt-0.5">{subValue}</p>
      )}
    </div>
  )
}
