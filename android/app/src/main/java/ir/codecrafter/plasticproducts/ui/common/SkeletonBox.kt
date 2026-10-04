package ir.codecrafter.plasticproducts.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonShimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
        ),
        label = "skeletonShimmerProgress",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = baseColor.copy(alpha = 0.4f)

    // A gradient band as wide as the box itself sweeps from off the right edge
    // to off the left edge as progress goes 0 -> 1, i.e. right-to-left.
    val bandWidth = boxSize.width.toFloat()
    val startX = bandWidth * (1f - 2f * progress)

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseColor, highlightColor, baseColor),
                    start = Offset(x = startX, y = 0f),
                    end = Offset(x = startX + bandWidth, y = 0f),
                ),
                shape = shape,
            ),
    )
}
