package com.bvs.smart.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bvs.smart.data.Apiary
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowLight
import com.bvs.smart.ui.components.YellowPrimary

@Composable
fun NavigationDrawerContent(
    apiaryList: List<Apiary>,
    selectedApiary: Apiary?,
    onApiarySelected: (Apiary) -> Unit
) {
    // Group apiaries by owner
    val groupedApiaries = remember(apiaryList) {
        apiaryList.groupBy { it.ownerName ?: "Sconosciuto" }
    }
    // Track expanded states for owners
    // Initially expand all or just the one containing the selected apiary? Let's expand all for visibility.
    var expandedOwners by remember { mutableStateOf(groupedApiaries.keys.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Navigazione",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Divider()
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            groupedApiaries.forEach { (owner, apiaries) ->
                item {
                    val isExpanded = expandedOwners.contains(owner)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedOwners = if (isExpanded) {
                                    expandedOwners - owner
                                } else {
                                    expandedOwners + owner
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = owner,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }

                if (expandedOwners.contains(owner)) {
                    items(apiaries) { apiary ->
                        val isSelected = apiary == selectedApiary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) YellowLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onApiarySelected(apiary) }
                                .padding(start = 32.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = apiary.name,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                item { Divider(color = Color.LightGray.copy(alpha = 0.5f)) }
            }
        }
    }
}

@Composable
fun SettingsDrawerContent(
    username: String,
    baseUrl: String,
    versionName: String,
    onShareLogs: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color.White)
            .padding(24.dp)
    ) {
        // Top: Logout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.Red)
            }
        }
        
        Text(
            text = "Impostazioni",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingItem(label = "Utente Connesso", value = username)
        SettingItem(label = "Versione App", value = versionName)
        SettingItem(label = "Server API", value = baseUrl)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onShareLogs,
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Condividi Log File")
        }
    }
}

@Composable
fun SettingItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
