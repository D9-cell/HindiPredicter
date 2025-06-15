package com.example.hindipredicter.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.hindipredicter.R

@Composable
fun ColorButton(
    buttonNumber: Int,
    isSelected: Boolean,
    colors: List<Color>,
    onColorSelected: (Int) -> Unit
) {
    // Icon changes based on the selection state
    val icon = if (isSelected) R.drawable.baseline_circle_24 else R.drawable.outline_circle_24

    IconButton(onClick = { onColorSelected(buttonNumber) }) {
        // Use modulo to ensure the index stays within the bounds of the list
        val color = if (colors.isNotEmpty()) colors[buttonNumber % colors.size] else Color.Gray

        Icon(
            painter = painterResource(id = icon),
            contentDescription = "Color Button $buttonNumber, $color",
            tint = color,
            modifier = Modifier
                .padding(8.dp)
                .graphicsLayer(scaleX = if (isSelected) 1.2f else 1f, scaleY = if (isSelected) 1.2f else 1f)
        )
    }
}
