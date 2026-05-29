import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  Smartphone,
  CheckCircle,
  Zap,
  Shield,
  Bot,
  ChevronLeft,
  Menu,
  Smartphone as SmartphoneIcon,
  Android,
  Camera as CameraIcon,
  Settings,
  Database
} from 'lucide-react';
import { ColorOSAnimations } from '../components/common/ColorOSComponents';

export default function NativeCameraPage() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const features = [
    {
      id: 1,
      title: '支持六大品牌相机',
      description: 'OPPO、一加、真我、小米、vivo、华为',
      icon: Smartphone,
      color: 'from-blue-500 to-indigo-500'
    },
    {
      id: 2,
      title: '无Root合法合规',
      description: '基于安卓无障碍服务，安全可靠',
      icon: Shield,
      color: 'from-emerald-500 to-teal-500'
    },
    {
      id: 3,
      title: '10+步简化为2步',
      description: '复杂操作全自动',
      icon: Zap,
      color: 'from-orange-500 to-red-500'
    },
    {
      id: 4,
      title: 'AI智能推荐',
      description: '根据场景自动匹配参数',
      icon: Bot,
      color: 'from-purple-500 to-pink-500'
    }
  ];

  const steps = [
    {
      step: 1,
      title: '开启无障碍服务',
      description: '在系统设置中找到"小O帮帮"服务',
      icon: Settings,
      color: 'from-blue-400 to-blue-600'
    },
    {
      step: 2,
      title: '选择相机参数',
      description: '在预设库中选择需要的参数配置',
      icon: Database,
      color: 'from-emerald-400 to-emerald-600'
    },
    {
      step: 3,
      title: '打开相机应用',
      description: '自动填入已选择的参数自动填入',
      icon: CameraIcon,
      color: 'from-orange-400 to-orange-600'
    },
    {
      step: 4,
      title: '开始拍摄',
      description: '享受专业相机参数带来的完美效果',
      icon: CheckCircle,
      color: 'from-purple-400 to-purple-600'
    }
  ];

  return (
    <div className="min-h-screen bg-deep-space">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="orb-oppo orb-1 w-72 h-72 top-1/3 -left-36 animate-float opacity-20" />
        <div className="orb-oppo orb-3 w-56 h-56 bottom-1/4 right-0 animate-float opacity-20" style={{ animationDelay: '3s' }} />
      </div>

      {/* 导航栏 */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-deep-space/80 backdrop-blur-xl border-b border-white/5">
        <div className="max-w-4xl mx-auto px-4 sm:px-6">
          <div className="flex items-center justify-between h-16">
            <Link to="/xiao-o-help" className="flex items-center space-x-3">
              <button className="p-2 rounded-xl hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center">
                <ChevronLeft className="w-6 h-6 text-white" />
              </button>
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-oppo-sunrise-gold to-hasselblad-pro flex items-center justify-center">
                <SmartphoneIcon className="w-5 h-5 text-deep-space" />
              </div>
              <span className="text-lg font-bold text-white">原生相机</span>
            </Link>
            <button 
              className="p-2 rounded-xl hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              <Menu className="w-6 h-6 text-white" />
            </button>
          </div>
        </div>
      </nav>

      {/* 主内容 */}
      <div className="relative pt-24 pb-12 px-4 sm:px-6">
        <div className="max-w-4xl mx-auto">
          <motion.div
            initial="initial"
            animate="animate"
            variants={ColorOSAnimations.fadeIn}
            className="space-y-6"
          >
            {/* Hero区 */}
            <motion.div variants={ColorOSAnimations.fadeIn} className="text-center mb-10">
              <div className="w-24 h-24 md:w-28 md:h-28 mx-auto mb-6 rounded-3xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center">
                <Smartphone className="w-12 h-12 md:w-14 md:h-14 text-white" />
              </div>
              <h1 className="text-3xl md:text-4xl font-bold text-white mb-3">
                原生相机参数自动填入
              </h1>
              <p className="text-text-secondary text-sm md:text-base">
                基于安卓无障碍服务，无Root自动填参数
              </p>
            </motion.div>

            {/* 功能特点网格 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
              {features.map((feature, index) => (
                <motion.div
                  key={feature.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                >
                  <div className="card-oppo p-6">
                    <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4">
                      <feature.icon className="w-7 h-7 text-white" />
                    </div>
                    <h3 className="text-lg font-bold text-white mb-2">{feature.title}</h3>
                    <p className="text-text-secondary text-sm">{feature.description}</p>
                  </div>
                </motion.div>
              ))}
            </div>

            {/* 使用步骤 */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="mt-10"
            >
              <div className="text-center mb-8">
                <h2 className="text-2xl font-bold text-white mb-2">使用步骤</h2>
                <p className="text-text-secondary text-sm">简单四步，轻松上手</p>
              </div>

              <div className="space-y-4">
                {steps.map((step, index) => (
                  <motion.div
                    key={step.step}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.5 + index * 0.1 }}
                    className="card-oppo p-5 md:p-6 flex items-start gap-5"
                  >
                    <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${step.color} flex items-center justify-center flex-shrink-0`}>
                      <step.icon className="w-6 h-6 text-white" />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-1">
                        <span className="text-oppo-sunrise-gold font-bold text-xl">步骤 {step.step}</span>
                        <h3 className="text-lg font-bold text-white">{step.title}</h3>
                      </div>
                      <p className="text-text-secondary text-sm md:text-base">{step.description}</p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            {/* 提示说明 */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.9 }}
              className="mt-10"
            >
              <div className="card-oppo p-6 border-l-4 border-oppo-sunrise-gold/50 bg-gradient-to-r from-oppo-sunrise-gold/10 to-transparent">
                <div className="flex items-start gap-4">
                  <Shield className="w-10 h-10 text-oppo-sunrise-gold flex-shrink-0" />
                  <div>
                    <h3 className="text-lg font-bold text-white mb-1">安全说明</h3>
                    <p className="text-text-secondary text-sm md:text-base">
                      所有数据完全在本地处理，不会发送任何信息到服务器，保护您的隐私安全
                    </p>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* 底部按钮
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.0 }}
              className="mt-10 flex justify-center"
            >
              <Link to="/">
                <button className="btn-primary text-lg px-10 py-4">
                开始使用
              </button>
              </Link>
            </motion.div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
