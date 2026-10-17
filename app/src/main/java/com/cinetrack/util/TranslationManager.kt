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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale
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
     * Determines the most accurate ISO language code for a newly composed comment:
     * 1. Evaluates [text] on-device using ML Kit language detection (fast ~5ms, zero network).
     * 2. If [text] is sufficiently long (>= 6 chars) and a language is identified with confidence, returns its ISO tag.
     * 3. If [text] is short, undetermined ("und"), or detection fails, falls back gracefully to the device's default locale language.
     */
    suspend fun detectCommentLanguage(text: String): String {
        val clean = text.trim()
        if (clean.length >= 6) {
            try {
                val identifier = LanguageIdentification.getClient(
                    LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(0.45f)
                        .build()
                )
                val lang = identifier.identifyLanguage(clean).await()
                identifier.close()
                if (lang != "und" && !lang.isNullOrBlank()) {
                    val normalized = resolveTargetLanguageCode(lang)
                    if (normalized.isNotBlank()) {
                        return normalized
                    }
                }
            } catch (e: Exception) {
                Log.w("TranslationManager", "detectCommentLanguage failed, using device locale fallback", e)
            }
        }
        return java.util.Locale.getDefault().language.lowercase().takeIf { it.isNotBlank() } ?: "en"
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

    sealed class TranslationResult {
        data class Success(val text: String) : TranslationResult()
        object AlreadyInLanguage : TranslationResult()
        object Error : TranslationResult()
    }

    /**
     * Translates a comment on-demand using Google Translate GTX endpoint with server-side auto-detection.
     * - Iterates over all sentences in JSONArray(0) concatenating fragments so long comments are never truncated.
     * - Uses URLEncoder with StandardCharsets.UTF_8 for emoji, newlines, and special characters.
     * - Checks rootArray index 2 for detected source language (returns AlreadyInLanguage if matching target).
     * - Unescapes HTML entities.
     * - If offline / network error: falls back to local on-device ML Kit if the model was already downloaded.
     */
    suspend fun translateCommentSmart(
        text: String,
        targetLang: String = currentTargetLanguage
    ): TranslationResult = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext TranslationResult.AlreadyInLanguage

        val tgt = resolveTargetLanguageCode(targetLang)

        // 1. Primary: Google Translate GTX public client with sl=auto (instant, full sentences, zero model download)
        try {
            val encodedQuery = java.net.URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$tgt&dt=t&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val rootArray = JSONArray(body)

                    // Detected source language from index 2 if available
                    val detectedSource = rootArray.optString(2)
                    if (detectedSource.isNotBlank() && detectedSource.equals(tgt, ignoreCase = true)) {
                        return@withContext TranslationResult.AlreadyInLanguage
                    }

                    // Iterate over sentences in array[0] to prevent truncating long or multi-line comments
                    val sentences = rootArray.optJSONArray(0)
                    if (sentences != null && sentences.length() > 0) {
                        val fullTranslation = StringBuilder()
                        for (i in 0 until sentences.length()) {
                            val chunk = sentences.optJSONArray(i)?.optString(0)
                            if (!chunk.isNullOrEmpty()) {
                                fullTranslation.append(chunk)
                            }
                        }
                        val rawTranslated = fullTranslation.toString().trim()
                        if (rawTranslated.isNotEmpty()) {
                            val unescaped = android.text.Html.fromHtml(rawTranslated, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                            if (unescaped.equals(clean, ignoreCase = true)) {
                                return@withContext TranslationResult.AlreadyInLanguage
                            }
                            return@withContext TranslationResult.Success(unescaped)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "Google Translate GTX online failed: ${e.message}")
        }

        // 2. Offline Fallback: ML Kit on-device if models are already downloaded
        try {
            val detectedSource = identifyLanguage(clean)
            if (detectedSource != null && (detectedSource.equals(tgt, ignoreCase = true) || detectedSource == targetLang)) {
                return@withContext TranslationResult.AlreadyInLanguage
            }
            val validSource = toSupportedMlKitLanguage(detectedSource)
            val validTarget = toSupportedMlKitLanguage(targetLang)
            if (validSource != null && validTarget != null && isModelDownloaded(validSource, validTarget)) {
                val localResult = translateFrom(clean, validSource, validTarget)
                if (localResult != null && !localResult.trim().equals(clean, ignoreCase = true)) {
                    return@withContext TranslationResult.Success(localResult)
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "Offline ML Kit fallback failed: ${e.message}")
        }

        // 3. Secondary Online Fallback: MyMemory Translate API
        try {
            val encodedQuery = java.net.URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=auto|$tgt"
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
                    if (!translatedText.isNullOrBlank() && !translatedText.startsWith("MYMEMORY WARNING")) {
                        val unescaped = android.text.Html.fromHtml(translatedText, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        if (unescaped.isNotEmpty() && !unescaped.equals(clean, ignoreCase = true)) {
                            return@withContext TranslationResult.Success(unescaped)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "MyMemory fallback failed: ${e.message}")
        }

        TranslationResult.Error
    }

    /**
     * Translates [text] explicitly from [sourceLang] to [targetLang].
     * Falls back to online translation if on-device model translation fails.
     * Strictly preserves all paragraphs, blank lines, and line breaks (\n and \n\n).
     */
    suspend fun translateFrom(text: String, sourceLang: String, targetLang: String = currentTargetLanguage): String? {
        val validSource = toSupportedMlKitLanguage(sourceLang) ?: return translateOnline(text, sourceLang, targetLang)
        val validTarget = toSupportedMlKitLanguage(targetLang) ?: return translateOnline(text, sourceLang, targetLang)
        return translatePreservingNewlines(text) { chunk ->
            try {
                val translator = getOrCreateTranslator(validSource, validTarget)
                translator.translate(chunk).await()
            } catch (e: Exception) {
                Log.w("TranslationManager", "On-device translateFrom failed, trying online fallback", e)
                translateOnlineDirect(chunk, validSource, validTarget)
            }
        }
    }

    /**
     * Translates [text] online via reliable translation endpoints when on-device models
     * cannot be downloaded or fail.
     * Strictly preserves all paragraphs, blank lines, and line breaks (\n and \n\n).
     */
    suspend fun translateOnline(
        text: String,
        sourceLang: String,
        targetLang: String = currentTargetLanguage
    ): String? {
        return translatePreservingNewlines(text) { chunk ->
            translateOnlineDirect(chunk, sourceLang, targetLang)
        }
    }

    private suspend fun translateOnlineDirect(
        text: String,
        sourceLang: String,
        targetLang: String = currentTargetLanguage
    ): String? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext null

        val src = if (sourceLang.isBlank() || sourceLang.equals("auto", ignoreCase = true)) "auto" else mapMlKitToBcp47(sourceLang)
        val tgt = resolveTargetLanguageCode(targetLang)
        if (src == tgt && src != "auto") return@withContext clean

        // 1. Primary: Google Translate GTX public client endpoint (fast, accurate, no api key required)
        try {
            val encodedQuery = java.net.URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
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
                        val fullTranslation = StringBuilder()
                        for (i in 0 until parts.length()) {
                            val chunk = parts.optJSONArray(i)?.optString(0)
                            if (!chunk.isNullOrEmpty()) {
                                fullTranslation.append(chunk)
                            }
                        }
                        val rawTranslated = fullTranslation.toString().trim()
                        if (rawTranslated.isNotEmpty()) {
                            val unescaped = android.text.Html.fromHtml(rawTranslated, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                            return@withContext unescaped
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TranslationManager", "Online Google translate failed: ${e.message}")
        }

        // 2. Secondary fallback: MyMemory Translate API
        try {
            val querySrc = if (src == "auto") "en" else src
            val encodedQuery = java.net.URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=$querySrc|$tgt"
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
     * Preserves paragraphs, blank lines, and line breaks (\n and \n\n) when translating text.
     * Both ML Kit's on-device NMT model and public translation endpoints strip \n or treat them
     * as token separators, turning multi-paragraph reviews into an unreadable wall of text.
     * This helper splits by \n, translates non-blank chunks concurrently, and reconstructs
     * the exact original paragraph and newline structure.
     */
    private suspend fun translatePreservingNewlines(
        text: String,
        translateChunk: suspend (String) -> String?
    ): String? = coroutineScope {
        if (!text.contains('\n')) {
            val clean = text.trim()
            if (clean.isEmpty()) return@coroutineScope text
            return@coroutineScope translateChunk(clean)
        }

        val lines = text.split('\n')
        val deferredLines = lines.map { line ->
            if (line.isBlank()) {
                null
            } else {
                async {
                    translateChunk(line.trim()) ?: line
                }
            }
        }

        val translatedLines = deferredLines.mapIndexed { index, deferred ->
            deferred?.await() ?: lines[index]
        }

        translatedLines.joinToString("\n")
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
        return translatePreservingNewlines(text) { chunk ->
            try {
                legacyTranslator.translate(chunk).await()
            } catch (e: Exception) {
                Log.w("TranslationManager", "ML Kit legacy translate failed, trying online fallback", e)
                translateOnlineDirect(chunk, "en", mapMlKitToBcp47(currentTargetLanguage))
            }
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

    /** Resolves BCP-47 / Google Translate language tag with regional variant support (e.g. zh-CN, zh-TW, pt-BR, pt) */
    fun resolveTargetLanguageCode(langCode: String?, locale: Locale = Locale.getDefault()): String {
        val raw = (if (langCode.isNullOrBlank() || langCode == "system") locale.language else langCode).trim().lowercase()
        if (raw.startsWith("zh")) {
            val script = locale.script.lowercase()
            val country = locale.country.lowercase()
            return if (raw.contains("tw") || raw.contains("hk") || script.contains("hant") || country == "tw" || country == "hk") {
                "zh-TW"
            } else {
                "zh-CN"
            }
        }
        if (raw.startsWith("pt")) {
            val country = locale.country.lowercase()
            return if (raw.contains("br") || country == "br") {
                "pt-BR"
            } else {
                "pt"
            }
        }
        return raw.take(2).ifEmpty { "en" }
    }
}

