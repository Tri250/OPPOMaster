
import { FilterType } from '@/types';

interface FilterChipsProps {
  selectedFilter: FilterType;
  onFilterSelected: (filter: FilterType) => void;
}

const filters: { type: FilterType; label: string }[] = [
  { type: 'ALL', label: '全部' },
  { type: 'FAVORITES', label: '收藏' },
  { type: 'HNCS', label: 'HNCS' },
  { type: 'FIND_X', label: 'Find X' },
  { type: 'RENO', label: 'Reno' },
];

export function FilterChips({ selectedFilter, onFilterSelected }: FilterChipsProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {filters.map((filter) => (
        <button
          key={filter.type}
          onClick={() => onFilterSelected(filter.type)}
          className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
            selectedFilter === filter.type
              ? 'bg-blue-600 text-white shadow-md'
              : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'
          }`}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
}
