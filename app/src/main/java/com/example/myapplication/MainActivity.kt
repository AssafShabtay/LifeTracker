package com.example.myapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.helpers.InsertExampleDataButton
import com.example.myapplication.helpers.insertExampleData
import com.example.myapplication.mainScreen.CalendarDateSelector
import com.example.myapplication.mainScreen.PieChartComposable
import com.example.myapplication.mainScreen.helpers.PieChartViewModel

import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.ActivityRecognition



class MainActivity : ComponentActivity() {
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)//TODO REMOVE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTransitions()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
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
                Logger.saveLog(applicationContext,
                    "Service Created"
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
    // somewhere in MainActivity (after runtime permissions)
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)//TODO remover
    private fun requestTransitions() {
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

        ActivityRecognition.getClient(this)
            .requestActivityTransitionUpdates(request, pendingIntent)
    }
}



