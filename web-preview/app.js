/* ============================================
   OPPO OMaster - Web 预览版
   ============================================ */

// 预设数据
const presets = [
    {
        id: "hncs_natural",
        name: "哈苏自然色彩 - 经典HNCS",
        description: "保留自然色彩，保留更多明暗层次与细节，展现哈苏色彩哲学",
        style: "natural",
        scenes: ["portrait", "landscape", "street"],
        isHncs: true,
        isFavorite: false,
        usageCount: 12847,
        rating: 4.9,
        creator: {
            name: "哈苏影像实验室",
            avatar: null
        },
        params: {
            exposure: 0.0,
            contrast: 8,
            saturation: 5,
            warmth: 0,
            tint: 0,
            vignette: 8,
            clarity: 12,
            saturationEnhancement: 8,
            skinToneProtection: 85,
            skyOptimization: 60,
            greenVibrance: 50
        },
        tips: [
            "最佳使用时间：日出或日落时分，光线柔和",
            "适合场景：风光、纪实、日常街拍",
            "曝光建议：略微欠曝0.3档可获得更好的色彩层次"
        ],
        coverImage: "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&q=80"
    },
    {
        id: "hncs_portrait",
        name: "哈苏人像 - 自然肤色优化",
        description: "专业优化人像肤色，保留真实质感，背景色彩自然呈现",
        style: "portrait",
        scenes: ["portrait", "urban"],
        isHncs: true,
        isFavorite: true,
        usageCount: 8932,
        rating: 4.8,
        creator: {
            name: "哈苏影像实验室",
            avatar: null
        },
        params: {
            exposure: 0.3,
            contrast: 5,
            saturation: 3,
            warmth: 5,
            tint: -2,
            vignette: 12,
            clarity: 8,
            saturationEnhancement: 5,
            skinToneProtection: 95,
            faceOptimization: true,
            eyeEnhancement: 70
        },
        tips: [
            "适合在自然光下拍摄人像",
            "建议使用大光圈虚化背景",
            "避免阳光直射面部"
        ],
        coverImage: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80"
    },
    {
        id: "film_kodachrome",
        name: "胶片色彩 - Kodachrome模拟",
        description: "经典柯达克罗姆胶片色彩，浓郁但不过分的色彩表现",
        style: "film",
        scenes: ["street", "landscape"],
        isHncs: false,
        isFavorite: false,
        usageCount: 15621,
        rating: 4.7,
        creator: {
            name: "胶片爱好者社区",
            avatar: null
        },
        params: {
            exposure: -0.2,
            contrast: 18,
            saturation: 18,
            warmth: 8,
            tint: 3,
            vignette: 15,
            clarity: 20,
            filmGrain: 12
        },
        tips: [
            "适合阳光充足的环境",
            "色彩浓郁，适合街拍和风光",
            "后期可适当降低饱和度"
        ],
        coverImage: "https://images.unsplash.com/photo-1493246507139-91e8fad9978e?w=800&q=80"
    },
    {
        id: "hncs_lowlight",
        name: "哈苏夜拍 - 纯净夜景",
        description: "优化夜景噪点控制，保留更多暗部细节，色彩依然准确",
        style: "night",
        scenes: ["night", "urban"],
        isHncs: true,
        isFavorite: false,
        usageCount: 6234,
        rating: 4.9,
        creator: {
            name: "哈苏影像实验室",
            avatar: null
        },
        params: {
            exposure: 0.0,
            contrast: 12,
            saturation: 8,
            warmth: 3,
            tint: 0,
            vignette: 10,
            clarity: 5,
            noiseReduction: 85,
            highlightRecovery: 70,
            shadowLift: 40
        },
        tips: [
            "建议使用三脚架稳定拍摄",
            "ISO尽量控制在800以内",
            "寻找有灯光的场景"
        ],
        coverImage: "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800&q=80"
    },
    {
        id: "cinematic_teal",
        name: "电影色调 - 青橙风格",
        description: "经典好莱坞青橙色调，强烈的色彩对比，营造电影感",
        style: "cinematic",
        scenes: ["urban", "portrait"],
        isHncs: false,
        isFavorite: false,
        usageCount: 21567,
        rating: 4.6,
        creator: {
            name: "电影调色师工作室",
            avatar: null
        },
        params: {
            exposure: -0.3,
            contrast: 22,
            saturation: 12,
            warmth: -5,
            tint: 0,
            vignette: 20,
            clarity: 18,
            splitTone: "teal_orange",
            filmGrain: 8
        },
        tips: [
            "适合有人物和天空的场景",
            "增加对比度增强戏剧感",
            "注意皮肤色调的保护"
        ],
        coverImage: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&q=80"
    },
    {
        id: "hncs_landscape",
        name: "哈苏风光 - 专业风光优化",
        description: "针对自然风光优化，天空更蓝、绿色更鲜活，细节更丰富",
        style: "landscape",
        scenes: ["landscape", "nature"],
        isHncs: true,
        isFavorite: true,
        usageCount: 9876,
        rating: 4.9,
        creator: {
            name: "哈苏影像实验室",
            avatar: null
        },
        params: {
            exposure: 0.0,
            contrast: 15,
            saturation: 12,
            warmth: -3,
            tint: 0,
            vignette: 10,
            clarity: 20,
            saturationEnhancement: 15,
            skyOptimization: 90,
            greenVibrance: 80,
            blueVibrance: 70
        },
        tips: [
            "日出日落时分效果最佳",
            "使用偏振镜消除反光",
            "适当增加曝光补偿"
        ],
        coverImage: "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&q=80"
    }
];

