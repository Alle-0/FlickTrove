import java.util.Locale

val releaseDate1 = "2021-12-17"
val releaseDate2 = "2022-07-20"
val locale = Locale("it", "IT")

// Funzione di confronto robusta (usa Calendar)
fun compareReleaseDates(date1: String, date2: String, locale: Locale): Int {
    val calendar1 = java.util.Calendar.getInstance(locale)
    val calendar2 = java.util.Calendar.getInstance(locale)

    // Estrai anno, mese, giorno e imposta i Calendar
    val (y1, m1, d1) = date1.split("-").map { it.toInt() }
    val (y2, m2, d2) = date2.split("-").map { it.toInt() }

    calendar1.set(y1, m1 - 1, d1) // Calendar usa mesi da 0 a 11
    calendar2.set(y2, m2 - 1, d2)

    // Confronta direttamente i Calendar
    return calendar1.compareTo(calendar2)
}

// Test del confronto robusto
val resultRobust = compareReleaseDates(releaseDate1, releaseDate2, locale)
println("Risultato confronto robusto: $resultRobust")

// Test con sortedByDescending
val sortedList = listOf(releaseDate1, releaseDate2).sortedByDescending { it }
println("Lista ordinata (sortedByDescending): $sortedList")
}
