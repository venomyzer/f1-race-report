// src/hooks/useF1Data.js
// ─────────────────────────────────────────────────────────────────────────────
// Custom React hooks for F1 data fetching.
//
// WHY CUSTOM HOOKS?
// Without hooks, every component that needs seasons data would need to
// duplicate: useState(loading), useState(error), useState(data), useEffect...
// Custom hooks extract that pattern ONCE. Components just call:
//   const { seasons, loading, error } = useSeasons()
//
// Real-world analogy: a custom hook is like a pre-assembled IKEA kit.
// Instead of sourcing individual screws, planks, and bolts separately for
// every project, you get a kit with everything pre-packaged and labelled.
//
// All hooks follow the same shape:
//   { data, loading, error, refetch }
// ─────────────────────────────────────────────────────────────────────────────
import { useState, useEffect, useCallback } from 'react'
import {
  fetchSeasons,
  fetchRaces,
  fetchRaceData,
  generateReport,
  fetchRecentReports,
} from '../services/api.js'

// ─── Generic fetch hook factory ──────────────────────────────────────────────
// useAsyncData handles the loading/error/data lifecycle for any async function.
// It's the foundation that all specific hooks are built on.
//
// `enabled` parameter: when false, the fetch doesn't run (used to wait for
// dependent values, e.g., don't fetch races until a season is selected).
function useAsyncData(asyncFn, deps = [], enabled = true) {
  const [data,    setData]    = useState(null)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const execute = useCallback(async () => {
    if (!enabled) return
    setLoading(true)
    setError(null)
    try {
      const result = await asyncFn()
      setData(result)
    } catch (err) {
      const message = err.response?.data?.message || err.message || 'An error occurred'
      setError(message)
    } finally {
      setLoading(false)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, ...deps])

  useEffect(() => {
    execute()
  }, [execute])

  return { data, loading, error, refetch: execute }
}

// ─── useSeasons ───────────────────────────────────────────────────────────────
/**
 * Fetch all F1 seasons on mount.
 * Used by: HomePage (season dropdown)
 *
 * @returns {{ seasons: SeasonDTO[]|null, loading: boolean, error: string|null }}
 */
export function useSeasons() {
  const { data, loading, error, refetch } = useAsyncData(
    () => fetchSeasons(),
    []
  )
  return { seasons: data, loading, error, refetch }
}

// ─── useRaces ─────────────────────────────────────────────────────────────────
/**
 * Fetch races for a season. Does NOT fetch until `season` is set.
 * Used by: HomePage (race dropdown, populated after season is selected)
 *
 * @param {number|null} season - The selected season year
 * @returns {{ races: RaceDTO[]|null, loading: boolean, error: string|null }}
 */
export function useRaces(season) {
  const { data, loading, error, refetch } = useAsyncData(
    () => fetchRaces(season),
    [season],
    !!season   // `enabled` = only fetch when season is truthy (not null/0)
  )
  return { races: data, loading, error, refetch }
}

// ─── useRaceData ──────────────────────────────────────────────────────────────
/**
 * Fetch the full race data payload (results + lap data + standings).
 * Does NOT fetch until both season and round are set.
 * Used by: ResultsPage
 *
 * @param {number|null} season
 * @param {number|null} round
 * @returns {{ raceData: RaceDataDTO|null, loading: boolean, error: string|null, refetch: fn }}
 */
export function useRaceData(season, round) {
  const { data, loading, error, refetch } = useAsyncData(
    () => fetchRaceData(season, round),
    [season, round],
    !!(season && round)
  )
  return { raceData: data, loading, error, refetch }
}

// ─── useGenerateReport ────────────────────────────────────────────────────────
/**
 * Hook for triggering AI report generation manually (not on mount).
 * The `generate` function is called by the user clicking "Generate Report".
 *
 * This is a "lazy" hook – it doesn't fetch automatically.
 * Real-world analogy: like a vending machine. Nothing happens until
 * you press the button (call `generate`).
 *
 * Used by: ResultsPage ("Generate AI Report" button)
 *
 * @returns {{ report, loading, error, generate: (season, round, force) => void }}
 */
export function useGenerateReport() {
  const [report,  setReport]  = useState(null)
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const generate = useCallback(async (season, round, forceRegenerate = false) => {
    setLoading(true)
    setError(null)
    try {
      const result = await generateReport(season, round, forceRegenerate)
      setReport(result)
      return result
    } catch (err) {
      const message = err.response?.data?.message || err.message || 'Report generation failed'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }, [])

  const reset = useCallback(() => {
    setReport(null)
    setError(null)
  }, [])

  return { report, loading, error, generate, reset }
}

// ─── useRecentReports ─────────────────────────────────────────────────────────
/**
 * Fetch the 10 most recently generated reports.
 * Used by: ReportsPage (dashboard widget)
 */
export function useRecentReports() {
  const { data, loading, error, refetch } = useAsyncData(
    () => fetchRecentReports(),
    []
  )
  return { reports: data, loading, error, refetch }
}
