
import { ArrowLeft, Heart } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useStore } from '@/store/useStore';

export function Detail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { getPresetById, toggleFavorite } = useStore();
  const preset = id ? getPresetById(id) : undefined;

  if (!preset) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-500 dark:text-gray-400 text-lg mb-4">
            预设不存在
          </p>
          <button
            onClick={() => navigate('/')}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            返回首页
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="sticky top-0 z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-200 dark:border-gray-800">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/')}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
            >
              <ArrowLeft className="w-6 h-6 text-gray-600 dark:text-gray-400" />
            </button>
            <h1 className="text-xl font-semibold text-gray-900 dark:text-white">
              {preset.name}
            </h1>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-8">
          <div className="relative rounded-2xl overflow-hidden">
            <img
              src={preset.coverPath}
              alt={preset.name}
              className="w-full aspect-video object-cover"
            />
            <button
              onClick={() => toggleFavorite(preset.id)}
              className="absolute top-4 right-4 p-3 bg-white/90 dark:bg-gray-800/90 rounded-full shadow-lg hover:bg-white dark:hover:bg-gray-700 transition-all"
            >
              <Heart
                className={`w-7 h-7 transition-colors ${
                  preset.isFavorite
                    ? 'fill-red-500 text-red-500'
                    : 'text-gray-400 hover:text-red-500'
                }`}
              />
            </button>
            {preset.cameraParams?.hasselblad_hncs && (
              <div className="absolute top-4 left-4 px-4 py-2 bg-yellow-500 text-yellow-900 text-sm font-semibold rounded-full">
                HNCS 认证
              </div>
            )}
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">
                  {preset.name}
                </h2>
                <div className="flex items-center gap-4 text-sm text-gray-500 dark:text-gray-400">
                  <span>{preset.deviceModel}</span>
                  <span>•</span>
                  <span>{preset.source}</span>
                </div>
              </div>
            </div>
            
            {preset.description && (
              <p className="text-gray-600 dark:text-gray-300 mb-6 leading-relaxed">
                {preset.description}
              </p>
            )}

            {preset.cameraParams && (
              <div className="border-t border-gray-200 dark:border-gray-700 pt-6">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                  相机参数
                </h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">模式</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.mode}
                    </p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">滤镜</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.filter}
                    </p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">ISO</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.iso}
                    </p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">快门</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.shutter}
                    </p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">曝光</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.ev}
                    </p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-xl p-4">
                    <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">白平衡</p>
                    <p className="font-medium text-gray-900 dark:text-white">
                      {preset.cameraParams.wb}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          <button className="w-full py-4 bg-gradient-to-r from-blue-600 to-blue-700 text-white font-semibold rounded-xl hover:from-blue-700 hover:to-blue-800 transition-all shadow-lg hover:shadow-xl">
            应用预设
          </button>
        </div>
      </main>
    </div>
  );
}
