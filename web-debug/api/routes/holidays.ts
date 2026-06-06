import { Router, type Request, type Response } from 'express'

const router = Router()

// 节日数据
const holidays = [
  {
    id: 'spring_festival',
    name: '春节',
    greeting: '新春快乐，万事如意！',
    startDate: '2025-01-29',
    endDate: '2025-02-04',
    theme: {
      primaryColor: '#D32F2F',
      secondaryColor: '#FFC107',
      backgroundGradient: ['#D32F2F', '#B71C1C'],
      accentColor: '#FFD700',
      icon: '🧧'
    },
    presetIds: ['spring_red', 'lantern_warm', 'new_year_gold'],
    isActive: true
  },
  {
    id: 'valentine',
    name: '情人节',
    greeting: '愿爱如星光，永恒闪耀！',
    startDate: '2025-02-14',
    endDate: '2025-02-14',
    theme: {
      primaryColor: '#E91E63',
      secondaryColor: '#F8BBD9',
      backgroundGradient: ['#E91E63', '#C2185B'],
      accentColor: '#FF80AB',
      icon: '💕'
    },
    presetIds: ['romantic_pink', 'rose_soft'],
    isActive: true
  },
  {
    id: 'labor_day',
    name: '劳动节',
    greeting: '劳动最光荣，节日快乐！',
    startDate: '2025-05-01',
    endDate: '2025-05-05',
    theme: {
      primaryColor: '#2196F3',
      secondaryColor: '#90CAF9',
      backgroundGradient: ['#2196F3', '#1565C0'],
      accentColor: '#64B5F6',
      icon: '🛠️'
    },
    presetIds: ['worker_strong', 'industrial_cool'],
    isActive: true
  },
  {
    id: 'dragon_boat',
    name: '端午节',
    greeting: '粽香飘万里，端午安康！',
    startDate: '2025-05-31',
    endDate: '2025-05-31',
    theme: {
      primaryColor: '#009688',
      secondaryColor: '#80CBC4',
      backgroundGradient: ['#009688', '#00695C'],
      accentColor: '#4DB6AC',
      icon: '🐲'
    },
    presetIds: ['dragon_green', 'rice_white'],
    isActive: true
  },
  {
    id: 'mid_autumn',
    name: '中秋节',
    greeting: '月圆人团圆，中秋快乐！',
    startDate: '2025-10-06',
    endDate: '2025-10-06',
    theme: {
      primaryColor: '#FF9800',
      secondaryColor: '#FFE0B2',
      backgroundGradient: ['#FF9800', '#F57C00'],
      accentColor: '#FFCC80',
      icon: '🌕'
    },
    presetIds: ['moon_warm', 'night_gold'],
    isActive: true
  },
  {
    id: 'national_day',
    name: '国庆节',
    greeting: '祖国繁荣昌盛，国庆快乐！',
    startDate: '2025-10-01',
    endDate: '2025-10-07',
    theme: {
      primaryColor: '#D32F2F',
      secondaryColor: '#FFEB3B',
      backgroundGradient: ['#D32F2F', '#B71C1C'],
      accentColor: '#FFC107',
      icon: '🇨🇳'
    },
    presetIds: ['china_red', 'celebration_gold'],
    isActive: true
  },
  {
    id: 'christmas',
    name: '圣诞节',
    greeting: '圣诞快乐，平安喜乐！',
    startDate: '2025-12-25',
    endDate: '2025-12-25',
    theme: {
      primaryColor: '#2E7D32',
      secondaryColor: '#EF5350',
      backgroundGradient: ['#1B5E20', '#2E7D32'],
      accentColor: '#FFFFFF',
      icon: '🎄'
    },
    presetIds: ['christmas_green', 'snow_white', 'warm_fire'],
    isActive: true
  },
  {
    id: 'new_year',
    name: '元旦',
    greeting: '新年快乐，万事如意！',
    startDate: '2025-01-01',
    endDate: '2025-01-01',
    theme: {
      primaryColor: '#673AB7',
      secondaryColor: '#FFD700',
      backgroundGradient: ['#673AB7', '#512DA8'],
      accentColor: '#FFD700',
      icon: '🎆'
    },
    presetIds: ['new_year_purple', 'firework_colorful'],
    isActive: true
  }
]

// 获取所有节日
router.get('/', (req: Request, res: Response) => {
  res.json({
    success: true,
    data: holidays
  })
})

// 获取当前节日
router.get('/current', (req: Request, res: Response) => {
  const today = new Date().toISOString().split('T')[0]
  const current = holidays.find(h => today >= h.startDate && today <= h.endDate)
  res.json({
    success: true,
    data: current || null
  })
})

// 更新节日配置
router.put('/:id', (req: Request, res: Response) => {
  const index = holidays.findIndex(h => h.id === req.params.id)
  if (index === -1) {
    res.status(404).json({ success: false, error: '节日不存在' })
    return
  }
  holidays[index] = { ...holidays[index], ...req.body }
  res.json({ success: true, data: holidays[index] })
})

export default router
