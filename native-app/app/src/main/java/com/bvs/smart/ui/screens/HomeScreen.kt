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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bvs.smart.data.Apiary
import com.bvs.smart.data.Arnia
import com.bvs.smart.network.AuthManager
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.ConfirmDialog
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onApiarySelected: (Apiary) -> Unit,
    // Scan & Location Props
    showScanDialogForHive: Arnia?,
    onShowScanDialog: (Arnia?) -> Unit,
    getHiveLocation: (String) -> AuthManager.Location?,
    onMapRequest: () -> Unit,
    onCurrentLocationRequest: () -> Unit,
    onScanRequest: (Arnia, AuthManager.ScanSettings, Double?, Double?) -> Unit,
    onOpenMap: (Double, Double, String) -> Unit,
    
    onShareLogs: () -> Unit,
    onLogout: () -> Unit
) {
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Pull to Refresh State
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }
    
    // Sync state: if external isRefreshing becomes false, stop the internal state
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullRefreshState.endRefresh()
        } else {
            pullRefreshState.startRefresh()
        }
    }
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logic for Nested Drawers
    // We nest the LEFT drawer INSIDE the RIGHT drawer to try and allow swipe gestures for both.
    // Inner consumes start-drag first (Left edge). Outer consumes end-drag (Right edge) potentially.
    
    // Outer: Right Drawer (Settings)
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
                            versionCode = versionCode,
                            onShareLogs = onShareLogs,
                            onLogout = {
                                scope.launch { rightDrawerState.close() }
                                showLogoutDialog = true
                            }
                        )
                    }
                }
            }
        ) {
            // Inner: Left Drawer (Navigation) - Reset direction to LTR for this drawer context
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                    // Content: Scaffold
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    val titleText = remember(selectedApiary) {
                                        if (selectedApiary != null) {
                                            val owner = selectedApiary.ownerName ?: "Utente"
                                            "$owner / ${selectedApiary.name}"
                                        } else {
                                            "Seleziona Apiario"
                                        }
                                    }
                                    Text(
                                        text = titleText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { leftDrawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    // Apiary Map Icon
                                    if (selectedApiary?.latitude != null && selectedApiary.longitude != null) {
                                        IconButton(onClick = { 
                                            onOpenMap(selectedApiary.latitude, selectedApiary.longitude, selectedApiary.name) 
                                        }) {
                                            Icon(Icons.Default.Map, contentDescription = "Mappa Apiario")
                                        }
                                    }
                                    
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
                        Box(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                                .nestedScroll(pullRefreshState.nestedScrollConnection)
                        ) {
                            if (selectedApiary == null || selectedApiary.hives.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Nessuna arnia disponibile", color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        top = 16.dp,
                                        bottom = 32.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(selectedApiary.hives) { hive ->
                                        HiveCard(
                                            hive = hive,
                                            getHiveLocation = getHiveLocation,
                                            onClick = { onShowScanDialog(hive) },
                                            onOpenMap = onOpenMap
                                        )
                                    }
                                }
                            }
                            
                            PullToRefreshContainer(
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter),
                                containerColor = Color.White,
                                contentColor = YellowPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showScanDialogForHive != null) {
        val targetHive = showScanDialogForHive
        val savedLoc = remember(targetHive) { getHiveLocation(targetHive.code) }
        
        ScanDialog(
            hive = targetHive,
            initialSettings = scanSettings,
            initialLat = savedLoc?.lat,
            initialLon = savedLoc?.lon,
            onDismiss = { onShowScanDialog(null) },
            onMapRequest = onMapRequest,
            onCurrentLocationRequest = onCurrentLocationRequest,
            onConfirm = { newSettings, lat, lon ->
                onShowScanDialog(null)
                onScanRequest(targetHive, newSettings, lat, lon)
            }
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Vuoi disconnetterti?",
            confirmText = "LOGOUT",
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }
}

@Composable
fun HiveCard(
    hive: Arnia, 
    getHiveLocation: (String) -> AuthManager.Location?,
    onClick: () -> Unit,
    onOpenMap: (Double, Double, String) -> Unit
) {
    // Resolve location: Use local sticky location if available
    val localLoc = remember(hive) { getHiveLocation(hive.code) }
    val hasLocation = localLoc != null

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

            // Sticky Map Icon (only if sticky location is set)
            if (hasLocation) {
                IconButton(
                    onClick = { onOpenMap(localLoc!!.lat, localLoc.lon, hive.name) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Posizione Arnia",
                        tint = YellowPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
