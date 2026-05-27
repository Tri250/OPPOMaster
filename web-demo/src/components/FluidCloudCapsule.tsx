import React, { useState } from 'react';
import { motion } from 'framer-motion';
import type { Preset } from '../types';

interface FluidCloudCapsuleProps {
  preset: Preset;
  onExpand?: () => void;
}

const FluidCloudCapsule: React.FC<FluidCloudCapsuleProps> = ({ preset, onExpand }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <motion.div
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      className="fixed top-24 right-6 z-40"
    >
      <motion.div
        layoutId="capsule"
        onClick={() => {
          setIsExpanded(!isExpanded);
          onExpand?.();
        }}
        className="relative"
      >
        <div className="bg-gradient-to-br from-blue-600 via-purple-600 to-pink-600 rounded-2xl p-4 shadow-2xl shadow-purple-500/30 cursor-pointer hover:shadow-purple-500/50 transition-shadow">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center">
              <img
                src={preset.coverPath}
                alt=""
                className="w-full h-full object-cover rounded-xl"
              />
            </div>
            <div className="text-white">
              <p className="text-sm font-bold truncate max-w-[150px]">
                {preset.name}
              </p>
              <p className="text-xs opacity-90">
                ISO {preset.cameraParams.iso} • {preset.cameraParams.shutter}
              </p>
            </div>
          </div>

          {isExpanded && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              className="mt-4 pt-4 border-t border-white/20 space-y-3"
            >
              <div className="grid grid-cols-2 gap-3 text-xs text-white/90">
                <div className="bg-white/10 rounded-xl p-3">
                  <p className="opacity-70">对比度</p>
                  <p className="font-bold">{preset.cameraParams.contrast.toFixed(1)}</p>
                </div>
                <div className="bg-white/10 rounded-xl p-3">
                  <p className="opacity-70">饱和度</p>
                  <p className="font-bold">{preset.cameraParams.saturation.toFixed(1)}</p>
                </div>
                <div className="bg-white/10 rounded-xl p-3">
                  <p className="opacity-70">白平衡</p>
                  <p className="font-bold">{preset.cameraParams.wb}</p>
                </div>
                <div className="bg-white/10 rounded-xl p-3">
                  <p className="opacity-70">暗角</p>
                  <p className="font-bold">{preset.cameraParams.vignette.toFixed(2)}</p>
                </div>
              </div>
            </motion.div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
};

export default FluidCloudCapsule;
