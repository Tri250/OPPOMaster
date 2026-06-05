import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Link } from "react-router-dom";
import {
  Camera,
  ScanLine,
  Wifi,
  WifiOff,
  Sparkles,
  Timer,
  TrendingUp,
  ChevronRight,
  RefreshCw,
  Aperture,
  CircleDot,
  Zap,
  Sun,
} from "lucide-react";
import PageHeader from "../components/PageHeader";
import { detectSceneFromImage, presets, sceneInfo } from "../data/mock";
import type { SceneDetectionResult, Preset } from "../types";

const sampleScenes = [
  { id: 0, label: "人像", image: "https://picsum.photos/seed/ai-portrait/800/600" },
  { id: 1, label: "风景", image: "https://picsum.photos/seed/ai-landscape/800/600" },
  { id: 2, label: "夜景", image: "https://picsum.photos/seed/ai-night/800/600" },
  { id: 3, label: "美食", image: "https://picsum.photos/seed/ai-food/800/600" },
  { id: 4, label: "街拍", image: "https://picsum.photos/seed/ai-street/800/600" },
  { id: 5, label: "微距", image: "https://picsum.photos/seed/ai-macro/800/600" },
  { id: 6, label: "日落", image: "https://picsum.photos/seed/ai-sunset/800/600" },
  { id: 7, label: "城市", image: "https://picsum.photos/seed/ai-city/800/600" },
];

