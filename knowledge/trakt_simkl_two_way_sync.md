# Architettura Two-Way Sync Esterno: Trakt, SIMKL e FlickTrove

Questo documento definisce i principi architetturali, gli invarianti di dominio e le regole operative per la sincronizzazione bidirezionale tra **FlickTrove** e i servizi cloud terzi **Trakt.tv** e **SIMKL**.

---

## 1. Modello Mentale e Asimmetria di Dominio

FlickTrove adotta un'architettura **Offline-First**: il database Room locale sul dispositivo dell'utente costituisce la fonte di verità immediata e primaria. I servizi cloud (Firestore per la sincronizzazione multi-device proprietaria, Trakt e SIMKL per il tracking universale) agiscono come replica federata.

### Mappatura degli Stati

| Stato FlickTrove | Trakt.tv | SIMKL | Note di Sincronizzazione |
| :--- | :--- | :--- | :--- |
| **Watchlist** (`favorite = true`, `!watched`) | `sync/watchlist` | Status `"plantowatch"` | Su Trakt, marcare un titolo come visto lo rimuove *automaticamente* dalla watchlist remota. |
| **Visto / Completato** (`watched = true`) | `sync/history` | Status `"completed"` | Per le serie TV, vale anche l'invariante `isTvUpToDateOrCompleted()`. |
| **In Corso** (Serie TV `progress > 0`, `!watched`) | Progress episodi / `playback` | Status `"watching"` | Episodi spuntati e sincronizzati individualmente. |
| **Interrotto** (`dropped = true`) | `lists/dropped` (o status) | Status `"dropped"` | Gli episodi già visti rimangono preservati al 100%. |

---

## 2. Le 3 Regole d'Oro del Two-Way Merge

### Regola 1: Merge Costruttivo della Watchlist (Anti-Orphan Cleanup Trap)
- **Divieto di Rimozione Silenziosa**: È severamente vietato azzerare (`favorite = false`) i titoli locali presenti nella Watchlist solo perché non compaiono nella lista remota di Trakt o SIMKL.
- **Approccio Additivo**: I titoli presenti in locale con `favorite = true` e non ancora presenti sul server vengono accodati per il caricamento (`addToWatchlist`), preservando la watchlist aggiunta offline o prima del collegamento dell'account.
- **Eliminazione solo con Intento Esplicito**: La rimozione locale di un preferito avviene esclusivamente tramite:
  1. Azione esplicita dell'utente in UI (`ToggleFavoriteUseCase` con tombstone e rimozione esterna accodata).
  2. Riconciliazione da evento di visione (vedi Regola 2).
  3. Tombstone sincronizzato da Firestore (`syncStatus == "deleted"`).

### Regola 2: Mutua Esclusione Watchlist vs Watched (Anti-Reupload Loop)
Trakt applica la regola server-side: **"Un elemento visto non può risiedere nella Watchlist"**.
Se l'utente segna un titolo come visto via web su Trakt o su un'altra app:
1. Al sync successivo, il titolo scompare da `remoteWatchlistIds` e compare in `remoteWatchedIds`.
2. Se il worker controlla la watchlist senza incrociare la cronologia dei visti, interpreterà l'assenza da `remoteWatchlistIds` come una mancanza sul server e tenterà di ricaricare il film visto nella watchlist di Trakt, innescando un ciclo infinito.
3. **Condizione di Guardia Tassativa**:
   ```kotlin
   val isWatchedLocalOrRemote = local.watched ||
       (local.mediaType == "tv" && local.isTvUpToDateOrCompleted()) ||
       remoteWatchedMovieIds.contains(key)

   if (local.favorite && !isWatchedLocalOrRemote && !remoteWatchlistIds.contains(key)) {
       favoritesToPush.add(local)
   }
   ```
4. Se il titolo è arrivato come visto da remoto (`remoteWatchedMovieIds.contains(key)`), lo stato locale deve riconciliarsi a `watched = true` e `favorite = false`.

### Regola 3: De-escalation dei Gesti UI (Pull-to-Refresh Incrementale)
- Il gesto di pull-to-refresh (`HomeViewModel.pullToRefresh()`) è un'azione frequente e compulsiva dell'utente.
- **Divieto di `force = true` al Refresh**: Passare `force = true` costringe il worker a bypassare i controlli dei timestamp e a scatenare il download e il parsing dell'intero catalogo.
- **Delta Sync basato su Timestamp**: Il pull-to-refresh deve sempre passare `"force" to false`. Il worker confronta gli hash `last_activities` (Trakt) o i timestamp di modifica (SIMKL); se nulla è cambiato sul cloud, il sync si conclude in frazioni di secondo a zero letture ridondanti.
- Il parametro `force = true` è riservato esclusivamente a:
  - Primo collegamento dell'account (`isFirstSync = true`).
  - Pressione esplicita del pulsante "Sincronizza adesso" nelle impostazioni di backup/account.

---

## 3. Partizionamento dello Stato in SIMKL

L'endpoint `simklService.getSyncAllItems(...)` di SIMKL restituisce i media suddivisi per categorie e stati (`plantowatch`, `completed`, `watching`, `dropped`).
È obbligatorio partizionare la risposta remota in set disgiunti tipizzati con chiave composita `Pair(tmdbId, mediaType)`:

```kotlin
val remoteWatchedIds = mutableSetOf<Pair<Int, String>>()
val remotePlanToWatchIds = mutableSetOf<Pair<Int, String>>()
val remoteDroppedIds = mutableSetOf<Pair<Int, String>>()

// Estrazione film
simklAll.movies?.forEach { movie ->
    val tmdbId = movie.movie?.ids?.tmdb?.toIntOrNull() ?: return@forEach
    val key = Pair(tmdbId, "movie")
    when (movie.status) {
        "completed" -> remoteWatchedIds.add(key)
        "plantowatch" -> remotePlanToWatchIds.add(key)
        "dropped", "hold" -> remoteDroppedIds.add(key)
    }
}
```

In questo modo:
- I titoli locali `favorite = true` vengono inviati a SIMKL solo se assenti sia da `remotePlanToWatchIds` sia da `remoteWatchedIds`.
- Non si verificano falsi "orfani" e la watchlist rimane blindata al 100%.

---

## 4. Checklist di Verifica Pre-Commit / Pre-Release

Prima di modificare i worker di sincronizzazione (`TraktSyncWorker`, `SimklSyncWorker`, `FirebaseSyncWorker`):
- [ ] Nessuna condizione `else` svuota `favorite = false` o `watched = false` su elementi non trovati in remoto senza tombstone esplicito.
- [ ] Prima di aggiungere a `favoritesToPush`, verificare che l'elemento non sia visto né in locale né in remoto.
- [ ] Il pull-to-refresh nella Home invoca il worker con `"force" to false`.
- [ ] Gli elementi marcati `syncStatus == "pending_delete"` o `"deleted"` in Room vengono ignorati durante l'importazione remota (Scudo Inbound Anti-Zombie).
