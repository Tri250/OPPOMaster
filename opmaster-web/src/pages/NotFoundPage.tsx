import { motion } from 'framer-motion';
import { Link, useNavigate } from 'react-router-dom';
import { Search, Home, AlertCircle } from 'lucide-react';
import { useState } from 'react';

export default function NotFoundPage() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  return (
    <div className="min-h-screen bg-[#0F0F0F] flex items-center justify-center px-page">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-center max-w-2xl mx-auto"
      >
        {/* 错误图标 */}
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
          className="mb-8"
        >
          <div className="inline-flex items-center justify-center w-24 h-24 bg-[#1A1A1A] rounded-full">
            <AlertCircle className="w-12 h-12 text-[#FF6B35]" />
          </div>
        </motion.div>

        {/* 错误标题 */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="text-4xl md:text-5xl font-bold text-[#FFFFFF] mb-4"
        >
          页面未找到
        </motion.h1>

        {/* 错误描述 */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="text-lg text-[#CCCCCC] mb-8"
        >
          抱歉，您访问的页面不存在或已被移除
        </motion.p>

        {/* 搜索框 */}
        <motion.form
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          onSubmit={handleSearch}
          className="mb-8"
        >
          <div className="relative max-w-md mx-auto">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[#999999]" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="搜索您需要的预设..."
              className="w-full pl-12 pr-4 py-3 bg-[#1A1A1A] text-[#FFFFFF] rounded-[12px] border border-white/10 focus:border-[#FF6B35] focus:outline-none transition-colors"
            />
          </div>
        </motion.form>

        {/* 返回首页按钮 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <Link
            to="/"
            className="inline-flex items-center gap-2 px-8 py-3 bg-[#FF6B35] text-[#0F0F0F] font-semibold rounded-[12px] hover:bg-[#FF6B35]/90 transition-colors"
          >
            <Home className="w-5 h-5" />
            返回首页
          </Link>
        </motion.div>

        {/* 建议 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
          className="mt-12 text-left"
        >
          <h3 className="text-[#D4A574] font-semibold mb-4">您可以尝试：</h3>
          <ul className="space-y-2 text-[#999999]">
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#FF6B35] mt-2 flex-shrink-0" />
              <span>检查URL是否正确</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#FF6B35] mt-2 flex-shrink-0" />
              <span>使用搜索功能查找内容</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#FF6B35] mt-2 flex-shrink-0" />
              <span>浏览首页了解功能</span>
            </li>
          </ul>
        </motion.div>
      </motion.div>
    </div>
  );
}
