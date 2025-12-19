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

// Brand Colors - Clear & Luminous Theme
val YellowPrimary = Color(0xFFFFD54F) // Warm professional yellow
val YellowLight = Color(0xFFFFF8E1)   // Very light yellow for backgrounds
val AppBackground = Color(0xFFFAFAFA) // Almost white, clean background
val TextPrimary = Color(0xFF1A1A1A)   // Almost black for strong readability
val TextSecondary = Color(0xFF616161) // Dark gray for secondary info
val CardBackground = Color(0xFFFFFFFF) // White cards
val BorderColor = Color(0xFFE0E0E0)   // Light gray borders

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fixedWidth: Boolean = true
) {
    val baseModifier = modifier.height(56.dp)
    val finalModifier = if (fixedWidth) baseModifier.width(260.dp) else baseModifier

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = YellowPrimary,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = finalModifier
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BeehiveBadge(label: String) {
    Box(
        modifier = Modifier
            .background(YellowLight, shape = RoundedCornerShape(8.dp))
            .border(1.dp, YellowPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Beehive: $label",
            color = Color(0xFFF57F17), // Darker yellow/orange for text contrast
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
