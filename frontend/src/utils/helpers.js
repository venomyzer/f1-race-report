// src/utils/helpers.js
// ─────────────────────────────────────────────────────────────────────────────
// Utility functions used across multiple components.
// Keeping pure functions here (no React, no side effects) makes them easy
// to test and reuse.
// ─────────────────────────────────────────────────────────────────────────────

// ── Team Colour Map ───────────────────────────────────────────────────────────
// Maps constructor names to their official team colours.
// Used for chart line colours and driver card accents.
// Real-world analogy: like a brand style guide that says "Red Bull = blue,
// Ferrari = red" so all materials use consistent colours.
const TEAM_COLORS = {
  'Red Bull':       '#3671C6',
  'Ferrari':        '#E8002D',
  'Mercedes':       '#27F4D2',
  'McLaren':        '#FF8000',
  'Aston Martin':   '#358C75',
  'Alpine':         '#FF87BC',
  'Williams':       '#64C4FF',
  'Haas F1 Team':   '#B6BABD',
  'Haas':           '#B6BABD',
  'Kick Sauber':    '#52E252',
  'Sauber':         '#52E252',
  'Alfa Romeo':     '#C92D4B',
  'AlphaTauri':     '#5E8FAA',
  'RB':             '#6692FF',
  'Racing Bulls':   '#6692FF',
  'Toro Rosso':     '#0032FF',
  'Force India':    '#FF80C7',
  'Renault':        '#FFF500',
  'Lotus':          '#FFB800',
  'Brawn':          '#B8FF00',
  'Toyota':         '#CC1E4A',
  'BMW Sauber':     '#9B97C4',
  'Honda':          '#CC1E4A',
  'Jordan':         '#FFFF00',
  // fallback
  'default':        '#6B7280',
}

/**
 * Get the official team colour for a constructor name.
 * Falls back to grey if the team is not in our map.
 *
 * @param {string} constructorName - e.g., "Red Bull", "Ferrari"
 * @returns {string} Hex colour string
 */
export function getTeamColor(constructorName) {
  if (!constructorName) return TEAM_COLORS.default
  // Partial match: "Red Bull Racing" → matches "Red Bull"
  const key = Object.keys(TEAM_COLORS).find(k =>
    constructorName.toLowerCase().includes(k.toLowerCase())
  )
  return key ? TEAM_COLORS[key] : TEAM_COLORS.default
}

/**
 * Get colour for a finishing position (podium highlight colours).
 * @param {number|null} position
 * @returns {string} Tailwind colour class
 */
export function getPositionColor(position) {
  if (!position) return 'text-red-400'   // DNF
  if (position === 1) return 'text-yellow-400'
  if (position === 2) return 'text-gray-300'
  if (position === 3) return 'text-orange-400'
  return 'text-white'
}

/**
 * Get background colour for a position badge (used in results table).
 * @param {number|null} position
 * @returns {string} Inline style hex colour
 */
export function getPositionBg(position) {
  if (!position) return 'rgba(239,68,68,0.15)'       // DNF – red tint
  if (position === 1) return 'rgba(255,215,0,0.15)'  // Gold tint
  if (position === 2) return 'rgba(192,192,192,0.15)'// Silver tint
  if (position === 3) return 'rgba(205,127,50,0.15)' // Bronze tint
  return 'transparent'
}

/**
 * Format a finish status for display.
 * "Finished"  → green
 * "+1 Lap"    → amber (lapped)
 * "DNF"/"DSQ" → red
 *
 * @param {string} status
 * @returns {{ label: string, colorClass: string }}
 */
export function formatStatus(status) {
  if (!status) return { label: '–', colorClass: 'text-gray-500' }
  if (status === 'Finished')        return { label: 'FIN',  colorClass: 'text-green-400' }
  if (status.startsWith('+'))       return { label: status, colorClass: 'text-yellow-500' }
  if (status === 'Disqualified')    return { label: 'DSQ',  colorClass: 'text-red-500' }
  if (status === 'Did not qualify') return { label: 'DNQ',  colorClass: 'text-gray-500' }
  // Everything else (mechanical, accident, etc.) → DNF
  return { label: 'DNF', colorClass: 'text-red-400' }
}

