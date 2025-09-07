package org.veritas.minimalapp // Перевірте, чи ім'я пакета ваше

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import java.time.LocalDate
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var tableView: TableView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Налаштування таблиці ---
        val rows = 64 // Задайте бажану кількість рядків
        val cols = 32 // Задайте бажану кількість стовпців

        // 1. Створюємо екземпляр нашої кастомної TableView
        tableView = TableView(this, rows, cols)

        // 2. Встановлюємо її як головний вміст екрана
        setContentView(tableView)

        // 3. Встановлюємо слухач натискань на комірки
        tableView.onCellClickListener = { row, col ->
            showCalendarForCell(row, col)
        }
    }

    private fun showCalendarForCell(row: Int, col: Int) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Створюємо і показуємо стандартний DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Цей код виконається, коли користувач вибере дату
                val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                // Оновлюємо дані в нашій TableView
                tableView.updateCell(row, col, selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}