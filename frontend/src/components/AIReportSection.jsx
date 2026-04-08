// src/components/AIReportSection.jsx
import { Sparkles, RefreshCw, Download, Clock, Cpu } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { formatReportTime } from '../utils/helpers.js'
import { InlineLoader } from './ui/StateComponents.jsx'

export default function AIReportSection({ report, loading, error, onGenerate, onRegenerate, onDownload }) {

    // Loading state
    if (loading) {
        return (
            <div className="f1-card">
                <div className="flex items-center gap-2 mb-4">
                    <Sparkles size={18} className="text-f1-red" />
                    <h2 className="section-heading border-l-0 pl-0 mb-0">AI Race Report</h2>
                </div>
                <div className="flex flex-col items-center justify-center py-16 gap-4">
                    <InlineLoader size={32} />
                    <p className="text-gray-400 text-sm animate-pulse">
                        Groq AI is writing your race report...
                    </p>
                    <p className="text-gray-600 text-xs">This usually takes 5–15 seconds</p>
                </div>
            </div>
        )
    }

    // Not yet generated
    if (!report && !error) {
        return (
            <div className="f1-card border-dashed border-f1-gray/50">
                <div className="flex flex-col items-center justify-center py-12 text-center">
                    <Sparkles size={40} className="text-gray-600 mb-4" />
                    <h3 className="text-white font-semibold text-lg mb-2">AI Race Report</h3>
                    <p className="text-gray-400 text-sm max-w-sm mb-6 leading-relaxed">
                        Generate a journalist-style race report powered by Groq AI based on the race data above.
                    </p>
                    <button className="btn-primary" onClick={onGenerate}>
                        <Sparkles size={16} />
                        Generate AI Report
                    </button>
                </div>
            </div>
        )
    }

    // Error state
    if (error && !report) {
        return (
            <div className="f1-card border-red-900/40 bg-red-950/10">
                <div className="flex items-center gap-2 mb-3">
                    <Sparkles size={18} className="text-f1-red" />
                    <h2 className="section-heading border-l-0 pl-0 mb-0">AI Race Report</h2>
                </div>
                <p className="text-red-400 text-sm mb-4">{error}</p>
                <button className="btn-secondary text-sm" onClick={onGenerate}>
                    <RefreshCw size={14} /> Try Again
                </button>
            </div>
        )
    }

    // Report ready
    return (
        <div className="f1-card">
            {/* Header */}
            <div className="flex items-center justify-between mb-2 flex-wrap gap-3">
                <div className="flex items-center gap-2">
                    <Sparkles size={18} className="text-f1-red" />
                    <h2 className="section-heading border-l-0 pl-0 mb-0">AI Race Report</h2>
                    {report.fromCache && (
                        <span className="text-xs text-gray-500 bg-f1-gray/30 px-2 py-0.5 rounded">cached</span>
                    )}
                </div>

                <div className="flex gap-2">
                    <button className="btn-secondary text-xs py-1.5 px-3" onClick={onRegenerate} disabled={loading}>
                        <RefreshCw size={12} /> Regenerate
                    </button>
                    <button className="btn-primary text-xs py-1.5 px-3" onClick={onDownload}>
                        <Download size={12} /> PDF
                    </button>
                </div>
            </div>

            {/* Metadata */}
            <div className="flex items-center gap-4 text-xs text-gray-600 mb-6 pb-4 border-b border-f1-gray/40">
        <span className="flex items-center gap-1">
          <Cpu size={11} /> {report.modelUsed}
        </span>
                <span className="flex items-center gap-1">
          <Clock size={11} /> {formatReportTime(report.generatedAt)}
        </span>
                <span>{(report.promptTokens || 0) + (report.completionTokens || 0)} tokens</span>
            </div>

            {/* Report content */}
            <div className="report-content prose prose-invert max-w-none">
                <ReactMarkdown>{report.reportContent}</ReactMarkdown>
            </div>
        </div>
    )
}