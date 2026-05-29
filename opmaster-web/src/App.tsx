import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import './index.css'

const HomePage = lazy(() => import('./pages/HomePage'))
const AIDemoPage = lazy(() => import('./pages/AIDemoPage'))
const TechPage = lazy(() => import('./pages/TechPage'))
const AboutPage = lazy(() => import('./pages/AboutPage'))
const PresetDetailPage = lazy(() => import('./pages/PresetDetailPage'))
const WatermarkPage = lazy(() => import('./pages/WatermarkPage'))
const PresetEditorPage = lazy(() => import('./pages/PresetEditorPage'))
const SettingsPage = lazy(() => import('./pages/SettingsPage'))
const AiFineTunePage = lazy(() => import('./pages/AiFineTunePage'))
const SceneDetectionPage = lazy(() => import('./pages/SceneDetectionPage'))
const FloatingWindowPage = lazy(() => import('./pages/FloatingWindowPage'))
const LutManagerPage = lazy(() => import('./pages/LutManagerPage'))
const CloudSyncPage = lazy(() => import('./pages/CloudSyncPage'))
const OcrDemoPage = lazy(() => import('./pages/OcrDemoPage'))
const FilterLibraryPage = lazy(() => import('./pages/FilterLibraryPage'))
const MasterParamsPage = lazy(() => import('./pages/MasterParamsPage'))
const P0Overview = lazy(() => import('./pages/P0Overview'))
const XiaoOHelpPage = lazy(() => import('./pages/XiaoOHelpPage'))
const NativeCameraPage = lazy(() => import('./pages/NativeCameraPage'))
const PresetEcosystemPage = lazy(() => import('./pages/PresetEcosystemPage'))

function Loading() {
  return (
    <div className="min-h-screen bg-oppo-black flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-oppo-orange/30 border-t-oppo-orange rounded-full animate-spin mx-auto mb-4" />
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
          <Route path="/" element={<HomePage />} />
          <Route path="/p0-overview" element={<P0Overview />} />
          <Route path="/app" element={<HomePage />} />
          <Route path="/ai-demo" element={<AIDemoPage />} />
          <Route path="/tech" element={<TechPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/preset/:id" element={<PresetDetailPage />} />
          <Route path="/watermark" element={<WatermarkPage />} />
          <Route path="/editor" element={<PresetEditorPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/ai-finetune" element={<AiFineTunePage />} />
          <Route path="/scene-detection" element={<SceneDetectionPage />} />
          <Route path="/floating-window" element={<FloatingWindowPage />} />
          <Route path="/lut-manager" element={<LutManagerPage />} />
          <Route path="/cloud-sync" element={<CloudSyncPage />} />
          <Route path="/ocr-demo" element={<OcrDemoPage />} />
          <Route path="/filter-library" element={<FilterLibraryPage />} />
          <Route path="/master-params" element={<MasterParamsPage />} />
          <Route path="/xiao-o-help" element={<XiaoOHelpPage />} />
          <Route path="/native-camera" element={<NativeCameraPage />} />
          <Route path="/preset-ecosystem" element={<PresetEcosystemPage />} />
        </Routes>
      </Suspense>
    </Router>
  )
}

export default App
