import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

interface ToolCardProps {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  isPrimary?: boolean;
  index: number;
  linkTo: string;
}

export default function ToolCard({
  icon,
  title,
  subtitle,
  isPrimary = false,
  index,
  linkTo
}: ToolCardProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(linkTo);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.15 }}
      whileHover={{ scale: 1.02, y: -2 }}
      onClick={handleClick}
      className={`rounded-[16px] overflow-hidden relative group cursor-pointer ${
        isPrimary 
          ? 'bg-[#D4A574] text-[#0F0F0F]' 
          : 'bg-[#333333] text-[#FFFFFF]'
      }`}
    >
      <div className="p-6 flex flex-col items-center justify-center text-center h-full min-h-[140px]">
        {/* 图标 - 32x32dp */}
        <div className={`mb-3 ${isPrimary ? 'text-[#0F0F0F]' : 'text-[#FFFFFF]'}`}>
          {icon}
        </div>

        {/* 标题 - 18sp, 600字重 */}
        <h3 className={`text-[18px] font-semibold mb-1 ${
          isPrimary ? 'text-[#0F0F0F]' : 'text-[#FFFFFF]'
        }`}>
          {title}
        </h3>

        {/* 小字 - 12sp */}
        <p className={`text-[12px] ${
          isPrimary ? 'text-[#0F0F0F]/80' : 'text-[#CCCCCC]'
        }`}>
          {subtitle}
        </p>
      </div>

      {/* Hover效果 */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
    </motion.div>
  );
}
