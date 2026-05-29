import { motion, HTMLMotionProps } from 'framer-motion'
import { ReactNode, useState } from 'react'
import { ChevronRight, Check, Loader2 } from 'lucide-react'

export const ColorOSAnimations = {
  fadeIn: {
    initial: { opacity: 0, y: 20 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -20 },
    transition: { duration: 0.3, ease: [0.4, 0, 0.2, 1] }
  },
  slideIn: {
    initial: { opacity: 0, x: 20 },
    animate: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -20 },
    transition: { duration: 0.3, ease: [0.4, 0, 0.2, 1] }
  },
  scaleIn: {
    initial: { opacity: 0, scale: 0.95 },
    animate: { opacity: 1, scale: 1 },
    exit: { opacity: 0, scale: 0.95 },
    transition: { duration: 0.2, ease: [0.4, 0, 0.2, 1] }
  },
  stagger: {
    animate: {
      transition: {
        staggerChildren: 0.08
      }
    }
  }
}

interface ColorOSCardProps extends HTMLMotionProps<"div"> {
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
  const baseStyles = 'rounded-oppo-md overflow-hidden transition-all duration-300'
  
  const variants = {
    default: 'bg-card-surface border border-oppo-border/50',
    elevated: 'bg-elevated border border-oppo-border/30 shadow-lg',
    glass: 'bg-white/5 backdrop-blur-xl border border-white/10',
    gradient: 'bg-gradient-to-br from-card-surface to-elevated border border-oppo-border/30'
  }
  
  const interactiveStyles = interactive 
    ? 'hover:border-oppo-sunrise-gold/30 hover:shadow-oppo cursor-pointer hover:-translate-y-1' 
    : ''
  
  return (
    <motion.div
      className={`${baseStyles} ${variants[variant]} ${interactiveStyles} ${className}`}
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
  const baseStyles = 'inline-flex items-center justify-center font-semibold transition-all duration-300 rounded-oppo-sm disabled:opacity-50 disabled:cursor-not-allowed'
  
  const sizes = {
    sm: 'px-4 py-2 text-sm gap-1.5',
    md: 'px-6 py-3 text-base gap-2',
    lg: 'px-8 py-4 text-lg gap-2.5'
  }
  
  const variants = {
    primary: 'bg-oppo-sunrise-gold text-deep-space hover:bg-oppo-sunrise-gold/90 hover:shadow-oppo hover:-translate-y-0.5 active:translate-y-0',
    secondary: 'bg-white/10 text-white border border-white/20 hover:bg-white/20 hover:border-white/30',
    ghost: 'bg-transparent text-text-secondary hover:text-white hover:bg-white/5',
    danger: 'bg-error-vital text-white hover:bg-error-vital/90'
  }
  
  return (
    <motion.button
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      disabled={disabled || loading}
      className={`${baseStyles} ${sizes[size]} ${variants[variant]} ${className}`}
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
        {label && <p className="text-white font-medium">{label}</p>}
        {description && <p className="text-text-tertiary text-sm mt-0.5">{description}</p>}
      </div>
      <motion.button
        onClick={() => onChange(!checked)}
        className={`w-12 h-7 rounded-full p-1 transition-colors duration-300 ${
          checked ? 'bg-oppo-sunrise-gold' : 'bg-white/20'
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
          <span className="text-white font-medium">{value}{unit}</span>
        </div>
      )}
      <div className="relative h-2 bg-white/10 rounded-full overflow-hidden">
        <motion.div
          className="absolute left-0 top-0 h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full"
          initial={false}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.15 }}
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
      className={`flex items-center gap-4 p-4 rounded-oppo-sm transition-all duration-200 ${
        onClick ? 'cursor-pointer hover:bg-white/5 active:bg-white/10' : ''
      }`}
      whileTap={onClick ? { scale: 0.98 } : undefined}
    >
      {icon && (
        <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center flex-shrink-0">
          {icon}
        </div>
      )}
      <div className="flex-1 min-w-0">
        <p className="text-white font-medium truncate">{title}</p>
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
        <h3 className="text-lg font-semibold text-white">{title}</h3>
        {subtitle && <p className="text-text-tertiary text-sm mt-0.5">{subtitle}</p>}
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
      className={`inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 ${
        selected
          ? 'bg-oppo-sunrise-gold text-deep-space'
          : 'bg-white/10 text-text-secondary hover:bg-white/15 hover:text-white'
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
          {showPercentage && <span className="text-white font-medium">{Math.round(percentage)}%</span>}
        </div>
      )}
      <div className="h-2 bg-white/10 rounded-full overflow-hidden">
        <motion.div
          className="h-full bg-gradient-to-r from-oppo-sunrise-gold to-oppo-sunrise-gold-light rounded-full"
          initial={{ width: 0 }}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
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
    <div className="flex gap-2 p-1 bg-white/5 rounded-oppo-sm">
      {tabs.map((tab) => (
        <motion.button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-sm font-medium transition-colors ${
            activeTab === tab.id
              ? 'text-deep-space'
              : 'text-text-secondary hover:text-white'
          }`}
          animate={{
            backgroundColor: activeTab === tab.id ? '#FFB347' : 'transparent'
          }}
          transition={{ duration: 0.2 }}
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
          className={`flex items-center gap-4 p-4 rounded-oppo-sm cursor-pointer transition-all ${
            value === option.value
              ? 'bg-oppo-sunrise-gold/10 border border-oppo-sunrise-gold/30'
              : 'bg-white/5 border border-transparent hover:bg-white/10'
          }`}
          whileTap={{ scale: 0.98 }}
        >
          <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
            value === option.value
              ? 'border-oppo-sunrise-gold bg-oppo-sunrise-gold'
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
            <p className="text-white font-medium">{option.label}</p>
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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ duration: 0.2 }}
        className="w-full max-w-md bg-card-surface rounded-oppo-md border border-oppo-border/50 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-6">
          <h2 className="text-xl font-semibold text-white mb-4">{title}</h2>
          <div className="text-text-secondary">{children}</div>
        </div>
        {actions && (
          <div className="flex gap-3 p-4 border-t border-oppo-border/50">
            {actions}
          </div>
        )}
      </motion.div>
    </motion.div>
  )
}

export function ColorOSSkeleton() {
  return (
    <div className="animate-pulse space-y-4">
      <div className="h-4 bg-white/10 rounded w-3/4" />
      <div className="h-4 bg-white/10 rounded w-1/2" />
      <div className="h-32 bg-white/10 rounded-oppo-sm" />
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
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -20, scale: 0.95 }}
      className={`flex items-center gap-3 px-4 py-3 rounded-oppo-sm border ${colors[type]}`}
    >
      {icon}
      <span className="text-white text-sm font-medium">{message}</span>
    </motion.div>
  )
}
