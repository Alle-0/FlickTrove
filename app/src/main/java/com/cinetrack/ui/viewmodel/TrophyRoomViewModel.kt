package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.TrophyUnlockBannerEvent
import com.cinetrack.ui.components.badge.OFFICIAL_TROPHY_ROOM_CATALOG
import com.cinetrack.ui.components.badge.TrophyRoomItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrophyRoomViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    /**
     * Flusso reattivo degli elementi trofeo alimentato da Room con fallback al catalogo.
     */
    val trophyItems: StateFlow<List<TrophyRoomItemUi>> = badgeRepository
        .getTrophyRoomItemsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OFFICIAL_TROPHY_ROOM_CATALOG
        )

    /**
     * Eventi di sblocco in tempo reale per banner celebrativi PlayStation/Steam.
     */
    val unlockedTierEvents: SharedFlow<TrophyUnlockBannerEvent> = badgeRepository.unlockedTierEvents

    init {
        refreshBadges()
    }

    /**
     * Avvia la valutazione asincrona dell'archivio cinefilo e sincronizza la rarità globale.
     */
    fun refreshBadges() {
        viewModelScope.launch {
            badgeRepository.evaluateAllBadges()
            badgeRepository.syncPendingBadgesToFirestore()
            badgeRepository.fetchGlobalRarityStats()
        }
    }

    /**
     * Emette un evento di test per verificare visivamente l'animazione e lo stile del banner celebrativo.
     */
    fun triggerTestUnlockBanner(
        badgeId: String = "badge_movies",
        tier: com.cinetrack.ui.components.badge.PreviewBadgeTier = com.cinetrack.ui.components.badge.PreviewBadgeTier.MM_70
    ) {
        badgeRepository.triggerTestUnlockBanner(badgeId, tier)
    }

    suspend fun verifySupporterEmail(email: String): BadgeRepository.SupporterVerificationResult {
        return badgeRepository.verifySupporterByEmail(email)
    }

    suspend fun getUnlockingMoviesForBadge(badgeId: String): List<com.cinetrack.data.model.Movie> {
        return badgeRepository.findUnlockingMoviesForBadge(badgeId)
    }

    suspend fun getUnlockingMovieForBadge(badgeId: String): com.cinetrack.data.model.Movie? {
        return badgeRepository.findUnlockingMovieForBadge(badgeId)
    }
}
