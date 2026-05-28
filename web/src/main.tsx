
import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './index.css';
import { useStore } from './store/useStore';

function ThemeInitializer() {
  const { themeMode } = useStore();
  
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
  
  return <App />;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeInitializer />
  </StrictMode>
);
