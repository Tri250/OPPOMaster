/* ============================================================
   OPPO Master · Hasselblad Professional Imaging
   Screen Data Definitions
   8 Core Screens · ColorOS 16 金标规范
   ============================================================ */

const SCREENS = [
  // ============================================================
  // SCREEN 1: HOME / PRESET LIBRARY
  // ============================================================
  {
    num: '01',
    name: '预设首页',
    tag: 'PRESET LIBRARY',
    type: 'home'
  },

  // ============================================================
  // SCREEN 2: PRESET DETAIL
  // ============================================================
  {
    num: '02',
    name: '预设详情',
    tag: 'PRESET DETAIL',
    type: 'detail'
  },

  // ============================================================
  // SCREEN 3: AI EDIT / CREATE
  // ============================================================
  {
    num: '03',
    name: '创作中心',
    tag: 'CREATE CENTER',
    type: 'create'
  },

  // ============================================================
  // SCREEN 4: WATERMARK EDITOR
  // ============================================================
  {
    num: '04',
    name: '哈苏水印编辑',
    tag: 'WATERMARK',
    type: 'watermark'
  },

  // ============================================================
  // SCREEN 5: CAMERA CONTROL
  // ============================================================
  {
    num: '05',
    name: '相机控制面板',
    tag: 'CAMERA HUD',
    type: 'camera'
  },

  // ============================================================
  // SCREEN 6: PROFILE
  // ============================================================
  {
    num: '06',
    name: '我的中心',
    tag: 'MY PROFILE',
    type: 'profile'
  },

  // ============================================================
  // SCREEN 7: SETTINGS
  // ============================================================
  {
    num: '07',
    name: '设置',
    tag: 'SETTINGS',
    type: 'settings'
  },

  // ============================================================
  // SCREEN 8: FEEDBACK
  // ============================================================
  {
    num: '08',
    name: '意见反馈',
    tag: 'FEEDBACK',
    type: 'feedback'
  }
];

