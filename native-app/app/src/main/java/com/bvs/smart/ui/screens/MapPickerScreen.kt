package com.bvs.smart.ui.screens

import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bvs.smart.ui.components.YellowPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapPickerScreen(
    initialLat: Double,
    initialLon: Double,
    onConfirmLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaleFactor(1.5f) // Scale tiles to make text more readable
            controller.setZoom(18.0)
            controller.setCenter(GeoPoint(initialLat, initialLon))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/search?q=$searchQuery&format=json&limit=1")
                    .header("User-Agent", context.packageName)
                    .build()

                val response = client.newCall(request).execute()
                val json = response.body?.string()

                if (response.isSuccessful && !json.isNullOrEmpty()) {
                    val jsonArray = JSONArray(json)
                    if (jsonArray.length() > 0) {
                        val firstResult = jsonArray.getJSONObject(0)
                        val lat = firstResult.getDouble("lat")
                        val lon = firstResult.getDouble("lon")
                        
                        withContext(Dispatchers.Main) {
                            mapView.controller.animateTo(GeoPoint(lat, lon))
                            mapView.controller.setZoom(18.0)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore nella ricerca", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Search Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, shape = RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cerca indirizzo...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() })
            )
            IconButton(onClick = { performSearch() }) {
                Icon(Icons.Default.Search, contentDescription = "Cerca")
            }
        }

        // Center Pin
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Center Pin",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 24.dp) // Adjust to make the point of the pin land on center
        )

        // Confirm Button
        FloatingActionButton(
            onClick = {
                val center = mapView.mapCenter
                onConfirmLocation(center.latitude, center.longitude)
            },
            containerColor = YellowPrimary,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Confirm Location")
        }
    }
}
