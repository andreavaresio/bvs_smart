package com.bvs.smart.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.bvs.smart.R
import com.bvs.smart.ui.components.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    initialUsername: String,
    initialPassword: String,
    initialScanner: String,
    versionName: String,
    versionCode: Int
) {
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var scanner by rememberSaveable(initialScanner) { mutableStateOf(initialScanner) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showBeeDance by remember { mutableStateOf(false) }

    if (showBeeDance) {
        BeeDanceScreen(onExit = { showBeeDance = false })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "BeeVS Logo",
                modifier = Modifier
                    .size(150.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "BeeVS Mobile",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            
            Text(
                text = "Accedi per gestire le tue arnie",
                fontSize = 16.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Form
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Email") },
                placeholder = { Text("esempio@mail.it") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Nascondi password" else "Mostra password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = scanner,
                onValueChange = { scanner = it },
                label = { Text("Scanner ID") },
                placeholder = { Text("SCANNER_DEMO_1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFD32F2F),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            PrimaryButton(
                text = if (isLoading) "Accesso in corso..." else "ACCEDI",
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank() && scanner.isNotBlank()) {
                        onLoginSuccess(username.trim(), password, scanner.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        Text(
            text = "Version $versionName ($versionCode)",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showBeeDance = true }
                )
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun BeeDanceScreen(onExit: () -> Unit) {
    val beeSize = 64.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .clickable { onExit() }
    ) {
        val maxWidthPx = with(density) { constraints.maxWidth.toFloat() }
        val maxHeightPx = with(density) { constraints.maxHeight.toFloat() }
        val beeSizePx = with(density) { beeSize.toPx() }
        val speedPx = with(density) { 180.dp.toPx() }
        val minSpeed = speedPx * 0.4f

        fun randomVelocity(): Offset {
            val angle = Random.nextDouble(0.0, PI * 2)
            return Offset(
                x = (cos(angle) * speedPx).toFloat(),
                y = (sin(angle) * speedPx).toFloat()
            )
        }

        var position by remember {
            mutableStateOf(
                Offset(
                    x = maxWidthPx / 2f - beeSizePx / 2f,
                    y = maxHeightPx / 2f - beeSizePx / 2f
                )
            )
        }

        var velocity by remember { mutableStateOf(randomVelocity()) }

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            if (maxWidthPx == 0f || maxHeightPx == 0f) return@LaunchedEffect
            var lastTime = 0L
            while (true) {
                withFrameNanos { frameTime ->
                    if (lastTime == 0L) {
                        lastTime = frameTime
                        return@withFrameNanos
                    }
                    val deltaSeconds = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime

                    var newPos = Offset(
                        x = position.x + velocity.x * deltaSeconds,
                        y = position.y + velocity.y * deltaSeconds
                    )
                    var newVel = velocity

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

                    position = newPos
                    velocity = newVel
                }
            }
        }

        Text(
            text = "🐝",
            fontSize = 48.sp,
            modifier = Modifier.offset {
                IntOffset(position.x.roundToInt(), position.y.roundToInt())
            }
        )

        Text(
            text = "Tocca lo schermo per tornare alla login",
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
