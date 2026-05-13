package com.blindrunner.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_records")
data class RunningRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val durationMinutes: Int,
    val location: String,
    val distanceKm: Float,
    val status: String,        // "pending", "accepted", "completed"
    val remoteId: Int? = null  // ID from remote API after sync
)
