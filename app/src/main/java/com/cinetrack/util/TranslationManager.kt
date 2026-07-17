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

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.ITALIAN)
        .build()

    private val translator: Translator = Translation.getClient(options)

    /**
     * Checks if the Italian translation model is already downloaded.
     */
    suspend fun isModelDownloaded(): Boolean {
        return try {
            val modelManager = RemoteModelManager.getInstance()
            val model = TranslateRemoteModel.Builder(TranslateLanguage.ITALIAN).build()
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
     * Translates a given text from English to Italian.
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
