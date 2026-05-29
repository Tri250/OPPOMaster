import { motion } from 'framer-motion'
import type { ReactNode } from 'react'
import { 
  ChevronRight, 
  Check, 
  Loader2, 
  X, 
  Eye, 
  EyeOff, 
  Move,
  ChevronDown,
  Info,
  Camera,
  Heart
} from 'lucide-react'

// ==========================================
// ColorOS 16 标准缓动曲线
// ==========================================
export const easeOppoEnter: [number, number, number, number] = [0.05, 0.7, 0.1, 1.0]
export const easeOppoExit: [number, number, number, number] = [0.3, 0.0, 0.8, 0.15]
export const easeOppoBounce: [number, number, number, number] = [0.175, 0.885, 0.32, 1.275]
export const easeStandard: [number, number, number, number] = [0.2, 0.0, 0.0, 1.0]
export const easeDecelerate: [number, number, number, number] = [0.0, 0.0, 0.2, 1.0]
export const easeAccelerate: [number, number, number, number] = [0.4, 0.0, 1.0, 0.0]

// ==========================================
// ColorOS 16 标准动画配置
// ==========================================
export const ColorOSAnimations = {
  fadeIn: {
    initial: { opacity: 0 },
    animate: { opacity: 1 },
    exit: { opacity: 0 },
    transition: { duration: 0.3, ease: easeOppoEnter }
  },
  fadeInUp: {
    initial: { opacity: 0, y: 24 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -24 },
    transition: { duration: 0.4, ease: easeOppoEnter }
  },
  fadeInDown: {
    initial: { opacity: 0, y: -24 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -24 },
    transition: { duration: 0.4, ease: easeOppoEnter }
  },
  scaleIn: {
    initial: { opacity: 0, scale: 0.95 },
    animate: { opacity: 1, scale: 1 },
    exit: { opacity: 0, scale: 0.95 },
    transition: { duration: 0.3, ease: easeOppoBounce }
  },
  slideInRight: {
    initial: { opacity: 0, x: 40 },
    animate: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -40 },
    transition: { duration: 0.3, ease: easeOppoEnter }
  },
  slideInLeft: {
    initial: { opacity: 0, x: -40 },
    animate: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: 40 },
    transition: { duration: 0.3, ease: easeOppoEnter }
  },
  slideUp: {
    initial: { opacity: 0, y: '100%' },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: '100%' },
    transition: { duration: 0.4, ease: easeOppoEnter }
  },
  stagger: {
    animate: { transition: { staggerChildren: 0.08 } }
  },
  staggerFast: {
    animate: { transition: { staggerChildren: 0.05 } }
  }
}

// ==========================================
// ColorOS 16 标准卡片
// ==========================================
interface ColorOSCardProps {
  variant?: 'default' | 'elevated' | 'glass' | 'glassElevated'
  interactive?: boolean
  children: ReactNode
  className?: string
  onClick?: () => void
}

export function ColorOSCard({ 
  variant = 'default', 
  interactive = false, 
  children, 
  className = '',
  onClick
}: ColorOSCardProps) {
  const baseStyles = 'rounded-2xl overflow-hidden transition-all duration-200 ease-out'
  
  const variants = {
    default: 'bg-bg-secondary border border-border-default',
    elevated: 'bg-bg-elevated border border-white/10 shadow-oppo-elevation-2',
    glass: 'bg-bg-glass backdrop-blur-xl border border-white/8',
    glassElevated: 'bg-bg-glass backdrop-blur-2xl border border-white/12 shadow-oppo-elevation-2'
  }
  
  const interactiveStyles = interactive 
    ? 'hover:border-oppo-orange/30 hover:shadow-oppo-elevation-3 cursor-pointer active:bg-bg-tertiary' 
    : ''
  
  const MotionComponent = onClick ? motion.div : 'div'
  
  return (
    <MotionComponent
      className={`${baseStyles} ${variants[variant]} ${interactiveStyles} ${className}`}
      whileHover={onClick ? { scale: 1.02, y: -4 } : undefined}
      whileTap={onClick ? { scale: 0.98 } : undefined}
      transition={{ duration: 0.2, ease: easeOppoBounce }}
      onClick={onClick}
    >
      {children}
    </MotionComponent>
  )
}

