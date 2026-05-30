import { motion, AnimatePresence } from 'framer-motion';
import { X, Copy as CopyIcon, Star, ChevronLeft, ChevronRight } from 'lucide-react';
import { useState } from 'react';

interface Preset {
  id: string;
  name: string;
  params: {
    iso: string;
    shutter: string;
    ev: string;
    wb: string;
  };
}

interface FloatingWidgetProps {
  isVisible: boolean;
  currentPreset: Preset | null;
  onClose: () => void;
  onCopy: () => void;
}

export default function FloatingWidget({
  isVisible,
  currentPreset,
  onClose,
  onCopy
}: FloatingWidgetProps) {
  const [isExpanded, setIsExpanded] = useState(true);

  if (!isVisible) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.8 }}
        className="fixed top-24 right-4 z-50"
      >
        {isExpanded ? (
          <motion.div
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-[#0F0F0F] rounded-[16px] shadow-2xl w-72 overflow-hidden"
            style={{
              boxShadow: '0 8px 16px rgba(0, 0, 0, 0.3)',
            }}
          >
            {/* Header */}
            <div className="p-4 border-b border-white/10">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold text-[#FFFFFF]">
                  {currentPreset?.name || '预设参数'}
                </h3>
                <button
                  onClick={onClose}
                  className="w-7 h-7 flex items-center justify-center hover:bg-white/10 rounded-[8px] transition-colors"
                >
                  <X className="w-5 h-5 text-[#D4A574]" />
                </button>
              </div>
            </div>

            {/* Content */}
            <div className="p-4 space-y-3">
              {currentPreset?.params && (
                <>
                  <ParamRow label="ISO" value={currentPreset.params.iso} />
                  <ParamRow label="快门" value={currentPreset.params.shutter} />
                  <ParamRow label="EV" value={currentPreset.params.ev} />
                  <ParamRow label="白平衡" value={currentPreset.params.wb} />
                </>
              )}
            </div>

            {/* Actions */}
            <div className="p-4 pt-0 space-y-2">
              <button
                onClick={onCopy}
                className="w-full py-3 bg-[#FF6B35] text-[#0F0F0F] rounded-[12px] font-bold flex items-center justify-center gap-2 hover:bg-[#FF6B35]/90 transition-colors"
              >
                <CopyIcon className="w-5 h-5" />
                <span>一键复制</span>
              </button>
              
              <button
                onClick={() => setIsExpanded(false)}
                className="w-full py-2 text-[#CCCCCC] text-sm hover:text-[#FFFFFF] transition-colors"
              >
                收起悬浮窗
              </button>
            </div>
          </motion.div>
        ) : (
          <motion.button
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            onClick={() => setIsExpanded(true)}
            className="w-12 h-12 bg-[#FF6B35] rounded-full shadow-lg flex items-center justify-center hover:scale-110 transition-transform"
            style={{
              boxShadow: '0 4px 8px rgba(255, 107, 53, 0.3)',
            }}
          >
            <CopyIcon className="w-6 h-6 text-[#0F0F0F]" />
          </motion.button>
        )}
      </motion.div>
    </AnimatePresence>
  );
}

function ParamRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-white/70">{label}</span>
      <span className="text-sm font-bold text-[#FFFFFF]">{value}</span>
    </div>
  );
}

// Hook for managing floating widget state
export function useFloatingWidget() {
  const [isVisible, setIsVisible] = useState(false);
  const [currentPreset, setCurrentPreset] = useState<Preset | null>(null);

  const showWidget = (preset: Preset) => {
    setCurrentPreset(preset);
    setIsVisible(true);
  };

  const hideWidget = () => {
    setIsVisible(false);
  };

  const toggleWidget = () => {
    setIsVisible(prev => !prev);
  };

  return {
    isVisible,
    currentPreset,
    showWidget,
    hideWidget,
    toggleWidget,
    setCurrentPreset
  };
}

// Error toast component for network issues
interface ErrorToastProps {
  message: string;
  onRetry: () => void;
  onDismiss: () => void;
}

export function ErrorToast({ message, onRetry, onDismiss }: ErrorToastProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -50 }}
      className="fixed top-20 left-1/2 -translate-x-1/2 z-50 bg-[#FF4444] text-white px-6 py-4 rounded-[12px] shadow-lg max-w-md"
    >
      <div className="flex items-start gap-3">
        <div className="flex-1">
          <p className="font-semibold mb-1">错误</p>
          <p className="text-sm opacity-90">{message}</p>
        </div>
        <button onClick={onDismiss} className="text-white/70 hover:text-white">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex gap-2 mt-3">
        <button
          onClick={onRetry}
          className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-[8px] text-sm font-medium transition-colors"
        >
          重试
        </button>
        <button
          onClick={onDismiss}
          className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-[8px] text-sm font-medium transition-colors"
        >
          取消
        </button>
      </div>
    </motion.div>
  );
}
