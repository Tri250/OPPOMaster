import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Preset } from '../data/mockPresets';

// ==================== 订阅管理 ====================

export interface SubscriptionSource {
  id: string;
  name: string;
  url: string;
  version: string;
  lastCheck: string | null;
  updateInterval: number;
  enabled: boolean;
  autoUpdate: boolean;
  presets: Preset[];
}

interface SubscriptionState {
  subscriptions: SubscriptionSource[];
  activeSubscription: string | null;
  isChecking: boolean;
  lastUpdate: string | null;
  updateAvailable: boolean;
  
  addSubscription: (source: Omit<SubscriptionSource, 'id' | 'lastCheck' | 'presets'>) => void;
  removeSubscription: (id: string) => void;
  updateSubscription: (id: string, updates: Partial<SubscriptionSource>) => void;
  setActiveSubscription: (id: string | null) => void;
  checkForUpdates: () => Promise<void>;
  syncSubscription: (id: string) => Promise<void>;
  getSubscriptionPresets: (id: string) => Preset[];
  toggleEnabled: (id: string) => void;
  toggleAutoUpdate: (id: string) => void;
}

// ==================== 云同步 ====================

export interface SyncHistory {
  id: string;
  name: string;
  date: string;
  status: 'completed' | 'failed' | 'pending';
  size: string;
  type: 'upload' | 'download';
  presetId?: string;
}

export interface SyncSettings {
  autoSync: boolean;
  syncOnWifi: boolean;
  syncInterval: number;
  lastSyncTime: string | null;
  cloudStorage: {
    total: number;
    used: number;
  };
}

interface CloudSyncState {
  syncHistory: SyncHistory[];
  settings: SyncSettings;
  isSyncing: boolean;
  connectedDevices: Array<{
    id: string;
    name: string;
    platform: string;
    lastSync: string;
    status: 'connected' | 'disconnected';
  }>;
  
  setAutoSync: (enabled: boolean) => void;
  setSyncOnWifi: (enabled: boolean) => void;
  setSyncInterval: (interval: number) => void;
  addSyncHistory: (history: Omit<SyncHistory, 'id'>) => void;
  syncNow: () => Promise<void>;
  clearSyncHistory: () => void;
  addConnectedDevice: (device: Omit<CloudSyncState['connectedDevices'][0], 'lastSync'>) => void;
  removeConnectedDevice: (id: string) => void;
}

// ==================== 收藏管理 ====================

interface FavoritesState {
  favorites: Set<string>;
  recentViews: string[];
  
  toggleFavorite: (presetId: string) => void;
  addToRecent: (presetId: string) => void;
  getFavorites: () => string[];
  getRecentViews: () => string[];
  clearRecentViews: () => void;
}

// ==================== 订阅管理Store ====================

export const useSubscriptionStore = create<SubscriptionState>()(
  persist(
    (set, get) => ({
      subscriptions: [
        {
          id: 'official',
          name: '官方预设库',
          url: 'https://api.omaster.com/presets/official.json',
          version: '2.0.0',
          lastCheck: null,
          updateInterval: 86400000, // 24小时
          enabled: true,
          autoUpdate: true,
          presets: []
        },
        {
          id: 'community',
          name: '社区预设库',
          url: 'https://api.omaster.com/presets/community.json',
          version: '1.5.0',
          lastCheck: null,
          updateInterval: 172800000, // 48小时
          enabled: true,
          autoUpdate: false,
          presets: []
        }
      ],
      activeSubscription: 'official',
      isChecking: false,
      lastUpdate: null,
      updateAvailable: false,
      
      addSubscription: (source) => {
        const id = `sub_${Date.now()}`;
        set((state) => ({
          subscriptions: [
            ...state.subscriptions,
            { ...source, id, lastCheck: null, presets: [] }
          ]
        }));
      },
      
      removeSubscription: (id) => {
        set((state) => ({
          subscriptions: state.subscriptions.filter((s) => s.id !== id),
          activeSubscription: state.activeSubscription === id ? null : state.activeSubscription
        }));
      },
      
      updateSubscription: (id, updates) => {
        set((state) => ({
          subscriptions: state.subscriptions.map((s) =>
            s.id === id ? { ...s, ...updates } : s
          )
        }));
      },
      
      setActiveSubscription: (id) => {
        set({ activeSubscription: id });
      },
      
      checkForUpdates: async () => {
        set({ isChecking: true });
        
        // 模拟检查更新
        await new Promise((resolve) => setTimeout(resolve, 1500));
        
        set((state) => ({
          isChecking: false,
          lastUpdate: new Date().toISOString(),
          updateAvailable: Math.random() > 0.5,
          subscriptions: state.subscriptions.map((s) => ({
            ...s,
            lastCheck: new Date().toISOString()
          }))
        }));
      },
      
      syncSubscription: async (id) => {
        set({ isSyncing: true });
        
        // 模拟同步
        await new Promise((resolve) => setTimeout(resolve, 2000));
        
        set((state) => ({
          isSyncing: false,
          subscriptions: state.subscriptions.map((s) =>
            s.id === id ? { ...s, lastCheck: new Date().toISOString() } : s
          )
        }));
      },
      
      getSubscriptionPresets: (id) => {
        const subscription = get().subscriptions.find((s) => s.id === id);
        return subscription?.presets || [];
      },
      
      toggleEnabled: (id) => {
        set((state) => ({
          subscriptions: state.subscriptions.map((s) =>
            s.id === id ? { ...s, enabled: !s.enabled } : s
          )
        }));
      },
      
      toggleAutoUpdate: (id) => {
        set((state) => ({
          subscriptions: state.subscriptions.map((s) =>
            s.id === id ? { ...s, autoUpdate: !s.autoUpdate } : s
          )
        }));
      }
    }),
    {
      name: 'subscription-storage'
    }
  )
);

