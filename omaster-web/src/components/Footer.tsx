import { Link } from "react-router-dom";
import { Camera, Github, Twitter, Instagram, Heart } from "lucide-react";

export default function Footer() {
  return (
    <footer className="border-t border-white/[0.06] bg-ink-900">
      <div className="max-w-7xl mx-auto px-6 py-16">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-10 mb-12">
          <div className="col-span-2">
            <Link to="/" className="flex items-center gap-2.5 mb-4">
              <div className="w-9 h-9 rounded-xl bg-hasselblad-500 flex items-center justify-center">
                <Camera className="w-5 h-5 text-ink-900" strokeWidth={2.2} />
              </div>
              <div className="flex flex-col leading-none">
                <span className="font-display text-lg font-bold text-ink-50">OMaster</span>
                <span className="text-[10px] text-ink-300 tracking-[0.18em] uppercase">Hasselblad × OPPO</span>
              </div>
            </Link>
            <p className="text-ink-300 text-sm leading-relaxed max-w-sm">
              让每一台 OPPO/一加/真我 设备，都拥有哈苏的色彩科学。
              探索影像的边界，捕捉每一个决定性瞬间。
            </p>
            <div className="flex items-center gap-3 mt-5">
              {[Github, Twitter, Instagram].map((Icon, i) => (
                <a
                  key={i}
                  href="#"
                  className="w-9 h-9 rounded-full border border-white/10 flex items-center justify-center text-ink-300 hover:border-hasselblad-500 hover:text-hasselblad-400 transition-colors"
                >
                  <Icon className="w-4 h-4" />
                </a>
              ))}
            </div>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold text-ink-100 mb-4">产品</h4>
            <ul className="space-y-2.5 text-sm text-ink-300">
              <li><Link to="/presets" className="hover:text-hasselblad-400 transition-colors">预设库</Link></li>
              <li><Link to="/scene-detection" className="hover:text-hasselblad-400 transition-colors">AI 场景检测</Link></li>
              <li><Link to="/watermark" className="hover:text-hasselblad-400 transition-colors">水印编辑</Link></li>
              <li><Link to="/camera-config" className="hover:text-hasselblad-400 transition-colors">相机配置</Link></li>
            </ul>
          </div>

          <div>
            <h4 className="font-display text-sm font-bold text-ink-100 mb-4">资源</h4>
            <ul className="space-y-2.5 text-sm text-ink-300">
              <li><a href="#" className="hover:text-hasselblad-400 transition-colors">使用文档</a></li>
              <li><a href="#" className="hover:text-hasselblad-400 transition-colors">社区</a></li>
              <li><a href="#" className="hover:text-hasselblad-400 transition-colors">更新日志</a></li>
              <li><a href="#" className="hover:text-hasselblad-400 transition-colors">联系我们</a></li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-white/[0.06] flex flex-col md:flex-row items-center justify-between gap-3 text-xs text-ink-400">
          <p>© 2026 OMaster. 与哈苏影像实验室合作开发.</p>
          <p className="inline-flex items-center gap-1.5">
            Made with <Heart className="w-3 h-3 text-hasselblad-500 fill-hasselblad-500" /> for photographers
          </p>
        </div>
      </div>
    </footer>
  );
}
