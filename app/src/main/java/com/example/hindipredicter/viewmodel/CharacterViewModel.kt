package com.example.hindipredicter.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.hindipredicter.ai.TFLiteClassifierHindi
import android.graphics.RectF // Import RectF

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    // For multi-character prediction (time-based segmentation)
    private val _predictionResult = mutableStateOf<List<String>?>(null)
    val predictionResult: State<List<String>?> = _predictionResult

    // NEW: For single-character prediction (square-based)
    // Stores a Pair of (RectF, PredictedCharacterString)
    private val _singleCharPredictionResult = mutableStateOf<Pair<RectF, String>?>(null)
    val singleCharPredictionResult: State<Pair<RectF, String>?> = _singleCharPredictionResult


    private val classifier = TFLiteClassifierHindi(application.applicationContext)

    // Original predictCharacter (can be kept if needed elsewhere for single predictions, or removed)
    // Note: This won't be called by BrushBottomSheet anymore for multi-char prediction.
    fun predictCharacter(features: List<Double>) {
        try {
            val predictedIndex = classifier.predict(features)
            val labelClasses = listOf(
                "A/अ/অ", "AA/आ/আ", "ADA/ढ़/ঢ", "AN/ং/ং",
                "BA/ब/ব", "BHA/भ/ভ", "BI/ः/ঃ",
                "C/চ/চ", "CH/ছ/ছ", "CN/ँ/ঁ",
                "DA/द/द", "DDA/ड/ड", "DDH/ढ/ঢ", "DHA/ध/ধ", "DRA/ड़/ড়",
                "E/ए/এ", "EN/ञ/ঞ",
                "G/ग/গ", "GH/ঘ/ঘ",
                "HA/ह/ह",
                "I/इ/इ", "II/ई/ঈ",
                "JA/ज/জ", "JH/झ/ঝ",
                "K/क/क", "KH/ख/ख", "KT/त্/ৎ",
                "LA/ল/ল",
                "MA/ম/ম", "MN/ण/ণ", "MSA/ষ/ষ",
                "NA/ন/ন",
                "O/ओ/ओ", "OI/ऐ/ঐ", "OU/औ/ঔ",
                "PA/প/প", "PHA/ফ/ফ",
                "RA/র/র", "RI/ऋ/ঋ",
                "S/स/स", "SA/শ/শ",
                "T/ट/ट", "TA/त/त", "THA/थ/थ", "TTA/ঠ/ঠ",
                "U/उ/उ", "UN/ङ/ঙ", "UU/ऊ/ঊ",
                "Y/य़/য়", "YA/य/য"
            )
            val predictedChar = labelClasses[predictedIndex]
            Log.d("CharacterViewModel", "Predicted Class Index: $predictedIndex")
            Log.d("CharacterViewModel", "Predicted Character: $predictedChar")
            // If you keep this function, consider if _predictionResult should be String? or List<String>?
            // For consistency with multi-char, it's better to always update the List<String> type.
            _predictionResult.value = listOf(predictedChar) // Wrap in a list
        } catch (e: Exception) {
            Log.e("CharacterViewModel", "Prediction failed: ${e.message}")
            _predictionResult.value = listOf("Error: Draw a complete character") // Wrap in a list
        }
    }

    // NEW: Function to predict multiple characters
    fun predictMultipleCharacters(charactersFeatures: List<List<Double>>) {
        val predictedCharacters = mutableListOf<String>()
        val labelClasses = listOf(
            "A/अ/অ", "AA/आ/आ", "ADA/ढ़/ঢ়", "AN/ं/ং",
            "BA/ब/ব", "BHA/भ/ভ", "BI/ः/ঃ",
            "C/च/চ", "CH/ছ/ছ", "CN/ँ/ঁ",
            "DA/द/द", "DDA/ड/ड", "DDH/ढ/ঢ", "DHA/ध/ध", "DRA/ड़/ड़",
            "E/ए/এ", "EN/ञ/ঞ",
            "G/ग/ग", "GH/घ/घ",
            "HA/ह/ह",
            "I/इ/इ", "II/ई/ঈ",
            "JA/ज/ज", "JH/झ/झ",
            "K/क/क", "KH/ख/ख", "KT/त्/ৎ",
            "LA/ল/ল",
            "MA/ম/ম", "MN/ণ/ণ","MSA/ष/ষ",
            "NA/ন/ন",
            "O/ओ/ओ", "OI/ऐ/ঐ", "OU/औ/ঔ",
            "PA/प/प", "PHA/ফ/ফ",
            "RA/র/র", "RI/ऋ/ঋ",
            "S/स/स", "SA/শ/শ",
            "T/ट/ट", "TA/त/त", "THA/थ/थ", "TTA/ঠ/ঠ",
            "U/उ/उ", "UN/ङ/ঙ", "UU/ऊ/ঊ",
            "Y/य़/য়", "YA/य/য"
        )
        for (features in charactersFeatures) {
            try {
                val predictedIndex = classifier.predict(features)
                val predictedChar = labelClasses[predictedIndex]
                Log.d("CharacterViewModel", "Predicted Class Index: $predictedIndex")
                Log.d("CharacterViewModel", "Predicted Character: $predictedChar")
                predictedCharacters.add(predictedChar)
            } catch (e: Exception) {
                Log.e("CharacterViewModel", "Prediction failed for a character: ${e.message}")
                predictedCharacters.add("Error") // Add error for this specific character
            }
        }
        _predictionResult.value = predictedCharacters // Update with all predictions
    }

    // NEW: Function to predict a single character based on square selection
    fun predictSingleCharacterForSquare(rect: RectF, features: List<Double>) {
        try {
            val predictedIndex = classifier.predict(features)
            val labelClasses = listOf(
                "A/अ/অ", "AA/आ/आ", "ADA/ढ़/ঢ়", "AN/ं/ং",
                "BA/ब/ব", "BHA/भ/ভ", "BI/ः/ঃ",
                "C/च/च", "CH/ছ/ছ", "CN/ँ/ঁ",
                "DA/द/द", "DDA/ड/ड", "DDH/ढ/ঢ", "DHA/ध/ध", "DRA/ड़/ड़",
                "E/ए/এ", "EN/ञ/ঞ",
                "G/ग/ग", "GH/ঘ/घ",
                "HA/ह/ह",
                "I/इ/इ", "II/ई/ई",
                "JA/ज/ज", "JH/झ/झ",
                "K/क/क", "KH/ख/ख", "KT/त्/ৎ",
                "LA/ল/ল",
                "MA/ম/ম", "MN/ণ/ণ","MSA/ষ/ষ",
                "NA/ন/ন",
                "O/ओ/ओ", "OI/ऐ/ঐ", "OU/औ/ঔ",
                "PA/প/प", "PHA/ফ/ফ",
                "RA/র/র", "RI/ऋ/ঋ",
                "S/स/स", "SA/শ/শ",
                "T/ट/ट", "TA/ত/ত", "THA/थ/থ", "TTA/ঠ/ঠ",
                "U/उ/उ", "UN/ङ/ঙ", "UU/ऊ/ঊ",
                "Y/य़/য়", "YA/य/য"
            )
            val predictedChar = labelClasses[predictedIndex]
            Log.d("CharacterViewModel", "Single Character Prediction for Square: $predictedChar")
            _singleCharPredictionResult.value = Pair(rect, predictedChar)
        } catch (e: Exception) {
            Log.e("CharacterViewModel", "Single Character Prediction failed for square: ${e.message}")
            _singleCharPredictionResult.value = Pair(rect, "Error")
        }
    }


    fun clearPrediction() {
        _predictionResult.value = null // Clears the list of results
    }

    // NEW: Clear single char prediction result
    fun clearSingleCharPrediction() {
        _singleCharPredictionResult.value = null
    }
}