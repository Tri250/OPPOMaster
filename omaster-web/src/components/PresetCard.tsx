import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import { Heart, Star, Download, Camera as CameraIcon } from "lucide-react";
import type { Preset } from "../types";
import { useAppStore } from "../store/useAppStore";

interface Props {
  preset: Preset;
  index?: number;
  showRank?: boolean;
}

export default function PresetCard({ preset, index = 0, showRank = false }: Props) {
  const isFavorite = useAppStore((s) => s.favorites.has(preset.id));
  const toggleFavorite = useAppStore((s) => s.toggleFavorite);

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-50px" }}
      transition={{ duration: 0.5, delay: Math.min(index * 0.05, 0.4) }}
      className="group relative card overflow-hidden hover:border-hasselblad-500/40 transition-all duration-500"
    >
      <Link to={`/presets/${preset.id}`} className="block">
        {/* 封面 */}
        <div className="relative aspect-[4/3] overflow-hidden">
          <img
            src={preset.coverUrl}
            alt={preset.name}
            loading="lazy"
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
          />
          {/* 渐变遮罩 */}
          <div className="absolute inset-0 bg-gradient-to-t from-ink-900 via-ink-900/30 to-transparent" />

          {/* 角标 */}
          {showRank && (
            <div className="absolute top-3 left-3 w-8 h-8 rounded-lg bg-hasselblad-500 text-ink-900 font-bold text-sm flex items-center justify-center shadow-lg">
              {index + 1}
            </div>
          )}

          {preset.isHncsCertified && (
            <div className="absolute top-3 right-3 px-2 py-1 rounded-md bg-hasselblad-500/95 text-ink-900 text-[10px] font-bold tracking-wider flex items-center gap-1">
              <span className="w-1 h-1 rounded-full bg-ink-900" />
              HNCS
            </div>
          )}

          {/* 底部信息 */}
          <div className="absolute bottom-0 inset-x-0 p-4">
            <div className="flex items-center gap-2 text-[11px] text-ink-200/80 mb-1.5">
              <CameraIcon className="w-3 h-3" />
              <span>{preset.deviceModel}</span>
            </div>
            <h3 className="font-display text-base font-bold text-ink-50 line-clamp-2 leading-snug">
              {preset.name}
            </h3>
          </div>
        </div>
      </Link>

      {/* 收藏按钮 */}
      <button
        onClick={(e) => {
          e.preventDefault();
          toggleFavorite(preset.id);
        }}
        className="absolute top-3 right-3 z-10 w-9 h-9 rounded-full glass-strong flex items-center justify-center hover:scale-110 transition-transform"
        style={{ display: preset.isHncsCertified ? "none" : "flex" }}
        aria-label="收藏"
      >
        <Heart
          className={`w-4 h-4 transition-colors ${
            isFavorite ? "fill-hasselblad-500 text-hasselblad-500" : "text-ink-200"
          }`}
        />
      </button>

      {/* 元数据 */}
      <div className="p-4 flex items-center justify-between text-xs text-ink-300">
        <div className="flex items-center gap-3">
          <span className="inline-flex items-center gap-1">
            <Star className="w-3.5 h-3.5 fill-hasselblad-500 text-hasselblad-500" />
            <span className="text-ink-100 font-medium">{preset.rating.toFixed(1)}</span>
          </span>
          <span className="inline-flex items-center gap-1">
            <Download className="w-3.5 h-3.5" />
            <span>{(preset.downloadCount / 1000).toFixed(1)}K</span>
          </span>
        </div>
        <span className="text-ink-400">{preset.author}</span>
      </div>
    </motion.div>
  );
}
