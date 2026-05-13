package com.blindrunner.app.data.remote.model

/**
 * JSONPlaceholder /users response.
 */
data class UserResponse(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val phone: String,
    val website: String
)
