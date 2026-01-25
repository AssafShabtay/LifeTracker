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
    /**
     * Service-scoped coroutines.
     * SupervisorJob prevents one failure from cancelling all work.
     */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"

        // Activity types that involve movement, used to check if an activity is a movement activity
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
        dao = ActivityDatabase.getDatabase(applicationContext).activityDao()
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
    // updates current Activity and then start and DB record if it's enter, otherwise closes db activity
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

    /**
     * One-shot location fetch.
     * Uses balanced accuracy to limit battery drain.
     */
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
                    .addOnFailureListener { e ->
                        Log.e(TAG, "getCurrentLocation failed", e)
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
            lat = currentLocation?.latitude,
            lng = currentLocation?.longitude,
            startTimeDate = Date(),

        )
        try {
            currentTrackingId = dao.insertStillLocation(stillLocation)
        }
        catch (e: Exception) {
            Log.e(TAG, "Failed to insert start still location into database and update current location", e)
            currentTrackingId = null
        }
    }



    private suspend fun endStillTracking() {
        currentLocation = getLocationOnce() // Ending location
        val id = currentTrackingId ?: return
        val still = dao.getStillLocationById(id) ?: run {
            Log.e(TAG, "StillLocation missing for id=$id")
            currentTrackingId = null
            return
        }

        val startLat = still.lat
        val startLng = still.lng
        val startTime = still.startTimeDate
        val endTime = Date()
        if (startLat != null && startLng != null && currentLocation?.latitude != null && currentLocation?.longitude != null) {

            val resolvedActivityType = checkIfStillIsMovement(
                startLatitude = startLat,
                startLongitude = startLng,
                endTime =endTime,
                startTime= startTime,
                endLatitude = currentLocation!!.latitude,
                endLongitude = currentLocation!!.longitude
            )

            if (resolvedActivityType  == "still") {
                try {
                    dao.endStillLocation(id, endTime)
                }
                catch (e: Exception) {
                    Log.e(TAG, "Failed to end still location into database", e)
                    currentTrackingId = null
                }

            } else {
                val movement = MovementActivity(
                    activityType = resolvedActivityType,
                    startLat = startLat,
                    startLng = startLng,
                    endLat = currentLocation!!.latitude,
                    endLng = currentLocation!!.longitude,
                    startTimeDate = startTime,
                    endTimeDate = endTime
                )
                try {
                    dao.replaceStillWithMovement(id, movement)
                }
                catch (e: Exception) {
                    Log.e(TAG, "Failed to replace still with movement location into database", e)
                    currentTrackingId = null
                }
            }
        } else {
            try {
                dao.endStillLocation(id, endTime)
            }
            catch (e: Exception) {
                Log.e(TAG, "Failed to end still location into database", e)
                currentTrackingId = null
            }
        }
        currentTrackingId = null
    }

    private fun checkIfStillIsMovement(
        startLatitude: Double,
        startLongitude: Double,
        startTime: Date,
        endTime: Date,
        endLatitude: Double,
        endLongitude: Double,
    ): String {

        val distanceMeters = distanceInMeters(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude
        )

        val durationMillis = endTime.time - startTime.time
        val durationSeconds = if (durationMillis > 0) durationMillis / 1000f else 0f

        val speedMps = if (durationSeconds > 0) {
            distanceMeters / durationSeconds
        } else {
            0f
        } // meters per second


        return when {
            distanceMeters < 100f || speedMps < 0.3f -> "Still"
            speedMps < 2f -> "Walking"
            speedMps < 15f -> "Running"
            else -> "Driving"
        }
    }

    private suspend fun startMovementTracking(activityType: Int){
        currentLocation = getLocationOnce()
        val movementActivity = MovementActivity(
            activityType = getActivityName(activityType),
            startLat = currentLocation?.latitude,
            startLng = currentLocation?.longitude,
            startTimeDate = Date(),
        )
        try {
            currentTrackingId = dao.insertMovementActivity(movementActivity)
        }
        catch (e: Exception) {
            Log.e(TAG, "Failed to update current location and add still location into database", e)
            currentTrackingId = null
        }
    }
    private suspend fun endMovementTracking(){
        val id = currentTrackingId ?: return
        try {
            dao.endMovementActivity(currentTrackingId, currentLocation?.latitude, currentLocation?.longitude, Date())
        }
        catch (e: Exception) {
            Log.e(TAG, "Failed to end movement location into database", e)
            currentTrackingId = null
        }

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
            DetectedActivity.WALKING -> "Walking"
            DetectedActivity.STILL -> "Still"
            DetectedActivity.UNKNOWN -> "Still"
            else -> "Unknown"
        }
    }
}