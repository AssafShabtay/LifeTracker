package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.helpers.InsertExampleDataButton
import com.example.myapplication.helpers.insertExampleData
import com.example.myapplication.mainScreen.CalendarDateSelector
import com.example.myapplication.mainScreen.PermissionRequiredScreen
import com.example.myapplication.mainScreen.PieChartComposable
import com.example.myapplication.mainScreen.helpers.PieChartViewModel

import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie


class                                                                                                               MainActivity : ComponentActivity() {


    //-----------------------------Permissions----------------------------

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { result ->
            val allGranted = result.values.all { it }
            if (allGranted) {
                return@registerForActivityResult //If all permissions granted, continue
            }

            //filter map so only denied permissions are left
            val deniedPermissions = result.filterValues { granted -> !granted }.keys

            // True if the user denied permission before but didn't choose "Don't ask again".
            // False on first request or when permission is permanently denied.
            val shouldShowRationale = deniedPermissions.any { perm ->
                shouldShowRequestPermissionRationale(perm)
            }

            if (shouldShowRationale) {
                Log.d("Permissions", "Should show rationale")
                showPermissionRationaleDialog(
                    onRetry = { requestPermissions() },
                    onCancel = { onPermissionsDenied() }
                )
            } else {
                Log.d("Permissions", " NOT Should show rationale")
                // User either checked "Don't ask again" OR policy/device blocks it.
                showGoToSettingsDialog(
                    onOpenSettings = { openAppSettings() },
                    onCancel = { onPermissionsDenied() }
                )
            }
        }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }


    private fun onPermissionsDenied() {
        Log.d("Permissions", "Permissions denied")
    }

    private fun showPermissionRationaleDialog(onRetry: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Permission required")
            .setMessage("We need these permissions to use the camera feature. Please allow them.")
            .setPositiveButton("Allow") { _, _ -> onRetry() }
            .setNegativeButton("Not now") { _, _ -> onCancel() }
            .show()
    }



    private fun showGoToSettingsDialog(onOpenSettings: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Enable permissions in Settings")
            .setMessage("Permissions are denied permanently. Please enable them in Settings to continue.")
            .setPositiveButton("Open Settings") { _, _ -> onOpenSettings() }
            .setNegativeButton("Cancel") { _, _ -> onCancel() }
            .show()
    }


    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null)
        )
        startActivity(intent)
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun isPermanentlyDenied(permission: String): Boolean {
        val denied = ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        val noRationale = !shouldShowRequestPermissionRationale(permission)
        return denied && noRationale
    }

    private fun isAnyPermissionPermanentlyDenied(): Boolean =
        requiredPermissions.any { isPermanentlyDenied(it) }


    //Request activity receiver to start
    private fun requestTransitions() {
        if (!hasAllPermissions()) {
            Log.w("ActivityRecognition", "Aborting requestTransitions: Permissions not fully granted.")
            return
        }
        val transitions = listOf(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.ON_FOOT
        ).flatMap { type ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(type)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent)
                .addOnFailureListener { e ->
                    Log.e("ActivityRecognition", "Registration failed.", e)
                }
        } catch (e: SecurityException) {
            Log.e("ActivityRecognition", "SecurityException: Missing permission for transitions", e)
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {

                var hasPerms by remember {
                    mutableStateOf(hasAllPermissions())
                }

                // Ask once on first entry if missing
                LaunchedEffect(Unit) {
                    if (!hasPerms) requestPermissions()
                }
                LaunchedEffect(hasPerms) {
                    if (hasPerms) {
                        // runs when permissions are fully granted
                        requestTransitions()

                        // start the service to show the Idle notification
                        val intent = Intent(this@MainActivity, LocationService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }
                    }
                }

                // Re check when coming back from settings
                DisposableEffect(Unit) {//TODO CHECK WHATS THAT MEANS
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPerms = hasAllPermissions()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                if (!hasPerms) {
                    // Blocking screen until all permissions are granted
                    PermissionRequiredScreen(
                        permanentlyDenied = isAnyPermissionPermanentlyDenied(),
                        onRequest = { requestPermissions() },
                        onOpenSettings = { openAppSettings() }
                    )
                    return@MyApplicationTheme
                }

                val dao = ActivityDatabase
                    .getDatabase(applicationContext)
                    .activityDao()

                val viewModel: PieChartViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return PieChartViewModel(dao) as T
                        }
                    }
                )
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        InsertExampleDataButton(dao)
                        CalendarDateSelector(
                            selectedDate = viewModel.selectedDate,
                            onDateSelected = { newDate ->
                                viewModel.loadDataForDay(newDate)
                            },
                            viewModel
                        )
                        PieChartComposable(viewModel)
                    }
                }
            }
        }

    }
}



