package com.example.kt4_4

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class) // Добавляем аннотацию для TopAppBar
@Composable
fun SocialFeedScreen(viewModel: SocialFeedViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Social Feed") },
            actions = {
                Button(
                    onClick = { viewModel.refresh() },
                    enabled = !viewModel.isLoading
                ) {
                    Text(if (viewModel.isLoading) "Loading..." else "Refresh")
                }
            }
        )

        if (viewModel.posts.isEmpty() && viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(viewModel.posts) { postWithData ->
                    PostCard(postWithData)
                }
            }
        }
    }
}