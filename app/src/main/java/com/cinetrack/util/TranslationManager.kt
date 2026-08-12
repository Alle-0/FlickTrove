package com.cinetrack.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles on-device translation using ML Kit.
 *
 * Gotcha 1 (short texts): identifyLanguage() returns "und" for very short/ambiguous texts.
 *   → We return null in that case so the UI can skip translation silently.
 *
 * Gotcha 2 (model downloads): Each source/target language pair requires a ~30MB model download.
 *   → We expose isModelDownloaded() and downloadModel() so the caller can manage download state.
 *
 * Gotcha 3 (memory leaks): Translator instances hold native resources.
 *   → We cache them in translatorsCache and expose closeAll() to release all memory on app destroy.
 */
@Singleton
class TranslationManager @Inject constructor() {

    // Cache of Translator instances keyed by "sourceLang_targetLang"
    private val translatorsCache = mutableMapOf<String, Translator>()

    // The user's current target language (ML Kit code)
    private var currentTargetLanguage: String = TranslateLanguage.ITALIAN

    // Legacy single-translator (EN→target), kept for existing movie overview translations
    private var legacyTranslator: Translator = buildTranslator(TranslateLanguage.ENGLISH, currentTargetLanguage)

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — target language
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the target language. Rebuilds the legacy translator if needed.
     * Call this whenever the user changes their content language preference.
     */
    fun setTargetLanguage(langCode: String, systemLang: String) {
        val resolved = if (langCode == "system") systemLang else langCode
        val mlkitLang = mapToMlKitLanguage(resolved)
        if (mlkitLang != currentTargetLanguage) {
            currentTargetLanguage = mlkitLang
            legacyTranslator = buildTranslator(TranslateLanguage.ENGLISH, mlkitLang)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — comment translation (any → target)
    // ─────────────────────────────────────────────────────────────────────────

    fun getCurrentTargetLanguage(): String = currentTargetLanguage

    /**
     * Identifies the language of [text].
     * Returns null if confidence is low or language is undetermined ("und").
     */
    suspend fun identifyLanguage(text: String): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return try {
            val identifier = LanguageIdentification.getClient(
                LanguageIdentificationOptions.Builder()
                    .setConfidenceThreshold(0.3f) // Identify even short phrases
                    .build()
            )
            val lang = identifier.identifyLanguage(clean).await()
            identifier.close()
            if (lang == "und") null else lang
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the model for the given source→target pair is already on-device.
     */
    suspend fun isModelDownloaded(sourceLang: String, targetLang: String = currentTargetLanguage): Boolean {
        return try {
            val modelManager = RemoteModelManager.getInstance()
            val sourceModel = TranslateRemoteModel.Builder(sourceLang).build()
            val targetModel = TranslateRemoteModel.Builder(targetLang).build()
            modelManager.isModelDownloaded(sourceModel).await() &&
                    modelManager.isModelDownloaded(targetModel).await()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Downloads models for the given source→target pair if needed (Gotcha 2).
     * @param requireWifi If true, postpones download until Wi-Fi is available.
     */
    suspend fun downloadModels(
        sourceLang: String,
        targetLang: String = currentTargetLanguage,
        requireWifi: Boolean = false
    ): Boolean {
        return try {
            val conditions = DownloadConditions.Builder()
                .apply { if (requireWifi) requireWifi() }
                .build()
            getOrCreateTranslator(sourceLang, targetLang).downloadModelIfNeeded(conditions).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Translates [text] explicitly from [sourceLang] to [targetLang].
     */
    suspend fun translateFrom(text: String, sourceLang: String, targetLang: String = currentTargetLanguage): String? {
        return try {
            val translator = getOrCreateTranslator(sourceLang, targetLang)
            translator.translate(text).await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Translates [text] from any detected language to the user's target language.
     * The caller must call downloadModels() first (and show a loading indicator).
     */
    suspend fun translateFromAny(text: String, targetLang: String = currentTargetLanguage): String? {
        val sourceLang = identifyLanguage(text) ?: return null
        // No need to translate if already in the target language
        val targetBcp47 = mapMlKitToBcp47(targetLang)
        if (sourceLang == targetBcp47 || sourceLang == targetLang) return text
        return translateFrom(text, sourceLang, targetLang)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — legacy (EN→target, used for movie overviews)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun isModelDownloaded(): Boolean =
        isModelDownloaded(TranslateLanguage.ENGLISH, currentTargetLanguage)

    suspend fun downloadModel(requireWifi: Boolean): Boolean =
        downloadModels(TranslateLanguage.ENGLISH, currentTargetLanguage, requireWifi)

    suspend fun translate(text: String): String? {
        return try {
            legacyTranslator.translate(text).await()
        } catch (e: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup (Gotcha 3 — release all Translator native resources)
    // ─────────────────────────────────────────────────────────────────────────

    fun closeAll() {
        translatorsCache.values.forEach { runCatching { it.close() } }
        translatorsCache.clear()
        runCatching { legacyTranslator.close() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun getOrCreateTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "${sourceLang}_${targetLang}"
        return translatorsCache.getOrPut(key) { buildTranslator(sourceLang, targetLang) }
    }

    private fun buildTranslator(sourceLang: String, targetLang: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        return Translation.getClient(options)
    }

    private fun mapToMlKitLanguage(lang: String): String {
        return when (lang.lowercase().take(2)) {
            "it" -> TranslateLanguage.ITALIAN
            "es" -> TranslateLanguage.SPANISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            "pt" -> TranslateLanguage.PORTUGUESE
            "zh" -> TranslateLanguage.CHINESE
            "ja" -> TranslateLanguage.JAPANESE
            "ko" -> TranslateLanguage.KOREAN
            "ru" -> TranslateLanguage.RUSSIAN
            "ar" -> TranslateLanguage.ARABIC
            else -> TranslateLanguage.ENGLISH
        }
    }

    /** Maps ML Kit language constants back to BCP-47 codes for comparison */
    fun mapMlKitToBcp47(mlkitLang: String): String {
        return when (mlkitLang) {
            TranslateLanguage.ITALIAN -> "it"
            TranslateLanguage.SPANISH -> "es"
            TranslateLanguage.FRENCH -> "fr"
            TranslateLanguage.GERMAN -> "de"
            TranslateLanguage.PORTUGUESE -> "pt"
            TranslateLanguage.CHINESE -> "zh"
            TranslateLanguage.JAPANESE -> "ja"
            TranslateLanguage.KOREAN -> "ko"
            TranslateLanguage.RUSSIAN -> "ru"
            TranslateLanguage.ARABIC -> "ar"
            else -> "en"
        }
    }
}
