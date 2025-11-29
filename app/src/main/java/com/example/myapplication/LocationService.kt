package com.example.myapplication

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date


class LocationService : Service() {
    private var currentActivity: Int = DetectedActivity.UNKNOWN
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private lateinit var dao: ActivityDao
    private var currentTrackingId: Long? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"
        const val ACTION_ACTIVITY_UPDATE_UI = "com.example.myapplication.ACTIVITY_UPDATE_UI"
        const val EXTRA_ACTIVITY_TYPE = "activity_type"
        const val EXTRA_TRANSITION_TYPE = "transition_type"
        const val EXTRA_ACTIVITY_NAME = "extra_activity_name"
        // Activity types that involve movement
        val MOVEMENT_ACTIVITIES = setOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
            DetectedActivity.ON_FOOT,
            DetectedActivity.ON_BICYCLE
        )
    }
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val db = ActivityDatabase.getDatabase(applicationContext)
        dao = db.activityDao()
        Log.d(TAG, "Service created")

    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        if (intent?.action == ActivityTransitionReceiver.ACTION_ACTIVITY_UPDATE) {
            val activityType = intent.getIntExtra(ActivityTransitionReceiver.EXTRA_ACTIVITY_TYPE, DetectedActivity.UNKNOWN)
            val transitionType = intent.getIntExtra(ActivityTransitionReceiver.EXTRA_TRANSITION_TYPE, -1)
            serviceScope.launch {
                handleActivityUpdate(activityType, transitionType)
            }


        }
        return START_STICKY
    }

    private suspend fun handleActivityUpdate(activityType: Int, transitionType: Int) {

        val enteringActivity = transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        if (activityType == DetectedActivity.UNKNOWN) {
            Log.d(TAG, "Ignoring unknown activity update (raw=$activityType)")

            //TODO update that the app isn't recording when its unknown

            return
        }


        if (enteringActivity) {

            currentActivity = activityType

            when (activityType) {
                DetectedActivity.STILL -> startStillTracking()
                in MOVEMENT_ACTIVITIES -> startMovementTracking(activityType)
            }

        } else {

            when (activityType) {
                DetectedActivity.STILL -> endStillTracking()
                in MOVEMENT_ACTIVITIES -> endMovementTracking()
            }
            currentActivity = DetectedActivity.UNKNOWN
        }
        val activityName = getActivityName(activityType)
        updateNotification()
    }

    private fun updateNotification() {
        TODO("Not yet implemented")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun getLocationOnce(): Location? =
        suspendCancellableCoroutine { cont ->
            try {
                fusedLocationClient //TODO add permissions
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    )
                    .addOnSuccessListener { loc ->
                        if (!cont.isCompleted) cont.resume(loc) {}
                    }
                    .addOnFailureListener {
                        if (!cont.isCompleted) cont.resume(null) {}
                    }
            } catch (e: Exception) {
                if (!cont.isCompleted) cont.resume(null) {}
            }
        }

    private fun startForeground() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    private fun buildNotification(): Notification {

        TODO()
    }
    // End and start activities
    private suspend fun startStillTracking(){
        currentLocation = getLocationOnce()
        val stillLocation = StillLocation(
            latitude = currentLocation?.latitude,
            longitude = currentLocation?.longitude,
            startTimeDate = Date(),

        )
        currentTrackingId = dao.insertStillLocation(stillLocation)
    }
    private suspend fun endStillTracking() {
        currentLocation = getLocationOnce()
        val id = currentTrackingId ?: return
        val still = dao.getStillLocationById(id)
        val startLatitude = still?.latitude
        val startLongitude = still?.longitude
        val startTime = still?.startTimeDate
        val endTime = Date()
        if (startLatitude != null && startLongitude != null && currentLocation?.latitude != null && currentLocation?.longitude != null) {


            val distanceMeters = distanceInMeters(
                startLatitude,
                startLongitude,
                currentLocation!!.latitude,
                currentLocation!!.longitude
            )

            val durationMillis = endTime.time - (startTime?.time ?: return)
            val durationSeconds = if (durationMillis > 0) durationMillis / 1000f else 0f
            val speedMps = if (durationSeconds > 0) {
                distanceMeters / durationSeconds
            } else 0f // Meter per second

            val inferredActivity = when {
                distanceMeters < 100f || speedMps < 0.3f -> "still"
                speedMps < 2f -> "walking"
                speedMps < 15f -> "running"
                else -> "car"
            }
            if (inferredActivity == "still") {
                dao.endStillLocation(id, endTime)
            } else {
                dao.deleteMovementActivity(id)

                val movement = MovementActivity(
                    activityType = inferredActivity,
                    startLatitude = startLatitude,
                    startLongitude = startLongitude,
                    endLatitude = currentLocation!!.latitude,
                    endLongitude = currentLocation!!.longitude,
                    startTimeDate = startTime,
                    endTimeDate = endTime
                )
                dao.insertMovementActivity(movement)
            }
        } else {
            dao.endStillLocation(id, endTime)
        }
        currentTrackingId = null
    }
    private suspend fun startMovementTracking(activityType: Int){
        currentLocation = getLocationOnce()
        val movementActivity = MovementActivity(
            activityType = getActivityName(activityType),
            startLatitude = currentLocation?.latitude,
            startLongitude = currentLocation?.longitude,
            startTimeDate = Date(),
        )
        currentTrackingId = dao.insertMovementActivity(movementActivity)
    }
    private suspend fun endMovementTracking(){
        val id = currentTrackingId ?: return
        dao.endMovementActivity(currentTrackingId, currentLocation?.latitude, currentLocation?.longitude, Date())
        currentTrackingId = null
    }


    //Tracking

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            currentLocation = loc
        }
    }



    // Helpers

    fun distanceInMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0] // meters
    }
    private fun getActivityName(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "Driving"
            DetectedActivity.ON_BICYCLE -> "Cycling"
            DetectedActivity.ON_FOOT -> "On Foot"
            DetectedActivity.RUNNING -> "Running"
            DetectedActivity.STILL -> "Still"
            DetectedActivity.WALKING -> "Walking"
            DetectedActivity.UNKNOWN -> "Still"
            else -> "Unknown"
        }
    }
}