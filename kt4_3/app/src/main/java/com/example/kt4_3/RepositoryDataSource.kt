package com.example.kt4_3

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class RepositoryDataSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchRepositories(query: String): List<Repository> = withContext(Dispatchers.IO) {
        delay(800)

        try {
            val jsonString = context.assets.open("github_repos.json")
                .bufferedReader()
                .use { it.readText() }

            val allRepos = json.decodeFromString<List<Repository>>(jsonString)

            if (query.isBlank()) {
                allRepos
            } else {
                allRepos.filter { repo ->
                    repo.full_name.contains(query, ignoreCase = true) ||
                            (repo.description?.contains(query, ignoreCase = true) == true) ||
                            (repo.language?.contains(query, ignoreCase = true) == true)
                }
            }
        } catch (e: IOException) {
            emptyList()
        }
    }
}