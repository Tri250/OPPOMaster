import { motion, AnimatePresence } from 'framer-motion';
import { WifiOff, RefreshCw, X } from 'lucide-react';

interface NetworkErrorProps {
  isVisible: boolean;
  onRetry: () => void;
  onDismiss: () => void;
}

export default function NetworkError({ 
  isVisible, 
  onRetry, 
  onDismiss 
}: NetworkErrorProps) {
  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-page bg-black/50 backdrop-blur-sm"
          onClick={onDismiss}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            onClick={(e) => e.stopPropagation()}
            className="bg-[#1A1A1A] rounded-[16px] p-6 max-w-md w-full mx-4 shadow-2xl"
          >
            {/* 图标 */}
            <div className="flex items-center justify-center mb-4">
              <div className="w-16 h-16 bg-[#FF4444]/20 rounded-full flex items-center justify-center">
                <WifiOff className="w-8 h-8 text-[#FF4444]" />
              </div>
            </div>

            {/* 标题 */}
            <h2 className="text-xl font-bold text-[#FFFFFF] text-center mb-2">
              网络连接失败
            </h2>

            {/* 描述 */}
            <p className="text-[#CCCCCC] text-center mb-6">
              请检查您的网络设置，确保网络连接正常后重试
            </p>

            {/* 按钮组 */}
            <div className="flex gap-3">
              <button
                onClick={onRetry}
                className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-[#FF6B35] text-[#0F0F0F] font-semibold rounded-[12px] hover:bg-[#FF6B35]/90 transition-colors"
              >
                <RefreshCw className="w-5 h-5" />
                重试
              </button>
              <button
                onClick={onDismiss}
                className="flex-1 px-4 py-3 bg-[#333333] text-[#FFFFFF] font-semibold rounded-[12px] hover:bg-[#333333]/80 transition-colors"
              >
                取消
              </button>
            </div>

            {/* 关闭按钮 */}
            <button
              onClick={onDismiss}
              className="absolute top-4 right-4 w-8 h-8 flex items-center justify-center text-[#999999] hover:text-[#FFFFFF] transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// 网络状态钩子
export function useNetworkStatus() {
  const [isOnline, setIsOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOnline;
}

import { useState, useEffect } from 'react';
