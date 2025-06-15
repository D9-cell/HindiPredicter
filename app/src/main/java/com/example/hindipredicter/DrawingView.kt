package com.example.hindipredicter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    // NEW: Timestamp of the last ACTION_UP event
    private var lastActionUpTime: Long = 0L

    // NOTE: The 'allCoordinates' variable here might become redundant for prediction purposes
    // since we are now segmenting characters. Keep it if it's used elsewhere for full canvas data.
    private val allCoordinates = mutableListOf<List<Triple<Int, Int, Int>>>()

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
        val touchX = event?.x?.toInt() ?: return false
        val touchY = event.y.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUp = currentTime - lastActionUpTime
                val NEW_CHARACTER_THRESHOLD = 3000L // 3 seconds

                Log.d("DrawingView", "ACTION_DOWN: currentCharacterCoordinates size = ${currentCharacterCoordinates.size}, timeSinceLastUp = $timeSinceLastUp ms")

                // Condition for starting a new character:
                // 1. There are existing points in currentCharacterCoordinates (meaning a stroke was just completed)
                // 2. AND the time since the last ACTION_UP is greater than the threshold (3 seconds).
                // OR if it's the very first stroke on a fresh canvas, it's always a new character.
                if (currentCharacterCoordinates.isNotEmpty() && timeSinceLastUp > NEW_CHARACTER_THRESHOLD) {
                    mSegmentedCharactersCoordinates.add(currentCharacterCoordinates) // Add the completed character
                    currentCharacterCoordinates = mutableListOf() // Start new character buffer
                    Log.d("DrawingView", "New character started after a ${timeSinceLastUp}ms gap.")
                }
                // If currentCharacterCoordinates is empty, it's either the very first stroke or
                // getSegmentedCharactersCoordinates() has already moved the last character to segments.
                // In these cases, we simply continue/start the currentCharacterCoordinates.


                mDrawPath = CustomPath(color, mBrushSize) // Always create a new path for a new stroke
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize
                mDrawPath!!.reset()
                mDrawPath!!.moveTo(touchX.toFloat(), touchY.toFloat())

                mDrawPath!!.coordinates.add(Triple(touchX, touchY, 1)) // 1 for pen down
                currentCharacterCoordinates.add(Triple(touchX, touchY, 1))
            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath!!.lineTo(touchX.toFloat(), touchY.toFloat())
                mDrawPath!!.coordinates.add(Triple(touchX, touchY, 1)) // 1 for pen move
                currentCharacterCoordinates.add(Triple(touchX, touchY, 1))
            }
            MotionEvent.ACTION_UP -> {
                mDrawPath!!.coordinates.add(Triple(touchX, touchY, 0)) // 0 for pen up
                mPaths.add(mDrawPath!!) // Add the completed stroke to mPaths
                currentCharacterCoordinates.add(Triple(touchX, touchY, 0)) // Add to current char buffer

                updateAllCoordinates() // If you still use this, ensure it's here
                Log.d("DrawingView", "Updated allCoordinates: $allCoordinates")

                lastActionUpTime = System.currentTimeMillis() // NEW: Record the time of ACTION_UP
                mDrawPath = CustomPath(color, mBrushSize) // Reset mDrawPath for the next stroke
            }
        }
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float) {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    // Set color method
    fun setColor(currentColor: String) {
        color = currentColor.toColorInt()
        mDrawPaint!!.color = color
        invalidate()
    }

    fun getAllCoordinates(): List<List<Triple<Int, Int, Int>>> {
        return allCoordinates
    }

    // Get the segmented characters' coordinates
    fun getSegmentedCharactersCoordinates(): List<List<Triple<Int, Int, Int>>> {
        // If there are any un-added strokes for the current character when this is called (e.g., predict button pressed),
        // add them to the segmented list. This ensures the last drawn character is also included.
        if (currentCharacterCoordinates.isNotEmpty()) {
            // Make a defensive copy to prevent concurrent modification issues
            // if drawing continues while prediction is happening.
            mSegmentedCharactersCoordinates.add(currentCharacterCoordinates.toMutableList())
            currentCharacterCoordinates.clear() // Clear for next drawing session
        }
        return mSegmentedCharactersCoordinates
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