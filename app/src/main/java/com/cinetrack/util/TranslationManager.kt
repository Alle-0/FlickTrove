package com.cinetrack.util

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles on-device translation using ML Kit, with seamless online fallback
 * when offline models cannot be downloaded or fail.
 *
 * Gotcha 1 (short texts): identifyLanguage() returns "und" for very short/ambiguous texts.
 *   → We fall back to auto-detection online if needed.
 *
 * Gotcha 2 (model downloads): Each source/target language pair requires a ~30MB model download.
 *   → We expose isModelDownloaded() and downloadModels(), and gracefully fallback online if download fails.
 *
 * Gotcha 3 (memory leaks): Translator instances hold native resources.
 *   → We cache them in translatorsCache and expose closeAll() to release all memory on app destroy.
 */
@Singleton
class TranslationManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    constructor() : this(OkHttpClient())

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
            try {
                if (mlkitLang != TranslateLanguage.ENGLISH) {
                    legacyTranslator = buildTranslator(TranslateLanguage.ENGLISH, mlkitLang)
                }
            } catch (e: Exception) {
                // Ignore initialization error
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — comment translation (any → target)
    // ─────────────────────────────────────────────────────────────────────────

    fun getCurrentTargetLanguage(): String = currentTargetLanguage

    /**
     * Normalizes and validates any language tag into a valid ML Kit TranslateLanguage constant.
     * Returns null if the language is unsupported or invalid.
     */
    fun toSupportedMlKitLanguage(langTag: String?): String? {
        if (langTag.isNullOrBlank()) return null
        val clean = langTag.trim().lowercase()
        val all = TranslateLanguage.getAllLanguages()
        if (clean in all) return clean
        val fromTag = TranslateLanguage.fromLanguageTag(clean)
        if (fromTag != null && fromTag in all) return fromTag
        val prefix = clean.take(2)
        if (prefix in all) return prefix
        return null
    }

    /**
     * Identifies the language of [text].
     * Returns null if confidence is low, language is undetermined ("und"),
     * or if the language is not supported by ML Kit Translate.
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
            if (lang == "und") null else toSupportedMlKitLanguage(lang)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the model for the given source→target pair is already on-device.
     */
    suspend fun isModelDownloaded(sourceLang: String, targetLang: String = currentTargetLanguage): Boolean {
        val validSource = toSupportedMlKitLanguage(sourceLang) ?: return false
        val validTarget = toSupportedMlKitLanguage(targetLang) ?: return false
        return try {
            val modelManager = RemoteModelManager.getInstance()
            val sourceModel = TranslateRemoteModel.Builder(validSource).build()
            val targetModel = TranslateRemoteModel.Builder(validTarget).build()
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
        val validSource = toSupportedMlKitLanguage(sourceLang) ?: return false
        val validTarget = toSupportedMlKitLanguage(targetLang) ?: return false
        return try {
            kotlinx.coroutines.withTimeoutOrNull(45_000L) {
                val conditions = DownloadConditions.Builder()
                    .apply { if (requireWifi) requireWifi() }
                    .build()
                try {
                    getOrCreateTranslator(validSource, validTarget).downloadModelIfNeeded(conditions).await()
                    true
                } catch (e: Exception) {
                    Log.w("TranslationManager", "Translator downloadModelIfNeeded failed, trying RemoteModelManager fallback", e)
                    val modelManager = RemoteModelManager.getInstance()
                    val sourceModel = TranslateRemoteModel.Builder(validSource).build()
                    val targetModel = TranslateRemoteModel.Builder(validTarget).build()

                    if (!modelManager.isModelDownloaded(sourceModel).await()) {
                        modelManager.download(sourceModel, conditions).await()
                    }
                    if (!modelManager.isModelDownloaded(targetModel).await()) {
                        modelManager.download(targetModel, conditions).await()
                    }
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("TranslationManager", "ML Kit model download failed for $validSource -> $validTarget", e)
            false
        }
    }

    /**
     * Translates [text] explicitly from [sourceLang] to [targetLang].
     * Falls back to online translation if on-device model translation fails.
     */
    suspend fun translateFrom(text: String, sourceLang: String, targetLang: String = currentTargetLanguage): String? {
        val validSource = toSupportedMlKitLanguage(sourceLang) ?: return translateOnline(text, sourceLang, targetLang)
        val validTarget = toSupportedMlKitLanguage(targetLang) ?: return translateOnline(text, sourceLang, targetLang)
        return try {
            val translator = getOrCreateTranslator(validSource, validTarget)
            translator.translate(text).await()
        } catch (e: Exception) {
            Log.w("TranslationManager", "On-device translateFrom failed, trying online fallback", e)
            translateOnline(text, validSource, validTarget)
        }
    }

    /**
     * Translates [text] online via reliable translation endpoints when on-device models
     * cannot be downloaded or fail.
     */
    suspend fun translateOnline(
        text: String,
        sourceLang: String,
        targetLang: String = currentTargetLanguage
    ): String? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext null

        val src = mapMlKitToBcp47(sourceLang)
        val tgt = mapMlKitToBcp47(targetLang)
        if (src == tgt) return@withContext clean

        // 1. Primary: Google Translate GTX public client endpoint (fast, accurate, no api key required)
        try {
            val encodedQuery = java.net.URLEncoder.encode(clean, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$src&tl=$tgt&dt=t&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val rootArray = JSONArray(body)
                    val parts = rootArray.optJSONArray(0)
                    if (parts != null && parts.length() > 0) {
                        val sb = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val chunk = parts.optJSONArray(i)?.optString(0)
                            if (!chunk.isNullOrEmpty()) {
                                sb.append(chunk)
                            }
                        }
                        val translated = sb.toString().trim()
                        if (translated.isNotEmpty()) {
                            return@withContext translated
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "Online Google translate failed: ${e.message}")
        }

        // 2. Secondary fallback: MyMemory Translate API
        try {
            val encodedQuery = java.net.URLEncoder.encode(clean, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$src|$tgt"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FlickTrove/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val responseData = json.optJSONObject("responseData")
                    val translatedText = responseData?.optString("translatedText")
                    if (!translatedText.isNullOrBlank()) {
                        val unescaped = android.text.Html.fromHtml(translatedText, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        if (unescaped.isNotEmpty() && !unescaped.startsWith("MYMEMORY WARNING")) {
                            return@withContext unescaped
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "Online MyMemory translate failed: ${e.message}")
        }

        null
    }

    /**
     * Translates [text] from any detected language to the user's target language.
     * The caller can call downloadModels() first or rely on seamless online fallback.
     */
    suspend fun translateFromAny(text: String, targetLang: String = currentTargetLanguage): String? {
        val sourceLang = identifyLanguage(text)
        if (sourceLang == null) {
            return translateOnline(text, "auto", targetLang)
        }
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
            Log.w("TranslationManager", "ML Kit legacy translate failed, trying online fallback", e)
            translateOnline(text, "en", mapMlKitToBcp47(currentTargetLanguage))
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
            "hi" -> TranslateLanguage.HINDI
            "tr" -> TranslateLanguage.TURKISH
            "id", "in" -> TranslateLanguage.INDONESIAN
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
            TranslateLanguage.HINDI -> "hi"
            TranslateLanguage.TURKISH -> "tr"
            TranslateLanguage.INDONESIAN -> "id"
            else -> mlkitLang.lowercase().take(2).ifEmpty { "en" }
        }
    }
}
