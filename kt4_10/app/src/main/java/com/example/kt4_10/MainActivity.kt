package com.example.kt4_10

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kt4_10.ui.theme.Kt4_10Theme
import com.google.accompanist.permissions.*
import com.google.android.gms.location.FusedLocationProviderClient
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Kt4_10Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as MainApplication

    val viewModel: LocationViewModel = viewModel(
        factory = LocationViewModelFactory(
            context = context,
            fusedLocationClient = application.fusedLocationClient,
            geocoder = application.geocoder
        )
    )

    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val permissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "My Location",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        when {
            permissionState.status.isGranted -> {
                LocationContent(
                    state = locationState,
                    onGetLocation = { viewModel.getCurrentLocation() }
                )
            }
            permissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { permissionState.launchPermissionRequest() }
                )
            }
            else -> {
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Request Location Permission")
                }
            }
        }
    }
}

@Composable
fun LocationContent(
    state: LocationState,
    onGetLocation: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onGetLocation,
            enabled = !state.isLoading,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(if (state.isLoading) "Getting location..." else "Get My Address")
        }

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }
            state.error.isNotBlank() -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            state.address.isNotBlank() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = state.address,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Divider()
                        Text(
                            text = state.coordinates,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Location permission is needed to show your address",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

class LocationViewModelFactory(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LocationViewModel(context, fusedLocationClient, geocoder) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}