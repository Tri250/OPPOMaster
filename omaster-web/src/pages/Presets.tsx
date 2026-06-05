import { useMemo, useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Search, SlidersHorizontal, X } from "lucide-react";
import PageHeader from "../components/PageHeader";
import PresetCard from "../components/PresetCard";
import { presets } from "../data/mock";
import { useAppStore } from "../store/useAppStore";
import type { FilterType, BrandType } from "../types";
import { BRAND_CONFIG } from "../types";

const filterOptions: { value: FilterType; label: string; icon?: string }[] = [
  { value: "ALL", label: "全部" },
  { value: "OPPO", label: "OPPO / 一加" },
  { value: "REALME", label: "Realme" },
  { value: "VIVO", label: "vivo / 蔡司" },
  { value: "HONOR", label: "荣耀" },
  { value: "FAVORITES", label: "收藏" },
  { value: "NEW", label: "最新" },
];

// 品牌颜色映射
const brandColors: Record<BrandType, string> = {
  OPPO: "bg-green-500/20 border-green-500/40 text-green-400",
  REALME: "bg-yellow-500/20 border-yellow-500/40 text-yellow-400",
  VIVO: "bg-blue-500/20 border-blue-500/40 text-blue-400",
  HONOR: "bg-purple-500/20 border-purple-500/40 text-purple-400",
};

