import { motion } from 'framer-motion'
import { ReactNode, useState, HTMLAttributes } from 'react'
import { ChevronRight, Check, Loader2, X, Maximize2, Minimize2, Eye, EyeOff, Move } from 'lucide-react'

export const ColorOSAnimations = {
  fadeIn: {
    initial: { opacity: 0, y: 20 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -20 },
    transition: { duration: 0.3, ease: 'easeOut' }
  },
  slideIn: {
    initial: { opacity: 0, x: 20 },
    animate: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -20 },
    transition: { duration: 0.3, ease: 'easeOut' }
  },
  scaleIn: {
    initial: { opacity: 0, scale: 0.98 },
    animate: { opacity: 1, scale: 1 },
    exit: { opacity: 0, scale: 0.98 },
    transition: { duration: 0.2, ease: 'easeInOut' }
  },
  slideUp: {
    initial: { opacity: 0, y: '100%' },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: '100%' },
    transition: { duration: 0.3, ease: 'easeOut' }
  },
  stagger: {
    animate: {
      transition: {
        staggerChildren: 0.1
      }
    }
  }
}

const easeStandard = [0.2, 0.0, 0.0, 1.0]
const easeDecelerate = [0.0, 0.0, 0.2, 1.0]
const easeAccelerate = [0.4, 0.0, 1.0, 0.0]
const easeOppoEnter = [0.05, 0.7, 0.1, 1.0]
const easeOppoExit = [0.3, 0.0, 0.8, 0.15]
const easeOppoBounce = [0.175, 0.885, 0.32, 1.275]

interface ColorOSCardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'elevated' | 'glass' | 'gradient'
  interactive?: boolean
  children: ReactNode
}

export function ColorOSCard({ 
  variant = 'default', 
  interactive = false, 
  children, 
  className = '',
  ...props 
}: ColorOSCardProps) {
  const baseStyles = 'rounded-oppo overflow-hidden transition-all duration-200'
  
  const variants = {
    default: 'bg-card-surface border border-oppo-border',
    elevated: 'bg-elevated border border-oppo-border/30 shadow-oppo-card',
    glass: 'bg-black/50 backdrop-blur-xl border border-white/15',
    gradient: 'bg-gradient-to-br from-card-surface to-elevated border border-oppo-border'
  }
  
  const interactiveStyles = interactive 
    ? 'hover:border-accent-primary/30 hover:shadow-oppo-hover cursor-pointer hover:-translate-y-0.5 active:bg-card-pressed active:scale-[0.98]' 
    : ''
  
  return (
    <motion.div
      className={`${baseStyles} ${variants[variant]} ${interactiveStyles} ${className}`}
      whileHover={interactive ? { scale: 1.02 } : undefined}
      whileTap={{ scale: 0.98 }}
      transition={{ duration: 0.2, ease: easeOppoBounce }}
      {...props}
    >
      {children}
    </motion.div>
  )
}

interface ColorOSButtonProps {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  icon?: ReactNode
  children: ReactNode
  onClick?: () => void
  className?: string
  disabled?: boolean
}

