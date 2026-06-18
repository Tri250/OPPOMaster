import './style.css';

// ========================================
// Translations
// ========================================
const translations = {
  zh: {
    'meta.title': 'Alcedo Studio — AI 驱动的开源专业级 RAW 图像处理软件',
    'meta.desc': 'Alcedo Studio 是一款 AI 驱动、开源免费的 RAW 图像处理软件，支持 Windows/CUDA 和 macOS/Metal，提供专业级色彩科学、AI 内容识别与自然语言检索及高性能处理引擎。',
    'nav.ai': 'AI 智能',
    'nav.features': '功能特性',
    'nav.films': '胶片模拟',
    'nav.download': '下载',
    'hero.badge1': 'AI 驱动 · 开源免费',
    'hero.badge2': 'Windows / macOS',
    'hero.subtitle': '<span class="text-gold">AI 驱动</span>的专业级 RAW 图像处理，不该被价格束缚',
    'hero.desc': 'AI 驱动、开源免费的 RAW 图像处理软件，支持 Windows/CUDA 与 macOS/Metal 双平台。AI 内容识别、自然语言检索、丰富的胶片模拟与调整工具、高性能处理引擎、强大的影像管理——为摄影师与创作者而生。',
    'hero.download': '免费下载',
    'ai.label': 'AI POWERED',
    'ai.title': '让 AI 成为你的<br/><span class="text-gradient">影像助理</span>',
    'ai.desc': '从内容识别到自然语言检索，Alcedo Studio 将 AI 深度融入影像管理流程，让你用最自然的方式找到、筛选与组织照片。',
    'ai.f1.title': 'AI 内容识别',
    'ai.f1.desc': '导入即自动分析画面内容，为每张照片生成语义标签——场景、主体、风格、情绪一应俱全。无需手动关键字，海量图库也能被精准理解与组织。',
    'ai.f1.tag1': '自动语义标注',
    'ai.f1.tag2': '场景 / 主体识别',
    'ai.f1.tag3': '导入即生成',
    'ai.f2.title': 'AI 内容过滤',
    'ai.f2.desc': '基于语义标签的智能筛选器，一键过滤“人像”“日落”“街拍”等内容主题。从成千上万张照片中秒速定位目标，让筛选从体力活变成一键操作。',
    'ai.f2.tag1': '语义主题筛选',
    'ai.f2.tag2': '一键过滤',
    'ai.f2.tag3': '与 EXIF 叠加',
    'ai.f3.title': 'AI 自然语言搜索',
    'ai.f3.desc': '用一句话描述你要找的照片——“海边日落的人像”“雨夜街头的霓虹”——AI 理解你的意图并跨整个图库精准匹配。找图，从未如此直观。',
    'ai.f3.tag1': '自然语言理解',
    'ai.f3.tag2': '语义向量检索',
    'ai.f3.tag3': '全库即时匹配',
    'features.label': 'FEATURES',
    'features.title': '为创作而生的<br/><span class="text-gradient">强大工具集</span>',
    'features.desc': '从色彩科学到几何校正，从基础调整到智能管理——每一步都经过精心打磨',
    'features.f1.title': '极速图像管理',
    'features.f1.desc': '响应迅速、资源占用极低的图像管理系统。支持市面上绝大多数 RAW 格式，浏览、筛选、评级一气呵成，让海量照片整理不再头疼。',
    'features.f1.tag1': '多格式 RAW 支持',
    'features.f1.tag2': '低资源占用',
    'features.f1.tag3': '快速预览',
    'features.f2.title': '双引擎色彩科学',
    'features.f2.desc': 'ACES 与 OpenDRT 双色彩渲染管线，无论是追求真实还原还是艺术风格化，都能找到最适合场景的色彩倾向与影调表达。',
    'features.f2.tag1': 'ACES 色彩管线',
    'features.f2.tag2': 'OpenDRT 渲染',
    'features.f2.tag3': '场景化影调',
    'features.f3.title': '亿级像素，实时调整',
    'features.f3.desc': '曝光、对比度、白平衡、色调曲线……所有基础调整工具性能均经过深度优化。即便面对 <strong>1.5 亿像素</strong> 的巨无霸 RAW 文件，拖动滑块依然丝般顺滑，所见即所得。',
    'features.f3.tag1': '1.5 亿像素实时处理',
    'features.f3.tag2': 'CUDA / Metal 加速',
    'features.f3.tag3': '零延迟反馈',
    'features.f4.title': '高级色彩控制',
    'features.f4.desc': '色轮、HSL 分离调整、通道混合……将每种颜色的色相、饱和度、明度都置于你的指尖之下。想让人像肤色更暖、天空更通透？只需几秒钟。',
    'features.f4.tag1': '色轮调色',
    'features.f4.tag2': 'HSL 精细分离',
    'features.f4.tag3': '通道混合器',
    'features.f5.title': '几何与构图修正',
    'features.f5.desc': '镜头畸变校正、透视变形修复、自由裁剪，配合丰富的画面比例预设（1:1、4:5、16:9、2.39:1 等），助你轻松修正构图瑕疵，打造完美画面。',
    'features.f5.tag1': '镜头畸变校正',
    'features.f5.tag2': '透视变形修复',
    'features.f5.tag3': '多比例裁剪预设',
    'features.f6.title': '专业级导出',
    'features.f6.desc': '从 HDR 高动态范围到标准 JPEG，支持嵌入 ICC 色彩配置文件与 HDR 导出模式。无论你的作品最终流向网络、印刷还是影视流程，色彩都能精准呈现。',
    'features.f6.tag1': 'HDR 导出',
    'features.f6.tag2': 'ICC 色彩配置嵌入',
    'features.f6.tag3': '多格式输出',
    'films.label': 'FILM SIMULATION',
    'films.title': '经典胶片风格<br/><span class="text-gradient">一键直出</span>',
    'films.desc': '基于真实胶片特性数字复刻的 LUT 预设，搭配胶片颗粒与 Halation 光晕模拟，让数字影像拥有胶片般的质感与灵魂',
    'films.showcase.tag': '人像 · 细腻',
    'films.showcase.desc': '人像摄影标杆，肤色还原极其细腻自然，影调柔和优雅。400 度的高感光度让它在多变光线下游刃有余，是日常与人像创作的可靠之选。',
    'films.grain.title': '胶片颗粒与 Halation 光晕模拟',
    'films.grain.desc': '不止于色彩还原。Alcedo Studio 复刻真实胶片的物理特性：可调颗粒大小与强度的胶片颗粒、高光溢出形成的 Halation 光晕、以及边缘柔化等暗房效果。从干净数字到粗粝胶片，质感尽在掌控。',
    'films.grain.tag1': '可调胶片颗粒',
    'films.grain.tag2': 'Halation 光晕',
    'films.grain.tag3': '暗房质感还原',
    'films.stocks.label': 'SUPPORTED FILM STOCKS',
    'cta.version': '最新版本',
    'cta.title': '准备好开始创作了吗？',
    'cta.desc': 'Alcedo Studio 完全开源免费，无需订阅，没有功能限制。下载即可使用全部专业功能，包括 AI 内容识别与自然语言检索。',
    'cta.win': 'Windows 版下载',
    'cta.mac': 'macOS 版下载',
    'cta.baidu': '网盘分流',
    'cta.note': '支持 CUDA (NVIDIA) 与 Metal (Apple Silicon) GPU 加速',
    'footer.tagline': 'AI 驱动，自由创作，从 Alcedo 开始',
    'footer.copy': '© 2026 Alcedo Studio. 开源软件，自由使用。'
  },
  en: {
    'meta.title': 'Alcedo Studio — AI-Powered Open-Source Professional RAW Image Processor',
    'meta.desc': 'Alcedo Studio is an AI-powered, free and open-source RAW image processor supporting Windows/CUDA and macOS/Metal, with professional color science, AI content recognition, natural-language search, and a high-performance engine.',
    'nav.ai': 'AI',
    'nav.features': 'Features',
    'nav.films': 'Film',
    'nav.download': 'Download',
    'hero.badge1': 'AI-Powered · Open Source',
    'hero.badge2': 'Windows / macOS',
    'hero.subtitle': '<span class="text-gold">AI-powered</span> professional RAW processing should not be held back by price',
    'hero.desc': 'An AI-powered, free and open-source RAW processor for both Windows/CUDA and macOS/Metal. AI content recognition, natural-language search, rich film simulations, a high-performance engine, and powerful asset management — built for photographers and creators.',
    'hero.download': 'Free Download',
    'ai.label': 'AI POWERED',
    'ai.title': 'Let AI Be Your<br/><span class="text-gradient">Photo Assistant</span>',
    'ai.desc': 'From content recognition to natural-language search, Alcedo Studio weaves AI deep into the asset workflow — so you find, filter, and organize photos the natural way.',
    'ai.f1.title': 'AI Content Recognition',
    'ai.f1.desc': 'Imports are analyzed automatically, generating semantic tags for every photo — scene, subject, style, and mood. No manual keywords needed; even massive libraries are precisely understood and organized.',
    'ai.f1.tag1': 'Auto semantic tagging',
    'ai.f1.tag2': 'Scene / subject detection',
    'ai.f1.tag3': 'Generated on import',
    'ai.f2.title': 'AI Content Filtering',
    'ai.f2.desc': 'Smart filters driven by semantic tags — one click to filter by “portrait”, “sunset”, “street”. Instantly locate targets among thousands of photos. Filtering becomes a one-click action, not a chore.',
    'ai.f2.tag1': 'Semantic topic filter',
    'ai.f2.tag2': 'One-click filtering',
    'ai.f2.tag3': 'Stacks with EXIF',
    'ai.f3.title': 'AI Natural-Language Search',
    'ai.f3.desc': 'Describe the photo in a sentence — “portrait by the sea at sunset”, “neon on a rainy night” — and AI understands your intent, matching precisely across your entire library. Finding photos has never been this intuitive.',
    'ai.f3.tag1': 'Natural-language understanding',
    'ai.f3.tag2': 'Semantic vector search',
    'ai.f3.tag3': 'Instant library-wide match',
    'features.label': 'FEATURES',
    'features.title': 'A Powerful Toolkit<br/><span class="text-gradient">Built for Creation</span>',
    'features.desc': 'From color science to geometry correction, from basic adjustments to smart management — every step is carefully crafted',
    'features.f1.title': 'Blazing-Fast Asset Management',
    'features.f1.desc': 'A responsive, low-resource image management system supporting the vast majority of RAW formats. Browse, filter, and rate in one seamless flow.',
    'features.f1.tag1': 'Multi-format RAW support',
    'features.f1.tag2': 'Low resource usage',
    'features.f1.tag3': 'Fast preview',
    'features.f2.title': 'Dual-Engine Color Science',
    'features.f2.desc': 'ACES and OpenDRT dual color rendering pipelines. Whether you seek faithful reproduction or artistic stylization, find the perfect color grade for every scene.',
    'features.f2.tag1': 'ACES pipeline',
    'features.f2.tag2': 'OpenDRT rendering',
    'features.f2.tag3': 'Scene-based tones',
    'features.f3.title': '150MP Real-Time Adjustments',
    'features.f3.desc': 'Exposure, contrast, white balance, tone curves... every basic tool is deeply optimized. Even with <strong>150-megapixel</strong> monster RAW files, sliders remain buttery smooth.',
    'features.f3.tag1': '150MP real-time processing',
    'features.f3.tag2': 'CUDA / Metal acceleration',
    'features.f3.tag3': 'Zero-latency feedback',
    'features.f4.title': 'Advanced Color Control',
    'features.f4.desc': 'Color wheels, HSL separation, channel mixer... every hue, saturation, and luminance value is at your fingertips. Warmer skin tones, clearer skies — just seconds away.',
    'features.f4.tag1': 'Color wheels',
    'features.f4.tag2': 'Fine HSL separation',
    'features.f4.tag3': 'Channel mixer',
    'features.f5.title': 'Geometry & Composition',
    'features.f5.desc': 'Lens distortion correction, perspective repair, free crop, with rich aspect ratio presets (1:1, 4:5, 16:9, 2.39:1, etc.). Fix composition flaws and craft the perfect frame.',
    'features.f5.tag1': 'Lens distortion correction',
    'features.f5.tag2': 'Perspective repair',
    'features.f5.tag3': 'Multi-ratio crop presets',
    'features.f6.title': 'Pro-Grade Export',
    'features.f6.desc': 'From HDR to standard JPEG, with embedded ICC profiles and HDR export modes. Whether your work goes to the web, print, or cinema pipeline, colors stay accurate.',
    'features.f6.tag1': 'HDR export',
    'features.f6.tag2': 'ICC profile embedding',
    'features.f6.tag3': 'Multi-format output',
    'films.label': 'FILM SIMULATION',
    'films.title': 'Classic Film Looks<br/><span class="text-gradient">One Click Away</span>',
    'films.desc': 'Digitally recreated LUT presets based on real film characteristics, paired with film grain and Halation bloom simulation — giving digital images the texture and soul of film.',
    'films.showcase.tag': 'Portrait · Delicate',
    'films.showcase.desc': 'The portrait photography benchmark. Extremely delicate and natural skin tone reproduction with soft, elegant tonality. Its ISO 400 speed handles variable light with ease — a reliable choice for everyday and portrait work.',
    'films.grain.title': 'Film Grain & Halation Simulation',
    'films.grain.desc': 'Beyond color reproduction. Alcedo Studio recreates the physical traits of real film: adjustable grain size and intensity, highlight bloom forming Halation, and darkroom edge-softening effects. From clean digital to gritty film, texture is yours to control.',
    'films.grain.tag1': 'Adjustable film grain',
    'films.grain.tag2': 'Halation bloom',
    'films.grain.tag3': 'Darkroom texture',
    'films.stocks.label': 'SUPPORTED FILM STOCKS',
    'cta.version': 'Latest release',
    'cta.title': 'Ready to Create?',
    'cta.desc': 'Alcedo Studio is completely free and open-source. No subscription, no feature limits. Download and access all professional features immediately, including AI content recognition and natural-language search.',
    'cta.win': 'Download for Windows',
    'cta.mac': 'Download for macOS',
    'cta.baidu': 'Baidu Wangpan',
    'cta.note': 'Accelerated by CUDA (NVIDIA) and Metal (Apple Silicon)',
    'footer.tagline': 'AI-powered. Create freely, start with Alcedo.',
    'footer.copy': '© 2026 Alcedo Studio. Open source, free to use.'
  }
};

