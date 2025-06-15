package com.example.hindipredicter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF // Import RectF for drawing rectangles
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.hindipredicter.model.CustomPath
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.hindipredicter.ai.getProcessedFeaturesForCharacter // Required for predictCharacterInSquare

class DrawingView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 7.toFloat()
    private var color = Color.BLACK
    private var canvas: Canvas? = null

    // Paths to store drawn lines
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()
    private val mRedoPaths = ArrayList<CustomPath>()

    // List to store segmented characters' coordinates. Each inner list represents one character.
    private val mSegmentedCharactersCoordinates = mutableListOf<MutableList<Triple<Int, Int, Int>>>()
    // Temporary list to accumulate coordinates for the character currently being drawn.
    private var currentCharacterCoordinates = mutableListOf<Triple<Int, Int, Int>>()

    // Timestamp of the last ACTION_UP event
    private var lastActionUpTime: Long = 0L

    // NOTE: The 'allCoordinates' variable here might become redundant for prediction purposes
    // since we are now segmenting characters. Keep it if it's used elsewhere for full canvas data.
    private val allCoordinates = mutableListOf<List<Triple<Int, Int, Int>>>()

    // NEW: Drawing Mode
    enum class DrawingMode { FREEHAND, SQUARE }
    private var currentDrawingMode: DrawingMode = DrawingMode.FREEHAND

    // NEW: Variables for Square Drawing
    private var currentSquare: RectF? = null
    private var startSquareX: Float = 0f
    private var startSquareY: Float = 0f

    // NEW: List to store drawn squares and their predictions
    data class PredictedSquare(val rect: RectF, var predictedChar: String? = null)
    private val mPredictedSquares = mutableListOf<PredictedSquare>()

    // NEW: Paint for drawing squares
    private var mSquarePaint: Paint? = null
    private var mTextPaint: Paint? = null // For drawing predicted text

    // NEW: Callback for prediction
    var onPredictCharacterInSquare: ((RectF, List<Double>) -> Unit)? = null


    init {
        setUpDrawing()
    }

    // Function to update the coordinates list (potentially less relevant for segmented prediction)
    private fun updateAllCoordinates() {
        allCoordinates.clear()
        allCoordinates.addAll(mPaths.map { it.coordinates })
    }

    // Clear the canvas
    fun clearCanvas() {
        mPaths.clear()
        mUndoPaths.clear()
        mRedoPaths.clear()
        mSegmentedCharactersCoordinates.clear() // Clear segmented characters
        currentCharacterCoordinates.clear() // Clear current character buffer
        lastActionUpTime = 0L // Reset the timestamp on clear
        mPredictedSquares.clear() // NEW: Clear drawn squares
        currentSquare = null // NEW: Clear current square in progress
        updateAllCoordinates() // Update after clear
        invalidate()
    }

    private fun setUpDrawing() {
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)

        // NEW: Setup for square drawing paint
        mSquarePaint = Paint().apply {
            color = Color.BLUE // Default square color
            style = Paint.Style.STROKE
            strokeWidth = 5f // Square line thickness
        }

        // NEW: Setup for text paint
        mTextPaint = Paint().apply {
            color = Color.RED // Default text color
            textSize = 30f // Text size
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = createBitmap(w, h)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        for (path in mPaths) {
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }
        if (!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPaint!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

        // NEW: Draw any squares that have been finalized
        for (predictedSquare in mPredictedSquares) {
            canvas.drawRect(predictedSquare.rect, mSquarePaint!!)
            predictedSquare.predictedChar?.let { char ->
                // Draw text slightly above and to the right of the top-right corner
                canvas.drawText(char, predictedSquare.rect.right, predictedSquare.rect.top - 10, mTextPaint!!)
            }
        }

        // NEW: Draw the current square being drawn (if any)
        currentSquare?.let {
            canvas.drawRect(it, mSquarePaint!!)
        }
    }

    // Redo functionality - consider how this interacts with character segmentation
    fun onClickRedo() {
        if (mUndoPaths.isNotEmpty()) {
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            // For multi-character redo, you'd need more complex logic to
            // re-add paths to the correct currentCharacterCoordinates or mSegmentedCharactersCoordinates.
            // For simplicity, we're not fully integrating redo with the new segmentation for now.
            updateAllCoordinates()
            Log.d("DrawingView","After Redo : $allCoordinates")
            Log.d("DrawingView","After Redo : ${allCoordinates.size}")
            invalidate()
        }
    }

    // Undo functionality - consider how this interacts with character segmentation
    fun onClickUndo() {
        if (mPaths.isNotEmpty()) {
            val lastPath = mPaths.removeAt(mPaths.size - 1)
            mUndoPaths.add(lastPath)
            mRedoPaths.clear()
            // If the last path removed was the only one in currentCharacterCoordinates,
            // you might need to adjust currentCharacterCoordinates or pop from mSegmentedCharactersCoordinates.
            // This is a more advanced undo for segmented characters.
            updateAllCoordinates()
            Log.d("DrawingView","After Undo : $allCoordinates")
            Log.d("DrawingView","After Undo : ${allCoordinates.size}")
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x ?: return false
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (currentDrawingMode) {
                    DrawingMode.FREEHAND -> {
                        val currentTime = System.currentTimeMillis()
                        val timeSinceLastUp = currentTime - lastActionUpTime
                        val NEW_CHARACTER_THRESHOLD = 3000L // 3 seconds

                        Log.d("DrawingView", "ACTION_DOWN (FREEHAND): currentCharacterCoordinates size = ${currentCharacterCoordinates.size}, timeSinceLastUp = $timeSinceLastUp ms")

                        if (currentCharacterCoordinates.isNotEmpty() && timeSinceLastUp > NEW_CHARACTER_THRESHOLD) {
                            mSegmentedCharactersCoordinates.add(currentCharacterCoordinates) // Add the completed character
                            currentCharacterCoordinates = mutableListOf() // Start new character buffer
                            Log.d("DrawingView", "New character started after a ${timeSinceLastUp}ms gap.")
                        }

                        mDrawPath = CustomPath(color, mBrushSize)
                        mDrawPath!!.color = color
                        mDrawPath!!.brushThickness = mBrushSize
                        mDrawPath!!.reset()
                        mDrawPath!!.moveTo(touchX, touchY)

                        mDrawPath!!.coordinates.add(Triple(touchX.toInt(), touchY.toInt(), 1)) // 1 for pen down
                        currentCharacterCoordinates.add(Triple(touchX.toInt(), touchY.toInt(), 1))
                    }
                    DrawingMode.SQUARE -> {
                        startSquareX = touchX
                        startSquareY = touchY
                        currentSquare = RectF(startSquareX, startSquareY, startSquareX, startSquareY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (currentDrawingMode) {
                    DrawingMode.FREEHAND -> {
                        mDrawPath!!.lineTo(touchX, touchY)
                        mDrawPath!!.coordinates.add(Triple(touchX.toInt(), touchY.toInt(), 1)) // 1 for pen move
                        currentCharacterCoordinates.add(Triple(touchX.toInt(), touchY.toInt(), 1))
                    }
                    DrawingMode.SQUARE -> {
                        currentSquare?.set(
                            minOf(startSquareX, touchX),
                            minOf(startSquareY, touchY),
                            maxOf(startSquareX, touchX),
                            maxOf(startSquareY, touchY)
                        )
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                when (currentDrawingMode) {
                    DrawingMode.FREEHAND -> {
                        mDrawPath!!.coordinates.add(Triple(touchX.toInt(), touchY.toInt(), 0)) // 0 for pen up
                        mPaths.add(mDrawPath!!) // Add the completed stroke to mPaths
                        currentCharacterCoordinates.add(Triple(touchX.toInt(), touchY.toInt(), 0)) // Add to current char buffer

                        updateAllCoordinates()
                        Log.d("DrawingView", "Updated allCoordinates: $allCoordinates")

                        lastActionUpTime = System.currentTimeMillis() // Record the time of ACTION_UP
                        mDrawPath = CustomPath(color, mBrushSize)
                    }
                    DrawingMode.SQUARE -> {
                        currentSquare?.let {
                            // Ensure the square has valid dimensions
                            if (it.width() > 0 && it.height() > 0) {
                                mPredictedSquares.add(PredictedSquare(RectF(it))) // Add a copy
                                Log.d("DrawingView", "Square drawn: $it")
                                // Automatically predict when a square is drawn
                                predictCharacterInSquare(it)
                            }
                        }
                        currentSquare = null // Clear the current square in progress
                    }
                }
            }
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    fun setColor(currentColor: String) {
        color = currentColor.toColorInt()
        mDrawPaint!!.color = color
        invalidate()
    }

    fun getAllCoordinates(): List<List<Triple<Int, Int, Int>>> {
        return allCoordinates
    }

    fun getSegmentedCharactersCoordinates(): List<List<Triple<Int, Int, Int>>> {
        if (currentCharacterCoordinates.isNotEmpty()) {
            mSegmentedCharactersCoordinates.add(currentCharacterCoordinates.toMutableList())
            currentCharacterCoordinates.clear()
        }
        return mSegmentedCharactersCoordinates
    }

    // NEW: Function to set the drawing mode
    fun setDrawingMode(mode: DrawingMode) {
        currentDrawingMode = mode
        Log.d("DrawingView", "Drawing mode set to: $currentDrawingMode")
        Toast.makeText(context, "Drawing Mode: $currentDrawingMode", Toast.LENGTH_SHORT).show()
    }

    // NEW: Function to extract and predict character within a given square
    private fun predictCharacterInSquare(squareRect: RectF) {
        val characterPointsInSquare = mutableListOf<Triple<Int, Int, Int>>()

        // Iterate through all drawn paths (strokes)
        for (path in mPaths) {
            // Iterate through all points in the current path
            for (pointTriple in path.coordinates) {
                val x = pointTriple.first.toFloat()
                val y = pointTriple.second.toFloat()
                val penState = pointTriple.third

                // Check if the point is within the square
                if (squareRect.contains(x, y)) {
                    characterPointsInSquare.add(Triple(x.toInt(), y.toInt(), penState))
                }
            }
        }

        if (characterPointsInSquare.isNotEmpty()) {
            val predictedFeatures = getProcessedFeaturesForCharacter(characterPointsInSquare)
            Log.d("DrawingView", "Features for square prediction: $predictedFeatures")

            val squareToUpdate = mPredictedSquares.find { it.rect == squareRect }
            squareToUpdate?.predictedChar = "Predicting..." // Initial state

            // Trigger actual prediction (Requires access to CharacterViewModel)
            onPredictCharacterInSquare?.invoke(squareRect, predictedFeatures)

        } else {
            val squareToUpdate = mPredictedSquares.find { it.rect == squareRect }
            squareToUpdate?.predictedChar = "No Char"
            Log.d("DrawingView", "No character points found within the drawn square.")
        }
        invalidate() // Redraw to show "Predicting..." or "No Char"
    }

    fun updatePredictedCharacter(squareRect: RectF, predictedChar: String) {
        val squareToUpdate = mPredictedSquares.find { it.rect == squareRect }
        if (squareToUpdate != null) {
            squareToUpdate.predictedChar = predictedChar
            invalidate() // Redraw to show the actual prediction
        }
    }


    fun getBitmap(currentBackgroundColor: androidx.compose.ui.graphics.Color): Bitmap {
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(currentBackgroundColor.toArgb())
        draw(canvas)
        return bitmap
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): Uri? {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Drawing_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving drawing: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}