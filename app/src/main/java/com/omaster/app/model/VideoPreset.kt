package com.omaster.app.model

data class VideoPreset(
    val id: String,
    val name: String,
    val coverPath: String,
    val fps: Int = 30,
    val codec: String = "H.264",
    val bitrate: Long = 10_000_000,
    val resolution: String = "1080p",
    val videoLut: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
