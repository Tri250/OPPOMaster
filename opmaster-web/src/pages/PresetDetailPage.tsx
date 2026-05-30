import { motion } from 'framer-motion';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { ArrowLeft, Heart, Share2, Star, Camera, Sliders, Download, Tag, MessageSquare, ChevronDown, Sparkles, Play, Pause } from 'lucide-react';
import { useAppStore } from '../store/useAppStore';
import { usePresetStore } from '../store/usePresetStore';
import { useState, useEffect, useRef } from 'react';
import { mockPresets } from '../data/mockPresets';

export default function PresetDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { toggleFavorite } = useAppStore();
  const { downloadPreset, downloadQueue, myPresets, addMyPreset } = usePresetStore();

  // 获取预设数据 - 优先使用路由参数，否则从mock数据中查找
  const [preset, setPreset] = useState<any>(null);
  const [compareMode, setCompareMode] = useState<'slider' | 'tabs'>('slider');
  const [sliderPosition, setSliderPosition] = useState(50);
  const [isDownloading, setIsDownloading] = useState(false);
  const [isDownloaded, setIsDownloaded] = useState(false);

  // 评论输入
  const [commentText, setCommentText] = useState('');
  const [rating, setRating] = useState(5);

  useEffect(() => {
    // 先尝试从路由状态获取预设
    if (location.state?.preset) {
      setPreset(location.state.preset);
    } else {
      // 否则从mock数据中查找
      const foundPreset = mockPresets.find(p => p.id === id);
      setPreset(foundPreset);
    }
  }, [id, location.state]);

  // 检查是否已下载
  useEffect(() => {
    if (preset) {
      const downloaded = myPresets.some(p => p.id === preset.id);
      setIsDownloaded(downloaded);
    }
  }, [myPresets, preset]);

  if (!preset) {
    return (
      <div className="min-h-screen pt-20 flex items-center justify-center">
        <div className="text-center">
          <p className="text-text-tertiary text-xl mb-4">预设不存在</p>
          <button onClick={() => navigate('/preset-ecosystem')} className="btn-primary">
            返回预设生态
          </button>
        </div>
      </div>
    );
  }

  // 模拟评论数据
  const comments = [
    {
      id: '1',
      userId: 'user1',
      userName: '摄影爱好者',
      userAvatar: 'https://picsum.photos/seed/user1/100/100',
      content: '这个预设太棒了！色彩调校得非常漂亮，很有胶片感！',
      rating: 5,
      createdAt: '2024-01-15',
      likes: 24
    },
    {
      id: '2',
      userId: 'user2',
      userName: '旅行摄影师',
      userAvatar: 'https://picsum.photos/seed/user2/100/100',
      content: '用在风景照片上效果特别好，推荐！',
      rating: 4,
      createdAt: '2024-01-10',
      likes: 18
    },
    {
      id: '3',
      userId: 'user3',
      userName: '后期达人',
      userAvatar: 'https://picsum.photos/seed/user3/100/100',
      content: '蓝调风格处理夜景照片特别好看，赞！',
      rating: 5,
      createdAt: '2024-01-05',
      likes: 32
    }
  ];

  const handleDownload = () => {
    if (isDownloaded) {
      // 已下载，重新下载确认
      if (confirm('该预设已下载，是否重新下载？')) {
        performDownload();
      }
    } else {
      performDownload();
    }
  };

  const performDownload = () => {
    setIsDownloading(true);
    downloadPreset(preset);
    
    // 模拟下载完成
    setTimeout(() => {
      setIsDownloading(false);
      setIsDownloaded(true);
    }, 3000);
  };

  const getParamDisplayValue = (value: string | number | undefined): string => {
    if (value === undefined || value === null) return '-';
    return String(value);
  };

  // 检查下载状态
  const currentDownload = downloadQueue.find(d => d.preset.id === preset.id);

  return (
    <div className="min-h-screen bg-[#000000]">
      {/* 固定顶部导航栏 */}
      <div className="sticky top-0 z-30 bg-[#000000]/95 backdrop-blur-sm border-b border-[#262626]">
        <div className="max-w-6xl mx-auto px-4 sm:px-6">
          <div className="flex items-center h-16">
            <button
              onClick={() => navigate(-1)}
              className="flex items-center space-x-2 text-[#8A8A8A] hover:text-white transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
              <span>返回</span>
            </button>
            <div className="flex-1 text-center">
              <h1 className="text-lg font-semibold">预设详情</h1>
            </div>
            <div className="flex items-center space-x-3">
              <button
                onClick={() => preset && toggleFavorite(preset.id)}
                className="p-2 text-[#8A8A8A] hover:text-white"
              >
                <Heart
                  className={`w-5 h-5 ${preset?.isFavorite ? 'fill-[#FF6B35] text-[#FF6B35]' : ''}`}
                />
              </button>
              <button className="p-2 text-[#8A8A8A] hover:text-white">
                <Share2 className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* 左侧：预览区域 */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="space-y-6"
          >
            {/* 主要预览 - 前后对比 */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold">效果预览</h2>
                <div className="flex bg-[#141414] rounded-lg p-1">
                  <button
                    onClick={() => setCompareMode('slider')}
                    className={`px-4 py-2 rounded-md text-sm transition-colors ${compareMode === 'slider' ? 'bg-[#FF6B35]' : 'text-[#8A8A8A]'}`}
                  >
                    滑动对比
                  </button>
                  <button
                    onClick={() => setCompareMode('tabs')}
                    className={`px-4 py-2 rounded-md text-sm transition-colors ${compareMode === 'tabs' ? 'bg-[#FF6B35]' : 'text-[#8A8A8A]'}`}
                  >
                    标签对比
                  </button>
                </div>
              </div>

              {compareMode === 'slider' ? (
                /* 滑动对比 */
                <div className="relative aspect-[3/4] rounded-2xl overflow-hidden bg-[#141414]">
                  {/* 原图 */}
                  <img
                    src={preset.coverPath}
                    alt="原图"
                    className="absolute inset-0 w-full h-full object-cover"
                  />
                  {/* 效果图 */}
                  <div
                    className="absolute inset-0"
                    style={{ width: `${100 - sliderPosition}%`, right: 0, left: 'auto' }}
                  >
                    <img
                      src={preset.coverPath}
                      alt="效果图"
                      className="w-full h-full object-cover"
                      style={{ filter: 'saturate(1.2) contrast(1.1) brightness(0.95) sepia(0.1) hue-rotate(-5deg)' }}
                    />
                  </div>
                  {/* 滑动条 */}
                  <div
                    className="absolute top-0 bottom-0"
                    style={{ left: `${sliderPosition}%`, transform: 'translateX(-50%)' }}
                  >
                    <div className="w-1 h-full bg-white/80" />
                    <div className="absolute top-1/2 -mt-6 -ml-6 w-12 h-12 rounded-full bg-[#FF6B35] flex items-center justify-center shadow-lg">
                      <div className="flex items-center justify-center">
                        <div className="w-0 h-0 border-t-4 border-t-transparent border-b-4 border-b-transparent border-r-6 border-r-white mr-1" />
                        <div className="w-0 h-0 border-t-4 border-t-transparent border-b-4 border-b-transparent border-l-6 border-l-white ml-1" />
                      </div>
                    </div>
                  </div>
                  {/* 拖拽区域 */}
                  <input
                    type="range"
                    min="0"
                    max="100"
                    value={sliderPosition}
                    onChange={(e) => setSliderPosition(Number(e.target.value))}
                    className="absolute inset-0 w-full h-full opacity-0 cursor-ew-resize"
                  />
                  {/* 标签 */}
                  <div className="absolute bottom-4 left-4 px-3 py-1 rounded-full bg-black/50 text-xs">
                    原图
                  </div>
                  <div className="absolute bottom-4 right-4 px-3 py-1 rounded-full bg-[#FF6B35] text-xs">
                    效果图
                  </div>
                </div>
              ) : (
                /* 标签对比 */
                <div className="grid grid-cols-2 gap-4">
                  <div className="aspect-[3/4] rounded-2xl overflow-hidden">
                    <img
                      src={preset.coverPath}
                      alt="原图"
                      className="w-full h-full object-cover"
                    />
                    <div className="absolute bottom-4 left-4 px-3 py-1 rounded-full bg-black/50 text-xs">
                      原图
                    </div>
                  </div>
                  <div className="aspect-[3/4] rounded-2xl overflow-hidden">
                    <img
                      src={preset.coverPath}
                      alt="效果图"
                      className="w-full h-full object-cover"
                      style={{ filter: 'saturate(1.2) contrast(1.1) brightness(0.95) sepia(0.1) hue-rotate(-5deg)' }}
                    />
                    <div className="absolute bottom-4 right-4 px-3 py-1 rounded-full bg-[#FF6B35] text-xs">
                      效果图
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* 图库 */}
            {preset.galleryImages && preset.galleryImages.length > 0 && (
              <div className="grid grid-cols-3 gap-3">
                {preset.galleryImages.map((img: string, idx: number) => (
                  <motion.div
                    key={idx}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 0.2 + idx * 0.05 }}
                    className="aspect-[3/4] rounded-xl overflow-hidden"
                  >
                    <img
                      src={img}
                      alt={`${preset.name} sample ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />
                  </motion.div>
                ))}
              </div>
            )}

            {/* 标签 */}
            {preset.tags && preset.tags.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {preset.tags.map((tag: string, idx: number) => (
                  <span
                    key={idx}
                    className="text-xs bg-[#141414] px-3 py-1.5 rounded-full text-[#8A8A8A]"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </motion.div>

          {/* 右侧：详情区域 */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="space-y-6"
          >
            {/* 标题和基本信息 */}
            <div>
              <div className="flex items-start justify-between mb-2">
                <h1 className="text-2xl font-bold">{preset.name}</h1>
                {preset.isNew && (
                  <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[#FF6B35] text-xs font-medium">
                    <Sparkles className="w-3 h-3" />
                    新品
                  </div>
                )}
              </div>
              <div className="flex items-center gap-4 text-sm text-[#8A8A8A] mb-3">
                <div className="flex items-center gap-1">
                  <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                  <span>{preset.rating?.toFixed(1) || '4.8'}</span>
                </div>
                <span>{(preset.downloadCount || 1234).toLocaleString()} 下载</span>
                {preset.author && (
                  <span>by {preset.author}</span>
                )}
              </div>
              <p className="text-[#8A8A8A]">
                适用于 {preset.deviceModel}
              </p>
            </div>

            {/* 下载按钮 */}
            <div className="space-y-3">
              {currentDownload && currentDownload.status !== 'completed' ? (
              <div className="bg-[#141414] rounded-xl p-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm">下载中...</span>
                  <span className="text-sm text-[#FF6B35]">{Math.round(currentDownload.progress)}%</span>
                </div>
                <div className="w-full h-2 bg-[#262626] rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-[#FF6B35] rounded-full transition-all duration-300"
                    style={{ width: `${currentDownload.progress}%` }}
                  />
                </div>
                <div className="flex gap-2 mt-3">
                  <button
                    onClick={() => {
                      if (currentDownload.status === 'downloading') {
                        // 暂停
                      } else {
                        // 继续
                      }
                    }}
                    className="flex-1 py-2 border border-[#262626] rounded-lg text-sm"
                  >
                    {currentDownload.status === 'downloading' ? '暂停' : '继续'}
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={handleDownload}
                disabled={isDownloading}
                className={`w-full py-4 rounded-xl font-medium transition-colors flex items-center justify-center gap-2 ${
                  isDownloaded 
                    ? 'bg-[#141414] border border-[#262626]' 
                    : 'bg-[#FF6B35]'
                }`}
              >
                <Download className="w-5 h-5" />
                <span>{isDownloaded ? '已下载' : '下载预设'}</span>
              </button>
            )}

              {preset.price !== 'free' && preset.price && (
                <button
                  className="w-full py-4 rounded-xl border border-[#262626] text-sm">
                  ¥{preset.price}
                </button>
              )}
            </div>

            {/* 相机参数 */}
            {preset.cameraParams && (
              <div className="bg-[#141414] rounded-2xl p-6">
                <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                  <Sliders className="w-5 h-5 text-[#FF6B35]" />
                  <span>哈苏大师模式参数</span>
                </h2>
                
                <div className="grid grid-cols-2 gap-3">
                  {[
                    { label: '模式', value: preset.cameraParams.mode },
                    { label: '滤镜', value: `${preset.cameraParams.filter} ${preset.cameraParams.filter_intensity}%` },
                    { label: '柔光', value: preset.cameraParams.soft_light },
                    { label: '色调曲线', value: preset.cameraParams.tone_curve },
                    { label: '饱和度', value: `${preset.cameraParams.saturation}%` },
                    { label: '冷暖调', value: preset.cameraParams.warm_cool },
                    { label: '青红调', value: preset.cameraParams.cyan_magenta },
                    { label: '锐度', value: preset.cameraParams.sharpness },
                    { label: '暗角', value: preset.cameraParams.vignette ? '开启' : '关闭' },
                    { label: 'ISO', value: getParamDisplayValue(preset.cameraParams.iso) },
                    { label: '快门', value: getParamDisplayValue(preset.cameraParams.shutter_speed) },
                    { label: '曝光补偿', value: getParamDisplayValue(preset.cameraParams.exposure_compensation) },
                    { label: '自定义白平衡', value: getParamDisplayValue(preset.cameraParams.custom_wb ? `${preset.cameraParams.custom_wb}K` : undefined) }
                  ].filter(p => p.value !== '-').map((param, idx) => (
                    <div key={param.label} className="bg-[#0a0a0a] rounded-lg p-3">
                      <span className="text-xs text-[#8A8A8A] block">{param.label}</span>
                      <span className="text-sm font-medium">{param.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 描述 */}
            {(preset.sections && preset.sections.length > 0 || preset.description) && (
              <div className="bg-[#141414] rounded-2xl p-6">
                <h2 className="text-lg font-bold mb-4">使用说明</h2>
                <div className="space-y-4">
                  {preset.description && (
                    <div>
                      <h3 className="text-sm font-bold text-[#FF6B35] mb-1">{preset.description.title}</h3>
                      <p className="text-sm text-[#8A8A8A] whitespace-pre-wrap">{preset.description.content}</p>
                    </div>
                  )}
                  {preset.sections && preset.sections.map((section: any, idx: number) => (
                    <div key={idx}>
                      <h3 className="text-sm font-bold text-[#FF6B35] mb-2">{section.title}</h3>
                      <div className="space-y-2">
                        {section.items.map((item: any, itemIdx: number) => (
                          <div key={itemIdx} className="flex justify-between bg-[#0a0a0a] px-3 py-2 rounded-lg">
                            <span className="text-sm text-[#8A8A8A]">{item.label}</span>
                            <span className="text-sm font-medium">{item.value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 用户评论 */}
            <div className="bg-[#141414] rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold flex items-center gap-2">
                  <MessageSquare className="w-5 h-5" />
                  <span>用户评论 ({comments.length})</span>
                </h2>
              </div>

              {/* 评论输入框 */}
              <div className="space-y-4 mb-6">
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-sm text-[#8A8A8A]">评分</span>
                  <div className="flex gap-1">
                    {[1,2,3,4,5].map((star) => (
                      <button
                        key={star}
                        onClick={() => setRating(star)}
                        className="p-1"
                      >
                        <Star
                          className={`w-5 h-5 ${star <= rating ? 'text-yellow-400 fill-yellow-400' : 'text-[#262626]'}`}
                        />
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex gap-3">
                  <img
                    src="https://picsum.photos/seed/me/100/100"
                    alt="我的头像"
                    className="w-10 h-10 rounded-full"
                  />
                  <div className="flex-1">
                    <input
                      type="text"
                      placeholder="分享你的使用体验..."
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      className="w-full px-4 py-3 bg-[#0a0a0a] border border-[#262626] rounded-xl text-sm focus:outline-none focus:border-[#FF6B35]"
                    />
                  </div>
                  <button
                    disabled={!commentText.trim()}
                    className="px-4 py-3 bg-[#FF6B35] rounded-xl text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    发布
                  </button>
                </div>
              </div>

              {/* 评论列表 */}
              <div className="space-y-4">
                {comments.map((comment) => (
                  <div key={comment.id} className="flex gap-3">
                    <img
                      src={comment.userAvatar}
                      alt={comment.userName}
                      className="w-10 h-10 rounded-full"
                    />
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium">{comment.userName}</span>
                        <span className="text-xs text-[#8A8A8A]">{comment.createdAt}</span>
                      </div>
                      <div className="flex items-center gap-1 mb-2">
                        {[1,2,3,4,5].map((star) => (
                          <Star
                            key={star}
                            className={`w-3 h-3 ${star <= comment.rating ? 'text-yellow-400 fill-yellow-400' : 'text-[#262626]'}`}
                          />
                        ))}
                      </div>
                      <p className="text-sm text-[#8A8A8A]">{comment.content}</p>
                      <div className="flex items-center gap-4 mt-2">
                        <button className="text-xs text-[#8A8A8A] hover:text-[#FF6B35]">
                        👍 {comment.likes}
                      </button>
                        <button className="text-xs text-[#8A8A8A] hover:text-[#FF6B35]">
                        回复
                      </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
