package com.omaster.app.model

enum class SceneType(val displayName: String, val description: String) {
    LANDSCAPE("风景", "适合户外风景、山川湖海"),
    PORTRAIT("人像", "适合人物摄影"),
    NIGHT("夜景", "适合夜间城市、星空"),
    SUNSET("日落", "适合日落、黄金时刻"),
    FOOD("美食", "适合美食拍摄"),
    STREET("街头", "适合街头纪实"),
    NATURE("自然", "适合自然生态、植物"),
    ARCHITECTURE("建筑", "适合城市建筑、室内空间"),
    MACRO("微距", "适合特写、微距摄影"),
    SPORTS("运动", "适合快速移动物体"),
    NIGHT_PORTRAIT("夜景人像", "适合夜晚环境下的人像拍摄"),
    BLACK("全黑场景", "光线太暗，无法识别"),
    WHITE("全白场景", "无法识别场景"),
    BLURRY("模糊场景", "画面模糊，无法识别"),
    UNKNOWN("未知", "自动识别场景")
}
