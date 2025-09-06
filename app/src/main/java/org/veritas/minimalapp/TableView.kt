package org.veritas.minimalapp // Перевірте, чи ім'я пакета ваше

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class TableView(
    context: Context,
    private val rows: Int,
    private val cols: Int
) : View(context) {

    // --- Змінні для малювання та стилів ---
    private val gridPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }
    private val cellBackgroundPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    // --- Змінні для масштабування та панорамування ---
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    // НОВЕ: Детектор для панорамування та довгих натискань
    private val gestureDetector = GestureDetector(context, GestureListener())

    // --- Змінні для логіки ---
    private var cellWidth = 250f
    private var cellHeight = 150f
    private val cellData = mutableMapOf<Pair<Int, Int>, CellData>()

    // --- Інтерфейс для комунікації з MainActivity ---
    var onCellClickListener: ((row: Int, col: Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        // Застосовуємо зсув (панорамування)
        canvas.translate(offsetX, offsetY)
        // Застосовуємо масштаб
        canvas.scale(scaleFactor, scaleFactor)

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
                    val text = "$daysPassed днів"
                    val textX = left + cellWidth / 2
                    val textY = top + cellHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2
                    canvas.drawText(text, textX, textY, textPaint)
                }
            }
        }
        canvas.restore()
    }

    // --- ПОВНІСТЮ ПЕРЕПИСАНИЙ МЕТОД ОБРОБКИ ДОТИКІВ ---
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Передаємо подію обом детекторам. Вони самі розберуться, який жест виконується.
        val scaleHandled = scaleGestureDetector.onTouchEvent(event)
        val gestureHandled = gestureDetector.onTouchEvent(event)
        // Повертаємо true, якщо хоча б один детектор обробив подію
        return scaleHandled || gestureHandled || super.onTouchEvent(event)
    }

    fun updateCell(row: Int, col: Int, date: LocalDate) {
        cellData[row to col] = CellData(date)
        invalidate() // Перемальовуємо View
    }

    // --- НОВЕ: Обробник жестів панорамування та довгих натискань ---
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        // Обов'язково повернути true, щоб детектор почав працювати
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // ВИРІШЕННЯ ПРОБЛЕМИ 2: Панорамування таблиці
        override fun onScroll(
            e1: MotionEvent?, // Початкова точка
            e2: MotionEvent, // Поточна точка
            distanceX: Float, // Різниця по X
            distanceY: Float  // Різниця по Y
        ): Boolean {
            offsetX -= distanceX
            offsetY -= distanceY
            invalidate() // Перемальовуємо при переміщенні
            return true
        }

        // ВИРІШЕННЯ ПРОБЛЕМИ 1 і 3 та РЕКОМЕНДАЦІЯ: Вибір комірки довгим натисканням
        override fun onLongPress(e: MotionEvent) {
            // ВИПРАВЛЕНА ФОРМУЛА: Коректно розраховуємо комірку з урахуванням зсуву та масштабу
            val col = ((e.x - offsetX) / scaleFactor / cellWidth).toInt()
            val row = ((e.y - offsetY) / scaleFactor / cellHeight).toInt()

            if (row in 0 until rows && col in 0 until cols) {
                onCellClickListener?.invoke(row, col)
            }
        }
    }

    // --- ОНОВЛЕНО: Обробник жестів масштабування ---
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f)) // Обмеження масштабу

            // ВИПРАВЛЕНА ЛОГІКА: Коректно змінюємо зсув, щоб масштабування відбувалося "під пальцями"
            val focusX = detector.focusX
            val focusY = detector.focusY
            offsetX = focusX - (focusX - offsetX) * (scaleFactor / oldScaleFactor)
            offsetY = focusY - (focusY - offsetY) * (scaleFactor / oldScaleFactor)

            invalidate()
            return true
        }
    }
}