import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';

interface FeatureCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  features: string[];
  gradient?: string;
  color: string;
  index: number;
  linkTo: string;
}

export default function FeatureCard({
  icon,
  title,
  description,
  features,
  gradient,
  color,
  index,
  linkTo
}: FeatureCardProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(linkTo);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.1 }}
      whileHover={{ y: -4, scale: 1.02 }}
      onClick={handleClick}
      className="bg-[#1A1A1A]/95 backdrop-blur-sm rounded-[16px] overflow-hidden relative group cursor-pointer"
      style={{
        boxShadow: '0 4px 8px rgba(0, 0, 0, 0.1)',
      }}
    >
      <div className="p-6">
        {/* 图标容器 - 64x64dp，圆角12dp */}
        <div
          className={`w-16 h-16 rounded-[12px] flex items-center justify-center mb-4`}
          style={gradient ? {
            background: gradient
          } : {
            backgroundColor: color
          }}
        >
          {icon}
        </div>

        {/* 标题 - 20sp, 700字重, #FFFFFF */}
        <h3 className="text-[20px] font-bold text-[#FFFFFF] mb-3" style={{
          lineHeight: '1.3',
          letterSpacing: '0.5px'
        }}>
          {title}
        </h3>

        {/* 描述 - 14sp, 400字重, #CCCCCC */}
        <p className="text-[14px] text-[#CCCCCC] mb-4" style={{
          lineHeight: '1.5',
          letterSpacing: '0.3px'
        }}>
          {description}
        </p>

        {/* 列表项 */}
        <ul className="space-y-2">
          {features.map((feature, idx) => (
            <li key={idx} className="flex items-start gap-2">
              {/* 实心圆点标记 - 直径6dp */}
              <span
                className="w-1.5 h-1.5 rounded-full mt-1.5 flex-shrink-0"
                style={{ backgroundColor: color }}
              />
              <span className="text-[13px] text-[#999999]" style={{
                lineHeight: '1.5'
              }}>
                {feature}
              </span>
            </li>
          ))}
        </ul>
      </div>

      {/* Hover效果 */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
    </motion.div>
  );
}