export default function Presets() {
  const { searchQuery, setSearchQuery, filterType, setFilterType, favorites } = useAppStore();
  const [localSearch, setLocalSearch] = useState(searchQuery);

  // 防抖搜索
  useEffect(() => {
    const timer = setTimeout(() => setSearchQuery(localSearch), 250);
    return () => clearTimeout(timer);
  }, [localSearch, setSearchQuery]);

  const filtered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return presets.filter((preset) => {
      const matchesQuery =
        q === "" ||
        preset.name.toLowerCase().includes(q) ||
        preset.brand.toLowerCase().includes(q) ||
        preset.tags.some((t) => t.toLowerCase().includes(q)) ||
        preset.author.toLowerCase().includes(q);

      const matchesFilter = (() => {
        switch (filterType) {
          case "ALL":
            return true;
          case "OPPO":
            return preset.brand === "OPPO";
          case "REALME":
            return preset.brand === "REALME";
          case "VIVO":
            return preset.brand === "VIVO";
          case "HONOR":
            return preset.brand === "HONOR";
          case "FAVORITES":
            return favorites.has(preset.id);
          case "NEW":
            return preset.isNew || (preset.downloadCount || 0) < 10000;
        }
      })();

      return matchesQuery && matchesFilter;
    });
  }, [searchQuery, filterType, favorites]);

  // 按品牌分组统计
  const brandStats = useMemo(() => {
    const stats: Record<BrandType, number> = { OPPO: 0, REALME: 0, VIVO: 0, HONOR: 0 };
    presets.forEach(p => stats[p.brand]++);
    return stats;
  }, []);

  const titleMap: Record<FilterType, string> = {
    ALL: "全部预设",
    OPPO: "OPPO / 一加 大师预设",
    REALME: "Realme GR 预设",
    VIVO: "vivo 蔡司影像预设",
    HONOR: "荣耀专业模式预设",
    FAVORITES: "我的收藏",
    NEW: "最新预设",
  };

  return (
    <div className="pt-24 pb-20 min-h-screen">
      {/* 背景 */}
      <div className="fixed inset-0 -z-10 dotted-grid opacity-30" />
      <div className="fixed inset-0 -z-10 bg-gradient-to-b from-hasselblad-500/5 via-transparent to-transparent" />

      <div className="max-w-7xl mx-auto px-6">
        <PageHeader>
          <div className="text-center mb-12">
            <span className="text-hasselblad-500 text-sm font-semibold tracking-[0.2em] uppercase">
              Preset Library
            </span>
            <h1 className="font-display text-5xl md:text-6xl font-bold mt-3 mb-4">
              {titleMap[filterType]}
            </h1>
            <p className="text-ink-300 max-w-2xl mx-auto">
              {filterType === "FAVORITES" && favorites.size === 0
                ? "还没有收藏任何预设，去探索你喜欢的吧"
                : `共 ${filtered.length} 个预设 · 按品牌分类 · 专业调校`}
            </p>
          </div>
        </PageHeader>

        {/* 品牌统计卡片 */}
        <PageHeader delay={0.05}>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-10">
            {(Object.keys(BRAND_CONFIG) as BrandType[]).map((brand) => (
              <button
                key={brand}
                onClick={() => setFilterType(brand as FilterType)}
                className={`relative p-4 rounded-2xl border transition-all ${
                  filterType === brand
                    ? brandColors[brand]
                    : "border-white/10 bg-white/[0.04] hover:bg-white/[0.08]"
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg font-bold ${
                    filterType === brand ? "bg-white/20" : "bg-white/10"
                  }`}>
                    {brand === "OPPO" && "O"}
                    {brand === "REALME" && "R"}
                    {brand === "VIVO" && "v"}
                    {brand === "HONOR" && "荣"}
                  </div>
                  <div className="flex-1 text-left">
                    <div className={`font-semibold ${filterType === brand ? "" : "text-ink-100"}`}>
                      {BRAND_CONFIG[brand].label}
                    </div>
                    <div className={`text-sm ${filterType === brand ? "opacity-80" : "text-ink-400"}`}>
                      {brandStats[brand]} 个预设
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </PageHeader>

        {/* 搜索栏 */}
        <PageHeader delay={0.1}>
          <div className="max-w-2xl mx-auto mb-8">
            <div className="relative group">
              <Search className="absolute left-5 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-300 group-focus-within:text-hasselblad-500 transition-colors" />
              <input
                type="text"
                value={localSearch}
                onChange={(e) => setLocalSearch(e.target.value)}
                placeholder="搜索预设名称、品牌、标签..."
                maxLength={50}
                className="w-full pl-14 pr-14 py-4 rounded-2xl glass-strong text-ink-50 placeholder-ink-400 focus:border-hasselblad-500/50 focus:outline-none focus:ring-2 focus:ring-hasselblad-500/20 transition-all"
              />
              {localSearch && (
                <button
                  onClick={() => setLocalSearch("")}
                  className="absolute right-4 top-1/2 -translate-y-1/2 w-7 h-7 rounded-full hover:bg-white/10 flex items-center justify-center"
                  aria-label="清除"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
            <div className="text-center text-xs text-ink-400 mt-2">
              {localSearch.length >= 50 && "已达最大输入长度"}
            </div>
          </div>
        </PageHeader>

        {/* 筛选器 */}
        <PageHeader delay={0.2}>
          <div className="flex items-center justify-center mb-12 flex-wrap gap-2">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full chip mr-2">
              <SlidersHorizontal className="w-3.5 h-3.5" />
              筛选
            </div>
            {filterOptions.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setFilterType(opt.value)}
                className={`chip transition-all ${
                  filterType === opt.value ? "chip-active" : ""
                }`}
              >
                {opt.label}
                {opt.value === "FAVORITES" && favorites.size > 0 && (
                  <span className="ml-1 text-[10px] opacity-70">({favorites.size})</span>
                )}
              </button>
            ))}
          </div>
        </PageHeader>

        {/* 网格 */}
        <AnimatePresence mode="popLayout">
          {filtered.length > 0 ? (
            <motion.div
              key={`${filterType}-${searchQuery}`}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
            >
              {filtered.map((preset, i) => (
                <PresetCard key={preset.id} preset={preset} index={i} />
              ))}
            </motion.div>
          ) : (
            <motion.div
              key="empty"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              className="text-center py-20"
            >
              <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-hasselblad-500/10 flex items-center justify-center">
                <Search className="w-8 h-8 text-hasselblad-500" />
              </div>
              <h3 className="font-display text-2xl font-bold mb-2">
                {filterType === "FAVORITES" ? "暂无收藏的预设" : "没有找到匹配的预设"}
              </h3>
              <p className="text-ink-300">
                {filterType === "FAVORITES"
                  ? "去发现并收藏你喜欢的预设吧"
                  : "试试调整筛选条件或搜索其他关键词"}
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}