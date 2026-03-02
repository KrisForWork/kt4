package com.example.kt4_21

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

data class FindResult(
    val duplicates: List<List<String>>,
    val message: String,
    val timeMs: Long
)

object DuplicateFinder {

    suspend fun findDuplicates(
        directory: File,
        timeoutSeconds: Long
    ): FindResult {
        val timeMs = measureTimeMillis {
        }

        val result = withTimeoutOrNull(timeoutSeconds * 1000) {
            val files = directory.walk()
                .filter { it.isFile && it.extension == "json" }
                .toList()

            if (files.isEmpty()) {
                return@withTimeoutOrNull FindResult(
                    duplicates = emptyList(),
                    message = "JSON файлы не найдены",
                    timeMs = timeMs
                )
            }

            val mutex = Mutex()
            val hashToFiles = mutableMapOf<String, MutableList<String>>()

            coroutineScope {
                files.map { file ->
                    async(Dispatchers.IO) {
                        val hash = calculateSha256(file)
                        mutex.withLock {
                            hashToFiles.getOrPut(hash) { mutableListOf() }
                                .add(file.absolutePath)
                        }
                    }
                }.awaitAll()
            }

            val duplicates = hashToFiles.values
                .filter { it.size > 1 }
                .sortedByDescending { it.size }

            FindResult(
                duplicates = duplicates,
                message = if (duplicates.isEmpty())
                    "Дубликаты не найдены"
                else
                    "Найдено ${duplicates.size} групп дубликатов",
                timeMs = timeMs
            )
        }

        return result ?: FindResult(
            duplicates = emptyList(),
            message = "Поиск прерван по таймауту ($timeoutSeconds сек)",
            timeMs = timeMs
        )
    }

    private fun calculateSha256(file: File): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
    }
}