package com.bvs.smart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BeeWorldBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Warm Gradient Base (Cream to pale Honey)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFAFA), // Snow White
                            Color(0xFFFFF8E1), // Very pale Amber
                            Color(0xFFFFECB3)  // Soft Honey
                        )
                    )
                )
        )

        // 2. Procedural Honeycomb Pattern
        HoneycombPattern(
            color = Color(0xFFFFD54F).copy(alpha = 0.15f), // Very subtle yellow
            strokeWidth = 3f
        )
    }
}

@Composable
fun HoneycombPattern(color: Color, strokeWidth: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val hexSize = 60.dp.toPx()
        val width = size.width
        val height = size.height
        
        val horizontalDist = 1.5f * hexSize
        val verticalDist = (kotlin.math.sqrt(3.0) * hexSize).toFloat()
        
        val rows = (height / verticalDist).toInt() + 2
        val cols = (width / horizontalDist).toInt() + 2

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val xOffset = col * horizontalDist
                val yOffset = row * verticalDist + if (col % 2 == 1) verticalDist / 2f else 0f
                
                val path = Path().apply {
                    var angle = 0.0
                    for (i in 0..6) {
                        val x = xOffset + hexSize * cos(angle).toFloat()
                        val y = yOffset + hexSize * sin(angle).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                        angle += (PI / 3)
                    }
                    close()
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
