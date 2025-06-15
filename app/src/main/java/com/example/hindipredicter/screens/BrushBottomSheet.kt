package com.example.hindipredicter.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hindipredicter.DrawingView
import com.example.hindipredicter.viewmodel.CharacterViewModel
import com.example.hindipredicter.R
import com.example.hindipredicter.ai.getProcessedFeaturesForCharacter // NEW: Corrected import and function name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
    fun BrushBottomSheet(
    onDismiss: () -> Unit,
    drawingView: DrawingView?,
    colors: List<Color>,
    backgroundColors: List<Color>,
    currentBrushSize: Float,
    currentColorIndex: Int,
    currentBackgroundColorIndex: Int,
    changeBrushSize: (Float) -> Unit,
    changeBrushColor: (Color) -> Unit,
    changeBackgroundColor: (Color) -> Unit,
    updateCurrentColorIndex: (Int) -> Unit,
    updateCurrentBackgroundColorIndex: (Int) -> Unit,
    characterViewModel: CharacterViewModel = viewModel()
) {
    val context = LocalContext.current

    // NEW: Observe the prediction result from the ViewModel (now a list of strings)
    val predictionResults = characterViewModel.predictionResult.value

    // Use LaunchedEffect to react to changes in predictionResults
    LaunchedEffect(predictionResults) {
        predictionResults?.let { results ->
            if (results.isNotEmpty()) {
                val message = "Predicted Characters: ${results.joinToString(", ")}"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                characterViewModel.clearPrediction() // Clear the result after showing
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ... (Brush Size Selector, Brush Color Selection, Background Color Selection - NO CHANGES HERE)
            Text(
                text = "Select Brush Size",
                fontSize = 18.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 9.dp, top = 14.dp)
            )
            Slider(
                value = currentBrushSize,
                onValueChange = { size -> changeBrushSize(size) },
                valueRange = 2f..18f,
                steps = 16
            )
            Spacer(modifier = Modifier.height(9.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorResource(id = R.color.lightGray))
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select Brush Color",
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 9.dp, top = 14.dp)
                )
                colors.forEachIndexed { index, color ->
                    ColorButton(
                        buttonNumber = index,
                        isSelected = index == currentColorIndex,
                        colors = colors,
                        onColorSelected = { buttonIndex ->
                            changeBrushColor(colors[buttonIndex])
                            updateCurrentColorIndex(buttonIndex)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorResource(id = R.color.lightGray))
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select Background Color",
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 9.dp, top = 14.dp)
                )
                backgroundColors.forEachIndexed { index, color ->
                    ColorButton(
                        buttonNumber = index,
                        isSelected = index == currentBackgroundColorIndex,
                        colors = backgroundColors,
                        onColorSelected = { buttonIndex ->
                            changeBackgroundColor(backgroundColors[buttonIndex])
                            updateCurrentBackgroundColorIndex(buttonIndex)
                        }
                    )
                }
            }
            // End of NO CHANGES section

            // Bottom bar with buttons like Undo, Redo, Clear, Share, AI prediction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { drawingView?.onClickUndo() }) {
                    Icon(
                        painter = painterResource(R.drawable.undo),
                        contentDescription = "Undo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { drawingView?.onClickRedo() }) {
                    Icon(
                        painter = painterResource(R.drawable.undo),
                        contentDescription = "Redo",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(rotationY = 180f)
                    )
                }
                IconButton(onClick = { drawingView?.clearCanvas() }) {
                    Icon(
                        painter = painterResource(R.drawable.clear),
                        contentDescription = "Clear",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(
                    onClick = {
                        drawingView?.let { view ->
                            val bitmap = view.getBitmap(backgroundColors[currentBackgroundColorIndex])
                            val uri = drawingView.saveBitmap(context, bitmap)
                            if (uri != null) {
                                shareBitmap(context, uri)
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share Drawing",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                }
                // AI prediction button - MODIFIED
                IconButton(onClick = {
                    drawingView?.let { view ->
                        val segmentedCharacters = view.getSegmentedCharactersCoordinates()
                        if (segmentedCharacters.isNotEmpty()) {
                            val allFeaturesForPrediction = mutableListOf<List<Double>>()

                            for (charCoordinates in segmentedCharacters) {
                                if (charCoordinates.isNotEmpty()) {
                                    // Use the refactored function from utils.kt
                                    val features = getProcessedFeaturesForCharacter(charCoordinates)
                                    Log.d("ProcessedFeatures", "Extracted Features for one char: $features")

                                    // Verify features size matches model input expectation (218 in your case)
                                    if (features.size == 218) {
                                        allFeaturesForPrediction.add(features)
                                    } else {
                                        Log.e("BrushBottomSheet", "Features list for char is incorrect size: ${features.size}. Expected 218.")
                                        // You might choose to add a placeholder or error for this character, e.g., allPredictedChars.add("Error")
                                    }
                                }
                            }

                            if (allFeaturesForPrediction.isNotEmpty()) {
                                characterViewModel.predictMultipleCharacters(allFeaturesForPrediction)
                            } else {
                                Toast.makeText(context, "No valid characters to predict.", Toast.LENGTH_SHORT).show()
                            }

                        } else {
                            Toast.makeText(context, "Draw characters first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ai),
                        contentDescription = "Predict Characters", // Changed content description
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun shareBitmap(context: Context, uri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Drawing"))
}