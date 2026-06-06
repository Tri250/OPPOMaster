import { motion } from 'framer-motion'
import { useInView } from 'framer-motion'
import { useRef } from 'react'
import { Heart, Download } from 'lucide-react'

const presets = [
  {
    id: '1',
    name: '哈苏自然',
    author: '小O帮帮官方',
    image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600',
    tags: ['风景', '自然'],
    likes: 12580,
    downloads: 8920
  },
  {
    id: '2',
    name: '胶片复古',
    author: '摄影师小王',
    image: 'https://images.unsplash.com/photo-1493863641943-9b68992a8d07?w=600',
    tags: ['复古', '胶片'],
    likes: 8920,
    downloads: 6540
  },
  {
    id: '3',
    name: '夜景霓虹',
    author: '城市猎人',
    image: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=600',
    tags: ['夜景', '城市'],
    likes: 6540,
    downloads: 4320
  },
  {
    id: '4',
    name: '清新日系',
    author: '樱花妹',
    image: 'https://images.unsplash.com/photo-1522383225653-ed111181a951?w=600',
    tags: ['日系', '清新'],
    likes: 11200,
    downloads: 9870
  },
  {
    id: '5',
    name: '黑白人文',
    author: '街拍大师',
    image: 'https://images.unsplash.com/photo-1444723121867-c6126bab4d6e?w=600',
    tags: ['黑白', '人文'],
    likes: 9870,
    downloads: 7650
  },
  {
    id: '6',
    name: '美食诱惑',
    author: '吃货摄影师',
    image: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600',
    tags: ['美食', '生活'],
    likes: 7650,
    downloads: 5430
  }
]

function GalleryItem({ preset, index }: { preset: typeof presets[0]; index: number }) {
  const ref = useRef(null)
  const isInView = useInView(ref, { once: true, margin: "-50px" })

  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 30 }}
      animate={isInView ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.5, delay: index * 0.1 }}
      className="group relative overflow-hidden rounded-2xl bg-[#161B22] border border-[#30363D] hover:border-[#FF6B35]/30 transition-all duration-300"
    >
      {/* Image */}
      <div className="relative aspect-[4/3] overflow-hidden">
        <motion.img
          src={preset.image}
          alt={preset.name}
          className="w-full h-full object-cover"
          whileHover={{ scale: 1.05 }}
          transition={{ duration: 0.4 }}
        />
        
        {/* Overlay on hover */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
        
        {/* Stats on hover */}
        <div className="absolute bottom-0 left-0 right-0 p-4 translate-y-full group-hover:translate-y-0 transition-transform duration-300">
          <div className="flex items-center gap-4 text-white">
            <span className="flex items-center gap-1">
              <Heart size={16} className="text-red-400" />
              {preset.likes.toLocaleString()}
            </span>
            <span className="flex items-center gap-1">
              <Download size={16} className="text-blue-400" />
              {preset.downloads.toLocaleString()}
            </span>
          </div>
        </div>

        {/* Tags */}
        <div className="absolute top-3 left-3 flex gap-2">
          {preset.tags.map(tag => (
            <span 
              key={tag}
              className="px-2 py-1 bg-black/50 backdrop-blur-sm text-white text-xs rounded-md"
            >
              {tag}
            </span>
          ))}
        </div>
      </div>

      {/* Info */}
      <div className="p-4">
        <h3 className="text-white font-semibold text-lg mb-1 group-hover:text-[#FF6B35] transition-colors">
          {preset.name}
        </h3>
        <p className="text-gray-500 text-sm">by {preset.author}</p>
      </div>
    </motion.div>
  )
}

export default function Gallery() {
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
            精选预设
          </span>
          <h2 className="text-4xl sm:text-5xl font-bold text-white mb-4">
            专业摄影师的<span className="text-[#FF6B35]">调色配方</span>
          </h2>
          <p className="text-xl text-gray-400 max-w-2xl mx-auto">
            每一款预设都经过精心调校，让你的照片瞬间提升质感
          </p>
        </motion.div>

        {/* Gallery grid */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {presets.map((preset, index) => (
            <GalleryItem key={preset.id} preset={preset} index={index} />
          ))}
        </div>

        {/* View more button */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={isInView ? { opacity: 1 } : {}}
          transition={{ delay: 0.6 }}
          className="text-center mt-12"
        >
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            className="px-8 py-3 bg-[#30363D] hover:bg-[#484F58] text-white rounded-xl font-medium transition-colors"
          >
            查看更多预设
          </motion.button>
        </motion.div>
      </div>
    </section>
  )
}
