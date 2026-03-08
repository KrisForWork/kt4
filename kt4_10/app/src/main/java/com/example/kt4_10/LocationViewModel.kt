package com.example.kt4_10

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationState(
    val isLoading: Boolean = false,
    val address: String = "",
    val coordinates: String = "",
    val error: String = ""
)

class LocationViewModel(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : ViewModel() {

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    fun getCurrentLocation() {
        viewModelScope.launch {
            _locationState.value = _locationState.value.copy(
                isLoading = true,
                error = "",
                address = ""
            )

            try {
                // Проверяем разрешения перед вызовом
                if (!checkLocationPermissions()) {
                    _locationState.value = _locationState.value.copy(
                        isLoading = false,
                        error = "Location permission denied"
                    )
                    return@launch
                }

                val location = getFreshLocation()

                // Проверяем Geocoder
                val addresses = try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                } catch (e: Exception) {
                    null
                }

                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not available"

                _locationState.value = _locationState.value.copy(
                    isLoading = false,
                    address = address,
                    coordinates = String.format(
                        "%.6f, %.6f",
                        location.latitude,
                        location.longitude
                    ),
                    error = ""
                )
            } catch (e: SecurityException) {
                _locationState.value = _locationState.value.copy(
                    isLoading = false,
                    error = "Location permission denied"
                )
            } catch (e: LocationTimeoutException) {
                _locationState.value = _locationState.value.copy(
                    isLoading = false,
                    error = "Could not get location. Please try again"
                )
            } catch (e: Exception) {
                _locationState.value = _locationState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return try {
            val fineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            fineLocation || coarseLocation
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(LocationTimeoutException())
                }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }
}

class LocationTimeoutException : Exception()