# 🏆 FlickTrove — Architettura Database & Piano di Implementazione Badge

Documento tecnico di riferimento per l'integrazione del sistema Trofei & Palmarès Cinefilo in **FlickTrove**.  
Definisce la persistenza locale (Room), la sincronizzazione Cloud (Firestore), l'algoritmo di calcolo della rarità globale e la tabella di fattibilità immediata dei 37 trofei ufficiali.

---

## 1. Architettura Database Locale (Room)

Per garantire la massima velocità (60/120 FPS), funzionamento 100% offline e reattività di Compose via `StateFlow`/`Flow`, lo stato dei trofei viene persistito in SQLite locale tramite due nuove entità Room nel database dell'app.

### 1.1 Entità: `UserBadgeEntity` (Tabella `user_badges`)
Memorizza lo stato di conquista e avanzamento personale dell'utente per ciascun trofeo.

```kotlin
package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "user_badges",
    indices = [
        Index("current_tier"),
        Index("is_unlocked"),
        Index("category")
    ]
)
data class UserBadgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_id")
    val badgeId: String,                           // es. "badge_movies", "secret_groundhog_day"

    @ColumnInfo(name = "current_tier")
    val currentTier: String,                       // "SUPER_8", "MM_16", "MM_35", "MM_70", "THE_FINAL_CUT", "LOST_REEL"

    @ColumnInfo(name = "category")
    val category: String,                          // "PROGRESSIVE_TIERS", "SPECIAL_ACHIEVEMENT", "DAY_ONE_HONOR", "LOST_REEL_SECRET"

    @ColumnInfo(name = "progress_current")
    val progressCurrent: Int = 0,                  // Avanzamento attuale (es. 47)

    @ColumnInfo(name = "progress_target")
    val progressTarget: Int = 1,                   // Soglia per il tier attuale o prossimo (es. 100)

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false,               // true se ha raggiunto almeno il 1° tier

    @ColumnInfo(name = "unlocked_date")
    val unlockedDate: String? = null,              // Formato ISO-8601 o formattato "10 Ago 2026"

    @ColumnInfo(name = "is_revealed")
    val isRevealed: Boolean = false,               // Per i Lost Reel segreti: true quando scoperto

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",             // "pending", "synced"

    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
```

---

### 1.2 Entità: `BadgeGlobalStatsEntity` (Tabella `badge_global_stats`)
Cache locale per le percentuali globali di possesso (la rarità, es. *"Sbloccato dal 4.2% dei cinefili"*). Evita chiamate di rete ripetitive e consente il rendering istantaneo anche offline.

```kotlin
@Serializable
@Entity(tableName = "badge_global_stats")
data class BadgeGlobalStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_id")
    val badgeId: String,

    @ColumnInfo(name = "rarity_percent")
    val rarityPercent: Float,                      // Valore da 0.1 a 100.0 (es. 4.2f)

    @ColumnInfo(name = "unlocked_users_count")
    val unlockedUsersCount: Long = 0L,

    @ColumnInfo(name = "total_users_count")
    val totalUsersCount: Long = 0L,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)
```

---

### 1.3 `BadgeDao` (Operazioni Reattive)
```kotlin
@Dao
interface BadgeDao {
    @Query("SELECT * FROM user_badges")
    fun getAllUserBadgesFlow(): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges WHERE badge_id = :badgeId")
    suspend fun getBadgeById(badgeId: String): UserBadgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadge(badge: UserBadgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadges(badges: List<UserBadgeEntity>)

    @Query("SELECT * FROM badge_global_stats")
    fun getGlobalStatsFlow(): Flow<List<BadgeGlobalStatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalStats(stats: List<BadgeGlobalStatsEntity>)
}
```

---

## 2. Architettura Cloud (Firestore) & Calcolo Rarità Globale

La rarità di un trofeo (es. *"Sbloccato dal 0.3% dei cinefili"*) richiede la conoscenza dell'intera platea di utenti attivi.  
Per rispettare le **Product Rules di FlickTrove** ed evitare il collasso dei costi di billing (Anti-Billing Trap su Firestore):

> ⚠️ **Regola Anti-Billing**:  
> È categoricamente vietato scansionare la collezione di tutti gli utenti (`/users/{uid}/badges`) da parte del singolo client Android. Un'operazione simile costerebbe decine di migliaia di letture a ogni avvio dell'app.

