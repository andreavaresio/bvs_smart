package com.bvs.smart.ui.screens

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bvs.smart.ui.components.DarkBackground
import com.bvs.smart.ui.components.SecondaryButton
import com.bvs.smart.ui.components.YellowPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryScreen(
    onPhotoSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    // Determine permission based on SDK
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(permission)

    if (permissionState.status.isGranted) {
        GalleryContent(onPhotoSelected, onBack)
    } else {
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Gallery permission is required", color = Color.White)
                SecondaryButton(text = "Request Permission", onClick = { permissionState.launchPermissionRequest() })
                SecondaryButton(text = "Back", onClick = onBack)
            }
        }
    }
}

@Composable
fun GalleryContent(
    onPhotoSelected: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Loading data from the device (File System/Content Provider) is a "side effect"
    // and should not block the main UI thread.
    // LaunchedEffect(Unit) runs once when the screen appears.
    LaunchedEffect(Unit) {
        // Switch to the IO dispatcher for input/output operations (database/files).
        withContext(Dispatchers.IO) {
            val imageList = mutableListOf<Uri>()
            try {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )

                cursor?.use {
                    val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (it.moveToNext()) {
                        val id = it.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        imageList.add(contentUri)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In a real app, you might want to show an error state
            }
            // Update the state (this triggers a UI refresh).
            photos = imageList
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            SecondaryButton(text = "Back", onClick = onBack)
            Text(
                text = "Gallery",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YellowPrimary)
            }
        } else {
            // LazyVerticalGrid: Like LazyColumn, but for grids.
            // columns = GridCells.Fixed(3) creates 3 columns.
            // It only renders items currently visible on screen.
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { uri ->
                    Box(modifier = Modifier.padding(1.dp)) {
                        // AsyncImage: A composable from the Coil library.
                        // It loads images asynchronously from a URL or URI.
                        AsyncImage(
                            model = uri,
                            contentDescription = null, // Accessibility description
                            contentScale = ContentScale.Crop, // Scale type (like CenterCrop)
                            modifier = Modifier
                                .aspectRatio(1f) // Force it to be square
                                .clickable { onPhotoSelected(uri) }
                        )
                    }
                }
            }
        }
    }
}
