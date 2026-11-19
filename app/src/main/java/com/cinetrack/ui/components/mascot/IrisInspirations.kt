package com.cinetrack.ui.components.mascot

import android.content.Context
import androidx.annotation.StringRes
import com.cinetrack.R

/**
 * Catalogo dei prompt cinematografici d'ispirazione per IRIS (Mood Discovery).
 *
 * Supporta la piena localizzazione tramite risorse `@StringRes`:
 * - Sull'emulatore o telefoni in lingua inglese, carica automaticamente i prompt in inglese da `values/strings.xml`.
 * - Su dispositivi in lingua italiana, carica i prompt in italiano da `values-it/strings.xml`.
 * - Per tutte le altre lingue Android effettua il fallback su `values/strings.xml`.
 */
object IrisInspirations {

    @StringRes
    val CANDIDATE_PROMPT_RES_IDS: List<Int> = listOf(
        R.string.iris_prompt_noir,
        R.string.iris_prompt_scifi,
        R.string.iris_prompt_italian_comedy,
        R.string.iris_prompt_cabin_thriller,
        R.string.iris_prompt_cyberpunk,
        R.string.iris_prompt_spaghetti_western,
        R.string.iris_prompt_whodunit_mansion,
        R.string.iris_prompt_archaeological_adventure
    )

    private var lastSelectedIndex: Int = -1

    /**
     * Restituisce un'ispirazione casuale localizzata, evitando di ripetere la stessa consecutivamente.
     */
    fun getRandomPrompt(context: Context): String {
        if (CANDIDATE_PROMPT_RES_IDS.isEmpty()) return ""
        if (CANDIDATE_PROMPT_RES_IDS.size == 1) return context.getString(CANDIDATE_PROMPT_RES_IDS.first())

        var nextIndex: Int
        do {
            nextIndex = CANDIDATE_PROMPT_RES_IDS.indices.random()
        } while (nextIndex == lastSelectedIndex)
        lastSelectedIndex = nextIndex
        return context.getString(CANDIDATE_PROMPT_RES_IDS[nextIndex])
    }
}
