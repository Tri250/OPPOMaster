package com.omaster.app.model

enum class SceneType(
    val displayName: String, 
    val description: String,
    val keywords: List<String>,
    val recommendedParams: CameraParams?
) {
    // ==================== 人像类（15种） ====================
    PORTRAIT("人像", "适合人物摄影", 
        listOf("人像", "自拍", "合影", "写真", "肖像", "大头照", "证件照"),
        CameraParams(hasselblad_hncs = true, master_hdr = "智能")),
    
    PORTRAIT_NATURAL("自然人像", "自然光人像",
        listOf("自然光", "柔和光", "窗光", "逆光人像"),
        CameraParams(hasselblad_hncs = true, ai_scene = true)),
    
    PORTRAIT_GLOWING("发光人像", "柔光人像",
        listOf("柔光", "发光", "梦幻", "仙气"),
        CameraParams(saturation = 12, clarity = 8)),
    
    PORTRAIT_MOOD("情绪人像", "有氛围感的人像",
        listOf("情绪", "氛围", "暗调", "情绪人像"),
        CameraParams(brightness = -5, contrast = 15, saturation = 8)),
    
    PORTRAIT_RETRO("复古人像", "复古风格人像",
        listOf("复古", "怀旧", "胶片感", "颗粒"),
        CameraParams(saturation = 5, contrast = 10, warmth = 8)),
    
    PORTRAIT_HIGHKEY("高调人像", "明亮清新人像",
        listOf("高调", "明亮", "清新", "日系"),
        CameraParams(brightness = 15, contrast = -5, saturation = 10)),
    
    PORTRAIT_LOWKEY("低调人像", "深沉有质感人像",
        listOf("低调", "深沉", "质感", "电影感"),
        CameraParams(brightness = -10, contrast = 20, saturation = 5)),
    
    GROUP_PHOTO("合影", "多人合影",
        listOf("合影", "团体", "集体照", "聚会"),
        CameraParams(hdr = true, ai_scene = true)),
    
    KIDS("儿童", "儿童摄影",
        listOf("儿童", "宝宝", "小孩", "萌娃"),
        CameraParams(saturation = 15, brightness = 10, ai_scene = true)),
    
    WEDDING("婚礼", "婚礼摄影",
        listOf("婚礼", "婚纱", "典礼", "婚庆"),
        CameraParams(hasselblad_hncs = true, master_hdr = "智能")),
    
    MATERNITY("孕妇", "孕妇摄影",
        listOf("孕妇", "孕照", "孕期"),
        CameraParams(saturation = 12, brightness = 8, clarity = 5)),
    
    PET("宠物", "宠物摄影",
        listOf("宠物", "猫", "狗", "小动物"),
        CameraParams(ai_scene = true, saturation = 10)),
    
    // ==================== 风景类（15种） ====================
    LANDSCAPE("风景", "户外风景摄影",
        listOf("风景", "山川", "自然", " scenery"),
        CameraParams(hdr = true, ai_scene = true)),
    
    MOUNTAIN("山脉", "山景摄影",
        listOf("山", "山脉", "峰峦", "高原"),
        CameraParams(contrast = 15, clarity = 12, saturation = 10)),
    
    OCEAN("海洋", "海边摄影",
        listOf("海", "海洋", "沙滩", "波浪"),
        CameraParams(saturation = 15, brightness = 5, hdr = true)),
    
    LAKE("湖泊", "湖景摄影",
        listOf("湖", "湖泊", "倒影", "静谧"),
        CameraParams(saturation = 12, clarity = 10, brightness = 5)),
    
    FOREST("森林", "森林摄影",
        listOf("森林", "树木", "绿植", "林间"),
        CameraParams(saturation = 15, clarity = 8, ai_scene = true)),
    
    FLOWER("花卉", "花卉摄影",
        listOf("花", "花卉", "植物", "花园"),
        CameraParams(saturation = 20, clarity = 15, macro = true)),
    
    SKY("天空", "天空云彩",
        listOf("天空", "云", "蓝天", "云彩"),
        CameraParams(brightness = 10, contrast = 12, saturation = 8)),
    
    SNOW("雪景", "雪地摄影",
        listOf("雪", "雪景", "冬季", "冰雪"),
        CameraParams(brightness = 15, contrast = 5, saturation = 3)),
    
    DESERT("沙漠", "沙漠摄影",
        listOf("沙漠", "沙丘", "戈壁", "荒漠"),
        CameraParams(saturation = 10, contrast = 15, warmth = 12)),
    
    WATERFALL("瀑布", "瀑布摄影",
        listOf("瀑布", "水流", "溪流", "江河"),
        CameraParams(contrast = 12, clarity = 15, brightness = 3)),
    
    STARS("星空", "星空摄影",
        listOf("星空", "星星", "银河", "夜空"),
        CameraParams(brightness = -5, contrast = 15, saturation = 8)),
    
    RAINBOW("彩虹", "彩虹摄影",
        listOf("彩虹", "虹", "光谱"),
        CameraParams(saturation = 25, brightness = 8, hdr = true)),
    
    SUNRISE("日出", "日出摄影",
        listOf("日出", "朝阳", "晨曦", "破晓"),
        CameraParams(warmth = 20, brightness = 8, saturation = 15)),
    
    SUNSET("日落", "日落摄影",
        listOf("日落", "夕阳", "黄昏", "晚霞"),
        CameraParams(warmth = 25, saturation = 20, brightness = 5)),
    
    CLOUDS("云海", "云海摄影",
        listOf("云海", "云雾", "仙境", "山间云雾"),
        CameraParams(contrast = 10, brightness = 8, clarity = 12)),
    
    // ==================== 城市建筑类（10种） ====================
    ARCHITECTURE("建筑", "建筑摄影",
        listOf("建筑", "楼房", "大厦", "现代建筑"),
        CameraParams(contrast = 15, clarity = 12, ai_scene = true)),
    
    CITY_NIGHT("城市夜景", "都市夜景",
        listOf("城市夜景", "霓虹", "灯火", "夜景"),
        CameraParams(brightness = -8, contrast = 20, saturation = 15)),
    
    STREET("街头", "街头摄影",
        listOf("街头", "街道", "城市", "纪实"),
        CameraParams(contrast = 12, clarity = 8, blackWhite = false)),
    
    LANDMARK("地标", "城市地标",
        listOf("地标", "标志性建筑", "纪念碑", "塔"),
        CameraParams(clarity = 15, contrast = 12, hdr = true)),
    
    INTERIOR("室内", "室内空间",
        listOf("室内", "房间", "家居", "装修"),
        CameraParams(brightness = 8, saturation = 5, ai_scene = true)),
    
    ROOM("空间", "室内设计",
        listOf("空间", "装修", "北欧", "简约"),
        CameraParams(saturation = 8, brightness = 10, contrast = 5)),
    
    OFFICE("办公", "办公空间",
        listOf("办公室", "工作", "写字楼", "商务"),
        CameraParams(contrast = 8, saturation = 5, brightness = 5)),
    
    SHOP("商铺", "商业空间",
        listOf("商店", "店铺", "橱窗", "商业"),
        CameraParams(saturation = 12, brightness = 8, contrast = 8)),
    
    RESTAURANT("餐厅", "餐饮空间",
        listOf("餐厅", "饭店", "咖啡厅", "餐饮"),
        CameraParams(warmth = 10, saturation = 12, brightness = 5)),
    
    HOTEL("酒店", "酒店空间",
        listOf("酒店", "民宿", "客房", "旅馆"),
        CameraParams(brightness = 10, saturation = 5, contrast = 5)),
    
    // ==================== 美食类（8种） ====================
    FOOD("美食", "美食摄影",
        listOf("美食", "菜肴", "料理", " food"),
        CameraParams(saturation = 18, brightness = 5, ai_scene = true)),
    
    DISH("菜品", "中式菜品",
        listOf("菜品", "中餐", "炒菜", "佳肴"),
        CameraParams(saturation = 20, warmth = 5, brightness = 8)),
    
    CAKE("甜点", "蛋糕甜品",
        listOf("蛋糕", "甜点", "甜品", "下午茶"),
        CameraParams(saturation = 22, brightness = 10, clarity = 12)),
    
    DRINK("饮品", "饮料咖啡",
        listOf("饮品", "咖啡", "饮料", "奶茶"),
        CameraParams(saturation = 15, brightness = 8, warmth = 8)),
    
    SUSHI("寿司", "日料摄影",
        listOf("寿司", "日料", "刺身", "日本料理"),
        CameraParams(saturation = 12, contrast = 8, brightness = 5)),
    
    BBQ("烧烤", "烧烤美食",
        listOf("烧烤", "烤肉", "撸串", "BBQ"),
        CameraParams(warmth = 12, saturation = 15, contrast = 8)),
    
    NOODLE("面食", "面食摄影",
        listOf("面条", "面食", "拉面", "米粉"),
        CameraParams(saturation = 10, warmth = 8, brightness = 5)),
    
    VEGETABLE("蔬果", "蔬菜水果",
        listOf("蔬菜", "水果", "沙拉", "健康"),
        CameraParams(saturation = 25, clarity = 12, brightness = 8)),
    
    // ==================== 自然生态类（8种） ====================
    NATURE("自然", "自然生态",
        listOf("自然", "生态", "户外", " nature"),
        CameraParams(saturation = 12, ai_scene = true, hdr = true)),
    
    PLANT("植物", "植物摄影",
        listOf("植物", "绿植", "盆栽", "花卉"),
        CameraParams(saturation = 18, clarity = 12, macro = true)),
    
    TREE("树木", "树木摄影",
        listOf("树", "树木", "森林", "林间"),
        CameraParams(saturation = 12, contrast = 8, clarity = 10)),
    
    GRASS("草地", "草坪草地",
        listOf("草地", "草坪", "绿草", "草原"),
        CameraParams(saturation = 15, brightness = 8, ai_scene = true)),
    
    BUG("昆虫", "昆虫微距",
        listOf("昆虫", "虫子", "蚂蚁", "蝴蝶"),
        CameraParams(macro = true, clarity = 18, contrast = 10)),
    
    BIRD("鸟类", "鸟类摄影",
        listOf("鸟", "鸟类", "飞鸟", "天鹅"),
        CameraParams(contrast = 12, clarity = 15, ai_scene = true)),
    
    AQUARIUM("水族", "水下世界",
        listOf("鱼", "水族", "珊瑚", "海洋生物"),
        CameraParams(saturation = 18, brightness = 8, hdr = true)),
    
    ZOO("动物园", "动物园摄影",
        listOf("动物园", "动物", "狮子", "老虎"),
        CameraParams(contrast = 10, saturation = 12, ai_scene = true)),
    
    // ==================== 创意类（10种） ====================
    MACRO("微距", "微距摄影",
        listOf("微距", "特写", "细节", " macro"),
        CameraParams(macro = true, clarity = 20, contrast = 12)),
    
    BLACK_WHITE("黑白", "黑白摄影",
        listOf("黑白", "单色", "Mono", "B&W"),
        CameraParams(blackWhite = true, contrast = 15, clarity = 10)),
    
    VINTAGE("复古", "复古风格",
        listOf("复古", "怀旧", "胶片", "Vintage"),
        CameraParams(saturation = 5, contrast = 12, warmth = 15)),
    
    CINEMATIC("电影感", "电影色调",
        listOf("电影感", "Cinematic", "电影", "好莱坞"),
        CameraParams(contrast = 18, saturation = 8, brightness = -3)),
    
    CYBERPUNK("赛博朋克", "科技感色调",
        listOf("赛博朋克", "科技", "霓虹", "Cyberpunk"),
        CameraParams(saturation = 25, contrast = 20, hue = 15)),
    
    PAINTING("油画感", "油画效果",
        listOf("油画", "绘画", "艺术", "Painting"),
        CameraParams(saturation = 8, contrast = 5, clarity = 15)),
    
    MINIMAL("极简", "极简风格",
        listOf("极简", "简约", "Less", "Minimal"),
        CameraParams(saturation = 3, contrast = 8, brightness = 10)),
    
    NEON("霓虹", "霓虹灯光",
        listOf("霓虹", "灯光", "夜店", "Neon"),
        CameraParams(saturation = 25, contrast = 15, brightness = -5)),
    
    FILM("胶片", "胶片质感",
        listOf("胶片", "Film", "颗粒", " Kodak"),
        CameraParams(saturation = 8, contrast = 10, grain = true)),
    
    RETRO80S("80年代", "80年代风格",
        listOf("80s", "复古", "迪斯科", "年代"),
        CameraParams(saturation = 20, contrast = 12, warmth = 15)),
    
    // ==================== 其他类（9种） ====================
    DOCUMENTARY("纪实", "纪实摄影",
        listOf("纪实", "记录", " Documentary"),
        CameraParams(contrast = 10, saturation = 5, clarity = 8)),
    
    SPORTS("运动", "运动摄影",
        listOf("运动", "体育", "足球", "篮球"),
        CameraParams(shutter_speed = "1/1000", ai_scene = true)),
    
    CAR("汽车", "汽车摄影",
        listOf("汽车", "车", "跑车", "Car"),
        CameraParams(contrast = 15, clarity = 12, hdr = true)),
    
    PRODUCT("产品", "产品摄影",
        listOf("产品", "商品", "电商", "Product"),
        CameraParams(contrast = 12, saturation = 10, clarity = 15)),
    
    DOCUMENT("文档", "文档扫描",
        listOf("文档", "文件", "文字", "扫描"),
        CameraParams(contrast = 20, saturation = 0, clarity = 15)),
    
    SCREENSHOT("截图", "屏幕截图",
        listOf("截图", "屏幕", "界面", "UI"),
        CameraParams(contrast = 10, saturation = 0, brightness = 5)),
    
    QR_CODE("二维码", "二维码",
        listOf("二维码", "QR", "码"),
        CameraParams(contrast = 20, saturation = 0, clarity = 18)),
    
    XRAY("X光", "X光透视",
        listOf("X光", "透视", "Medical"),
        CameraParams(contrast = 25, saturation = 0, brightness = 5)),
    
    UNKNOWN("智能识别", "AI自动识别",
        listOf("未知", "自动", "智能"),
        CameraParams(ai_scene = true))
}
