import { motion } from "framer-motion";
import { useState } from "react";
import { Check, Save, RotateCcw, Image as ImageIcon } from "lucide-react";
import PageHeader from "../components/PageHeader";
import { useAppStore } from "../store/useAppStore";
import { watermarkTemplateInfo } from "../data/mock";
import type { WatermarkTemplate, WatermarkPosition } from "../types";

const positions: { value: WatermarkPosition; label: string; className: string }[] = [
  { value: "TOP_LEFT", label: "左上", className: "top-3 left-3" },
  { value: "TOP_CENTER", label: "上中", className: "top-3 left-1/2 -translate-x-1/2" },
  { value: "TOP_RIGHT", label: "右上", className: "top-3 right-3" },
  { value: "CENTER", label: "居中", className: "top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" },
  { value: "BOTTOM_LEFT", label: "左下", className: "bottom-3 left-3" },
  { value: "BOTTOM_CENTER", label: "下中", className: "bottom-3 left-1/2 -translate-x-1/2" },
  { value: "BOTTOM_RIGHT", label: "右下", className: "bottom-3 right-3" },
];

const templates: WatermarkTemplate[] = ["HASSELBLAD", "OPPO", "ONEPLUS", "REALME", "CUSTOM"];

const sampleImage = "https://picsum.photos/seed/watermark-demo/1200/800";

