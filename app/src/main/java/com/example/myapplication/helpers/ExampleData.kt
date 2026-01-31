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

    // Still location (1h)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.52,
            lng = 13.405,
            startTimeDate = Date(now.time - 8 * oneHour),
            endTimeDate = Date(now.time - 6 * oneHour),
            placeName = "Home"
        )
    )

    // Walking to transit (15 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.52,
            startLng = 13.405,
            endLat = 52.523,
            endLng = 13.41,
            startTimeDate = Date(now.time - 6 * oneHour),
            endTimeDate = Date(now.time - 6 * oneHour + fifteenMin)
        )
    )

    // Bus ride (45 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "On Foot",
            startLat = 52.523,
            startLng = 13.41,
            endLat = 52.53,
            endLng = 13.42,
            startTimeDate = Date(now.time - 6 * oneHour + fifteenMin),
            endTimeDate = Date(now.time - 5 * oneHour)
        )
    )

    // Office (3h)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.53,
            lng = 13.42,
            startTimeDate = Date(now.time - 5 * oneHour),
            endTimeDate = Date(now.time - 2 * oneHour),
            placeName = "Office"
        )
    )

    // ===== Afternoon =====

    // Walking to lunch (15 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.53,
            startLng = 13.42,
            endLat = 52.528,
            endLng = 13.418,
            startTimeDate = Date(now.time - 2 * oneHour),
            endTimeDate = Date(now.time - 2 * oneHour + fifteenMin)
        )
    )

    // Restaurant (45 min)
    dao.insertStillLocation(
        StillLocation(
            lat = 52.528,
            lng = 13.418,
            startTimeDate = Date(now.time - 2 * oneHour + fifteenMin),
            endTimeDate = Date(now.time - oneHour)
            ,
            placeName = "Restaurant"
        )
    )

    // Walking back (15 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Walking",
            startLat = 52.528,
            startLng = 13.418,
            endLat = 52.53,
            endLng = 13.42,
            startTimeDate = Date(now.time - oneHour),
            endTimeDate = Date(now.time - oneHour + fifteenMin)
        )
    )

    // ===== Evening =====

    // Driving home (45 min)
    dao.insertMovementActivity(
        MovementActivity(
            activityType = "Driving",
            startLat = 52.53,
            startLng = 13.42,
            endLat = 52.52,
            endLng = 13.405,
            startTimeDate = Date(now.time - oneHour + fifteenMin),
            endTimeDate = now
        )
    )

}
