import { motion, AnimatePresence } from 'framer-motion';
import { 
  Download, 
  Palette, 
  Move, 
  CheckCircle2, 
  X, 
  RotateCw, 
  Image as ImageIcon,
  AlertCircle,
  Check
} from 'lucide-react';
import { useState, useRef, useEffect, useCallback } from 'react';
import { 
  ColorOSCard, 
  ColorOSButton, 
  ColorOSSectionHeader,
  easeOppoEnter,
  easeOppoBounce
} from '../components/common/ColorOSComponents';

// ==========================================
// 水印模板定义（10+品牌）
// ==========================================

interface WatermarkTemplate {
  id: string;
  name: string;
  brand: 'hasselblad' | 'oppo' | 'oneplus' | 'realme' | 'xiaomi' | 'samsung' | 'apple' | 'minimal' | 'film' | 'polaroid' | 'leica' | 'canon' | '2026-cyberpunk' | '2026-nature' | '2026-tech' | '2026-minimalist' | '2026-retro-future' | '2026-ocean' | '2026-aurora' | '2026-vintage' | '2026-neon' | '2026-classic' | '2026-elegant' | '2026-dynamic' | '2026-zen' | '2026-urban' | '2026-forest' | '2026-sky' | '2026-fire' | '2026-stone' | '2026-rainbow';
  color: string;
  bgColor: string;
  secondaryColor: string;
  fontFamily: string;
  fontSize: { title: number; subtitle: number; date: number };
  logo: string;
}

