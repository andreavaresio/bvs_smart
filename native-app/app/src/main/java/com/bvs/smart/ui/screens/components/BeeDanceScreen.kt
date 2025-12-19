package com.bvs.smart.ui.screens.components

import android.media.MediaActionSound
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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

private const val COUNTER_BOX_SIZE_DP = 120
private val CounterOverlay = Color(0x66FFFFFF)

@Composable
fun BeeDanceScreen(onExit: () -> Unit) {
    val beeSize = 64.dp
    val density = LocalDensity.current

    val actionSound = remember {
        MediaActionSound().apply {
            load(MediaActionSound.FOCUS_COMPLETE)
            load(MediaActionSound.SHUTTER_CLICK)
        }
    }
    DisposableEffect(Unit) {
        onDispose { actionSound.release() }
    }

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
                        var bounced = false

                        if (newPos.x <= 0f) {
                            newPos = newPos.copy(x = 0f)
                            val bounce = max(abs(newVel.x), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(x = bounce)
                            bounced = true
                        } else if (newPos.x >= maxWidthPx - beeSizePx) {
                            newPos = newPos.copy(x = maxWidthPx - beeSizePx)
                            val bounce = max(abs(newVel.x), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(x = -bounce)
                            bounced = true
                        }

                        if (newPos.y <= 0f) {
                            newPos = newPos.copy(y = 0f)
                            val bounce = max(abs(newVel.y), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(y = bounce)
                            bounced = true
                        } else if (newPos.y >= maxHeightPx - beeSizePx) {
                            newPos = newPos.copy(y = maxHeightPx - beeSizePx)
                            val bounce = max(abs(newVel.y), minSpeed) * (0.8f + Random.nextFloat() * 0.4f)
                            newVel = newVel.copy(y = -bounce)
                            bounced = true
                        }

                        val updatedBee = Bee(newPos, newVel)
                        if (index < bees.size) {
                            bees[index] = updatedBee
                        }

                        if (bounced) {
                            actionSound.play(MediaActionSound.FOCUS_COMPLETE)
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
                            actionSound.play(MediaActionSound.SHUTTER_CLICK)
                        } else {
                            onExit()
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(COUNTER_BOX_SIZE_DP.dp)
                    .background(
                        color = CounterOverlay,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bees.size.toString(),
                    color = TextSecondary,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            bees.forEach { bee ->
                Text(
                    text = "🐝",
                    fontSize = 48.sp,
                    modifier = Modifier.offset {
                        IntOffset(bee.position.x.roundToInt(), bee.position.y.roundToInt())
                    }
                )
            }
        }
    }
}