export function ColorOSButton({
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  children,
  onClick,
  className = '',
  disabled = false
}: ColorOSButtonProps) {
  const baseStyles = 'inline-flex items-center justify-center font-semibold transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed select-none'
  
  const sizes = {
    sm: 'px-4 py-2 text-sm gap-1.5 rounded-oppo h-9 min-w-[88px]',
    md: 'px-6 py-3 text-base gap-2 rounded-oppo h-11 min-w-[120px]',
    lg: 'px-8 py-4 text-lg gap-2.5 rounded-oppo h-14 min-w-[160px]'
  }
  
  const variants = {
    primary: 'bg-accent-primary text-deep-space hover:bg-accent-secondary hover:shadow-oppo-hover active:bg-accent-tertiary active:scale-[0.98]',
    secondary: 'bg-white/10 text-text-primary border border-white/20 hover:bg-white/20 active:scale-[0.98]',
    ghost: 'bg-transparent text-text-secondary hover:text-text-primary hover:bg-white/5 active:scale-[0.98]',
    danger: 'bg-error-vital text-text-primary hover:bg-error-vital/90 active:scale-[0.98]'
  }
  
  return (
    <motion.button
      onClick={onClick}
      disabled={disabled || loading}
      className={`${baseStyles} ${sizes[size]} ${variants[variant]} ${className}`}
      whileHover={!disabled && !loading ? { y: -2 } : undefined}
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

interface ColorOSSwitchProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
  description?: string
}

export function ColorOSSwitch({ checked, onChange, label, description }: ColorOSSwitchProps) {
  return (
    <div className="flex items-center justify-between py-3">
      <div className="flex-1">
        {label && <p className="text-text-primary font-medium">{label}</p>}
        {description && <p className="text-text-tertiary text-sm mt-0.5">{description}</p>}
      </div>
      <motion.button
        onClick={() => onChange(!checked)}
        className={`w-12 h-7 rounded-full p-1 transition-colors duration-200 ${
          checked ? 'bg-accent-primary' : 'bg-white/20'
        }`}
        whileTap={{ scale: 0.95 }}
      >
        <motion.div
          className="w-5 h-5 rounded-full bg-white shadow-md"
          animate={{ x: checked ? 20 : 0 }}
          transition={{ type: 'spring', stiffness: 500, damping: 30 }}
        />
      </motion.button>
    </div>
  )
}

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
      <div className="relative h-2 bg-white/10 rounded-full overflow-hidden">
        <motion.div
          className="absolute left-0 top-0 h-full bg-gradient-to-r from-accent-primary to-accent-secondary rounded-full"
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

interface ColorOSListItemProps {
  icon?: ReactNode
  title: string
  subtitle?: string
  trailing?: ReactNode
  onClick?: () => void
  showArrow?: boolean
}

export function ColorOSListItem({
  icon,
  title,
  subtitle,
  trailing,
  onClick,
  showArrow = false
}: ColorOSListItemProps) {
  return (
    <motion.div
      onClick={onClick}
      className={`flex items-center gap-4 p-4 rounded-oppo transition-all duration-200 select-none ${
        onClick ? 'cursor-pointer hover:bg-white/5 active:bg-white/10' : ''
      }`}
      whileTap={onClick ? { scale: 0.98 } : undefined}
    >
      {icon && (
        <div className="w-12 h-12 rounded-oppo bg-white/5 flex items-center justify-center flex-shrink-0">
          {icon}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-text-primary font-medium truncate">{title}</p>
        {subtitle && <p className="text-text-tertiary text-sm truncate">{subtitle}</p>}
      </div>
      {trailing}
      {showArrow && <ChevronRight className="w-5 h-5 text-text-tertiary flex-shrink-0" />}
    </motion.div>
  )
}

interface ColorOSSectionHeaderProps {
  title: string
  subtitle?: string
  action?: ReactNode
}

export function ColorOSSectionHeader({ title, subtitle, action }: ColorOSSectionHeaderProps) {
  return (
    <div className="flex items-center justify-between mb-4">
      <div>
        <h3 className="text-lg font-semibold text-text-primary">{title}</h3>
        {subtitle && <p className="text-text-tertiary text-sm mt-1">{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

interface ColorOSChipProps {
  label: string
  selected?: boolean
  onClick?: () => void
  icon?: ReactNode
}

export function ColorOSChip({ label, selected = false, onClick, icon }: ColorOSChipProps) {
  return (
    <motion.button
      onClick={onClick}
      whileTap={{ scale: 0.95 }}
      className={`inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 select-none ${
        selected
          ? 'bg-accent-primary text-deep-space shadow-md'
          : 'bg-white/10 text-text-secondary hover:bg-white/15 hover:text-text-primary'
      }`}
    >
      {icon}
      {label}
    </motion.button>
  )
}

interface ColorOSProgressBarProps {
  value: number
  max?: number
  label?: string
  showPercentage?: boolean
}

export function ColorOSProgressBar({ value, max = 100, label, showPercentage = true }: ColorOSProgressBarProps) {
  const percentage = Math.min((value / max) * 100, 100)
  
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
          className="h-full bg-gradient-to-r from-accent-primary to-accent-secondary rounded-full"
          initial={{ width: 0 }}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.5, ease: easeDecelerate }}
        />
      </div>
    </div>
  )
}

interface ColorOSTabsProps {
  tabs: { id: string; label: string; icon?: ReactNode }[]
  activeTab: string
  onChange: (id: string) => void
}

export function ColorOSTabs({ tabs, activeTab, onChange }: ColorOSTabsProps) {
  return (
    <div className="flex gap-1 p-1 bg-white/5 rounded-oppo">
      {tabs.map((tab) => (
        <motion.button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-oppo-sm text-sm font-medium transition-all duration-200 select-none ${
            activeTab === tab.id
              ? 'text-deep-space'
              : 'text-text-secondary hover:text-text-primary hover:bg-white/5'
          }`}
          animate={{
            backgroundColor: activeTab === tab.id ? '#FF6B35' : 'transparent'
          }}
          transition={{ duration: 0.2, ease: easeStandard }}
          whileTap={{ scale: 0.96 }}
        >
          {tab.icon}
          {tab.label}
        </motion.button>
      ))}
    </div>
  )
}

interface ColorOSRadioOption {
  value: string
  label: string
  description?: string
}

interface ColorOSRadioGroupProps {
  options: ColorOSRadioOption[]
  value: string
  onChange: (value: string) => void
  title?: string
}

export function ColorOSRadioGroup({ options, value, onChange, title }: ColorOSRadioGroupProps) {
  return (
    <div className="space-y-3">
      {title && <p className="text-text-secondary text-sm mb-2">{title}</p>}
      {options.map((option) => (
        <motion.div
          key={option.value}
          onClick={() => onChange(option.value)}
          className={`flex items-center gap-4 p-4 rounded-oppo cursor-pointer transition-all duration-200 select-none ${
            value === option.value
              ? 'bg-accent-primary/10 border border-accent-primary/30'
              : 'bg-white/5 border border-transparent hover:bg-white/10'
          }`}
          whileTap={{ scale: 0.98 }}
        >
          <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors duration-200 ${
            value === option.value
              ? 'border-accent-primary bg-accent-primary'
              : 'border-white/30'
          }`}>
            {value === option.value && (
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="w-2 h-2 rounded-full bg-deep-space"
              />
            )}
          </div>
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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.98, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.98, y: 20 }}
        transition={{ duration: 0.3, ease: easeOppoBounce }}
        className="w-full max-w-md bg-card-surface rounded-oppo border border-oppo-border overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-text-primary">{title}</h2>
            <button onClick={onClose} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors">
              <X className="w-4 h-4 text-text-secondary" />
            </button>
          </div>
          <div className="text-text-secondary">{children}</div>
        </div>
        {actions && (
          <div className="flex gap-3 p-4 border-t border-oppo-border bg-black/20">
            {actions}
          </div>
        )}
      </motion.div>
    </motion.div>
  )
}

export function ColorOSSkeleton() {
  return (
    <div className="animate-skeleton space-y-4">
      <div className="h-4 bg-white/10 rounded w-3/4" />
      <div className="h-4 bg-white/10 rounded w-1/2" />
      <div className="h-32 bg-white/10 rounded-oppo" />
    </div>
  )
}

interface ColorOSToastProps {
  message: string
  type?: 'success' | 'error' | 'info'
  icon?: ReactNode
}

export function ColorOSToast({ message, type = 'info', icon }: ColorOSToastProps) {
  const colors = {
    success: 'bg-oppo-green/20 border-oppo-green/30',
    error: 'bg-error-vital/20 border-error-vital/30',
    info: 'bg-ocean-blue/20 border-ocean-blue/30'
  }
  
  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -20, scale: 0.98 }}
      className={`flex items-center gap-3 px-4 py-3 rounded-oppo border ${colors[type]} backdrop-blur-xl`}
    >
      {icon}
      <span className="text-text-primary text-sm font-medium">{message}</span>
    </motion.div>
  )
}

interface ColorOSBottomSheetProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

export function ColorOSBottomSheet({ open, onClose, title, children }: ColorOSBottomSheetProps) {
  if (!open) return null
  
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className="fixed inset-0 z-50 flex items-end justify-center p-4 bg-black/60 backdrop-blur-md"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, y: '100%' }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: '100%' }}
        transition={{ duration: 0.3, ease: easeOppoEnter }}
        className="w-full max-w-lg bg-card-surface rounded-t-oppo border border-oppo-border overflow-hidden shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="w-12 h-1.5 bg-white/20 rounded-full mx-auto mt-3 mb-2" />
        <div className="p-6">
          <h2 className="text-xl font-semibold text-text-primary mb-4">{title}</h2>
          <div className="text-text-secondary">{children}</div>
        </div>
      </motion.div>
    </motion.div>
  )
}

