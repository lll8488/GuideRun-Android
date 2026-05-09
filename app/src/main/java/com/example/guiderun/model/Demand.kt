package com.example.guiderun.data.model

data class Demand(
    val demandId: Long = 0,
    val publisherId: Long, // 发布者ID
    val runnerId: Long? = null, // 接单志愿者ID
    val runDate: String, // 格式：yyyy-MM-dd
    val runTime: String, // 格式：HH:mm
    val location: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val duration: Int, // 分钟
    val remark: String? = null,
    val status: Int = 0, // 0=待接单，1=已接单，2=已完成，3=已取消
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)