// ==========================================
// ColorOS 16 标准按钮
// ==========================================
interface ColorOSButtonProps {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg' | 'xl'
  loading?: boolean
  icon?: ReactNode
  children: ReactNode
  onClick?: () => void
  className?: string
  disabled?: boolean
  fullWidth?: boolean
}

export function ColorOSButton({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  children,
  onClick,
  className = '',
  disabled = false,
  fullWidth = false
}: ColorOSButtonProps) {
  const baseStyles = 'inline-flex items-center justify-center font-bold transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed select-none'
  
  const sizes = {
    sm: 'px-4 py-2.5 text-sm gap-1.5 rounded-xl h-10 min-w-[80px]',
    md: 'px-6 py-3.5 text-base gap-2 rounded-2xl h-12 min-w-[120px]',
    lg: 'px-8 py-4 text-lg gap-2.5 rounded-2xl h-14 min-w-[160px]',
    xl: 'px-10 py-4.5 text-xl gap-3 rounded-2xl h-16 min-w-[200px]'
  }
  
  const variants = {
    primary: 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black hover:shadow-oppo-glow-orange active:scale-[0.98]',
    secondary: 'bg-bg-tertiary text-text-primary border border-border-light hover:bg-white/10 active:scale-[0.98]',
    outline: 'bg-transparent text-oppo-orange border-2 border-oppo-orange/50 hover:border-oppo-orange hover:bg-oppo-orange/10 active:scale-[0.98]',
    ghost: 'bg-transparent text-text-secondary hover:text-text-primary hover:bg-white/5 active:scale-[0.98]',
    danger: 'bg-error text-white hover:bg-error/90 active:scale-[0.98]'
  }
  
  return (
    <motion.button
      onClick={onClick}
      disabled={disabled || loading}
      className={`${baseStyles} ${sizes[size]} ${variants[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      whileHover={!disabled && !loading ? { y: -2, scale: 1.02 } : undefined}
      whileTap={!disabled && !loading ? { scale: 0.98 } : undefined}
    >
      {loading ? (
        <Loader2 className="w-5 h-5 animate-spin" />
      ) : (
        <>
          {icon}
          {children}
        </>
      )}
    </motion.button>
  )
}

// ==========================================
// ColorOS 16 标准开关
// ==========================================
interface ColorOSSwitchProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
  description?: string
}

export function ColorOSSwitch({ checked, onChange, label, description }: ColorOSSwitchProps) {
  return (
    <div className="flex items-center justify-between py-3.5">
      <div className="flex-1">
        {label && <p className="text-text-primary font-medium text-base">{label}</p>}
        {description && <p className="text-text-tertiary text-sm mt-1">{description}</p>}
      </div>
      <motion.button
        onClick={() => onChange(!checked)}
        className={`w-14 h-8 rounded-full p-1 transition-colors duration-200 flex items-center ${
          checked ? 'bg-oppo-orange' : 'bg-white/15'
        }`}
        whileTap={{ scale: 0.95 }}
      >
        <motion.div
          className="w-6 h-6 rounded-full bg-white shadow-lg"
          animate={{ x: checked ? 24 : 0 }}
          transition={{ type: 'spring', stiffness: 500, damping: 30 }}
        />
      </motion.button>
    </div>
  )
}

// ==========================================
// ColorOS 16 标准滑块
// ==========================================
interface ColorOSSliderProps {
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  step?: number
  label?: string
  unit?: string
}

export function ColorOSSlider({
  value,
  onChange,
  min = 0,
  max = 100,
  step = 1,
  label,
  unit = ''
}: ColorOSSliderProps) {
  const percentage = ((value - min) / (max - min)) * 100
  
  return (
    <div className="space-y-3">
      {label && (
        <div className="flex justify-between items-center">
          <span className="text-text-secondary text-sm">{label}</span>
          <span className="text-text-primary font-medium">{value}{unit}</span>
        </div>
      )}
      <div className="relative h-2.5 bg-white/10 rounded-full overflow-hidden">
        <motion.div
          className="absolute left-0 top-0 h-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange rounded-full"
          initial={false}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.15, ease: easeStandard }}
        />
        <input
          type="range"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(e) => onChange(Number(e.target.value))}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
        />
      </div>
    </div>
  )
}

// ==========================================
// ColorOS 16 标准列表项
// ==========================================
interface ColorOSListItemProps {
  icon?: ReactNode
  iconBg?: string
  title: string
  subtitle?: string
  trailing?: ReactNode
  onClick?: () => void
  showArrow?: boolean
  className?: string
}

export function ColorOSListItem({
  icon,
  iconBg,
  title,
  subtitle,
  trailing,
  onClick,
  showArrow = false,
  className = ''
}: ColorOSListItemProps) {
  return (
    <motion.div
      onClick={onClick}
      className={`flex items-center gap-4 p-4.5 rounded-2xl transition-all duration-200 select-none ${
        onClick ? 'cursor-pointer hover:bg-white/5 active:bg-white/10' : ''
      } ${className}`}
      whileTap={onClick ? { scale: 0.98 } : undefined}
    >
      {icon && (
        <div className={`w-12 h-12 rounded-2xl ${iconBg || 'bg-white/5'} flex items-center justify-center flex-shrink-0`}>
          {icon}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-text-primary font-medium truncate">{title}</p>
        {subtitle && <p className="text-text-tertiary text-sm truncate mt-0.5">{subtitle}</p>}
      </div>
      {trailing}
      {showArrow && <ChevronRight className="w-5 h-5 text-text-tertiary flex-shrink-0" />}
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 标准章节标题
// ==========================================
interface ColorOSSectionHeaderProps {
  title: string
  subtitle?: string
  action?: ReactNode
}

export function ColorOSSectionHeader({ title, subtitle, action }: ColorOSSectionHeaderProps) {
  return (
    <div className="flex items-center justify-between mb-5">
      <div>
        <h3 className="text-h3 font-bold text-text-primary">{title}</h3>
        {subtitle && <p className="text-text-tertiary text-sm mt-1">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

// ==========================================
// ColorOS 16 标准标签/芯片
// ==========================================
interface ColorOSChipProps {
  label: string
  selected?: boolean
  onClick?: () => void
  icon?: ReactNode
  variant?: 'default' | 'primary' | 'ghost'
}

export function ColorOSChip({ label, selected = false, onClick, icon, variant = 'default' }: ColorOSChipProps) {
  const variants = {
    default: selected 
      ? 'bg-oppo-orange text-oppo-black shadow-oppo-elevation-1'
      : 'bg-white/8 text-text-secondary hover:bg-white/12 hover:text-text-primary',
    primary: selected
      ? 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black shadow-oppo-elevation-1'
      : 'bg-white/8 text-text-secondary hover:bg-white/12 hover:text-text-primary',
    ghost: selected
      ? 'bg-white/12 text-text-primary'
      : 'bg-transparent text-text-tertiary hover:text-text-primary hover:bg-white/5'
  }
  
  return (
    <motion.button
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-medium transition-all duration-200 select-none ${variants[variant]}`}
    >
      {icon}
      {label}
    </motion.button>
  )
}