### 2.1 Schema Collezioni Firestore

```
firestore/
  ├── users/
  │    └── {uid}/
  │         └── badges/
  │              └── {badge_id}   --> Documento sync trofeo utente
  │                   ├── currentTier: "THE_FINAL_CUT"
  │                   ├── progressCurrent: 100
  │                   ├── isUnlocked: true
  │                   └── unlockedAt: 1770681600000
  │
  └── system_metrics/
       └── badges_global          --> Singolo documento aggregato condiviso (1 sola read per utente!)
            ├── total_active_users: 14200
            ├── last_calculated_at: 1770685000000
            └── rarities: {
                 "badge_movies": 92.5,
                 "badge_executive_producer": 0.1,
                 "secret_groundhog_day": 0.3,
                 "secret_night_owl": 4.8,
                 ...
            }
```

### 2.2 Come Calcolare le Percentuali Globali (2 Metodi Possibili)

#### Metodo A: Cloud Function Aggregatrice Giornaliera (Raccomandato)
1. Uno script / Cloud Function schedulata (es. ogni notte alle 03:00 UTC) esegue una query aggregata conteggiando gli utenti attivi (`total_active_users`) e il conteggio di documenti sbloccati per ogni `badge_id`.
2. Calcola la formula:
   $$\text{Rarità}_b = \max\left(0.1\%,\, \frac{\text{Utenti con Badge Sbloccato}_b}{\text{Utenti Attivi Totali}} \times 100\right)$$
3. Salva la mappa delle percentuali nel singolo documento `/system_metrics/badges_global`.
4. **Consumo Client**: Il client Android legge **esattamente 1 documento** una volta ogni 24 ore (o al primo sync) e memorizza i dati nella tabella Room `badge_global_stats`.

#### Metodo B: Incremento Atomico On-Unlock (Zero Cloud Functions)
1. Quando un utente sblocca per la prima volta un badge in locale, il sync invia un aggiornamento a Firestore.
2. Usando una transazione o `FieldValue.increment(1)`:
   - Viene incrementato il contatore relativo in `/system_metrics/badges_global`.
3. Il client calcola localmente la percentuale dividendo il contatore per `total_active_users`.

---

## 3. Motore di Valutazione (`BadgeEvaluationEngine`) & Edge-Cases Architetturali

Il calcolo dei badge in locale non deve rallentare la UI. Viene eseguito in background (`Dispatchers.Default`) in risposta ad azioni dell'utente:
- Quando un film/serie viene contrassegnato come **Visto**.
- Quando viene aggiunto/modificato un **Voto Personale**.
- Quando viene scritta una **Nota Personale**.
- Quando viene spuntato un **Episodio**.
- Quando viene completato il **Backup/Sync Cloud**.

### 3.1 Event Bus per Transizioni di Tier & Banner In-App (Stile PlayStation / Steam)
Quando l'utente passa da una soglia all'altra (es. da 24 a 25 film visti), non basta aggiornare il database: l'utente deve vivere il momento celebrativo.
- Il `BadgeEvaluationEngine` espone un `SharedFlow<BadgeTierUnlockedEvent>`:
```kotlin
data class BadgeTierUnlockedEvent(
    val badgeId: String,
    val title: String,
    val tier: PreviewBadgeTier,
    val iconRes: Int,
    val isFirstUnlock: Boolean
)
```
- `MainScreen` ascolta il flusso ed espone un toast/banner glassy in-app animato dall'alto con curva elastica, icona del formato del tier (es. *16mm*, *The Final Cut*), colore del tier e micro-vibrazione:  
  *« 🏆 Trofeo Sbloccato: Frequenza 24fps (16mm) »*

