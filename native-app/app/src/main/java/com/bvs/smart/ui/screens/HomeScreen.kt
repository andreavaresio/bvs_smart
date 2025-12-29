package com.bvs.smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bvs.smart.data.Apiary
import com.bvs.smart.data.Arnia
import com.bvs.smart.network.AuthManager
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowPrimary
import com.bvs.smart.ui.screens.components.NavigationDrawerContent
import com.bvs.smart.ui.screens.components.ScanDialog
import com.bvs.smart.ui.screens.components.SettingsDrawerContent
import com.bvs.smart.utils.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedApiary: Apiary?,
    apiaryList: List<Apiary>,
    scanSettings: AuthManager.ScanSettings,
    versionName: String,
    baseUrl: String,
    loggedUsername: String,
    onApiarySelected: (Apiary) -> Unit,
    onScanRequest: (Arnia, AuthManager.ScanSettings, Boolean) -> Unit, // Boolean: true=Camera, false=Gallery
    onShareLogs: () -> Unit,
    onLogout: () -> Unit
) {
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showScanDialogForHive by remember { mutableStateOf<Arnia?>(null) }

    // Logic for Nested Drawers
    // Outer Drawer = Left (Navigation)
    // Inner Drawer = Right (Settings) - We simulate Right Drawer by using CompositionLocalProvider to flip direction temporarily
    
    ModalNavigationDrawer(
        drawerState = leftDrawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerContent(
                    apiaryList = apiaryList,
                    selectedApiary = selectedApiary,
                    onApiarySelected = {
                        onApiarySelected(it)
                        scope.launch { leftDrawerState.close() }
                    }
                )
            }
        }
    ) {
        // To implement a Right Drawer with standard Material3, we wrap the content in another ModalNavigationDrawer
        // and flip the LayoutDirection for the drawer itself.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                drawerContent = {
                    // Reset direction for content inside the right drawer
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet {
                            SettingsDrawerContent(
                                username = loggedUsername,
                                baseUrl = baseUrl,
                                versionName = versionName,
                                onShareLogs = onShareLogs,
                                onLogout = onLogout
                            )
                        }
                    }
                }
            ) {
                // Reset direction for the main content
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = selectedApiary?.name ?: "Seleziona Apiario",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { leftDrawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { scope.launch { rightDrawerState.open() } }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = YellowPrimary,
                                    titleContentColor = TextPrimary,
                                    navigationIconContentColor = TextPrimary,
                                    actionIconContentColor = TextPrimary
                                )
                            )
                        },
                        containerColor = AppBackground
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                            if (selectedApiary == null || selectedApiary.hives.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Nessuna arnia disponibile", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(selectedApiary.hives) { hive ->
                                        HiveCard(
                                            hive = hive,
                                            onClick = { showScanDialogForHive = hive }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog handling
    if (showScanDialogForHive != null) {
        val targetHive = showScanDialogForHive!!
        ScanDialog(
            hive = targetHive,
            initialSettings = scanSettings,
            onDismiss = { showScanDialogForHive = null },
            onConfirm = { newSettings, isCamera ->
                showScanDialogForHive = null
                onScanRequest(targetHive, newSettings, isCamera)
            }
        )
    }
}

@Composable
fun HiveCard(hive: Arnia, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = hive.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = hive.code,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            TimeUtils.getRelativeTimeDisplay(hive.lastSampleDate)?.let { dateDisplay ->
                Text(
                    text = dateDisplay,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}