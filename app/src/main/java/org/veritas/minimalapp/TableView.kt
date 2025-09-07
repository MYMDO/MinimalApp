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
//import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

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
        textSize = 96f // Ваша зміна збережена
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

    // НОВЕ: Прапорець, щоб логіка початкового центрування виконувалась лише раз
    private var isInitialFitDone = false

    init {
        backgroundDrawable = context.getDrawable(R.drawable.steve_minecraft_opt)
        loadData()
    }

    // НОВЕ: Метод, який викликається, коли розмір View стає відомим.
    // Ідеальне місце для початкового налаштування.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Виконуємо лише один раз, коли View вперше з'являється на екрані
        if (!isInitialFitDone && w > 0 && h > 0) {
            fitToScreen()
            isInitialFitDone = true
        }
    }

    // НОВЕ: Логіка для початкового масштабування та центрування
    private fun fitToScreen() {
        val drawable = backgroundDrawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Отримуємо "природні" розміри векторного зображення
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        if (imageWidth <= 0 || imageHeight <= 0) return

        // 1. КОМПЕНСАЦІЯ ПРОПОРЦІЙ:
        // Перераховуємо розміри комірок, щоб сітка точно відповідала пропорціям зображення
        cellWidth = imageWidth / cols
        cellHeight = imageHeight / rows

        // 2. МАСШТАБУВАННЯ:
        // Розраховуємо коефіцієнт масштабування, щоб вписати зображення в екран
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        scaleFactor = min(scaleX, scaleY) // min() гарантує, що зображення влізе повністю

        // 3. ЦЕНТРУВАННЯ:
        // Розраховуємо зсув, щоб відцентрувати зображення на екрані
        val scaledWidth = imageWidth * scaleFactor
        val scaledHeight = imageHeight * scaleFactor
        offsetX = (viewWidth - scaledWidth) / 2f
        offsetY = (viewHeight - scaledHeight) / 2f

        // Перемальовуємо View з новими, розрахованими параметрами
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
                    canvas.drawRect(left, top, right, bottom, cellBackgroundPaint)
                }
                canvas.drawRect(left, top, right, bottom, gridPaint)

                cellData[row to col]?.let { data ->
                    val daysPassed = ChronoUnit.DAYS.between(data.selectedDate, LocalDate.now())
                    val text = "$daysPassed" // Ваша зміна збережена
                    val textX = left + cellWidth / 2
                    val textY = top + cellHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                    canvas.drawText(text, textX, textY, textPaint)
                }
            }
        }
        canvas.restore()
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
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
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