### 3.2 Tabella di Appoggio Zero-Bloat per Attore e Regista (`user_person_frequency`)
Per i badge **Attore Feticcio** (#32) e **Regista del Cuore** (#33), memorizzare l'intero cast nella tabella `favorites` o effettuare join complesse su stringhe JSON ad ogni calcolo creerebbe gravissimo overhead e GC jank.
- **Soluzione Light-weight**: Creazione di una mini-tabella Room di appoggio:
```kotlin
@Entity(tableName = "user_person_frequency", primaryKeys = ["person_id", "role"])
data class UserPersonFrequencyEntity(
    @ColumnInfo(name = "person_id") val personId: Long,
    @ColumnInfo(name = "person_name") val personName: String,
    @ColumnInfo(name = "role") val role: String, // "ACTOR" o "DIRECTOR"
    @ColumnInfo(name = "watch_count") val watchCount: Int = 1,
    @ColumnInfo(name = "last_watched_at") val lastWatchedAt: Long = System.currentTimeMillis()
)
```
- Si aggiorna unicamente in background quando un film passa allo stato `watched = true`. La query per il badge diventa una semplice `SELECT MAX(watch_count) FROM user_person_frequency WHERE role = :role`.

### 3.3 Trigger Elegante per Roulette del Fato (Lost Reel #05)
Per il badge **Roulette del Fato** (#30 - visto un film scoperto scuotendo lo smartphone con *Surprise Me*):
- Quando l'utente apre `MovieDetailScreen` dal modale generatore casuale, si passa nell'argomento di navigazione:  
  `fromSurpriseMe = true`.
- Se l'utente clicca *"Visto"* durante quella sessione, l'evento passa il flag all'engine, che sblocca istantaneamente il trofeo segreto Lost Reel #05 senza alcuna logica complessa di tracciamento background.

---

## 4. Tabella di Fattibilità dei 37 Trofei

Legenda stato di implementazione:
- 🟢 **SUBITO PRONTO**: Calcolabile immediatamente dai dati già residenti nel DB locale Room (`favorites`, `watch_history`, `folders`).
- 🟡 **QUASI PRONTO**: Richiede solo una query SQL mirata (es. su orari o sequenze di `watch_history`).
- 🟠 **DA LAVORARCI**: Richiede un nuovo trigger d'azione UI (es. gesto shake, commento CommsUni federato, toggle contatore).

---

### Sezione 1: I 4 Trofei di Volume & Progresso Globale

| # | ID Badge | Nome Trofeo | Tier Massimo | Stato | Dati Esistenti in DB / Logica di Calcolo |
|---|---|---|---|:---:|---|
| 1 | `badge_movies` | **Frequenza 24fps** | Final Cut (500) | 🟢 **SUBITO** | `favorites.filter { mediaType == "movie" && watched }.size`<br>Soglie: 10, 25, 100, 250, 500 film. |
| 2 | `badge_tv` | **La Grande Abbuffata** | Final Cut (100) | 🟢 **SUBITO** | `favorites.filter { mediaType == "tv" && watched }.size`<br>Soglie: 3, 10, 25, 50, 100 serie TV completate. |
| 3 | `badge_episodes` | **Maratoneta Seriale** | Final Cut (2500) | 🟢 **SUBITO** | Somma di tutti gli episodi registrati in `Movie.watchedEpisodes`: `favorites.sumOf { it.watchedEpisodes?.values?.sumOf { eps -> eps.size } ?: 0 }`. |
| 4 | `badge_watch_time` | **Cronache dalla Sala Buia** | Final Cut (1000h) | 🟢 **SUBITO** | Somma durata: `(film.runtime + episodi * runtimeMedio) / 60`. Tutti i film hanno già `runtime` in `Movie`. |

---

### Sezione 2: I 12 Trofei di Genere Cinematografico

Tutti i film e le serie TV in `favorites` hanno già il campo `genres: List<Genre>` popolato con gli ID standard TMDB. Il calcolo per tutti i 12 badge è **immediato al 100%**:

| # | ID Badge | Nome Trofeo | Genere TMDB | Stato | Dati Esistenti in DB / Logica di Calcolo |
|---|---|---|---|:---:|---|
| 5 | `genre_crime_thriller` | **Indagine a Mezzanotte** | Crime (80) & Mystery (9648) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id in listOf(80, 9648) } }.size` |
| 6 | `genre_scifi` | **Odissea nello Spazio** | Sci-Fi (878) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 878 } }.size` |
| 7 | `genre_horror` | **Notte delle Ombre** | Horror (27) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 27 } }.size` |
| 8 | `genre_animation` | **Il Tratto Animato** | Animation (16) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 16 } }.size` |
| 9 | `genre_drama` | **L'Anima Umana** | Drama (18) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 18 } }.size` |
| 10 | `genre_comedy` | **Il Riso Amaro** | Comedy (35) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 35 } }.size` |
| 11 | `genre_action` | **Scarica di Adrenalina** | Action (28) & Adventure (12) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id in listOf(28, 12) } }.size` |
| 12 | `genre_romance` | **Batticuore** | Romance (10749) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 10749 } }.size` |
| 13 | `genre_documentary` | **Occhio del Reale** | Documentary (99) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 99 } }.size` |
| 14 | `genre_war_history` | **Memorie di Guerra** | War (10752) & History (36) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id in listOf(10752, 36) } }.size` |
| 15 | `genre_fantasy` | **Terre Fantastiche** | Fantasy (14) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 14 } }.size` |
| 16 | `genre_western` | **C'era una Volta il West** | Western (37) | 🟢 **SUBITO** | `favorites.filter { watched && genres.any { it.id == 37 } }.size` |

