package com.cinetrack.data.repository.importers

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ImporterUtils {

    fun parseCsvRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            if (c == '"') {
                if (inQuotes && i + 1 < len && text[i + 1] == '"') {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                currentRecord.add(currentField.toString().trim())
                currentField.clear()
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < len && text[i + 1] == '\n') {
                    i++
                }
                currentRecord.add(currentField.toString().trim())
                currentField.clear()
                if (currentRecord.any { it.isNotBlank() }) {
                    records.add(currentRecord)
                }
                currentRecord = mutableListOf()
            } else {
                currentField.append(c)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(currentRecord)
            }
        }
        return records
    }

    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    current.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    fun parseAndNormalizeWatchedDate(rawDate: String?): String? {
        if (rawDate.isNullOrBlank()) return null
        val trimmed = rawDate.trim()
        try {
            Instant.parse(trimmed)
            return trimmed
        } catch (_: Exception) {}

        try {
            val cleanDate = trimmed.replace("/", "-").take(10)
            val localDate = LocalDate.parse(cleanDate)
            return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString()
        } catch (_: Exception) {}

        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val localDateTime = LocalDateTime.parse(trimmed, formatter)
            return localDateTime.toInstant(ZoneOffset.UTC).toString()
        } catch (_: Exception) {}

        return null
    }
}