export default function SceneDetection() {
  const [selectedSceneIndex, setSelectedSceneIndex] = useState<number | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);
  const [result, setResult] = useState<SceneDetectionResult | null>(null);
  const [isOnline, setIsOnline] = useState(true);

  const handleDetect = (index: number) => {
    setSelectedSceneIndex(index);
    setResult(null);
    setIsDetecting(true);

    setTimeout(() => {
      const r = detectSceneFromImage(index, !isOnline);
      setResult(r);
      setIsDetecting(false);
    }, isOnline ? 1800 : 1200);
  };

  const handleRetry = () => {
    if (selectedSceneIndex !== null) handleDetect(selectedSceneIndex);
  };

  const recommendedPresets: Preset[] = result
    ? result.recommendedPresetIds
        .map((id) => presets.find((p) => p.id === id))
        .filter((p): p is Preset => Boolean(p))
    : [];

  return (
    <div className="pt-24 pb-20 min-h-screen">
      <div className="fixed inset-0 -z-10 dotted-grid opacity-20" />
      <div className="fixed inset-0 -z-10 bg-gradient-to-b from-hasselblad-500/5 via-transparent to-transparent" />

      <div className="max-w-7xl mx-auto px-6">
        <PageHeader>
          <div className="text-center mb-12">
            <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
              AI Scene Detection
            </span>
            <h1 className="font-display text-5xl md:text-6xl font-bold mt-3 mb-4">
              <span className="gradient-text italic">AI 场景识别</span> 演示
            </h1>
            <p className="text-ink-300 max-w-2xl mx-auto">
              基于 ML Kit 与云端大模型，毫秒级识别 8 大场景，自动推荐匹配预设。
            </p>
          </div>
        </PageHeader>

        {/* 模式切换 */}
        <div className="flex items-center justify-center mb-10">
          <div className="inline-flex items-center gap-1 p-1.5 rounded-2xl glass-strong">
            <button
              onClick={() => setIsOnline(true)}
              className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isOnline ? "bg-hasselblad-500 text-ink-900" : "text-ink-300 hover:text-ink-100"
              }`}
            >
              <Wifi className="w-4 h-4" />
              在线检测
            </button>
            <button
              onClick={() => setIsOnline(false)}
              className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                !isOnline ? "bg-hasselblad-500 text-ink-900" : "text-ink-300 hover:text-ink-100"
              }`}
            >
              <WifiOff className="w-4 h-4" />
              离线 ML Kit
            </button>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* 左：取景器 */}
          <PageHeader>
            <div className="card aspect-[4/3] overflow-hidden relative">
              <AnimatePresence mode="wait">
                {selectedSceneIndex === null ? (
                  <motion.div
                    key="empty"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="absolute inset-0 flex flex-col items-center justify-center bg-ink-800"
                  >
                    <div className="w-20 h-20 rounded-full bg-hasselblad-500/10 flex items-center justify-center mb-4 animate-breathe">
                      <Camera className="w-10 h-10 text-hasselblad-500" />
                    </div>
                    <h3 className="font-display text-xl font-bold mb-2">点击下方场景开始识别</h3>
                    <p className="text-sm text-ink-300 text-center max-w-xs px-4">
                      选择一个示例场景，体验 AI 场景检测的完整流程
                    </p>
                  </motion.div>
                ) : (
                  <motion.div
                    key={`scene-${selectedSceneIndex}`}
                    initial={{ opacity: 0, scale: 1.05 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.5 }}
                    className="absolute inset-0"
                  >
                    <img
                      src={sampleScenes[selectedSceneIndex].image}
                      alt={sampleScenes[selectedSceneIndex].label}
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-ink-900 via-ink-900/30 to-transparent" />

                    {/* 扫描框 */}
                    {isDetecting && (
                      <>
                        <div className="absolute inset-12 border-2 border-hasselblad-500 rounded-2xl">
                          <span className="absolute -top-1 -left-1 w-6 h-6 border-t-4 border-l-4 border-hasselblad-500 rounded-tl-2xl" />
                          <span className="absolute -top-1 -right-1 w-6 h-6 border-t-4 border-r-4 border-hasselblad-500 rounded-tr-2xl" />
                          <span className="absolute -bottom-1 -left-1 w-6 h-6 border-b-4 border-l-4 border-hasselblad-500 rounded-bl-2xl" />
                          <span className="absolute -bottom-1 -right-1 w-6 h-6 border-b-4 border-r-4 border-hasselblad-500 rounded-br-2xl" />
                        </div>
                        <motion.div
                          initial={{ y: 0 }}
                          animate={{ y: ["0%", "100%", "0%"] }}
                          transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
                          className="absolute left-12 right-12 h-1 bg-gradient-to-r from-transparent via-hasselblad-500 to-transparent shadow-[0_0_30px_rgba(255,107,0,0.6)]"
                        />
                      </>
                    )}

                    {/* 识别结果浮层 */}
                    {result && (
                      <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="absolute bottom-6 left-6 right-6"
                      >
                        <div className="glass-strong rounded-2xl p-4">
                          <div className="flex items-center justify-between mb-2">
                            <div className="flex items-center gap-2">
                              <span
                                className="w-2.5 h-2.5 rounded-full"
                                style={{ backgroundColor: sceneInfo[result.scene].color }}
                              />
                              <span className="font-display text-lg font-bold">
                                {result.sceneName}
                              </span>
                            </div>
                            <span className="text-xs text-ink-300">
                              {result.isOffline ? "本地 ML" : "云端 AI"} · {result.detectionTime}ms
                            </span>
                          </div>
                          <div className="flex items-center gap-3">
                            <div className="flex-1 h-2 rounded-full bg-white/10 overflow-hidden">
                              <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: `${result.confidence * 100}%` }}
                                transition={{ duration: 0.8, ease: "easeOut" }}
                                className="h-full bg-gradient-to-r from-hasselblad-500 to-amber-400"
                              />
                            </div>
                            <span className="font-mono text-sm font-bold text-hasselblad-400">
                              {(result.confidence * 100).toFixed(1)}%
                            </span>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* 重新识别 */}
            {result && selectedSceneIndex !== null && (
              <button onClick={handleRetry} className="btn-ghost w-full mt-4">
                <RefreshCw className="w-4 h-4" />
                重新识别
              </button>
            )}
          </PageHeader>

          {/* 右：场景选择 + 结果 */}
          <div className="space-y-6">
            <PageHeader delay={0.1}>
              <h3 className="font-display text-xl font-bold mb-4">选择场景</h3>
              <div className="grid grid-cols-4 gap-3">
                {sampleScenes.map((scene, i) => (
                  <button
                    key={scene.id}
                    onClick={() => handleDetect(i)}
                    className={`relative aspect-square rounded-2xl overflow-hidden border-2 transition-all hover:scale-105 ${
                      selectedSceneIndex === i
                        ? "border-hasselblad-500 shadow-[0_0_30px_rgba(255,107,0,0.4)]"
                        : "border-white/10 hover:border-white/30"
                    }`}
                  >
                    <img src={scene.image} alt={scene.label} className="w-full h-full object-cover" />
                    <div className="absolute inset-0 bg-gradient-to-t from-ink-900/80 to-transparent" />
                    <span className="absolute bottom-1.5 inset-x-0 text-center text-xs font-medium">
                      {scene.label}
                    </span>
                    {isDetecting && selectedSceneIndex === i && (
                      <div className="absolute inset-0 bg-ink-900/60 flex items-center justify-center">
                        <div className="w-5 h-5 border-2 border-hasselblad-500 border-t-transparent rounded-full animate-spin" />
                      </div>
                    )}
                  </button>
                ))}
              </div>
            </PageHeader>

            {/* 状态卡 */}
            <PageHeader delay={0.2}>
              <div className="grid grid-cols-3 gap-3">
                <div className="card p-4 text-center">
                  <Timer className="w-5 h-5 text-hasselblad-500 mx-auto mb-2" />
                  <div className="text-xs text-ink-300">响应时间</div>
                  <div className="font-mono text-lg font-bold text-ink-50">
                    {result ? `${result.detectionTime}ms` : "—"}
                  </div>
                </div>
                <div className="card p-4 text-center">
                  <Sparkles className="w-5 h-5 text-hasselblad-500 mx-auto mb-2" />
                  <div className="text-xs text-ink-300">置信度</div>
                  <div className="font-mono text-lg font-bold text-ink-50">
                    {result ? `${(result.confidence * 100).toFixed(0)}%` : "—"}
                  </div>
                </div>
                <div className="card p-4 text-center">
                  <TrendingUp className="w-5 h-5 text-hasselblad-500 mx-auto mb-2" />
                  <div className="text-xs text-ink-300">准确率</div>
                  <div className="font-mono text-lg font-bold text-ink-50">≥85%</div>
                </div>
              </div>
            </PageHeader>

            {/* 推荐预设 */}
            <AnimatePresence>
              {result && recommendedPresets.length > 0 && (
                <motion.div
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                >
                  <h3 className="font-display text-xl font-bold mb-4 flex items-center gap-2">
                    <ScanLine className="w-5 h-5 text-hasselblad-500" />
                    推荐预设
                  </h3>
                  <div className="space-y-3">
                    {recommendedPresets.map((preset, i) => (
                      <motion.div
                        key={preset.id}
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: i * 0.1 }}
                      >
                        <Link
                          to={`/presets/${preset.id}`}
                          className="card p-4 flex items-center gap-4 hover:border-hasselblad-500/40 transition-all group"
                        >
                          <div className="relative w-16 h-16 rounded-xl overflow-hidden flex-shrink-0">
                            <img
                              src={preset.coverUrl}
                              alt={preset.name}
                              className="w-full h-full object-cover"
                            />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5 mb-1">
                              <span
                                className="w-1.5 h-1.5 rounded-full"
                                style={{ backgroundColor: sceneInfo[result.scene].color }}
                              />
                              <span className="text-[10px] text-hasselblad-400 font-semibold tracking-wider uppercase">
                                匹配度 {95 - i * 5}%
                              </span>
                            </div>
                            <h4 className="font-medium text-ink-50 line-clamp-1 text-sm">
                              {preset.name}
                            </h4>
                            <div className="flex items-center gap-3 text-xs text-ink-400 mt-1">
                              <span className="flex items-center gap-1">
                                <CircleDot className="w-3 h-3" />
                                ISO {preset.cameraParams.iso}
                              </span>
                              <span className="flex items-center gap-1">
                                <Zap className="w-3 h-3" />
                                {preset.cameraParams.shutter}
                              </span>
                            </div>
                          </div>
                          <ChevronRight className="w-5 h-5 text-ink-400 group-hover:text-hasselblad-500 group-hover:translate-x-1 transition-all" />
                        </Link>
                      </motion.div>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {!result && !isDetecting && (
              <PageHeader delay={0.3}>
                <div className="card p-6 text-center">
                  <ScanLine className="w-8 h-8 text-ink-400 mx-auto mb-3" />
                  <p className="text-sm text-ink-300">
                    {isOnline
                      ? "在线模式：使用云端大模型，识别精度更高"
                      : "离线模式：使用本地 ML Kit 模型，响应更快"}
                  </p>
                </div>
              </PageHeader>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
