import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Upload,
  Save,
  Share2,
  Settings,
  Eye,
  Sun,
  Contrast,
  Palette,
  Sliders,
  Zap,
  Check,
} from 'lucide-react';
import PageLayout from '../components/common/PageLayout';

interface PresetParams {
  exposure: number;
  contrast: number;
  highlights: number;
  shadows: number;
  whiteBalance: number;
  tint: number;
  saturation: number;
  clarity: number;
  vignette: number;
  sharpness: number;
}

export default function NewPresetEditorPage() {
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [params, setParams] = useState<PresetParams>({
    exposure: 0,
    contrast: 0,
    highlights: 0,
    shadows: 0,
    whiteBalance: 5000,
    tint: 0,
    saturation: 0,
    clarity: 0,
    vignette: 0,
    sharpness: 0,
  });
  const [presetName, setPresetName] = useState('');
  const [description, setDescription] = useState('');

  const updateParam = (key: keyof PresetParams, value: number) => {
    setParams((prev) => ({ ...prev, [key]: value }));
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-4 py-6">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="lg:w-1/2">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="bg-neutral-900/50 border border-neutral-800 rounded-3xl overflow-hidden"
            >
              <div className="aspect-[3/4] bg-neutral-800 relative">
                <div className="absolute inset-0 flex flex-col items-center justify-center text-neutral-500">
                  <Upload className="w-12 h-12 mb-3" />
                  <p className="text-body">拖放或点击上传照片</p>
                  <button className="mt-4 bg-neutral-800 hover:bg-neutral-700 text-white px-6 py-2.5 rounded-xl transition-colors">
                    选择照片
                  </button>
                </div>

                <div className="absolute bottom-4 left-4 right-4 flex gap-2">
                  <button className="flex-1 bg-black/50 backdrop-blur-sm text-white py-2.5 rounded-xl text-sm font-medium flex items-center justify-center gap-2">
                    <Eye className="w-4 h-4" />
                    对比视图
                  </button>
                  <button className="bg-black/50 backdrop-blur-sm text-white p-2.5 rounded-xl">
                    <Eye className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </motion.div>
          </div>

          <div className="lg:w-1/2">
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="space-y-6"
            >
              <div className="bg-neutral-900/50 border border-neutral-800 rounded-2xl p-5">
                <h3 className="text-h5 font-bold text-white mb-4 flex items-center gap-2">
                  <Sun className="w-5 h-5 text-oppo-orange" />
                  基础调整
                </h3>
                <div className="space-y-4">
                  <ParamSlider
                    label="曝光补偿"
                    value={params.exposure}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('exposure', v)}
                  />
                  <ParamSlider
                    label="对比度"
                    value={params.contrast}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('contrast', v)}
                  />
                  <ParamSlider
                    label="高光"
                    value={params.highlights}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('highlights', v)}
                  />
                  <ParamSlider
                    label="阴影"
                    value={params.shadows}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('shadows', v)}
                  />
                </div>
              </div>

              <div className="bg-neutral-900/50 border border-neutral-800 rounded-2xl p-5">
                <h3 className="text-h5 font-bold text-white mb-4 flex items-center gap-2">
                  <Palette className="w-5 h-5 text-oppo-blue" />
                  色彩调整
                </h3>
                <div className="space-y-4">
                  <ParamSlider
                    label="色温"
                    value={params.whiteBalance}
                    min={2000}
                    max={10000}
                    onChange={(v) => updateParam('whiteBalance', v)}
                  />
                  <ParamSlider
                    label="色调"
                    value={params.tint}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('tint', v)}
                  />
                  <ParamSlider
                    label="饱和度"
                    value={params.saturation}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('saturation', v)}
                  />
                </div>
              </div>

              <div className="bg-neutral-900/50 border border-neutral-800 rounded-2xl p-5">
                <h3 className="text-h5 font-bold text-white mb-4 flex items-center gap-2">
                  <Sliders className="w-5 h-5 text-oppo-green" />
                  效果
                </h3>
                <div className="space-y-4">
                  <ParamSlider
                    label="清晰度"
                    value={params.clarity}
                    min={-100}
                    max={100}
                    onChange={(v) => updateParam('clarity', v)}
                  />
                  <ParamSlider
                    label="锐化"
                    value={params.sharpness}
                    min={0}
                    max={100}
                    onChange={(v) => updateParam('sharpness', v)}
                  />
                  <ParamSlider
                    label="暗角"
                    value={params.vignette}
                    min={0}
                    max={100}
                    onChange={(v) => updateParam('vignette', v)}
                  />
                </div>
              </div>

              <div className="bg-neutral-900/50 border border-neutral-800 rounded-2xl p-5">
                <h3 className="text-h5 font-bold text-white mb-4">预设信息</h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm text-neutral-400 mb-1.5">预设名称</label>
                    <input
                      type="text"
                      value={presetName}
                      onChange={(e) => setPresetName(e.target.value)}
                      placeholder="给你的预设起个名字"
                      className="w-full bg-neutral-800 border border-neutral-700 text-white px-4 py-2.5 rounded-xl focus:outline-none focus:border-oppo-orange"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-neutral-400 mb-1.5">描述</label>
                    <textarea
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="描述一下这个预设的特点..."
                      rows={3}
                      className="w-full bg-neutral-800 border border-neutral-700 text-white px-4 py-2.5 rounded-xl focus:outline-none focus:border-oppo-orange resize-none"
                    />
                  </div>
                </div>
              </div>

              <div className="flex gap-3">
                <button className="flex-1 bg-neutral-800 hover:bg-neutral-700 text-white py-3.5 rounded-2xl font-semibold flex items-center justify-center gap-2 transition-colors">
                  <Save className="w-5 h-5" />
                  保存草稿
                </button>
                <button
                  onClick={() => setShowSubmitModal(true)}
                  className="flex-1 bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white py-3.5 rounded-2xl font-semibold flex items-center justify-center gap-2 hover:opacity-90 transition-opacity"
                >
                  <Share2 className="w-5 h-5" />
                  发布预设
                </button>
              </div>
            </motion.div>
          </div>
        </div>
      </div>

      {showSubmitModal && (
        <SubmitModal
          isOpen={showSubmitModal}
          onClose={() => setShowSubmitModal(false)}
          presetName={presetName}
        />
      )}
    </PageLayout>
  );
}

