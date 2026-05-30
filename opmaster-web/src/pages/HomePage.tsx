import HeroSection from '../components/home/HeroSection';
import FeaturesSection from '../components/home/FeaturesSection';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      {/* 英雄区 - 导航栏固定在顶部，高度64dp */}
      <HeroSection />
      
      {/* 页面整体结构：导航栏 - 影像参数区 - 影像工具区 */}
      {/* 影像参数区占页面约70%高度，包含6个功能卡片 */}
      {/* 影像工具区占页面约30%高度，包含2个工具卡片 */}
      {/* 区域之间使用"哈苏橙标题 + 分隔线"方式分隔，分隔线高度1dp，颜色#333333 */}
      <FeaturesSection />
    </div>
  );
}
