
import { Settings } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useStore } from '@/store/useStore';
import { SearchBar } from '@/components/SearchBar';
import { FilterChips } from '@/components/FilterChips';
import { PresetCard } from '@/components/PresetCard';

export function Home() {
  const navigate = useNavigate();
  const { 
    searchQuery, 
    filterType, 
    setSearchQuery, 
    setFilterType, 
    toggleFavorite,
    getFilteredPresets 
  } = useStore();
  
  const filteredPresets = getFilteredPresets();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="sticky top-0 z-10 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-200 dark:border-gray-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-yellow-500 bg-clip-text text-transparent">
              OMaster
            </h1>
            <button
              onClick={() => navigate('/settings')}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg transition-colors"
            >
              <Settings className="w-6 h-6 text-gray-600 dark:text-gray-400" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-6 mb-8">
          <SearchBar query={searchQuery} onQueryChange={setSearchQuery} />
          <FilterChips 
            selectedFilter={filterType} 
            onFilterSelected={setFilterType} 
          />
        </div>

        {filteredPresets.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-gray-500 dark:text-gray-400 text-lg">
              没有找到匹配的预设
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredPresets.map((preset) => (
              <PresetCard
                key={preset.id}
                preset={preset}
                onClick={() => navigate(`/detail/${preset.id}`)}
                onFavoriteToggle={() => toggleFavorite(preset.id)}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