// ========================================
// i18n Engine
// ========================================
function detectLanguage() {
  const stored = localStorage.getItem('alcedo-lang');
  if (stored && translations[stored]) return stored;
  const navLang = navigator.language || navigator.userLanguage;
  if (navLang && navLang.toLowerCase().startsWith('zh')) return 'zh';
  return 'en';
}

let currentLang = detectLanguage();

function setLanguage(lang) {
  if (!translations[lang]) return;
  currentLang = lang;
  localStorage.setItem('alcedo-lang', lang);
  document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';

  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    const value = translations[lang][key];
    if (value === undefined) return;

    if (el.tagName === 'META') {
      el.setAttribute('content', value);
    } else if (el.tagName === 'TITLE') {
      el.textContent = value;
    } else {
      el.innerHTML = value;
    }
  });

  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.lang === lang);
  });

  // Re-stamp version label after i18n, preserving the version tag if resolved.
  stampVersionLabel();
}

// ========================================
// Dynamic latest release download
// Fetches the GitHub API "latest" release and points the
// Windows / macOS buttons at the real asset URLs + version tag.
// Falls back to the /releases/latest page if the API is unreachable.
// ========================================
const GITHUB_REPO = 'zidage/AlcedoStudio';
const FALLBACK_TAG = 'v0.2.5';
let resolvedVersion = null;

