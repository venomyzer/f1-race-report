// src/pages/NotFoundPage.jsx
import { useNavigate } from 'react-router-dom'
import { Flag, ArrowLeft } from 'lucide-react'

export default function NotFoundPage() {
    const navigate = useNavigate()
    return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
            <div className="text-8xl font-black text-f1-red mb-4">404</div>
            <Flag size={40} className="text-gray-600 mb-4" />
            <h2 className="text-white text-2xl font-bold mb-2">Page Not Found</h2>
            <p className="text-gray-400 mb-8">This page has retired from the race.</p>
            <button className="btn-primary" onClick={() => navigate('/')}>
                <ArrowLeft size={16} /> Back to Home
            </button>
        </div>
    )
}