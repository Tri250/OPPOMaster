import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, X, AlertTriangle, Info, XCircle } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

interface ToastContextType {
  toasts: Toast[];
  showToast: (toast: Omit<Toast, 'id'>) => void;
  hideToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
};

const ToastIcon = ({ type }: { type: ToastType }) => {
  switch (type) {
    case 'success':
      return <Check className="w-5 h-5" />;
    case 'error':
      return <XCircle className="w-5 h-5" />;
    case 'warning':
      return <AlertTriangle className="w-5 h-5" />;
    case 'info':
      return <Info className="w-5 h-5" />;
  }
};

const getToastColors = (type: ToastType) => {
  switch (type) {
    case 'success':
      return {
        bg: 'bg-green-500/20',
        border: 'border-green-500/30',
        icon: 'text-green-400',
        text: 'text-green-50'
      };
    case 'error':
      return {
        bg: 'bg-red-500/20',
        border: 'border-red-500/30',
        icon: 'text-red-400',
        text: 'text-red-50'
      };
    case 'warning':
      return {
        bg: 'bg-yellow-500/20',
        border: 'border-yellow-500/30',
        icon: 'text-yellow-400',
        text: 'text-yellow-50'
      };
    case 'info':
      return {
        bg: 'bg-blue-500/20',
        border: 'border-blue-500/30',
        icon: 'text-blue-400',
        text: 'text-blue-50'
      };
  }
};

interface ToastItemProps {
  toast: Toast;
  onClose: () => void;
}

const ToastItem = ({ toast, onClose }: ToastItemProps) => {
  const colors = getToastColors(toast.type);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -20, scale: 0.95 }}
      className={`${colors.bg} ${colors.border} border backdrop-blur-md rounded-xl p-4 shadow-xl max-w-sm w-full flex items-start gap-3`}
    >
      <div className={`${colors.icon} flex-shrink-0 mt-0.5`}>
        <ToastIcon type={toast.type} />
      </div>
      <p className={`${colors.text} text-sm font-medium flex-1`}>
        {toast.message}
      </p>
      <button
        onClick={onClose}
        className={`${colors.text} hover:${colors.text} hover:opacity-70 transition-opacity p-1 rounded-lg flex-shrink-0`}
        aria-label="关闭通知"
      >
        <X className="w-4 h-4" />
      </button>
    </motion.div>
  );
};

interface ToastProviderProps {
  children: ReactNode;
}

export const ToastProvider = ({ children }: ToastProviderProps) => {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((toast: Omit<Toast, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    const newToast = { ...toast, id };
    
    setToasts((prev) => [...prev, newToast]);

    const duration = toast.duration ?? 3000;
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const hideToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, showToast, hideToast }}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2">
        <AnimatePresence>
          {toasts.map((toast) => (
            <ToastItem
              key={toast.id}
              toast={toast}
              onClose={() => hideToast(toast.id)}
            />
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
};

export const ToastHelper = {
  success: (message: string, duration?: number) => {
    const event = new CustomEvent('toast', {
      detail: { type: 'success', message, duration }
    });
    window.dispatchEvent(event);
  },
  error: (message: string, duration?: number) => {
    const event = new CustomEvent('toast', {
      detail: { type: 'error', message, duration }
    });
    window.dispatchEvent(event);
  },
  warning: (message: string, duration?: number) => {
    const event = new CustomEvent('toast', {
      detail: { type: 'warning', message, duration }
    });
    window.dispatchEvent(event);
  },
  info: (message: string, duration?: number) => {
    const event = new CustomEvent('toast', {
      detail: { type: 'info', message, duration }
    });
    window.dispatchEvent(event);
  }
};