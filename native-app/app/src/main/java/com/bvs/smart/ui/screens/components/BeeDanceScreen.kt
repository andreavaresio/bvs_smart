package com.bvs.smart.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BeeDanceScreen(onExit: () -> Unit) {
    val beeSize = 64.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val maxWidthPx = with(density) { constraints.maxWidth.toFloat() }
        val maxHeightPx = with(density) { constraints.maxHeight.toFloat() }
        val beeSizePx = with(density) { beeSize.toPx() }
        val speedPx = with(density) { 180.dp.toPx() }
        val minSpeed = speedPx * 0.4f

        data class Bee(val position: Offset, val velocity: Offset)

        fun randomVelocity(): Offset {
            val angle = Random.nextDouble(0.0, PI * 2)
            return Offset(
                x = (cos(angle) * speedPx).toFloat(),
                y = (sin(angle) * speedPx).toFloat()
            )
        }

        fun clampPosition(offset: Offset): Offset {
            return Offset(
                x = offset.x.coerceIn(0f, maxWidthPx - beeSizePx),
                y = offset.y.coerceIn(0f, maxHeightPx - beeSizePx)
            )
        }

        val bees = remember { mutableStateListOf<Bee>() }

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            if (maxWidthPx == 0f || maxHeightPx == 0f) return@LaunchedEffect
            if (bees.isEmpty()) {
                val startPosition = Offset(
                    x = maxWidthPx / 2f - beeSizePx / 2f,
                    y = maxHeightPx / 2f - beeSizePx / 2f
                )
                bees.add(Bee(startPosition, randomVelocity()))
            }
            var lastTime = 0L
            while (true) {
                withFrameNanos { frameTime ->
                    if (lastTime == 0L) {
                        lastTime = frameTime
                        return@withFrameNanos
                    }
                    val deltaSeconds = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime

                    val snapshot = bees.toList()
                    snapshot.forEachIndexed { index, bee ->
                        var newPos = Offset(
                            x = bee.position.x + bee.velocity.x * deltaSeconds,
                            y = bee.position.y + bee.velocity.y * deltaSeconds
                        )
                        var newVel = bee.velocity

                        if (newPos.x <= 0f) {
                            newPos = newPos.copy(x = 0f)
                            val bounce = max(abs(newVel.x), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(x = bounce)
                        } else if (newPos.x >= maxWidthPx - beeSizePx) {
                            newPos = newPos.copy(x = maxWidthPx - beeSizePx)
                            val bounce = max(abs(newVel.x), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(x = -bounce)
                        }

                        if (newPos.y <= 0f) {
                            newPos = newPos.copy(y = 0f)
                            val bounce = max(abs(newVel.y), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(y = bounce)
                        } else if (newPos.y >= maxHeightPx - beeSizePx) {
                            newPos = newPos.copy(y = maxHeightPx - beeSizePx)
                            val bounce = max(abs(newVel.y), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(y = -bounce)
                        }

                        val updatedBee = Bee(newPos, newVel)
                        if (index < bees.size) {
                            bees[index] = updatedBee
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bees.size, maxWidthPx, maxHeightPx) {
                    detectTapGestures { tapOffset ->
                        val tappedIndex = bees.indexOfFirst { bee ->
                            tapOffset.x >= bee.position.x &&
                                tapOffset.x <= bee.position.x + beeSizePx &&
                                tapOffset.y >= bee.position.y &&
                                tapOffset.y <= bee.position.y + beeSizePx
                        }
                        if (tappedIndex >= 0) {
                            val startPos = clampPosition(
                                Offset(
                                    x = tapOffset.x - beeSizePx / 2f,
                                    y = tapOffset.y - beeSizePx / 2f
                                )
                            )
                            bees.add(Bee(startPos, randomVelocity()))
                        } else {
                            onExit()
                        }
                    }
                }
        ) {
            bees.forEach { bee ->
                Text(
                    text = "🐝",
                    fontSize = 48.sp,
                    modifier = Modifier.offset {
                        IntOffset(bee.position.x.roundToInt(), bee.position.y.roundToInt())
                    }
                )
            }

            Text(
                text = "Tocca un'ape per aggiungerne un'altra.\nTocca fuori per tornare alla login",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
