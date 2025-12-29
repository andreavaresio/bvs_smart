package com.bvs.smart.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bvs.smart.data.Arnia
import com.bvs.smart.network.AuthManager
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanDialog(
    hive: Arnia,
    initialSettings: AuthManager.ScanSettings,
    onDismiss: () -> Unit,
    onConfirm: (AuthManager.ScanSettings) -> Unit
) {
    var scale by remember { mutableStateOf(initialSettings.scale.toString()) }
    var permanenceDays by remember { mutableStateOf(initialSettings.permanenceDays.toString()) }
    var photosPerScan by remember { mutableStateOf(initialSettings.photosPerScan.toString()) }
    var measureType by remember { mutableStateOf(initialSettings.measureType) }
    
    val currentDate = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Nuova Scansione",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = hive.name,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Data: $currentDate",
                    fontSize = 14.sp,
                    color = YellowPrimary, // Highlight current date
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Parameters Form
                OutlinedTextField(
                    value = scale,
                    onValueChange = { scale = it },
                    label = { Text("Fattore di scala") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = permanenceDays,
                        onValueChange = { permanenceDays = it },
                        label = { Text("Giorni Perm.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = photosPerScan,
                        onValueChange = { photosPerScan = it },
                        label = { Text("N. Foto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Measurement Type Radio Buttons
                Text(
                    text = "Tipo Misura:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = measureType == "CadutaNaturale",
                        onClick = { measureType = "CadutaNaturale" },
                        colors = RadioButtonDefaults.colors(selectedColor = YellowPrimary)
                    )
                    Text("Caduta Naturale", fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    RadioButton(
                        selected = measureType == "Trattamento",
                        onClick = { measureType = "Trattamento" },
                        colors = RadioButtonDefaults.colors(selectedColor = YellowPrimary)
                    )
                    Text("Trattamento", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Next)
                Button(
                    onClick = {
                        val settings = validateAndCreateSettings(scale, permanenceDays, measureType, photosPerScan)
                        onConfirm(settings)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Text(text = "AVANTI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BigActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = TextPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(80.dp),
        elevation = ButtonDefaults.buttonElevation(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontWeight = FontWeight.Bold)
        }
    }
}

private fun validateAndCreateSettings(
    scaleStr: String,
    daysStr: String,
    type: String,
    photosStr: String
): AuthManager.ScanSettings {
    val s = scaleStr.replace(',', '.').toDoubleOrNull() ?: 1.0
    val d = daysStr.toIntOrNull() ?: 1
    val p = photosStr.toIntOrNull() ?: 1
    return AuthManager.ScanSettings(s, d, type, p)
}
