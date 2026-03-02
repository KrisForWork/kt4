package com.example.kt4_4

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    val avatarUrl: String
)

@Serializable
data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val body: String
)

sealed class LoadState<out T> {
    object Loading : LoadState<Nothing>()
    data class Ready<T>(val data: T) : LoadState<T>()
    data class Error(val message: String) : LoadState<Nothing>()
}

data class PostWithData(
    val post: Post,
    val avatarState: LoadState<String> = LoadState.Loading,
    val commentsState: LoadState<List<Comment>> = LoadState.Loading
)