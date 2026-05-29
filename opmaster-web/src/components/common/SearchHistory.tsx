import { motion } from 'framer-motion';
import { Clock, X, Search } from 'lucide-react';
import { useState } from 'react';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SearchHistoryState {
  searchHistory: string[];
  addToHistory: (query: string) => void;
  removeFromHistory: (query: string) => void;
  clearHistory: () => void;
}

export const useSearchHistoryStore = create<SearchHistoryState>()(
  persist(
    (set, get) => ({
      searchHistory: [],
      
      addToHistory: (query) => {
        if (!query.trim()) return;
        const { searchHistory } = get();
        const filtered = searchHistory.filter(item => item !== query);
        const newHistory = [query, ...filtered].slice(0, 10);
        set({ searchHistory: newHistory });
      },
      
      removeFromHistory: (query) => {
        const { searchHistory } = get();
        set({ searchHistory: searchHistory.filter(item => item !== query) });
      },
      
      clearHistory: () => {
        set({ searchHistory: [] });
      }
    }),
    {
      name: 'search-history-storage'
    }
  )
);

interface SearchHistoryProps {
  onSearch: (query: string) => void;
  onClose: () => void;
}

export default function SearchHistory({ onSearch, onClose }: SearchHistoryProps) {
  const { searchHistory, removeFromHistory, clearHistory } = useSearchHistoryStore();
  const [localQuery, setLocalQuery] = useState('');

  const handleSubmit = (query: string) => {
    if (query.trim()) {
      onSearch(query);
      onClose();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && localQuery.trim()) {
      handleSubmit(localQuery);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      className="absolute top-full left-0 right-0 mt-2 bg-deep-space/95 backdrop-blur-xl rounded-2xl border border-white/10 overflow-hidden shadow-2xl z-50"
    >
      {/* 搜索输入框 */}
      <div className="p-3 border-b border-white/10">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-tertiary" />
          <input
            type="text"
            value={localQuery}
            onChange={(e) => setLocalQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="搜索预设、场景..."
            className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-white/5 border border-white/10 focus:border-oppo-sunrise-gold focus:outline-none text-sm"
            autoFocus
          />
          {localQuery && (
            <button
              onClick={() => setLocalQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-white/10"
            >
              <X className="w-4 h-4 text-text-tertiary" />
            </button>
          )}
        </div>
      </div>

      {/* 历史记录 */}
      {searchHistory.length > 0 && (
        <div className="p-3">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-xs font-medium text-text-tertiary uppercase tracking-wider flex items-center gap-1">
              <Clock className="w-3 h-3" />
              搜索历史
            </h3>
            <button
              onClick={clearHistory}
              className="text-xs text-oppo-sunrise-gold hover:underline"
            >
              清空
            </button>
          </div>
          
          <div className="space-y-1">
            {searchHistory.map((query, index) => (
              <motion.div
                key={query}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.03 }}
                className="flex items-center gap-2 p-2 rounded-lg hover:bg-white/5 cursor-pointer group transition-colors"
                onClick={() => handleSubmit(query)}
              >
                <Clock className="w-4 h-4 text-text-tertiary flex-shrink-0" />
                <span className="flex-1 text-sm text-text-secondary">{query}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFromHistory(query);
                  }}
                  className="p-1 rounded-full opacity-0 group-hover:opacity-100 hover:bg-white/10 transition-all"
                >
                  <X className="w-3 h-3 text-text-tertiary" />
                </button>
              </motion.div>
            ))}
          </div>
        </div>
      )}

      {/* 热门搜索 */}
      <div className="p-3 border-t border-white/10">
        <h3 className="text-xs font-medium text-text-tertiary uppercase tracking-wider mb-2">
          热门搜索
        </h3>
        <div className="flex flex-wrap gap-2">
          {['人像', '哈苏', '夜景', '美食', '胶片'].map((tag) => (
            <button
              key={tag}
              onClick={() => handleSubmit(tag)}
              className="px-3 py-1.5 rounded-full bg-white/5 hover:bg-white/10 text-sm text-text-secondary transition-colors"
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      {/* 搜索建议 */}
      {localQuery && (
        <div className="p-3 border-t border-white/10">
          <h3 className="text-xs font-medium text-text-tertiary uppercase tracking-wider mb-2">
            搜索建议
          </h3>
          <div className="space-y-1">
            <button
              onClick={() => handleSubmit(localQuery)}
              className="w-full flex items-center gap-2 p-2 rounded-lg hover:bg-white/5 text-left"
            >
              <Search className="w-4 h-4 text-text-tertiary" />
              <span className="text-sm text-white">搜索"{localQuery}"</span>
            </button>
          </div>
        </div>
      )}
    </motion.div>
  );
}
