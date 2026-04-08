// src/App.jsx
// ─────────────────────────────────────────────────────────────────────────────
// Root application component.
//
// React Router v6 concepts:
//   BrowserRouter:  uses the History API (pushState) for URL navigation.
//                   Real-world analogy: a GPS that reads real street addresses.
//
//   Routes:         the routing table – maps URL paths → components.
//
//   Route:          a single mapping: path="/results" → <ResultsPage />
//                   When the URL matches `path`, the `element` renders.
//
//   Outlet:         in a layout route, renders the matched child route.
//                   Like a picture frame – the frame (Layout) stays constant,
//                   the picture (page content) swaps based on the URL.
//
// Layout pattern:
//   "/" wraps everything in <Layout> (navbar + footer).
//   Child routes render inside the <Outlet /> inside Layout.
//   This means navbar/footer are always visible, only the page content changes.
// ─────────────────────────────────────────────────────────────────────────────
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import HomePage from './pages/HomePage.jsx'
import ResultsPage from './pages/ResultsPage.jsx'
import ReportsPage from './pages/ReportsPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/*
          Layout route: the "/" path wraps all children in <Layout>.
          No `element` children inside Layout render by default.
          The `index` route handles the "/" URL exactly.
        */}
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="results"  element={<ResultsPage />} />
          <Route path="reports"  element={<ReportsPage />} />
          <Route path="*"        element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