---

### Sezione 3: I 6 Trofei Curatoriali & Interazione Personale

| # | ID Badge | Nome Trofeo | Tier | Stato | Dati Esistenti in DB / Logica di Calcolo |
|---|---|---|:---:|:---:|---|
| 17 | `badge_personal_rating_master` | **Il Capolavoro Assoluto** | 70mm | 🟢 **SUBITO** | `favorites.count { personalRating != null && personalRating >= 10.0 } >= 10`. |
| 18 | `badge_harsh_critic` | **Pollice Verso** | 35mm | 🟢 **SUBITO** | `favorites.count { personalRating != null && personalRating <= 2.0 } >= 5`. |
| 19 | `badge_rewatch` | **Il Nastro Riavvolto** | 70mm | 🟢 **SUBITO** | Tabella `watch_history`: `watchHistoryDao.getRewatchCount() >= 5` (il campo `isRewatch` esiste già!). |
| 20 | `personal_diary` | **Diario di Bordo** | 35mm | 🟢 **SUBITO** | `favorites.count { !personalNote.isNullOrBlank() } >= 5`. |
| 21 | `badge_folder_master` | **Il Custode dell'Archivio** | 35mm | 🟢 **SUBITO** | Tabella `folders`: `folderDao.getFoldersCount() >= 3` e film assegnati alle cartelle. |
| 22 | `custom_backdrop` | **Scenografo Personale** | Super 8 | 🟢 **SUBITO** | Verifica se l'utente ha impostato un backdrop profilo (`preferences.customBackdropPath != null`). |

---

### Sezione 4: I 5 Trofei Speciali & Onorificenze di Sistema

