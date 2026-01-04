package com.bvs.smart.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bvs.smart.data.Arnia
import com.bvs.smart.network.AuthManager
import com.bvs.smart.ui.components.AppBackground
import com.bvs.smart.ui.components.PrimaryButton
import com.bvs.smart.ui.components.TextPrimary
import com.bvs.smart.ui.components.TextSecondary
import com.bvs.smart.ui.components.YellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSessionScreen(
    hive: Arnia,
    settings: AuthManager.ScanSettings,
    photos: List<Uri?>,
    onPhotoSlotClicked: (Int) -> Unit, // Index of the slot clicked
    onDeletePhoto: (Int) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit
) {
    val totalSlots = settings.photosPerScan
    val filledSlots = photos.count { it != null }
    val isReadyToSend = filledSlots == totalSlots

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Sessione di Scatto", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = hive.name, fontSize = 14.sp, fontWeight = FontWeight.Normal)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = YellowPrimary,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = AppBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = if (isReadyToSend) "INVIA SCANSIONE" else "COMPLETA ($filledSlots/$totalSlots)",
                    onClick = onSend,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isReadyToSend
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Info Header
            Text(
                text = "Riempi tutti gli slot per inviare",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grid of Slots
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(photos) { index, uri ->
                    PhotoSlot(
                        index = index + 1,
                        uri = uri,
                        onClick = { onPhotoSlotClicked(index) },
                        onDelete = { onDeletePhoto(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoSlot(
    index: Int,
    uri: Uri?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (uri != null) Color.White else Color(0xFFEEEEEE))
            .border(
                width = 2.dp,
                color = if (uri != null) YellowPrimary else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (uri != null) {
            // Filled Slot
            AsyncImage(
                model = uri,
                contentDescription = "Photo $index",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Delete Button overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Slot Number Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(YellowPrimary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "#$index",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        } else {
            // Empty Slot
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Foto $index",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SourceSelectionDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onExternalCamera: () -> Unit,
    onGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Foto") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Scegli da dove acquisire la foto.", modifier = Modifier.padding(bottom = 8.dp))
                
                Button(
                    onClick = onCamera,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }

                Button(
                    onClick = onExternalCamera,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera Esterna")
                }

                Button(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galleria")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
        containerColor = Color.White,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}
