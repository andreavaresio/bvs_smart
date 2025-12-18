package com.bvs.smart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Defining constants for consistent styling across the app
val YellowPrimary = Color(0xFFFFD54F)
val DarkBackground = Color(0xFF000000)
val DarkCard = Color(0xFF1C1C1C)

// Reusable Components:
// Instead of rewriting the same Button code multiple times, we create a custom Composable.
// This ensures consistency and makes changes easier (change once, update everywhere).

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Allow passing a modifier to customize from the outside
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .width(240.dp)
            .height(72.dp)
    ) {
        // The content of the button (what's inside it)
        Text(
            text = text,
            color = Color.Black,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)), // rgba(255,255,255,0.1)
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BeehiveBadge(label: String) {
    Box(
        modifier = Modifier
            .background(Color(0x80000000), shape = RoundedCornerShape(12.dp))
            .border(1.dp, YellowPrimary, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Beehive: $label",
            color = YellowPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
