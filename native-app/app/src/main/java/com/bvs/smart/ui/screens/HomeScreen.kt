package com.bvs.smart.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Hive
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
    versionCode: Int,
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = rightDrawerState,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet {
                            SettingsDrawerContent(
                                username = loggedUsername,
                                baseUrl = baseUrl,
                                versionName = versionName,
                                versionCode = versionCode,
                                onShareLogs = onShareLogs,
                                onLogout = onLogout
                            )
                        }
                    }
                }
            ) {
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(YellowPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Hive,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hive.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hive.code,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            TimeUtils.getRelativeTimeDisplay(hive.lastSampleDate)?.let { dateDisplay ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dateDisplay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF616161)
                    )
                }
            }
        }
    }
}
