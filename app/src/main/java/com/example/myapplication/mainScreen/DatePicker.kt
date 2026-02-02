package com.example.myapplication.mainScreen

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.example.myapplication.ActivityDatabase
import com.example.myapplication.mainScreen.helpers.ActivityData
import com.example.myapplication.mainScreen.helpers.PieChartViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarDateSelector(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    viewModel: PieChartViewModel,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    calendar.time = selectedDate

    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var expandedCalendar by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected date header (collapsible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expandedCalendar = !expandedCalendar },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(selectedDate).split(",")[0],
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(selectedDate).split(",").drop(1).joinToString(",").trim(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expandedCalendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expandedCalendar) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expandable Calendar Grid
        if (expandedCalendar) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Month/Year header with navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (currentMonth == 0) {
                                currentMonth = 11
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }) {
                            Icon(Icons.Default.ExpandLess, "Previous month",
                                modifier = Modifier.rotate(-90f))
                        }

                        Text(
                            text = monthFormat.format(Calendar.getInstance().apply {
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.YEAR, currentYear)
                            }.time),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            if (currentMonth == 11) {
                                currentMonth = 0
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        }) {
                            Icon(Icons.Default.ExpandMore, "Next month",
                                modifier = Modifier.rotate(-90f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day of week headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        //TODO add an option for the starting day of the wek
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid
                    CalendarGrid(
                        month = currentMonth,
                        year = currentYear,
                        selectedDate = selectedDate,
                        onDateSelected = { newDate ->
                            onDateSelected(newDate)
                            expandedCalendar = false
                        },
                        viewModel,
                    )
                }
            }
        }
    }
}
@Composable
fun CalendarGrid(
    month: Int,
    year: Int,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    viewModel: PieChartViewModel,
) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month)
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val selectedCal = Calendar.getInstance()
    selectedCal.time = selectedDate
    val selectedDay = selectedCal.get(Calendar.DAY_OF_MONTH)
    val selectedMonth = selectedCal.get(Calendar.MONTH)
    val selectedYear = selectedCal.get(Calendar.YEAR)

    // Load activity data for all days in the month
    val context = LocalContext.current
    val database = remember { ActivityDatabase.getDatabase(context) }
    val dao = remember { database.activityDao() }
    var monthActivityData by remember { mutableStateOf<Map<Int, List<ActivityData>>>(emptyMap()) }

    LaunchedEffect(month, year) {
        monthActivityData = viewModel.loadDataForDay(dao, month, year)
    }

    val weeks = mutableListOf<List<Int>>()
    var week = mutableListOf<Int>()

    // Padding before first day
    repeat(firstDayOfWeek) { week.add(0) }

    // Fill days
    for (day in 1..daysInMonth) {
        week.add(day)
        if (week.size == 7) {
            weeks.add(week.toList())
            week = mutableListOf()
        }
    }

    // Pad last row
    if (week.isNotEmpty()) {
        while (week.size < 7) {
            week.add(0)
        }
        weeks.add(week.toList())
    }

    Column {
        weeks.forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEach { day ->
                    if (day == 0) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        CalendarDay(
                            day = day,
                            isSelected = day == selectedDay &&
                                    month == selectedMonth &&
                                    year == selectedYear,
                            activityData = monthActivityData[day]?.let { applySavedColors(context, it) },
                            onClick = {
                                val newDate = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.time
                                onDateSelected(newDate)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}