// ==========================================
// ColorOS 16 标准进度条
// ==========================================
interface ColorOSProgressBarProps {
  value: number
  max?: number
  label?: string
  showPercentage?: boolean
  variant?: 'default' | 'gradient' | 'success'
}

export function ColorOSProgressBar({ 
  value, 
  max = 100, 
  label, 
  showPercentage = true,
  variant = 'default'
}: ColorOSProgressBarProps) {
  const percentage = Math.min((value / max) * 100, 100)
  
  const variants = {
    default: 'bg-oppo-orange',
    gradient: 'bg-gradient-to-r from-oppo-orange to-hasselblad-orange',
    success: 'bg-oppo-green'
  }
  
  return (
    <div className="space-y-2">
      {(label || showPercentage) && (
        <div className="flex justify-between text-sm">
          {label && <span className="text-text-secondary">{label}</span>}
          {showPercentage && <span className="text-text-primary font-medium">{Math.round(percentage)}%</span>}
        </div>
      )}
      <div className="h-2 bg-white/10 rounded-full overflow-hidden">
        <motion.div
          className={`h-full ${variants[variant]} rounded-full`}
          initial={{ width: 0 }}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.6, ease: easeDecelerate }}
        />
      </div>
    </div>
  )
}

// ==========================================
// ColorOS 16 标准标签页
// ==========================================
interface ColorOSTabsProps {
  tabs: { id: string; label: string; icon?: ReactNode }[]
  activeTab: string
  onChange: (id: string) => void
  variant?: 'default' | 'minimal'
}

