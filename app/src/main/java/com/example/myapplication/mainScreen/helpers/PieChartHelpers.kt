package com.example.myapplication.mainScreen.helpers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.Color
import com.example.myapplication.ActivityDao
import com.example.myapplication.MovementActivity
import com.example.myapplication.StillLocation

import java.util.Calendar
import java.util.Date

sealed class TimelineItem { //Class to organize data into timeline
    data class Still(val item: StillLocation): TimelineItem()
    data class Movement(val item: MovementActivity): TimelineItem()

    //For the time that is not filled in the day
    data class Remaining(
        val startTimeDate: Date?,
        val endTimeDate: Date,
    ) : TimelineItem()
}
enum class PieType{
    Movement,
    Still,
    Remaining
}
private const val TOTAL_MINUTES = 1440.0
private const val FULL_CIRCLE_DEG = 360.0
private const val MIN_ANGLE_DEG = 25.0

fun getDayRange(date: Date = Date()): Pair<Date, Date> {
    // returns  the range of a specific date
    val cal = Calendar.getInstance()
    cal.time = date

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.time

    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val end = cal.time

    return start to end
}

fun totalDurationMinutes(timeline: List<TimelineItem>, dayStart: Date, dayEnd: Date): Int {
    return timeline.sumOf { item ->
        when (item) {
            is TimelineItem.Still ->
                durationMinutes(
                    item.item.startTimeDate,
                    item.item.endTimeDate,
                    dayStart,
                    dayEnd,

                )
            is TimelineItem.Movement ->
                durationMinutes(
                    item.item.startTimeDate,
                    item.item.endTimeDate,
                    dayStart,
                    dayEnd,

                )
            is TimelineItem.Remaining ->
                durationMinutes(item.startTimeDate, item.endTimeDate, dayStart, dayEnd)
        }
    }
}

suspend fun getTimelineForRange(dao: ActivityDao, startOfDay: Date, endOfDay: Date): List<TimelineItem> {
    //Get the data from today

    val still = dao.getStillForRange(startOfDay, endOfDay)
        .map{TimelineItem.Still(it)}

    val movement = dao.getMovementForRange(startOfDay, endOfDay)
        .map{TimelineItem.Movement(it)}

    val timeline = (still + movement)
        .sortedBy {
            when (it) {
                is TimelineItem.Still -> it.item.startTimeDate
                is TimelineItem.Movement -> it.item.startTimeDate
                is TimelineItem.Remaining -> it.startTimeDate
            }
        }


        if (totalDurationMinutes(timeline, startOfDay, endOfDay)<1440){

            val lastEndTime = timeline
                .lastOrNull { it is TimelineItem.Still || it is TimelineItem.Movement }
                ?.let {
                    when (it) {
                        is TimelineItem.Still -> it.item.endTimeDate
                        is TimelineItem.Movement -> it.item.endTimeDate
                        else -> null
                    }
                } ?: startOfDay

            val customSlice = TimelineItem.Remaining(
            startTimeDate = lastEndTime,
            endTimeDate = endOfDay,
        )
        return timeline + customSlice
}

    return timeline
}
fun durationMinutes(start: Date?, end: Date?, startOfDay: Date? = null, endOfDay: Date? = null): Int {
    // Return the duration of the activity in minutes
    if (start == null) return 0

    val actualEnd = end ?: Date()
    // if activity starts before 00:00, change the start time to 00:00
    if (startOfDay != null && endOfDay != null) {
        if (start.time !in startOfDay.time..endOfDay.time) {
            return ((actualEnd.time - startOfDay.time)
                .coerceAtLeast(0) / 1000 / 60).toInt()
        }
        // if activity ends after 00:00, change the end time to 00:00
        if (actualEnd.time !in startOfDay.time..endOfDay.time) {
            return ((endOfDay.time - start.time)
                .coerceAtLeast(0) / 1000 / 60).toInt()
        }
    }
    return ((actualEnd.time - start.time)
        .coerceAtLeast(0) / 1000 / 60).toInt()
}

