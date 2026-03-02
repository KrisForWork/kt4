package com.example.kt4_3

import kotlinx.serialization.Serializable

@Serializable
data class Repository(
    val id: Int,
    val full_name: String,
    val description: String?,
    val stargazers_count: Int,
    val language: String?
)