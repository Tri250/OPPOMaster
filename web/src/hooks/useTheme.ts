
import { useEffect } from 'react';
import { useStore } from '@/store/useStore';
import { ThemeMode } from '@/types';

export function useTheme() {
  const { themeMode, setThemeMode } = useStore();

  useEffect(() => {
    let effectiveTheme: 'light' | 'dark';
    
    if (themeMode === 'system') {
      effectiveTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    } else {
      effectiveTheme = themeMode;
    }

    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.classList.add(effectiveTheme);
  }, [themeMode]);

  const isDark = 
    themeMode === 'dark' || 
    (themeMode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

  return {
    themeMode,
    setThemeMode,
    isDark,
  };
}