| # | ID Badge | Nome Trofeo | Tier | Stato | Dati Esistenti in DB / Logica di Calcolo |
|---|---|---|:---:|:---:|---|
| 23 | `badge_day_one_pioneer` | **L'Inaugurazione** | Final Cut | 🟢 **SUBITO** | Verifica se l'utente ha effettuato il primo accesso nei primi mesi di rilascio (`auth.currentUser.metadata.creationTimestamp`). |
| 24 | `badge_executive_producer` | **Il Produttore Esecutivo** | Final Cut | 🟢 **SUBITO** | Flag profilo supporter/donatore (`isSupporter == true` o whitelist UID/email). |
| 25 | `badge_onboarding_sync` | **Il Trasloco** | 16mm | 🟡 **QUASI** | Hook sul completamento della prima sincronizzazione cloud riuscita in `SyncRepository`. |
| 26 | `world_tour` | **Giro del Mondo in 80 Film** | 70mm | 🟡 **QUASI** | `favorites.mapNotNull { it.originCountry }.distinct().size >= 15` (il campo paese d'origine è già nel modello `Movie.productionCountries`). |
| 27 | `badge_commsuni_critic` | **Voce della Critica** | 35mm | 🟠 **DA LAVORARE** | Hook all'invio di un commento sulla rete CommsUni federata (`CommentRepository.sendComment()`). |

---

### Sezione 5: I 10 Trofei Segreti (Lost Reel 🗝️)

| # | ID Badge | Nome Trofeo | Tier | Stato | Dati Esistenti / Lavoro Necessario |
|---|---|---|:---:|:---:|---|
| 28 | `secret_groundhog_day` | **Il Giorno della Marmotta** | Lost Reel | 🟡 **QUASI** | Query SQL su `watch_history`: due record con lo stesso `movieId` con `abs(julianday(w1.watchedAt) - julianday(w2.watchedAt)) <= 2.0`. |
| 29 | `secret_night_owl` | **Cinefilo Notturno** | Lost Reel | 🟡 **QUASI** | Query SQL su `watch_history`: estrazione ora da `watchedAt` compresa tra `02:00` e `05:00`. |
| 30 | `secret_surprise_fate` | **Roulette del Fato** | Lost Reel | 🟠 **DA LAVORARE** | Richiede passare un extra/tag `fromSurpriseMe = true` quando un film aperto da Surprise Me viene contrassegnato come visto. |
| 31 | `secret_genreless_rebel` | **Il Ribelle Senza Genere** | Lost Reel | 🟡 **QUASI** | Algoritmo su ultimi 10 film di `watch_history`: verificare che la sequenza abbia 10 generi primari tutti diversi. |
| 32 | `actor_muse` | **Attore Feticcio** | 35mm | 🟠 **DA LAVORARE** | 5 film visti dello stesso attore: richiede query incrociata con il cast salvato in cache crediti o endpoint TMDB persone. |
| 33 | `director_heart` | **Regista del Cuore** | 70mm | 🟠 **DA LAVORARE** | 5 film visti dello stesso regista: richiede memorizzare l'ID del regista principale nei dettagli film o query su crediti. |
| 34 | `secret_indecisive` | **L'Eterno Dubbioso** | Lost Reel | 🟠 **DA LAVORARE** | Aggiungere un contatore incrementale nelle preferenze per i toggle Aggiungi/Rimuovi dalla Watchlist sullo stesso film. |
| 35 | `secret_completionist` | **La Grande Saga** | 70mm | 🟡 **QUASI** | Query sulle saghe/collection TMDB in `favorites`: tutti i film appartenenti a una `belongsToCollection` risultano visti. |
| 36 | `secret_iron_heart` | **Cuore d'Acciaio** | Lost Reel | 🟡 **QUASI** | 5 film drammatici/strappalacrime visti senza aver assegnato un voto negativo o aver interrotto la visione. |
| 37 | `secret_unloved_gem` | **Gemma Incompresa** | Lost Reel | 🟢 **SUBITO** | `favorites.any { watched && personalRating >= 8.5 && (voteAverage <= 5.8 && voteCount >= 100) }` (Dati già presenti in `Movie`!). |

---

## 5. Riepilogo Sintetico di Fattibilità

- **🟢 SUBITO PRONTI AL CODICE: 24 trofei su 37 (65%)**  
  Tutti i dati necessari (film, serie, episodi, ore, tutti i 12 generi, rewatch, voti 10, stroncature, note personali, cartelle, backdrop, pioniere, mecenate, gemme incomprese) sono **già salvati nel database locale SQLite e sincronizzati su Firestore**.
- **🟡 QUASI PRONTI: 7 trofei su 37 (19%)**  
  Richiedono solo query analitiche SQL su `watch_history` (orari notturni, doppia visione in 48 ore, sequenze generi, saghe complete).
- **🟠 DA LAVORARCI: 6 trofei su 37 (16%)**  
  Richiedono piccoli hook di eventi puntuali (tag da Surprise Me, cast/regista TMDB, commento CommsUni, contatore toggle watchlist).

---

## 6. Prossimi Passi Consigliati

1. **Creazione Entità e DAO Room**: Aggiungere `UserBadgeEntity`, `BadgeGlobalStatsEntity` e `BadgeDao` a `FlickTroveDatabase` con migrazione 25 -> 26.
2. **Creazione `BadgeRepository` & `CalculateBadgesUseCase`**: Un caso d'uso pulito che calcola lo stato dei 24 trofei immediati con una singola scansione in memoria dei film/serie dell'utente.
3. **Collezione Firestore Globale**: Setup della lettura del documento `/system_metrics/badges_global` per le percentuali di rarità a costo zero.
4. **Binding con la UI**: Collegare `TrophyRoomScreenContent` e le card al `StateFlow` del repository invece dei dati campione di anteprima.
