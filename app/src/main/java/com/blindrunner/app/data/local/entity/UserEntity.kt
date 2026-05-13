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
    val remoteId: Int? = null    // ID from remote API after sync
)
