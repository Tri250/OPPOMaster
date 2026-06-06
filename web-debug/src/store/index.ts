import { create } from 'zustand'

// 系统状态
interface SystemState {
  status: 'running' | 'stopped'
  uptime: number
  memory: { used: number; total: number }
  network: { connected: boolean; latency: number }
}

interface StatusStore {
  systemStatus: SystemState | null
  metrics: { cpu: number; memory: number; network: number; fps: number } | null
  history: Array<{ time: string; cpu: number; memory: number; network: number }>
  fetchStatus: () => Promise<void>
  fetchMetrics: () => Promise<void>
  fetchHistory: () => Promise<void>
}

export const useStatusStore = create<StatusStore>((set) => ({
  systemStatus: null,
  metrics: null,
  history: [],
  fetchStatus: async () => {
    const res = await fetch('/api/status')
    const data = await res.json()
    if (data.success) {
      set({ systemStatus: data.data })
    }
  },
  fetchMetrics: async () => {
    const res = await fetch('/api/status/metrics')
    const data = await res.json()
    if (data.success) {
      set({ metrics: data.data })
    }
  },
  fetchHistory: async () => {
    const res = await fetch('/api/status/metrics/history')
    const data = await res.json()
    if (data.success) {
      set({ history: data.data })
    }
  }
}))

// 日志状态
interface LogEntry {
  id: string
  timestamp: number
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
  tag: string
  message: string
  stackTrace?: string
}

interface LogsStore {
  logs: LogEntry[]
  level: string
  search: string
  setLevel: (level: string) => void
  setSearch: (search: string) => void
  fetchLogs: () => Promise<void>
  clearLogs: () => Promise<void>
}

export const useLogsStore = create<LogsStore>((set, get) => ({
  logs: [],
  level: 'ALL',
  search: '',
  setLevel: (level) => set({ level }),
  setSearch: (search) => set({ search }),
  fetchLogs: async () => {
    const { level, search } = get()
    const params = new URLSearchParams()
    if (level !== 'ALL') params.append('level', level)
    if (search) params.append('search', search)
    params.append('limit', '100')
    
    const res = await fetch(`/api/logs?${params}`)
    const data = await res.json()
    if (data.success) {
      set({ logs: data.data })
    }
  },
  clearLogs: async () => {
    await fetch('/api/logs', { method: 'DELETE' })
    set({ logs: [] })
  }
}))

// 预设状态
interface Preset {
  id: string
  name: string
  author: string
  description: string
  coverUrl: string
  params: Record<string, any>
  tags: string[]
  rating: number
  downloadCount: number
  isNew: boolean
  createdAt: string
}

interface PresetsStore {
  presets: Preset[]
  search: string
  sort: string
  setSearch: (search: string) => void
  setSort: (sort: string) => void
  fetchPresets: () => Promise<void>
  deletePreset: (id: string) => Promise<void>
}

export const usePresetsStore = create<PresetsStore>((set, get) => ({
  presets: [],
  search: '',
  sort: 'newest',
  setSearch: (search) => set({ search }),
  setSort: (sort) => set({ sort }),
  fetchPresets: async () => {
    const { search, sort } = get()
    const params = new URLSearchParams()
    if (search) params.append('search', search)
    params.append('sort', sort)
    
    const res = await fetch(`/api/presets?${params}`)
    const data = await res.json()
    if (data.success) {
      set({ presets: data.data })
    }
  },
  deletePreset: async (id) => {
    await fetch(`/api/presets/${id}`, { method: 'DELETE' })
    get().fetchPresets()
  }
}))

// 设备状态
interface DeviceInfo {
  model: string
  manufacturer: string
  androidVersion: string
  sdkVersion: number
  screenWidth: number
  screenHeight: number
  density: number
  totalMemory: number
  availableMemory: number
  totalStorage: number
  availableStorage: number
  cpu: string
  gpu: string
  cameraInfo: Record<string, string>
}

interface DeviceStore {
  deviceInfo: DeviceInfo | null
  performance: any
  appInfo: any
  fetchDeviceInfo: () => Promise<void>
  fetchPerformance: () => Promise<void>
  fetchAppInfo: () => Promise<void>
}

export const useDeviceStore = create<DeviceStore>((set) => ({
  deviceInfo: null,
  performance: null,
  appInfo: null,
  fetchDeviceInfo: async () => {
    const res = await fetch('/api/device')
    const data = await res.json()
    if (data.success) {
      set({ deviceInfo: data.data })
    }
  },
  fetchPerformance: async () => {
    const res = await fetch('/api/device/performance')
    const data = await res.json()
    if (data.success) {
      set({ performance: data.data })
    }
  },
  fetchAppInfo: async () => {
    const res = await fetch('/api/device/app')
    const data = await res.json()
    if (data.success) {
      set({ appInfo: data.data })
    }
  }
}))

// 节日状态
interface Holiday {
  id: string
  name: string
  greeting: string
  startDate: string
  endDate: string
  theme: {
    primaryColor: string
    secondaryColor: string
    backgroundGradient: string[]
    accentColor: string
    icon: string
  }
  presetIds: string[]
  isActive: boolean
}

interface HolidaysStore {
  holidays: Holiday[]
  currentHoliday: Holiday | null
  fetchHolidays: () => Promise<void>
  fetchCurrentHoliday: () => Promise<void>
  updateHoliday: (id: string, data: Partial<Holiday>) => Promise<void>
}

export const useHolidaysStore = create<HolidaysStore>((set, get) => ({
  holidays: [],
  currentHoliday: null,
  fetchHolidays: async () => {
    const res = await fetch('/api/holidays')
    const data = await res.json()
    if (data.success) {
      set({ holidays: data.data })
    }
  },
  fetchCurrentHoliday: async () => {
    const res = await fetch('/api/holidays/current')
    const data = await res.json()
    if (data.success) {
      set({ currentHoliday: data.data })
    }
  },
  updateHoliday: async (id, data) => {
    await fetch(`/api/holidays/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    get().fetchHolidays()
  }
}))
