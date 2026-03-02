package com.example.kt4_4

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class SocialFeedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(SocialFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialFeedViewModel(SocialRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}