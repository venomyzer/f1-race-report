// src/pages/HomePage.jsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Flag, ChevronDown, Zap, AlertCircle } from 'lucide-react'
import { useSeasons, useRaces } from '../hooks/useF1Data.js'
import { LoadingSpinner, InlineLoader } from '../components/ui/StateComponents.jsx'
import toast from 'react-hot-toast'

export default function HomePage() {
    const navigate   = useNavigate()
    const [selectedSeason, setSelectedSeason] = useState('')
    const [selectedRound,  setSelectedRound]  = useState('')

    const { seasons, loading: seasonsLoading } = useSeasons()
    const { races,   loading: racesLoading   } = useRaces(selectedSeason ? Number(selectedSeason) : null)

    function handleSeasonChange(e) {
        setSelectedSeason(e.target.value)
        setSelectedRound('')
    }

    function handleGenerate() {
        if (!selectedSeason || !selectedRound) {
            toast.error('Please select both a season and a race')
            return
        }
        navigate(`/results?season=${selectedSeason}&round=${selectedRound}`)
    }

    return (
        <div className="min-h-screen bg-f1-darker">

            {/* Hero */}
            <div className="relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-br from-f1-red/10 via-transparent to-transparent" />
                <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-f1-red via-red-400 to-transparent" />

                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-16 text-center relative">
                    {/* Icon */}
                    <div className="flex justify-center mb-6">
                        <div className="w-20 h-20 rounded-full bg-f1-red/10 border border-f1-red/30 flex items-center justify-center">
                            <Flag size={36} className="text-f1-red" />
                        </div>
                    </div>

                    <h1 className="text-5xl sm:text-6xl font-black text-white mb-4 tracking-tight">
                        F1 Race
                        <span className="text-f1-red"> Report</span>
                    </h1>
                    <p className="text-gray-400 text-lg max-w-xl mx-auto leading-relaxed">
                        Select a season and race to generate an AI-powered journalist report,
                        visualise lap data, and explore full race results.
                    </p>
                </div>
            </div>

            {/* Selector Card */}
            <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 pb-20">
                <div className="f1-card-accent">
                    <h2 className="text-white font-bold text-lg mb-6 flex items-center gap-2">
                        <Zap size={18} className="text-f1-red" />
                        Select a Race
                    </h2>

                    <div className="space-y-4">
                        {/* Season dropdown */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Season
                            </label>
                            <div className="relative">
                                <select
                                    className="f1-select"
                                    value={selectedSeason}
                                    onChange={handleSeasonChange}
                                    disabled={seasonsLoading}
                                >
                                    <option value="">
                                        {seasonsLoading ? 'Loading seasons...' : '— Select a season —'}
                                    </option>
                                    {seasons?.map(s => (
                                        <option key={s.season} value={s.season}>
                                            {s.season} Formula 1 World Championship
                                        </option>
                                    ))}
                                </select>
                                <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                            </div>
                        </div>

                        {/* Race dropdown */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Grand Prix
                            </label>
                            <div className="relative">
                                <select
                                    className="f1-select"
                                    value={selectedRound}
                                    onChange={e => setSelectedRound(e.target.value)}
                                    disabled={!selectedSeason || racesLoading}
                                >
                                    <option value="">
                                        {!selectedSeason
                                            ? '— Select a season first —'
                                            : racesLoading
                                                ? 'Loading races...'
                                                : '— Select a Grand Prix —'}
                                    </option>
                                    {races?.map(r => (
                                        <option key={r.round} value={r.round}>
                                            Round {r.round} – {r.raceName}
                                        </option>
                                    ))}
                                </select>
                                <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                            </div>
                        </div>

                        {/* Generate button */}
                        <button
                            className="btn-primary w-full justify-center mt-2 py-4 text-base"
                            onClick={handleGenerate}
                            disabled={!selectedSeason || !selectedRound}
                        >
                            <Zap size={18} />
                            Generate Race Report
                        </button>
                    </div>
                </div>

                {/* Info cards */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-8">
                    {[
                        { icon: '📊', title: 'Race Results',   desc: 'Full 20-driver results with position changes' },
                        { icon: '📈', title: 'Lap Charts',     desc: 'Position tracker across every lap of the race' },
                        { icon: '🤖', title: 'AI Report',      desc: 'Journalist-style summary powered by Groq AI' },
                    ].map(card => (
                        <div key={card.title} className="f1-card text-center p-4">
                            <div className="text-3xl mb-2">{card.icon}</div>
                            <p className="text-white font-semibold text-sm">{card.title}</p>
                            <p className="text-gray-500 text-xs mt-1 leading-relaxed">{card.desc}</p>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    )
}