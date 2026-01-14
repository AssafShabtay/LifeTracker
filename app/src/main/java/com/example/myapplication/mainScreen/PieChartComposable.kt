    package com.example.myapplication.mainScreen

    import androidx.compose.animation.core.Spring
    import androidx.compose.animation.core.spring
    import androidx.compose.animation.core.tween
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.size
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.unit.dp
    import com.example.myapplication.mainScreen.helpers.PieChartViewModel
    import com.example.myapplication.mainScreen.helpers.getTodayTimeline
    import com.example.myapplication.mainScreen.helpers.pieDataFromTimeline

    import androidx.compose.runtime.remember
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.example.myapplication.ActivityDao
    import com.example.myapplication.mainScreen.helpers.Pie
    import com.example.myapplication.mainScreen.helpers.PieChart

    @Composable
    fun PieChartComposable(
        dao: ActivityDao,
        modifier: Modifier = Modifier.size(220.dp)

    ) {
        val viewModel: PieChartViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PieChartViewModel(dao) as T
                }
            }
        )
        val timeline = viewModel.timeline

        var pieData = remember(timeline) {
            pieDataFromTimeline(timeline)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    pieData  = deselectAll(pieData)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PieChart(
                modifier = Modifier.size(300.dp) ,
                data = pieData,
                onPieClick = { clickedPie ->
                    val pieIndex = pieData.indexOf(clickedPie)

                    if (pieIndex != -1) {
                        pieData = pieData.mapIndexed { index, pie ->
                            if (index == pieIndex) {
                                pie.copy(selected = !pie.selected)
                            } else {
                                pie.copy(selected = false)
                            }
                        }
                    }
                },
                selectedScale = 1.2f,
                scaleAnimEnterSpec = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                colorAnimEnterSpec = tween(300),
                colorAnimExitSpec = tween(300),
                scaleAnimExitSpec = tween(300),
                spaceDegreeAnimExitSpec = tween(300),
                style = Pie.Style.Fill
            )
        }
    }

    fun deselectAll(data: List<Pie>): List<Pie> =
        data.map { it.copy(selected = false) }

