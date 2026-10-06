package io.github.mohuddle.hours.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Heraldic Jerusalem Cross: a large Greek cross with four smaller crosses.
@Composable
fun JerusalemCross(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Canvas(modifier.size(size)) {
        val side = this.size.minDimension
        val x0 = (this.size.width - side) / 2f
        val y0 = (this.size.height - side) / 2f

        fun greekCross(cx: Float, cy: Float, arm: Float, thickness: Float) {
            drawRect(
                color = color,
                topLeft = Offset(cx - thickness / 2f, cy - arm),
                size = Size(thickness, arm * 2f),
            )
            drawRect(
                color = color,
                topLeft = Offset(cx - arm, cy - thickness / 2f),
                size = Size(arm * 2f, thickness),
            )
        }

        greekCross(x0 + side * 0.5f, y0 + side * 0.5f, side * 0.46f, side * 0.14f)
        val offset = side * 0.255f
        val smallArm = side * 0.09f
        val smallThickness = side * 0.055f
        greekCross(x0 + side * 0.5f - offset, y0 + side * 0.5f - offset, smallArm, smallThickness)
        greekCross(x0 + side * 0.5f + offset, y0 + side * 0.5f - offset, smallArm, smallThickness)
        greekCross(x0 + side * 0.5f - offset, y0 + side * 0.5f + offset, smallArm, smallThickness)
        greekCross(x0 + side * 0.5f + offset, y0 + side * 0.5f + offset, smallArm, smallThickness)
    }
}