export function ColorOSTabs({ tabs, activeTab, onChange, variant = 'default' }: ColorOSTabsProps) {
  return (
    <div className={`${variant === 'default' ? 'flex gap-1 p-1 bg-white/5 rounded-2xl' : 'flex gap-6 border-b border-white/5'}`}>
      {tabs.map((tab) => (
        <motion.button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 ${variant === 'default' ? 'rounded-xl' : 'pb-4 border-b-2 border-transparent'} text-sm font-semibold transition-all duration-200 select-none ${
            activeTab === tab.id
              ? variant === 'default'
                ? 'text-oppo-black bg-gradient-to-r from-oppo-orange to-hasselblad-orange shadow-oppo-elevation-1'
                : 'text-oppo-orange border-oppo-orange'
              : 'text-text-secondary hover:text-text-primary hover:bg-white/5'
          }`}
          whileTap={{ scale: 0.96 }}
        >
          {tab.icon}
          {tab.label}
        </motion.button>
      ))}
    </div>
  )
}

// ==========================================
// ColorOS 16 标准单选组
// ==========================================
interface ColorOSRadioOption {
  value: string
  label: string
  description?: string
  icon?: ReactNode
}

interface ColorOSRadioGroupProps {
  options: ColorOSRadioOption[]
  value: string
  onChange: (value: string) => void
  title?: string
}

export function ColorOSRadioGroup({ options, value, onChange, title }: ColorOSRadioGroupProps) {
  return (
    <div className="space-y-2.5">
      {title && <p className="text-text-secondary text-sm mb-2">{title}</p>}
      {options.map((option) => (
        <motion.div
          key={option.value}
          onClick={() => onChange(option.value)}
          className={`flex items-center gap-4 p-4.5 rounded-2xl cursor-pointer transition-all duration-200 select-none ${
            value === option.value
              ? 'bg-oppo-orange/10 border border-oppo-orange/30'
              : 'bg-white/5 border border-transparent hover:bg-white/10'
          }`}
          whileTap={{ scale: 0.98 }}
        >
          <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors duration-200 ${
            value === option.value
              ? 'border-oppo-orange bg-oppo-orange'
              : 'border-white/30'
          }`}>
            {value === option.value && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="w-2.5 h-2.5 rounded-full bg-oppo-black"
              />
            )}
          </div>
          {option.icon && (
            <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center">
              {option.icon}
            </div>
          )}
          <div className="flex-1">
            <p className="text-text-primary font-medium">{option.label}</p>
            {option.description && (
              <p className="text-text-tertiary text-sm mt-0.5">{option.description}</p>
            )}
          </div>
        </motion.div>
      ))}
    </div>
  )
}

// ==========================================
// ColorOS 16 标准对话框
// ==========================================
interface ColorOSDialogProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  actions?: ReactNode
}

