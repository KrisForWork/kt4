package com.example.kt4_4

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class SocialFeedViewModel(
    private val repository: SocialRepository
) : ViewModel() {

    var posts by mutableStateOf<List<PostWithData>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var loadJob: Job? = null

    fun loadFeed() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            posts = emptyList()

            try {
                val loadedPosts = repository.loadPosts()

                // Сразу показываем посты без данных
                posts = loadedPosts.map { PostWithData(post = it) }

                // Загружаем данные для каждого поста параллельно
                loadedPosts.forEachIndexed { index, post ->
                    launch {
                        val postWithData = loadPostData(post)
                        posts = posts.toMutableList().apply {
                            this[index] = postWithData
                        }
                    }
                    delay(100) // Небольшая задержка для плавности UI
                }
            } catch (e: Exception) {
                // Обработка ошибки загрузки постов
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadPostData(post: Post): PostWithData = supervisorScope {
        val avatarDeferred = async { repository.loadAvatar(post.avatarUrl) }
        val commentsDeferred = async { repository.loadCommentsForPost(post.id) }

        val (avatarState, commentsState) = awaitAll(avatarDeferred, commentsDeferred)

        PostWithData(
            post = post,
            avatarState = avatarState as LoadState<String>,
            commentsState = commentsState as LoadState<List<Comment>>
        )
    }

    fun refresh() {
        loadFeed()
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }
}