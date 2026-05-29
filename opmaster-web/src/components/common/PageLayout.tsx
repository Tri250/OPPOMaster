import { motion } from 'framer-motion';
import { ReactNode } from 'react';
import SidebarNav from './SidebarNav';
import TopNavBar from './TopNavBar';
import BottomTabNav from './BottomTabNav';

interface PageLayoutProps {
  children: ReactNode;
  className?: string;
}

export default function PageLayout({ children, className = '' }: PageLayoutProps) {
  return (
    <div className={`min-h-screen bg-bg-primary text-text-primary overflow-x-hidden ${className}`}>
      {/* 背景光效 */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div
          animate={{ x: [0, 80, 0], y: [0, 40, 0] }}
          transition={{ duration: 25, repeat: Infinity, ease: 'easeInOut' }}
          className="absolute -top-52 -left-52 w-[500px] h-[500px] orb-oppo orb-orange opacity-50"
        />
        <motion.div
          animate={{ x: [0, -60, 0], y: [0, -50, 0] }}
          transition={{ duration: 30, repeat: Infinity, ease: 'easeInOut' }}
          className="absolute -bottom-52 -right-52 w-[450px] h-[450px] orb-oppo orb-blue opacity-50"
        />
      </div>

      {/* 导航组件 */}
      <SidebarNav />
      <TopNavBar />

      {/* 主内容区域 */}
      <main className="relative pt-14 pb-24 lg:pb-12 lg:pl-64 z-10">
        {children}
      </main>

      {/* 底部导航 */}
      <BottomTabNav />
    </div>
  );
}