export function ColorOSDialog({ open, onClose, title, children, actions }: ColorOSDialogProps) {
  if (!open) return null
  
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xl"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 20 }}
        transition={{ duration: 0.35, ease: easeOppoBounce }}
        className="w-full max-w-md bg-bg-secondary rounded-3xl border border-border-default overflow-hidden shadow-oppo-elevation-3"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-h2 font-bold text-text-primary">{title}</h2>
            <button onClick={onClose} className="w-9 h-9 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors">
              <X className="w-4.5 h-4.5 text-text-secondary" />
            </button>
          </div>
          <div className="text-text-secondary">{children}</div>
        </div>
        {actions && (
          <div className="flex gap-3 p-5 border-t border-border-default bg-black/10">
            {actions}
          </div>
        )}
      </motion.div>
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 标准骨架屏
// ==========================================
export function ColorOSSkeleton() {
  return (
    <div className="animate-skeleton space-y-4">
      <div className="h-4 bg-white/10 rounded w-3/4" />
      <div className="h-4 bg-white/10 rounded w-1/2" />
      <div className="h-32 bg-white/10 rounded-2xl" />
    </div>
  )
}

// ==========================================
// ColorOS 16 标准Toast/提示
// ==========================================
interface ColorOSToastProps {
  message: string
  type?: 'success' | 'error' | 'info' | 'warning'
  icon?: ReactNode
}

export function ColorOSToast({ message, type = 'info', icon }: ColorOSToastProps) {
  const colors = {
    success: 'bg-oppo-green/20 border-oppo-green/30',
    error: 'bg-error/20 border-error/30',
    warning: 'bg-warning/20 border-warning/30',
    info: 'bg-ocean-blue/20 border-ocean-blue/30'
  }
  
  const icons = {
    success: <Check className="w-4.5 h-4.5 text-oppo-green" />,
    error: <X className="w-4.5 h-4.5 text-error" />,
    warning: <Info className="w-4.5 h-4.5 text-warning" />,
    info: <Info className="w-4.5 h-4.5 text-ocean-blue" />
  }
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.96 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -20, scale: 0.96 }}
      className={`flex items-center gap-3.5 px-4.5 py-3.5 rounded-2xl border ${colors[type]} backdrop-blur-xl shadow-oppo-elevation-1`}
    >
      {icon || icons[type]}
      <span className="text-text-primary text-sm font-medium">{message}</span>
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 标准底部抽屉
// ==========================================
interface ColorOSBottomSheetProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  height?: 'default' | 'large' | 'medium'
}

