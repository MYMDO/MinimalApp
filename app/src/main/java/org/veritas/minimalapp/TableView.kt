package org.veritas.minimalapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class TableView(
    context: Context,
    private val rows: Int,
    private val cols: Int
) : View(context) {

    private companion object {
        const val PREFS_NAME = "TableViewPrefs"
        const val KEY_CELL_DATA = "cell_data"
    }

    private val gridPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    private val cellBackgroundPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    private var backgroundDrawable: Drawable? = null
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    private var cellWidth = 250f
    private var cellHeight = 150f
    private val cellData = mutableMapOf<Pair<Int, Int>, CellData>()
    var onCellClickListener: ((row: Int, col: Int) -> Unit)? = null
    private var isInitialFitDone = false

    init {
        backgroundDrawable = context.getDrawable(R.drawable.steve_minecraft_opt)
        loadData()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        clearExpiredCells()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isInitialFitDone && w > 0 && h > 0) {
            fitToScreen()
            isInitialFitDone = true
        }
    }

    private fun fitToScreen() {
        val drawable = backgroundDrawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        if (imageWidth <= 0 || imageHeight <= 0) return
        cellWidth = imageWidth / cols
        cellHeight = imageHeight / rows
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        scaleFactor = min(scaleX, scaleY)
        val scaledWidth = imageWidth * scaleFactor
        val scaledHeight = imageHeight * scaleFactor
        offsetX = (viewWidth - scaledWidth) / 2f
        offsetY = (viewHeight - scaledHeight) / 2f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scaleFactor, scaleFactor)

        val totalTableWidth = (cols * cellWidth).toInt()
        val totalTableHeight = (rows * cellHeight).toInt()
        backgroundDrawable?.setBounds(0, 0, totalTableWidth, totalTableHeight)
        backgroundDrawable?.draw(canvas)

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val left = col * cellWidth
                val top = row * cellHeight
                val right = left + cellWidth
                val bottom = top + cellHeight

                if (cellData.containsKey(row to col)) {
                    val data = cellData[row to col]!!
                    val daysPassed = ChronoUnit.DAYS.between(data.selectedDate, LocalDate.now())

                    if (daysPassed <= 25) {
                        val cellColor = getColorForDays(daysPassed)
                        cellBackgroundPaint.color = cellColor
                        canvas.drawRect(left, top, right, bottom, cellBackgroundPaint)

                        val text = "$daysPassed"
                        val textX = left + cellWidth / 2
                        val textY = top + cellHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                        canvas.drawText(text, textX, textY, textPaint)
                    }
                }
                canvas.drawRect(left, top, right, bottom, gridPaint)
            }
        }
        canvas.restore()
    }

    private fun clearExpiredCells() {
        val today = LocalDate.now()
        val expiredKeys = mutableListOf<Pair<Int, Int>>()
        cellData.forEach { (key, data) ->
            val daysPassed = ChronoUnit.DAYS.between(data.selectedDate, today)
            if (daysPassed > 25) {
                expiredKeys.add(key)
            }
        }
        if (expiredKeys.isNotEmpty()) {
            expiredKeys.forEach { key ->
                cellData.remove(key)
            }
            saveData()
            invalidate()
        }
    }

    // --- ОНОВЛЕНО: Конструкція 'when' тепер з об'єктом ---
    private fun getColorForDays(days: Long): Int {
        val red = Color.RED
        val yellow = Color.YELLOW
        val green = Color.GREEN
        val transparentGreen = Color.argb(64, Color.red(green), Color.green(green), Color.blue(green))

        return when (days) { // 'days' тепер є об'єктом
            in 0..8 -> {
                val fraction = days.toFloat() / 9f
                val easedFraction = sqrt(fraction)
                interpolateColor(red, yellow, easedFraction)
            }
            in 9..25 -> {
                val fraction = (days - 9).toFloat() / (25f - 9f)
                val easedFraction = sqrt(fraction)
                interpolateColor(yellow, transparentGreen, easedFraction)
            }
            else -> Color.TRANSPARENT
        }
    }

    private fun interpolateColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val startA = Color.alpha(startColor)
        val startR = Color.red(startColor)
        val startG = Color.green(startColor)
        val startB = Color.blue(startColor)
        val endA = Color.alpha(endColor)
        val endR = Color.red(endColor)
        val endG = Color.green(endColor)
        val endB = Color.blue(endColor)
        val a = (startA + fraction * (endA - startA)).toInt()
        val r = (startR + fraction * (endR - startR)).toInt()
        val g = (startG + fraction * (endG - startG)).toInt()
        val b = (startB + fraction * (endB - startB)).toInt()
        return Color.argb(a, r, g, b)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)
        val gestureHandled = gestureDetector.onTouchEvent(event)
        return scaleHandled || gestureHandled || super.onTouchEvent(event)
    }

    fun updateCell(row: Int, col: Int, date: LocalDate) {
        cellData[row to col] = CellData(date)
        saveData()
        invalidate()
    }

    private fun saveData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val dataToSave = cellData.map { (coords, data) ->
            "${coords.first},${coords.second}:${data.selectedDate}"
        }.toSet()
        editor.putStringSet(KEY_CELL_DATA, dataToSave)
        editor.apply()
    }

    private fun loadData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDataSet = prefs.getStringSet(KEY_CELL_DATA, emptySet()) ?: return
        cellData.clear()
        savedDataSet.forEach { entryString ->
            try {
                val (coordsStr, dateStr) = entryString.split(":")
                val (rowStr, colStr) = coordsStr.split(",")
                val row = rowStr.toInt()
                val col = colStr.toInt()
                val date = LocalDate.parse(dateStr)
                cellData[row to col] = CellData(date)
            } catch (e: Exception) {
                // Ignore malformed data
            }
        }
        invalidate()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean { return true }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate()
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            val col = ((e.x - offsetX) / scaleFactor / cellWidth).toInt()
            val row = ((e.y - offsetY) / scaleFactor / cellHeight).toInt()
            if (row in 0 until rows && col in 0 until cols) {
                onCellClickListener?.invoke(row, col)
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f))
            val focusX = detector.focusX
            val focusY = detector.focusY
            offsetX = focusX - (focusX - offsetX) * (scaleFactor / oldScaleFactor)
            offsetY = focusY - (focusY - offsetY) * (scaleFactor / oldScaleFactor)
            invalidate()
            return true
        }
    }
}