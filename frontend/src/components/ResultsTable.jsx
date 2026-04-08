// src/components/ResultsTable.jsx
import { useState } from 'react'
import { ChevronUp, ChevronDown, Minus } from 'lucide-react'
import {
  getTeamColor, getPositionBg, formatStatus,
  formatPoints, getNationalityFlag
} from '../utils/helpers.js'

// ─────────────────────────────────────────────────────────────────────────────
// ResultsTable – full race results grid showing all 20 drivers.
//
// Features:
//   • Colour-coded rows: Gold (P1), Silver (P2), Bronze (P3), red (DNF)
//   • Team colour left-border accent on each row
//   • Grid position change indicator (gained/lost positions)
//   • Purple fastest lap highlight
//   • Responsive: hides less-important columns on mobile
//
// Props:
//   results: DriverResultDTO[] – all driver results sorted by finish position
// ─────────────────────────────────────────────────────────────────────────────

// Position change indicator component
function PositionChange({ grid, finish }) {
  if (!grid || !finish) return <Minus size={12} className="text-gray-600" />
  const diff = grid - finish  // positive = gained positions, negative = lost
  if (diff === 0) return <Minus size={12} className="text-gray-600" />
  if (diff > 0) return (
    <span className="flex items-center gap-0.5 text-green-400 text-xs font-semibold tabular-nums">
      <ChevronUp size={12} />{diff}
    </span>
  )
  return (
    <span className="flex items-center gap-0.5 text-red-400 text-xs font-semibold tabular-nums">
      <ChevronDown size={12} />{Math.abs(diff)}
    </span>
  )
}

