import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  Sparkles,
  Camera,
  Wand2,
  Layers,
  Aperture,
  ShieldCheck,
  Cpu,
  Palette,
  ScanLine,
} from "lucide-react";
import AnimatedCounter from "../components/AnimatedCounter";
import PresetCard from "../components/PresetCard";
import { presets } from "../data/mock";

const features = [
  {
    icon: Palette,
    title: "哈苏 HNCS 色彩科学",
    desc: "将 X2D 真实色彩还原搬进你的手机。每一帧都是哈苏校色，每一束光都被精确计算。",
    color: "from-hasselblad-500/20 to-hasselblad-500/0",
    glow: "shadow-[0_0_60px_-15px_rgba(255,107,0,0.5)]",
  },
  {
    icon: ScanLine,
    title: "AI 场景识别",
    desc: "1.6 秒内识别 8 大场景，自动推荐最匹配的预设。毫秒级响应，让灵感不被等待。",
    color: "from-amber-500/20 to-amber-500/0",
    glow: "shadow-[0_0_60px_-15px_rgba(212,165,116,0.5)]",
  },
  {
    icon: Wand2,
    title: "无障碍智能填充",
    desc: "一键将预设参数注入系统相机应用，参数精度 95%+，拍摄从此无需手动调节。",
    color: "from-emerald-500/20 to-emerald-500/0",
    glow: "shadow-[0_0_60px_-15px_rgba(16,185,129,0.5)]",
  },
];

const stats = [
  { value: 50, suffix: "+", label: "哈苏认证预设" },
  { value: 8, suffix: "", label: "AI 识别场景" },
  { value: 95, suffix: "%+", label: "参数填充准确率" },
  { value: 1.6, suffix: "s", label: "AI 分析响应", decimals: 1 },
];

const heroImages = [
  "https://picsum.photos/seed/hero-1/600/800",
  "https://picsum.photos/seed/hero-2/600/800",
  "https://picsum.photos/seed/hero-3/600/800",
  "https://picsum.photos/seed/hero-4/600/800",
];

const topPresets = presets.slice(0, 3);

