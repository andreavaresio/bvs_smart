package com.bvs.smart.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
    initialLat: Double?,
    initialLon: Double?,
    onDismiss: () -> Unit,
    onMapRequest: () -> Unit,
    onCurrentLocationRequest: () -> Unit,
    onConfirm: (AuthManager.ScanSettings, Double?, Double?) -> Unit
) {
    var scale by remember { mutableStateOf(initialSettings.scale.toString()) }
    var permanenceDays by remember { mutableStateOf(initialSettings.permanenceDays.toString()) }
    var photosPerScan by remember { mutableStateOf(initialSettings.photosPerScan.toString()) }
    var measureType by remember { mutableStateOf(initialSettings.measureType) }
    
    // Location State
    var latStr by remember(initialLat) { mutableStateOf(initialLat?.toString() ?: "") }
    var lonStr by remember(initialLon) { mutableStateOf(initialLon?.toString() ?: "") }
    
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
                
                // Location Section
                Text(
                    text = "Posizione (GPS)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = { latStr = it },
                        label = { Text("Lat") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = { lonStr = it },
                        label = { Text("Lon") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCurrentLocationRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = TextPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GPS", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onMapRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE), contentColor = TextPrimary),
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mappa", fontSize = 12.sp)
                    }
                }

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
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { measureType = "CadutaNaturale" }
                    ) {
                        RadioButton(
                            selected = measureType == "CadutaNaturale",
                            onClick = { measureType = "CadutaNaturale" },
                            colors = RadioButtonDefaults.colors(selectedColor = YellowPrimary)
                        )
                        Text("Caduta Naturale", fontSize = 14.sp)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { measureType = "Trattamento" }
                    ) {
                        RadioButton(
                            selected = measureType == "Trattamento",
                            onClick = { measureType = "Trattamento" },
                            colors = RadioButtonDefaults.colors(selectedColor = YellowPrimary)
                        )
                        Text("Trattamento", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button (Next)
                Button(
                    onClick = {
                        val settings = validateAndCreateSettings(scale, permanenceDays, measureType, photosPerScan)
                        val lat = latStr.replace(',', '.').toDoubleOrNull()
                        val lon = lonStr.replace(',', '.').toDoubleOrNull()
                        onConfirm(settings, lat, lon)
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
