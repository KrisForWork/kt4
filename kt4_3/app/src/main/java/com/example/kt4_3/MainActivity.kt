package com.example.kt4_3


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.kt4_3.ui.theme.Kt4_3Theme

class MainActivity : ComponentActivity() {

    private lateinit var repositoryDataSource: RepositoryDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repositoryDataSource = RepositoryDataSource(this)

        setContent {
            Kt4_3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchScreen(
                        dataSource = repositoryDataSource
                    )
                }
            }
        }
    }
}