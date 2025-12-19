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
    apiaryList: List<Apiary>,
    hiveList: List<Arnia>,
    onInternalCamera: () -> Unit,
    onExternalCamera: () -> Unit,
    onGallery: () -> Unit,
    onApiarySelected: (Apiary) -> Unit,
    onArniaSelected: (Arnia) -> Unit,
    onScaleUpdated: (Double) -> Unit
) {
    var apiaryMenuExpanded by remember { mutableStateOf(false) }
    var scaleText by remember(scale) { mutableStateOf(String.format(Locale.US, "%.2f", scale)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = "BeeVS Logo",
                modifier = Modifier
                    .height(120.dp)
                    .width(120.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BeeVS Mobile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
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
                    text = "Alveari",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                ExposedDropdownMenuBox(
                    expanded = apiaryMenuExpanded,
                    onExpandedChange = { apiaryMenuExpanded = !apiaryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedApiary?.name ?: "Seleziona un alveare",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = apiaryMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Alveare") },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    DropdownMenu(
                        expanded = apiaryMenuExpanded,
                        onDismissRequest = { apiaryMenuExpanded = false }
                    ) {
                        apiaryList.forEach { apiary ->
                            DropdownMenuItem(
                                text = { Text(apiary.name) },
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
                        text = "Nessuna arnia disponibile per l'alveare selezionato.",
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
                                    Text(
                                        text = hive.name,
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

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(text = "In-app camera", onClick = onInternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Device-camera", onClick = onExternalCamera)
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(text = "Gallery", onClick = onGallery)

            Spacer(modifier = Modifier.weight(1f, fill = true))

            Text(
                text = "Version $versionName ($versionCode)",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}
