import { lazy, Suspense } from 'react';
import './index.css';

const MobileDemoPage = lazy(() => import('./pages/MobileDemoPage'));

function Loading() {
  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-hasselblad/30 border-t-hasselblad rounded-full animate-spin mx-auto mb-4" />
        <p className="text-white/60">加载中...</p>
      </div>
    </div>
  );
}

function App() {
  return (
    <div className="min-h-screen bg-gray-900">
      <Suspense fallback={<Loading />}>
        <MobileDemoPage />
      </Suspense>
    </div>
  );
}

export default App;