// ============================================================
// Icon Library (inline SVG)
// ============================================================
const ICONS = {
  // Brand & UI
  hasselMark: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24l-7.19-.61L12 2z"/></svg>`,
  search: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>`,
  back: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>`,
  share: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13"/></svg>`,
  heart: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>`,
  more: `<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/></svg>`,

  // Bottom Nav
  home: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12L12 3l9 9v9a2 2 0 01-2 2h-4v-7H9v7H5a2 2 0 01-2-2v-9z"/></svg>`,
  create: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l2.5 6.5L21 11l-6.5 2.5L12 20l-2.5-6.5L3 11l6.5-2.5L12 2zM19 3v4M21 5h-4"/></svg>`,
  user: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M16 7a4 4 0 11-8 0 4 4 0 018 0z"/></svg>`,

  // Camera
  camera: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2v11z"/><circle cx="12" cy="13" r="4"/></svg>`,
  flip: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9V5a2 2 0 012-2h4M21 9V5a2 2 0 00-2-2h-4M3 15v4a2 2 0 002 2h4M21 15v4a2 2 0 01-2 2h-4"/></svg>`,
  grid: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
  flash: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M13 2L3 14h7l-1 8 10-12h-7l1-8z"/></svg>`,
  timer: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2 2M9 2h6"/></svg>`,
  hdr: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><text x="12" y="15" text-anchor="middle" font-size="6" fill="currentColor" stroke="none" font-weight="bold">HDR</text></svg>`,

  // Status Bar
  signal: `<svg viewBox="0 0 16 12" fill="currentColor"><rect x="0" y="8" width="2.5" height="4" rx="0.5"/><rect x="4" y="6" width="2.5" height="6" rx="0.5"/><rect x="8" y="3" width="2.5" height="9" rx="0.5"/><rect x="12" y="0" width="2.5" height="12" rx="0.5"/></svg>`,
  wifi: `<svg viewBox="0 0 16 12" fill="currentColor"><path d="M8 11.5l1.5-1.5a2 2 0 00-3 0L8 11.5zM2 6l1.5-1.5a6 6 0 019 0L14 6 8 12 2 6zM5 9l1.5-1.5a3 3 0 013 0L11 9 8 12 5 9z"/></svg>`,
  battery: `<svg viewBox="0 0 26 12" fill="none"><rect x="0.5" y="0.5" width="22" height="11" rx="2.5" stroke="currentColor" stroke-opacity="0.5"/><rect x="2" y="2" width="19" height="8" rx="1.5" fill="currentColor"/><rect x="24" y="3.5" width="2" height="5" rx="1" fill="currentColor" fill-opacity="0.5"/></svg>`,

  // Watermark tools
  text: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 7V4h16v3M9 20h6M12 4v16"/></svg>`,
  image: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>`,
  logo: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3" fill="currentColor"/></svg>`,
  layers: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>`,
  size: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21h6v-6M21 3h-6v6M3 3l7 7M21 21l-7-7"/></svg>`,
  opacity: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M12 3v18M3 12h18" fill-opacity="0.3"/></svg>`,
  rotate: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 11-9-9c2.5 0 4.7 1 6.4 2.6L21 8M21 3v5h-5"/></svg>`,

  // Settings
  moon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>`,
  bell: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0"/></svg>`,
  shield: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
  storage: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/></svg>`,
  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>`,
  globe: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 010 20M12 2a15 15 0 000 20"/></svg>`,

  // Feedback
  bug: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 2l1.5 1.5M16 2l-1.5 1.5M9 18l-2 4M15 18l2 4M3 8h3M18 8h3M3 16h3M18 16h3M12 18a6 6 0 006-6V8H6v4a6 6 0 006 6z"/></svg>`,
  lightbulb: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18h6M10 22h4M12 2a7 7 0 00-4 12.7V17h8v-2.3A7 7 0 0012 2z"/></svg>`,
  star: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3 7 7 .8-5.3 4.8L18 22l-6-3.5L6 22l1.3-7.4L2 9.8 9 9l3-7z"/></svg>`,
  chat: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z"/></svg>`
};

// ============================================================
// Status Bar HTML (reused on all screens)
// ============================================================
function statusBar() {
  return `
    <div class="status-bar">
      <span>9:41</span>
      <div class="status-icons">
        ${ICONS.signal}
        ${ICONS.wifi}
        ${ICONS.battery}
      </div>
    </div>
  `;
}

function dynamicIsland() {
  return `<div class="dynamic-island"></div>`;
}