export default function Watermark() {
  const { watermark, updateWatermark, resetWatermark } = useAppStore();
  const [saved, setSaved] = useState(false);

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const tpl = watermarkTemplateInfo[watermark.template];
  const displayText = watermark.template === "CUSTOM" && watermark.customText
    ? watermark.customText
    : tpl.displayText;

  const timestamp = new Date();
  const tsString = `${timestamp.getFullYear()}-${String(timestamp.getMonth() + 1).padStart(2, "0")}-${String(timestamp.getDate()).padStart(2, "0")} ${String(timestamp.getHours()).padStart(2, "0")}:${String(timestamp.getMinutes()).padStart(2, "0")}`;

  return (
    <div className="pt-24 pb-20 min-h-screen">
      <div className="fixed inset-0 -z-10 dotted-grid opacity-20" />
      <div className="fixed inset-0 -z-10 bg-gradient-to-b from-hasselblad-500/5 via-transparent to-transparent" />

      <div className="max-w-7xl mx-auto px-6">
        <PageHeader>
          <div className="text-center mb-12">
            <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
              Watermark Editor
            </span>
            <h1 className="font-display text-5xl md:text-6xl font-bold mt-3 mb-4">
              自定义 <span className="gradient-text italic">水印</span>
            </h1>
            <p className="text-ink-300 max-w-2xl mx-auto">
              个性化定制相机水印，支持多模板、多位置、可调节透明度。
            </p>
          </div>
        </PageHeader>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* 预览区 */}
          <div className="lg:col-span-2">
            <PageHeader>
              <div className="card aspect-[4/3] overflow-hidden relative">
                <img src={sampleImage} alt="预览" className="w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/40" />

                {/* 水印 */}
                {positions.map((p) => {
                  if (p.value !== watermark.position) return null;
                  return (
                    <motion.div
                      key={p.value}
                      layout
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: watermark.opacity, scale: watermark.scale }}
                      transition={{ duration: 0.3 }}
                      className={`absolute ${p.className} max-w-[60%]`}
                    >
                      <div
                        className="px-3 py-2 rounded-lg backdrop-blur-md bg-black/40 border border-white/20"
                        style={{ borderColor: tpl.color + "60" }}
                      >
                        <div
                          className="font-display font-bold tracking-wider leading-none"
                          style={{
                            color: tpl.color,
                            fontSize: `${14 * watermark.scale}px`,
                            textShadow: "0 2px 8px rgba(0,0,0,0.5)",
                          }}
                        >
                          {displayText}
                        </div>
                        {watermark.showDevice && (
                          <div className="text-[10px] text-white/90 mt-0.5 font-medium">
                            OPPO Find X8 Ultra
                          </div>
                        )}
                        {watermark.showTimestamp && (
                          <div className="text-[10px] text-white/70 mt-0.5 font-mono">
                            {tsString}
                          </div>
                        )}
                      </div>
                    </motion.div>
                  );
                })}

                {/* 顶部标签 */}
                <div className="absolute top-4 left-4 flex items-center gap-2 px-3 py-1.5 rounded-full glass-strong">
                  <ImageIcon className="w-3.5 h-3.5 text-hasselblad-500" />
                  <span className="text-xs font-medium">实时预览</span>
                </div>
              </div>
            </PageHeader>

            <div className="flex items-center gap-3 mt-6">
              <button onClick={handleSave} className="btn-primary flex-1">
                {saved ? <Check className="w-4 h-4" /> : <Save className="w-4 h-4" />}
                {saved ? "已保存" : "保存水印配置"}
              </button>
              <button onClick={resetWatermark} className="btn-ghost">
                <RotateCcw className="w-4 h-4" />
                重置
              </button>
            </div>
          </div>

          {/* 配置面板 */}
          <div className="space-y-6">
            {/* 模板 */}
            <PageHeader delay={0.1}>
              <div className="card p-6">
                <h3 className="font-display text-lg font-bold mb-4">模板</h3>
                <div className="space-y-2">
                  {templates.map((tpl) => {
                    const info = watermarkTemplateInfo[tpl];
                    const active = watermark.template === tpl;
                    return (
                      <button
                        key={tpl}
                        onClick={() => updateWatermark({ template: tpl })}
                        className={`w-full p-3 rounded-xl text-left transition-all border-2 ${
                          active
                            ? "border-hasselblad-500 bg-hasselblad-500/10"
                            : "border-white/10 hover:border-white/20 bg-white/[0.02]"
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="text-sm font-medium text-ink-50">{info.name}</div>
                            <div className="text-[10px] text-ink-400 font-mono mt-0.5">{info.displayText}</div>
                          </div>
                          <div
                            className="w-6 h-6 rounded-md"
                            style={{ backgroundColor: info.color }}
                          />
                        </div>
                      </button>
                    );
                  })}
                </div>

                {watermark.template === "CUSTOM" && (
                  <div className="mt-4">
                    <label className="text-xs text-ink-300 mb-1.5 block">自定义文字</label>
                    <input
                      type="text"
                      value={watermark.customText}
                      onChange={(e) => updateWatermark({ customText: e.target.value })}
                      maxLength={20}
                      placeholder="输入水印文字"
                      className="w-full px-3 py-2 rounded-lg glass-strong text-sm focus:border-hasselblad-500/50 focus:outline-none"
                    />
                  </div>
                )}
              </div>
            </PageHeader>

            {/* 位置 */}
            <PageHeader delay={0.2}>
              <div className="card p-6">
                <h3 className="font-display text-lg font-bold mb-4">位置</h3>
                <div className="grid grid-cols-3 gap-2">
                  {positions.map((p) => (
                    <button
                      key={p.value}
                      onClick={() => updateWatermark({ position: p.value })}
                      className={`px-3 py-2 rounded-lg text-xs font-medium transition-all ${
                        watermark.position === p.value
                          ? "bg-hasselblad-500 text-ink-900"
                          : "bg-white/[0.04] text-ink-200 hover:bg-white/[0.08]"
                      }`}
                    >
                      {p.label}
                    </button>
                  ))}
                </div>
              </div>
            </PageHeader>

            {/* 调整 */}
            <PageHeader delay={0.3}>
              <div className="card p-6 space-y-5">
                <h3 className="font-display text-lg font-bold">调整</h3>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-ink-300">文字大小</span>
                    <span className="text-sm font-mono text-hasselblad-400">
                      {(watermark.scale * 100).toFixed(0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0.5"
                    max="2"
                    step="0.05"
                    value={watermark.scale}
                    onChange={(e) => updateWatermark({ scale: parseFloat(e.target.value) })}
                    className="w-full accent-hasselblad-500"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-ink-300">透明度</span>
                    <span className="text-sm font-mono text-hasselblad-400">
                      {(watermark.opacity * 100).toFixed(0)}%
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0.1"
                    max="1"
                    step="0.05"
                    value={watermark.opacity}
                    onChange={(e) => updateWatermark({ opacity: parseFloat(e.target.value) })}
                    className="w-full accent-hasselblad-500"
                  />
                </div>

                <div className="pt-3 border-t border-white/[0.06] space-y-3">
                  <label className="flex items-center justify-between cursor-pointer">
                    <span className="text-sm text-ink-200">显示时间戳</span>
                    <input
                      type="checkbox"
                      checked={watermark.showTimestamp}
                      onChange={(e) => updateWatermark({ showTimestamp: e.target.checked })}
                      className="w-5 h-5 accent-hasselblad-500"
                    />
                  </label>
                  <label className="flex items-center justify-between cursor-pointer">
                    <span className="text-sm text-ink-200">显示设备型号</span>
                    <input
                      type="checkbox"
                      checked={watermark.showDevice}
                      onChange={(e) => updateWatermark({ showDevice: e.target.checked })}
                      className="w-5 h-5 accent-hasselblad-500"
                    />
                  </label>
                </div>
              </div>
            </PageHeader>
          </div>
        </div>
      </div>
    </div>
  );
}
