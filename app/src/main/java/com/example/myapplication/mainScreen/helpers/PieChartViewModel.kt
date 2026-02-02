package com.example.myapplication.mainScreen.helpers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ActivityDao
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class PieChartViewModel(
    private val dao: ActivityDao,
) : ViewModel() {

    var selectedDate by mutableStateOf(Date())
        private set


    var dayTimeline by mutableStateOf<List<TimelineItem>>(emptyList())
        private set


    var monthData by mutableStateOf<Map<Int, List<TimelineItem>>>(emptyMap())
        private set

    init {
        loadDataForDay(selectedDate)
    }

    fun loadDataForDay(date: Date) {
        selectedDate = date
        viewModelScope.launch {
            val (start, end) = getDayRange(date)
            dayTimeline = getTimelineForRange(dao, start, end)
        }
    }
    fun loadDataForLastMonth(month: Int, year: Int) {
        viewModelScope.launch {
            val newData = mutableMapOf<Int, List<TimelineItem>>()
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            for (day in 1..days) {
                cal.set(Calendar.DAY_OF_MONTH, day)
                val (start, end) = getBounds(cal.time)
                newData[day] = getTimelineForRange(dao, start, end)
            }
            monthData = newData
        }
    }
    private fun getBounds(date: Date): Pair<Date, Date> {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val start = cal.time
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        return start to cal.time
    }
}