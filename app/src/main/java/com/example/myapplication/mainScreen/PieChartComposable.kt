    package com.example.myapplication.mainScreen

    import SliceCustomizePopover
    import SliceCustomizePopoverMovement
    import android.R.attr.maxHeight
    import android.R.attr.maxWidth
    import androidx.compose.animation.core.Spring
    import androidx.compose.animation.core.spring
    import androidx.compose.animation.core.tween
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.layout.Arrangement
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
    import androidx.compose.foundation.layout.Box
    import androidx.compose.runtime.remember
    import androidx.compose.ui.geometry.Offset
    import androidx.compose.ui.platform.LocalDensity
    import androidx.compose.ui.unit.IntOffset
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.ViewModelProvider
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.example.myapplication.ActivityDao
    import com.example.myapplication.mainScreen.helpers.Pie
    import com.example.myapplication.mainScreen.helpers.PieChart
    import com.example.myapplication.mainScreen.helpers.PieType

    private data class SlicePopoverState(
        val index: Int,
        val tapOffsetPx: Offset
    )

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

        var stillPopover by remember { mutableStateOf<SlicePopoverState?>(null) }
        var movementPopover by remember { mutableStateOf<SlicePopoverState?>(null) }

        var pieData by remember(timeline) {
            mutableStateOf(pieDataFromTimeline(timeline))
        }
        val density = LocalDensity.current
        val containerSizePx = remember(maxWidth, maxHeight) {
            with(density) {
                IntOffset(maxWidth, maxHeight)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    pieData = deselectAll(pieData)
                },
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                modifier = Modifier.size(300.dp),
                data = pieData,
                onPieClick = { clickedPie , tapOffset ->
                    val index = pieData.indexOf(clickedPie)

                    if (index != -1) {

                        if(clickedPie.type==PieType.Movement){
                            movementPopover = SlicePopoverState(index, tapOffset)
                            stillPopover = null
                        }
                        else if(clickedPie.type==PieType.Still){
                            stillPopover  = SlicePopoverState(index, tapOffset)
                            movementPopover = null
                        }
                        pieData =
                            if (pieData[index].selected) {
                                deselectAll(pieData)
                            } else {
                                pieData.mapIndexed { i, pie ->
                                    pie.copy(selected = i == index)
                                }
                            }
                    }
                },
                selectedScale = 1.2f,
                scaleAnimEnterSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                colorAnimEnterSpec = tween(300),
                colorAnimExitSpec = tween(300),
                scaleAnimExitSpec = tween(300),
                spaceDegreeAnimExitSpec = tween(300),
                spaceDegree = 1.5f,
                selectedPaddingDegree = 1.5f,
                style = Pie.Style.Stroke(width = 70.dp)

            )
            val still = stillPopover
            SliceCustomizePopover(
                expanded = still != null,
                anchorOffsetPx = still?.tapOffsetPx ?: Offset.Zero,
                containerSizePx = containerSizePx,
                pie = still?.let { pieData.getOrNull(it.index) },
                onClose = { stillPopover = null },
                onUpdatePie = { updated ->
                    val s = stillPopover ?: return@SliceCustomizePopover
                    pieData = pieData.mapIndexed { i, p -> if (i == s.index) updated else p }
                }
            )

            val movement = movementPopover
            SliceCustomizePopoverMovement(
                expanded = movement != null,
                anchorOffsetPx = movement?.tapOffsetPx ?: Offset.Zero,
                containerSizePx = containerSizePx,
                pie = movement?.let { pieData.getOrNull(it.index) },
                onClose = { movementPopover = null },
                onUpdatePie = { updated ->
                    val s = movementPopover ?: return@SliceCustomizePopoverMovement
                    pieData = pieData.mapIndexed { i, p -> if (i == s.index) updated else p }
                }
            )

        }
    }


    fun deselectAll(data: List<Pie>): List<Pie> =
        data.map { it.copy(selected = false)
        }

