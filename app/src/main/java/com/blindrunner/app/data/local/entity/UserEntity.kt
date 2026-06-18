package com.blindrunner.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phone: String,
    val name: String,
    val userType: String,        // "blind", "volunteer"
    val emergencyContact: String? = null,
    val rating: Float = 0f,      // average rating (1-5)
    val ratingCount: Int = 0,    // number of ratings received
    val totalRuns: Int = 0,      // total completed runs (for badge system)
    val totalDistanceKm: Float = 0f, // lifetime distance
    val examPassed: Boolean = false,  // PRD 4.2: 考核是否通过（80分及以上）
    val examScore: Int = 0,       // PRD 4.2: 最近一次考核分数
    val remoteId: Int? = null    // ID from remote API after sync
)
