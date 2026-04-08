// src/pages/ReportsPage.jsx
import { useNavigate } from 'react-router-dom'
import { FileText, Clock, Cpu, ChevronRight, BarChart2 } from 'lucide-react'
import { useRecentReports } from '../hooks/useF1Data.js'
import { LoadingSpinner, ErrorCard, EmptyState } from '../components/ui/StateComponents.jsx'
import { formatReportTime, shortRaceName } from '../utils/helpers.js'

export default function ReportsPage() {
    const navigate = useNavigate()
    const { reports, loading, error, refetch } = useRecentReports()

    if (loading) return <LoadingSpinner message="Loading reports..." />
    if (error)   return (
        <div className="max-w-2xl mx-auto px-4 py-20">
            <ErrorCard message={error} onRetry={refetch} />
        </div>
    )

    return (
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">

            <div className="flex items-center gap-3 mb-8">
                <FileText size={24} className="text-f1-red" />
                <div>
                    <h1 className="text-2xl font-black text-white">Recent Reports</h1>
                    <p className="text-gray-500 text-sm">Last 10 AI-generated race reports</p>
                </div>
            </div>

            {!reports?.length ? (
                <EmptyState
                    icon={FileText}
                    title="No reports yet"
                    description="Generate your first AI race report by selecting a season and race from the home page."
                    action={
                        <button className="btn-primary" onClick={() => navigate('/')}>
                            <BarChart2 size={16} /> Generate a Report
                        </button>
                    }
                />
            ) : (
                <div className="space-y-3">
                    {reports.map(report => (
                        <div
                            key={report.id}
                            className="f1-card cursor-pointer hover:border-f1-red/50 transition-colors duration-200 group"
                            onClick={() => navigate(`/results?season=${report.season}&round=${report.round}`)}
                        >
                            <div className="flex items-center justify-between gap-4">
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-1">
                    <span className="text-f1-red text-xs font-bold border border-f1-red/30 bg-f1-red/10 px-2 py-0.5 rounded">
                      S{report.season} R{report.round}
                    </span>
                                        <h3 className="text-white font-semibold truncate">
                                            {report.raceName || `Season ${report.season} Round ${report.round}`}
                                        </h3>
                                    </div>

                                    <p className="text-gray-400 text-sm line-clamp-2 leading-relaxed">
                                        {report.reportContent?.substring(0, 150)}...
                                    </p>

                                    <div className="flex items-center gap-4 mt-2 text-xs text-gray-600">
                    <span className="flex items-center gap-1">
                      <Clock size={11} />
                        {formatReportTime(report.generatedAt)}
                    </span>
                                        <span className="flex items-center gap-1">
                      <Cpu size={11} />
                                            {report.modelUsed || 'AI'}
                    </span>
                                        {report.winnerName && (
                                            <span>🏆 {report.winnerName}</span>
                                        )}
                                    </div>
                                </div>

                                <ChevronRight
                                    size={18}
                                    className="text-gray-600 group-hover:text-f1-red transition-colors shrink-0"
                                />
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}