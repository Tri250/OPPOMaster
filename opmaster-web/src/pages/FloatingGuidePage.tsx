import { motion } from 'framer-motion';
import { 
  Layers, 
  MousePointer, 
  Move, 
  Copy as CopyIcon, 
  ChevronRight,
  Smartphone,
  Settings
} from 'lucide-react';

export default function FloatingGuidePage() {
  return (
    <div className="min-h-screen bg-[#0F0F0F]">
      {/* Header */}
      <div className="pt-24 pb-12 px-4 text-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="inline-flex items-center justify-center w-20 h-20 bg-[#FF6B35]/15 rounded-[20px] mb-6"
        >
          <Layers className="w-10 h-10 text-[#FF6B35]" />
        </motion.div>
        <h1 className="text-4xl md:text-5xl font-bold text-[#FFFFFF] mb-4">
          悬浮窗功能指南
        </h1>
        <p className="text-lg text-[#CCCCCC] max-w-2xl mx-auto">
          了解如何使用悬浮窗在相机上层显示参数信息，支持六大品牌相机
        </p>
      </div>

      {/* Supported Brands */}
      <section className="py-12 px-4">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-2xl font-bold text-[#D4A574] mb-8 text-center">
            支持的品牌
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {[
              { name: 'OPPO', icon: '📱', color: '#FF6B35' },
              { name: '一加', icon: '📱', color: '#00C853' },
              { name: 'Realme', icon: '📱', color: '#FFC107' },
              { name: '小米', icon: '📱', color: '#FF9800' },
              { name: '华为', icon: '📱', color: '#2196F3' },
              { name: 'vivo', icon: '📱', color: '#9C27B0' },
            ].map((brand, idx) => (
              <motion.div
                key={brand.name}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: idx * 0.1 }}
                className="bg-[#1A1A1A] rounded-[16px] p-6 text-center hover:bg-[#1A1A1A]/80 transition-colors"
              >
                <div 
                  className="w-12 h-12 rounded-[12px] mx-auto mb-3 flex items-center justify-center text-2xl"
                  style={{ backgroundColor: `${brand.color}20` }}
                >
                  {brand.icon}
                </div>
                <p className="font-semibold text-[#FFFFFF]">{brand.name}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Usage Guide */}
      <section className="py-12 px-4">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-2xl font-bold text-[#D4A574] mb-8 text-center">
            使用指南
          </h2>
          <div className="grid md:grid-cols-2 gap-6">
            {[
              {
                icon: <MousePointer className="w-8 h-8" />,
                title: '点击展开',
                description: '点击悬浮球展开悬浮窗，查看完整参数信息',
                color: '#FF6B35',
                gradient: 'from-[#FF6B35] to-[#FF8F6B]'
              },
              {
                icon: <ChevronRight className="w-8 h-8" />,
                title: '双击关闭',
                description: '快速双击悬浮球可立即关闭悬浮窗',
                color: '#00C853',
                gradient: 'from-[#00C853] to-[#33D976]'
              },
              {
                icon: <Move className="w-8 h-8" />,
                title: '拖动移动',
                description: '长按并拖动悬浮球可自由移动位置',
                color: '#2962FF',
                gradient: 'from-[#2962FF] to-[#64B5F6]'
              },
              {
                icon: <CopyIcon className="w-8 h-8" />,
                title: '一键复制',
                description: '点击按钮快速复制所有参数到剪贴板',
                color: '#FF9800',
                gradient: 'from-[#FF9800] to-[#FFB74D]'
              },
            ].map((item, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, x: idx % 2 === 0 ? -20 : 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.15 }}
                className="bg-[#1A1A1A] rounded-[16px] p-6 flex gap-4 hover:bg-[#1A1A1A]/80 transition-colors"
              >
                <div 
                  className={`w-16 h-16 rounded-[16px] flex items-center justify-center bg-gradient-to-br ${item.gradient} flex-shrink-0`}
                >
                  <div className="text-white">
                    {item.icon}
                  </div>
                </div>
                <div>
                  <h3 className="text-xl font-bold text-[#FFFFFF] mb-2">
                    {item.title}
                  </h3>
                  <p className="text-sm text-[#CCCCCC]">
                    {item.description}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Permission Guide */}
      <section className="py-12 px-4">
        <div className="max-w-6xl mx-auto">
          <h2 className="text-2xl font-bold text-[#D4A574] mb-8 text-center">
            权限申请
          </h2>
          <div className="grid md:grid-cols-2 gap-6">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-[#1A1A1A] rounded-[16px] p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 bg-[#FF6B35]/15 rounded-[12px] flex items-center justify-center">
                  <Smartphone className="w-6 h-6 text-[#FF6B35]" />
                </div>
                <h3 className="text-xl font-bold text-[#FFFFFF]">
                  首次使用
                </h3>
              </div>
              <ol className="space-y-3 text-sm text-[#CCCCCC]">
                <li className="flex gap-3">
                  <span className="w-6 h-6 bg-[#FF6B35] text-[#0F0F0F] rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">1</span>
                  <span>点击悬浮窗功能入口</span>
                </li>
                <li className="flex gap-3">
                  <span className="w-6 h-6 bg-[#FF6B35] text-[#0F0F0F] rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">2</span>
                  <span>系统会提示授权悬浮窗权限</span>
                </li>
                <li className="flex gap-3">
                  <span className="w-6 h-6 bg-[#FF6B35] text-[#0F0F0F] rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">3</span>
                  <span>点击"去授权"按钮打开设置</span>
                </li>
                <li className="flex gap-3">
                  <span className="w-6 h-6 bg-[#FF6B35] text-[#0F0F0F] rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">4</span>
                  <span>开启"悬浮窗"权限开关</span>
                </li>
                <li className="flex gap-3">
                  <span className="w-6 h-6 bg-[#FF6B35] text-[#0F0F0F] rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0">5</span>
                  <span>返回应用，悬浮窗即可正常使用</span>
                </li>
              </ol>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15 }}
              className="bg-[#1A1A1A] rounded-[16px] p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 bg-[#00C853]/15 rounded-[12px] flex items-center justify-center">
                  <Settings className="w-6 h-6 text-[#00C853]" />
                </div>
                <h3 className="text-xl font-bold text-[#FFFFFF]">
                  安全说明
                </h3>
              </div>
              <ul className="space-y-3 text-sm text-[#CCCCCC]">
                <li className="flex items-start gap-3">
                  <span className="w-1.5 h-1.5 bg-[#00C853] rounded-full mt-2 flex-shrink-0" />
                  <span>悬浮窗权限仅用于在相机上层显示信息，不会读取任何隐私数据</span>
                </li>
                <li className="flex items-start gap-3">
                  <span className="w-1.5 h-1.5 bg-[#00C853] rounded-full mt-2 flex-shrink-0" />
                  <span>您可以随时在系统设置中关闭悬浮窗权限</span>
                </li>
                <li className="flex items-start gap-3">
                  <span className="w-1.5 h-1.5 bg-[#00C853] rounded-full mt-2 flex-shrink-0" />
                  <span>应用不会在后台收集或上传任何数据</span>
                </li>
                <li className="flex items-start gap-3">
                  <span className="w-1.5 h-1.5 bg-[#00C853] rounded-full mt-2 flex-shrink-0" />
                  <span>悬浮窗不会影响相机预览画面质量</span>
                </li>
                <li className="flex items-start gap-3">
                  <span className="w-1.5 h-1.5 bg-[#00C853] rounded-full mt-2 flex-shrink-0" />
                  <span>支持六大品牌相机，适配率超过95%</span>
                </li>
              </ul>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <div className="py-12 px-4 text-center">
        <p className="text-[#999999] text-sm">
          如遇问题，请查看帮助文档或联系技术支持
        </p>
      </div>
    </div>
  );
}
