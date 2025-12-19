package com.bvs.smart.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bvs.smart.R
import com.bvs.smart.data.BEEHIVES
import com.bvs.smart.data.Beehive
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.CardBackground
import com.bvs.smart.ui.components.BorderColor
import com.bvs.smart.ui.components.PrimaryButton
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowLight
import com.bvs.smart.ui.components.YellowPrimary

@Composable
fun HomeScreen(
    selectedBeehive: Beehive?,
    scale: Double,
    versionName: String,
    versionCode: Int,
    onInternalCamera: () -> Unit,
    onExternalCamera: () -> Unit,
    onGallery: () -> Unit,
    onUpdateSettings: (Beehive, Double) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            // Official Brand Logo
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "BeeVS Logo",
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BeeVS Mobile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable { showSettings = true }
                    .padding(vertical = 20.dp, horizontal = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Beehive", color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(selectedBeehive?.label ?: "Select", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                            .padding(vertical = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scale", color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text(String.format("%.2f", scale), color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            PrimaryButton(text = "In-app camera", onClick = onInternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Device-camera", onClick = onExternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Gallery", onClick = onGallery)
        }
        
        Text(
            text = "Version $versionName ($versionCode)",
            color = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter),
            fontSize = 12.sp
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentBeehive = selectedBeehive,
            currentScale = scale,
            onDismiss = { showSettings = false },
            onSave = { beehive, newScale ->
                onUpdateSettings(beehive, newScale)
                showSettings = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentBeehive: Beehive?,
    currentScale: Double,
    onDismiss: () -> Unit,
    onSave: (Beehive, Double) -> Unit
) {
    var tempBeehive by remember { mutableStateOf(currentBeehive ?: BEEHIVES.first()) }
    var tempScaleString by remember { mutableStateOf(currentScale.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Beehive", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(BEEHIVES) { beehive ->
                        val isSelected = tempBeehive.id == beehive.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) YellowLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) YellowPrimary else BorderColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { tempBeehive = beehive }
                                .padding(12.dp)
                        ) {
                            Text(
                                beehive.label, 
                                color = if (isSelected) Color(0xFFF57F17) else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Scale", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedTextField(
                    value = tempScaleString,
                    onValueChange = { tempScaleString = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = YellowPrimary,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = {
                            val scale = tempScaleString.toDoubleOrNull() ?: 1.0
                            onSave(tempBeehive, scale)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(YellowPrimary, RoundedCornerShape(8.dp))
                    ) {
                        Text("Save", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
