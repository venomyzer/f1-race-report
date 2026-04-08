// src/components/PositionChart.jsx
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer
} from 'recharts'
import { Activity } from 'lucide-react'
import { getTeamColor, getChartColor } from '../utils/helpers.js'

// ─────────────────────────────────────────────────────────────────────────────
// PositionChart – Recharts LineChart showing position per lap for each driver.
//
// Recharts concepts:
//   ResponsiveContainer: makes the chart fill its parent's width (responsive).
//   LineChart:           the chart container; takes `data` as the array of
//                        { lap: 1, VER: 1, HAM: 3, LEC: 2, ... } objects.
//   Line:                one line per driver; `dataKey` maps to the driver
//                        code field in each data object.
//   XAxis/YAxis:         axes configuration.
//   CartesianGrid:       background grid lines.
//   Tooltip:             hover popup showing values at a lap.
//   Legend:              driver name labels.
//
// Y-axis is INVERTED (domain reversed) because position 1 should be at TOP.
// Without inversion, P1=1 would appear at the bottom (lower number = lower).
//
// Real-world analogy: the chart is like watching an F1 broadcast timing
// tower that shows every driver's position throughout the race, animated.
//
// Props:
//   lapData: LapDataDTO – { lapPoints, driverCodes, driverNames, driverConstructors, totalLaps }
//   maxDrivers: number  – how many drivers to show (default 10 for readability)
// ─────────────────────────────────────────────────────────────────────────────

// Custom Tooltip component for dark theme
function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null

  // Sort by position (ascending) for the tooltip
  const sorted = [...payload].sort((a, b) => (a.value || 99) - (b.value || 99))

  return (
    <div className="bg-f1-dark border border-f1-gray rounded-xl p-3 shadow-card min-w-[160px]">
      <p className="text-gray-400 text-xs font-semibold mb-2 border-b border-f1-gray pb-1">
        Lap {label}
      </p>
      {sorted.slice(0, 8).map((entry) => (
        <div key={entry.dataKey} className="flex items-center justify-between gap-4 py-0.5">
          <div className="flex items-center gap-1.5">
            <div className="w-2 h-2 rounded-full" style={{ backgroundColor: entry.color }} />
            <span className="text-gray-300 text-xs font-mono">{entry.dataKey}</span>
          </div>
          <span className="text-white text-xs font-bold tabular-nums">
            P{entry.value}
          </span>
        </div>
      ))}
    </div>
  )
}

export default function PositionChart({ lapData, maxDrivers = 10 }) {
  if (!lapData?.lapDataAvailable || !lapData?.lapPoints?.length) {
    return (
      <div className="f1-card">
        <div className="flex items-center gap-2 mb-4">
          <Activity size={18} className="text-f1-red" />
          <h2 className="section-heading border-l-0 pl-0 mb-0">Position Tracker</h2>
        </div>
        <div className="flex items-center justify-center py-12 text-center">
          <div>
            <Activity size={40} className="text-gray-600 mx-auto mb-3" />
            <p className="text-gray-400 font-medium">Lap data not available</p>
            <p className="text-gray-600 text-sm mt-1">
              Detailed lap timing is not available for this race
            </p>
          </div>
        </div>
      </div>
    )
  }

  // Limit to top N drivers by their best position (to keep chart readable)
  // We take the first `maxDrivers` codes, which are already ordered by finish
  const displayDrivers = (lapData.driverCodes || []).slice(0, maxDrivers)

  return (
    <div className="f1-card">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Activity size={18} className="text-f1-red" />
          <h2 className="section-heading border-l-0 pl-0 mb-0">Position Tracker</h2>
        </div>
        <span className="text-gray-500 text-xs">
          Top {displayDrivers.length} drivers · {lapData.totalLaps} laps
        </span>
      </div>

      <ResponsiveContainer width="100%" height={380}>
        <LineChart
          data={lapData.lapPoints}
          margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
        >
          {/* Background grid */}
          <CartesianGrid
            strokeDasharray="3 3"
            stroke="rgba(56,56,63,0.5)"
            vertical={false}
          />

          {/* X axis: lap numbers */}
          <XAxis
            dataKey="lap"
            tick={{ fill: '#9CA3AF', fontSize: 11 }}
            tickLine={false}
            axisLine={{ stroke: '#38383F' }}
            label={{
              value: 'Lap',
              position: 'insideBottom',
              offset: -2,
              fill: '#6B7280',
              fontSize: 11
            }}
          />

          {/* Y axis: position (inverted so P1 is at top) */}
          <YAxis
            reversed={true}          // ← KEY: P1 at top of chart
            domain={[1, 20]}
            ticks={[1, 5, 10, 15, 20]}
            tick={{ fill: '#9CA3AF', fontSize: 11 }}
            tickLine={false}
            axisLine={false}
            tickFormatter={(v) => `P${v}`}
            width={35}
          />

          {/* Hover tooltip */}
          <Tooltip content={<CustomTooltip />} />

          {/* Legend */}
          <Legend
            wrapperStyle={{ paddingTop: '12px', fontSize: '11px' }}
            formatter={(value) => {
              const fullName = lapData.driverNames?.[value] || value
              // Show "VER – Max Verstappen" style
              return (
                <span style={{ color: '#D1D5DB', fontSize: '10px' }}>
                  {value}
                </span>
              )
            }}
          />

          {/* One Line per driver */}
          {displayDrivers.map((driverId, index) => {
            // Try to get team colour; fall back to chart palette
            const constructorName = lapData.driverConstructors?.[driverId]
            const color = constructorName
              ? getTeamColor(constructorName)
              : getChartColor(index)

            return (
              <Line
                key={driverId}
                type="monotone"
                dataKey={driverId}
                stroke={color}
                strokeWidth={2}
                dot={false}           // no dots (too many = cluttered)
                activeDot={{          // dot appears only on hover
                  r: 5,
                  stroke: color,
                  strokeWidth: 2,
                  fill: '#15151E'
                }}
                connectNulls={true}   // connect across gaps (pit laps, etc.)
              />
            )
          })}
        </LineChart>
      </ResponsiveContainer>

      <p className="text-gray-600 text-xs text-center mt-2">
        Y-axis inverted: P1 (leader) shown at top · Showing top {displayDrivers.length} finishers
      </p>
    </div>
  )
}
