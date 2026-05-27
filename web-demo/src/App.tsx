import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "@/pages/Home";
import Navbar from "@/components/Navbar";
import PresetDetail from "@/pages/PresetDetail";
import AISceneDetection from "@/pages/AISceneDetection";
import ColorAnalysis from "@/pages/ColorAnalysis";

export default function App() {
  return (
    <Router>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/preset/:id" element={<PresetDetail />} />
        <Route path="/ai-scene" element={<AISceneDetection />} />
        <Route path="/color-analysis" element={<ColorAnalysis />} />
      </Routes>
    </Router>
  );
}
