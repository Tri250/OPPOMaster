import { useState, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Scan, X, Camera, CheckCircle, Smartphone, Download, ExternalLink } from 'lucide-react'

interface QRScannerProps {
  isOpen: boolean
  onClose: () => void
}

export default function QRScanner({ isOpen, onClose }: QRScannerProps) {
  const [scanning, setScanning] = useState(false)
  const [scanned, setScanned] = useState(false)
  const [result, setResult] = useState<string | null>(null)
  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)

  // Mock QR codes for demo
  const mockQRCodes: { id: number; name: string; url: string; color: string }[] = []

  const startScanning = async () => {
    setScanning(true)
    setScanned(false)
    setResult(null)
    
    // Simulate scanning delay
    setTimeout(() => {
      const randomQR = mockQRCodes[Math.floor(Math.random() * mockQRCodes.length)]
      setResult(randomQR.url)
      setScanned(true)
      setScanning(false)
    }, 2000)
  }

  const stopScanning = () => {
    setScanning(false)
    setScanned(false)
    setResult(null)
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/90 backdrop-blur-md z-50 flex items-center justify-center p-4"
          onClick={onClose}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            className="bg-[#161B22] rounded-3xl p-8 max-w-md w-full border border-[#30363D] relative"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close button */}
            <button
              onClick={onClose}
              className="absolute top-4 right-4 p-2 hover:bg-[#30363D] rounded-full transition-colors"
            >
              <X size={24} className="text-gray-400" />
            </button>

            {/* Header */}
            <div className="text-center mb-8">
              <div className="w-16 h-16 bg-[#FF6B35]/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Scan size={32} className="text-[#FF6B35]" />
              </div>
              <h3 className="text-2xl font-bold text-white mb-2">扫码体验</h3>
              <p className="text-gray-400">扫描二维码快速下载或访问</p>
            </div>

            {/* Scanner area */}
            <div className="relative aspect-square bg-[#0D1117] rounded-2xl overflow-hidden mb-6">
              {!scanned ? (
                <>
                  {/* Scanning animation */}
                  <div className="absolute inset-0 flex items-center justify-center">
                    <div className="relative w-48 h-48">
                      {/* Corner markers */}
                      <div className="absolute top-0 left-0 w-8 h-8 border-l-4 border-t-4 border-[#FF6B35]" />
                      <div className="absolute top-0 right-0 w-8 h-8 border-r-4 border-t-4 border-[#FF6B35]" />
                      <div className="absolute bottom-0 left-0 w-8 h-8 border-l-4 border-b-4 border-[#FF6B35]" />
                      <div className="absolute bottom-0 right-0 w-8 h-8 border-r-4 border-b-4 border-[#FF6B35]" />
                      
                      {/* Scan line */}
                      {scanning && (
                        <motion.div
                          className="absolute left-0 right-0 h-0.5 bg-[#FF6B35]"
                          animate={{ top: ['0%', '100%', '0%'] }}
                          transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
                        />
                      )}
                      
                      {/* Center icon */}
                      <div className="absolute inset-0 flex items-center justify-center">
                        <Camera size={40} className={`${scanning ? 'text-[#FF6B35]' : 'text-gray-600'}`} />
                      </div>
                    </div>
                  </div>
                  
                  {/* Scanning text */}
                  {scanning && (
                    <div className="absolute bottom-4 left-0 right-0 text-center">
                      <span className="text-[#FF6B35] text-sm animate-pulse">正在扫描...</span>
                    </div>
                  )}
                </>
              ) : (
                <div className="absolute inset-0 flex flex-col items-center justify-center p-6">
                  <CheckCircle size={64} className="text-green-500 mb-4" />
                  <p className="text-white font-medium mb-2">扫描成功!</p>
                  <p className="text-gray-400 text-sm text-center break-all">{result}</p>
                </div>
              )}
            </div>

            {/* Action buttons */}
            <div className="space-y-3">
              {!scanned ? (
                <button
                  onClick={scanning ? stopScanning : startScanning}
                  className={`w-full py-4 rounded-xl font-semibold flex items-center justify-center gap-2 transition-all ${
                    scanning
                      ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20'
                      : 'bg-[#FF6B35] text-white hover:bg-[#FF8C42]'
                  }`}
                >
                  {scanning ? (
                    <>
                      <X size={20} />
                      取消扫描
                    </>
                  ) : (
                    <>
                      <Scan size={20} />
                      开始扫描
                    </>
                  )}
                </button>
              ) : (
                <div className="space-y-3">
                  <a
                    href={result || '#'}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="w-full py-4 bg-[#FF6B35] text-white rounded-xl font-semibold flex items-center justify-center gap-2 hover:bg-[#FF8C42] transition-colors"
                  >
                    <ExternalLink size={20} />
                    立即访问
                  </a>
                  <button
                    onClick={() => {
                      setScanned(false)
                      setResult(null)
                    }}
                    className="w-full py-4 bg-[#30363D] text-white rounded-xl font-semibold hover:bg-[#484F58] transition-colors"
                  >
                    重新扫描
                  </button>
                </div>
              )}
            </div>

            {/* Quick links */}
            <div className="mt-6 pt-6 border-t border-[#30363D]">
              <p className="text-gray-500 text-sm mb-3">或直接选择下载渠道：</p>
              <div className="grid grid-cols-3 gap-2">
                {mockQRCodes.map((qr) => (
                  <a
                    key={qr.id}
                    href={qr.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-3 bg-[#0D1117] rounded-lg text-center hover:bg-[#30363D] transition-colors group"
                  >
                    <div
                      className="w-8 h-8 rounded-lg mx-auto mb-2 flex items-center justify-center"
                      style={{ backgroundColor: `${qr.color}15` }}
                    >
                      <Download size={16} style={{ color: qr.color }} />
                    </div>
                    <span className="text-xs text-gray-400 group-hover:text-white">{qr.name}</span>
                  </a>
                ))}
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
