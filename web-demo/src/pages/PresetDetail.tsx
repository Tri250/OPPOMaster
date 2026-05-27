import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Heart, Download, Settings, Image, Star } from 'lucide-react';
import { usePresetStore } from '../store/usePresetStore';
import FluidCloudCapsule from '../components/FluidCloudCapsule';

const PresetDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { getPresetById, toggleFavorite } = usePresetStore();
  const [showSplitView, setShowSplitView] = useState(false);
  const [selectedParams, setSelectedParams] = useState({
    contrast: 1.0,
    saturation: 1.0,
    vignette: 0.0,
  });

  const preset = getPresetById(id || '');

  if (!preset) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-2">预设不存在</h2>
          <button
            onClick={() => navigate('/')}
            className="text-blue-600 font-semibold hover:underline"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50 to-purple-50">
      <div className="pt-24 pb-16">
        {/* Header */}
        <div className="max-w-7xl mx-auto px-4 mb-8">
          <motion.button
            initial={{ x: -20, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            onClick={() => navigate('/')}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-6"
          >
            <ArrowLeft className="w-5 h-5" />
            <span className="font-medium">返回</span>
          </motion.button>

          <div className="flex flex-col lg:flex-row gap-8 items-start">
            {/* Image Preview */}
            <motion.div
              initial={{ x: -50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="flex-1"
            >
              <div className="relative rounded-3xl overflow-hidden shadow-2xl shadow-gray-300/50">
                <div className="relative aspect-[4/3]">
                  <img
                    src={preset.coverPath}
                    alt={preset.name}
                    className="w-full h-full object-cover"
                  />

                  {/* Split View Toggle */}
                  <button
                    onClick={() => setShowSplitView(!showSplitView)}
                    className="absolute bottom-4 left-4 bg-white/95 backdrop-blur-sm px-4 py-2 rounded-xl font-semibold text-gray-800 shadow-lg hover:bg-white transition-all"
                  >
                    {showSplitView ? '关闭分屏' : '分屏对比'}
                  </button>

                  {/* Split View Overlay */}
                  {showSplitView && (
                    <div className="absolute inset-0 flex">
                      <div className="w-1/2 relative">
                        <div className="absolute inset-0 bg-black/20 flex items-center justify-center">
                          <span className="text-white font-bold text-lg">原图</span>
                        </div>
                        <img
                          src={preset.coverPath}
                          alt=""
                          className="w-full h-full object-cover"
                          style={{ filter: 'grayscale(50%)' }}
                        />
                      </div>
                      <div className="w-1/2">
                        <div className="absolute top-4 right-4 bg-white/95 backdrop-blur-sm px-4 py-2 rounded-xl font-bold text-gray-800">
                          预设效果
                        </div>
                        <img
                          src={preset.coverPath}
                          alt=""
                          className="w-full h-full object-cover"
                        />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </motion.div>

            {/* Info Sidebar */}
            <motion.div
              initial={{ x: 50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.3 }}
              className="w-full lg:w-[400px]"
            >
              <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
                <div className="flex items-start justify-between mb-6">
                  <div>
                    <h1 className="text-2xl font-bold text-gray-900 mb-2">
                      {preset.name}
                    </h1>
                    <p className="text-gray-600">{preset.deviceModel}</p>
                  </div>
                  <button
                    onClick={() => toggleFavorite(preset.id)}
                    className={cn(
                      'p-3 rounded-2xl transition-all',
                      preset.isFavorite
                        ? 'bg-pink-100 text-pink-600'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    )}
                  >
                    <Heart className={cn('w-6 h-6', preset.isFavorite && 'fill-current')} />
                  </button>
                </div>

                <div className="flex items-center space-x-4 mb-8">
                  <div className="flex items-center space-x-1">
                    <Star className="w-5 h-5 text-yellow-400 fill-yellow-400" />
                    <span className="font-bold text-gray-900">{preset.rating}</span>
                  </div>
                  <div className="text-gray-500 text-sm">
                    {preset.usageCount.toLocaleString()} 次使用
                  </div>
                </div>

                {/* Sections */}
                <div className="space-y-4 mb-8">
                  {preset.sections.map((section, idx) => (
                    <div key={idx} className="bg-gray-50 rounded-2xl p-4">
                      <h3 className="font-bold text-gray-900 mb-2">{section.title}</h3>
                      <p className="text-gray-600 text-sm">{section.content}</p>
                    </div>
                  ))}
                </div>

                {/* Action Buttons */}
                <div className="space-y-3">
                  <button className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white font-bold py-4 px-6 rounded-2xl shadow-lg shadow-blue-200 hover:shadow-xl hover:shadow-blue-300 transition-all flex items-center justify-center space-x-2">
                    <Download className="w-5 h-5" />
                    <span>立即使用</span>
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        </div>

        {/* Camera Parameters */}
        <motion.div
          initial={{ y: 50, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.4 }}
          className="max-w-7xl mx-auto px-4"
        >
          <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-900 mb-8 flex items-center space-x-3">
              <Settings className="w-7 h-7 text-purple-600" />
              <span>相机参数</span>
            </h2>

            <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <ParamCard
                label="ISO"
                value={preset.cameraParams.iso.toString()}
                icon="📷"
              />
              <ParamCard
                label="快门"
                value={preset.cameraParams.shutter}
                icon="⏱️"
              />
              <ParamCard
                label="曝光补偿"
                value={preset.cameraParams.ev}
                icon="☀️"
              />
              <ParamCard
                label="白平衡"
                value={preset.cameraParams.wb}
                icon="🎨"
              />
            </div>

            {/* Slider Controls */}
            <div className="grid md:grid-cols-3 gap-8 pt-8 border-t border-gray-100">
              <SliderControl
                label="对比度"
                value={preset.cameraParams.contrast}
                min={0.5}
                max={2.0}
              />
              <SliderControl
                label="饱和度"
                value={preset.cameraParams.saturation}
                min={0.0}
                max={2.0}
              />
              <SliderControl
                label="暗角"
                value={preset.cameraParams.vignette}
                min={0.0}
                max={1.0}
              />
            </div>
          </div>
        </motion.div>
      </div>

      {/* Floating Capsule */}
      <FluidCloudCapsule preset={preset} />
    </div>
  );
};

const ParamCard = ({ label, value, icon }: { label: string; value: string; icon: string }) => (
  <div className="bg-gradient-to-br from-gray-50 to-gray-100 rounded-2xl p-6 text-center">
    <div className="text-3xl mb-3">{icon}</div>
    <p className="text-gray-600 text-sm mb-1">{label}</p>
    <p className="text-2xl font-bold text-gray-900">{value}</p>
  </div>
);

const SliderControl = ({ label, value, min, max }: { label: string; value: number; min: number; max: number }) => (
  <div>
    <div className="flex justify-between items-center mb-3">
      <label className="font-semibold text-gray-900">{label}</label>
      <span className="text-sm font-bold text-purple-600">{value.toFixed(1)}</span>
    </div>
    <div className="relative h-3 bg-gray-200 rounded-full overflow-hidden">
      <div
        className="absolute left-0 top-0 h-full bg-gradient-to-r from-blue-500 to-purple-600 rounded-full"
        style={{ width: `${((value - min) / (max - min)) * 100}%` }}
      />
    </div>
  </div>
);

function cn(...classes: any[]) {
  return classes.filter(Boolean).join(' ');
}

export default PresetDetail;
