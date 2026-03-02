package com.example.kt4_21

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kt4_21.ui.theme.Kt4_21Theme
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Kt4_21Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DuplicateFinderScreen()
                }
            }
        }
    }
}

@Composable
fun DuplicateFinderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var duplicates by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var timeMs by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        copyAssetsToCache(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Поиск дубликатов JSON файлов",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = "2",
            onValueChange = {},
            label = { Text("Таймаут (секунды)") },
            enabled = false,
            readOnly = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    message = ""
                    duplicates = emptyList()

                    val result = DuplicateFinder.findDuplicates(
                        directory = context.cacheDir,
                        timeoutSeconds = 2L
                    )

                    duplicates = result.duplicates
                    message = result.message
                    timeMs = result.timeMs
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Поиск..." else "Найти дубликаты")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = if (message.contains("прерван")) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Text(text = "Время: ${timeMs}ms")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(duplicates) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "SHA-256: ${group.first().take(16)}...",
                            style = MaterialTheme.typography.labelSmall
                        )
                        group.forEach { file ->
                            Text(text = "  • ${File(file).name}")
                        }
                    }
                }
            }
        }
    }
}

private suspend fun copyAssetsToCache(context: android.content.Context) {
    withContext(Dispatchers.IO) {
        val assets = context.assets
        val files = assets.list("") ?: return@withContext

        files.filter { it.endsWith(".json") }.forEach { fileName ->
            try {
                val inputStream = assets.open(fileName)
                val outputFile = File(context.cacheDir, fileName)
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}