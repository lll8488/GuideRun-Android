package com.example.guiderun.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "running_record",
    foreignKeys = [
        ForeignKey(
            entity = Demand::class,
            parentColumns = ["demandId"],
            childColumns = ["demandId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["runnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RunningRecord(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val demandId: Long,
    val userId: Long,
    val runnerId: Long,
    val startTime: Long,
    val endTime: Long,
    val actualDuration: Int, // 分钟
    val location: String
)