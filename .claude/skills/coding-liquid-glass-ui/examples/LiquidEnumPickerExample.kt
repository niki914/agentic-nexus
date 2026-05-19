import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.floor
import kotlin.math.roundToInt

enum class LiquidPickerDensity(val label: String) {
    Compact("Compact"),
    Standard("Standard"),
    Expanded("Expanded")
}

@Composable
fun LiquidEnumPickerExample(
    value: LiquidPickerDensity,
    onValueChange: (LiquidPickerDensity) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val options = LiquidPickerDensity.entries
    val itemWidth = 84.dp
    val pickerHeight = 44.dp
    val pickerTop = 36.dp
    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val pickerHeightPx = with(density) { pickerHeight.toPx() }
    val pickerTopPx = with(density) { pickerTop.toPx() }

    var expanded by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var activeIndex by remember(value) {
        mutableIntStateOf(options.indexOf(value).coerceAtLeast(0))
    }

    fun indexAt(position: Offset): Int? {
        val inPickerHeight = position.y in pickerTopPx..(pickerTopPx + pickerHeightPx)
        if (!inPickerHeight) return null
        val index = floor(position.x / itemWidthPx).toInt()
        return index.takeIf { it in options.indices }
    }

    Box(
        modifier = modifier.pointerInput(options, value) {
            detectDragGesturesAfterLongPress(
                onDragStart = { start ->
                    expanded = true
                    dragPosition = start
                    indexAt(start)?.let { activeIndex = it }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragPosition += dragAmount
                    indexAt(dragPosition)?.let { activeIndex = it }
                },
                onDragEnd = {
                    onValueChange(options[activeIndex])
                    expanded = false
                },
                onDragCancel = {
                    expanded = false
                }
            )
        }
    ) {
        Text(
            text = value.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        if (expanded) {
            Row(
                modifier = Modifier
                    .offset { IntOffset(0, pickerTopPx.roundToInt()) }
                    .zIndex(1f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(22.dp) },
                        effects = {
                            vibrancy()
                            blur(10.dp.toPx())
                            lens(
                                refractionHeight = 14.dp.toPx(),
                                refractionAmount = 28.dp.toPx(),
                                chromaticAberration = true
                            )
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.24f))
                        }
                    )
                    .height(pickerHeight)
            ) {
                options.forEachIndexed { index, option ->
                    val active = index == activeIndex
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val scale = if (active) 1.08f else 1f
                                scaleX = scale
                                scaleY = scale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            color = Color.White.copy(alpha = if (active) 1f else 0.68f),
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
