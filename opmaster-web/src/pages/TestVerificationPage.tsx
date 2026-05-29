import { motion } from 'framer-motion'
import { 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Camera, 
  Search, 
  Zap, 
  Layers, 
  Upload, 
  Palette, 
  Shield, 
  Smartphone,
  Sparkles
} from 'lucide-react'
import { useState } from 'react'

interface TestCategory {
  id: string
  name: string
  icon: any
  tests: TestItem[]
}

interface TestItem {
  id: string
  name: string
  steps: string[]
  expected: string
  acceptance: string
  status: 'pass' | 'fail' | 'pending'
}

const testCategories: TestCategory[] = [
  {
    id: 'scene-recognition',
    name: '场景识别准确性',
    icon: Camera,
    tests: [
      {
        id: 'scene-accuracy',
        name: '场景识别准确性',
        steps: ['打开应用', '进入AI场景识别功能', '拍摄不同场景照片', '验证识别结果'],
        expected: '正确识别场景类型，推荐相应参数',
        acceptance: '识别准确率≥98%，响应时间≤500ms',
        status: 'pending'
      },
      {
        id: 'multi-scene',
        name: '多场景支持',
        steps: ['拍摄50+不同场景照片', '验证所有场景识别'],
        expected: '支持所有advertised场景类型',
        acceptance: '支持50+场景类型，无遗漏',
        status: 'pending'
      },
      {
        id: 'param-recommendation',
        name: '参数推荐',
        steps: ['识别场景后查看推荐参数', '验证参数合理性'],
        expected: '推荐参数符合场景需求',
        acceptance: '参数推荐符合摄影最佳实践',
        status: 'pending'
      }
    ]
  },
  {
    id: 'special-scenes',
    name: '特殊场景测试',
    icon: Sparkles,
    tests: [
      {
        id: 'low-light',
        name: '低光照场景',
        steps: ['在极暗环境下拍摄', '验证识别结果'],
        expected: '正确识别并推荐夜景参数',
        acceptance: '识别准确率≥95%',
        status: 'pending'
      },
      {
        id: 'fast-motion',
        name: '快速移动场景',
        steps: ['拍摄快速移动物体', '验证识别结果'],
        expected: '正确识别运动场景',
        acceptance: '识别准确率≥90%',
        status: 'pending'
      },
      {
        id: 'blur-scene',
        name: '模糊场景',
        steps: ['拍摄模糊照片', '验证识别结果'],
        expected: '正确识别并提示重拍',
        acceptance: '提供清晰度建议',
        status: 'pending'
      }
    ]
  },
  {
    id: 'auto-fill',
    name: '参数自动填入',
    icon: Smartphone,
    tests: [
      {
        id: 'param-fill',
        name: '参数自动填入',
        steps: ['连接原生相机', '识别场景后自动填入参数', '验证参数应用'],
        expected: '参数正确填入相机设置',
        acceptance: '参数填入准确率100%',
        status: 'pending'
      },
      {
        id: 'multi-brand',
        name: '多品牌支持',
        steps: ['在不同品牌相机上测试', '验证参数填入'],
        expected: '支持六大品牌相机',
        acceptance: '支持所有advertised品牌',
        status: 'pending'
      },
      {
        id: 'no-root',
        name: '无Root权限',
        steps: ['在未Root设备上测试', '验证功能可用性'],
        expected: '功能正常工作',
        acceptance: '无需Root权限',
        status: 'pending'
      }
    ]
  },
  {
    id: 'floating-window',
    name: '悬浮窗功能',
    icon: Layers,
    tests: [
      {
        id: 'floating-display',
        name: '悬浮窗显示',
        steps: ['启用悬浮窗功能', '验证显示位置和样式'],
        expected: '正确显示在相机上层',
        acceptance: '适配率≥98%',
        status: 'pending'
      },
      {
        id: 'multi-type',
        name: '多类型支持',
        steps: ['切换不同悬浮窗类型', '验证显示效果'],
        expected: '支持所有advertised类型',
        acceptance: '支持标准悬浮窗类型',
        status: 'pending'
      },
      {
        id: 'interaction',
        name: '交互功能',
        steps: ['点击悬浮窗按钮', '验证功能响应'],
        expected: '正确响应交互操作',
        acceptance: '交互响应时间≤200ms',
        status: 'pending'
      }
    ]
  },
  {
    id: 'search-filter',
    name: '分类搜索',
    icon: Search,
    tests: [
      {
        id: 'category-search',
        name: '分类搜索',
        steps: ['使用按风格分类搜索', '验证搜索结果'],
        expected: '返回正确分类结果',
        acceptance: '搜索准确率100%',
        status: 'pending'
      },
      {
        id: 'scene-category',
        name: '场景分类',
        steps: ['使用按场景分类搜索', '验证搜索结果'],
        expected: '返回正确场景结果',
        acceptance: '搜索准确率100%',
        status: 'pending'
      },
      {
        id: 'full-text',
        name: '全文搜索',
        steps: ['使用关键词全文搜索', '验证搜索结果'],
        expected: '返回相关结果',
        acceptance: '搜索准确率≥95%',
        status: 'pending'
      }
    ]
  },
  {
    id: 'preset-ecosystem',
    name: '预设生态',
    icon: Palette,
    tests: [
      {
        id: 'preset-editor',
        name: '预设编辑器',
        steps: ['使用预设编辑器创建预设', '验证编辑功能'],
        expected: '编辑功能正常',
        acceptance: '所有编辑功能可用',
        status: 'pending'
      },
      {
        id: 'one-click-contribute',
        name: '一键贡献',
        steps: ['提交预设到社区', '验证提交流程'],
        expected: '提交流程顺畅',
        acceptance: '提交成功率100%',
        status: 'pending'
      },
      {
        id: 'leaderboard',
        name: '排行榜功能',
        steps: ['查看预设排行榜', '验证排名逻辑'],
        expected: '排名逻辑正确',
        acceptance: '排名算法准确',
        status: 'pending'
      }
    ]
  },
  {
    id: 'import-export',
    name: '多格式导入导出',
    icon: Upload,
    tests: [
      {
        id: 'lut-parser',
        name: 'LUT文件解析',
        steps: ['导入LUT文件', '验证解析结果'],
        expected: '正确解析LUT文件',
        acceptance: '解析准确率100%',
        status: 'pending'
      },
      {
        id: 'polarr-preset',
        name: '泼辣修图预设',
        steps: ['导入泼辣修图预设', '验证兼容性'],
        expected: '兼容泼辣修图格式',
        acceptance: '兼容性≥98%',
        status: 'pending'
      },
      {
        id: 'lightroom-preset',
        name: 'Lightroom预设',
        steps: ['导入Lightroom预设', '验证兼容性'],
        expected: '兼容Lightroom格式',
        acceptance: '兼容性≥98%',
        status: 'pending'
      }
    ]
  },
  {
    id: 'performance',
    name: '性能测试',
    icon: Zap,
    tests: [
      {
        id: '24h-run',
        name: '24小时连续运行',
        steps: ['应用持续运行24小时', '监控内存、CPU使用'],
        expected: '资源使用稳定，无崩溃',
        acceptance: '内存泄漏≤5%，CPU使用≤80%',
        status: 'pending'
      },
      {
        id: 'high-frequency',
        name: '高频操作测试',
        steps: ['持续进行高频操作', '监控系统状态'],
        expected: '系统稳定不崩溃',
        acceptance: '操作成功率≥99.9%',
        status: 'pending'
      }
    ]
  },
  {
    id: 'security',
    name: '安全性测试',
    icon: Shield,
    tests: [
      {
        id: 'data-encryption',
        name: '数据加密',
        steps: ['验证敏感数据加密', '数据加密有效'],
        expected: '加密算法符合标准',
        acceptance: '加密算法符合标准',
        status: 'pending'
      },
      {
        id: 'permission-mgmt',
        name: '权限管理',
        steps: ['验证权限申请和使用', '权限管理正确'],
        expected: '权限使用符合规范',
        acceptance: '权限使用符合规范',
        status: 'pending'
      },
      {
        id: 'vulnerability-scan',
        name: '漏洞扫描',
        steps: ['进行安全漏洞扫描', '无高危漏洞'],
        expected: '漏洞等级：无高危',
        acceptance: '漏洞等级：无高危',
        status: 'pending'
      }
    ]
  }
]

