package com.example.kt4_3

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*

@Composable
fun SearchScreen(
    dataSource: RepositoryDataSource,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var searchJob: Job? by remember { mutableStateOf(null) }

    fun <T> debounce(
        waitMs: Long = 500L,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        return { param: T ->
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(waitMs)
                destinationFunction(param)
            }
        }
    }

    val debouncedSearch = remember {
        debounce<String> { query ->
            searchJob = coroutineScope.launch {
                isLoading = true
                errorMessage = null

                try {
                    val results = async { dataSource.searchRepositories(query) }
                    searchResults = results.await()
                } catch (e: CancellationException) {
                    // Поиск был отменён - игнорируем
                } catch (e: Exception) {
                    errorMessage = "Ошибка загрузки: ${e.message}"
                    searchResults = emptyList()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newQuery ->
                searchQuery = newQuery
                debouncedSearch(newQuery)
            },
            label = { Text("Поиск репозиториев...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(searchResults) { repo ->
                RepositoryItem(repo = repo)
            }

            if (searchResults.isEmpty() && !isLoading && errorMessage == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank())
                                "Введите запрос для поиска"
                            else
                                "Ничего не найдено",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryItem(repo: Repository) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = repo.full_name,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            repo.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repo.language?.let {
                    AssistChip(
                        onClick = {},
                        label = { Text(it) }
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text("⭐ ${repo.stargazers_count}") }
                )
            }
        }
    }
}