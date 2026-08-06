package com.cinetrack.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationManager @Inject constructor() {

    private var currentTargetLanguage: String = TranslateLanguage.ITALIAN
    private var translator: Translator = buildTranslator(currentTargetLanguage)

    private fun buildTranslator(targetLang: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(targetLang)
            .build()
        return Translation.getClient(options)
    }

    /**
     * Updates the target language if it changed. Rebuilds the translator if needed.
     * @param langCode The app language code (e.g. "it", "es", "de", "fr", "en", "system")
     * @param systemLang The device locale language tag to use when langCode == "system"
     */
    fun setTargetLanguage(langCode: String, systemLang: String) {
        val resolvedLang = if (langCode == "system") systemLang else langCode
        val mlkitLang = mapToMlKitLanguage(resolvedLang)
        if (mlkitLang != currentTargetLanguage) {
            currentTargetLanguage = mlkitLang
            translator = buildTranslator(mlkitLang)
        }
    }

    private fun mapToMlKitLanguage(lang: String): String {
        return when (lang.lowercase().take(2)) {
            "it" -> TranslateLanguage.ITALIAN
            "es" -> TranslateLanguage.SPANISH
            "fr" -> TranslateLanguage.FRENCH
            "de" -> TranslateLanguage.GERMAN
            else -> TranslateLanguage.ITALIAN // default fallback (en → no translation needed)
        }
    }

    /**
     * Checks if the current target language model is already downloaded.
     */
    suspend fun isModelDownloaded(): Boolean {
        return try {
            val modelManager = RemoteModelManager.getInstance()
            val model = TranslateRemoteModel.Builder(currentTargetLanguage).build()
            modelManager.isModelDownloaded(model).await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Tries to download the ML Kit model if not present.
     * @param requireWifi If true, forces download only on Wi-Fi.
     * @return true if successful, false otherwise.
     */
    suspend fun downloadModel(requireWifi: Boolean): Boolean {
        return try {
            val conditionsBuilder = DownloadConditions.Builder()
            if (requireWifi) {
                conditionsBuilder.requireWifi()
            }
            val conditions = conditionsBuilder.build()
            translator.downloadModelIfNeeded(conditions).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Translates a given text from English to the current target language.
     * Assumes the model is already downloaded.
     */
    suspend fun translate(text: String): String? {
        return try {
            translator.translate(text).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
