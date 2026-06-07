# OMaster 应用图标规格

## 图标要求

### 1. 应用图标 (App Icon)

| 尺寸 | 用途 | 格式 |
|------|------|------|
| 48x48 | 小图标 | PNG |
| 72x72 | 中图标 | PNG |
| 96x96 | 大图标 | PNG |
| 144x144 | 超大图标 | PNG |
| 192x192 | 启动图标 | PNG |
| 512x512 | 应用商店图标 | PNG |

### 2. 特色图形 (Feature Graphic)

| 尺寸 | 用途 | 格式 |
|------|------|------|
| 1024x500 | Google Play 特色图 | PNG/JPG |
| 1024x1024 | 应用商店宣传图 | PNG |

### 3. 截图要求

| 尺寸 | 用途 | 格式 |
|------|------|------|
| 1080x1920 | 手机截图 | PNG/JPG |
| 2560x1600 | 平板截图 (可选) | PNG/JPG |

---

## 设计规范

### 品牌色彩

```
主色 (哈苏橙): #EA580C
次色 (深色背景): #18181B
强调色: #F97316
文字色: #E4E4E7
```

### 图标设计要点

1. **前景层** (ic_launcher_foreground.xml)
   - 使用简洁的相机/镜头图形
   - 中心对齐，四周留白 10%
   - 使用单色或渐变

2. **背景层** (ic_launcher_background.xml)
   - 使用深色背景 (#18181B)
   - 或使用渐变效果

3. **自适应图标** (adaptive_icon.xml)
   - 圆形遮罩适配
   - 圆角方形遮罩适配
   - 花式遮罩适配

---

## 图标生成命令

### 使用 ImageMagick 生成各尺寸图标

```bash
# 从 512x512 源图生成各尺寸
convert icon-512.png -resize 48x48 icon-48.png
convert icon-512.png -resize 72x72 icon-72.png
convert icon-512.png -resize 96x96 icon-96.png
convert icon-512.png -resize 144x144 icon-144.png
convert icon-512.png -resize 192x192 icon-192.png
```

### 使用 Android Asset Studio

访问: https://romannurik.github.io/AndroidAssetStudio/

---

## 当前图标文件

```
app/src/main/res/
├── drawable/
│   ├── ic_launcher_foreground.xml  # 前景层
│   └── ic_launcher_background.xml  # 背景层
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml             # 自适应图标
│   ├── ic_launcher_round.xml       # 圆形图标
│   └── adaptive_icon.xml           # 图标定义
└── mipmap-*/
    └── ic_launcher.png             # 各密度图标
```

---

## 截图建议

### 首页截图
- 展示预设列表
- 突出哈苏 HNCS 认证徽章
- 显示精选预设卡片

### 预设详情截图
- 展示参数详情页
- 显示 ISO、快门、光圈等参数
- 突出应用按钮

### AI 场景识别截图
- 展示场景识别界面
- 显示 AI 分析结果
- 突出推荐预设

### 水印编辑截图
- 展示水印模板选择
- 显示编辑界面
- 突出预览效果

### 设置页面截图
- 展示设置选项
- 显示主题切换
- 突出语言设置

---

## 特色图形设计

### Google Play 特色图 (1024x500)

设计要点:
- 左侧放置应用图标
- 右侧展示核心功能
- 使用品牌色彩
- 包含简短标语: "哈苏摄影预设专家"

### 宣传图 (1024x1024)

设计要点:
- 中心放置应用图标
- 周围展示功能图标
- 底部放置标语
- 使用渐变背景

---

**文档更新时间**: 2026-06-07
