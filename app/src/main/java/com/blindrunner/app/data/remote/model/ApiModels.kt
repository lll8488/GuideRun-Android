package com.blindrunner.app.data.remote.model

import com.google.gson.annotations.SerializedName

// ====== 请求体 ======

data class SendCodeRequest(val phone: String)

data class LoginRequest(val phone: String, val code: String)

data class RegisterRequest(
    val phone: String,
    val password: String,
    val userType: String
)

data class DemandRequest(
    val ownerPhone: String,
    val date: String,
    val time: String,
    val location: String,
    val durationMinutes: Int,
    val distanceKm: Float,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class AcceptDemandRequest(
    val volunteerPhone: String,
    val note: String
)

data class RunningRecordRequest(
    val ownerPhone: String,
    val date: String,
    val durationMinutes: Int,
    val location: String,
    val distanceKm: Float,
    val demandId: Long = 0,
    val volunteerPhone: String = "",
    val trackJson: String = ""
)

data class ExamSubmitRequest(val phone: String, val score: Int)

// ====== 响应体 ======

data class ApiResponse(
    val success: Boolean = false,
    val message: String = "",
    val id: Long = 0,
    val passed: Boolean? = null,
    val score: Int? = null
)

data class LoginResponse(
    val success: Boolean = false,
    val newUser: Boolean = false,
    val phone: String = "",
    val message: String = "",
    val user: ServerUser? = null
)

data class ServerUser(
    val phone: String = "",
    val name: String = "",
    val userType: String = "",
    val rating: Float = 0f,
    val totalRuns: Int = 0,
    val totalDistanceKm: Float = 0f,
    val examPassed: Boolean = false,
    val examScore: Int = 0
)

data class DemandListResponse(
    val success: Boolean = false,
    val demands: List<ServerDemand> = emptyList()
)

data class ServerDemand(
    val id: Long = 0,
    val ownerPhone: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val durationMinutes: Int = 0,
    val distanceKm: Float = 0f,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "pending",
    val volunteerPhone: String = "",
    val volunteerNote: String = "",
    val blindConfirmed: Boolean = false
)

data class RunningRecordListResponse(
    val success: Boolean = false,
    val records: List<ServerRunningRecord> = emptyList()
)

data class ServerRunningRecord(
    val id: Long = 0,
    val date: String = "",
    val durationMinutes: Int = 0,
    val location: String = "",
    val distanceKm: Float = 0f,
    val demandId: Long = 0,
    val trackJson: String = ""
)

data class LeaderboardResponse(
    val success: Boolean = false,
    val leaderboard: List<LeaderboardEntry> = emptyList()
)

data class LeaderboardEntry(
    val phone: String = "",
    val name: String = "",
    val totalRuns: Int = 0,
    val rating: Float = 0f,
    @SerializedName("ratingCount")
    val ratingCount: Int = 0
)
