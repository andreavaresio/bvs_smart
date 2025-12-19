package com.bvs.smart.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bvs.smart.data.BEEHIVES
import com.bvs.smart.data.Beehive
import com.bvs.smart.ui.components.DarkBackground
import com.bvs.smart.ui.components.PrimaryButton
import com.bvs.smart.ui.components.YellowPrimary

@Composable
fun HomeScreen(
    // Parameters in Composable functions act as "Props" (properties).
    // They pass data down from parents to children.
    // Callbacks (lambdas like () -> Unit) allow children to communicate back up to parents (events).
    selectedBeehive: Beehive?,
    scale: Double,
    versionName: String,
    versionCode: Int,
    onInternalCamera: () -> Unit,
    onExternalCamera: () -> Unit,
    onGallery: () -> Unit,
    onUpdateSettings: (Beehive, Double) -> Unit
) {
    // Local state for this screen
    var showSettings by remember { mutableStateOf(false) }

    // Box: Used here as the root container to hold the background and stack elements (like the version text at the bottom).
    Box(
        modifier = Modifier
            .fillMaxSize() // Fills the available width and height
            .background(DarkBackground)
            .padding(24.dp) // Adds internal spacing
    ) {
        // Column: Arranges children vertically (like a vertical LinearLayout).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(text = "🐝", fontSize = 96.sp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp)) // Spacer adds empty space between elements
            
            // Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E291B), shape = RoundedCornerShape(16.dp)) // rgba(255, 213, 79, 0.14)
                    .border(1.dp, YellowPrimary, RoundedCornerShape(16.dp))
                    .clickable { showSettings = true } // Makes this Box interactive. Clicking sets state to true.
                    .padding(vertical = 16.dp, horizontal = 20.dp)
            ) {
                Column {
                    // Row: Arranges children horizontally (like a horizontal LinearLayout).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Beehive", color = YellowPrimary, fontWeight = FontWeight.SemiBold)
                        Text(selectedBeehive?.label ?: "Not set", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(YellowPrimary.copy(alpha = 0.35f))
                            .padding(vertical = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scale", color = YellowPrimary, fontWeight = FontWeight.SemiBold)
                        Text(String.format("%.2f", scale), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            PrimaryButton(text = "In-app camera", onClick = onInternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Device-camera", onClick = onExternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Gallery", onClick = onGallery)
        }
        
        Text(
            text = "Version $versionName ($versionCode)",
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter),
            fontSize = 12.sp
        )
    }

    // Conditionally show the dialog based on state.
    // In Compose, you don't "show()" or "hide()" views manually.
    // You simply include them in the composition if a condition is met.
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
    // Temporary state for the dialog.
    // We don't update the main app state until "Save" is clicked.
    var tempBeehive by remember { mutableStateOf(currentBeehive ?: BEEHIVES.first()) }
    var tempScaleString by remember { mutableStateOf(currentScale.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Beehive Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Beehive", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                
                // LazyColumn: An efficient list that only renders visible items (like RecyclerView).
                // It's essential for long lists.
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(BEEHIVES) { beehive ->
                        val isSelected = tempBeehive.id == beehive.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) YellowPrimary.copy(alpha = 0.2f) else Color(0xFF2A2A2A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) YellowPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { tempBeehive = beehive }
                                .padding(12.dp)
                        ) {
                            Text(beehive.label, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Scale", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = tempScaleString,
                    onValueChange = { tempScaleString = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1F1F1F),
                        unfocusedContainerColor = Color(0xFF1F1F1F),
                        focusedBorderColor = YellowPrimary,
                        unfocusedBorderColor = Color(0xFF3A3A3A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
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
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
