import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import './index.css'

// 导入新的App展示页面
import AppShowcase from './pages/AppShowcase'

const HomePage = lazy(() => import('./pages/HomePage'))
const AIDemoPage = lazy(() => import('./pages/AIDemoPage'))
const TechPage = lazy(() => import('./pages/TechPage'))
const AboutPage = lazy(() => import('./pages/AboutPage'))
const PresetDetailPage = lazy(() => import('./pages/PresetDetailPage'))

function Loading() {
  return (
    <div className="min-h-screen bg-deep-space flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-oppo-sunrise-gold/30 border-t-oppo-sunrise-gold rounded-full animate-spin mx-auto mb-4" />
        <p className="text-text-secondary">加载中...</p>
      </div>
    </div>
  )
}

function App() {
  return (
    <Router>
      <Suspense fallback={<Loading />}>
        <Routes>
          {/* 将新的App展示页面设为首页 */}
          <Route path="/" element={<AppShowcase />} />
          <Route path="/app" element={<HomePage />} />
          <Route path="/ai-demo" element={<AIDemoPage />} />
          <Route path="/tech" element={<TechPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/preset/:id" element={<PresetDetailPage />} />
        </Routes>
      </Suspense>
    </Router>
  )
}

export default App