// 状态管理
let currentFilter = 'all';
let currentStyle = 'all';
let currentScene = 'all';
let searchQuery = '';
let currentPreset = null;

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    renderPresets();
    setupEventListeners();
});

// 设置事件监听
function setupEventListeners() {
    // 首页筛选
    document.querySelectorAll('.filter-chips .chip').forEach(chip => {
        chip.addEventListener('click', () => {
            currentFilter = chip.dataset.filter;
            document.querySelectorAll('.filter-chips .chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            renderPresets();
        });
    });

    // 风格筛选
    document.querySelectorAll('.style-chips .style-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            currentStyle = chip.dataset.style;
            document.querySelectorAll('.style-chips .style-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            renderPresets();
        });
    });

    // 场景筛选
    document.querySelectorAll('.scene-chips .scene-chip').forEach(chip => {
        chip.addEventListener('click', () => {
            currentScene = chip.dataset.scene;
            document.querySelectorAll('.scene-chips .scene-chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            renderPresets();
        });
    });

    // 搜索
    document.getElementById('search-input').addEventListener('input', (e) => {
        searchQuery = e.target.value.toLowerCase();
        renderPresets();
    });

    // 返回按钮
    document.getElementById('back-btn').addEventListener('click', () => {
        showScreen('home');
    });

    // 应用按钮
    document.getElementById('apply-preset-btn').addEventListener('click', () => {
        if (currentPreset) {
            alert(`已将「${currentPreset.name}」应用到相机！`);
        }
    });
}

