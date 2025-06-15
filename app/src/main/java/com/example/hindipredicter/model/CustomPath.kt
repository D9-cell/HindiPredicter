package com.example.hindipredicter.model

import android.graphics.Path

class CustomPath(var color: Int, var brushThickness: Float) : Path() {
    val coordinates = mutableListOf<Triple<Int, Int, Int>>() // Now stores (Int, Int, Int)

    override fun toString(): String {
        return "CustomPath(color=$color, brushThickness=$brushThickness, coordinates=$coordinates)"
    }
}