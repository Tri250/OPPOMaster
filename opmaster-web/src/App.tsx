import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { lazy, Suspense, useEffect } from 'react';
import NavigationBar from './components/common/NavigationBar';
import { ToastProvider, useToast } from './components/ui/Toast';
import './index.css';

const LandingPage = lazy(() => import('./pages/LandingPage'));
const HomePage = lazy(() => import('./pages/HomePage'));
const AIDemoPage = lazy(() => import('./pages/AIDemoPage'));
const TechPage = lazy(() => import('./pages/TechPage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const PresetDetailPage = lazy(() => import('./pages/PresetDetailPage'));
const WatermarkPage = lazy(() => import('./pages/WatermarkPage'));
const PresetEditorPage = lazy(() => import('./pages/PresetEditorPage'));

function Loading() {
  return (
    <div className="min-h-screen bg-deep-space flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-hasselblad/30 border-t-hasselblad rounded-full animate-spin mx-auto mb-4" />
        <p className="text-white/60">加载中...</p>
      </div>
    </div>
  );
}

function ToastListener() {
  const { showToast } = useToast();

  useEffect(() => {
    const handleToast = (event: any) => {
      const { type, message, duration } = event.detail;
      showToast({ type, message, duration });
    };

    window.addEventListener('toast', handleToast as EventListener);
    return () => window.removeEventListener('toast', handleToast as EventListener);
  }, [showToast]);

  return null;
}

function App() {
  return (
    <Router>
      <ToastProvider>
        <ToastListener />
        <div className="min-h-screen bg-deep-space">
          <NavigationBar />
          <Suspense fallback={<Loading />}>
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/home" element={<HomePage />} />
              <Route path="/ai-demo" element={<AIDemoPage />} />
              <Route path="/tech" element={<TechPage />} />
              <Route path="/about" element={<AboutPage />} />
              <Route path="/preset/:id" element={<PresetDetailPage />} />
              <Route path="/watermark" element={<WatermarkPage />} />
              <Route path="/editor" element={<PresetEditorPage />} />
            </Routes>
          </Suspense>
        </div>
      </ToastProvider>
    </Router>
  );
}

export default App;