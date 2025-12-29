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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.bvs.smart.R
import com.bvs.smart.data.Apiary
import com.bvs.smart.data.Arnia
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.BorderColor
import com.bvs.smart.ui.components.CardBackground
import com.bvs.smart.ui.components.PrimaryButton
import com.bvs.smart.ui.components.SecondaryButton
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowLight
import com.bvs.smart.ui.components.YellowPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedApiary: Apiary?,
    selectedArnia: Arnia?,
    scale: Double,
    versionName: String,
    versionCode: Int,
    baseUrl: String,
    loggedUsername: String,
    apiaryList: List<Apiary>,
    hiveList: List<Arnia>,
    onExternalCamera: () -> Unit,
    onGallery: () -> Unit,
    onShareLogs: () -> Unit,
    onLogout: () -> Unit,
    onApiarySelected: (Apiary) -> Unit,
    onArniaSelected: (Arnia) -> Unit,
    onScaleUpdated: (Double) -> Unit
) {
    var apiaryMenuExpanded by remember { mutableStateOf(false) }
    var scaleText by remember(scale) { mutableStateOf(String.format(Locale.US, "%.2f", scale)) }

    // Calculate if there are multiple owners to decide whether to show owner name
    val hasMultipleOwners = remember(apiaryList) {
        apiaryList.mapNotNull { it.ownerName }.distinct().size > 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    Text(
                        text = "BeeVS Mobile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val displayName = loggedUsername.ifBlank { "Utente non identificato" }
                    Text(
                        text = "Utente: $displayName",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "BeeVS Logo",
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Apiari",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                ExposedDropdownMenuBox(
                    expanded = apiaryMenuExpanded,
                    onExpandedChange = { apiaryMenuExpanded = !apiaryMenuExpanded }
                ) {
                    val currentOwnerSuffix = if (hasMultipleOwners) selectedApiary?.ownerName?.let { " ($it)" } ?: "" else ""
                    val currentApiaryText = selectedApiary?.let { "${it.name}$currentOwnerSuffix" } ?: "Seleziona un apiario"
                    
                    OutlinedTextField(
                        value = currentApiaryText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = apiaryMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Apiario") },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    DropdownMenu(
                        expanded = apiaryMenuExpanded,
                        onDismissRequest = { apiaryMenuExpanded = false }
                    ) {
                        apiaryList.forEach { apiary ->
                            val ownerSuffix = if (hasMultipleOwners) apiary.ownerName?.let { " ($it)" } ?: "" else ""
                            DropdownMenuItem(
                                text = { Text("${apiary.name}$ownerSuffix") },
                                onClick = {
                                    apiaryMenuExpanded = false
                                    onApiarySelected(apiary)
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Arnie",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (hiveList.isEmpty()) {
                    Text(
                        text = "Nessuna arnia disponibile per l'apiario selezionato.",
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(hiveList) { hive ->
                            val isSelected = hive.code == selectedArnia?.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) YellowLight else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) YellowPrimary else BorderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onArniaSelected(hive) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val ownerSuffix = if (hasMultipleOwners) selectedApiary?.ownerName?.let { " ($it)" } ?: "" else ""
                                    Text(
                                        text = "${hive.name}$ownerSuffix",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = hive.code,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                hive.lastSampleDate?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it.substringBefore('T'),
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = scaleText,
                    onValueChange = {
                        scaleText = it
                        it.replace(',', '.').toDoubleOrNull()?.let(onScaleUpdated)
                    },
                    label = { Text("Scala per conta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = true))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    text = "Camera",
                    onClick = onExternalCamera,
                    modifier = Modifier.weight(1f),
                    fixedWidth = false
                )
                PrimaryButton(
                    text = "Gallery",
                    onClick = onGallery,
                    modifier = Modifier.weight(1f),
                    fixedWidth = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SecondaryButton(
                    text = "Condividi Log",
                    onClick = onShareLogs
                )
                Spacer(modifier = Modifier.width(8.dp))
                SecondaryButton(
                    text = "Logout",
                    onClick = onLogout
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = baseUrl,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
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
    }
}
