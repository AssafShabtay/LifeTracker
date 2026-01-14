package com.example.myapplication.helpers

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.ActivityDao
import com.example.myapplication.MovementActivity
import com.example.myapplication.StillLocation
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun InsertExampleDataButton(dao: ActivityDao) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                insertExampleData(dao)
            }
        }
    ) {
        Text("Insert Example Data")
    }
}
suspend fun insertExampleData(dao: ActivityDao) {
    val now = Date()
    val oneHour = 60 * 60 * 1000

    // Still location (1h)
    dao.insertStillLocation(
        StillLocation(
            latitude = 52.52,
            longitude = 13.405,
            startTimeDate = Date(now.time - 4 * oneHour),
            endTimeDate = Date(now.time - 3 * oneHour),
            placeName = "Home"
        )
    )

    // Walking (30 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLatitude = 52.52,
            startLongitude = 13.405,
            endLatitude = 52.525,
            endLongitude = 13.41,
            startTimeDate = Date(now.time - 3 * oneHour),
            endTimeDate = Date(now.time - (2.5 * oneHour).toLong())
        )
    )

    // Driving (1h)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Driving",
            startLatitude = 52.525,
            startLongitude = 13.41,
            endLatitude = 52.55,
            endLongitude = 13.45,
            startTimeDate = Date(now.time - 2 * oneHour),
            endTimeDate = Date(now.time - oneHour)
        )
    )
}
