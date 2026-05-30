import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { useState, useEffect } from 'react';
import NavigationBar from './components/common/NavigationBar';
import NetworkError from './components/common/NetworkError';
import NotFoundPage from './pages/NotFoundPage';
import FloatingGuidePage from './pages/FloatingGuidePage';
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
  const [isOnline, setIsOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);
  const [showNetworkError, setShowNetworkError] = useState(false);

  // 监听网络状态
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowNetworkError(false);
    };
    const handleOffline = () => {
      setIsOnline(false);
      setShowNetworkError(true);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleRetry = () => {
    if (navigator.onLine) {
      setShowNetworkError(false);
      window.location.reload();
    }
  };

  return (
    <Router>
      <div className="min-h-screen bg-page-bg">
        {/* 固定顶部导航栏，高度64dp */}
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
            <Route path="/floating-guide" element={<FloatingGuidePage />} />
            {/* 404页面 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </Suspense>
        
        {/* 网络错误提示 */}
        <NetworkError 
          isVisible={showNetworkError}
          onRetry={handleRetry}
          onDismiss={() => setShowNetworkError(false)}
        />
      </div>
    </Router>
  );
}

export default App;
