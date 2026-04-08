// src/services/api.js
// ─────────────────────────────────────────────────────────────────────────────
// Centralised API service layer using Axios.
//
// WHY A CENTRAL API FILE?
// Without it, every component would hardcode `axios.get('http://localhost:8080/...')`.
// Changing the base URL (e.g., to production) would require touching 20+ files.
// With this central file: one change here propagates everywhere.
//
// Real-world analogy: like a company's central switchboard. All calls go
// through one number (apiClient), which routes them to the right department.
// The callers don't need to know the internal extension numbers.
//
// Axios instance features we configure:
//   • baseURL        → prepended to all request URLs
//   • timeout        → abort slow requests after 30 seconds
//   • Content-Type   → JSON for all POST/PUT requests
//   • Interceptors   → middleware that runs on every request/response
// ─────────────────────────────────────────────────────────────────────────────
import axios from 'axios'
import toast from 'react-hot-toast'

// ── Create Axios instance ─────────────────────────────────────────────────────
// Vite injects VITE_API_BASE_URL from .env at build time.
// In development: Vite's proxy forwards /api/* → http://localhost:8080
// In production:  set VITE_API_BASE_URL to your deployed backend URL
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 60000,   // 60s – Groq AI can take up to 20s; PDF generation up to 10s
  headers: {
    'Content-Type': 'application/json',
    'Accept':       'application/json',
  },
})

// ── Request Interceptor ───────────────────────────────────────────────────────
// Runs BEFORE every request is sent. Like a security guard who checks your
// badge before letting you through the door.
// We use it to log requests in development.
apiClient.interceptors.request.use(
  (config) => {
    if (import.meta.env.DEV) {
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`, config.params || '')
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response Interceptor ──────────────────────────────────────────────────────
// Runs AFTER every response is received. Like a post-room clerk who opens
// every parcel and reports anything unexpected before passing it on.
// We use it for:
//   1. Unwrapping the { success, data, message } ApiResponseDTO envelope
//   2. Showing toast notifications for non-fatal errors
//   3. Logging errors in development
apiClient.interceptors.response.use(
  (response) => {
    // All successful responses from our backend are wrapped in ApiResponseDTO.
    // We unwrap them here so callers get `response.data.data` automatically.
    return response
  },
  (error) => {
    const status  = error.response?.status
    const message = error.response?.data?.message || error.message

    if (import.meta.env.DEV) {
      console.error(`[API Error] ${status}: ${message}`, error)
    }

    // Show toast for network errors (not 404s – those are handled per-component)
    if (!error.response) {
      toast.error('Cannot reach the server. Is the backend running?')
    } else if (status === 503) {
      toast.error('AI service is temporarily unavailable. Please try again.')
    } else if (status >= 500) {
      toast.error(`Server error (${status}): ${message}`)
    }

    return Promise.reject(error)
  }
)

// ═════════════════════════════════════════════════════════════════════════════
// API FUNCTIONS
// Each function maps 1:1 to a backend endpoint.
// All functions are async and return the `data` field from ApiResponseDTO.
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Fetch all available F1 seasons (1950–present).
 * Maps to: GET /api/seasons
 * @returns {Promise<SeasonDTO[]>} Array of { season, url }
 */
export async function fetchSeasons() {
  const res = await apiClient.get('/api/seasons')
  return res.data.data   // unwrap ApiResponseDTO.data
}

/**
 * Fetch all races for a given season.
 * Maps to: GET /api/races?season=YYYY
 * @param {number} season - The championship year (e.g., 2024)
 * @returns {Promise<RaceDTO[]>} Array of race objects with round, raceName, etc.
 */
export async function fetchRaces(season) {
  const res = await apiClient.get('/api/races', { params: { season } })
  return res.data.data
}

/**
 * Fetch full race data (results + lap data + standings).
 * Maps to: GET /api/race-data?season=YYYY&round=X
 * @param {number} season
 * @param {number} round
 * @returns {Promise<RaceDataDTO>} Full race data object
 */
export async function fetchRaceData(season, round) {
  const res = await apiClient.get('/api/race-data', { params: { season, round } })
  return res.data.data
}

/**
 * Generate (or retrieve cached) AI race report.
 * Maps to: POST /api/generate-report
 * @param {number} season
 * @param {number} round
 * @param {boolean} forceRegenerate - If true, ignore cached report and regenerate
 * @returns {Promise<ReportResponseDTO>} Report with content, metadata
 */
export async function generateReport(season, round, forceRegenerate = false) {
  const res = await apiClient.post('/api/generate-report', {
    season,
    round,
    forceRegenerate,
  })
  return res.data.data
}

/**
 * Fetch the 10 most recently generated reports.
 * Maps to: GET /api/reports
 * @returns {Promise<ReportResponseDTO[]>}
 */
export async function fetchRecentReports() {
  const res = await apiClient.get('/api/reports')
  return res.data.data
}

/**
 * Fetch all reports for a specific season.
 * Maps to: GET /api/reports/season/{season}
 * @param {number} season
 * @returns {Promise<ReportResponseDTO[]>}
 */
export async function fetchReportsBySeason(season) {
  const res = await apiClient.get(`/api/reports/season/${season}`)
  return res.data.data
}

/**
 * Download a race report as a PDF.
 * Maps to: GET /api/export-pdf/{id}
 *
 * Note: This returns a Blob (binary data), not JSON.
 * We trigger a browser file download by creating a temporary <a> element.
 * Real-world analogy: like clicking "Save as PDF" in a web app – the browser
 * receives binary bytes and saves them to the user's Downloads folder.
 *
 * @param {number} reportId - The DB primary key of the report
 * @param {string} filename - Suggested filename for the download
 */
export async function downloadPdf(reportId, filename = 'f1-race-report.pdf') {
  const response = await apiClient.get(`/api/export-pdf/${reportId}`, {
    responseType: 'blob',     // tells Axios to treat response as binary Blob
    timeout: 30000,           // PDF generation can take up to 10s
  })

  // Create a temporary download link and trigger it
  const url  = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href  = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()

  // Cleanup: revoke the object URL and remove the element
  link.parentNode.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default apiClient