// ==================== 云同步Store ====================

export const useCloudSyncStore = create<CloudSyncState>()(
  persist(
    (set, get) => ({
      syncHistory: [
        {
          id: '1',
          name: '富士胶片预设包',
          date: '2024-01-15 14:30',
          status: 'completed',
          size: '2.3 MB',
          type: 'download'
        },
        {
          id: '2',
          name: '人像柔光参数',
          date: '2024-01-15 12:15',
          status: 'completed',
          size: '856 KB',
          type: 'upload'
        },
        {
          id: '3',
          name: '徕卡预设收藏',
          date: '2024-01-14 18:45',
          status: 'failed',
          size: '1.8 MB',
          type: 'download'
        },
        {
          id: '4',
          name: '自定义预设',
          date: '2024-01-14 10:20',
          status: 'completed',
          size: '1.2 MB',
          type: 'upload'
        }
      ],
      settings: {
        autoSync: true,
        syncOnWifi: true,
        syncInterval: 3600000, // 1小时
        lastSyncTime: '2024-01-15 14:30',
        cloudStorage: {
          total: 5368709120, // 5GB
          used: 2684354560 // 2.5GB
        }
      },
      isSyncing: false,
      connectedDevices: [
        {
          id: 'device_1',
          name: 'OPPO Find X8 Ultra',
          platform: 'Android',
          lastSync: '2024-01-15 14:30',
          status: 'connected'
        },
        {
          id: 'device_2',
          name: 'OnePlus 14',
          platform: 'Android',
          lastSync: '2024-01-14 20:15',
          status: 'connected'
        },
        {
          id: 'device_3',
          name: 'Chrome浏览器',
          platform: 'Web',
          lastSync: '2024-01-15 10:00',
          status: 'connected'
        }
      ],
      
      setAutoSync: (enabled) => {
        set((state) => ({
          settings: { ...state.settings, autoSync: enabled }
        }));
      },
      
      setSyncOnWifi: (enabled) => {
        set((state) => ({
          settings: { ...state.settings, syncOnWifi: enabled }
        }));
      },
      
      setSyncInterval: (interval) => {
        set((state) => ({
          settings: { ...state.settings, syncInterval: interval }
        }));
      },
      
      addSyncHistory: (history) => {
        const id = `sync_${Date.now()}`;
        set((state) => ({
          syncHistory: [{ ...history, id }, ...state.syncHistory].slice(0, 50)
        }));
      },
      
      syncNow: async () => {
        set({ isSyncing: true });
        
        // 模拟同步过程
        await new Promise((resolve) => setTimeout(resolve, 3000));
        
        const historyEntry: Omit<SyncHistory, 'id'> = {
          name: '同步完成',
          date: new Date().toISOString().split('T')[1]?.slice(0, 5) || new Date().toLocaleString(),
          status: 'completed',
          size: '0 KB',
          type: 'download'
        };
        
        set((state) => ({
          isSyncing: false,
          lastUpdate: new Date().toISOString(),
          settings: { ...state.settings, lastSyncTime: new Date().toISOString() },
          syncHistory: [{ ...historyEntry, id: `sync_${Date.now()}` }, ...state.syncHistory]
        }));
      },
      
      clearSyncHistory: () => {
        set({ syncHistory: [] });
      },
      
      addConnectedDevice: (device) => {
        set((state) => ({
          connectedDevices: [
            ...state.connectedDevices,
            { ...device, lastSync: new Date().toISOString() }
          ]
        }));
      },
      
      removeConnectedDevice: (id) => {
        set((state) => ({
          connectedDevices: state.connectedDevices.filter((d) => d.id !== id)
        }));
      }
    }),
    {
      name: 'cloud-sync-storage'
    }
  )
);

// ==================== 收藏Store ====================

export const useFavoritesStore = create<FavoritesState>()(
  persist(
    (set, get) => ({
      favorites: new Set<string>(),
      recentViews: [],
      
      toggleFavorite: (presetId) => {
        set((state) => {
          const newFavorites = new Set(state.favorites);
          if (newFavorites.has(presetId)) {
            newFavorites.delete(presetId);
          } else {
            newFavorites.add(presetId);
          }
          return { favorites: newFavorites };
        });
      },
      
      addToRecent: (presetId) => {
        set((state) => {
          const filtered = state.recentViews.filter((id) => id !== presetId);
          return {
            recentViews: [presetId, ...filtered].slice(0, 20)
          };
        });
      },
      
      getFavorites: () => {
        return Array.from(get().favorites);
      },
      
      getRecentViews: () => {
        return get().recentViews;
      },
      
      clearRecentViews: () => {
        set({ recentViews: [] });
      }
    }),
    {
      name: 'favorites-storage',
      partialize: (state) => ({
        favorites: Array.from(state.favorites),
        recentViews: state.recentViews
      }),
      merge: (persistedState: any, currentState) => ({
        ...currentState,
        favorites: new Set(persistedState?.favorites || []),
        recentViews: persistedState?.recentViews || []
      })
    }
  )
);
