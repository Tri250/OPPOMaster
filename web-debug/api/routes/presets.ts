import { Router, type Request, type Response } from 'express'
import { randomUUID } from 'crypto'

const router = Router()

// 模拟预设数据
const mockPresets = [
  {
    id: '1',
    name: '哈苏自然',
    author: '小O帮帮官方',
    description: '还原哈苏相机自然色彩',
    coverUrl: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    params: {
      iso: 100,
      shutter: '1/125',
      ev: '0',
      wb: 'Auto',
      focal_length: '24mm',
      aperture: 'f/2.8',
      filter: 'Natural',
      hncs: true
    },
    tags: ['风景', '自然', '哈苏'],
    rating: 4.8,
    downloadCount: 12580,
    isNew: false,
    createdAt: '2024-01-15'
  },
  {
    id: '2',
    name: '胶片复古',
    author: '摄影师小王',
    description: '模拟经典胶片色彩',
    coverUrl: 'https://images.unsplash.com/photo-1493863641943-9b68992a8d07?w=400',
    params: {
      iso: 400,
      shutter: '1/60',
      ev: '-0.3',
      wb: '5500K',
      focal_length: '35mm',
      aperture: 'f/1.8',
      filter: 'Vintage',
      hncs: false
    },
    tags: ['复古', '胶片', '人像'],
    rating: 4.6,
    downloadCount: 8920,
    isNew: true,
    createdAt: '2024-12-20'
  },
  {
    id: '3',
    name: '夜景霓虹',
    author: '城市猎人',
    description: '城市夜景专用预设',
    coverUrl: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400',
    params: {
      iso: 800,
      shutter: '1/30',
      ev: '+0.7',
      wb: '3200K',
      focal_length: '50mm',
      aperture: 'f/1.4',
      filter: 'Neon',
      hncs: false
    },
    tags: ['夜景', '城市', '霓虹'],
    rating: 4.5,
    downloadCount: 6540,
    isNew: false,
    createdAt: '2024-06-10'
  },
  {
    id: '4',
    name: '清新日系',
    author: '樱花妹',
    description: '清新淡雅日系风格',
    coverUrl: 'https://images.unsplash.com/photo-1522383225653-ed111181a951?w=400',
    params: {
      iso: 200,
      shutter: '1/250',
      ev: '+0.3',
      wb: '6000K',
      focal_length: '35mm',
      aperture: 'f/2.0',
      filter: 'Fresh',
      hncs: false
    },
    tags: ['日系', '清新', '人像'],
    rating: 4.7,
    downloadCount: 11200,
    isNew: false,
    createdAt: '2024-03-20'
  },
  {
    id: '5',
    name: '黑白人文',
    author: '街拍大师',
    description: '经典黑白人文摄影',
    coverUrl: 'https://images.unsplash.com/photo-1444723121867-c6126bab4d6e?w=400',
    params: {
      iso: 400,
      shutter: '1/125',
      ev: '0',
      wb: 'Auto',
      focal_length: '28mm',
      aperture: 'f/2.8',
      filter: 'B&W',
      hncs: false
    },
    tags: ['黑白', '人文', '街拍'],
    rating: 4.9,
    downloadCount: 9870,
    isNew: true,
    createdAt: '2024-12-18'
  }
]

let presets = [...mockPresets]

// 获取所有预设
router.get('/', (req: Request, res: Response) => {
  const { search, tag, sort = 'newest' } = req.query
  let result = [...presets]
  
  // 搜索
  if (search) {
    const searchStr = search.toString().toLowerCase()
    result = result.filter(p => 
      p.name.toLowerCase().includes(searchStr) ||
      p.author.toLowerCase().includes(searchStr) ||
      p.description.toLowerCase().includes(searchStr)
    )
  }
  
  // 标签筛选
  if (tag) {
    result = result.filter(p => p.tags.includes(tag.toString()))
  }
  
  // 排序
  switch (sort) {
    case 'newest':
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      break
    case 'popular':
      result.sort((a, b) => b.downloadCount - a.downloadCount)
      break
    case 'rating':
      result.sort((a, b) => b.rating - a.rating)
      break
  }
  
  res.json({
    success: true,
    data: result
  })
})

// 获取单个预设
router.get('/:id', (req: Request, res: Response) => {
  const preset = presets.find(p => p.id === req.params.id)
  if (!preset) {
    res.status(404).json({ success: false, error: '预设不存在' })
    return
  }
  res.json({ success: true, data: preset })
})

// 创建预设
router.post('/', (req: Request, res: Response) => {
  const newPreset = {
    id: randomUUID(),
    ...req.body,
    rating: 0,
    downloadCount: 0,
    createdAt: new Date().toISOString().split('T')[0]
  }
  presets.unshift(newPreset)
  res.json({ success: true, data: newPreset })
})

// 更新预设
router.put('/:id', (req: Request, res: Response) => {
  const index = presets.findIndex(p => p.id === req.params.id)
  if (index === -1) {
    res.status(404).json({ success: false, error: '预设不存在' })
    return
  }
  presets[index] = { ...presets[index], ...req.body }
  res.json({ success: true, data: presets[index] })
})

// 删除预设
router.delete('/:id', (req: Request, res: Response) => {
  const index = presets.findIndex(p => p.id === req.params.id)
  if (index === -1) {
    res.status(404).json({ success: false, error: '预设不存在' })
    return
  }
  presets.splice(index, 1)
  res.json({ success: true, message: '预设已删除' })
})

export default router