const StatusIcon = ({ status }: { status: string }) => {
  if (status === 'pass') {
    return <CheckCircle2 className="w-6 h-6 text-green-500" />
  } else if (status === 'fail') {
    return <AlertCircle className="w-6 h-6 text-red-500" />
  }
  return <Clock className="w-6 h-6 text-yellow-500" />
}

export default function TestVerificationPage() {
  const [selectedCategory, setSelectedCategory] = useState<string>('scene-recognition')
  const [testStatuses, setTestStatuses] = useState<Record<string, 'pass' | 'fail' | 'pending'>>({})

  const toggleTestStatus = (testId: string) => {
    setTestStatuses(prev => {
      const current = prev[testId] || 'pending'
      let next: 'pass' | 'fail' | 'pending'
      if (current === 'pending') next = 'pass'
      else if (current === 'pass') next = 'fail'
      else next = 'pending'
      return { ...prev, [testId]: next }
    })
  }

  const selectedCategoryData = testCategories.find(c => c.id === selectedCategory) || testCategories[0]

  const passCount = Object.values(testStatuses).filter(s => s === 'pass').length
  const totalCount = testCategories.reduce((sum, cat) => sum + cat.tests.length, 0)

  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text-oppo">
            测试验证中心
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            小O帮帮 功能测试验证 - 全面检查各项功能是否符合验收标准
          </p>
        </motion.div>

        {/* Summary */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="mb-8"
        >
          <div className="card p-6">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="text-center">
                <div className="text-3xl font-bold text-white">{totalCount}</div>
                <div className="text-white/60 text-sm">总测试项</div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-green-500">{passCount}</div>
                <div className="text-white/60 text-sm">通过</div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-yellow-500">{totalCount - passCount - Object.values(testStatuses).filter(s => s === 'fail').length}</div>
                <div className="text-white/60 text-sm">待测试</div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-oppo-orange">
                  {totalCount > 0 ? Math.round((passCount / totalCount) * 100) : 0}%
                </div>
                <div className="text-white/60 text-sm">完成率</div>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Category Tabs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="mb-8"
        >
          <div className="flex flex-wrap gap-2 justify-center">
            {testCategories.map(category => {
              const Icon = category.icon
              return (
                <button
                  key={category.id}
                  onClick={() => setSelectedCategory(category.id)}
                  className={`px-4 py-2 rounded-full flex items-center gap-2 transition-all ${
                    selectedCategory === category.id
                      ? 'bg-oppo-orange text-oppo-black font-medium'
                      : 'bg-white/10 text-white hover:bg-white/20'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{category.name}</span>
                </button>
              )
            })}
          </div>
        </motion.div>

        {/* Tests */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className="card p-6">
            <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
              <selectedCategoryData.icon className="w-6 h-6 text-oppo-orange" />
              {selectedCategoryData.name}
            </h2>
            <div className="space-y-4">
              {selectedCategoryData.tests.map(test => {
                const currentStatus = testStatuses[test.id] || test.status
                return (
                  <motion.div
                    key={test.id}
                    className="bg-white/5 rounded-xl p-4 border border-white/10"
                    whileHover={{ scale: 1.01 }}
                    onClick={() => toggleTestStatus(test.id)}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <StatusIcon status={currentStatus} />
                          <h3 className="text-lg font-medium text-white">{test.name}</h3>
                        </div>
                        
                        <div className="space-y-3 ml-9">
                          <div>
                            <h4 className="text-sm font-medium text-white/80 mb-1">测试步骤：</h4>
                            <ol className="list-decimal list-inside text-sm text-white/60 space-y-1">
                              {test.steps.map((step, idx) => (
                                <li key={idx}>{step}</li>
                              ))}
                            </ol>
                          </div>
                          
                          <div>
                            <h4 className="text-sm font-medium text-white/80 mb-1">预期结果：</h4>
                            <p className="text-sm text-green-400">{test.expected}</p>
                          </div>
                          
                          <div>
                            <h4 className="text-sm font-medium text-white/80 mb-1">验收标准：</h4>
                            <p className="text-sm text-hasselblad-orange">{test.acceptance}</p>
                          </div>
                        </div>
                      </div>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          toggleTestStatus(test.id)
                        }}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                          currentStatus === 'pass'
                            ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                            : currentStatus === 'fail'
                            ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                            : 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30'
                        }`}
                      >
                        {currentStatus === 'pass' ? '通过' : currentStatus === 'fail' ? '失败' : '待测试'}
                      </button>
                    </div>
                  </motion.div>
                )
              })}
            </div>
          </div>
        </motion.div>

        {/* Acceptance Criteria */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-8"
        >
          <div className="card p-6">
            <h2 className="text-2xl font-bold mb-6 gradient-text-oppo">验收标准</h2>
            
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">功能性</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 所有功能模块通过率≥99.5%</li>
                  <li>• 边界条件和异常情况处理完善</li>
                  <li>• 用户操作流程完整无中断</li>
                </ul>
              </div>
              
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">稳定性</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 长时间运行无崩溃</li>
                  <li>• 压力测试下系统稳定</li>
                  <li>• 异常情况恢复机制有效</li>
                </ul>
              </div>
              
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">性能</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 启动时间≤3s</li>
                  <li>• 功能响应时间≤2s</li>
                  <li>• 内存使用≤500MB</li>
                </ul>
              </div>
              
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">兼容性</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 支持所有advertised设备</li>
                  <li>• 兼容性测试通过率≥98%</li>
                  <li>• 不同环境下功能一致性</li>
                </ul>
              </div>
              
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">安全性</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 无高危安全漏洞</li>
                  <li>• 数据加密有效</li>
                  <li>• 权限管理规范</li>
                </ul>
              </div>
              
              <div className="bg-white/5 rounded-xl p-4">
                <h3 className="text-lg font-medium text-white mb-3">用户体验</h3>
                <ul className="space-y-2 text-sm text-white/60">
                  <li>• 界面响应迅速</li>
                  <li>• 操作流程直观</li>
                  <li>• 错误提示清晰</li>
                </ul>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  )
}