// ============================================================
// SCREEN 1: HOME
// ============================================================
function renderHome() {
  return `
    <div class="screen-home">
      ${statusBar()}
      <div class="home-header">
        <div class="home-greeting">
          <div>
            <div class="greeting-text">下午好,</div>
            <div class="greeting-name">林墨白</div>
          </div>
          <div class="greeting-avatar">墨</div>
        </div>
        <div class="search-bar">
          <span class="search-icon">${ICONS.search}</span>
          <input class="search-input" placeholder="搜索预设、风格、摄影师..." readonly />
          <span class="search-shortcut">⌘ K</span>
        </div>
      </div>

      <div class="category-tabs">
        <div class="cat-tab active">全部 · 42</div>
        <div class="cat-tab">HNCS · 8</div>
        <div class="cat-tab">人像 · 12</div>
        <div class="cat-tab">风光 · 9</div>
        <div class="cat-tab">★ 收藏</div>
      </div>

      <div class="featured-section">
        <div class="featured-card">
          <div class="featured-bg"></div>
          <div class="featured-noise"></div>
          <div class="featured-content">
            <div>
              <span class="featured-tag">★ HASSELBLAD · 本周精选</span>
              <div class="featured-title">Golden Hour<br/>黄金时刻</div>
              <div class="featured-sub">HNCS 3.0 认证 · Hasselblad 官方调校</div>
            </div>
            <div class="featured-bottom">
              <div class="featured-meta">
                <span>● 8.2K 使用</span>
                <span>★ 4.9</span>
              </div>
              <div class="apply-btn">应用到相机 →</div>
            </div>
          </div>
        </div>
      </div>

      <div class="preset-grid-section">
        <div class="section-label">
          <div class="section-label-title">HNCS 认证预设</div>
          <div class="section-label-more">查看全部 →</div>
        </div>
        <div class="preset-grid">
          <div class="preset-card">
            <div class="preset-image gradient-1"></div>
            <div class="preset-badge">HNCS</div>
            <div class="preset-fav">${ICONS.heart}</div>
            <div class="preset-content">
              <div class="preset-name">Stockholm</div>
              <div class="preset-sub">北欧冷调 · 人像</div>
            </div>
          </div>
          <div class="preset-card">
            <div class="preset-image gradient-2"></div>
            <div class="preset-badge">HNCS</div>
            <div class="preset-fav">${ICONS.heart}</div>
            <div class="preset-content">
              <div class="preset-name">Reykjavik</div>
              <div class="preset-sub">极光蓝 · 风光</div>
            </div>
          </div>
          <div class="preset-card">
            <div class="preset-image gradient-3"></div>
            <div class="preset-badge">HNCS</div>
            <div class="preset-fav">${ICONS.heart}</div>
            <div class="preset-content">
              <div class="preset-name">Sahara</div>
              <div class="preset-sub">沙漠金 · 风光</div>
            </div>
          </div>
          <div class="preset-card">
            <div class="preset-image gradient-4"></div>
            <div class="preset-badge">HNCS</div>
            <div class="preset-fav">${ICONS.heart}</div>
            <div class="preset-content">
              <div class="preset-name">Kyoto</div>
              <div class="preset-sub">樱粉白 · 街拍</div>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-nav">
        <div class="nav-item active">${ICONS.home}<span class="label">预设</span></div>
        <div class="nav-item">${ICONS.create}<span class="label">创作</span></div>
        <div class="nav-item">${ICONS.user}<span class="label">我的</span></div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 2: PRESET DETAIL
// ============================================================
function renderDetail() {
  return `
    <div class="screen-detail">
      ${statusBar()}
      <div class="detail-hero">
        <div class="detail-hero-actions">
          <div class="action-circle">${ICONS.back}</div>
          <div style="display:flex;gap:8px;">
            <div class="action-circle">${ICONS.heart}</div>
            <div class="action-circle">${ICONS.share}</div>
          </div>
        </div>
        <div class="detail-hero-badge">
          <span class="badge-pill">★ <span class="hassel-mark">HNCS</span> 3.0 · 官方认证</span>
          <span class="badge-pill">8.2K · ★ 4.9</span>
        </div>
      </div>

      <div class="detail-body">
        <div class="detail-title">Stockholm No.7</div>
        <div class="detail-subtitle">北欧冷调人像 · Hasselblad 官方调校</div>

        <div class="params-card">
          <div class="params-title">— 核心参数 · HNCS Signature</div>
          <div class="params-row">
            <div class="param-item">
              <div class="param-label">饱和度</div>
              <div class="param-value">-8<span class="param-unit">%</span></div>
            </div>
            <div class="param-item">
              <div class="param-label">对比度</div>
              <div class="param-value">+12<span class="param-unit">%</span></div>
            </div>
            <div class="param-item">
              <div class="param-label">色温</div>
              <div class="param-value">5400<span class="param-unit">K</span></div>
            </div>
            <div class="param-item">
              <div class="param-label">清晰度</div>
              <div class="param-value">+6<span class="param-unit">%</span></div>
            </div>
            <div class="param-item">
              <div class="param-label">高光</div>
              <div class="param-value">-15<span class="param-unit">%</span></div>
            </div>
            <div class="param-item">
              <div class="param-label">阴影</div>
              <div class="param-value">+10<span class="param-unit">%</span></div>
            </div>
          </div>
        </div>

        <div class="description-text">
          灵感来自 <strong>斯德哥尔摩</strong> 冬季清晨的冷调光线。HNCS 3.0 自然色彩解决方案,精准还原 Hasselblad X2D 的色彩科学。适用于人像、街拍与极简建筑题材。
        </div>
      </div>

      <div class="detail-bottom">
        <div class="btn-secondary">${ICONS.heart}<span>收藏</span></div>
        <div class="btn-primary">${ICONS.camera}<span>应用到相机 · 一键出片</span></div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 3: CREATE CENTER (创作中心)
// ============================================================
function renderCreate() {
  return `
    <div class="screen-create">
      ${statusBar()}
      <div class="create-header">
        <div style="width:36px;height:36px;"></div>
        <div class="create-title"><span class="accent">创作</span> 中心</div>
        <div style="width:36px;height:36px;display:grid;place-items:center;color:var(--text-tertiary);">${ICONS.more}</div>
      </div>

      <div class="create-modes">
        <div class="mode-card primary">
          <div class="mode-glow"></div>
          <div class="mode-icon lg">${ICONS.create}</div>
          <div>
            <div class="mode-title">AI 智能修图</div>
            <div class="mode-sub">8 场景 · 4 风格</div>
          </div>
        </div>
        <div class="mode-card">
          <div class="mode-icon">${ICONS.logo}</div>
          <div>
            <div class="mode-title">哈苏水印</div>
            <div class="mode-sub">8 模板 · 实时预览</div>
          </div>
        </div>
        <div class="mode-card">
          <div class="mode-icon">${ICONS.layers}</div>
          <div>
            <div class="mode-title">批量处理</div>
            <div class="mode-sub">最多 20 张</div>
          </div>
          <span class="mode-tag">PRO</span>
        </div>
        <div class="mode-card">
          <div class="mode-icon">${ICONS.camera}</div>
          <div>
            <div class="mode-title">直接拍摄</div>
            <div class="mode-sub">系统相机调用</div>
          </div>
        </div>
      </div>

      <div class="photo-canvas">
        <div class="photo-corner tl"></div>
        <div class="photo-corner tr"></div>
        <div class="photo-corner bl"></div>
        <div class="photo-corner br"></div>
        <div class="ai-detect-overlay">
          <span class="ai-pulse"></span>
          <span class="ai-text">AI · 已识别「人像」</span>
        </div>
        <div class="photo-watermark">
          <div class="wm-logo">HASSELBLAD</div>
          <div class="wm-sub">STOCKHOLM · ISO 200 · 1/250s · f/2.8</div>
        </div>
      </div>

      <div class="ai-style-section">
        <div class="ai-label">
          <div class="ai-label-text">优化风格 · 4 选 1</div>
          <div class="ai-label-tag">AI 智能匹配 ✓</div>
        </div>
        <div class="style-list">
          <div class="style-chip active">
            <div class="style-thumb natural"></div>
            <div class="style-name">自然</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb vivid"></div>
            <div class="style-name">鲜明</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb cinematic"></div>
            <div class="style-name">电影</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb portrait"></div>
            <div class="style-name">人像</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb mono"></div>
            <div class="style-name">黑白</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb film"></div>
            <div class="style-name">胶片</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb vintage"></div>
            <div class="style-name">复古</div>
          </div>
          <div class="style-chip">
            <div class="style-thumb dream"></div>
            <div class="style-name">梦境</div>
          </div>
        </div>
      </div>

      <div class="param-section">
        <div class="sliders">
          <div class="slider-row">
            <div class="slider-label">饱和度</div>
            <div class="slider-track">
              <div class="slider-fill" style="width:65%"></div>
              <div class="slider-thumb" style="left:65%"></div>
            </div>
            <div class="slider-value">+13</div>
          </div>
          <div class="slider-row">
            <div class="slider-label">对比度</div>
            <div class="slider-track">
              <div class="slider-fill" style="width:42%"></div>
              <div class="slider-thumb" style="left:42%"></div>
            </div>
            <div class="slider-value">+8</div>
          </div>
          <div class="slider-row">
            <div class="slider-label">清晰度</div>
            <div class="slider-track">
              <div class="slider-fill" style="width:78%"></div>
              <div class="slider-thumb" style="left:78%"></div>
            </div>
            <div class="slider-value">+15</div>
          </div>
        </div>
      </div>

      <div class="create-actions">
        <div class="btn-secondary">${ICONS.layers}<span>原图对比</span></div>
        <div class="btn-primary">✨ 一键 AI 优化</div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 4: WATERMARK EDITOR
// ============================================================
function renderWatermark() {
  return `
    <div class="screen-watermark">
      ${statusBar()}
      <div class="wm-header">
        <div class="back-btn">${ICONS.back}</div>
        <div class="wm-title-block">
          <div class="wm-eyebrow">HASSELBLAD WATERMARK</div>
          <div class="wm-title">哈苏水印</div>
        </div>
        <div class="back-btn" style="visibility:hidden">${ICONS.back}</div>
      </div>

      <div class="wm-canvas">
        <div class="wm-overlay">
          <span class="wm-overlay-pill">● 实时预览</span>
          <span class="wm-overlay-pill">HDR · HNCS</span>
        </div>
        <div class="wm-selection-box">
          <span class="handle"></span>
          <span class="handle"></span>
        </div>
        <div class="wm-canvas-text">
          <div class="wm-canvas-brand">HASSELBLAD</div>
          <div class="wm-canvas-sub">OPPO MASTER · ISO 200 · f/1.8</div>
        </div>
      </div>

      <div class="wm-templates-section">
        <div class="ai-label">
          <div class="ai-label-text">官方模板 · 8 种</div>
          <div class="ai-label-tag">更多 →</div>
        </div>
        <div class="wm-templates-list">
          <div class="wm-template active"><span class="wm-template-text">HASSEL<br/>CLASSIC</span></div>
          <div class="wm-template"><span class="wm-template-text">OPPO<br/>CO-BRAND</span></div>
          <div class="wm-template"><span class="wm-template-text">PARAMS<br/>ONLY</span></div>
          <div class="wm-template"><span class="wm-template-text">DATE<br/>STAMP</span></div>
          <div class="wm-template"><span class="wm-template-text">LOCATION<br/>GPS</span></div>
          <div class="wm-template"><span class="wm-template-text">FILM<br/>STYLE</span></div>
          <div class="wm-template"><span class="wm-template-text">MINIMAL<br/>DOT</span></div>
          <div class="wm-template"><span class="wm-template-text">SIGN<br/>NAME</span></div>
        </div>
      </div>

      <div class="wm-tools">
        <div class="wm-tool active">${ICONS.text}<span class="wm-tool-label">文字</span></div>
        <div class="wm-tool">${ICONS.image}<span class="wm-tool-label">Logo</span></div>
        <div class="wm-tool">${ICONS.size}<span class="wm-tool-label">缩放</span></div>
        <div class="wm-tool">${ICONS.opacity}<span class="wm-tool-label">透明度</span></div>
        <div class="wm-tool">${ICONS.rotate}<span class="wm-tool-label">旋转</span></div>
      </div>

      <div class="wm-bottom">
        <div class="btn-secondary">${ICONS.layers}<span>20 张批量</span></div>
        <div class="btn-primary">✓ 应用水印 · 保存</div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 5: CAMERA CONTROL
// ============================================================
function renderCamera() {
  return `
    <div class="screen-camera">
      <div class="camera-viewfinder">
        <div class="camera-hud-top">
          <div class="hud-mode">
            <span class="dot"></span>
            <span class="text">PRO · M</span>
          </div>
          <div class="hud-icon-row">
            <div class="hud-icon">${ICONS.flash}</div>
            <div class="hud-icon">${ICONS.timer}</div>
            <div class="hud-icon">${ICONS.grid}</div>
            <div class="hud-icon">${ICONS.hdr}</div>
          </div>
        </div>

        <div class="scene-detect">
          <div class="scene-tag">
            <span class="scene-icon">🌅</span>
            <span class="scene-text">黄金时刻</span>
            <span class="scene-confidence">· 96%</span>
          </div>
        </div>

        <div class="focus-frame">
          <div class="focus-corner tl"></div>
          <div class="focus-corner tr"></div>
          <div class="focus-corner bl"></div>
          <div class="focus-corner br"></div>
          <div class="focus-ring"></div>
        </div>

        <div class="params-overlay">
          <div class="param-pill">
            <div class="param-pill-label">ISO</div>
            <div class="param-pill-value">200</div>
          </div>
          <div class="param-pill">
            <div class="param-pill-label">S</div>
            <div class="param-pill-value">1/250</div>
          </div>
          <div class="param-pill">
            <div class="param-pill-label">F</div>
            <div class="param-pill-value">f/1.8</div>
          </div>
          <div class="param-pill">
            <div class="param-pill-label">EV</div>
            <div class="param-pill-value">+0.3</div>
          </div>
          <div class="param-pill">
            <div class="param-pill-label">WB</div>
            <div class="param-pill-value">5400K</div>
          </div>
        </div>
      </div>

      <div class="camera-controls">
        <div class="preset-pills">
          <div class="preset-pill active">★ Stockholm</div>
          <div class="preset-pill">Reykjavik</div>
          <div class="preset-pill">Sahara</div>
          <div class="preset-pill">Kyoto</div>
          <div class="preset-pill">Golden</div>
        </div>
        <div class="shutter-row">
          <div class="shutter-side">${ICONS.grid}</div>
          <div class="shutter-btn"></div>
          <div class="shutter-side">${ICONS.flip}</div>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 6: PROFILE (我的中心)
// ============================================================
function renderProfile() {
  return `
    <div class="screen-profile">
      ${statusBar()}
      <div class="profile-header">
        <div class="profile-avatar-wrap">
          <div class="profile-avatar">墨<span class="profile-hncs-badge">HNCS</span></div>
        </div>
        <div class="profile-name">林墨白</div>
        <div class="profile-id">@Moby · ID: 8821 · Hasselblad Master</div>
        <div class="profile-stats">
          <div class="pstat"><div class="pstat-num">128</div><div class="pstat-label">作品</div></div>
          <div class="pstat"><div class="pstat-num">42</div><div class="pstat-label">收藏</div></div>
          <div class="pstat"><div class="pstat-num">3.2K</div><div class="pstat-label">点赞</div></div>
        </div>
      </div>

      <div class="profile-entries">
        <div class="profile-entry">
          <div class="entry-icon">${ICONS.hasselMark}</div>
          <div class="entry-text">
            <div class="entry-name">我的预设</div>
            <div class="entry-count">12 个 · 含 3 自定义</div>
          </div>
        </div>
        <div class="profile-entry">
          <div class="entry-icon">${ICONS.heart}</div>
          <div class="entry-text">
            <div class="entry-name">收藏夹</div>
            <div class="entry-count">42 个 · 已同步</div>
          </div>
        </div>
        <div class="profile-entry">
          <div class="entry-icon">${ICONS.timer}</div>
          <div class="entry-text">
            <div class="entry-name">历史记录</div>
            <div class="entry-count">128 条操作</div>
          </div>
        </div>
        <div class="profile-entry">
          <div class="entry-icon">${ICONS.more}</div>
          <div class="entry-text">
            <div class="entry-name">设置</div>
            <div class="entry-count">外观 · 隐私 · 拍摄</div>
          </div>
        </div>
      </div>

      <div class="profile-tabs">
        <div class="ptab active">成就</div>
        <div class="ptab">作品</div>
        <div class="ptab">收藏</div>
        <div class="ptab">历史</div>
      </div>

      <div class="achievements-section">
        <div class="section-label">
          <div class="section-label-title">哈苏成就</div>
          <div class="section-label-more">4/16 解锁</div>
        </div>
        <div class="achv-grid">
          <div class="achv-item">
            <div class="achv-icon gold">${ICONS.hasselMark}</div>
            <div class="achv-name">首次成片</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon gold">${ICONS.hasselMark}</div>
            <div class="achv-name">百张精选</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon gold">${ICONS.hasselMark}</div>
            <div class="achv-name">金标认证</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon gold">${ICONS.hasselMark}</div>
            <div class="achv-name">色彩大师</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon">${ICONS.hasselMark}</div>
            <div class="achv-name">夜色猎人</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon">${ICONS.hasselMark}</div>
            <div class="achv-name">街拍达人</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon">${ICONS.hasselMark}</div>
            <div class="achv-name">人像专家</div>
          </div>
          <div class="achv-item">
            <div class="achv-icon">${ICONS.hasselMark}</div>
            <div class="achv-name">视频大师</div>
          </div>
        </div>
      </div>

      <div class="recent-section">
        <div class="section-label">
          <div class="section-label-title">最近作品</div>
          <div class="section-label-more">查看全部 →</div>
        </div>
        <div class="recent-grid">
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
          <div class="recent-item"><span class="recent-watermark">HASSELBLAD</span></div>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 7: SETTINGS
// ============================================================
function renderSettings() {
  return `
    <div class="screen-settings">
      ${statusBar()}
      <div class="settings-header">
        <div class="back-btn">${ICONS.back}</div>
        <div class="settings-title">设置</div>
      </div>

      <div class="settings-section">
        <div class="settings-label">外观</div>
        <div class="settings-card">
          <div class="settings-row">
            <div class="settings-icon">${ICONS.moon}</div>
            <div class="settings-text">
              <div class="settings-row-name">深色模式</div>
              <div class="settings-row-desc">跟随系统 · 推荐</div>
            </div>
            <div class="toggle on"><div class="toggle-knob"></div></div>
          </div>
          <div class="settings-row">
            <div class="settings-icon">${ICONS.bell}</div>
            <div class="settings-text">
              <div class="settings-row-name">推送通知</div>
              <div class="settings-row-desc">新预设、精选活动</div>
            </div>
            <div class="toggle on"><div class="toggle-knob"></div></div>
          </div>
        </div>

        <div class="settings-label">拍摄</div>
        <div class="settings-card">
          <div class="settings-row">
            <div class="settings-icon">${ICONS.camera}</div>
            <div class="settings-text">
              <div class="settings-row-name">相机悬浮窗</div>
              <div class="settings-row-desc">实时显示参数与预设切换</div>
            </div>
            <div class="toggle on"><div class="toggle-knob"></div></div>
          </div>
          <div class="settings-row">
            <div class="settings-icon">${ICONS.hasselMark}</div>
            <div class="settings-text">
              <div class="settings-row-name">快门声</div>
              <div class="settings-row-desc">模拟哈苏镜间快门</div>
            </div>
            <div class="toggle on"><div class="toggle-knob"></div></div>
          </div>
          <div class="settings-row">
            <div class="settings-icon">${ICONS.flash}</div>
            <div class="settings-text">
              <div class="settings-row-name">触觉反馈</div>
              <div class="settings-row-desc">操作时的震动反馈</div>
            </div>
            <div class="toggle"><div class="toggle-knob"></div></div>
          </div>
        </div>

        <div class="settings-label">隐私与数据</div>
        <div class="settings-card">
          <div class="settings-row">
            <div class="settings-icon">${ICONS.shield}</div>
            <div class="settings-text">
              <div class="settings-row-name">位置水印</div>
              <div class="settings-row-desc">在照片中嵌入 GPS 信息</div>
            </div>
            <div class="toggle"><div class="toggle-knob"></div></div>
          </div>
          <div class="settings-row">
            <div class="settings-icon">${ICONS.storage}</div>
            <div class="settings-text">
              <div class="settings-row-name">存储管理</div>
              <div class="settings-row-desc">已用 2.4 GB · 清理缓存</div>
            </div>
            <div class="settings-icon" style="background:transparent">${ICONS.more}</div>
          </div>
        </div>

        <div class="settings-label">其他</div>
        <div class="settings-card">
          <div class="settings-row">
            <div class="settings-icon">${ICONS.globe}</div>
            <div class="settings-text">
              <div class="settings-row-name">语言</div>
              <div class="settings-row-desc">简体中文</div>
            </div>
            <div class="settings-icon" style="background:transparent">${ICONS.more}</div>
          </div>
          <div class="settings-row">
            <div class="settings-icon">${ICONS.info}</div>
            <div class="settings-text">
              <div class="settings-row-name">关于</div>
              <div class="settings-row-desc">OPPO Master v2.1.0</div>
            </div>
            <div class="settings-icon" style="background:transparent">${ICONS.more}</div>
          </div>
        </div>
      </div>
    </div>
  `;
}

// ============================================================
// SCREEN 8: FEEDBACK
// ============================================================
function renderFeedback() {
  return `
    <div class="screen-feedback">
      ${statusBar()}
      <div class="fb-header">
        <div class="back-btn">${ICONS.back}</div>
        <div class="fb-title">意见反馈</div>
        <div class="fb-subtitle">我们重视每一条建议 · 24 小时内回复</div>
      </div>

      <div class="fb-content">
        <div class="fb-categories">
          <div class="fb-cat active">
            <div class="fb-cat-icon">🐛</div>
            <div class="fb-cat-name">功能异常</div>
            <div class="fb-cat-desc">Bug 报告与崩溃</div>
          </div>
          <div class="fb-cat">
            <div class="fb-cat-icon">💡</div>
            <div class="fb-cat-name">功能建议</div>
            <div class="fb-cat-desc">想要的新功能</div>
          </div>
          <div class="fb-cat">
            <div class="fb-cat-icon">🎨</div>
            <div class="fb-cat-name">预设建议</div>
            <div class="fb-cat-desc">新色彩与风格</div>
          </div>
          <div class="fb-cat">
            <div class="fb-cat-icon">💬</div>
            <div class="fb-cat-name">其他反馈</div>
            <div class="fb-cat-desc">综合咨询</div>
          </div>
        </div>

        <textarea class="fb-textarea" placeholder="请详细描述您的问题或建议...&#10;&#10;例如:使用 Stockholm 预设拍摄人像时,面部肤色偏黄..." readonly></textarea>

        <div class="fb-contact">
          <input class="fb-contact-input" placeholder="联系方式 (邮箱 / 手机号)" readonly />
        </div>

        <div class="fb-log-toggle">
          <div class="fb-log-info">
            <div class="settings-icon" style="margin:0;">${ICONS.storage}</div>
            <div>
              <div class="fb-log-text">上传诊断日志</div>
              <div class="fb-log-sub">帮助我们更快定位问题 · 仅本次</div>
            </div>
          </div>
          <div class="toggle on"><div class="toggle-knob"></div></div>
        </div>
      </div>

      <div class="fb-bottom">
        <div class="btn-primary">提交反馈</div>
      </div>
    </div>
  `;
}

// ============================================================
// Screen Router
// ============================================================
const SCREEN_RENDERERS = {
  home: renderHome,
  detail: renderDetail,
  create: renderCreate,
  watermark: renderWatermark,
  camera: renderCamera,
  profile: renderProfile,
  settings: renderSettings,
  feedback: renderFeedback
};
