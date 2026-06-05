import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { FilterType, WatermarkConfig } from "../types";

interface AppState {
  // 收藏
  favorites: Set<string>;
  toggleFavorite: (id: string) => void;
  isFavorite: (id: string) => boolean;

  // 搜索
  searchQuery: string;
  setSearchQuery: (q: string) => void;

  // 筛选
  filterType: FilterType;
  setFilterType: (f: FilterType) => void;

  // 水印配置
  watermark: WatermarkConfig;
  updateWatermark: (patch: Partial<WatermarkConfig>) => void;
  saveWatermark: () => void;
  resetWatermark: () => void;

  // UI 状态
  isOnline: boolean;
  setOnline: (online: boolean) => void;
}

const defaultWatermark: WatermarkConfig = {
  template: "HASSELBLAD",
  position: "BOTTOM_RIGHT",
  opacity: 0.85,
  scale: 1.0,
  customText: "",
  showTimestamp: true,
  showDevice: true,
};

export const useAppStore = create<AppState>()(
  persist(
    (set, get) => ({
      favorites: new Set(),
      toggleFavorite: (id) =>
        set((state) => {
          const next = new Set(state.favorites);
          if (next.has(id)) next.delete(id);
          else next.add(id);
          return { favorites: next };
        }),
      isFavorite: (id) => get().favorites.has(id),

      searchQuery: "",
      setSearchQuery: (q) => set({ searchQuery: q }),

      filterType: "ALL",
      setFilterType: (f) => set({ filterType: f }),

      watermark: defaultWatermark,
      updateWatermark: (patch) =>
        set((state) => ({ watermark: { ...state.watermark, ...patch } })),
      saveWatermark: () => {
        // localStorage 持久化已由 zustand/middleware 处理
      },
      resetWatermark: () => set({ watermark: defaultWatermark }),

      isOnline: true,
      setOnline: (online) => set({ isOnline: online }),
    }),
    {
      name: "omaster-web-store",
      partialize: (state) => ({
        favorites: Array.from(state.favorites),
        filterType: state.filterType,
        watermark: state.watermark,
      }),
      // 自定义序列化：将 Set 转 Array
      storage: {
        getItem: (name) => {
          const str = localStorage.getItem(name);
          if (!str) return null;
          const data = JSON.parse(str);
          if (data.state && Array.isArray(data.state.favorites)) {
            data.state.favorites = new Set(data.state.favorites);
          }
          return data;
        },
        setItem: (name, value) => {
          const data = JSON.stringify(value, (_key, val) => {
            if (val instanceof Set) return Array.from(val);
            return val;
          });
          localStorage.setItem(name, data);
        },
        removeItem: (name) => localStorage.removeItem(name),
      },
    }
  )
);
