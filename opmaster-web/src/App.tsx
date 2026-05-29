import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './index.css';
import HomePage from './pages/HomePage';
import AIDemoPage from './pages/AIDemoPage';
import TechPage from './pages/TechPage';
import AboutPage from './pages/AboutPage';
import PresetDetailPage from './pages/PresetDetailPage';
import WatermarkPage from './pages/WatermarkPage';
import PresetEditorPage from './pages/PresetEditorPage';
import SettingsPage from './pages/SettingsPage';
import AiFineTunePage from './pages/AiFineTunePage';
import SceneDetectionPage from './pages/SceneDetectionPage';
import FloatingWindowPage from './pages/FloatingWindowPage';
import LutManagerPage from './pages/LutManagerPage';
import CloudSyncPage from './pages/CloudSyncPage';
import OcrDemoPage from './pages/OcrDemoPage';
import FilterLibraryPage from './pages/FilterLibraryPage';
import MasterParamsPage from './pages/MasterParamsPage';
import P0Overview from './pages/P0Overview';
import XiaoOHelpPage from './pages/XiaoOHelpPage';
import NativeCameraPage from './pages/NativeCameraPage';
import PresetEcosystemPage from './pages/PresetEcosystemPage';
import TestVerificationPage from './pages/TestVerificationPage';
import SubscriptionPage from './pages/SubscriptionPage';
import CommunityPage from './pages/CommunityPage';
import BottomNavigationBar from './components/common/BottomNavigationBar';

// 底部导航栏需要显示的路由
const showBottomNavRoutes = [
  '/',
  '/filter-library',
  '/ai-demo',
  '/floating-window',
  '/community'
];

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-bg-primary text-text-primary">
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
          <Route path="/test-verification" element={<TestVerificationPage />} />
          <Route path="/subscription" element={<SubscriptionPage />} />
          <Route path="/community" element={<CommunityPage />} />
        </Routes>
        
        {/* 底部导航栏 - 仅在特定路由显示 */}
        <Routes>
          {showBottomNavRoutes.map(path => (
            <Route 
              key={path} 
              path={path} 
              element={<BottomNavigationBar />} 
            />
          ))}
        </Routes>
      </div>
    </Router>
  );
}

export default App;
