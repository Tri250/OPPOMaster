package com.omaster.app.model

data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 100,
    val shutter: String = "1/125",
    val ev: String = "0",
    val wb: String = "5500K",
    val hasselblad_hncs: Boolean = false
)
