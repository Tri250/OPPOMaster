
import { ArrowLeft, Sun, Moon, Monitor } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@/hooks/useTheme';
import { ThemeMode } from '@/types';

export function Settings() {
  const navigate = useNavigate();
  const { themeMode, setThemeMode } = useTheme();

  const themes: { mode: ThemeMode; label: string; icon: typeof Sun }[] = [
    { mode: 'light', label: '浅色', icon: Sun },
    { mode: 'dark', label: '深色', icon: Moon },
    { mode: 'system', label: '跟随系统', icon: Monitor },
  ];

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
              设置
            </h1>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-6">
            主题
          </h2>
          <div className="space-y-3">
            {themes.map((theme) => {
            const Icon = theme.icon;
            const isSelected = themeMode === theme.mode;
            
            return (
              <button
                key={theme.mode}
                onClick={() => setThemeMode(theme.mode)}
                className={`w-full flex items-center gap-4 p-4 rounded-xl transition-all ${
                  isSelected
                    ? 'bg-blue-50 dark:bg-blue-900/30 border-2 border-blue-500'
                    : 'bg-gray-50 dark:bg-gray-700 border-2 border-transparent hover:bg-gray-100 dark:hover:bg-gray-600'
                }`}
              >
                <div className={`p-3 rounded-full ${
                  isSelected 
                    ? 'bg-blue-500 text-white' 
                    : 'bg-gray-200 dark:bg-gray-600 text-gray-600 dark:text-gray-300'
                }`}>
                  <Icon className="w-5 h-5" />
                </div>
                <div className="flex-1 text-left">
                  <p className={`font-medium ${
                    isSelected 
                      ? 'text-blue-900 dark:text-blue-100' 
                      : 'text-gray-900 dark:text-gray-100'
                  }`}>
                    {theme.label}
                  </p>
                </div>
                {isSelected && (
                  <div className="w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center">
                    <div className="w-2 h-2 bg-white rounded-full" />
                  </div>
                )}
              </button>
            );
          })}
          </div>
        </div>

        <div className="mt-8 bg-white dark:bg-gray-800 rounded-2xl p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            关于
          </h2>
          <div className="space-y-3 text-gray-600 dark:text-gray-400">
            <p>OMaster - OPPO 哈苏影像系统级参数中枢</p>
            <p className="text-sm">版本 1.0.0</p>
          </div>
        </div>
      </main>
    </div>
  );
}
