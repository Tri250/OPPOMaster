import { Sparkles, Palette, Zap } from 'lucide-react';
import { Link } from 'react-router-dom';

const features = [
  {
    id: 'ai',
    icon: Sparkles,
    title: 'AI场景识别',
    description: '智能识别拍摄场景，自动推荐最佳影像参数',
    action: '体验AI识别',
    actionLink: '/ai-demo'
  },
  {
    id: 'presets',
    icon: Palette,
    title: '大师预设库',
    description: '专业摄影师精心调校，哈苏认证影像质感',
    action: '探索预设',
    actionLink: '/filter-library'
  },
  {
    id: 'autofill',
    icon: Zap,
    title: '原生相机自动填参',
    description: '基于无障碍服务，无Root自动填入相机参数',
    action: null,
    actionLink: null
  }
];

export default function TechPage() {
  return (
    <div className="min-h-screen pt-20 pb-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <h1 className="text-4xl md:text-5xl font-bold mb-4 gradient-text">
            核心功能
          </h1>
          <p className="text-lg text-white/60 max-w-2xl mx-auto">
            专业级影像工具，轻松上手
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-16">
          {features.map((feature, index) => (
            <div key={feature.id} className="card p-6">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-oppo-orange to-hasselblad-orange p-4 mb-4">
                <feature.icon className="w-full h-full text-white" />
              </div>
              
              <h3 className="text-xl font-bold mb-2">{feature.title}</h3>
              <p className="text-white/60 text-sm mb-6">{feature.description}</p>
              
              {feature.actionLink && (
                <Link to={feature.actionLink} className="text-oppo-orange font-semibold">
                  {feature.action}
                </Link>
              )}
            </div>
          ))}
        </div>

        <div className="card p-8 text-center">
          <h2 className="text-2xl font-bold mb-6 gradient-text">
            开始使用
          </h2>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/filter-library" className="btn-primary px-8 py-3 inline-flex items-center justify-center gap-2">
              <Palette className="w-5 h-5" />
              探索预设库
            </Link>
            <Link to="/ai-demo" className="btn-secondary px-8 py-3 inline-flex items-center justify-center gap-2">
              <Sparkles className="w-5 h-5" />
              体验AI识别
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
