package com.blindrunner.app.domain.model

data class Demand(
    val id: Long = 0,
    val date: String,
    val time: String,
    val location: String,
    val durationMinutes: Int,
    val distanceKm: Float = 0f,
    val note: String = "",
    val status: DemandStatus = DemandStatus.PENDING,
    val volunteerPhone: String? = null
)

enum class DemandStatus { PENDING, ACCEPTED, COMPLETED, CANCELLED }
