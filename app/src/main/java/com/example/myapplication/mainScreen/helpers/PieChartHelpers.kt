package com.example.myapplication.mainScreen.helpers

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

private const val TOTAL_MINUTES = 1440.0
private const val FULL_CIRCLE_DEG = 360.0
private const val MIN_ANGLE_DEG = 25.0

fun todayRange(): Pair<Date, Date> {
    // Get the current date and time and set the time to midnight
    val cal = Calendar.getInstance()

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val start = cal.time

    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)

    return start to cal.time
}

fun totalDurationMinutes(timeline: List<TimelineItem>): Double {
    return timeline.sumOf { item ->
        when (item) {
            is TimelineItem.Still ->
                durationMinutes(item.item.startTimeDate, item.item.endTimeDate)

            is TimelineItem.Movement ->
                durationMinutes(item.item.startTimeDate, item.item.endTimeDate)

            is TimelineItem.Remaining ->
                durationMinutes(item.startTimeDate, item.endTimeDate)
        }
    }
}

suspend fun getTodayTimeline(dao: ActivityDao): List<TimelineItem> {
    //Get the data from today
    val (start, end) = todayRange()

    val still = dao.getStillForDay(start, end)
        .map{TimelineItem.Still(it)}

    val movement = dao.getMovementForDay(start, end)
        .map{TimelineItem.Movement(it)}

    val timeline = (still + movement)
        .sortedBy {
            when (it) {
                is TimelineItem.Still -> it.item.startTimeDate
                is TimelineItem.Movement -> it.item.startTimeDate
                is TimelineItem.Remaining -> it.startTimeDate
            }
        }


        if (totalDurationMinutes(timeline)<1440){

            val lastEndTime = timeline
                .lastOrNull { it is TimelineItem.Still || it is TimelineItem.Movement }
                ?.let {
                    when (it) {
                        is TimelineItem.Still -> it.item.endTimeDate
                        is TimelineItem.Movement -> it.item.endTimeDate
                        else -> null
                    }
                } ?: start

            val customSlice = TimelineItem.Remaining(
            startTimeDate = lastEndTime,
            endTimeDate = end,
        )
        return timeline + customSlice
}

    return timeline
}
fun durationMinutes(start: Date?, end: Date?): Double {
    //Return the duration of the activity in minutes
    if (start == null) return 0.0
    val (startOfDay, endOfDay) = todayRange()
    val end = if (end == null) Date() else end
    if(start.time !in startOfDay.time..endOfDay.time){
        return (end.time - startOfDay.time).coerceAtLeast(0) / 1000.0 / 60.0
    }
    if(end.time !in startOfDay.time..endOfDay.time){
        return (endOfDay.time - start.time).coerceAtLeast(0) / 1000.0 / 60.0
    }

    return (end.time - start.time).coerceAtLeast(0) / 1000.0 / 60.0
}
fun pieDataFromTimeline(timeline: List<TimelineItem>): List<Pie> {
    //Converts the timeline into pie data
    //TODO instead of rebuilding the data each time, we can just update the pie chart
    //TODO ADD ICONS
    //TODO ADD COLORS
    //TODO ADD TEXT
    return normalizePieByAngle(timeline.mapIndexed { index, item ->

        val duration = when (item) {
            is TimelineItem.Still -> durationMinutes(
                item.item.startTimeDate,
                item.item.endTimeDate
            )

            is TimelineItem.Movement -> durationMinutes(
                item.item.startTimeDate,
                item.item.endTimeDate
            )
            is TimelineItem.Remaining -> durationMinutes(
                item.startTimeDate,
                item.endTimeDate
            )
        }



        //TODO("ADD ICON AND COLOR")
        //if(item is TimelineItem.Still)
        val baseColor =  when (item) {
            is TimelineItem.Still -> Color.Gray
            is TimelineItem.Movement -> Color(0xFF4CAF50)
            is TimelineItem.Remaining -> Color(0xFFE0E0E0)
    }
        Pie(
            label = "${index + 1}:s ${if (item is TimelineItem.Still) "Still" else "Movement"}",
            data = duration,
            color = baseColor,
            selectedColor = baseColor.copy(alpha = 0.85f),
            clickable = item !is TimelineItem.Remaining
        )
    })
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
            pie.copy(data = rawAngles[i] * scale)
        }
    }

    // Case 2: Minimums are possible
    val clamped = rawAngles.map { it.coerceAtLeast(MIN_ANGLE_DEG) }
    val clampedSum = clamped.sum()

    // No overflow
    if (clampedSum <= FULL_CIRCLE_DEG) {
        return raw.mapIndexed { i, pie ->
            pie.copy(data = clamped[i])
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
        pie.copy(data = finalAngles[i])
    }
}







