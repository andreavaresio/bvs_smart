package com.bvs.smart.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bvs.smart.R
import com.bvs.smart.ui.components.*
import com.bvs.smart.ui.screens.components.BeeDanceScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    initialUsername: String,
    initialPassword: String,
    versionName: String,
    versionCode: Int
) {
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by rememberSaveable(initialPassword) { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showBeeDance by remember { mutableStateOf(false) }

    LaunchedEffect(initialUsername) {
        if (initialUsername.isNotBlank()) {
            username = initialUsername
        }
    }

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
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onLoginSuccess(username.trim(), password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showBeeDance = true }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$versionCode ",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "($versionName)",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