export default function Home() {
  return (
    <div className="relative">
      {/* HERO */}
      <section className="relative min-h-screen flex items-center overflow-hidden pt-16">
        {/* 背景层 */}
        <div className="absolute inset-0 gradient-mesh" />
        <div className="absolute inset-0 dotted-grid opacity-40" />
        <div
          className="absolute inset-0 opacity-30"
          style={{
            background:
              "radial-gradient(ellipse 60% 50% at 50% 40%, rgba(255,107,0,0.25), transparent 70%)",
          }}
        />

        <div className="relative max-w-7xl mx-auto px-6 py-20 grid lg:grid-cols-2 gap-12 items-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full chip chip-active mb-6">
              <span className="w-1.5 h-1.5 rounded-full bg-hasselblad-500 animate-pulse" />
              <span className="text-[11px] font-semibold tracking-widest uppercase">Hasselblad × OPPO</span>
            </div>

            <h1 className="font-display text-5xl md:text-6xl lg:text-7xl font-bold leading-[1.05] mb-6">
              让摄影成为
              <br />
              <span className="gradient-text italic">决定性的瞬间</span>
            </h1>

            <p className="text-ink-200 text-lg leading-relaxed max-w-xl mb-8">
              哈苏 X2D 色彩科学，AI 场景识别，专业参数微调。
              为 OPPO Find X 系列与一加设备打造的影像伙伴。
            </p>

            <div className="flex flex-wrap items-center gap-4">
              <Link to="/presets" className="btn-primary">
                <Sparkles className="w-4 h-4" />
                探索预设
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link to="/scene-detection" className="btn-ghost">
                <ScanLine className="w-4 h-4" />
                AI 场景检测演示
              </Link>
            </div>

            <div className="mt-10 flex items-center gap-6 text-xs text-ink-300">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-hasselblad-500" />
                <span>哈苏官方合作</span>
              </div>
              <div className="flex items-center gap-2">
                <Cpu className="w-4 h-4 text-hasselblad-500" />
                <span>本地 ML 模型</span>
              </div>
              <div className="flex items-center gap-2">
                <Aperture className="w-4 h-4 text-hasselblad-500" />
                <span>v3.0 全新版本</span>
              </div>
            </div>
          </motion.div>

          {/* 手机 mockup */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95, rotateY: 10 }}
            animate={{ opacity: 1, scale: 1, rotateY: 0 }}
            transition={{ duration: 1, delay: 0.2 }}
            className="relative perspective-1000"
          >
            <PhoneMockup />
          </motion.div>
        </div>

        {/* 向下滚动指示 */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.5 }}
          className="absolute bottom-8 left-1/2 -translate-x-1/2"
        >
          <div className="w-6 h-10 rounded-full border-2 border-ink-400 flex items-start justify-center p-1.5">
            <motion.div
              animate={{ y: [0, 12, 0] }}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="w-1 h-2 rounded-full bg-hasselblad-500"
            />
          </div>
        </motion.div>
      </section>

      {/* 数据统计 */}
      <section className="relative py-24 border-y border-white/[0.06]">
        <div className="max-w-7xl mx-auto px-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, i) => (
              <motion.div
                key={stat.label}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="text-center md:text-left"
              >
                <div className="font-display text-5xl md:text-6xl font-bold gradient-text mb-2">
                  <AnimatedCounter
                    end={stat.value}
                    suffix={stat.suffix}
                    decimals={stat.decimals || 0}
                  />
                </div>
                <div className="text-ink-300 text-sm tracking-widest uppercase">
                  {stat.label}
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* 核心特性 */}
      <section className="relative py-32">
        <div className="max-w-7xl mx-auto px-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-20"
          >
            <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
              Core Features
            </span>
            <h2 className="font-display text-4xl md:text-5xl font-bold mt-3 mb-4">
              重新定义 <span className="gradient-text italic">手机摄影</span>
            </h2>
            <p className="text-ink-300 max-w-2xl mx-auto leading-relaxed">
              从色彩科学到 AI 算法，从硬件识别到参数注入，每一项功能都经过哈苏影像实验室的严苛标准。
            </p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-6">
            {features.map((feature, i) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-100px" }}
                transition={{ delay: i * 0.15 }}
                className={`group relative card p-8 hover:border-hasselblad-500/30 transition-all duration-500 overflow-hidden`}
              >
                <div
                  className={`absolute -top-20 -right-20 w-40 h-40 rounded-full bg-gradient-to-br ${feature.color} blur-2xl group-hover:scale-150 transition-transform duration-700`}
                />

                <div className={`relative w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} ${feature.glow} flex items-center justify-center mb-6 border border-white/10`}>
                  <feature.icon className="w-7 h-7 text-hasselblad-400" />
                </div>

                <h3 className="relative font-display text-2xl font-bold text-ink-50 mb-3">
                  {feature.title}
                </h3>
                <p className="relative text-ink-300 leading-relaxed">{feature.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* 精选预设 */}
      <section className="relative py-32 bg-ink-900/40 border-y border-white/[0.06]">
        <div className="max-w-7xl mx-auto px-6">
          <div className="flex items-end justify-between mb-12 flex-wrap gap-6">
            <div>
              <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
                Featured Presets
              </span>
              <h2 className="font-display text-4xl md:text-5xl font-bold mt-3">
                哈苏认证 <span className="gradient-text italic">精选预设</span>
              </h2>
            </div>
            <Link
              to="/presets"
              className="inline-flex items-center gap-2 text-sm text-ink-200 hover:text-hasselblad-400 transition-colors"
            >
              查看全部
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {topPresets.map((preset, i) => (
              <PresetCard key={preset.id} preset={preset} index={i} showRank />
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="relative py-32 overflow-hidden">
        <div className="absolute inset-0 gradient-mesh opacity-50" />
        <div className="relative max-w-4xl mx-auto px-6 text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <Layers className="w-12 h-12 text-hasselblad-500 mx-auto mb-6" />
            <h2 className="font-display text-4xl md:text-5xl font-bold mb-6">
              准备好 <span className="gradient-text italic">拍出大片</span> 了吗？
            </h2>
            <p className="text-ink-300 text-lg leading-relaxed mb-10 max-w-2xl mx-auto">
              下载 OMaster，连接你的 OPPO/一加/真我设备，
              开启属于你的哈苏影像之旅。
            </p>
            <Link to="/presets" className="btn-primary text-base px-8 py-4">
              <Camera className="w-5 h-5" />
              立即开始
            </Link>
          </motion.div>
        </div>
      </section>
    </div>
  );
}

function PhoneMockup() {
  return (
    <div className="relative mx-auto" style={{ maxWidth: 380 }}>
      {/* 光晕 */}
      <div className="absolute -inset-20 bg-hasselblad-500/20 blur-[100px] rounded-full animate-breathe" />

      {/* 手机外框 */}
      <div className="relative aspect-[9/19.5] rounded-[3rem] bg-gradient-to-br from-ink-700 to-ink-900 p-2 shadow-2xl">
        <div className="absolute inset-0 rounded-[3rem] ring-1 ring-white/10" />
        <div className="relative w-full h-full rounded-[2.5rem] overflow-hidden bg-ink-900">
          {/* 状态栏 */}
          <div className="flex items-center justify-between px-6 py-2.5 text-[10px] text-ink-300">
            <span className="font-semibold">9:41</span>
            <div className="w-20 h-5 rounded-full bg-ink-800" />
            <div className="flex items-center gap-1">
              <span>5G</span>
              <span>100%</span>
            </div>
          </div>

          {/* APP 内容 */}
          <div className="relative h-[calc(100%-2.5rem)] flex flex-col">
            <div className="px-4 pt-2 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-hasselblad-500 flex items-center justify-center">
                  <Camera className="w-3.5 h-3.5 text-ink-900" strokeWidth={2.5} />
                </div>
                <div className="flex flex-col leading-none">
                  <span className="font-display text-xs font-bold text-ink-50">哈苏影像</span>
                  <span className="text-[8px] text-ink-400 tracking-wider">OMASTER</span>
                </div>
              </div>
            </div>

            <div className="flex-1 px-3 space-y-2.5 overflow-hidden">
              {heroImages.map((src, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.6 + i * 0.15 }}
                  className="relative h-[26%] rounded-2xl overflow-hidden ring-1 ring-white/5"
                >
                  <img src={src} alt="" className="w-full h-full object-cover" />
                  <div className="absolute inset-0 bg-gradient-to-t from-ink-900 via-ink-900/40 to-transparent" />
                  <div className="absolute bottom-2 left-2 right-2 flex items-end justify-between">
                    <div>
                      <div className="text-[8px] text-hasselblad-400 font-bold tracking-wider">HNCS</div>
                      <div className="text-[10px] text-ink-50 font-medium line-clamp-1">
                        哈苏 {["人像", "夜景", "风景", "街拍"][i]}
                      </div>
                    </div>
                    <div className="px-1.5 py-0.5 rounded bg-hasselblad-500 text-ink-900 text-[8px] font-bold">
                      {(4.5 + i * 0.1).toFixed(1)}
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>

            <div className="px-4 py-3 border-t border-white/5">
              <div className="flex items-center justify-around">
                {["预设", "检测", "微调", "我的"].map((label, i) => (
                  <div
                    key={label}
                    className={`text-[10px] font-medium ${
                      i === 0 ? "text-hasselblad-400" : "text-ink-400"
                    }`}
                  >
                    {label}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
