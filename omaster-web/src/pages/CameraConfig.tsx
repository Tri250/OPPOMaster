import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import {
  Camera,
  Activity,
  CircleDot,
  Zap,
  Aperture,
  Sun,
  Heart,
  Plus,
  Trash2,
  Save,
  Download,
  Upload,
} from "lucide-react";
import PageHeader from "../components/PageHeader";
import { cameraConfigs as initialConfigs } from "../data/mock";
import type { CameraConfig } from "../types";

const isoOptions = [50, 100, 200, 400, 800, 1600, 3200, 6400];
const shutterOptions = ["1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15"];
const apertureOptions = ["f/1.4", "f/1.8", "f/2.0", "f/2.8", "f/4.0", "f/5.6", "f/8.0"];
const evOptions = ["-1.0", "-0.7", "-0.3", "0", "+0.3", "+0.7", "+1.0"];
const wbOptions = ["3000K", "4000K", "5000K", "5500K", "6500K"];

export default function CameraConfig() {
  const [configs, setConfigs] = useState<CameraConfig[]>(initialConfigs);
  const [editingConfig, setEditingConfig] = useState<CameraConfig>({
    id: "new",
    name: "新配置",
    description: "",
    iso: 200,
    shutter: "1/125",
    aperture: "f/2.8",
    ev: "0",
    wb: "5500K",
    isFavorite: false,
    createdAt: Date.now(),
    tags: [],
  });
  const [isNew, setIsNew] = useState(true);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [currentParams, setCurrentParams] = useState({
    iso: 200,
    shutter: "1/125",
    aperture: "f/2.8",
    ev: "0",
    wb: "5500K",
  });
  const [savedToast, setSavedToast] = useState(false);

  // 模拟实时监控
  useEffect(() => {
    if (!isMonitoring) return;
    const timer = setInterval(() => {
      setCurrentParams({
        iso: isoOptions[Math.floor(Math.random() * isoOptions.length)],
        shutter: shutterOptions[Math.floor(Math.random() * shutterOptions.length)],
        aperture: apertureOptions[Math.floor(Math.random() * apertureOptions.length)],
        ev: evOptions[Math.floor(Math.random() * evOptions.length)],
        wb: wbOptions[Math.floor(Math.random() * wbOptions.length)],
      });
    }, 1500);
    return () => clearInterval(timer);
  }, [isMonitoring]);

  const handleSave = () => {
    if (isNew) {
      setConfigs([{ ...editingConfig, id: `c${Date.now()}`, createdAt: Date.now() }, ...configs]);
    } else {
      setConfigs(configs.map((c) => (c.id === editingConfig.id ? editingConfig : c)));
    }
    setSavedToast(true);
    setTimeout(() => setSavedToast(false), 2000);
  };

  const handleDelete = (id: string) => {
    setConfigs(configs.filter((c) => c.id !== id));
  };

  const handleToggleFavorite = (id: string) => {
    setConfigs(configs.map((c) => (c.id === id ? { ...c, isFavorite: !c.isFavorite } : c)));
  };

  const handleNewConfig = () => {
    setIsNew(true);
    setEditingConfig({
      id: "new",
      name: "新配置",
      description: "",
      iso: 200,
      shutter: "1/125",
      aperture: "f/2.8",
      ev: "0",
      wb: "5500K",
      isFavorite: false,
      createdAt: Date.now(),
      tags: [],
    });
  };

  const handleLoadConfig = (config: CameraConfig) => {
    setIsNew(false);
    setEditingConfig({ ...config });
  };

  return (
    <div className="pt-24 pb-20 min-h-screen">
      <div className="fixed inset-0 -z-10 dotted-grid opacity-20" />
      <div className="fixed inset-0 -z-10 bg-gradient-to-b from-hasselblad-500/5 via-transparent to-transparent" />

      {savedToast && (
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-xl glass-strong border border-emerald-500/40"
        >
          <p className="text-sm text-ink-50 flex items-center gap-2">
            <Save className="w-4 h-4 text-emerald-500" />
            配置已保存
          </p>
        </motion.div>
      )}

      <div className="max-w-7xl mx-auto px-6">
        <PageHeader>
          <div className="text-center mb-12">
            <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
              Camera Configuration
            </span>
            <h1 className="font-display text-5xl md:text-6xl font-bold mt-3 mb-4">
              <span className="gradient-text italic">相机配置</span> 管理
            </h1>
            <p className="text-ink-300 max-w-2xl mx-auto">
              实时监控相机参数，保存多套配置，一键导入导出。
            </p>
          </div>
        </PageHeader>

        <div className="grid lg:grid-cols-3 gap-6">
            {/* 实时监控 */}
            <PageHeader>
              <div className="card p-6 lg:col-span-1">
                <div className="flex items-center justify-between mb-6">
                  <div className="flex items-center gap-2">
                    <Activity className="w-5 h-5 text-hasselblad-500" />
                    <h3 className="font-display text-lg font-bold">实时监控</h3>
                  </div>
                  <button
                    onClick={() => setIsMonitoring(!isMonitoring)}
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium ${
                      isMonitoring ? "bg-emerald-500/20 text-emerald-400" : "bg-white/5 text-ink-300"
                    }`}
                  >
                    <span className={`w-1.5 h-1.5 rounded-full ${isMonitoring ? "bg-emerald-400 animate-pulse" : "bg-ink-400"}`} />
                    {isMonitoring ? "运行中" : "已停止"}
                  </button>
                </div>

                <div className="space-y-3">
                  <ParamDisplay icon={CircleDot} label="ISO" value={String(currentParams.iso)} />
                  <ParamDisplay icon={Zap} label="快门" value={currentParams.shutter} />
                  <ParamDisplay icon={Aperture} label="光圈" value={currentParams.aperture} />
                  <ParamDisplay icon={Sun} label="曝光" value={currentParams.ev} />
                  <ParamDisplay icon={Activity} label="白平衡" value={currentParams.wb} />
                </div>

                <button
                  onClick={() => setEditingConfig({ ...editingConfig, ...currentParams })}
                  className="btn-ghost w-full mt-6"
                >
                  <Camera className="w-4 h-4" />
                  同步至编辑面板
                </button>
              </div>
            </PageHeader>

            {/* 编辑器 */}
            <PageHeader delay={0.1}>
              <div className="card p-6 lg:col-span-2">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="font-display text-lg font-bold">
                    {isNew ? "新建配置" : "编辑配置"}
                  </h3>
                  <div className="flex gap-2">
                    <button className="btn-ghost text-xs px-3 py-1.5">
                      <Upload className="w-3.5 h-3.5" />
                      导入
                    </button>
                    <button className="btn-ghost text-xs px-3 py-1.5">
                      <Download className="w-3.5 h-3.5" />
                      导出
                    </button>
                  </div>
                </div>

                <div className="grid sm:grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="text-xs text-ink-300 mb-1.5 block">配置名称</label>
                    <input
                      type="text"
                      value={editingConfig.name}
                      onChange={(e) => setEditingConfig({ ...editingConfig, name: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg glass-strong text-sm focus:border-hasselblad-500/50 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-ink-300 mb-1.5 block">描述</label>
                    <input
                      type="text"
                      value={editingConfig.description}
                      onChange={(e) => setEditingConfig({ ...editingConfig, description: e.target.value })}
                      placeholder="可选"
                      className="w-full px-3 py-2 rounded-lg glass-strong text-sm focus:border-hasselblad-500/50 focus:outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
                  <ParamSelector
                    label="ISO"
                    value={String(editingConfig.iso)}
                    options={isoOptions.map(String)}
                    onChange={(v) => setEditingConfig({ ...editingConfig, iso: parseInt(v) })}
                  />
                  <ParamSelector
                    label="快门"
                    value={editingConfig.shutter}
                    options={shutterOptions}
                    onChange={(v) => setEditingConfig({ ...editingConfig, shutter: v })}
                  />
                  <ParamSelector
                    label="光圈"
                    value={editingConfig.aperture}
                    options={apertureOptions}
                    onChange={(v) => setEditingConfig({ ...editingConfig, aperture: v })}
                  />
                  <ParamSelector
                    label="曝光"
                    value={editingConfig.ev}
                    options={evOptions}
                    onChange={(v) => setEditingConfig({ ...editingConfig, ev: v })}
                  />
                  <ParamSelector
                    label="白平衡"
                    value={editingConfig.wb}
                    options={wbOptions}
                    onChange={(v) => setEditingConfig({ ...editingConfig, wb: v })}
                  />
                </div>

                <div className="flex gap-3 mt-6">
                  <button onClick={handleSave} className="btn-primary flex-1">
                    <Save className="w-4 h-4" />
                    保存配置
                  </button>
                  <button onClick={handleNewConfig} className="btn-ghost">
                    <Plus className="w-4 h-4" />
                    新建
                  </button>
                </div>
              </div>
            </PageHeader>

            {/* 配置列表 */}
            <div className="lg:col-span-3">
              <PageHeader delay={0.2}>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-display text-lg font-bold">已保存配置 · {configs.length}</h3>
                </div>
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {configs.map((config) => (
                    <motion.div
                      key={config.id}
                      layout
                      className={`card p-5 cursor-pointer transition-all ${
                        editingConfig.id === config.id ? "border-hasselblad-500/50 bg-hasselblad-500/5" : ""
                      }`}
                      onClick={() => handleLoadConfig(config)}
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <h4 className="font-display text-base font-bold text-ink-50">{config.name}</h4>
                          {config.description && (
                            <p className="text-xs text-ink-400 mt-1 line-clamp-1">{config.description}</p>
                          )}
                        </div>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleFavorite(config.id);
                          }}
                          className="p-1.5"
                        >
                          <Heart
                            className={`w-4 h-4 ${
                              config.isFavorite ? "fill-hasselblad-500 text-hasselblad-500" : "text-ink-400"
                            }`}
                          />
                        </button>
                      </div>

                      <div className="grid grid-cols-5 gap-1.5 mb-3 font-mono">
                        <ParamChip label="ISO" value={String(config.iso)} />
                        <ParamChip label="S" value={config.shutter} />
                        <ParamChip label="F" value={config.aperture} />
                        <ParamChip label="EV" value={config.ev} />
                        <ParamChip label="WB" value={config.wb} />
                      </div>

                      <div className="flex items-center justify-between text-xs">
                        <span className="text-ink-400">
                          {new Date(config.createdAt).toLocaleDateString("zh-CN")}
                        </span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(config.id);
                          }}
                          className="text-ink-400 hover:text-red-400 p-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </PageHeader>
            </div>
          </div>
      </div>
    </div>
  );
}

function ParamDisplay({ icon: Icon, label, value }: { icon: React.ComponentType<{ className?: string }>; label: string; value: string }) {
  return (
    <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.03]">
      <div className="flex items-center gap-2 text-ink-300">
        <Icon className="w-4 h-4 text-hasselblad-500" />
        <span className="text-sm">{label}</span>
      </div>
      <div className="font-mono text-sm font-semibold text-ink-50">{value}</div>
    </div>
  );
}

function ParamSelector({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (v: string) => void;
}) {
  return (
    <div>
      <label className="text-[10px] text-ink-400 tracking-wider uppercase mb-1.5 block">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full px-2.5 py-2 rounded-lg glass-strong text-sm font-mono focus:border-hasselblad-500/50 focus:outline-none"
      >
        {options.map((opt) => (
          <option key={opt} value={opt} className="bg-ink-800">
            {opt}
          </option>
        ))}
      </select>
    </div>
  );
}

function ParamChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col items-center justify-center p-1.5 rounded-md bg-white/[0.04]">
      <span className="text-[8px] text-ink-400">{label}</span>
      <span className="text-[10px] text-ink-100 font-semibold">{value}</span>
    </div>
  );
}
