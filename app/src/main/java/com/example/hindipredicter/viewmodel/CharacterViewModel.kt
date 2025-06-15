package com.example.hindipredicter.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.hindipredicter.ai.TFLiteClassifierHindi // Corrected import

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    // NEW: _predictionResult now holds a list of strings for multiple predictions
    private val _predictionResult = mutableStateOf<List<String>?>(null)
    val predictionResult: State<List<String>?> = _predictionResult

    private val classifier = TFLiteClassifierHindi(application.applicationContext)

    // Original predictCharacter (can be kept if needed elsewhere for single predictions, or removed)
    // Note: This won't be called by BrushBottomSheet anymore for multi-char prediction.
    fun predictCharacter(features: List<Double>) {
        try {
            val predictedIndex = classifier.predict(features)
            val labelClasses = listOf(
                "A/अ/অ", "AA/आ/আ", "ADA/ढ़/ঢ", "AN/ं/ং",
                "BA/ब/ব", "BHA/भ/ভ", "BI/ः/ঃ",
                "C/च/চ", "CH/ছ/ছ", "CN/ँ/ঁ",
                "DA/द/द", "DDA/ड/ड", "DDH/ढ/ঢ", "DHA/ध/ध", "DRA/ड़/ড়",
                "E/ए/এ", "EN/ञ/ঞ",
                "G/ग/ग", "GH/ঘ/ঘ",
                "HA/ह/ह",
                "I/इ/इ", "II/ई/ঈ",
                "JA/ज/ज", "JH/झ/ঝ",
                "K/क/क", "KH/ख/খ", "KT/त্/ৎ",
                "LA/ल/ল",
                "MA/म/म", "MN/ण/ण", "MSA/ष/ष",
                "NA/न/न",
                "O/ओ/ओ", "OI/ऐ/ঐ", "OU/औ/ঔ",
                "PA/प/প", "PHA/ফ/ফ",
                "RA/র/র", "RI/ऋ/ঋ",
                "S/स/स", "SA/শ/শ",
                "T/ट/ट", "TA/ত/ত", "THA/थ/थ", "TTA/ঠ/ঠ",
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
            "A/अ/অ", "AA/आ/আ", "ADA/ढ़/ঢ়", "AN/ं/ং",
            "BA/ब/ব", "BHA/भ/ভ", "BI/ः/ঃ",
            "C/च/চ", "CH/छ/ছ", "CN/ँ/ঁ",
            "DA/द/দ", "DDA/ड/ড", "DDH/ढ/ঢ", "DHA/ध/ধ", "DRA/ड़/ড়",
            "E/ए/এ", "EN/ञ/ঞ",
            "G/ग/গ", "GH/घ/ঘ",
            "HA/ह/হ",
            "I/इ/ই", "II/ई/ঈ",
            "JA/ज/জ", "JH/झ/ঝ",
            "K/क/ক", "KH/ख/খ", "KT/त्/ৎ",
            "LA/ल/ল",
            "MA/म/ম", "MN/ण/ণ","MSA/ष/ষ",
            "NA/न/ন",
            "O/ओ/ও", "OI/ऐ/ঐ", "OU/औ/ঔ",
            "PA/प/প", "PHA/फ/ফ",
            "RA/र/র", "RI/ऋ/ঋ",
            "S/स/স", "SA/श/শ",
            "T/ट/ট", "TA/त/ত", "THA/थ/থ", "TTA/ठ/ঠ",
            "U/उ/উ", "UN/ङ/ঙ", "UU/ऊ/ঊ",
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


    fun clearPrediction() {
        _predictionResult.value = null // Clears the list of results
    }
}