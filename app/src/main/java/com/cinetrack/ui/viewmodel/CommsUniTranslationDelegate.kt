package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.UiText
import com.cinetrack.util.TranslationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Delegate responsible for on-demand Smart Translation (ML Kit / Google Translate)
 * of comments in CommsUni.
 */
class CommsUniTranslationDelegate @Inject constructor(
    private val translationManager: TranslationManager,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager
) {
    private val _translationStates = MutableStateFlow<Map<String, CommentsViewModel.TranslationState>>(emptyMap())
    val translationStates: StateFlow<Map<String, CommentsViewModel.TranslationState>> = _translationStates.asStateFlow()

    private val _showTranslationPrompt = MutableStateFlow<Pair<String, String>?>(null)
    val showTranslationPrompt: StateFlow<Pair<String, String>?> = _showTranslationPrompt.asStateFlow()

    fun dismissTranslationPrompt() {
        _showTranslationPrompt.value = null
    }

    fun translateComment(commentId: String, text: String, requireWifi: Boolean? = null, scope: CoroutineScope) {
        val current = _translationStates.value[commentId]
        // Toggle: if already translated, reset to untranslated (original text)
        if (current is CommentsViewModel.TranslationState.Translated) {
            _translationStates.value = _translationStates.value - commentId
            return
        }
        scope.launch {
            val mediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
            val cleanText = text.replace(mediaRegex, "").trim()
            if (cleanText.isBlank()) {
                return@launch
            }

            // Sync user's target language from preferences
            val prefs = preferenceRepository.userPreferencesFlow.first()
            val systemLang = Locale.getDefault().language
            translationManager.setTargetLanguage(prefs.contentLanguage, systemLang)

            val targetMlKit = translationManager.getCurrentTargetLanguage()

            _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Translating)

            when (val result = translationManager.translateCommentSmart(cleanText, targetMlKit)) {
                is TranslationManager.TranslationResult.Success -> {
                    _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Translated(result.text))
                }
                is TranslationManager.TranslationResult.AlreadyInLanguage -> {
                    _translationStates.value = _translationStates.value - commentId
                    actionFeedbackManager.emit(UiText.StringResource(R.string.comment_already_in_language))
                }
                is TranslationManager.TranslationResult.Error -> {
                    _translationStates.value = _translationStates.value + (commentId to CommentsViewModel.TranslationState.Error)
                    actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_translating))
                }
            }
        }
    }

    fun translateAllComments() {
        // Reserved for bulk translation
    }

    fun close() {
        translationManager.closeAll()
    }
}
