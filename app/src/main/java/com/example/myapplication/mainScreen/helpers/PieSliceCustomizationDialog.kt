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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.myapplication.mainScreen.helpers.Pie
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SliceCustomizePopover(
    expanded: Boolean,
    anchorOffsetPx: Offset,          // tap position in the same Box coordinate space (px)
    containerSizePx: IntOffset,      // width/height of the anchor Box (px)
    pie: Pie?,
    onClose: () -> Unit,
    onUpdatePie: (Pie) -> Unit,
) {
    if (pie == null) return

    // Rough popover size for clamping (keeps it on-screen).
    // You can tweak these to match your design.
    val popoverWidth = 280.dp
    val popoverHeight = 220.dp
    val gap = 10.dp

    val density = LocalDensity.current
    val (popoverWpx, popoverHpx, gapPx) = with(density) {
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

    // Pretty enter/exit animation
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { visibleState.targetState = expanded }

    // Non-modal Popup: focusable=false => outside touches go through
    Popup(
        alignment = Alignment.TopStart,
        offset = animatedOffset,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
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
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
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

                    Divider()

                    // Quick actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdatePie(pie.copy(selected = !pie.selected)) }
                        ) {
                            Text(if (pie.selected) "Unhighlight" else "Highlight")
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdatePie(pie.copy(clickable = !pie.clickable)) }
                        ) {
                            Text(if (pie.clickable) "Disable tap" else "Enable tap")
                        }
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
