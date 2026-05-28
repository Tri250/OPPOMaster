
import { Heart } from 'lucide-react';
import { Preset } from '@/types';

interface PresetCardProps {
  preset: Preset;
  onClick: () => void;
  onFavoriteToggle: () => void;
}

export function PresetCard({ preset, onClick, onFavoriteToggle }: PresetCardProps) {
  return (
    <div
      onClick={onClick}
      className="group bg-white dark:bg-gray-800 rounded-2xl overflow-hidden shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1"
    >
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        <button
          onClick={(e) => {
            e.stopPropagation();
            onFavoriteToggle();
          }}
          className="absolute top-3 right-3 p-2 bg-white/90 dark:bg-gray-800/90 rounded-full shadow-md hover:bg-white dark:hover:bg-gray-700 transition-all"
        >
          <Heart
            className={`w-5 h-5 transition-colors ${
              preset.isFavorite
                ? 'fill-red-500 text-red-500'
                : 'text-gray-400 hover:text-red-500'
            }`}
          />
        </button>
        {preset.cameraParams?.hasselblad_hncs && (
          <div className="absolute top-3 left-3 px-3 py-1 bg-yellow-500 text-yellow-900 text-xs font-semibold rounded-full">
            HNCS
          </div>
        )}
      </div>
      <div className="p-4">
        <h3 className="font-semibold text-gray-900 dark:text-white text-lg mb-1">
          {preset.name}
        </h3>
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-500 dark:text-gray-400">{preset.deviceModel}</span>
          <span className="text-gray-400 dark:text-gray-500">{preset.source}</span>
        </div>
      </div>
    </div>
  );
}
