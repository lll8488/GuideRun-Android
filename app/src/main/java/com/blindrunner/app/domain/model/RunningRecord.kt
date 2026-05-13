package com.blindrunner.app.domain.model

/**
 * Domain-layer representation of a running record.
 * Decoupled from both the Room entity and the API response model.
 */
data class RunningRecord(
    val id: Long = 0,
    val date: String,
    val durationMinutes: Int,
    val location: String,
    val distanceKm: Float,
    val status: String
)
