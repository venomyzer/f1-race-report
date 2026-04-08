// src/components/StandingsChart.jsx
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid,
    Tooltip, ResponsiveContainer, Cell
} from 'recharts'
import { Trophy } from 'lucide-react'
import { getTeamColor } from '../utils/helpers.js'

function CustomTooltip({ active, payload, label }) {
    if (!active || !payload?.length) return null
    const d = payload[0].payload
    return (
        <div className="bg-f1-dark border border-f1-gray rounded-xl p-3 shadow-card min-w-[160px]">
            <p className="text-white font-bold text-sm mb-1">{d.driverName}</p>
            <p className="text-gray-400 text-xs">{d.constructorName}</p>
            <p className="text-f1-red font-bold text-lg mt-1">{d.points} pts</p>
            {d.wins > 0 && <p className="text-yellow-400 text-xs">{d.wins} win{d.wins !== 1 ? 's' : ''}</p>}
        </div>
    )
}

export default function StandingsChart({ standings, season, round }) {
    if (!standings?.length) return null

    const top10 = standings.slice(0, 10)

    return (
        <div className="f1-card">
            <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-2">
                    <Trophy size={18} className="text-yellow-400" />
                    <h2 className="section-heading border-l-0 pl-0 mb-0">Championship Standings</h2>
                </div>
                <span className="text-gray-500 text-xs">After Round {round} · Top 10</span>
            </div>

            <ResponsiveContainer width="100%" height={320}>
                <BarChart data={top10} margin={{ top: 5, right: 10, left: 0, bottom: 60 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(56,56,63,0.5)" vertical={false} />
                    <XAxis
                        dataKey="driverCode"
                        tick={{ fill: '#9CA3AF', fontSize: 11 }}
                        tickLine={false}
                        axisLine={{ stroke: '#38383F' }}
                        angle={-45}
                        textAnchor="end"
                        interval={0}
                    />
                    <YAxis
                        tick={{ fill: '#9CA3AF', fontSize: 11 }}
                        tickLine={false}
                        axisLine={false}
                        width={40}
                    />
                    <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(255,255,255,0.05)' }} />
                    <Bar dataKey="points" radius={[4, 4, 0, 0]}>
                        {top10.map((entry, index) => (
                            <Cell
                                key={entry.driverId}
                                fill={index === 0 ? '#FFD700' : getTeamColor(entry.constructorName)}
                            />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    )
}