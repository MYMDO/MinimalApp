package org.veritas.minimalapp // ПЕРЕВІРТЕ, ЩО ЦЕЙ РЯДОК ЗБІГАЄТЬСЯ З ВАШИМ ПАКЕТОМ

import java.time.LocalDate

// Цей клас буде зберігати дату, прив'язану до комірки
data class CellData(val selectedDate: LocalDate)