package com.example.myapplication.mainScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRequiredScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Location permission is required to use this app.")

        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))

        if (permanentlyDenied) {
            Text("Permission is permanently denied. Please enable it in Settings.")
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            androidx.compose.material3.Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        } else {
            Text("Please grant location permission to continue.")
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            androidx.compose.material3.Button(onClick = onRequest) {
                Text("Grant permission")
            }
        }
    }
}