const watermarkTemplates: WatermarkTemplate[] = [
  {
    id: 'hasselblad',
    name: '哈苏经典',
    brand: 'hasselblad',
    color: '#D4A574',
    secondaryColor: '#C49664',
    bgColor: 'rgba(212, 165, 116, 0.08)',
    fontFamily: 'system-ui, -apple-system, serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'H'
  },
  {
    id: 'oppo-find',
    name: 'OPPO Find',
    brand: 'oppo',
    color: '#00D7A0',
    secondaryColor: '#00B888',
    bgColor: 'rgba(0, 215, 160, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'OPPO'
  },
  {
    id: 'oneplus',
    name: 'OnePlus 旗舰',
    brand: 'oneplus',
    color: '#FF3333',
    secondaryColor: '#E62E2E',
    bgColor: 'rgba(255, 51, 51, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '1+'
  },
  {
    id: 'realme',
    name: 'realme 真我',
    brand: 'realme',
    color: '#FFC107',
    secondaryColor: '#E6AC06',
    bgColor: 'rgba(255, 193, 7, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'RM'
  },
  {
    id: 'xiaomi',
    name: '小米徕卡',
    brand: 'xiaomi',
    color: '#FF6900',
    secondaryColor: '#E65E00',
    bgColor: 'rgba(255, 105, 0, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'MI'
  },
  {
    id: 'samsung',
    name: 'Samsung Galaxy',
    brand: 'samsung',
    color: '#1428A0',
    secondaryColor: '#122490',
    bgColor: 'rgba(20, 40, 160, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'S'
  },
  {
    id: 'apple',
    name: 'Apple iPhone',
    brand: 'apple',
    color: '#A2AAAD',
    secondaryColor: '#929A9D',
    bgColor: 'rgba(162, 170, 173, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: ''
  },
  {
    id: 'minimal',
    name: '简约现代',
    brand: 'minimal',
    color: '#FFFFFF',
    secondaryColor: '#E0E0E0',
    bgColor: 'rgba(255, 255, 255, 0.05)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 16, subtitle: 12, date: 10 },
    logo: '•'
  },
  {
    id: 'film',
    name: '胶片风格',
    brand: 'film',
    color: '#8B7355',
    secondaryColor: '#7A6548',
    bgColor: 'rgba(139, 115, 85, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 17, subtitle: 12, date: 10 },
    logo: 'FILM'
  },
  {
    id: 'polaroid',
    name: '宝丽来',
    brand: 'polaroid',
    color: '#FF4500',
    secondaryColor: '#E63E00',
    bgColor: 'rgba(255, 69, 0, 0.08)',
    fontFamily: 'monospace',
    fontSize: { title: 16, subtitle: 12, date: 11 },
    logo: 'PLD'
  },
  {
    id: 'leica',
    name: '徕卡经典',
    brand: 'leica',
    color: '#D80027',
    secondaryColor: '#C20023',
    bgColor: 'rgba(216, 0, 39, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'L'
  },
  {
    id: 'canon',
    name: '佳能',
    brand: 'canon',
    color: '#CC0000',
    secondaryColor: '#B80000',
    bgColor: 'rgba(204, 0, 0, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: 'C'
  },
  // ==========================================
  // 2026 年水印模板
  // ==========================================
  {
    id: '2026-cyberpunk',
    name: '赛博朋克 2026',
    brand: '2026-cyberpunk',
    color: '#00FFFF',
    secondaryColor: '#FF00FF',
    bgColor: 'rgba(0, 255, 255, 0.1)',
    fontFamily: 'monospace',
    fontSize: { title: 19, subtitle: 14, date: 12 },
    logo: '2026'
  },
  {
    id: '2026-nature',
    name: '自然生态 2026',
    brand: '2026-nature',
    color: '#2ECC71',
    secondaryColor: '#27AE60',
    bgColor: 'rgba(46, 204, 113, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 17, subtitle: 13, date: 11 },
    logo: '🌿'
  },
  {
    id: '2026-tech',
    name: '科技未来 2026',
    brand: '2026-tech',
    color: '#3498DB',
    secondaryColor: '#2980B9',
    bgColor: 'rgba(52, 152, 219, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '⚡'
  },
  {
    id: '2026-minimalist',
    name: '极简主义 2026',
    brand: '2026-minimalist',
    color: '#ECF0F1',
    secondaryColor: '#BDC3C7',
    bgColor: 'rgba(236, 240, 241, 0.05)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 15, subtitle: 11, date: 10 },
    logo: '▢'
  },
  {
    id: '2026-retro-future',
    name: '复古未来 2026',
    brand: '2026-retro-future',
    color: '#FF6B35',
    secondaryColor: '#F39C12',
    bgColor: 'rgba(255, 107, 53, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🚀'
  },
  {
    id: '2026-ocean',
    name: '海洋之心 2026',
    brand: '2026-ocean',
    color: '#1ABC9C',
    secondaryColor: '#16A085',
    bgColor: 'rgba(26, 188, 156, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 17, subtitle: 12, date: 11 },
    logo: '🌊'
  },
  {
    id: '2026-aurora',
    name: '极光 2026',
    brand: '2026-aurora',
    color: '#9B59B6',
    secondaryColor: '#8E44AD',
    bgColor: 'rgba(155, 89, 182, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '✨'
  },
  {
    id: '2026-vintage',
    name: '复古风情 2026',
    brand: '2026-vintage',
    color: '#E67E22',
    secondaryColor: '#D35400',
    bgColor: 'rgba(230, 126, 34, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 17, subtitle: 12, date: 10 },
    logo: '📷'
  },
  {
    id: '2026-neon',
    name: '霓虹炫彩 2026',
    brand: '2026-neon',
    color: '#E74C3C',
    secondaryColor: '#C0392B',
    bgColor: 'rgba(231, 76, 60, 0.1)',
    fontFamily: 'monospace',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🎆'
  },
  {
    id: '2026-classic',
    name: '经典永恒 2026',
    brand: '2026-classic',
    color: '#F1C40F',
    secondaryColor: '#F39C12',
    bgColor: 'rgba(241, 196, 15, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🏆'
  },
  {
    id: '2026-elegant',
    name: '优雅气质 2026',
    brand: '2026-elegant',
    color: '#D4AF37',
    secondaryColor: '#C5A028',
    bgColor: 'rgba(212, 175, 55, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 17, subtitle: 12, date: 11 },
    logo: '🎀'
  },
  {
    id: '2026-dynamic',
    name: '动感活力 2026',
    brand: '2026-dynamic',
    color: '#E74C3C',
    secondaryColor: '#C0392B',
    bgColor: 'rgba(231, 76, 60, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🎯'
  },
  {
    id: '2026-zen',
    name: '禅意生活 2026',
    brand: '2026-zen',
    color: '#95A5A6',
    secondaryColor: '#7F8C8D',
    bgColor: 'rgba(149, 165, 166, 0.06)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 16, subtitle: 12, date: 10 },
    logo: '☯'
  },
  {
    id: '2026-urban',
    name: '都市现代 2026',
    brand: '2026-urban',
    color: '#7F8C8D',
    secondaryColor: '#95A5A6',
    bgColor: 'rgba(127, 140, 141, 0.07)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 17, subtitle: 12, date: 11 },
    logo: '🏙️'
  },
  {
    id: '2026-forest',
    name: '森林秘境 2026',
    brand: '2026-forest',
    color: '#27AE60',
    secondaryColor: '#1E8449',
    bgColor: 'rgba(39, 174, 96, 0.08)',
    fontFamily: 'Georgia, serif',
    fontSize: { title: 17, subtitle: 12, date: 10 },
    logo: '🌲'
  },
  {
    id: '2026-sky',
    name: '云端漫步 2026',
    brand: '2026-sky',
    color: '#5DADE2',
    secondaryColor: '#3498DB',
    bgColor: 'rgba(93, 173, 226, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 17, subtitle: 12, date: 11 },
    logo: '☁️'
  },
  {
    id: '2026-fire',
    name: '烈焰激情 2026',
    brand: '2026-fire',
    color: '#E74C3C',
    secondaryColor: '#C0392B',
    bgColor: 'rgba(231, 76, 60, 0.1)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🔥'
  },
  {
    id: '2026-stone',
    name: '石材质感 2026',
    brand: '2026-stone',
    color: '#BDC3C7',
    secondaryColor: '#95A5A6',
    bgColor: 'rgba(189, 195, 199, 0.06)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 16, subtitle: 12, date: 10 },
    logo: '⬜'
  },
  {
    id: '2026-rainbow',
    name: '彩虹梦想 2026',
    brand: '2026-rainbow',
    color: '#E74C3C',
    secondaryColor: '#F1C40F',
    bgColor: 'rgba(231, 76, 60, 0.08)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: { title: 18, subtitle: 13, date: 11 },
    logo: '🌈'
  }
];

