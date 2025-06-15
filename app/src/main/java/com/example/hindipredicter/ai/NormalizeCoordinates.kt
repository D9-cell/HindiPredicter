package com.example.hindipredicter.ai

import kotlin.math.sqrt
import kotlin.math.round
import kotlin.math.roundToInt

fun computeCG(points: List<Pair<Double, Double>>): Pair<Double, Double> {
    if (points.isEmpty()) return Pair(0.0, 0.0)
    val sumX = points.sumOf { it.first }
    val sumY = points.sumOf { it.second }
    val count = points.size.toDouble()
    return Pair(round(sumX / count * 100) / 100, round(sumY / count * 100) / 100)
}

fun calculateDistance(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
    return round(sqrt((p2.first - p1.first) * (p2.first - p1.first) + (p2.second - p1.second) * (p2.second - p1.second)) * 100) / 100
}

fun processSingleListDouble(points: List<Pair<Double, Double>>): List<Double> {
    if (points.isEmpty()) return emptyList()

    val mainCG = computeCG(points)
    val (CGx, CGy) = mainCG

    val quadrants = mutableMapOf(
        "Q1" to mutableListOf<Pair<Double, Double>>(),
        "Q2" to mutableListOf(),
        "Q3" to mutableListOf(),
        "Q4" to mutableListOf()
    )

    for ((x, y) in points) {
        when {
            x >= CGx && y >= CGy -> quadrants["Q1"]?.add(Pair(x, y))
            x < CGx && y >= CGy -> quadrants["Q2"]?.add(Pair(x, y))
            x < CGx && y < CGy -> quadrants["Q3"]?.add(Pair(x, y))
            else -> quadrants["Q4"]?.add(Pair(x, y))
        }
    }

    val quadrantCGs = quadrants.mapValues { if (it.value.isNotEmpty()) computeCG(it.value) else mainCG }
    val quadrantCounts = quadrants.mapValues { it.value.size }

    val distancesMainToQuadrants = listOf("Q1", "Q2", "Q3", "Q4").map { calculateDistance(mainCG, quadrantCGs[it]!!) }
    val distancesMainToPoints = points.map { calculateDistance(mainCG, it) }
    val firstPoint = points.firstOrNull() ?: Pair(0.0, 0.0)
    val distancesFirstToAll = points.map { calculateDistance(firstPoint, it) }

    return listOf(
        mainCG.first, mainCG.second,
        quadrantCGs["Q1"]!!.first, quadrantCGs["Q1"]!!.second,
        quadrantCGs["Q2"]!!.first, quadrantCGs["Q2"]!!.second,
        quadrantCGs["Q3"]!!.first, quadrantCGs["Q3"]!!.second,
        quadrantCGs["Q4"]!!.first, quadrantCGs["Q4"]!!.second,
        quadrantCounts["Q1"]!!.toDouble(), quadrantCounts["Q2"]!!.toDouble(),
        quadrantCounts["Q3"]!!.toDouble(), quadrantCounts["Q4"]!!.toDouble(),
        *distancesMainToQuadrants.toTypedArray(),
        *distancesMainToPoints.toTypedArray(),
        *distancesFirstToAll.toTypedArray()
    )
}

fun interpolatePointsToTargetCount(points: List<Pair<Int, Int>>, targetCount: Int = 100): List<Pair<Int, Int>> {
    if (points.size < 2) return points

    val newPoints = mutableListOf<Pair<Int, Int>>()
    val totalSegments = points.size - 1
    val pointsPerSegment = maxOf(1, targetCount / totalSegments)

    for (i in 0 until totalSegments) {
        val p1 = points[i]
        val p2 = points[i + 1]
        newPoints.add(p1)

        for (j in 1 until pointsPerSegment) {
            val t = j.toDouble() / pointsPerSegment
            val x = p1.first + t * (p2.first - p1.first)
            val y = p1.second + t * (p2.second - p1.second)
            newPoints.add(Pair(x.roundToInt(), y.roundToInt()))
        }
    }

    newPoints.add(points.last())

    // Trim to exactly `targetCount` if needed
    return if (newPoints.size > targetCount) {
        newPoints.subList(0, targetCount)
    } else {
        // If still less than targetCount, pad with last point
        while (newPoints.size < targetCount) {
            newPoints.add(newPoints.last())
        }
        newPoints
    }
}

// ORIGINAL: fun processList(nestedList: List<List<Triple<Int, Int, Int>>>): List<Pair<Int, Int>>
// This function processed all coordinates. We will keep it if it's used elsewhere,
// but the new getProcessedFeaturesForCharacter will use the new processListFromSingleCharacter.
fun processList(nestedList: List<List<Triple<Int, Int, Int>>>): List<Pair<Int, Int>> {
    val flatList = nestedList.flatten()
    val pairList = convertToPairList(flatList)
    return interpolatePointsToTargetCount(pairList)
}


// NEW: Helper function to process coordinates for a single character
fun processListFromSingleCharacter(singleCharacterTriples: List<Triple<Int, Int, Int>>): List<Pair<Int, Int>> {
    val pairList = convertToPairList(singleCharacterTriples)
    return interpolatePointsToTargetCount(pairList)
}

fun convertToPairList(tripleList: List<Triple<Int, Int, Int>>): List<Pair<Int, Int>> {
    return tripleList.map { Pair(it.first, it.second) }
}

fun normalizePointsToWindow(points: List<Pair<Int, Int>>, targetSize: Int = 64, padding: Int = 2): List<Pair<Double, Double>> {
    if (points.isEmpty()) return List(points.size) { Pair(padding.toDouble(), padding.toDouble()) }

    val xs = points.map { it.first }
    val ys = points.map { it.second }

    val minX = xs.minOrNull() ?: 0
    val maxX = xs.maxOrNull() ?: 0
    val minY = ys.minOrNull() ?: 0
    val maxY = ys.maxOrNull() ?: 0

    val width = maxX - minX
    val height = maxY - minY

    if (width == 0 || height == 0) {
        // If the character is a single point or a vertical/horizontal line without extent,
        // normalize to a default padded point to avoid division by zero.
        return List(points.size) { Pair(padding.toDouble(), padding.toDouble()) }
    }

    val scale = (targetSize - 2 * padding).toDouble() / maxOf(width, height)

    return points.map { (x, y) ->
        val newX = (x - minX) * scale + padding
        val newY = (y - minY) * scale + padding
        Pair(newX, newY)
    }
}

fun flattenCoordinates(coordinates: List<Pair<Double, Double>>): List<Double> {
    return coordinates.flatMap { listOf(it.first, it.second) }
}

// ORIGINAL: fun getProcessedFeatures(drawingView: DrawingView): List<Double>
// Renamed and modified to accept character coordinates directly
fun getProcessedFeaturesForCharacter(characterCoordinates: List<Triple<Int, Int, Int>>): List<Double> {
    val cleanCoordinates = processListFromSingleCharacter(characterCoordinates) // Use the new helper
    val normalizedCoordinates = normalizePointsToWindow(cleanCoordinates)
    val flatCoordinates = flattenCoordinates(normalizedCoordinates) // This might not be used directly by your model, but kept for consistency

    println("Actual Input List : $normalizedCoordinates")
    println("Flattened Coordinates : $flatCoordinates")
    println("Length of the Flattened List : ${flatCoordinates.size}")

    val listOutput: List<Double> = processSingleListDouble(normalizedCoordinates)
    println("Actual Calculated Features : $listOutput")
    // Ensure this length matches your model's expected input size (218)
    println("Length of the List : ${listOutput.size} (Expected 18 + 2 * targetCount, where targetCount is 100)")

    return listOutput
}