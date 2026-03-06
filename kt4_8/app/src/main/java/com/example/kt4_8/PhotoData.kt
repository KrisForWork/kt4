package com.example.kt4_8

data class PhotoData(
    val originalUri: String = "content://media/external/images/media/1",
    val compressedPath: String = "",
    val watermarkedPath: String = "",
    val uploadedUrl: String = "",
    val step: Int = 0,
    val progress: Int = 0,
    val error: String = ""
)