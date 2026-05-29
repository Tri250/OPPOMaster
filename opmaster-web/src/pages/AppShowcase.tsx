import {import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers,import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menuimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      iconimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-himport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整',import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradientimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', 'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      titleimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filterimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondaryimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      pathimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUTimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-pimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-spaceimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointerimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 himport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2simport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 himport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixedimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation"import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-betweenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touchimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orangeimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium textimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedbackimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-bodyimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-outimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hiddenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <spanimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hiddenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors durationimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ?import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </divimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacityimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'eimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="fleximport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClickimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orangeimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Linkimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AIimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px]import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-windowimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondaryimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-managerimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondaryimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="textimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48pximport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClickimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-himport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</buttonimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <mainimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initialimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacityimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-centerimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-greenimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">Colorimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motionimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animateimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3,import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              classNameimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.divimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          <section className="mb-16">
            <div className="space-yimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          <section className="mb-16">
            <div className="space-y-5">
              {featureCards.map((card, index) => (
                <motionimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          <section className="mb-16">
            <div className="space-y-5">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y:import { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          <section className="mb-16">
            <div className="space-y-5">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transitionimport { motion } from 'framer-motion'
import { Camera, Sparkles, Layers, Palette, Zap, Download, X, Menu, Upload, Filter } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

export default function AppShowcase() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  const featureCards = [
    {
      id: 'ai-scene',
      icon: Sparkles,
      title: 'AI 场景识别',
      description: '智能识别拍摄场景，自动优化参数',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      points: ['智能场景分析', '实时参数调整', '哈苏色彩优化'],
      path: '/scene-detection'
    },
    {
      id: 'native-camera',
      icon: Camera,
      title: '原生相机参数',
      description: '自动填入最佳相机参数设置',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['自动曝光', '白平衡', '对焦优化'],
      path: '/native-camera'
    },
    {
      id: 'floating-window',
      icon: Layers,
      title: '悬浮窗',
      description: '便捷悬浮窗，实时预览滤镜效果',
      gradient: 'from-ocean-blue to-aurora-purple',
      points: ['实时预览', '便捷操作', '全局悬浮'],
      path: '/floating-window'
    },
    {
      id: 'preset-search',
      icon: Filter,
      title: '预设分类搜索',
      description: '快速找到所需的滤镜预设',
      gradient: 'from-oppo-green to-ocean-blue',
      points: ['智能搜索', '分类管理', '标签筛选'],
      path: '/filter-library'
    },
    {
      id: 'preset-ecosystem',
      icon: Zap,
      title: '预设生态',
      description: '丰富的预设社区，分享与下载',
      gradient: 'from-oppo-orange to-accent-secondary',
      points: ['预设分享', '社区交流', '云端同步'],
      path: '/preset-ecosystem'
    },
    {
      id: 'import-export',
      icon: Upload,
      title: '多格式导入导出',
      description: '支持多种格式的预设文件',
      gradient: 'from-error-vital to-sakura-pink',
      points: ['JSON 格式', 'LUT 支持', '批量操作'],
      path: '/lut-manager'
    }
  ]

  const toolCards = [
    {
      id: 'watermark',
      icon: Zap,
      title: '水印生成器',
      description: '专业水印制作工具',
      gradient: 'from-oppo-orange to-hasselblad-orange',
      path: '/watermark'
    },
    {
      id: 'preset-editor',
      icon: Palette,
      title: '预设编辑器',
      description: '自定义滤镜参数编辑',
      gradient: 'from-aurora-purple to-sakura-pink',
      path: '/preset-editor'
    }
  ]

  return (
    <div className="min-h-screen bg-deep-space text-text-primary overflow-x-hidden">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-96 h-96 top-1/4 -left-48 animate-float" />
        <div className="orb-oppo orb-2 w-80 h-80 top-1/2 -right-40 animate-float" style={{ animationDelay: '2s' }} />
        <div className="orb-oppo orb-3 w-72 h-72 bottom-1/4 left-1/3 animate-float" style={{ animationDelay: '4s' }} />
      </div>

      <nav className="fixed top-0 left-0 right-0 z-50 h-14 glass-navigation" role="navigation" aria-label="主导航">
        <div className="max-w-7xl mx-auto px-4 h-full">
          <div className="flex items-center justify-between h-full">
            <Link to="/" className="flex items-center space-x-3 touch-feedback" aria-label="返回首页">
              <div className="w-10 h-10 rounded-md bg-gradient-to-br from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-1">
                <Camera className="w-5 h-5 text-oppo-black" />
              </div>
              <span className="text-h2 font-bold gradient-text-oppo">OPPO Master</span>
            </Link>

            <div className="hidden md:flex items-center space-x-6">
              <Link to="/" className="text-body2 font-medium text-oppo-orange touch-feedback">首页</Link>
              <Link to="/filter-library" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">滤镜库</Link>
              <Link to="/master-params" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">大师参数</Link>
              <Link to="/settings" className="text-body2 font-medium text-text-secondary hover:text-text-primary transition-colors duration-200 ease-out-cubic touch-feedback">设置</Link>
            </div>

            <button className="hidden md:flex items-center space-x-2 btn-primary text-body2 touch-feedback" aria-label="下载应用">
              <Download className="w-4 h-4" />
              <span>立即下载</span>
            </button>

            <button
              className="md:hidden p-2 text-text-primary touch-feedback min-h-[48px] min-w-[48px] flex items-center justify-center rounded-sm hover:bg-white/10 transition-colors duration-200 ease-out-cubic"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-expanded={mobileMenuOpen}
              aria-label={mobileMenuOpen ? '关闭菜单' : '打开菜单'}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>

      {mobileMenuOpen && (
        <motion.div
          initial={{ opacity: 0, x: 300 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 300 }}
          transition={{ duration: 0.3, ease: 'ease-out-cubic' }}
          className="fixed inset-0 z-40 bg-deep-space/95 backdrop-blur-xl md:hidden pt-20"
          role="dialog"
          aria-label="移动端菜单"
        >
          <div className="flex flex-col items-center space-y-4 p-6">
            <Link to="/" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-oppo-orange min-h-[48px] flex items-center touch-feedback">首页</Link>
            <Link to="/scene-detection" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-primary min-h-[48px] flex items-center touch-feedback">AI 场景识别</Link>
            <Link to="/native-camera" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">原生相机参数</Link>
            <Link to="/floating-window" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">悬浮窗</Link>
            <Link to="/filter-library" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设分类搜索</Link>
            <Link to="/preset-ecosystem" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设生态</Link>
            <Link to="/lut-manager" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">多格式导入导出</Link>
            <Link to="/watermark" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">水印生成器</Link>
            <Link to="/preset-editor" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">预设编辑器</Link>
            <Link to="/settings" onClick={() => setMobileMenuOpen(false)} className="text-h3 font-medium text-text-secondary min-h-[48px] flex items-center touch-feedback">设置</Link>
            <button className="btn-primary-large w-full mt-4 touch-feedback" aria-label="下载应用">立即下载</button>
          </div>
        </motion.div>
      )}

      <main className="relative pt-20 pb-24 px-4 safe-area-bottom">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: 'ease-out-cubic' }}
            className="text-center mb-12"
          >
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.1, duration: 0.5, ease: 'ease-out-cubic' }}
              className="inline-flex items-center space-x-2 bg-gradient-to-r from-oppo-orange/20 to-hasselblad-orange/20 border border-oppo-orange/30 rounded-full px-3 py-1.5 mb-6 shadow-oppo-elevation-1"
            >
              <span className="w-1.5 h-1.5 bg-oppo-green rounded-full animate-pulse" />
              <span className="text-caption text-text-secondary">ColorOS 16 专业摄影增强</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-4xl sm:text-5xl font-bold leading-tight mb-4"
            >
              <span className="gradient-text-oppo">影像参数</span>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3, duration: 0.5, ease: 'ease-out-cubic' }}
              className="text-body1 md:text-lg text-text-secondary mb-8"
            >
              核心功能展示 - 点击卡片查看详情
            </motion.p>
          </motion.div>

          <section className="mb-16">
            <div className="space-y-5">
              {featureCards.map((card, index) => (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 + index * 0.1, duration: 0.