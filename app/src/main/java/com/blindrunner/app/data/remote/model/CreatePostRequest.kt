package com.blindrunner.app.data.remote.model

/**
 * Request body for POST /posts (JSONPlaceholder).
 * Maps to creating a new running record on the mock API.
 */
data class CreatePostRequest(
    val title: String,
    val body: String,
    val userId: Int
)
