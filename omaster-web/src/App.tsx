import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useEffect } from "react";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import Home from "./pages/Home";
import Presets from "./pages/Presets";
import PresetDetail from "./pages/PresetDetail";
import SceneDetection from "./pages/SceneDetection";
import Watermark from "./pages/Watermark";
import CameraConfig from "./pages/CameraConfig";

function ScrollToTop() {
  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);
  return null;
}

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="flex-1">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/presets" element={<Presets />} />
            <Route path="/presets/:id" element={<PresetDetail />} />
            <Route path="/scene-detection" element={<SceneDetection />} />
            <Route path="/watermark" element={<Watermark />} />
            <Route path="/camera-config" element={<CameraConfig />} />
            <Route path="*" element={<Home />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </BrowserRouter>
  );
}

export default App;