export default function ResultsTable({ results }) {
  const [expandedDriver, setExpandedDriver] = useState(null)

  if (!results || results.length === 0) {
    return (
      <div className="f1-card">
        <p className="text-gray-400 text-center py-8">No results available</p>
      </div>
    )
  }

  return (
    <div className="f1-card overflow-hidden p-0">
      {/* Table header */}
      <div className="px-6 py-4 border-b border-f1-gray">
        <h2 className="section-heading border-l-0 pl-0 mb-0">Race Results</h2>
      </div>

      {/* Scrollable table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-f1-darker text-gray-400 text-xs uppercase tracking-wider">
              <th className="px-4 py-3 text-left w-12">Pos</th>
              <th className="px-4 py-3 text-left">Driver</th>
              <th className="px-4 py-3 text-left hidden sm:table-cell">Team</th>
              <th className="px-4 py-3 text-center hidden md:table-cell">Grid</th>
              <th className="px-4 py-3 text-center hidden md:table-cell">Change</th>
              <th className="px-4 py-3 text-center hidden lg:table-cell">Laps</th>
              <th className="px-4 py-3 text-left">Status</th>
              <th className="px-4 py-3 text-right">Points</th>
              <th className="px-4 py-3 text-right hidden lg:table-cell">Fastest Lap</th>
            </tr>
          </thead>
          <tbody>
            {results.map((driver, idx) => {
              const teamColor   = getTeamColor(driver.constructorName)
              const rowBg       = getPositionBg(driver.finishingPosition)
              const status      = formatStatus(driver.status)
              const isExpanded  = expandedDriver === driver.driverId

              return (
                <>
                  <tr
                    key={driver.driverId}
                    className="border-b border-f1-gray/40 table-row-hover cursor-pointer
                               transition-all duration-150 relative"
                    style={{ background: rowBg }}
                    onClick={() => setExpandedDriver(isExpanded ? null : driver.driverId)}
                  >
                    {/* Team colour stripe + Position */}
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {/* Team colour dot */}
                        <div
                          className="w-1 h-6 rounded-full shrink-0"
                          style={{ backgroundColor: teamColor }}
                        />
                        {/* Position number */}
                        <span className={`font-black text-base tabular-nums ${
                          driver.finishingPosition === 1 ? 'text-yellow-400' :
                          driver.finishingPosition === 2 ? 'text-gray-300'   :
                          driver.finishingPosition === 3 ? 'text-orange-400' :
                          !driver.finishingPosition      ? 'text-red-400'    :
                          'text-white'
                        }`}>
                          {driver.finishingPosition || '–'}
                        </span>
                      </div>
                    </td>

                    {/* Driver */}
                    <td className="px-4 py-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-gray-400 font-mono text-xs w-8">
                            {driver.driverCode}
                          </span>
                          <span className="text-white font-semibold">
                            {driver.driverName}
                          </span>
                        </div>
                        <div className="text-gray-500 text-xs mt-0.5 sm:hidden">
                          {driver.constructorName}
                        </div>
                      </div>
                    </td>

                    {/* Team */}
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <span className="text-gray-300 text-xs font-medium">
                        {driver.constructorName}
                      </span>
                    </td>

                    {/* Grid */}
                    <td className="px-4 py-3 text-center hidden md:table-cell">
                      <span className="text-gray-400 tabular-nums">
                        {driver.gridPosition || '–'}
                      </span>
                    </td>

                    {/* Position change */}
                    <td className="px-4 py-3 text-center hidden md:table-cell">
                      <div className="flex justify-center">
                        <PositionChange
                          grid={driver.gridPosition}
                          finish={driver.finishingPosition}
                        />
                      </div>
                    </td>

                    {/* Laps */}
                    <td className="px-4 py-3 text-center hidden lg:table-cell">
                      <span className="text-gray-400 tabular-nums">
                        {driver.lapsCompleted ?? '–'}
                      </span>
                    </td>

                    {/* Status */}
                    <td className="px-4 py-3">
                      <span className={`text-xs font-semibold ${status.colorClass}`}>
                        {status.label}
                      </span>
                    </td>

                    {/* Points */}
                    <td className="px-4 py-3 text-right">
                      <span className="text-white font-bold tabular-nums">
                        {driver.points > 0 ? formatPoints(driver.points) : '–'}
                      </span>
                    </td>

                    {/* Fastest Lap */}
                    <td className="px-4 py-3 text-right hidden lg:table-cell">
                      {driver.hasFastestLap ? (
                        <span className="fastest-lap-badge">
                          ⚡ {driver.fastestLapTime}
                        </span>
                      ) : (
                        <span className="text-gray-600 text-xs font-mono">
                          {driver.fastestLapTime || '–'}
                        </span>
                      )}
                    </td>
                  </tr>

                  {/* Expanded detail row (mobile-friendly extra info) */}
                  {isExpanded && (
                    <tr key={`${driver.driverId}-expanded`}
                        className="bg-f1-darker/80 border-b border-f1-gray/40">
                      <td colSpan={9} className="px-6 py-4">
                        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
                          <div>
                            <p className="text-gray-500 mb-1">Nationality</p>
                            <p className="text-white">
                              {getNationalityFlag(driver.nationality)} {driver.nationality}
                            </p>
                          </div>
                          <div>
                            <p className="text-gray-500 mb-1">Car #</p>
                            <p className="text-white font-mono">#{driver.carNumber}</p>
                          </div>
                          <div>
                            <p className="text-gray-500 mb-1">Fastest Lap</p>
                            <p className={`font-mono ${driver.hasFastestLap ? 'text-purple-400' : 'text-white'}`}>
                              {driver.fastestLapTime || '–'}
                            </p>
                          </div>
                          {driver.fastestLapAvgSpeed && (
                            <div>
                              <p className="text-gray-500 mb-1">Avg Speed</p>
                              <p className="text-white font-mono">
                                {driver.fastestLapAvgSpeed.toFixed(3)} kph
                              </p>
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Table footer legend */}
      <div className="px-6 py-3 border-t border-f1-gray bg-f1-darker/50 flex flex-wrap gap-4 text-xs text-gray-500">
        <span>Click a row for more details</span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-purple-500 inline-block" />
          Purple = Fastest lap
        </span>
        <span className="flex items-center gap-1">
          <ChevronUp size={10} className="text-green-400" />
          Positions gained from grid
        </span>
      </div>
    </div>
  )
}
