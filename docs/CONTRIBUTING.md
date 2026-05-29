# 贡献指南

感谢您有兴趣为 OPPO Master 项目做出贡献！

## 如何贡献

### 提交 Bug 报告和功能建议

- 使用 GitHub Issues 提交问题
- 详细描述问题重现步骤
- 提供截图和设备信息（如适用）

### 提交代码

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 遵循 Android 开发最佳实践
- Kotlin 代码遵循官方代码风格
- 使用有意义的变量和函数名
- 添加适当的注释

## 预设贡献指南

我们欢迎所有摄影爱好者分享您的专业预设！

### 预设格式

请按照以下格式提交预设：

```json
{
  "id": "unique_preset_id",
  "name": "预设名称",
  "coverPath": "封面图片URL",
  "sections": [
    {
      "title": "适用场景",
      "content": "描述此预设适用的场景"
    },
    {
      "title": "特点",
      "content": "此预设的特点和优势"
    }
  ],
  "cameraParams": {
    "mode": "master",
    "filter": "滤镜名称",
    "iso": 100,
    "shutter": "1/200",
    "ev": "0",
    "wb": "5500K",
    "focal_length": "24mm",
    "aperture": "f/1.8",
    "hdr": false,
    "night_mode": false,
    "portrait_mode": false,
    "ai_optimization": true,
    "hasselblad_hncs": false,
    "hasselblad_natural_color": true,
    "hasselblad_master_style": "Natural",
    "color_profile": "Natural",
    "sharpness": 50,
    "contrast": 50,
    "saturation": 50
  },
  "deviceModel": "OPPO Find X8 Pro",
  "source": "community",
  "isNew": false,
  "isFeatured": false,
  "isPremium": false,
  "downloadCount": 0,
  "rating": 4.5,
  "tags": ["风景", "哈苏", "自然"]
}
```

### 预设提交流程

1. 在项目根目录创建您的预设 JSON 文件
2. 确保预设名称有意义且与内容相符
3. 提供合适的封面图片（建议尺寸 600x600）
4. 详细填写参数说明
5. 添加合适的标签，方便用户搜索
6. 通过 PR 提交您的预设

## 社区行为准则

- 尊重所有贡献者
- 保持专业和友好的沟通
- 接受建设性的批评
- 帮助新贡献者

## 许可证

通过贡献代码或预设，您同意将您的贡献根据项目许可证（MIT）授权。