function ParamSlider({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}) {
  return (
    <div>
      <div className="flex justify-between mb-2">
        <span className="text-sm text-neutral-300">{label}</span>
        <span className="text-sm font-medium text-neutral-400">{value}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full h-2 bg-neutral-800 rounded-lg appearance-none cursor-pointer accent-oppo-orange"
      />
    </div>
  );
}

function SubmitModal({
  isOpen,
  onClose,
  presetName,
}: {
  isOpen: boolean;
  onClose: () => void;
  presetName: string;
}) {
  const [step, setStep] = useState(1);
  const [agreed, setAgreed] = useState(false);
  const [license, setLicense] = useState('cc-by-sa');

  if (!isOpen) return null;

  const handleSubmit = () => {
    setStep(3);
    setTimeout(() => {
      onClose();
    }, 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative bg-neutral-900 border border-neutral-800 rounded-3xl w-full max-w-lg overflow-hidden"
      >
        {step === 1 && (
          <div className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-h4 font-bold text-white">发布预设</h2>
              <button onClick={onClose} className="text-neutral-400 hover:text-white">
                ✕
              </button>
            </div>

            <div className="bg-neutral-800/50 rounded-xl p-4 mb-6">
              <p className="text-neutral-400 text-sm mb-1">即将发布</p>
              <p className="text-white font-semibold">{presetName || '未命名预设'}</p>
            </div>

            <div className="mb-6">
              <h3 className="text-body font-semibold text-white mb-3 flex items-center gap-2">
                <Check className="w-4 h-4 text-oppo-orange" />
                步骤 1：同意协议
              </h3>
              <label className="flex items-start gap-3 p-4 bg-neutral-800/50 rounded-xl cursor-pointer">
                <input
                  type="checkbox"
                  checked={agreed}
                  onChange={(e) => setAgreed(e.target.checked)}
                  className="mt-1 accent-oppo-orange"
                />
                <div className="text-sm">
                  <p className="text-white mb-1">我已阅读并同意</p>
                  <p className="text-neutral-400">
                    《原创内容贡献协议》和《用户内容发布规范》
                  </p>
                </div>
              </label>
            </div>

            <button
              onClick={() => agreed && setStep(2)}
              disabled={!agreed}
              className={`w-full py-3.5 rounded-2xl font-semibold transition-all ${
                agreed
                  ? 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white'
                  : 'bg-neutral-800 text-neutral-500 cursor-not-allowed'
              }`}
            >
              下一步
            </button>
          </div>
        )}

        {step === 2 && (
          <div className="p-6">
            <div className="flex items-center justify-between mb-6">
              <button onClick={() => setStep(1)} className="text-neutral-400 hover:text-white">
                ←
              </button>
              <h2 className="text-h4 font-bold text-white">选择开源协议</h2>
              <div className="w-6" />
            </div>

            <div className="space-y-3 mb-6">
              {[
                {
                  id: 'cc-by-sa',
                  name: 'CC BY-SA 4.0',
                  desc: '署名-相同方式共享，允许商业使用',
                },
                {
                  id: 'cc-by',
                  name: 'CC BY 4.0',
                  desc: '署名，允许商业使用和修改',
                },
                {
                  id: 'cc-by-nc',
                  name: 'CC BY-NC 4.0',
                  desc: '署名-非商业使用',
                },
              ].map((opt) => (
                <label
                  key={opt.id}
                  className={`flex items-center gap-3 p-4 rounded-xl cursor-pointer transition-all ${
                    license === opt.id
                      ? 'bg-oppo-orange/10 border border-oppo-orange/50'
                      : 'bg-neutral-800/50 border border-transparent hover:bg-neutral-800'
                  }`}
                >
                  <input
                    type="radio"
                    name="license"
                    value={opt.id}
                    checked={license === opt.id}
                    onChange={(e) => setLicense(e.target.value)}
                    className="accent-oppo-orange"
                  />
                  <div>
                    <p className="text-white font-semibold">{opt.name}</p>
                    <p className="text-neutral-400 text-sm">{opt.desc}</p>
                  </div>
                </label>
              ))}
            </div>

            <button
              onClick={handleSubmit}
              className="w-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-white py-3.5 rounded-2xl font-semibold flex items-center justify-center gap-2"
            >
              <Zap className="w-5 h-5" />
              立即发布
            </button>
          </div>
        )}

        {step === 3 && (
          <div className="p-8 text-center">
            <div className="w-20 h-20 bg-gradient-to-br from-oppo-orange to-hasselblad-orange rounded-full flex items-center justify-center mx-auto mb-6">
              <Check className="w-10 h-10 text-white" />
            </div>
            <h2 className="text-h3 font-bold text-white mb-2">发布成功！</h2>
            <p className="text-neutral-400 mb-6">
              你的预设已进入审核队列<br />
              审核通过后将自动同步到社区
            </p>
          </div>
        )}
      </motion.div>
    </div>
  );
}
