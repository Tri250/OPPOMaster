import { motion } from 'framer-motion'
import { useInView } from 'framer-motion'
import { useRef } from 'react'
import { Star, Quote } from 'lucide-react'

const testimonials = [
  {
    id: 1,
    name: '摄影师小王',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100',
    rating: 5,
    content: 'OMaster真的是摄影爱好者的福音！以前每次拍照都要在网上搜索参数，现在一键就能找到合适的预设，太方便了。',
    date: '2024-12-15'
  },
  {
    id: 2,
    name: '樱花妹',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100',
    rating: 5,
    content: '界面设计太美了，深色主题配上哈苏橙，高级感满满。预设质量也很高，特别是日系清新风格，拍出来的照片超有感觉！',
    date: '2024-12-10'
  },
  {
    id: 3,
    name: '城市猎人',
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100',
    rating: 5,
    content: '悬浮窗功能太实用了！拍照时可以随时查看参数，不用来回切换App。夜景预设也很专业，城市霓虹效果绝绝子。',
    date: '2024-12-08'
  },
  {
    id: 4,
    name: '街拍大师',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100',
    rating: 5,
    content: '作为一个街拍爱好者，OMaster的黑白人文预设帮了我大忙。参数调得很到位，直出就很有质感，省了很多后期时间。',
    date: '2024-12-05'
  }
]

function TestimonialCard({ testimonial, index }: { testimonial: typeof testimonials[0]; index: number }) {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true, margin: "-50px" })

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 30 }}
      animate={isInView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.5, delay: index * 0.1 }}
      className="group relative bg-[#161B22] rounded-2xl p-6 border border-[#30363D] hover:border-[#FF6B35]/30 transition-all duration-300"
    >
      {/* Quote icon */}
      <div className="absolute top-4 right-4 text-[#FF6B35]/20 group-hover:text-[#FF6B35]/40 transition-colors">
        <Quote size={40} />
      </div>

      {/* Rating */}
      <div className="flex gap-1 mb-4">
        {Array.from({ length: 5 }).map((_, i) => (
          <Star
            key={i}
            size={16}
            className={i < testimonial.rating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'}
          />
        ))}
      </div>

      {/* Content */}
      <p className="text-gray-300 leading-relaxed mb-6 relative z-10">
        "{testimonial.content}"
      </p>

      {/* Author */}
      <div className="flex items-center gap-3">
        <img
          src={testimonial.avatar}
          alt={testimonial.name}
          className="w-12 h-12 rounded-full object-cover border-2 border-[#30363D]"
        />
        <div>
          <div className="text-white font-medium">{testimonial.name}</div>
          <div className="text-gray-500 text-sm">{testimonial.date}</div>
        </div>
      </div>
    </motion.div>
  )
}

export default function Testimonials() {
  const sectionRef = useRef(null)
  const isInView = useInView(sectionRef, { once: true, margin: "-100px" })

  return (
    <section ref={sectionRef} className="py-24 bg-[#0D1117] relative">
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-[#30363D] to-transparent" />
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <span className="inline-block px-4 py-1.5 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-sm font-medium mb-4">
            用户评价
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            来自用户的<span className="text-[#FF6B35]">真实反馈</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            听听摄影爱好者们怎么说
          </p>
        </motion.div>

        {/* Testimonials grid */}
        <div className="grid md:grid-cols-2 gap-6">
          {testimonials.map((testimonial, index) => (
            <TestimonialCard key={testimonial.id} testimonial={testimonial} index={index} />
          ))}
        </div>

        {/* Stats */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={isInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.6, delay: 0.4 }}
          className="mt-16 grid grid-cols-2 md:grid-cols-4 gap-6"
        >
          {[
            { value: '10K+', label: '活跃用户' },
            { value: '23+', label: '专业预设' },
            { value: '4.9', label: '用户评分' },
            { value: '50K+', label: '下载次数' }
          ].map((stat, index) => (
            <div key={index} className="text-center">
              <div className="text-3xl sm:text-4xl font-bold text-[#FF6B35] mb-2">{stat.value}</div>
              <div className="text-gray-400">{stat.label}</div>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  )
}
