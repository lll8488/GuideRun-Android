package com.example.guiderun.data.model

data class RunningRecord(
    val recordId: Long = 0,
    val demandId: Long,
    val userId: Long,
    val runnerId: Long,
    val startTime: Long,
    val endTime: Long,
    val actualDuration: Int, // 分钟
    val location: String
)