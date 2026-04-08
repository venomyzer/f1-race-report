// src/components/PodiumCard.jsx
import { Trophy } from 'lucide-react'
import { getTeamColor, formatPoints, getNationalityFlag } from '../utils/helpers.js'

// ─────────────────────────────────────────────────────────────────────────────
// PodiumCard – displays the top 3 finishers in a visual podium layout.
//
// Design: P1 is centered and elevated (tallest podium block).
//         P2 is to the left, P3 to the right, both slightly lower.
//         Each card has the team's official colour as a left border accent.
//
// Props:
//   podium: DriverResultDTO[] – exactly 3 items (P1, P2, P3)
// ─────────────────────────────────────────────────────────────────────────────

const PODIUM_CONFIG = [
  { medal: '🥇', label: 'P1', bg: 'bg-yellow-500/10', border: 'border-yellow-500/50',
    text: 'text-yellow-400', height: 'h-24', order: 2 },
  { medal: '🥈', label: 'P2', bg: 'bg-gray-400/10',   border: 'border-gray-400/50',
    text: 'text-gray-300',   height: 'h-16', order: 1 },
  { medal: '🥉', label: 'P3', bg: 'bg-orange-700/10', border: 'border-orange-700/50',
    text: 'text-orange-400', height: 'h-12', order: 3 },
]

// Displayed order: [P2, P1, P3] so P1 is in the centre
const DISPLAY_INDICES = [1, 0, 2]

export default function PodiumCard({ podium }) {
  if (!podium || podium.length === 0) return null

  return (
    <div className="f1-card">
      {/* Section header */}
      <div className="flex items-center gap-2 mb-6">
        <Trophy size={18} className="text-yellow-400" />
        <h2 className="section-heading border-l-0 pl-0 mb-0">Podium</h2>
      </div>

      {/* Podium display: P2 | P1 | P3 */}
      <div className="flex items-end justify-center gap-3">
        {DISPLAY_INDICES.map((dataIndex) => {
          const driver = podium[dataIndex]
          const config = PODIUM_CONFIG[dataIndex]
          if (!driver) return null

          const teamColor = getTeamColor(driver.constructorName)

          return (
            <div
              key={dataIndex}
              className={`flex-1 max-w-[200px] flex flex-col ${dataIndex === 0 ? 'order-2' : dataIndex === 1 ? 'order-1' : 'order-3'}`}
            >
              {/* Driver info card */}
              <div
                className={`rounded-xl p-4 border ${config.bg} ${config.border}
                            relative overflow-hidden animate-slide-up`}
                style={{ animationDelay: `${dataIndex * 100}ms` }}
              >
                {/* Team colour stripe on left */}
                <div
                  className="absolute left-0 top-0 bottom-0 w-1 rounded-l-xl"
                  style={{ backgroundColor: teamColor }}
                />

                <div className="pl-2">
                  {/* Medal emoji */}
                  <div className="text-2xl mb-1">{config.medal}</div>

                  {/* Driver name */}
                  <p className="text-white font-bold text-sm leading-tight">
                    {driver.driverName}
                  </p>

                  {/* Driver code + nationality */}
                  <p className="text-gray-400 text-xs mt-0.5">
                    {getNationalityFlag(driver.nationality)} {driver.driverCode}
                  </p>

                  {/* Constructor */}
                  <p className="text-xs mt-1 font-medium" style={{ color: teamColor }}>
                    {driver.constructorName}
                  </p>

                  {/* Points */}
                  <div className="mt-2 pt-2 border-t border-white/10">
                    <span className={`text-sm font-bold ${config.text}`}>
                      {formatPoints(driver.points)} pts
                    </span>
                  </div>

                  {/* Finish time */}
                  {driver.finishTime && (
                    <p className="text-gray-500 text-xs mt-1 font-mono">
                      {driver.finishTime}
                    </p>
                  )}

                  {/* Fastest lap badge */}
                  {driver.hasFastestLap && (
                    <div className="fastest-lap-badge mt-2">
                      ⚡ Fastest Lap
                    </div>
                  )}
                </div>
              </div>

              {/* Podium block (visual height represents position) */}
              <div
                className={`${config.height} ${config.bg} border-t-2 ${config.border}
                            rounded-b-lg flex items-center justify-center mt-1`}
              >
                <span className={`font-black text-2xl ${config.text}`}>
                  {config.label}
                </span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
