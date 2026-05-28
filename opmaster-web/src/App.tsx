import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import NavigationBar from './components/common/NavigationBar';
import './index.css';

const HomePage = lazy(() => import('./pages/HomePage'));
const AIDemoPage = lazy(() => import('./pages/AIDemoPage'));
const TechPage = lazy(() => import('./pages/TechPage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const PresetDetailPage = lazy(() => import('./pages/PresetDetailPage'));

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

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-deep-space">
        <NavigationBar />
        <Suspense fallback={<Loading />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/ai-demo" element={<AIDemoPage />} />
            <Route path="/tech" element={<TechPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/preset/:id" element={<PresetDetailPage />} />
          </Routes>
        </Suspense>
      </div>
    </Router>
  );
}

export default App;