interface ColorOSFilterCardProps {
  name: string
  author?: string
  category?: string
  isFavorited?: boolean
  isSelected?: boolean
  isNew?: boolean
  isHasselblad?: boolean
  onClick?: () => void
}

export function ColorOSFilterCard({
  name,
  author,
  category,
  isFavorited = false,
  isSelected = false,
  isNew = false,
  isHasselblad = false,
  onClick
}: ColorOSFilterCardProps) {
  return (
    <motion.div
      onClick={onClick}
      whileHover={isSelected ? { scale: 1.05 } : { scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      className={`relative overflow-hidden rounded-oppo cursor-pointer transition-all duration-200 select-none ${
        isSelected 
          ? 'border-2 border-oppo-green shadow-lg -translate-y-1' 
          : 'border border-transparent hover:border-white/20'
      }`}
    >
      <div className="aspect-square bg-gradient-to-br from-accent-primary/30 to-ocean-blue/30 flex items-center justify-center relative">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGw9IiNmZmYiIGZpbGwtb3BhY2l0eT0iMC4wNSI+PHBhdGggZD0iTTIwIDIwLjVWMjB2LjV6TTIwLjUgMjBoLS41LjV6TTIwIDIwaC0uNS41em0tLjUtLjVoLjUtLjV6TTE5LjUgMjBoLjUtLjV6TTIwIDE5LjVWMjB2LS41ek0yMC41IDE5LjVoLS41LjV6Ii8+PC9nPjwvZz48L3N2Zz4=')]" />
        
        <div className="absolute top-2 left-2 flex gap-1.5 z-10">
          {isNew && <span className="px-2 py-1 bg-oppo-green text-deep-space text-xs font-bold rounded-full">NEW</span>}
          {isHasselblad && <span className="px-2 py-1 bg-hasselblad-orange text-deep-space text-xs font-bold rounded-full">HNCS</span>}
        </div>
        
        {isSelected && (
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            className="absolute top-2 right-2 w-6 h-6 rounded-full bg-oppo-green flex items-center justify-center z-10"
          >
            <Check className="w-4 h-4 text-deep-space" />
          </motion.div>
        )}
        
        <div className="w-full h-full flex items-center justify-center">
          <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center">
            <div className="w-5 h-5 rounded-full border-2 border-accent-primary/50" />
          </div>
        </div>
      </div>
      
      <div className="p-4 bg-card-surface">
        <p className="text-text-primary font-medium text-sm truncate">{name}</p>
        {author && <p className="text-text-tertiary text-xs mt-0.5">@{author}</p>}
      </div>
    </motion.div>
  )
}

interface ColorOSFloatingWindowProps {
  filterName: string
  intensity: number
  isVisible: boolean
  isLocked: boolean
  onToggleVisible: () => void
  onToggleLock: () => void
}

export function ColorOSFloatingWindow({
  filterName,
  intensity,
  isVisible,
  isLocked,
  onToggleVisible,
  onToggleLock
}: ColorOSFloatingWindowProps) {
  if (!isVisible) {
    return (
      <motion.button
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={onToggleVisible}
        className="w-12 h-12 rounded-full bg-accent-primary flex items-center justify-center shadow-lg"
      >
        <Eye className="w-5 h-5 text-deep-space" />
      </motion.button>
    )
  }
  
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.2 }}
      className="w-72 p-4 bg-black/85 backdrop-blur-xl border border-white/15 rounded-oppo shadow-oppo-card"
    >
      <div className="flex items-center justify-between mb-2">
        <p className="text-text-primary font-medium text-sm">{filterName}</p>
        <div className="flex items-center gap-2">
          <button
            onClick={onToggleLock}
            className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
          >
            <Move className="w-3 h-3 text-text-secondary" />
          </button>
          <button
            onClick={onToggleVisible}
            className="w-6 h-6 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors"
          >
            <EyeOff className="w-3 h-3 text-text-secondary" />
          </button>
        </div>
      </div>
      
      <div className="space-y-1">
        <div className="flex items-center justify-between text-xs">
          <span className="text-text-tertiary">强度</span>
          <span className="text-text-primary font-medium">{intensity}%</span>
        </div>
        <div className="h-1.5 bg-white/20 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-accent-primary to-accent-secondary rounded-full" style={{ width: `${intensity}%` }} />
        </div>
      </div>
    </motion.div>
  )
}

export { easeStandard, easeDecelerate, easeAccelerate, easeOppoEnter, easeOppoExit, easeOppoBounce }