export function ColorOSBottomSheet({ 
  open, 
  onClose, 
  title, 
  children,
  height = 'default'
}: ColorOSBottomSheetProps) {
  if (!open) return null
  
  const heights = {
    default: 'max-h-[70vh]',
    medium: 'max-h-[50vh]',
    large: 'max-h-[85vh]'
  }
  
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 backdrop-blur-xl"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, y: '100%' }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: '100%' }}
        transition={{ duration: 0.4, ease: easeOppoEnter }}
        className={`w-full max-w-lg bg-bg-secondary rounded-t-3xl border border-border-default overflow-hidden shadow-oppo-elevation-4 ${heights[height]}`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-12 h-1.5 bg-white/20 rounded-full mx-auto mt-3.5 mb-3" />
        <div className="px-6 pb-6 overflow-y-auto">
          <h2 className="text-h2 font-bold text-text-primary mb-4">{title}</h2>
          <div className="text-text-secondary">{children}</div>
        </div>
      </motion.div>
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 标准输入框
// ==========================================
interface ColorOSInputProps {
  value?: string
  onChange?: (value: string) => void
  placeholder?: string
  label?: string
  error?: string
  icon?: ReactNode
  disabled?: boolean
  type?: string
  className?: string
}

export function ColorOSInput({
  value,
  onChange,
  placeholder,
  label,
  error,
  icon,
  disabled = false,
  type = 'text',
  className = ''
}: ColorOSInputProps) {
  return (
    <div className="space-y-1.5">
      {label && <label className="text-text-secondary text-sm">{label}</label>}
      <div className="relative">
        {icon && (
          <div className="absolute left-4 top-1/2 -translate-y-1/2 text-text-tertiary">
            {icon}
          </div>
        )}
        <input
          type={type}
          value={value}
          onChange={(e) => onChange?.(e.target.value)}
          placeholder={placeholder}
          disabled={disabled}
          className={`w-full px-4 py-3.5 bg-bg-tertiary border ${
            error ? 'border-error' : 'border-border-default'
          } rounded-2xl text-text-primary placeholder-text-tertiary transition-all duration-200 focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20 disabled:opacity-50 ${
            icon ? 'pl-11' : ''
          } ${className}`}
        />
      </div>
      {error && <p className="text-error text-xs">{error}</p>}
    </div>
  )
}

// ==========================================
// ColorOS 16 预设卡片组件
// ==========================================
interface ColorOSFilterCardProps {
  name: string
  author?: string
  category?: string
  coverImage?: string
  isFavorited?: boolean
  isSelected?: boolean
  isNew?: boolean
  isHasselblad?: boolean
  isFeatured?: boolean
  isPremium?: boolean
  onClick?: () => void
  onFavorite?: () => void
}

export function ColorOSFilterCard({
  name,
  author,
  category,
  coverImage,
  isFavorited = false,
  isSelected = false,
  isNew = false,
  isHasselblad = false,
  isFeatured = false,
  isPremium = false,
  onClick,
  onFavorite
}: ColorOSFilterCardProps) {
  return (
    <motion.div
      onClick={onClick}
      whileHover={isSelected ? { scale: 1.06 } : { scale: 1.04, y: -4 }}
      whileTap={{ scale: 0.98 }}
      className={`relative overflow-hidden rounded-2xl cursor-pointer transition-all duration-300 ease-out-elastic select-none ${
        isSelected 
          ? 'border-2 border-oppo-green shadow-oppo-elevation-3 -translate-y-1' 
          : 'border border-transparent hover:border-white/20'
      }`}
    >
      <div className="aspect-[3/4] bg-gradient-to-br from-oppo-orange/25 via-hasselblad-orange/15 to-ocean-blue/20 flex items-center justify-center relative">
        {coverImage ? (
          <img 
            src={coverImage} 
            alt={name} 
            className="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
          />
        ) : (
          <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNCI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
        )}
        
        <div className="absolute top-3 left-3 right-3 flex items-center justify-between z-10">
          <div className="flex items-center gap-1.5 flex-wrap">
            {category && <span className="px-2 py-0.5 bg-white/10 text-white/80 text-[10px] font-medium rounded-full">{category}</span>}
            {isNew && <span className="px-2.5 py-1 bg-gradient-to-r from-oppo-orange to-hasselblad-orange text-oppo-black text-xs font-bold rounded-full animate-breathing shadow-oppo-elevation-1">NEW</span>}
            {isHasselblad && <span className="px-2.5 py-1 bg-hasselblad-orange text-oppo-black text-xs font-bold rounded-full shadow-oppo-elevation-1 flex items-center gap-1"><Camera className="w-3.5 h-3.5" />HNCS</span>}
            {isFeatured && <span className="px-2.5 py-1 bg-gradient-to-r from-oppo-green to-ocean-blue text-white text-xs font-bold rounded-full shadow-oppo-elevation-1">精选</span>}
            {isPremium && <span className="px-2.5 py-1 bg-gradient-to-r from-oppo-purple to-oppo-pink text-white text-xs font-bold rounded-full shadow-oppo-elevation-1">PRO</span>}
          </div>
          
          {isSelected && (
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              className="w-7 h-7 rounded-full bg-oppo-green flex items-center justify-center shadow-oppo-elevation-1"
            >
              <Check className="w-4 h-4 text-oppo-black" />
            </motion.div>
          )}
        </div>
        
        {onFavorite && (
          <motion.button
            whileHover={{ scale: 1.15 }}
            whileTap={{ scale: 0.9 }}
            onClick={(e) => { e.stopPropagation(); onFavorite(); }}
            className="absolute top-3 right-3 w-9 h-9 bg-bg-glass/80 backdrop-blur-xl rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10"
          >
            <Heart
              className={`w-4.5 h-4.5 transition-colors duration-200 ${
                isFavorited ? 'text-oppo-orange fill-oppo-orange scale-110' : 'text-white'
              }`}
            />
          </motion.button>
        )}
        
        <div className="w-full h-full flex items-center justify-center">
          {!coverImage && (
            <div className="w-12 h-12 rounded-full bg-white/10 backdrop-blur-sm flex items-center justify-center">
              <div className="w-6 h-6 rounded-full border-2 border-white/30" />
            </div>
          )}
        </div>
        
        <div className="absolute inset-0 bg-gradient-to-t from-bg-primary/95 via-bg-primary/30 to-transparent" />
      </div>
      
      <div className="p-4.5 bg-bg-secondary">
        <p className="text-text-primary font-bold text-sm truncate">{name}</p>
        {author && <p className="text-text-tertiary text-xs mt-1">by {author}</p>}
      </div>
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 悬浮窗组件
// ==========================================
interface ColorOSFloatingWindowProps {
  filterName: string
  presetName?: string
  deviceModel?: string
  intensity?: number
  isVisible: boolean
  isLocked: boolean
  isExpanded?: boolean
  onToggleVisible: () => void
  onToggleLock: () => void
  onToggleExpand?: () => void
}

export function ColorOSFloatingWindow({
  filterName,
  presetName,
  deviceModel,
  intensity = 100,
  isVisible,
  isLocked,
  isExpanded = false,
  onToggleVisible,
  onToggleLock,
  onToggleExpand
}: ColorOSFloatingWindowProps) {
  if (!isVisible) {
    return (
      <motion.button
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={onToggleVisible}
        className="w-14 h-14 rounded-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange flex items-center justify-center shadow-oppo-elevation-3 animate-pulse-glow"
      >
        <Eye className="w-6 h-6 text-oppo-black" />
      </motion.button>
    )
  }
  
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.85 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.25 }}
      className="w-80 p-5 bg-bg-glass/95 backdrop-blur-2xl border border-white/15 rounded-3xl shadow-oppo-elevation-3"
    >
      <div className="flex items-center justify-between mb-3">
        <div>
          <p className="text-text-primary font-bold text-base">{filterName}</p>
          {presetName && <p className="text-text-tertiary text-xs mt-0.5">{presetName}</p>}
        </div>
        <div className="flex items-center gap-1.5">
          {onToggleExpand && (
            <button
              onClick={onToggleExpand}
              className="w-7.5 h-7.5 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
            >
              <ChevronDown className={`w-4 h-4 text-text-tertiary transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`} />
            </button>
          )}
          <button
            onClick={onToggleLock}
            className={`w-7.5 h-7.5 rounded-full flex items-center justify-center transition-all duration-200 ${isLocked ? 'bg-oppo-orange/20' : 'bg-white/5 hover:bg-white/10'}`}
          >
            <Move className={`w-3.5 h-3.5 ${isLocked ? 'text-oppo-orange' : 'text-text-secondary'}`} />
          </button>
          <button
            onClick={onToggleVisible}
            className="w-7.5 h-7.5 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
          >
            <EyeOff className="w-3.5 h-3.5 text-text-secondary" />
          </button>
        </div>
      </div>
      
      {deviceModel && (
        <div className="mb-3">
          <span className="text-xs text-text-tertiary bg-white/5 px-2.5 py-1 rounded-full">
            {deviceModel}
          </span>
        </div>
      )}
      
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs">
          <span className="text-text-tertiary">参数强度</span>
          <span className="text-text-primary font-medium">{intensity}%</span>
        </div>
        <div className="h-2 bg-white/15 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange rounded-full transition-all duration-200" style={{ width: `${intensity}%` }} />
        </div>
      </div>
    </motion.div>
  )
}

// ==========================================
// ColorOS 16 分割线组件
// ==========================================
interface ColorOSDividerProps {
  inset?: boolean
  className?: string
}

export function ColorOSDivider({ inset = false, className = '' }: ColorOSDividerProps) {
  return (
    <div className={`h-px bg-border-default ${inset ? 'mx-4' : ''} ${className}`} />
  )
}

// ==========================================
// ColorOS 16 空状态组件
// ==========================================
interface ColorOSEmptyStateProps {
  icon?: ReactNode
  title: string
  subtitle?: string
  action?: ReactNode
}

export function ColorOSEmptyState({ icon, title, subtitle, action }: ColorOSEmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
      {icon && (
        <div className="w-20 h-20 rounded-3xl bg-white/5 flex items-center justify-center mb-6">
          {icon}
        </div>
      )}
      <h3 className="text-h3 font-bold text-text-primary mb-2">{title}</h3>
      {subtitle && <p className="text-text-secondary text-sm mb-6 max-w-xs">{subtitle}</p>}
      {action}
    </div>
  )
}

export { Heart } from 'lucide-react'
