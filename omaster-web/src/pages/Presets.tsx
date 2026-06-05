import { useMemo, useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Search, SlidersHorizontal, X } from "lucide-react";
import PageHeader from "../components/PageHeader";
import PresetCard from "../components/PresetCard";
import { presets } from "../data/mock";
import { useAppStore } from "../store/useAppStore";
import type { FilterType } from "../types";

const filterOptions: { value: FilterType; label: string }[] = [
  { value: "ALL", label: "全部" },
  { value: "FAVORITES", label: "收藏" },
  { value: "HNCS", label: "HNCS" },
  { value: "FIND_X", label: "Find X" },
  { value: "RENO", label: "Reno" },
  { value: "NEW", label: "最新" },
  { value: "TRENDING", label: "热门" },
];

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
        preset.deviceModel.toLowerCase().includes(q) ||
        preset.sceneType.toLowerCase().includes(q) ||
        preset.tags.some((t) => t.toLowerCase().includes(q)) ||
        preset.author.toLowerCase().includes(q);

      const matchesFilter = (() => {
        switch (filterType) {
          case "ALL":
            return true;
          case "FAVORITES":
            return favorites.has(preset.id);
          case "HNCS":
            return preset.isHncsCertified;
          case "FIND_X":
            return preset.deviceModel.toLowerCase().includes("find x");
          case "RENO":
            return preset.deviceModel.toLowerCase().includes("reno");
          case "NEW":
            return preset.version.startsWith("3.0") || preset.downloadCount < 10000;
          case "TRENDING":
            return preset.downloadCount > 100000;
        }
      })();

      return matchesQuery && matchesFilter;
    });
  }, [searchQuery, filterType, favorites]);

  const titleMap: Record<FilterType, string> = {
    ALL: "哈苏大师预设",
    FAVORITES: "我的收藏",
    HNCS: "HNCS 认证",
    FIND_X: "Find X 系列",
    RENO: "Reno 系列",
    NEW: "最新预设",
    TRENDING: "热门趋势",
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
                : `共 ${filtered.length} 个预设 · 哈苏官方认证 · AI 智能推荐`}
            </p>
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
                placeholder="搜索预设名称、设备、标签、场景..."
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
