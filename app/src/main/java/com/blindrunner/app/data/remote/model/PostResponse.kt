package com.blindrunner.app.data.remote.model

/**
 * JSONPlaceholder /posts response.
 * Used to represent a running record fetched from the mock API.
 */
data class PostResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)
