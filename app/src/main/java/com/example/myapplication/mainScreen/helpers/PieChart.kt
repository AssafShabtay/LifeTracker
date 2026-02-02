package com.example.myapplication.mainScreen.helpers

import android.graphics.Paint as AndroidPaint

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.extensions.getAngleInDegree
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.cos
import kotlin.math.sin

data class Pie(
    override val label: String? = null,
    override val data: Int,
    override val color: Color,
    override val lat: Double? = null,
    override val lng: Double? = null,
    override val endLat: Double? = null,
    override val endLng: Double? = null,
    override val durationText: String? = null,
    override val icon: ImageVector? = null,
    override val type: PieType,
    override val selectedColor: Color = color,
    val selectedScale: Float? = null,
    val selectedPaddingDegree: Float? = null,
    val selected: Boolean = false,
    val clickable: Boolean = true,
    val colorAnimEnterSpec: AnimationSpec<Color>? = null,
    val scaleAnimEnterSpec: AnimationSpec<Float>? = null,
    val spaceDegreeAnimEnterSpec: AnimationSpec<Float>? = null,
    val colorAnimExitSpec: AnimationSpec<Color>? = null,
    val scaleAnimExitSpec: AnimationSpec<Float>? = null,
    val spaceDegreeAnimExitSpec: AnimationSpec<Float>? = null,
    val style: Style        ? = null
) : ActivityData {
    sealed class Style {
        data object Fill : Style()
        data class Stroke(val width: Dp = 42.dp) : Style()
    }
}
@Composable
fun PieChart(
    modifier: Modifier = Modifier,
    data: List<Pie>,
    spaceDegree: Float = 0f,
    onPieClick: (Pie, Offset) -> Unit = { _, _ -> },
    selectedScale: Float = 1.1f,
    selectedPaddingDegree: Float = 5f,
    colorAnimEnterSpec: AnimationSpec<Color> = tween(500),
    scaleAnimEnterSpec: AnimationSpec<Float> = tween(500),
    spaceDegreeAnimEnterSpec: AnimationSpec<Float> = tween(500),
    colorAnimExitSpec: AnimationSpec<Color> = colorAnimEnterSpec,
    scaleAnimExitSpec: AnimationSpec<Float> = scaleAnimEnterSpec,
    spaceDegreeAnimExitSpec: AnimationSpec<Float> = spaceDegreeAnimEnterSpec,
    style: Pie.Style = Pie.Style.Fill,
) {
    // Start the first slice from the top (12:00) instead of the default 3:00.
    val startAngleOffset = -90f

    fun normalizeDegree(deg: Float): Float {
        val d = deg % 360f
        return if (d < 0f) d + 360f else d
    }

    fun isDegreeBetweenWrapped(angle: Float, start: Float, end: Float): Boolean {
        val a = normalizeDegree(angle)
        val s = normalizeDegree(start)
        val e = normalizeDegree(end)
        return if (s <= e) a in s..e else (a >= s || a <= e)
    }

    require(data.none { it.data < 0 }) { "Data must be at least 0" }

    val onPieClick by rememberUpdatedState(onPieClick)
    val scope = rememberCoroutineScope()

    var pieChartCenter by remember { mutableStateOf(Offset.Zero) }
    var details by remember { mutableStateOf(emptyList<PieDetails>()) }
    val pieces = remember { mutableListOf<PiePiece>() }

    val pathMeasure = remember { PathMeasure() }

    LaunchedEffect(data) {
        val currSize = details.size
        details = if (details.isNotEmpty()) {
            data.mapIndexed { index, pie ->
                if (index < currSize) {
                    details[index].copy(pie = pie)
                } else {
                    PieDetails(pie = pie)
                }
            }
        } else {
            data.map { PieDetails(pie = it) }
        }
        pieces.clear()
    }

    LaunchedEffect(details) {
        details.forEach {
            if (it.pie.selected) {
                scope.launch {
                    it.color.animateTo(
                        it.pie.selectedColor,
                        it.pie.colorAnimEnterSpec ?: colorAnimEnterSpec
                    )
                }
                scope.launch {
                    it.scale.animateTo(
                        it.pie.selectedScale ?: selectedScale,
                        it.pie.scaleAnimEnterSpec ?: scaleAnimEnterSpec
                    )
                }
                scope.launch {
                    it.space.animateTo(
                        it.pie.selectedPaddingDegree ?: selectedPaddingDegree,
                        it.pie.spaceDegreeAnimEnterSpec ?: spaceDegreeAnimEnterSpec
                    )
                }
            } else {
                scope.launch {
                    it.color.animateTo(
                        it.pie.color,
                        it.pie.colorAnimExitSpec ?: colorAnimExitSpec
                    )
                }
                scope.launch {
                    it.scale.animateTo(1f, it.pie.scaleAnimExitSpec ?: scaleAnimExitSpec)
                }
                scope.launch {
                    it.space.animateTo(0f, it.pie.spaceDegreeAnimExitSpec ?: spaceDegreeAnimExitSpec)
                }
            }
        }
    }
    val iconPainters = mutableMapOf<ImageVector, Painter>()
    for (p in data) {
        val icon = p.icon ?: continue
        iconPainters[icon] = rememberVectorPainter(image = icon)
    }

// Android paint for duration text (NOT Compose Paint)
    val durationPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {



        Canvas(
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures { offset ->
                    val angle = normalizeDegree(getAngleInDegree(offset, pieChartCenter))
                    val distance = (offset - pieChartCenter).getDistance()

                    pieces.firstOrNull { piece ->
                        val strokeWidthPx =
                            (style as? Pie.Style.Stroke)?.width?.toPx() ?: 0f

                        val halfStroke = strokeWidthPx / 2f

                        val innerRadius = (piece.radius - halfStroke).coerceAtLeast(0f)
                        val outerRadius = piece.radius + halfStroke

                        isDegreeBetweenWrapped(angle, piece.startFromDegree, piece.endToDegree) &&
                                distance in innerRadius..outerRadius

                    }?.let { piece ->
                        details.find { it.id == piece.id }?.let { detail ->
                            if (detail.pie.clickable) {
                                onPieClick(detail.pie, offset)
                            }
                        }
                    }
                }



            }
        ) {
            pieces.clear()
            pieChartCenter = center

            val radius = when (style) {
                is Pie.Style.Fill -> minOf(size.width, size.height) / 2
                is Pie.Style.Stroke ->
                    (minOf(size.width, size.height) / 2) - (style.width.toPx() / 2)
            }

            val total = details.sumOf { it.pie.data }

            details.forEachIndexed { index, detail ->
// ... inside details.forEachIndexed { index, detail ->

                val degree = ((detail.pie.data * 360) / total)

                val drawStyle = if ((detail.pie.style ?: style) is Pie.Style.Stroke) {
                    Stroke(width = ((detail.pie.style ?: style) as Pie.Style.Stroke).width.toPx())
                } else {
                    Fill
                }

                val arcRect = Rect(center = center, radius = radius * detail.scale.value)

// We'll compute these for BOTH full-circle and normal slices
                val arcStart: Float
                val arcSweep: Float

                val piecePath = if (degree >= 360.0) {
                    arcStart = 0f
                    arcSweep = 360f

                    pieces.add(
                        PiePiece(
                            id = detail.id,
                            radius = radius * detail.scale.value,
                            startFromDegree = 0f,
                            endToDegree = 360f
                        )
                    )

                    Path().apply {
                        addOval(Rect(center = center, radius = radius * detail.scale.value))
                    }
                } else {
                    val beforeItems = data.filterIndexed { filterIndex, _ -> filterIndex < index }
                    val startFromDegree = beforeItems.sumOf { (it.data * 360) / total }

                    arcStart = startFromDegree.toFloat() + detail.space.value + startAngleOffset
                    arcSweep = degree.toFloat() - ((detail.space.value * 2) + spaceDegree)

                    val p = Path().apply { arcTo(arcRect, arcStart, arcSweep, true) }

                    if ((detail.pie.style ?: style) is Pie.Style.Fill) {
                        pathMeasure.setPath(p, false)
                        p.reset()
                        val start = pathMeasure.getPosition(0f)
                        if (!start.isUnspecified) p.moveTo(start.x, start.y)
                        p.lineTo(size.width / 2, size.height / 2)
                        p.arcTo(arcRect, arcStart, arcSweep, true)
                        p.lineTo(size.width / 2, size.height / 2)
                    }

                    pieces.add(
                        PiePiece(
                            id = detail.id,
                            radius = radius * detail.scale.value,
                            startFromDegree = normalizeDegree(arcStart),
                            endToDegree = normalizeDegree(arcStart + arcSweep),
                        )
                    )

                    p
                }

                drawPath(
                    path = piecePath,
                    color = detail.color.value,
                    style = drawStyle,
                )

// ---------- ICON + DURATION (drawn over slice) ----------
                detail.pie.durationText?.let { duration ->
                    val midAngle = arcStart + arcSweep / 2f

// ✅ Pop-out distance when selected
                    val popOutDistance =
                        (detail.scale.value - 1f) * 100.dp.toPx()

// Direction outward
                    val offsetX =
                        popOutDistance * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                    val offsetY =
                        popOutDistance * sin(Math.toRadians(midAngle.toDouble())).toFloat()

// Stroke thickness adjustment
                    val strokeWidthPx =
                        ((detail.pie.style ?: style) as? Pie.Style.Stroke)?.width?.toPx() ?: 0f

                    val adjustedRadius = radius + strokeWidthPx / 2f

// Label radius inside the slice
                    val labelRadius =
                        if (strokeWidthPx > 0f)
                            adjustedRadius * 0.77f
                        else
                            radius * 0.65f

// ✅ Label center moves with slice
                    val labelCenter = Offset(
                        x = center.x + offsetX +
                                labelRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat(),
                        y = center.y + offsetY +
                                labelRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()
                    )

                    val painter = detail.pie.icon?.let { iconPainters[it] }
                    val iconSize = 22.dp.toPx()

                    // draw icon (optional) ABOVE the text
                    if (painter != null) {
                        translate(left = labelCenter.x - iconSize / 2f, top = labelCenter.y - iconSize) {
                            with(painter) {
                                draw(
                                    size = Size(iconSize, iconSize),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }
                        }
                    }

                    // draw duration text (Android native canvas)
                    drawIntoCanvas { canvas ->
                        durationPaint.textSize = 12.dp.toPx()
                        durationPaint.color = android.graphics.Color.WHITE
                        val textY = labelCenter.y + (if (painter != null) 14.dp.toPx() else 0f)
                        canvas.nativeCanvas.drawText(duration, labelCenter.x, textY, durationPaint)
                    }
                }

            }
        }
    }
}


private data class PieDetails(
    val id: String = Random.nextInt(0, 999999).toString(),
    val pie: Pie,
    val color: Animatable<Color, AnimationVector4D> = Animatable(pie.color),
    val scale: Animatable<Float, AnimationVector1D> = Animatable(1f),
    val space: Animatable<Float, AnimationVector1D> = Animatable(0f)
)

private data class PiePiece(
    val id: String,
    val radius: Float,
    val startFromDegree: Float,
    val endToDegree: Float,
)