// 渲染预设卡片
function renderPresets() {
    const grid = document.getElementById('presets-grid');
    let filteredPresets = presets;

    // 基础筛选
    if (currentFilter === 'favorites') {
        filteredPresets = filteredPresets.filter(p => p.isFavorite);
    } else if (currentFilter === 'hncs') {
        filteredPresets = filteredPresets.filter(p => p.isHncs);
    }

    // 风格筛选
    if (currentStyle !== 'all') {
        filteredPresets = filteredPresets.filter(p => p.style === currentStyle);
    }

    // 场景筛选
    if (currentScene !== 'all') {
        filteredPresets = filteredPresets.filter(p => p.scenes.includes(currentScene));
    }

    // 搜索筛选
    if (searchQuery) {
        filteredPresets = filteredPresets.filter(p => 
            p.name.toLowerCase().includes(searchQuery) || 
            p.description.toLowerCase().includes(searchQuery)
        );
    }

    if (filteredPresets.length === 0) {
        grid.innerHTML = `
            <div style="text-align: center; padding: 60px 20px; color: var(--text-secondary-dark);">
                <div style="font-size: 48px; margin-bottom: 16px;">🔍</div>
                <h3 style="font-size: 18px; margin-bottom: 8px; color: var(--text-primary-dark);">没有找到预设</h3>
                <p style="font-size: 14px;">试试其他筛选条件</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = filteredPresets.map(preset => `
        <div class="preset-card" onclick="showDetail('${preset.id}')">
            <div class="preset-cover">
                <img src="${preset.coverImage}" alt="${preset.name}">
                <div class="preset-cover-gradient"></div>
                ${preset.isHncs ? `
                    <div class="preset-hncs-badge">
                        <div class="preset-hncs-circle"></div>
                        <span class="preset-hncs-text">HNCS</span>
                    </div>
                ` : ''}
                <button class="preset-fav-btn" onclick="event.stopPropagation(); toggleFavorite('${preset.id}')">
                    ${preset.isFavorite ? '❤️' : '🤍'}
                </button>
                <div class="preset-stats">
                    ▶️ ${preset.usageCount.toLocaleString()}
                </div>
            </div>
            <div class="preset-info">
                <div class="preset-name">${preset.name}</div>
                <div class="preset-meta">
                    <span class="meta-tag style-tag">${getStyleLabel(preset.style)}</span>
                    <span class="meta-tag rating-tag">⭐ ${preset.rating}</span>
                </div>
                <div class="preset-description">${preset.description}</div>
            </div>
        </div>
    `).join('');
}

// 获取风格标签
function getStyleLabel(style) {
    const labels = {
        'natural': '自然真实',
        'film': '胶片质感',
        'cinematic': '电影感',
        'portrait': '人像优化',
        'landscape': '风光优化',
        'night': '夜景优化'
    };
    return labels[style] || style;
}

// 获取场景标签
function getSceneLabel(scene) {
    const labels = {
        'portrait': '人像',
        'landscape': '风光',
        'night': '夜景',
        'street': '街拍',
        'urban': '城市',
        'nature': '自然',
        'food': '美食',
        'sunset': '日落',
        'architecture': '建筑'
    };
    return labels[scene] || scene;
}

// 切换收藏
function toggleFavorite(id) {
    const preset = presets.find(p => p.id === id);
    if (preset) {
        preset.isFavorite = !preset.isFavorite;
        renderPresets();
        if (currentPreset && currentPreset.id === id) {
            currentPreset.isFavorite = preset.isFavorite;
            updateDetailFavButton();
        }
    }
}

// 更新详情页收藏按钮
function updateDetailFavButton() {
    const btn = document.getElementById('detail-fav-btn');
    if (currentPreset) {
        btn.textContent = currentPreset.isFavorite ? '❤️' : '🤍';
    }
}

// 显示详情
function showDetail(id) {
    currentPreset = presets.find(p => p.id === id);
    if (!currentPreset) return;

    const content = document.getElementById('detail-content');
    content.innerHTML = `
        <div class="detail-cover">
            <img src="${currentPreset.coverImage}" alt="${currentPreset.name}">
            <div class="detail-cover-gradient"></div>
        </div>
        
        <div class="detail-title">${currentPreset.name}</div>
        
        <div class="detail-meta">
            <span class="meta-tag style-tag">${getStyleLabel(currentPreset.style)}</span>
            ${currentPreset.isHncs ? `
                <span class="meta-tag device-tag">✨ HNCS 认证</span>
            ` : ''}
            <span class="meta-tag rating-tag">⭐ ${currentPreset.rating}</span>
        </div>
        
        <div class="detail-stats">
            <div class="stat-item">
                <div class="stat-value">${currentPreset.usageCount.toLocaleString()}</div>
                <div class="stat-label">使用次数</div>
            </div>
            <div class="stat-item">
                <div class="stat-value">${currentPreset.rating}</div>
                <div class="stat-label">评分</div>
            </div>
        </div>
        
        ${currentPreset.creator ? `
            <div class="detail-section">
                <div class="info-card">
                    <div class="info-card-title">创作者</div>
                    <div class="info-card-content">${currentPreset.creator.name}</div>
                </div>
            </div>
        ` : ''}
        
        <div class="detail-section">
            <div class="detail-section-title">相机参数</div>
            <div class="params-card">
                <div class="param-row">
                    <span class="param-label">曝光补偿</span>
                    <span class="param-value">${currentPreset.params.exposure >= 0 ? '+' : ''}${currentPreset.params.exposure} EV</span>
                </div>
                <div class="param-row">
                    <span class="param-label">对比度</span>
                    <span class="param-value">${currentPreset.params.contrast}</span>
                </div>
                <div class="param-row">
                    <span class="param-label">饱和度</span>
                    <span class="param-value">${currentPreset.params.saturation}</span>
                </div>
                <div class="param-row">
                    <span class="param-label">色温</span>
                    <span class="param-value">${currentPreset.params.warmth >= 0 ? '+' : ''}${currentPreset.params.warmth}</span>
                </div>
                <div class="param-row">
                    <span class="param-label">晕影</span>
                    <span class="param-value">${currentPreset.params.vignette}</span>
                </div>
                <div class="param-row">
                    <span class="param-label">清晰度</span>
                    <span class="param-value">${currentPreset.params.clarity}</span>
                </div>
            </div>
        </div>
        
        ${currentPreset.scenes && currentPreset.scenes.length > 0 ? `
            <div class="detail-section">
                <div class="detail-section-title">适用场景</div>
                <div class="scene-tags-card">
                    <div class="scene-tag-list">
                        ${currentPreset.scenes.map(scene => `
                            <span class="scene-tag">${getSceneLabel(scene)}</span>
                        `).join('')}
                    </div>
                </div>
            </div>
        ` : ''}
        
        ${currentPreset.tips && currentPreset.tips.length > 0 ? `
            <div class="detail-section">
                <div class="detail-section-title">使用提示</div>
                <div class="tips-card">
                    <div class="tip-list">
                        ${currentPreset.tips.map((tip, index) => `
                            <div class="tip-item">
                                <div class="tip-bullet">${index + 1}</div>
                                <div class="tip-text">${tip}</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        ` : ''}
        
        <div style="height: 100px;"></div>
    `;

    // 更新收藏按钮
    document.getElementById('detail-fav-btn').textContent = currentPreset.isFavorite ? '❤️' : '🤍';
    document.getElementById('detail-fav-btn').onclick = () => toggleFavorite(currentPreset.id);

    showScreen('detail');
}

// 显示页面
function showScreen(screen) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(`${screen}-screen`).classList.add('active');
}