/**
 * Format a race date string to a human-readable format.
 * "2024-03-02" → "2 March 2024"
 *
 * @param {string} dateStr - ISO date string
 * @returns {string}
 */
export function formatRaceDate(dateStr) {
  if (!dateStr) return 'TBC'
  try {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
  } catch {
    return dateStr
  }
}

/**
 * Format a report timestamp to a relative or absolute string.
 * < 24 hours ago → "3 hours ago"
 * >= 24 hours    → "15 Jan 2024"
 *
 * @param {string} isoString
 * @returns {string}
 */
export function formatReportTime(isoString) {
  if (!isoString) return ''
  const date    = new Date(isoString)
  const now     = new Date()
  const diffMs  = now - date
  const diffHrs = diffMs / (1000 * 60 * 60)

  if (diffHrs < 1)  return 'Just now'
  if (diffHrs < 24) return `${Math.floor(diffHrs)} hour${Math.floor(diffHrs) !== 1 ? 's' : ''} ago`
  if (diffHrs < 168) return `${Math.floor(diffHrs / 24)} day${Math.floor(diffHrs / 24) !== 1 ? 's' : ''} ago`
  return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}

/**
 * Truncate a string to a max length, appending "..." if truncated.
 * @param {string} str
 * @param {number} maxLen
 * @returns {string}
 */
export function truncate(str, maxLen = 100) {
  if (!str || str.length <= maxLen) return str
  return str.substring(0, maxLen) + '...'
}

/**
 * Format championship points: always show 1 decimal if not whole number.
 * 25.0 → "25"   |   8.5 → "8.5"
 * @param {number} points
 * @returns {string}
 */
export function formatPoints(points) {
  if (points == null) return '0'
  return Number.isInteger(points) ? String(points) : points.toFixed(1)
}

/**
 * Get a flag emoji for a country/nationality.
 * Used in driver nationality display.
 * @param {string} nationality - e.g., "British", "Dutch", "German"
 * @returns {string} Flag emoji
 */
export function getNationalityFlag(nationality) {
  const flags = {
    'British':    '🇬🇧', 'Dutch':     '🇳🇱', 'German':    '🇩🇪',
    'Spanish':    '🇪🇸', 'Finnish':   '🇫🇮', 'French':    '🇫🇷',
    'Australian': '🇦🇺', 'Canadian':  '🇨🇦', 'Mexican':   '🇲🇽',
    'Monegasque': '🇲🇨', 'Italian':   '🇮🇹', 'Thai':      '🇹🇭',
    'Danish':     '🇩🇰', 'Chinese':   '🇨🇳', 'American':  '🇺🇸',
    'Japanese':   '🇯🇵', 'Brazilian': '🇧🇷', 'Argentine': '🇦🇷',
    'Austrian':   '🇦🇹', 'Belgian':   '🇧🇪', 'New Zealander': '🇳🇿',
    'Polish':     '🇵🇱', 'Russian':   '🇷🇺', 'Swedish':   '🇸🇪',
  }
  return flags[nationality] || '🏁'
}

/**
 * Get a colour for the Recharts line chart based on driver index.
 * Used as fallback when team colour is unknown.
 * @param {number} index
 * @returns {string} Hex colour
 */
export function getChartColor(index) {
  const palette = [
    '#E10600', '#3671C6', '#27F4D2', '#FF8000', '#358C75',
    '#FF87BC', '#64C4FF', '#B6BABD', '#52E252', '#6692FF',
    '#FFD700', '#C0C0C0', '#CD7F32', '#A855F7', '#22C55E',
    '#F59E0B', '#EF4444', '#6366F1', '#EC4899', '#14B8A6',
  ]
  return palette[index % palette.length]
}

/**
 * Shorten a race name for compact display.
 * "Bahrain Grand Prix" → "Bahrain GP"
 * @param {string} raceName
 * @returns {string}
 */
export function shortRaceName(raceName) {
  return raceName?.replace('Grand Prix', 'GP') || raceName || ''
}