async function resolveLatestRelease() {
  const winBtn = document.getElementById('downloadWin');
  const macBtn = document.getElementById('downloadMac');
  if (!winBtn || !macBtn) return;

  const fallbackBase = `https://github.com/${GITHUB_REPO}/releases/download/${FALLBACK_TAG}`;
  const fallbackWin = `${fallbackBase}/AlcedoStudio-0.2.5-Windows-AMD64.exe`;
  const fallbackMac = `${fallbackBase}/AlcedoStudio-0.2.5-Darwin-arm64.dmg`;

  try {
    const res = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`, {
      headers: { Accept: 'application/vnd.github+json' }
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const tag = data.tag_name || FALLBACK_TAG;
    resolvedVersion = tag;
    // Asset filenames embed the bare version (e.g. 0.2.5), derive from tag.
    const bare = tag.replace(/^v/, '');

    let winUrl = null;
    let macUrl = null;
    for (const asset of (data.assets || [])) {
      const name = String(asset.name || '').toLowerCase();
      const url = asset.browser_download_url;
      if (!winUrl && name.endsWith('.exe') && name.includes('windows')) winUrl = url;
      else if (!winUrl && name.endsWith('.exe')) winUrl = url;
      if (!macUrl && (name.endsWith('.dmg') || name.endsWith('.zip')) && (name.includes('darwin') || name.includes('mac') || name.includes('osx'))) macUrl = url;
      else if (!macUrl && name.endsWith('.dmg')) macUrl = url;
    }

    winBtn.href = winUrl || fallbackWin;
    macBtn.href = macUrl || fallbackMac;
  } catch (err) {
    // Network / rate-limit / offline — keep pointing at the latest page.
    resolvedVersion = null;
    winBtn.href = `https://github.com/${GITHUB_REPO}/releases/latest`;
    macBtn.href = `https://github.com/${GITHUB_REPO}/releases/latest`;
  }

  stampVersionLabel();
}

