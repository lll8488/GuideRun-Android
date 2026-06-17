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
    val status: String,            // "pending", "accepted", "completed", "cancelled"
    val ownerPhone: String = "",   // blind user's phone who created the demand
    val volunteerPhone: String = "", // volunteer's phone who accepted
    val volunteerNote: String = "",  // note from volunteer when accepting
    val blindConfirmed: Boolean = false, // blind user confirmed this volunteer
    val demandId: Long = 0,            // linked demand ID (0 = independent run)
    val lat: Double = 0.0,             // location latitude
    val lng: Double = 0.0,             // location longitude
    val trackJson: String = "",        // GPS track as JSON array [[lng,lat],...]
    val remoteId: Int? = null
)
