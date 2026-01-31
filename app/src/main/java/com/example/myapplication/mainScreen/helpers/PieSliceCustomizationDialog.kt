import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.myapplication.mainScreen.helpers.Pie
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SliceCustomizePopoverMovement(
    expanded: Boolean,
    anchorOffsetPx: Offset,
    containerSizePx: IntOffset,
    pie: Pie?,
    onClose: () -> Unit,
    onUpdatePie: (Pie) -> Unit,
) {
    if (pie == null) return
    // Sizes
    val popoverWidth = 280.dp
    val popoverHeight = 350.dp
    val gap = 10.dp

    val density = LocalDensity.current
    val (popoverWpx, popoverHpx, gapPx) = with(density) {//TODO()
        Triple(popoverWidth.toPx(), popoverHeight.toPx(), gap.toPx())
    }

    fun clamp(v: Float, min: Float, max: Float) = v.coerceIn(min, max)

    // Prefer showing ABOVE the tap; if not enough space, show BELOW.
    val prefersAbove = anchorOffsetPx.y - popoverHpx - gapPx > 0f
    val desiredY = if (prefersAbove) {
        anchorOffsetPx.y - popoverHpx - gapPx
    } else {
        anchorOffsetPx.y + gapPx
    }

    // Center horizontally around tap
    val desiredX = anchorOffsetPx.x - (popoverWpx / 2f)

    val maxX = (containerSizePx.x.toFloat() - popoverWpx).coerceAtLeast(0f)
    val maxY = (containerSizePx.y.toFloat() - popoverHpx).coerceAtLeast(0f)

    val targetOffset = IntOffset(
        x = clamp(desiredX, 0f, maxX).roundToInt(),
        y = clamp(desiredY, 0f, maxY).roundToInt()
    )

    // Smooth position animation when user taps another slice
    val animatedOffset by animateIntOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "popoverOffset"
    )

    // enter/exit animation
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { visibleState.targetState = expanded }

    Popup(
        alignment = Alignment.TopStart,
        offset = animatedOffset,
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(popoverWidth)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = pie.label ?: "Customize movement",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val subtitle = pie.durationText ?: "Tap options below"
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()

                    // Google Maps
                    val startLat = pie.lat
                    val startLng = pie.lng
                    val endLat = pie.endLat // Assumes Pie has these properties
                    val endLng = pie.endLng

                    if (startLat != null && startLng != null && endLat != null && endLng != null) {
                        val startPos = LatLng(startLat, startLng)
                        val endPos = LatLng(endLat, endLng)

                        val cameraPositionState = rememberCameraPositionState()

                        // Calculate bounds to show both start and end
                        LaunchedEffect(startPos, endPos) {
                            try {
                                val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                                    .include(startPos)
                                    .include(endPos)
                                    .build()

                                // Padding in pixels (e.g., 50) to keep markers away from edges
                                cameraPositionState.move(
                                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                )
                            } catch (e: Exception) {
                                // Fallback if points are identical or invalid
                                cameraPositionState.move(
                                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(startPos, 15f)
                                )
                            }
                        }

                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp)), // Added clip for cleaner look
                            cameraPositionState = cameraPositionState,
                            uiSettings = com.google.maps.android.compose.MapUiSettings(
                                zoomControlsEnabled = true,
                                mapToolbarEnabled = false
                            )
                        ) {
                            // Start Marker (Default Red)
                            Marker(
                                state = MarkerState(position = startPos),
                                title = "Start",
                                snippet = "Start Location"
                            )

                            // End Marker (Blue to distinguish)
                            Marker(
                                state = MarkerState(position = endPos),
                                title = "End",
                                snippet = "End Location",
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                                )
                            )
                        }
                    }

                    Divider()

                    // Color chips
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val palette = listOf(
                        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF42A5F5),
                        Color(0xFF26A69A), Color(0xFFFFCA28), Color(0xFF8D6E63),
                        Color(0xFF78909C), Color(0xFF66BB6A)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        palette.forEach { c ->
                            ColorChip(
                                color = c,
                                selected = (c.value == pie.color.value),
                                onClick = {
                                    onUpdatePie(
                                        pie.copy(
                                            color = c,
                                            selectedColor = c.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                            )
                        }

                        // Random color chip
                        ColorChip(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            selected = false,
                            label = "?",
                            onClick = {
                                val rc = Color(
                                    Random.nextInt(40, 256),
                                    Random.nextInt(40, 256),
                                    Random.nextInt(40, 256),
                                )
                                onUpdatePie(
                                    pie.copy(
                                        color = rc,
                                        selectedColor = rc.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SliceCustomizePopover(
    expanded: Boolean,
    anchorOffsetPx: Offset,
    containerSizePx: IntOffset,
    pie: Pie?,
    onClose: () -> Unit,
    onUpdatePie: (Pie) -> Unit,
) {
    if (pie == null) return
    // Sizes
    val popoverWidth = 280.dp
    val popoverHeight = 350.dp
    val gap = 10.dp

    val density = LocalDensity.current
    val (popoverWpx, popoverHpx, gapPx) = with(density) {//TODO()
        Triple(popoverWidth.toPx(), popoverHeight.toPx(), gap.toPx())
    }

    fun clamp(v: Float, min: Float, max: Float) = v.coerceIn(min, max)

    // Prefer showing ABOVE the tap; if not enough space, show BELOW.
    val prefersAbove = anchorOffsetPx.y - popoverHpx - gapPx > 0f
    val desiredY = if (prefersAbove) {
        anchorOffsetPx.y - popoverHpx - gapPx
    } else {
        anchorOffsetPx.y + gapPx
    }

    // Center horizontally around tap
    val desiredX = anchorOffsetPx.x - (popoverWpx / 2f)

    val maxX = (containerSizePx.x.toFloat() - popoverWpx).coerceAtLeast(0f)
    val maxY = (containerSizePx.y.toFloat() - popoverHpx).coerceAtLeast(0f)

    val targetOffset = IntOffset(
        x = clamp(desiredX, 0f, maxX).roundToInt(),
        y = clamp(desiredY, 0f, maxY).roundToInt()
    )

    // Smooth position animation when user taps another slice
    val animatedOffset by animateIntOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "popoverOffset"
    )

    // enter/exit animation
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { visibleState.targetState = expanded }

    Popup(
        alignment = Alignment.TopStart,
        offset = animatedOffset,
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .width(popoverWidth)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = pie.label ?: "Customize slice",
                                style = MaterialTheme.typography.titleMedium
                            )
                            val subtitle = pie.durationText ?: "Tap options below"
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider()
                    val lat = pie.lat
                    val lng = pie.lng
                    if (lat != null && lng != null) {

                        val markerPosition = LatLng(lat, lng)

                        val cameraPositionState = rememberCameraPositionState()

                        // Move camera every time marker changes
                        LaunchedEffect(markerPosition) {4
                            cameraPositionState.animate(
                                update = com.google.android.gms.maps.CameraUpdateFactory
                                    .newLatLngZoom(markerPosition, 15f)
                            )
                        }

                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            cameraPositionState = cameraPositionState
                        ) {
                            Marker(
                                state = MarkerState(position = LatLng(lat, lng)),
                                title = pie.label
                            )
                        }
                    }
                    Divider()
                    // Color chips
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val palette = listOf(
                        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFF42A5F5),
                        Color(0xFF26A69A), Color(0xFFFFCA28), Color(0xFF8D6E63),
                        Color(0xFF78909C), Color(0xFF66BB6A)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        palette.forEach { c ->
                            ColorChip(
                                color = c,
                                selected = (c.value == pie.color.value),
                                onClick = {
                                    onUpdatePie(
                                        pie.copy(
                                            color = c,
                                            selectedColor = c.copy(alpha = 0.85f)
                                        )
                                    )
                                }
                            )
                        }

                        // Random color chip
                        ColorChip(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            selected = false,
                            label = "🎲",
                            onClick = {
                                val rc = Color(
                                    Random.nextInt(40, 256),
                                    Random.nextInt(40, 256),
                                    Random.nextInt(40, 256),
                                )
                                onUpdatePie(
                                    pie.copy(
                                        color = rc,
                                        selectedColor = rc.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorChip(
    color: Color,
    selected: Boolean,
    label: String? = null,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color, CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
