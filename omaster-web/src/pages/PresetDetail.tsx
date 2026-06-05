import { useParams, Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Heart,
  Star,
  Download,
  Camera as CameraIcon,
  PlayCircle,
  Sparkles,
  ArrowRight,
  Check,
  X as XIcon,
  Smartphone,
  Aperture,
  Sun,
  Zap,
  CircleDot,
  Award,
} from "lucide-react";
import { useState } from "react";
import PageHeader from "../components/PageHeader";
import { presets } from "../data/mock";
import { useAppStore } from "../store/useAppStore";

const paramIcons: Record<string, React.ComponentType<{ className?: string }>> = {
  iso: CircleDot,
  shutter: Zap,
  aperture: Aperture,
  ev: Sun,
  wb: Sparkles,
};

export default function PresetDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const preset = presets.find((p) => p.id === id);
  const { favorites, toggleFavorite } = useAppStore();
  const [showApplyDialog, setShowApplyDialog] = useState(false);
  const [showPermissionDialog, setShowPermissionDialog] = useState(false);
  const [isTuning, setIsTuning] = useState(false);
  const [showTuningResult, setShowTuningResult] = useState(false);
  const [applyMessage, setApplyMessage] = useState<string | null>(null);

  if (!preset) {
    return (
      <div className="pt-32 pb-20 text-center min-h-screen">
        <h1 className="font-display text-4xl font-bold mb-4">预设不存在</h1>
        <Link to="/presets" className="btn-primary inline-flex">
          返回预设库
        </Link>
      </div>
    );
  }

  const isFavorite = favorites.has(preset.id);

  // 模拟AI微调后的参数
  const tunedParams = {
    iso: Math.max(50, preset.cameraParams.iso - 50),
    shutter: preset.cameraParams.shutter,
    aperture: preset.cameraParams.aperture,
    ev: parseFloat(preset.cameraParams.ev) > 0
      ? "+" + (parseFloat(preset.cameraParams.ev) + 0.2).toFixed(1)
      : (parseFloat(preset.cameraParams.ev) + 0.2).toFixed(1),
    wb: preset.cameraParams.wb,
  };

  const handleApply = () => setShowApplyDialog(true);

  const confirmApply = () => {
    setShowApplyDialog(false);
    setShowPermissionDialog(true);
  };

  const goToSettings = () => {
    setShowPermissionDialog(false);
    setApplyMessage("已跳转至无障碍设置");
    setTimeout(() => setApplyMessage(null), 2500);
  };

  const handleTuning = () => {
    setIsTuning(true);
    setTimeout(() => {
      setIsTuning(false);
      setShowTuningResult(true);
    }, 1800);
  };

  const params: { key: keyof typeof preset.cameraParams; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { key: "iso", label: "ISO 感光度", icon: paramIcons.iso },
    { key: "shutter", label: "快门速度", icon: paramIcons.shutter },
    { key: "aperture", label: "光圈", icon: paramIcons.aperture },
    { key: "ev", label: "曝光补偿", icon: paramIcons.ev },
    { key: "wb", label: "白平衡", icon: paramIcons.wb },
  ];

  return (
    <div className="pt-24 pb-20 min-h-screen">
      {applyMessage && (
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-5 py-3 rounded-xl glass-strong border border-hasselblad-500/40"
        >
          <p className="text-sm text-ink-50 flex items-center gap-2">
            <Check className="w-4 h-4 text-hasselblad-500" />
            {applyMessage}
          </p>
        </motion.div>
      )}

      <div className="max-w-6xl mx-auto px-6">
        {/* 返回按钮 */}
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-ink-300 hover:text-hasselblad-400 mb-6 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          返回
        </button>

        <div className="grid lg:grid-cols-2 gap-10">
          {/* 左：封面图 */}
          <PageHeader>
            <div className="relative aspect-[4/3] rounded-3xl overflow-hidden card">
              <img src={preset.coverUrl} alt={preset.name} className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-gradient-to-t from-ink-900/80 via-transparent to-transparent" />
              {preset.isHncsCertified && (
                <div className="absolute top-5 left-5 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-hasselblad-500 text-ink-900 text-xs font-bold">
                  <Award className="w-3.5 h-3.5" />
                  HNCS 认证
                </div>
              )}
              <button
                onClick={() => toggleFavorite(preset.id)}
                className="absolute top-5 right-5 w-11 h-11 rounded-full glass-strong flex items-center justify-center hover:scale-110 transition-transform"
              >
                <Heart
                  className={`w-5 h-5 ${
                    isFavorite ? "fill-hasselblad-500 text-hasselblad-500" : "text-ink-200"
                  }`}
                />
              </button>
            </div>
          </PageHeader>

          {/* 右：信息 */}
          <PageHeader delay={0.1}>
            <div>
              <div className="flex items-center gap-2 mb-3 flex-wrap">
                <span className="text-xs text-ink-300 px-2.5 py-1 rounded-md bg-white/5">
                  v{preset.version}
                </span>
                <span className="text-xs text-ink-300 px-2.5 py-1 rounded-md bg-white/5">
                  {preset.sceneType}
                </span>
                {preset.tags.slice(0, 3).map((tag) => (
                  <span key={tag} className="text-xs text-hasselblad-400 px-2.5 py-1 rounded-md bg-hasselblad-500/10 border border-hasselblad-500/20">
                    #{tag}
                  </span>
                ))}
              </div>

              <h1 className="font-display text-3xl md:text-4xl font-bold mb-4 leading-tight">
                {preset.name}
              </h1>

              <p className="text-ink-300 leading-relaxed mb-6">{preset.description}</p>

              {/* 元数据 */}
              <div className="grid grid-cols-2 gap-3 mb-6">
                <div className="card p-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-hasselblad-500/10 flex items-center justify-center">
                    <CameraIcon className="w-5 h-5 text-hasselblad-500" />
                  </div>
                  <div>
                    <div className="text-[10px] text-ink-400 tracking-wider uppercase">设备</div>
                    <div className="text-sm font-medium text-ink-100">{preset.deviceModel}</div>
                  </div>
                </div>
                <div className="card p-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-amber-500/10 flex items-center justify-center">
                    <Star className="w-5 h-5 text-amber-500 fill-amber-500" />
                  </div>
                  <div>
                    <div className="text-[10px] text-ink-400 tracking-wider uppercase">评分</div>
                    <div className="text-sm font-medium text-ink-100">{preset.rating.toFixed(1)} / 5.0</div>
                  </div>
                </div>
                <div className="card p-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center">
                    <Download className="w-5 h-5 text-emerald-500" />
                  </div>
                  <div>
                    <div className="text-[10px] text-ink-400 tracking-wider uppercase">下载量</div>
                    <div className="text-sm font-medium text-ink-100">{(preset.downloadCount / 1000).toFixed(1)}K</div>
                  </div>
                </div>
                <div className="card p-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-cyan-500/10 flex items-center justify-center">
                    <Smartphone className="w-5 h-5 text-cyan-500" />
                  </div>
                  <div>
                    <div className="text-[10px] text-ink-400 tracking-wider uppercase">作者</div>
                    <div className="text-sm font-medium text-ink-100 line-clamp-1">{preset.author}</div>
                  </div>
                </div>
              </div>

              {/* 相机参数 */}
              <div className="card p-6 mb-6">
                <h3 className="font-display text-lg font-bold mb-4 flex items-center gap-2">
                  <Aperture className="w-5 h-5 text-hasselblad-500" />
                  相机参数
                </h3>
                <div className="space-y-3">
                  {params.map(({ key, label, icon: Icon }) => {
                    const value = preset.cameraParams[key];
                    return (
                      <div
                        key={key}
                        className="flex items-center justify-between py-2 border-b border-white/[0.06] last:border-0"
                      >
                        <div className="flex items-center gap-2 text-ink-300">
                          <Icon className="w-4 h-4 text-hasselblad-500" />
                          <span className="text-sm">{label}</span>
                        </div>
                        <div className="font-mono text-sm font-semibold text-ink-50">
                          {String(value)}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* 操作按钮 */}
              <div className="flex flex-wrap gap-3">
                <button onClick={handleApply} className="btn-primary flex-1">
                  <PlayCircle className="w-5 h-5" />
                  应用预设
                </button>
                <button
                  onClick={handleTuning}
                  disabled={isTuning}
                  className="btn-ghost flex-1 disabled:opacity-50"
                >
                  <Sparkles className="w-5 h-5" />
                  {isTuning ? "AI 分析中..." : "AI 微调"}
                </button>
              </div>
            </div>
          </PageHeader>
        </div>

        {/* AI 微调结果 */}
        {showTuningResult && (
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-12 card p-8"
          >
            <div className="flex items-center gap-3 mb-6">
              <div className="w-12 h-12 rounded-2xl bg-hasselblad-500/10 flex items-center justify-center">
                <Sparkles className="w-6 h-6 text-hasselblad-500" />
              </div>
              <div>
                <h2 className="font-display text-2xl font-bold">AI 微调建议</h2>
                <p className="text-sm text-ink-300">基于当前场景智能分析，耗时 1.8 秒</p>
              </div>
            </div>

            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <h3 className="text-sm font-semibold text-ink-300 mb-3 tracking-wider uppercase">调整前</h3>
                <div className="space-y-2">
                  {params.map(({ key, label, icon: Icon }) => (
                    <div key={`before-${key}`} className="flex items-center justify-between p-3 rounded-xl bg-white/[0.03]">
                      <div className="flex items-center gap-2 text-sm text-ink-300">
                        <Icon className="w-4 h-4" />
                        {label}
                      </div>
                      <div className="font-mono text-sm text-ink-400">
                        {String(preset.cameraParams[key])}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <h3 className="text-sm font-semibold text-hasselblad-400 mb-3 tracking-wider uppercase">AI 建议</h3>
                <div className="space-y-2">
                  {params.map(({ key, label, icon: Icon }) => {
                    const before = preset.cameraParams[key];
                    const after = tunedParams[key as keyof typeof tunedParams];
                    const changed = String(before) !== String(after);
                    return (
                      <div
                        key={`after-${key}`}
                        className={`flex items-center justify-between p-3 rounded-xl ${
                          changed ? "bg-hasselblad-500/10 border border-hasselblad-500/30" : "bg-white/[0.03]"
                        }`}
                      >
                        <div className="flex items-center gap-2 text-sm text-ink-300">
                          <Icon className="w-4 h-4" />
                          {label}
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="font-mono text-sm text-ink-400 line-through">{String(before)}</div>
                          <ArrowRight className="w-3 h-3 text-ink-400" />
                          <div className="font-mono text-sm font-bold text-hasselblad-400">{String(after)}</div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            <div className="mt-6 p-4 rounded-xl bg-hasselblad-500/5 border border-hasselblad-500/20">
              <p className="text-sm text-ink-200">
                <strong className="text-hasselblad-400">AI 建议说明：</strong>
                降低 ISO 以减少噪点，适当提高曝光补偿增强画面亮度。
                优化后的参数在保留哈苏色彩科学基础上，更适合当前拍摄环境。
              </p>
            </div>

            <button
              onClick={() => {
                setApplyMessage("AI 微调参数已应用");
                setShowTuningResult(false);
                setTimeout(() => setApplyMessage(null), 2500);
              }}
              className="btn-primary w-full mt-6"
            >
              <Check className="w-5 h-5" />
              应用调整后参数
            </button>
          </motion.div>
        )}

        {/* 预设说明 */}
        <div className="mt-12 grid md:grid-cols-2 gap-4">
          {preset.sections.map((section, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="card p-6"
            >
              <h3 className="font-display text-lg font-bold text-hasselblad-400 mb-3">
                {section.title}
              </h3>
              <p className="text-ink-200 leading-relaxed text-sm">{section.content}</p>
            </motion.div>
          ))}
        </div>
      </div>

      {/* 应用预设确认 */}
      {showApplyDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink-900/80 backdrop-blur-sm">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="card p-8 max-w-md w-full"
          >
            <h2 className="font-display text-2xl font-bold mb-3">应用预设</h2>
            <p className="text-ink-300 mb-6">
              将切换至相机应用并填充参数。是否继续？
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowApplyDialog(false)}
                className="btn-ghost flex-1"
              >
                <XIcon className="w-4 h-4" />
                取消
              </button>
              <button onClick={confirmApply} className="btn-primary flex-1">
                <Check className="w-4 h-4" />
                确认
              </button>
            </div>
          </motion.div>
        </div>
      )}

      {/* 权限申请 */}
      {showPermissionDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink-900/80 backdrop-blur-sm">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="card p-8 max-w-md w-full"
          >
            <h2 className="font-display text-2xl font-bold mb-3">需要无障碍服务权限</h2>
            <p className="text-ink-300 mb-6">
              请在系统设置中开启 OMaster 无障碍服务，以实现预设参数自动填充。
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowPermissionDialog(false)}
                className="btn-ghost flex-1"
              >
                取消
              </button>
              <button onClick={goToSettings} className="btn-primary flex-1">
                前往设置
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}
