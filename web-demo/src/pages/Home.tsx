import React, { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Zap, Camera, Palette, Star, Users, Download, RefreshCw, Loader2 } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import PresetCard from '../components/PresetCard';
import { usePresetStore } from '../store/usePresetStore';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const Home = () => {
  const { presets, isLoading, error, loadPresets } = usePresetStore();
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    loadPresets();
  }, [loadPresets]);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await loadPresets();
    } finally {
      setIsRefreshing(false);
    }
  };

  const stats = [
    { number: presets.length.toString(), label: '预设总数', icon: Star, color: 'from-yellow-400 to-orange-500' },
    { number: '56.7K', label: '活跃用户', icon: Users, color: 'from-blue-500 to-purple-600' },
    { number: '2.3M', label: '下载次数', icon: Download, color: 'from-pink-500 to-rose-600' },
  ];

  const features = [
    {
      title: 'AI 智能推荐',
      description: '实时分析场景，自动匹配最佳预设',
      icon: Zap,
      color: 'from-blue-500 to-cyan-500',
    },
    {
      title: '流体云胶囊',
      description: 'ColorOS 原生体验，悬浮窗式快速切换',
      icon: Camera,
      color: 'from-purple-500 to-pink-500',
    },
    {
      title: '色调分析',
      description: '导入照片自动提取色彩，智能匹配预设',
      icon: Palette,
      color: 'from-emerald-500 to-teal-500',
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50 to-purple-50">
      {/* Hero Section */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="pt-32 pb-20 px-4"
      >
        <div className="max-w-7xl mx-auto text-center">
          <motion.div
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="inline-flex items-center space-x-2 bg-gradient-to-r from-blue-100 to-purple-100 px-4 py-2 rounded-full mb-6"
          >
            <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
            <span className="text-sm font-semibold text-gray-700">
              🎨 ColorOS 16 原生体验
            </span>
          </motion.div>

          <motion.h1
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="text-6xl md:text-7xl font-bold mb-6"
          >
            <span className="bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
              专业相机预设
            </span>
            <br />
            <span className="text-gray-800">
              触手可及
            </span>
          </motion.h1>

          <motion.p
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.4 }}
            className="text-xl text-gray-600 max-w-2xl mx-auto mb-10"
          >
            基于 OPPO 哈苏影像系统，AI 场景识别与实时色调分析，
            让每一张照片都有大师级的表现
          </motion.p>
        </div>
      </motion.div>

      {/* Stats */}
      <div className="max-w-7xl mx-auto px-4 mb-20">
        <div className="grid md:grid-cols-3 gap-6">
          {stats.map((stat, index) => (
            <motion.div
              key={stat.label}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 + index * 0.1 }}
              className="bg-white rounded-3xl p-8 shadow-xl shadow-gray-200/50 border border-gray-100"
            >
              <div className={cn('w-14 h-14 rounded-2xl bg-gradient-to-br', stat.color, 'flex items-center justify-center mb-4')}>
                <stat.icon className="w-7 h-7 text-white" />
              </div>
              <p className="text-4xl font-bold text-gray-900 mb-2">{stat.number}</p>
              <p className="text-gray-600 font-medium">{stat.label}</p>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Features */}
      <div className="max-w-7xl mx-auto px-4 mb-20">
        <div className="grid md:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 + index * 0.1 }}
              whileHover={{ y: -4 }}
              className="bg-white rounded-3xl p-8 shadow-lg shadow-gray-200/50 border border-gray-100"
            >
              <div className={cn('w-16 h-16 rounded-2xl bg-gradient-to-br', feature.color, 'flex items-center justify-center mb-6')}>
                <feature.icon className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-2xl font-bold text-gray-900 mb-3">{feature.title}</h3>
              <p className="text-gray-600 leading-relaxed">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Presets Grid */}
      <div className="max-w-7xl mx-auto px-4 pb-20">
        <div className="flex items-center justify-between mb-10">
          <div>
            <h2 className="text-4xl font-bold text-gray-900 mb-2">精选预设</h2>
            <p className="text-gray-600">由专业摄影师打造的高质量预设</p>
          </div>
          <button
            onClick={handleRefresh}
            disabled={isLoading || isRefreshing}
            className="flex items-center space-x-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-2xl font-semibold hover:from-blue-700 hover:to-purple-700 transition-all duration-300 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isLoading || isRefreshing ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <RefreshCw className="w-5 h-5" />
            )}
            <span>{isLoading || isRefreshing ? '加载中...' : '刷新'}</span>
          </button>
        </div>

        {isLoading && !isRefreshing ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Loader2 className="w-12 h-12 text-blue-600 animate-spin mb-4" />
            <p className="text-gray-600 text-lg">正在加载预设...</p>
          </div>
        ) : error ? (
          <div className="text-center py-20">
            <p className="text-red-600 text-lg mb-4">{error}</p>
            <button
              onClick={handleRefresh}
              className="px-6 py-2 bg-red-500 text-white rounded-xl hover:bg-red-600"
            >
              重试
            </button>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {presets.map((preset, index) => (
              <PresetCard key={preset.id} preset={preset} index={index} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
