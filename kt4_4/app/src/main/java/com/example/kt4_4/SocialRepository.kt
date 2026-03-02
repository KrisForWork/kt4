package com.example.kt4_4

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.random.Random

class SocialRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadPosts(): List<Post> = withContext(Dispatchers.IO) {
        delay(500) // Имитация загрузки
        val jsonString = context.assets.open("social_posts.json")
            .bufferedReader().use { it.readText() }
        json.decodeFromString<List<Post>>(jsonString)
    }

    suspend fun loadCommentsForPost(postId: Int): LoadState<List<Comment>> =
        withContext(Dispatchers.IO) {
            try {
                delay(800 + Random.nextLong(400)) // Случайная задержка 800-1200ms

                // 10% шанс ошибки для демонстрации
                if (Random.nextInt(100) < 10) {
                    throw Exception("Network error")
                }

                val jsonString = context.assets.open("comments.json")
                    .bufferedReader().use { it.readText() }
                val allComments = json.decodeFromString<List<Comment>>(jsonString)
                LoadState.Ready(allComments.filter { it.postId == postId })
            } catch (e: Exception) {
                LoadState.Error("Failed to load comments: ${e.message}")
            }
        }

    suspend fun loadAvatar(url: String): LoadState<String> = withContext(Dispatchers.IO) {
        try {
            delay(600 + Random.nextLong(400)) // Случайная задержка 600-1000ms

            // 10% шанс ошибки для демонстрации
            if (Random.nextInt(100) < 10) {
                throw Exception("Failed to load image")
            }

            LoadState.Ready(url)
        } catch (e: Exception) {
            LoadState.Error("Avatar load failed")
        }
    }
}