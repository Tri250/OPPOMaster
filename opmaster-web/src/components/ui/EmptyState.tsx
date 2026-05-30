import React from 'react';
import { motion } from 'framer-motion';
import { Search, Heart, Image, FolderOpen, Camera, Zap } from 'lucide-react';

export type EmptyStateType = 'search' | 'favorites' | 'photos' | 'presets' | 'camera' | 'loading' | 'error';

interface EmptyStateProps {
  type?: EmptyStateType;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
    icon?: React.ReactNode;
  };
  className?: string;
}

const EmptyStateIcon = ({ type }: { type: EmptyStateType }) => {
  const iconClass = "w-16 h-16 text-hasselblad/50";
  
  switch (type) {
    case 'search':
      return <Search className={iconClass} />;
    case 'favorites':
      return <Heart className={iconClass} />;
    case 'photos':
      return <Image className={iconClass} />;
    case 'presets':
      return <FolderOpen className={iconClass} />;
    case 'camera':
      return <Camera className={iconClass} />;
    case 'loading':
      return <Zap className={`${iconClass} animate-pulse`} />;
    case 'error':
      return <Zap className={iconClass} />;
    default:
      return <Search className={iconClass} />;
  }
};

export const EmptyState = ({ 
  type = 'search', 
  title, 
  description, 
  action, 
  className = ''
}: EmptyStateProps) => {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`flex flex-col items-center justify-center py-16 px-4 text-center ${className}`}
    >
      <div className="mb-6">
        <EmptyStateIcon type={type} />
      </div>
      
      <h3 className="text-xl font-semibold text-white mb-2">
        {title}
      </h3>
      
      {description && (
        <p className="text-white/60 text-sm max-w-xs mb-6">
          {description}
        </p>
      )}
      
      {action && (
        <button
          onClick={action.onClick}
          className="btn-primary btn-sm flex items-center gap-2"
        >
          {action.icon}
          {action.label}
        </button>
      )}
    </motion.div>
  );
};

export const EmptySearchState = ({ query, onReset }: { query: string; onReset?: () => void }) => (
  <EmptyState
    type="search"
    title="未找到相关结果"
    description={`没有找到包含 "${query}" 的预设，请尝试其他关键词`}
    action={onReset ? { label: '清除搜索', onClick: onReset } : undefined}
  />
);

export const EmptyFavoritesState = ({ onExplore }: { onExplore?: () => void }) => (
  <EmptyState
    type="favorites"
    title="还没有收藏"
    description="开始探索哈苏大师预设，收藏您喜欢的预设吧"
    action={onExplore ? { label: '探索预设', onClick: onExplore } : undefined}
  />
);

export const EmptyPhotosState = ({ onUpload }: { onUpload?: () => void }) => (
  <EmptyState
    type="photos"
    title="还没有照片"
    description="上传照片开始体验AI场景识别和预设匹配"
    action={onUpload ? { label: '上传照片', onClick: onUpload } : undefined}
  />
);

export const EmptyPresetsState = ({ onCreate }: { onCreate?: () => void }) => (
  <EmptyState
    type="presets"
    title="还没有预设"
    description="创建您的第一个哈苏大师调色预设"
    action={onCreate ? { label: '创建预设', onClick: onCreate } : undefined}
  />
);

export const EmptyCameraState = ({ onOpen }: { onOpen?: () => void }) => (
  <EmptyState
    type="camera"
    title="相机未连接"
    description="连接您的OPPO设备开始使用哈苏相机参数"
    action={onOpen ? { label: '连接相机', onClick: onOpen } : undefined}
  />
);

export const EmptyErrorState = ({ onRetry }: { onRetry?: () => void }) => (
  <EmptyState
    type="error"
    title="出错了"
    description="发生了一些问题，请重试"
    action={onRetry ? { label: '重试', onClick: onRetry } : undefined}
  />
);

export const EmptyLoadingState = () => (
  <EmptyState
    type="loading"
    title="加载中..."
    description="请稍候，正在获取数据"
  />
);