type WatermarkPosition = 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'center';

interface PositionConfig {
  x: number;
  y: number;
  label: string;
  icon: string;
}

const positionConfigs: Record<WatermarkPosition, PositionConfig> = {
  'top-left': { x: 20, y: 20, label: '左上', icon: '↖' },
  'top-right': { x: -300, y: 20, label: '右上', icon: '↗' },
  'bottom-left': { x: 20, y: -150, label: '左下', icon: '↙' },
  'bottom-right': { x: -300, y: -150, label: '右下', icon: '↘' },
  'center': { x: -150, y: -75, label: '居中', icon: '⊕' }
};

interface BatchImage {
  id: string;
  file: File;
  src: string;
  processed: boolean;
  error: boolean;
}

interface ExportProgress {
  current: number;
  total: number;
  status: 'idle' | 'exporting' | 'completed' | 'error';
}

// ==========================================
// 主组件
// ==========================================

export default function WatermarkPage() {
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState(watermarkTemplates[0]);
  const [deviceName, setDeviceName] = useState('Find X9 Pro');
  const [lensInfo, setLensInfo] = useState('24mm f/1.8');
  const [showWatermark, setShowWatermark] = useState(true);
  const [isDragOver, setIsDragOver] = useState(false);
  const [watermarkPosition, setWatermarkPosition] = useState<WatermarkPosition>('bottom-right');
  const [customOffset, setCustomOffset] = useState<{ x: number; y: number } | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
  const [watermarkScale, setWatermarkScale] = useState(1);
  const [watermarkRotation, setWatermarkRotation] = useState(0);
  const [opacity, setOpacity] = useState(1);
  const [batchImages, setBatchImages] = useState<BatchImage[]>([]);
  const [exportProgress, setExportProgress] = useState<ExportProgress>({ current: 0, total: 0, status: 'idle' });
  const [isExporting, setIsExporting] = useState(false);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [permissionStatus, setPermissionStatus] = useState<'idle' | 'granted' | 'denied'>('idle');

  const fileInputRef = useRef<HTMLInputElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const canvasContainerRef = useRef<HTMLDivElement>(null);
  const dragStartRef = useRef<{ x: number; y: number } | null>(null);

  // ==========================================
  // 通知系统
  // ==========================================

  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
    setNotification({ message, type });
  };

  // ==========================================
  // 图片选择与加载
  // ==========================================

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length > 1) {
      handleBatchSelect(files);
    } else if (files.length === 1) {
      loadSingleImage(files[0]);
    }
  };

  const loadSingleImage = (file: File) => {
    const reader = new FileReader();
    reader.onload = (event) => {
      setSelectedImage(event.target?.result as string);
      setBatchImages([]);
      showNotification('图片已加载', 'success');
    };
    reader.readAsDataURL(file);
  };

  const handleBatchSelect = (files: File[]) => {
    const imageFiles = files.filter(f => f.type.startsWith('image/'));
    const newImages: BatchImage[] = [];

    imageFiles.forEach((file, index) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        newImages.push({
          id: `img-${Date.now()}-${index}`,
          file,
          src: event.target?.result as string,
          processed: false,
          error: false
        });

        if (newImages.length === imageFiles.length) {
          setBatchImages(newImages);
          setSelectedImage(null);
          showNotification(`已选择 ${imageFiles.length} 张图片`, 'info');
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files || []);
    if (files.length > 1) {
      handleBatchSelect(files);
    } else if (files.length === 1 && files[0].type.startsWith('image/')) {
      loadSingleImage(files[0]);
    }
  };

  // ==========================================
  // 水印位置计算
  // ==========================================

  const getWatermarkPosition = (width: number, height: number) => {
    if (customOffset) {
      return customOffset;
    }
    const config = positionConfigs[watermarkPosition];
    let x = config.x >= 0 ? config.x : width + config.x;
    let y = config.y >= 0 ? config.y : height + config.y;
    return { x, y };
  };

  // ==========================================
  // Canvas渲染
  // ==========================================

  useEffect(() => {
    if (selectedImage && canvasRef.current) {
      renderWatermark(selectedImage);
    }
  }, [selectedImage, selectedTemplate, showWatermark, deviceName, lensInfo, watermarkPosition, customOffset, watermarkScale, watermarkRotation, opacity]);

  const renderWatermark = async (imageSrc: string, targetCanvas?: HTMLCanvasElement) => {
    const canvas = targetCanvas || canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const img = new window.Image();
    img.src = imageSrc;
    
    await new Promise((resolve) => {
      img.onload = resolve;
    });

    const maxWidth = 1200;
    const maxHeight = 900;
    let width = img.width;
    let height = img.height;
    
    if (width > maxWidth) {
      height = (maxWidth / width) * height;
      width = maxWidth;
    }
    if (height > maxHeight) {
      width = (maxHeight / height) * width;
      height = maxHeight;
    }

    canvas.width = width;
    canvas.height = height;

    ctx.drawImage(img, 0, 0, width, height);

    if (showWatermark) {
      drawWatermark(ctx, width, height);
    }
  };

  const drawWatermark = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
    const currentDate = new Date().toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });

    const pos = getWatermarkPosition(width, height);
    const wmWidth = 280 * watermarkScale;
    const wmHeight = 140 * watermarkScale;
    
    let x = Math.max(10, Math.min(pos.x, width - wmWidth - 10));
    let y = Math.max(10, Math.min(pos.y, height - wmHeight - 10));

    ctx.save();
    ctx.globalAlpha = opacity;
    
    const centerX = x + wmWidth / 2;
    const centerY = y + wmHeight / 2;
    ctx.translate(centerX, centerY);
    ctx.rotate((watermarkRotation * Math.PI) / 180);
    ctx.translate(-centerX, -centerY);

    ctx.fillStyle = selectedTemplate.bgColor;
    ctx.fillRect(x, y, wmWidth, wmHeight);

    ctx.strokeStyle = selectedTemplate.color;
    ctx.lineWidth = 2 * watermarkScale;
    ctx.strokeRect(x, y, wmWidth, wmHeight);

    ctx.fillStyle = selectedTemplate.color;
    ctx.font = `bold ${selectedTemplate.fontSize.title * watermarkScale}px ${selectedTemplate.fontFamily}`;
    ctx.fillText(deviceName, x + 20 * watermarkScale, y + 40 * watermarkScale);

    ctx.font = `${selectedTemplate.fontSize.subtitle * watermarkScale}px ${selectedTemplate.fontFamily}`;
    ctx.fillStyle = 'rgba(255,255,255,0.85)';
    ctx.fillText(lensInfo, x + 20 * watermarkScale, y + 65 * watermarkScale);

    ctx.beginPath();
    ctx.moveTo(x + 20 * watermarkScale, y + 80 * watermarkScale);
    ctx.lineTo(x + wmWidth - 20 * watermarkScale, y + 80 * watermarkScale);
    ctx.strokeStyle = selectedTemplate.secondaryColor;
    ctx.lineWidth = 1 * watermarkScale;
    ctx.stroke();

    ctx.font = `${selectedTemplate.fontSize.date * watermarkScale}px ${selectedTemplate.fontFamily}`;
    ctx.fillStyle = 'rgba(255,255,255,0.7)';
    ctx.fillText(currentDate, x + 20 * watermarkScale, y + 100 * watermarkScale);

    ctx.font = `bold ${16 * watermarkScale}px ${selectedTemplate.fontFamily}`;
    ctx.fillStyle = selectedTemplate.color;
    ctx.fillText(selectedTemplate.logo, x + wmWidth - 40 * watermarkScale, y + wmHeight - 20 * watermarkScale);

    ctx.restore();
  };

  // ==========================================
  // 拖拽控制
  // ==========================================

  const handleCanvasMouseDown = (e: React.MouseEvent | React.TouchEvent) => {
    if (!canvasRef.current || !showWatermark) return;
    
    setIsDragging(true);
    const rect = canvasRef.current.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    
    dragStartRef.current = {
      x: clientX - rect.left,
      y: clientY - rect.top
    };
  };

  const handleCanvasMouseMove = (e: React.MouseEvent | React.TouchEvent) => {
    if (!isDragging || !canvasRef.current || !dragStartRef.current) return;
    
    const rect = canvasRef.current.getBoundingClientRect();
    const scaleX = canvasRef.current.width / rect.width;
    const scaleY = canvasRef.current.height / rect.height;
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    
    const x = (clientX - rect.left) * scaleX;
    const y = (clientY - rect.top) * scaleY;
    
    setCustomOffset({
      x: x - 140 * watermarkScale,
      y: y - 70 * watermarkScale
    });
  };

  const handleCanvasMouseUp = () => {
    if (isDragging) {
      setIsDragging(false);
      dragStartRef.current = null;
      showNotification('水印位置已调整', 'success');
    }
  };

  // ==========================================
  // 导出功能
  // ==========================================

  const handleRequestPermission = () => {
    setShowPermissionModal(true);
  };

  const handlePermissionGrant = () => {
    setPermissionStatus('granted');
    setShowPermissionModal(false);
    showNotification('权限已授予', 'success');
  };

  const handlePermissionDeny = () => {
    setPermissionStatus('denied');
    setShowPermissionModal(false);
    showNotification('权限已拒绝，将使用浏览器默认下载', 'info');
  };

  const handleSingleDownload = useCallback(async () => {
    if (!canvasRef.current) return;
    
    if (permissionStatus === 'idle') {
      handleRequestPermission();
      return;
    }

    try {
      const link = document.createElement('a');
      link.download = `watermarked_${Date.now()}.png`;
      link.href = canvasRef.current.toDataURL('image/png', 1.0);
      link.click();
      showNotification('图片已下载（无损画质）', 'success');
    } catch (error) {
      showNotification('下载失败', 'error');
    }
  }, [permissionStatus]);

  const handleBatchExport = async () => {
    if (batchImages.length === 0) return;
    
    if (permissionStatus === 'idle') {
      handleRequestPermission();
      return;
    }

    setIsExporting(true);
    setExportProgress({ current: 0, total: batchImages.length, status: 'exporting' });

    let successCount = 0;
    let errorCount = 0;

    for (let i = 0; i < batchImages.length; i++) {
      try {
        const tempCanvas = document.createElement('canvas');
        await renderWatermark(batchImages[i].src, tempCanvas);
        
        const link = document.createElement('a');
        const timestamp = Date.now();
        link.download = `watermarked_${String(i + 1).padStart(3, '0')}_${timestamp}.png`;
        link.href = tempCanvas.toDataURL('image/png', 1.0);
        link.click();

        setBatchImages(prev => prev.map((img, idx) => 
          idx === i ? { ...img, processed: true } : img
        ));

        successCount++;
        setExportProgress(prev => ({ ...prev, current: i + 1 }));
        
        await new Promise(resolve => setTimeout(resolve, 300));
        
      } catch (error) {
        errorCount++;
        setBatchImages(prev => prev.map((img, idx) => 
          idx === i ? { ...img, error: true } : img
        ));
      }
    }

    setExportProgress(prev => ({ ...prev, status: 'completed' }));
    setIsExporting(false);
    
    if (errorCount === 0) {
      showNotification(`已成功导出 ${successCount} 张图片（水印位置：${watermarkPosition}）`, 'success');
    } else {
      showNotification(`导出完成：${successCount} 张成功，${errorCount} 张失败`, 'error');
    }
    
    setTimeout(() => {
      setExportProgress({ current: 0, total: 0, status: 'idle' });
    }, 3000);
  };

  const handlePositionChange = (position: WatermarkPosition) => {
    setWatermarkPosition(position);
    setCustomOffset(null);
    showNotification(`水印位置: ${positionConfigs[position].label}`, 'success');
  };

  const resetPosition = () => {
    setWatermarkPosition('bottom-right');
    setCustomOffset(null);
    setWatermarkScale(1);
    setWatermarkRotation(0);
    setOpacity(1);
    showNotification('水印样式已重置', 'success');
  };

  // ==========================================
  // 渲染
  // ==========================================

  return (
    <div className="min-h-screen pt-20 pb-24 px-4 sm:px-6 lg:px-8 bg-[#0F0F0F]">
      {/* 顶部通知 */}
      <AnimatePresence>
        {notification && (
          <motion.div
            initial={{ opacity: 0, y: -20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            transition={{ duration: 0.3, ease: easeOppoEnter }}
            className={`fixed top-24 left-1/2 -translate-x-1/2 z-50 px-6 py-3 rounded-2xl shadow-2xl backdrop-blur-xl border ${
              notification.type === 'success' 
                ? 'bg-oppo-green/20 border-oppo-green/30 text-oppo-green' 
                : notification.type === 'error'
                ? 'bg-red-500/20 border-red-500/30 text-red-400'
                : 'bg-oppo-blue/20 border-oppo-blue/30 text-oppo-blue'
            }`}
          >
            <div className="flex items-center gap-2">
              {notification.type === 'success' ? (
                <CheckCircle2 className="w-5 h-5" />
              ) : notification.type === 'error' ? (
                <X className="w-5 h-5" />
              ) : (
                <AlertCircle className="w-5 h-5" />
              )}
              <span className="font-medium text-base">{notification.message}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* 权限请求弹窗 */}
      <AnimatePresence>
        {showPermissionModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowPermissionModal(false)}
          >
            <motion.div
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.9, y: 20 }}
              transition={{ duration: 0.3, ease: easeOppoBounce }}
              onClick={(e) => e.stopPropagation()}
              className="bg-[#1A1A1A] rounded-3xl p-6 max-w-sm w-full border border-white/10"
            >
              <h3 className="text-xl font-bold mb-2">需要存储权限</h3>
              <p className="text-text-secondary mb-6">为了保存处理后的图片，需要请求存储权限。</p>
              <div className="flex gap-3">
                <ColorOSButton 
                  variant="secondary" 
                  onClick={handlePermissionDeny}
                  className="flex-1"
                >
                  拒绝
                </ColorOSButton>
                <ColorOSButton 
                  variant="primary" 
                  onClick={handlePermissionGrant}
                  className="flex-1"
                >
                  允许
                </ColorOSButton>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="max-w-7xl mx-auto">
        {/* 标题区域 */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="text-center mb-10"
        >
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', stiffness: 200, damping: 15, delay: 0.1 }}
            className="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-oppo-green to-oppo-blue rounded-2xl mb-6 shadow-2xl"
          >
            <Palette className="w-12 h-12 text-oppo-black" />
          </motion.div>
          <h1 className="text-[32px] font-bold mb-4 bg-gradient-to-r from-oppo-green via-oppo-blue to-oppo-purple bg-clip-text text-transparent">
            水印生成器
          </h1>
          <p className="text-base text-text-secondary max-w-2xl mx-auto">
            为您的照片添加专业水印 - 支持12+品牌风格，拖拽定位水印，批量导出
          </p>
        </motion.div>

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          {/* 左侧：图片预览区域 */}
          <div className="xl:col-span-2 space-y-6">
            {/* 上传区域 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="上传图片" 
              />
              
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleImageSelect}
                accept="image/*"
                multiple
                className="hidden"
              />
              
              <motion.div
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={handleDrop}
                whileHover={{ scale: 1.01 }}
                whileTap={{ scale: 0.99 }}
                className={`border-2 border-dashed rounded-2xl p-8 text-center transition-all cursor-pointer ${
                  isDragOver 
                    ? 'border-oppo-green bg-oppo-green/10' 
                    : 'border-white/20 hover:border-oppo-orange/50 hover:bg-white/5'
                }`}
              >
                <motion.div
                  animate={{ y: isDragging ? [0, -10, 0] : 0 }}
                  transition={{ repeat: isDragging ? Infinity : 0, duration: 1 }}
                >
                  <ImageIcon className="w-12 h-12 mx-auto mb-3 text-text-tertiary" />
                </motion.div>
                <p className="text-text-secondary text-base">点击上传或拖拽图片</p>
                <p className="text-xs text-text-tertiary mt-1">支持 JPG、PNG、WebP 格式 · 支持批量上传</p>
              </motion.div>
            </ColorOSCard>

            {/* 批量图片预览 */}
            {batchImages.length > 0 && (
              <ColorOSCard className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <ColorOSSectionHeader 
                    title={`批量处理 (${batchImages.length})`} 
                  />
                  {exportProgress.status === 'exporting' && (
                    <div className="flex items-center gap-2">
                      <div className="w-32 h-2 bg-white/10 rounded-full overflow-hidden">
                        <motion.div
                          className="h-full bg-oppo-green"
                          initial={{ width: 0 }}
                          animate={{ width: `${(exportProgress.current / exportProgress.total) * 100}%` }}
                        />
                      </div>
                      <span className="text-sm text-text-secondary">
                        {exportProgress.current}/{exportProgress.total}
                      </span>
                    </div>
                  )}
                </div>
                
                <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-3">
                  {batchImages.map((img, idx) => (
                    <motion.div
                      key={img.id}
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: idx * 0.03 }}
                      className="relative aspect-square rounded-xl overflow-hidden bg-bg-tertiary"
                    >
                      <img 
                        src={img.src} 
                        alt="" 
                        className="w-full h-full object-cover"
                      />
                      {img.processed && (
                        <div className="absolute inset-0 bg-oppo-green/30 flex items-center justify-center">
                          <Check className="w-6 h-6 text-white" />
                        </div>
                      )}
                      {img.error && (
                        <div className="absolute inset-0 bg-red-500/30 flex items-center justify-center">
                          <X className="w-6 h-6 text-white" />
                        </div>
                      )}
                    </motion.div>
                  ))}
                </div>

                {/* 批量导出进度条 */}
                {isExporting && exportProgress.status === 'exporting' && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="mt-4 p-4 bg-bg-secondary rounded-2xl"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm font-medium">正在导出...</span>
                      <span className="text-sm text-oppo-orange font-bold">
                        {exportProgress.current} / {exportProgress.total}
                      </span>
                    </div>
                    <div className="w-full h-2 bg-white/10 rounded-full overflow-hidden">
                      <motion.div
                        className="h-full bg-gradient-to-r from-oppo-orange to-hasselblad-orange"
                        initial={{ width: '0%' }}
                        animate={{ width: `${(exportProgress.current / exportProgress.total) * 100}%` }}
                        transition={{ duration: 0.3, ease: 'easeOut' }}
                      />
                    </div>
                    <p className="text-xs text-white/50 mt-2 text-center">
                      请勿关闭页面，图片将逐一下载
                    </p>
                  </motion.div>
                )}

                <div className="mt-4 flex gap-3">
                  <ColorOSButton
                    variant="primary"
                    onClick={handleBatchExport}
                    loading={isExporting}
                    className="flex-1"
                    icon={<Download className="w-5 h-5" />}
                    disabled={batchImages.length === 0}
                  >
                    {isExporting ? '导出中...' : `批量导出 ${batchImages.length > 0 ? `(${batchImages.length})` : ''}`}
                  </ColorOSButton>
                  <ColorOSButton
                    variant="secondary"
                    onClick={() => setBatchImages([])}
                    disabled={isExporting}
                  >
                    清除
                  </ColorOSButton>
                </div>
              </ColorOSCard>
            )}

            {/* 单图预览 */}
            {selectedImage && (
              <ColorOSCard className="p-6">
                <ColorOSSectionHeader 
                  title="预览效果" 
                  subtitle="支持拖拽定位水印"
                />
                <div 
                  ref={canvasContainerRef}
                  className="relative bg-bg-secondary rounded-2xl overflow-hidden"
                >
                  <motion.div
                    onMouseDown={handleCanvasMouseDown}
                    onMouseMove={handleCanvasMouseMove}
                    onMouseUp={handleCanvasMouseUp}
                    onMouseLeave={handleCanvasMouseUp}
                    onTouchStart={handleCanvasMouseDown}
                    onTouchMove={handleCanvasMouseMove}
                    onTouchEnd={handleCanvasMouseUp}
                    className={`relative ${showWatermark ? 'cursor-move' : 'cursor-default'}`}
                    whileHover={{ scale: isDragging ? 1 : 1.01 }}
                    whileTap={{ scale: 0.99 }}
                  >
                    <canvas
                      ref={canvasRef}
                      className="w-full h-auto rounded-xl"
                    />
                    {isDragging && (
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="absolute inset-0 border-2 border-dashed border-oppo-orange pointer-events-none rounded-xl"
                      />
                    )}
                  </motion.div>
                </div>
                
                <div className="mt-4 space-y-3">
                  <ColorOSButton
                    variant="primary"
                    onClick={handleSingleDownload}
                    fullWidth
                    icon={<Download className="w-5 h-5" />}
                  >
                    下载带水印图片（无损）
                  </ColorOSButton>
                  {customOffset && (
                    <ColorOSButton
                      variant="secondary"
                      onClick={resetPosition}
                      fullWidth
                    >
                      重置水印样式
                    </ColorOSButton>
                  )}
                </div>
              </ColorOSCard>
            )}
          </div>

          {/* 右侧：控制面板 */}
          <div className="space-y-6">
            {/* 水印模板选择 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="选择水印模板" 
              />
              
              <div className="grid grid-cols-2 gap-3 max-h-[300px] overflow-y-auto pr-1">
                {watermarkTemplates.map((template, index) => (
                  <motion.button
                    key={template.id}
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: index * 0.03 }}
                    whileHover={{ scale: 1.05, y: -2 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      setSelectedTemplate(template);
                      showNotification(`已切换到${template.name}`, 'success');
                    }}
                    className={`p-4 rounded-2xl border-2 transition-all ${
                      selectedTemplate.id === template.id
                        ? 'border-oppo-orange bg-oppo-orange/10'
                        : 'border-white/10 hover:border-white/30 hover:bg-white/5'
                    }`}
                  >
                    <div 
                      className="w-full h-14 rounded-xl mb-2 flex items-center justify-center text-white font-bold text-sm"
                      style={{ backgroundColor: template.bgColor }}
                    >
                      <span style={{ color: template.color }}>{template.logo}</span>
                    </div>
                    <p className="text-sm font-medium truncate">{template.name}</p>
                  </motion.button>
                ))}
              </div>
            </ColorOSCard>

            {/* 水印位置 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="水印位置" 
              />
              
              <div className="grid grid-cols-5 gap-2 mb-4">
                {(Object.keys(positionConfigs) as WatermarkPosition[]).map((position) => (
                  <motion.button
                    key={position}
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    onClick={() => handlePositionChange(position)}
                    className={`p-3 rounded-2xl border-2 transition-all text-center ${
                      (watermarkPosition === position && !customOffset)
                        ? 'border-oppo-orange bg-oppo-orange/10'
                        : 'border-white/10 hover:border-white/30'
                    }`}
                    title={positionConfigs[position].label}
                  >
                    <span className="text-2xl">{positionConfigs[position].icon}</span>
                    <p className="text-xs mt-1 opacity-70">{positionConfigs[position].label}</p>
                  </motion.button>
                ))}
              </div>

              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => {
                  setCustomOffset(null);
                  setWatermarkPosition('bottom-right');
                  showNotification('已切换为拖拽模式', 'success');
                }}
                className="w-full p-3 rounded-2xl border-2 border-dashed border-white/20 hover:border-oppo-orange/50 transition-colors flex items-center justify-center gap-2"
              >
                <Move className="w-5 h-5" />
                <span className="text-sm">拖拽模式 - 在预览区自由调整</span>
              </motion.button>
            </ColorOSCard>

            {/* 水印参数调整 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="水印参数" 
              />
              
              <div className="space-y-5">
                {/* 缩放 */}
                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium text-text-secondary">
                      缩放大小
                    </label>
                    <span className="text-sm text-oppo-orange">{Math.round(watermarkScale * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min="0.5"
                    max="2"
                    step="0.1"
                    value={watermarkScale}
                    onChange={(e) => setWatermarkScale(parseFloat(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-oppo-orange"
                  />
                </div>

                {/* 旋转 */}
                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium text-text-secondary flex items-center gap-2">
                      <RotateCw className="w-4 h-4" />
                      旋转角度
                    </label>
                    <span className="text-sm text-oppo-orange">{watermarkRotation}°</span>
                  </div>
                  <input
                    type="range"
                    min="-180"
                    max="180"
                    step="1"
                    value={watermarkRotation}
                    onChange={(e) => setWatermarkRotation(parseInt(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-oppo-orange"
                  />
                </div>

                {/* 透明度 */}
                <div>
                  <div className="flex justify-between mb-2">
                    <label className="text-sm font-medium text-text-secondary">
                      透明度
                    </label>
                    <span className="text-sm text-oppo-orange">{Math.round(opacity * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min="0.1"
                    max="1"
                    step="0.05"
                    value={opacity}
                    onChange={(e) => setOpacity(parseFloat(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-oppo-orange"
                  />
                </div>
              </div>
            </ColorOSCard>

            {/* 自定义信息 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="自定义信息" 
              />
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    设备名称
                  </label>
                  <motion.input
                    type="text"
                    value={deviceName}
                    onChange={(e) => setDeviceName(e.target.value)}
                    placeholder="例如：Find X9 Pro"
                    whileFocus={{ scale: 1.01 }}
                    className="w-full px-4 py-3 bg-[#1A1A1A] border border-white/10 rounded-2xl text-text-primary placeholder-text-tertiary focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20 transition-all"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-text-secondary mb-2">
                    镜头参数
                  </label>
                  <motion.input
                    type="text"
                    value={lensInfo}
                    onChange={(e) => setLensInfo(e.target.value)}
                    placeholder="例如：24mm f/1.8"
                    whileFocus={{ scale: 1.01 }}
                    className="w-full px-4 py-3 bg-[#1A1A1A] border border-white/10 rounded-2xl text-text-primary placeholder-text-tertiary focus:outline-none focus:border-oppo-orange focus:ring-2 focus:ring-oppo-orange/20 transition-all"
                  />
                </div>

                <div className="flex items-center justify-between pt-2">
                  <span className="text-sm font-medium text-text-secondary">显示水印</span>
                  <motion.button
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      setShowWatermark(!showWatermark);
                      showNotification(showWatermark ? '水印已隐藏' : '水印已显示', 'success');
                    }}
                    className={`relative w-14 h-7 rounded-full transition-colors ${
                      showWatermark ? 'bg-oppo-orange' : 'bg-white/20'
                    }`}
                  >
                    <motion.div
                      animate={{ x: showWatermark ? 30 : 3 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                      className="absolute top-1 w-5 h-5 bg-white rounded-full shadow-md"
                    />
                  </motion.button>
                </div>
              </div>
            </ColorOSCard>

            {/* 快速预设 */}
            <ColorOSCard className="p-6">
              <ColorOSSectionHeader 
                title="快速预设" 
              />
              <div className="grid grid-cols-2 gap-3">
                {[
                  { name: 'OPPO Find', device: 'Find X9 Pro', lens: '24mm f/1.8', template: watermarkTemplates[1] },
                  { name: 'OnePlus', device: 'OnePlus 13', lens: '23mm f/1.8', template: watermarkTemplates[2] },
                  { name: '哈苏', device: 'X2D 100c', lens: '35mm f/2', template: watermarkTemplates[0] },
                  { name: 'iPhone', device: 'iPhone 16 Pro', lens: '24mm f/1.78', template: watermarkTemplates[6] },
                ].map((preset, idx) => (
                  <motion.button
                    key={idx}
                    whileHover={{ scale: 1.05, y: -2 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => {
                      setDeviceName(preset.device);
                      setLensInfo(preset.lens);
                      setSelectedTemplate(preset.template);
                      showNotification(`已切换到${preset.name}预设`, 'success');
                    }}
                    className="px-4 py-3 bg-[#1A1A1A] border border-white/10 rounded-2xl hover:border-oppo-orange/30 transition-all text-sm font-medium"
                  >
                    {preset.name}
                  </motion.button>
                ))}
              </div>
            </ColorOSCard>

            {/* 使用提示 */}
            <ColorOSCard className="p-6 bg-gradient-to-br from-oppo-green/10 to-transparent">
              <ColorOSSectionHeader 
                title="使用提示" 
              />
              <ul className="space-y-2 text-sm text-text-secondary">
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>支持拖拽或点击上传多张图片</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>在预览区拖拽水印可自由调整位置</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>支持缩放、旋转、透明度调节</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>PNG无损画质导出，无压缩模糊</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle2 className="w-4 h-4 text-oppo-green mt-0.5 flex-shrink-0" />
                  <span>12+品牌风格快速切换</span>
                </li>
              </ul>
            </ColorOSCard>
          </div>
        </div>
      </div>
    </div>
  );
}
