import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Palette, Upload, CheckCircle2, Sparkles } from 'lucide-react';
import { usePresetStore } from '../store/usePresetStore';

const ColorAnalysis = () => {
  const navigate = useNavigate();
  const { presets } = usePresetStore();
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [hasResult, setHasResult] = useState(false);

  const extractedColors = [
    '#2563EB',
    '#7C3AED',
    '#DB2777',
    '#F59E0B',
    '#10B981',
  ];

  const matchedPresets = presets.slice(0, 3);

  const handleAnalyze = () => {
    setIsAnalyzing(true);
    setTimeout(() => {
      setIsAnalyzing(false);
      setHasResult(true);
    }, 2000);
  };

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
            <div className="inline-flex items-center space-x-2 bg-gradient-to-r from-purple-100 to-pink-100 px-6 py-3 rounded-full mb-6">
              <Palette className="w-6 h-6 text-purple-600 animate-pulse" />
              <span className="font-bold text-purple-700">色调分析</span>
            </div>
            <h1 className="text-5xl font-bold text-gray-900 mb-4">
              智能色调提取
            </h1>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto">
              导入照片自动分析色彩特征，智能匹配最佳预设，快速获得你想要的色调风格
            </p>
          </motion.div>

          <div className="grid lg:grid-cols-2 gap-12">
            {/* Left - Upload & Analysis */}
            <motion.div
              initial={{ x: -50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.2 }}
            >
              <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
                {/* Upload Area */}
                {!hasResult && !isAnalyzing && (
                  <div
                    onClick={handleAnalyze}
                    className="relative aspect-square bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl border-2 border-dashed border-gray-300 flex flex-col items-center justify-center cursor-pointer hover:border-purple-400 hover:bg-gradient-to-br hover:from-purple-50 hover:to-pink-50 transition-all"
                  >
                    <div className="w-20 h-20 bg-gradient-to-br from-purple-500 to-pink-500 rounded-2xl flex items-center justify-center mb-6">
                      <Upload className="w-10 h-10 text-white" />
                    </div>
                    <p className="text-xl font-bold text-gray-900 mb-2">点击上传照片</p>
                    <p className="text-gray-600">支持 JPG、PNG、HEIC 格式</p>
                  </div>
                )}

                {/* Analyzing State */}
                {isAnalyzing && (
                  <div className="relative aspect-square bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl flex flex-col items-center justify-center">
                    <div className="w-24 h-24 border-4 border-purple-400 border-t-transparent rounded-full animate-spin mb-6" />
                    <p className="text-2xl font-bold text-gray-900 mb-2">正在分析</p>
                    <p className="text-gray-600">提取色彩特征中...</p>
                  </div>
                )}

                {/* Results State */}
                {hasResult && !isAnalyzing && (
                  <div className="space-y-8">
                    <div className="relative aspect-square rounded-2xl overflow-hidden">
                      <img
                        src="https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80"
                        alt="Analyzed"
                        className="w-full h-full object-cover"
                      />
                      <div className="absolute top-4 right-4 bg-green-500 text-white px-4 py-2 rounded-xl font-bold flex items-center space-x-2">
                        <CheckCircle2 className="w-5 h-5" />
                        <span>分析完成</span>
                      </div>
                    </div>

                    {/* Extracted Colors */}
                    <div>
                      <h3 className="text-xl font-bold text-gray-900 mb-4">提取的色调</h3>
                      <div className="flex space-x-3">
                        {extractedColors.map((color, index) => (
                          <motion.div
                            key={color}
                            initial={{ opacity: 0, scale: 0.8 }}
                            animate={{ opacity: 1, scale: 1 }}
                            transition={{ delay: 0.5 + index * 0.1 }}
                            className="w-16 h-16 rounded-2xl shadow-lg cursor-pointer hover:scale-110 transition-transform"
                            style={{ backgroundColor: color }}
                          />
                        ))}
                      </div>
                    </div>

                    {/* Color Histogram */}
                    <div className="bg-gray-50 rounded-2xl p-6">
                      <h4 className="font-bold text-gray-900 mb-4">色彩分布</h4>
                      <div className="flex items-end space-x-2 h-32">
                        {[80, 60, 70, 45, 55, 90, 75].map((height, index) => (
                          <motion.div
                            key={index}
                            initial={{ height: 0 }}
                            animate={{ height: `${height}%` }}
                            transition={{ delay: 0.7 + index * 0.1 }}
                            className="flex-1 bg-gradient-to-t from-blue-500 to-purple-500 rounded-t-lg"
                          />
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </motion.div>

            {/* Right - Matched Presets */}
            <motion.div
              initial={{ x: 50, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
            >
              <div className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
                <div className="flex items-center space-x-3 mb-8">
                  <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-pink-600 rounded-2xl flex items-center justify-center">
                    <Sparkles className="w-7 h-7 text-white" />
                  </div>
                  <div>
                    <h3 className="text-2xl font-bold text-gray-900">匹配预设</h3>
                    <p className="text-gray-600">基于色彩特征智能匹配</p>
                  </div>
                </div>

                {hasResult ? (
                  <div className="space-y-6">
                    {matchedPresets.map((preset, index) => (
                      <motion.div
                        key={preset.id}
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.6 + index * 0.1 }}
                        whileHover={{ x: 4 }}
                        className="flex items-center space-x-4 p-4 bg-gray-50 rounded-2xl hover:bg-gray-100 transition-colors"
                      >
                        <img
                          src={preset.coverPath}
                          alt={preset.name}
                          className="w-24 h-24 rounded-2xl object-cover"
                        />
                        <div className="flex-1 min-w-0">
                          <h4 className="font-bold text-gray-900 truncate">{preset.name}</h4>
                          <div className="flex items-center space-x-2 text-sm text-gray-600 mt-1">
                            <span>匹配度</span>
                            <span className="font-bold text-purple-600">{95 - index * 5}%</span>
                          </div>
                          <div className="mt-2 h-2 bg-gray-200 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-gradient-to-r from-blue-500 to-purple-600 rounded-full"
                              style={{ width: `${95 - index * 5}%` }}
                            />
                          </div>
                        </div>
                      </motion.div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-16 text-gray-500">
                    <Palette className="w-16 h-16 mx-auto mb-4 opacity-50" />
                    <p className="text-lg">上传照片后将显示匹配结果</p>
                  </div>
                )}

                {/* Generate Custom Preset */}
                {hasResult && (
                  <motion.button
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 1 }}
                    className="w-full mt-8 bg-gradient-to-r from-purple-600 to-pink-600 text-white font-bold py-4 px-6 rounded-2xl shadow-lg shadow-purple-200 hover:shadow-xl hover:shadow-purple-300 transition-all flex items-center justify-center space-x-2"
                  >
                    <Sparkles className="w-5 h-5" />
                    <span>生成自定义预设</span>
                  </motion.button>
                )}
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ColorAnalysis;
