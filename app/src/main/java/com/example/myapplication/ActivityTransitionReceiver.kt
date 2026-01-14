    package com.example.myapplication

    import android.content.BroadcastReceiver
    import android.content.Context
    import android.content.Intent
    import android.os.Build
    import com.google.android.gms.location.ActivityTransition
    import com.google.android.gms.location.ActivityTransitionEvent
    import com.google.android.gms.location.ActivityTransitionResult
    import com.google.android.gms.location.DetectedActivity
    import android.app.Service
    import android.os.IBinder

    class ActivityTransitionReceiver : BroadcastReceiver() {

        companion object {
            const val ACTION_ACTIVITY_UPDATE = "com.example.myapplication.ACTIVITY_UPDATE"
            const val EXTRA_ACTIVITY_TYPE = "activity_type"
            const val EXTRA_TRANSITION_TYPE = "transition_type"
        }



        override fun onReceive(context: Context, intent: Intent) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            handleActivityTransitions(context, result)
        }

        private fun handleActivityTransitions(context: Context, result: ActivityTransitionResult) {
            for (event in result.transitionEvents) {
                val activityType = handleUnkownActivity(event.activityType) ?: continue

                when (activityType) {
                    DetectedActivity.IN_VEHICLE -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleDrivingStarted(context) else handleDrivingStopped(context)
                    DetectedActivity.RUNNING -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleRunningStarted(context) else handleRunningStopped(context)
                    DetectedActivity.WALKING -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleWalkingStarted(context) else handleWalkingStopped(context)
                    DetectedActivity.ON_BICYCLE -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleCyclingStarted(context) else handleCyclingStopped(context)
                    DetectedActivity.STILL -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleStillStarted(context) else handleStillStopped(context)
                    DetectedActivity.ON_FOOT -> if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) handleOnFootStarted(context) else handleOnFootStopped(context)
                }

                notifyLocationService(context, event, activityType)
            }
        }

        private fun notifyLocationService(context: Context, event: ActivityTransitionEvent, activityType: Int) {

            val serviceIntent = Intent(context, LocationService::class.java).apply {
                action = ACTION_ACTIVITY_UPDATE
                putExtra(EXTRA_ACTIVITY_TYPE, activityType)
                putExtra(EXTRA_TRANSITION_TYPE, event.transitionType)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        private fun handleDrivingStarted(context: Context) {
        }
        private fun handleDrivingStopped(context: Context) {}
        private fun handleRunningStarted(context: Context) {}
        private fun handleRunningStopped(context: Context) {}
        private fun handleWalkingStarted(context: Context) {}
        private fun handleWalkingStopped(context: Context) {}
        private fun handleCyclingStarted(context: Context) {}
        private fun handleCyclingStopped(context: Context) {}
        private fun handleStillStarted(context: Context) {}
        private fun handleStillStopped(context: Context) {}
        private fun handleOnFootStarted(context: Context) {}
        private fun handleOnFootStopped(context: Context) {}

        private fun handleUnkownActivity(activityType: Int): Int? {
            return if (activityType == DetectedActivity.UNKNOWN) DetectedActivity.STILL else activityType
        }
    }