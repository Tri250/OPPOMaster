import { Smartphone, Camera, Zap, Shield, Palette, Share2, Image, Layers, Sparkles, Settings, Download, Wifi, Battery, Cpu, Globe, Lock } from 'lucide-react';
import { motion } from 'framer-motion';

const androidFeatures = [
  {
    category: '相机核心',
    icon: <Camera className="w-6 h-6" />,
    features: [
      '哈苏色彩模式',
      '专业模式参数调节',
      '悬浮窗实时显示',
      'AI场景识别24种',
      '智能参数推荐',
      '长曝光模式'
    ]
  },
  {
    category: '影像处理',
    icon: <Image className="w-6 h-6" />,
    features: [
      '专业水印编辑',
      '文字/图片水印',
      '8套专业模板',
      '批量处理图片',
      '撤销重做功能',
      '无损原图保存'
    ]
  },
  {
    category: '预设系统',
    icon: <Layers className="w-6 h-6" />,
    features: [
      'HNCS认证预设',
      '分类智能筛选',
      '一键收藏预设',
      '预设参数详解',
      '自定义预设',
      '云端同步'
    ]
  },
  {
    category: 'AI智能',
    icon: <Sparkles className="w-6 h-6" />,
    features: [
      '场景自动识别',
      '参数OCR识别',
      'AI样张微调',
      '批量预设应用',
      '智能色彩优化',
      '本地处理无网络'
    ]
  },
  {
    category: '社交分享',
    icon: <Share2 className="w-6 h-6" />,
    features: [
      '微信好友/朋友圈',
      'QQ/QQ空间',
      '微博',
      '抖音',
      '小红书',
      '原图分享'
    ]
  },
  {
    category: '系统优化',
    icon: <Zap className="w-6 h-6" />,
    features: [
      'ColorOS 16适配',
      '60fps流畅动画',
      '内存实时监控',
      '快速冷启动',
      '弱网环境适配',
      '图片智能缓存'
    ]
  },
  {
    category: '安全隐私',
    icon: <Shield className="w-6 h-6" />,
    features: [
      '数据端侧加密',
      'EncryptedSharedPreferences',
      '权限细粒度管理',
      '本地AI处理',
      '隐私设置中心',
      '安全数据存储'
    ]
  },
  {
    category: '个性化',
    icon: <Palette className="w-6 h-6" />,
    features: [
      '深色主题模式',
      '浅色主题模式',
      '跟随系统主题',
      '护眼暖色调模式',
      'Material You设计',
      'ColorOS风格'
    ]
  }
];

const specialFeatures = [
  {
    icon: <Smartphone className="w-8 h-8 text-hasselblad" />,
    title: 'OPPO设备专属优化',
    description: '深度适配OPPO Find系列、Reno系列，充分发挥哈苏影像系统能力',
    highlight: true
  },
  {
    icon: <Cpu className="w-8 h-8 text-purple-400" />,
    title: 'NPU芯片加速',
    description: '利用马里亚纳芯片加速AI处理，提供极速的场景识别体验',
    highlight: false
  },
  {
    icon: <Download className="w-8 h-8 text-blue-400" />,
    title: '离线可用',
    description: '所有核心功能无需网络，AI处理完全在本地端侧完成',
    highlight: false
  },
  {
    icon: <Battery className="w-8 h-8 text-green-400" />,
    title: '低功耗设计',
    description: '智能后台管理，极致优化电量消耗，持久续航',
    highlight: false
  }
];

const permissions = [
  { name: '相机权限', desc: '用于拍摄和实时取景' },
  { name: '存储权限', desc: '用于保存和读取照片' },
  { name: '悬浮窗权限', desc: '用于显示相机参数悬浮窗' },
  { name: '网络权限', desc: '用于预设更新和分享' },
  { name: '麦克风权限', desc: '用于拍摄视频录音' }
];

export default function AndroidFeatures() {
  return (
    <section className="py-20">
      <div className="max-w-6xl mx-auto px-6">
        {/* Android特色功能 */}
        <div className="text-center mb-12">
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl md:text-4xl font-bold mb-4"
          >
            Android端<span className="gradient-text"> 特色功能</span>
          </motion.h2>
          <p className="text-white/60 text-lg max-w-2xl mx-auto">
            专为OPPO/一加设备打造，深度集成ColorOS系统能力
          </p>
        </div>

        {/* 专属特性 */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4 mb-16">
          {specialFeatures.map((feature, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              whileHover={{ y: -4, scale: 1.02 }}
              className={`card p-6 ${feature.highlight ? 'border-hasselblad/30' : ''}`}
            >
              <div className="mb-4">{feature.icon}</div>
              <h3 className="font-semibold mb-2">{feature.title}</h3>
              <p className="text-white/60 text-sm">{feature.description}</p>
            </motion.div>
          ))}
        </div>

        {/* 功能矩阵 */}
        <div className="mb-16">
          <h3 className="text-xl font-bold mb-6 text-center">完整功能矩阵</h3>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
            {androidFeatures.map((group, groupIndex) => (
              <motion.div
                key={groupIndex}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: groupIndex * 0.1 }}
                className="card p-6"
              >
                <div className="flex items-center gap-3 mb-4">
                  <span className="text-hasselblad">{group.icon}</span>
                  <h4 className="font-semibold">{group.category}</h4>
                </div>
                <div className="space-y-2">
                  {group.features.map((feature, featureIndex) => (
                    <div key={featureIndex} className="flex items-center gap-2 text-sm">
                      <span className="text-oppo-green">✓</span>
                      <span className="text-white/80">{feature}</span>
                    </div>
                  ))}
                </div>
              </motion.div>
            ))}
          </div>
        </div>

        {/* 权限说明 */}
        <div className="card p-6">
          <div className="flex items-center gap-2 mb-6">
            <Lock className="w-5 h-5 text-hasselblad" />
            <h3 className="text-xl font-bold">权限说明</h3>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
            {permissions.map((perm, index) => (
              <div key={index} className="bg-white/5 rounded-xl p-4">
                <div className="font-medium mb-1">{perm.name}</div>
                <div className="text-white/60 text-sm">{perm.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