fun pieDataFromTimeline(timeline: List<TimelineItem>,selectedDate: Date): List<Pie> {
    //Converts the timeline into pie data
    //TODO instead of rebuilding the data each time, we can just update the pie chart
    //TODO ADD COLORS
    val (startOfDay, endOfDay) = getDayRange(selectedDate)
    return normalizePieByAngle(timeline.mapIndexed { index, item ->

        val duration = when (item) {
            is TimelineItem.Still -> durationMinutes(
                item.item.startTimeDate,
                item.item.endTimeDate,
                null,
                null
            )

            is TimelineItem.Movement -> durationMinutes(
                item.item.startTimeDate,
                item.item.endTimeDate,
                null,
                null
            )
            is TimelineItem.Remaining -> durationMinutes(
                item.startTimeDate,
                item.endTimeDate,
                null,
                null

            )
        }



        //TODO(" COLOR")
        //if(item is TimelineItem.Still)
        val baseColor =  when (item) {
            is TimelineItem.Still -> Color.Gray
            is TimelineItem.Movement -> Color(0xFF4CAF50)
            is TimelineItem.Remaining -> Color(0xFFE0E0E0)
    }
        val icon =  when (item) {
            is TimelineItem.Still -> Icons.Filled.Home
            is TimelineItem.Movement -> when (item.item.activityType) {
                "Driving" -> Icons.Filled.DirectionsCar
                "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                "On Foot" -> Icons.AutoMirrored.Filled.DirectionsWalk
                "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                else -> null
            }

            is TimelineItem.Remaining -> null
        }
        val latLng = when (item) {
            is TimelineItem.Still -> item.item.lat to item.item.lng
            is TimelineItem.Movement -> item.item.startLat to item.item.startLng
            else -> null
        }
        val endLatLng = when(item){
            is TimelineItem.Movement -> item.item.endLat to item.item.endLng
            else -> null
        }
        val pieType = when (item) {
            is TimelineItem.Still -> PieType.Still
            is TimelineItem.Movement -> PieType.Movement
            is TimelineItem.Remaining -> PieType.Remaining
        }
        Pie(
            label = "...",
            data = duration,
            color = baseColor,
            lat = latLng?.first,
            lng = latLng?.second,
            endLat = endLatLng?.first,
            endLng = endLatLng?.second,
            durationText = if (item is TimelineItem.Remaining) null
            else minutesToTimeStamp(duration),
            icon = icon,
            type = pieType,
            selectedColor = baseColor.copy(alpha = 0.85f),
            clickable = item !is TimelineItem.Remaining

        )
    })
}
private fun minutesToTimeStamp(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours == 0) {
        "${remainingMinutes}m"
        } else if (remainingMinutes == 0) {
        "${hours}h"
    } else {
        "${hours}h ${remainingMinutes}m"
    }
}
private fun normalizePieByAngle(raw: List<Pie>): List<Pie> {

    val rawAngles = raw.map {
        (it.data / TOTAL_MINUTES) * FULL_CIRCLE_DEG
    }

    val minTotal = raw.size * MIN_ANGLE_DEG

    // Case 1: Minimums are impossible → scale everything
    if (minTotal > FULL_CIRCLE_DEG) {
        val scale = FULL_CIRCLE_DEG / rawAngles.sum()

        return raw.mapIndexed { i, pie ->
            pie.copy(data = (rawAngles[i] * scale).toInt())
        }
    }

    // Case 2: Minimums are possibl e
    val clamped = rawAngles.map { it.coerceAtLeast(MIN_ANGLE_DEG) }
    val clampedSum = clamped.sum()

    // No overflow
    if (clampedSum <= FULL_CIRCLE_DEG) {
        return raw.mapIndexed { i, pie ->
            pie.copy(data = clamped[i].toInt())
        }
    }

    // Redistribute excess only above minimum
    val excess = clampedSum - FULL_CIRCLE_DEG
    val adjustable = clamped.map { it - MIN_ANGLE_DEG }

    val adjustableTotal = adjustable.sum().coerceAtLeast(1e-9)

    val finalAngles = clamped.mapIndexed { i, angle ->
        angle - excess * (adjustable[i] / adjustableTotal)
    }

    return raw.mapIndexed { i, pie ->
        pie.copy(data = finalAngles[i].toInt())
    }
}