function stampVersionLabel() {
  const el = document.getElementById('ctaVersion');
  if (!el) return;
  // Rebuild so i18n label + resolved tag coexist.
  const label = (translations[currentLang] && translations[currentLang]['cta.version']) || 'Latest release';
  el.innerHTML = '<span class="cta-version-dot"></span><span>' +
    (resolvedVersion ? `${label} · ${resolvedVersion}` : label) + '</span>';
}

// ========================================
// Landing Page Interactions
// ========================================

document.documentElement.classList.add('js-enabled');

document.addEventListener('DOMContentLoaded', () => {
  // Initialize i18n
  setLanguage(currentLang);

  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const lang = btn.dataset.lang;
      if (lang && lang !== currentLang) {
        setLanguage(lang);
      }
    });
  });

  // Resolve latest release (non-blocking)
  resolveLatestRelease();

  // ========================================
  // Navbar scroll effect
  // ========================================
  const navbar = document.getElementById('navbar');

  function updateNavbar() {
    if (window.scrollY > 40) {
      navbar.classList.add('scrolled');
    } else {
      navbar.classList.remove('scrolled');
    }
  }

  window.addEventListener('scroll', updateNavbar, { passive: true });
  updateNavbar();

  // ========================================
  // Mobile menu toggle
  // ========================================
  const navToggle = document.getElementById('navToggle');
  const navMenu = document.getElementById('navMenu');

  if (navToggle && navMenu) {
    navToggle.addEventListener('click', () => {
      navToggle.classList.toggle('active');
      navMenu.classList.toggle('open');
    });

    navMenu.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', () => {
        navToggle.classList.remove('active');
        navMenu.classList.remove('open');
      });
    });
  }

  // ========================================
  // Scroll reveal animations
  // ========================================
  const revealElements = document.querySelectorAll('.reveal');

  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        revealObserver.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.1,
    rootMargin: '0px 0px -40px 0px'
  });

  revealElements.forEach(el => {
    const rect = el.getBoundingClientRect();
    if (rect.top < window.innerHeight && rect.bottom > 0) {
      el.classList.add('visible');
    }
    revealObserver.observe(el);
  });

  // ========================================
  // Smooth scroll for anchor links (fallback)
  // ========================================
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      const href = this.getAttribute('href');
      if (href === '#' || href.length <= 1) return;
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        const offsetTop = target.getBoundingClientRect().top + window.scrollY - 90;
        window.scrollTo({ top: offsetTop, behavior: 'smooth' });
      }
    });
  });

  // ========================================
  // Stagger delay for AI cards
  // ========================================
  document.querySelectorAll('.ai-card').forEach((card, index) => {
    card.style.transitionDelay = `${index * 90}ms`;
  });

  // ========================================
  // Stagger delay for feature items
  // ========================================
  document.querySelectorAll('.feature-item').forEach((item, index) => {
    item.style.transitionDelay = `${index * 60}ms`;
  });
});
