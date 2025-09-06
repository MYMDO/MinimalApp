package org.veritas.minimalapp // Перевірте, чи ім'я пакета ваше

import android.app.Activity // 1. Імпортуємо базовий клас Activity
import android.os.Bundle
import android.widget.TextView

// 2. Наслідуємо наш клас від Activity
class MainActivity : Activity() {

    // 3. Це головний метод, який викликається при створенні екрана
    override fun onCreate(savedInstanceState: Bundle?) {
        // 4. Важливо викликати метод батьківського класу
        super.onCreate(savedInstanceState)

        // 5. Створюємо елемент інтерфейсу (текстове поле) прямо в коді
        val textView = TextView(this)
        textView.text = "Hello, Minimal World!"
        textView.textSize = 24f // Зробимо текст трохи більшим

        // 6. Встановлюємо цей елемент як вміст нашого екрана
        setContentView(textView)
    }
}