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
    val oneHour = 60 * 60 * 1000L
    val fifteenMin = 15 * 60 * 1000L
    val thirtyMin = 30 * 60 * 1000L
    val fortyFiveMin = 45 * 60 * 1000L
    val oneDay = 24 * oneHour

    // ==========================================
    // TODAY (Original Data)
    // ==========================================

    // Still location (Home - Morning)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.52,
            lng = 13.405,
            startTimeDate = Date(now.time - 8 * oneHour),
            endTimeDate = Date(now.time - 6 * oneHour),
            placeName = "Home"
        )
    )

    // Walking to transit
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.52, startLng = 13.405,
            endLat = 52.523, endLng = 13.41,
            startTimeDate = Date(now.time - 6 * oneHour),
            endTimeDate = Date(now.time - 6 * oneHour + fifteenMin)
        )
    )

    // Bus ride (incorrectly labeled "On Foot" in original, kept as is or changed to Vehicle if you prefer)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "On Foot", // Kept consistent with your original code
            startLat = 52.523, startLng = 13.41,
            endLat = 52.53, endLng = 13.42,
            startTimeDate = Date(now.time - 6 * oneHour + fifteenMin),
            endTimeDate = Date(now.time - 5 * oneHour)
        )
    )

    // Office
    dao.insertStillLocation(
        StillLocation(
            lat = 52.53,
            lng = 13.42,
            startTimeDate = Date(now.time - 5 * oneHour),
            endTimeDate = Date(now.time - 2 * oneHour),
            placeName = "Office"
        )
    )

    // Walking to lunch
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.53, startLng = 13.42,
            endLat = 52.528, endLng = 13.418,
            startTimeDate = Date(now.time - 2 * oneHour),
            endTimeDate = Date(now.time - 2 * oneHour + fifteenMin)
        )
    )

    // Restaurant
    dao.insertStillLocation(
        StillLocation(
            lat = 52.528,
            lng = 13.418,
            startTimeDate = Date(now.time - 2 * oneHour + fifteenMin),
            endTimeDate = Date(now.time - oneHour),
            placeName = "Restaurant"
        )
    )

    // Walking back
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.528, startLng = 13.418,
            endLat = 52.53, endLng = 13.42,
            startTimeDate = Date(now.time - oneHour),
            endTimeDate = Date(now.time - oneHour + fifteenMin)
        )
    )

    // Driving home
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Driving",
            startLat = 52.53, startLng = 13.42,
            endLat = 52.52, endLng = 13.405,
            startTimeDate = Date(now.time - oneHour + fifteenMin),
            endTimeDate = now
        )
    )

    // ==========================================
    // YESTERDAY (1 Day Ago) - "Work from Home + Jog"
    // ==========================================
    val yesterdayBase = now.time - oneDay

    // Home all morning (WFH)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.52,
            lng = 13.405,
            startTimeDate = Date(yesterdayBase - 9 * oneHour),
            endTimeDate = Date(yesterdayBase - 3 * oneHour),
            placeName = "Home"
        )
    )

    // Afternoon Jog (Running)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Running",
            startLat = 52.52, startLng = 13.405,
            endLat = 52.54, endLng = 13.415, // Run to park
            startTimeDate = Date(yesterdayBase - 3 * oneHour),
            endTimeDate = Date(yesterdayBase - 2 * oneHour)
        )
    )

    // Park (Short break)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.54,
            lng = 13.415,
            startTimeDate = Date(yesterdayBase - 2 * oneHour),
            endTimeDate = Date(yesterdayBase - 2 * oneHour + thirtyMin),
            placeName = "Park"
        )
    )

    // Jog back
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Running",
            startLat = 52.54, startLng = 13.415,
            endLat = 52.52, endLng = 13.405,
            startTimeDate = Date(yesterdayBase - 2 * oneHour + thirtyMin),
            endTimeDate = Date(yesterdayBase - oneHour)
        )
    )

    // ==========================================
    // 2 DAYS AGO - "Supermarket Trip"
    // ==========================================
    val twoDaysAgoBase = now.time - (2 * oneDay)

    // Home
    dao.insertStillLocation(
        StillLocation(
            lat = 52.52,
            lng = 13.405,
            startTimeDate = Date(twoDaysAgoBase - 5 * oneHour),
            endTimeDate = Date(twoDaysAgoBase - 4 * oneHour),
            placeName = "Home"
        )
    )

    // Drive to Supermarket
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Driving",
            startLat = 52.52, startLng = 13.405,
            endLat = 52.51, endLng = 13.39,
            startTimeDate = Date(twoDaysAgoBase - 4 * oneHour),
            endTimeDate = Date(twoDaysAgoBase - 4 * oneHour + fifteenMin)
        )
    )

    // Shopping
    dao.insertStillLocation(
        StillLocation(
            lat = 52.51,
            lng = 13.39,
            startTimeDate = Date(twoDaysAgoBase - 4 * oneHour + fifteenMin),
            endTimeDate = Date(twoDaysAgoBase - 3 * oneHour),
            placeName = "Supermarket"
        )
    )

    // Drive back
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Driving",
            startLat = 52.51, startLng = 13.39,
            endLat = 52.52, endLng = 13.405,
            startTimeDate = Date(twoDaysAgoBase - 3 * oneHour),
            endTimeDate = Date(twoDaysAgoBase - 3 * oneHour + fifteenMin)
        )
    )
}