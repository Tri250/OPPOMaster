import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import NavigationBar from './components/common/NavigationBar';
import './index.css';

const HomePage = lazy(() => import('./pages/HomePage'));
const AIDemoPage = lazy(() => import('./pages/AIDemoPage'));
const TechPage = lazy(() => import('./pages/TechPage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const PresetDetailPage = lazy(() => import('./pages/PresetDetailPage'));
const WatermarkPage = lazy(() => import('./pages/WatermarkPage'));
const PresetEditorPage = lazy(() => import('./pages/PresetEditorPage'));

function Loading() {
  return (
    <div className="min-h-screen bg-page-bg flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-oppo-primary/30 border-t-oppo-primary rounded-full animate-spin mx-auto mb-4" />
        <p className="text-text-tertiary">加载中...</p>
      </div>
    </div>
  );
}

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-page-bg">
        <NavigationBar />
        <Suspense fallback={<Loading />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/ai-demo" element={<AIDemoPage />} />
            <Route path="/tech" element={<TechPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/preset/:id" element={<PresetDetailPage />} />
            <Route path="/watermark" element={<WatermarkPage />} />
            <Route path="/editor" element={<PresetEditorPage />} />
          </Routes>
        </Suspense>
      </div>
    </Router>
  );
}

export default App;
