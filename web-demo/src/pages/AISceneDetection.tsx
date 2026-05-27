import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Zap, Camera, Users, Mountain, Utensils, Moon, ZoomIn, Sparkles, CheckCircle2 } from 'lucide-react';
import { SCENE_TYPES } from '../data/presets';
import { usePresetStore } from '../store/usePresetStore';

const AISceneDetection = () => {
  const navigate = useNavigate();
  const { presets } = usePresetStore();
  const [detectedScene, setDetectedScene] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const handleAnalyze = () => {
    setIsAnalyzing(true);
    setTimeout(() => {
      setDetectedScene('landscape');
      setIsAnalyzing(false);
    }, 2000);
  };

  const recommendedPresets = presets.filter(p =>
    p.cameraParams.sceneTags.includes('landscape')
  ).slice(0, 3);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50 to-purple-50">
      <div className="pt-24 pb-16">
        <div className="max-w-7xl mx-auto px-4">
          <motion.button
            initial={{ x: -20, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            onClick={() => navigate('/')}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-900 mb-8"
          >
            <ArrowLeft className="w-5 h-5" />
            <span className="font-medium">返回</span>
          </motion.button>

          {/* Header */}
          <motion.div
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            className="text-center mb-12"
          >
            <div className="inline-flex items-center space-x-2 bg-gradient-to-r from-blue-100 to-purple-100 px-6 py-3 rounded-full mb-6">
              <Zap className="w-6 h-6 text-blue-600 animate-pulse" />
              <span className="font-bold text-blue-700">AI 场景识别</span>
            </div>
            <h1 className="text-5xl font-bold text-gray-900 mb-4">
              智能场景检测
            </h1>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              实时分析拍摄场景，自动匹配最佳预设，让每张照片都有完美的色彩表现
            </p>
          </motion.div>

          <div className="grid lg:grid-cols-2 gap-12">
            {/* Left - Analysis Panel */}
            <motion.div
              initial={{ x: -50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.2 }}
            >
              <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
                <div className="relative aspect-square bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl overflow-hidden mb-8">
                  {/* Sample Image */}
                  <img
                    src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80"
                    alt="Landscape"
                    className="w-full h-full object-cover"
                  />

                  {/* Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent flex items-end justify-center pb-8">
                    {isAnalyzing ? (
                      <div className="text-center">
                        <div className="w-16 h-16 border-4 border-blue-400 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                        <p className="text-white font-bold text-lg">正在分析...</p>
                      </div>
                    ) : detectedScene ? (
                      <div className="text-center">
                        <CheckCircle2 className="w-16 h-16 text-green-400 mx-auto mb-4" />
                        <p className="text-white font-bold text-lg">检测成功！</p>
                      </div>
                    ) : (
                      <button
                        onClick={handleAnalyze}
                        className="bg-white text-gray-900 px-8 py-4 rounded-2xl font-bold shadow-2xl hover:scale-105 transition-transform flex items-center space-x-3"
                      >
                        <Camera className="w-6 h-6" />
                        <span>开始分析</span>
                      </button>
                    )}
                  </div>
                </div>

                {/* Scene Types Grid */}
                <h3 className="text-xl font-bold text-gray-900 mb-6">场景类型</h3>
                <div className="grid grid-cols-2 gap-4">
                  {SCENE_TYPES.map((scene, index) => (
                    <motion.div
                      key={scene.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.3 + index * 0.1 }}
                      whileHover={{ scale: 1.05 }}
                      className={cn(
                        'p-6 rounded-2xl border-2 cursor-pointer transition-all',
                        detectedScene === scene.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 bg-white hover:border-gray-300'
                      )}
                    >
                      <div className={cn(
                        'w-12 h-12 rounded-xl bg-gradient-to-br',
                        scene.color,
                        'flex items-center justify-center mb-4'
                      )}>
                        <scene.icon className="w-6 h-6 text-white" />
                      </div>
                      <h4 className="font-bold text-gray-900 mb-2">{scene.name}</h4>
                      <p className="text-sm text-gray-600">{scene.description}</p>
                    </motion.div>
                  ))}
                </div>
              </div>
            </motion.div>

            {/* Right - Recommendations */}
            <motion.div
              initial={{ x: 50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
            >
              <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
                <div className="flex items-center space-x-3 mb-8">
                  <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center">
                    <Sparkles className="w-7 h-7 text-white" />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-gray-900">推荐预设</h3>
                    <p className="text-gray-600">基于场景智能匹配</p>
                  </div>
                </div>

                <div className="space-y-6">
                  {recommendedPresets.map((preset, index) => (
                    <motion.div
                      key={preset.id}
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.5 + index * 0.1 }}
                      whileHover={{ x: 4 }}
                      className="flex items-center space-x-4 p-4 bg-gray-50 rounded-2xl hover:bg-gray-100 transition-colors"
                    >
                      <img
                        src={preset.coverPath}
                        alt={preset.name}
                        className="w-20 h-20 rounded-xl object-cover"
                      />
                      <div className="flex-1 min-w-0">
                        <h4 className="font-bold text-gray-900 truncate">{preset.name}</h4>
                        <div className="flex items-center space-x-2 text-sm text-gray-600">
                          <span>ISO {preset.cameraParams.iso}</span>
                          <span>•</span>
                          <span>{preset.cameraParams.shutter}</span>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-lg font-bold text-blue-600">{preset.rating}</p>
                        <p className="text-xs text-gray-500">{preset.usageCount} 次使用</p>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
};

function cn(...classes: any[]) {
  return classes.filter(Boolean).join(' ');
}

export default AISceneDetection;
