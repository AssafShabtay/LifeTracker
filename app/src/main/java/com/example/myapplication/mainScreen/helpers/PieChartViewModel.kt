package com.example.myapplication.mainScreen.helpers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ActivityDao
import kotlinx.coroutines.launch

class PieChartViewModel(
    private val dao: ActivityDao
) : ViewModel() {

    var timeline by mutableStateOf<List<TimelineItem>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            timeline = getTodayTimeline(dao)
        }
    }
}
