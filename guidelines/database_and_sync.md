# FlickTrove Guidelines: Database, Room & Cloud Sync

## Regole Chiave da Anti-Feature Trap

- **Auto-Conciliazione Idempotente delle Statistiche Globali dei Media (`autoReconcileGlobalRatingIfMissing`)**:
   - **Divieto Scritture Massive / Flag di Import**: È tassativamente vietato introdurre scritture batch massive o flag binarie di stato (es. `initial_ratings_synced`) per sincronizzare in blocco i voti importati (da file di backup, Trakt, SIMKL, CSV o YAML) verso collezioni aggregate anonime come `/global_movie_stats`.
   - **Protezione Quota e Idempotenza**: L'allineamento dei voti mancanti deve avvenire esclusivamente **on-demand (lazy)** alla consultazione della scheda di dettaglio media (`autoReconcileGlobalRatingIfMissing`) e **solo se** `ratingCount == 0L`. Questo garantisce zero operazioni di rete all'importazione, zero rischi di saturazione quote Firebase e l'impossibilità matematica di conteggiare doppiamente un voto.
   - **Consistenza Visiva Ottimistica Immediata**: In UI (`DetailPersonalZone`), se il media non ha ancora voti globali sul server (`ratingCount == 0L`) ma l'utente possiede un rating personale locale $>0$, il layout adotta istantaneamente `previewRating` mostrando subito la media (es. "FLICKTROVE 4.0") con zero latenza visiva.

- **Ordinamento Cronologico e Uscita al Cinema nelle Saghe (`Movie.effectiveReleaseDate`)**:
   - Nelle saghe e collezioni (`CollectionDetailViewModel`, `EditorialCollectionViewModel`), le opere multimediali comprendono sia film che serie TV (es. Universo Marvel MCU, Star Wars).
   - TMDB assegna `release_date` ai film e `first_air_date` alle serie TV: l'accesso diretto al solo `movie.releaseDate` restituisce `null` per le serie TV, causando la segregazione forzata (tutti i film raggruppati prima e tutte le serie TV spinte in fondo al fallback `"9999-99-99"` o `""`).
   - Utilizzare tassativamente `movie.effectiveReleaseDate` (ovvero `(movie.releaseDate ?: movie.firstAirDate)?.takeIf { it.isNotBlank() }`) in combinazione con `it.second` (`timelineIndex`) come tie-breaker:
     - Ascendente: `compareBy { it.first.effectiveReleaseDate ?: "9999-99-99" }.thenBy { it.second }`
     - Discendente: `compareByDescending { it.first.effectiveReleaseDate ?: "" }.thenByDescending { it.second }`
   - Questo garantisce che film e serie TV si intercalino in perfetto ordine cronologico naturale di debutto nelle sale e in streaming.
   - Per la corrispondenza con i preferiti/visti (`favorites`), non filtrare unicamente per `it.id == part.id`, ma verificare sempre la compatibilità del tipo di media (`it.mediaType == part.mediaType || it.mediaType.isBlank() || part.mediaType.isBlank()`) per prevenire collisioni tra film e serie TV aventi lo stesso ID numerico grezzo.

- **Diario di Visione & Watch History Multi-Media Timeline (`HistoryScreen` & `WatchHistory`)**:
    - **Zero-API Invariant (100% Offline-First)**: La schermata Diario / History non effettua **MAI** chiamate di rete a TMDB, Firestore o Trakt. I dati sono alimentati unicamente da una join Room SQLite locale (`watch_history` JOIN `favorites`), e i poster sono renderizzati dalla cache su disco Coil.
    - **Protezione Collisioni Multi-Media (Room Migration 26 ➔ 27)**: Poiché TMDB assegna gli stessi ID numerici a film e serie TV distinti, `watch_history` adotta la colonna `mediaType TEXT NOT NULL DEFAULT 'movie'` e l'indice composito univoco `index_watch_history_movieId_mediaType_watchedAt` su `(movieId, mediaType, watchedAt)`.
    - **Simmetria Cloud Firestore & ID Documenti Collision-Proof**: Nel delta sync di Firestore (`fetchWatchHistoryDelta`, `batchSetWatchHistory`, `softDeleteWatchHistory`), l'ID documento adotta la sintassi retro-compatibile:
      `if (mediaType == "tv") "tv_${movieId}_${safeWatchedAt}" else "${movieId}_${safeWatchedAt}"`
      preservando al 100% l'integrità dei record storici dei film senza collisioni coi metadati delle serie TV.
    - **Algoritmo Anti-Tenet di Conteggio Rewatch**: Le query Room restituiscono le sessioni ordinate per data decrescente (`watchedAt DESC`). Il conteggio del progressivo ordinale di visione (`Rewatch #1`, `Rewatch #2`...) deve essere eseguito tassativamente invertendo l'ordine e iterando dal record più antico a quello più recente (`rawRecords.indices.reversed()`) raggruppando per chiave composita `"${mediaType}_${movieId}"`.
    - **Schermatura Orari Predefiniti (Anti-00:00)**: I record importati da backup, CSV o Trakt privi di orario esplicito riportano `00:00:00` o `00:00`. Il parser deve contrassegnare `hasExplicitTime = false` quando ore, minuti e secondi sono 0, evitando di mostrare l'antiestetico orario `00:00` nella UI.
    - **Lookup O(1) nella Griglia Calendario**: Nella vista Calendario (`HistoryCalendarView`), la mappa `calendarEntriesByDate: Map<LocalDate, List<WatchTimelineEntry>>` è precalcolata a monte nel `HistoryViewModel`. È severamente vietato eseguire scansioni lineari (`filter { ... }`) dentro le 42 celle della griglia mensile durante la ricomposizione Compose.
    - **Citazione Editoriale Note Personali**: Quando una sessione possiede una nota personale associata sul media nei preferiti (`favorites.personalNote`), la card nella timeline espone un box editoriale corsivo con virgolette e sfondo traslucido dedicato.
    - **Internazionalizzazione Paritaria su 15 Lingue**: Tutti i 24 identificatori stringa del Diario (`history_*`) sono rigorosamente tradotti e sincronizzati su tutte le 15 cartelle risorse `values*`.
    - **Avanzamento Cronologico Episodi TV (`ensureFirstViewingCreated` & `UpdateEpisodesUseCase`)**:
      - Quando l'utente spunta o aggiunge nuovi episodi o stagioni avanzando nella serie (`isProgressAdvancing == true`), l'evento deve memorizzare la data odierna (`LocalDate.now()`). Se la serie non aveva ancora sessioni storiche e viene creata retroattivamente la prima visione, essa adotta la data storica di aggiunta solo se non si sta avanzando attivamente con nuovi episodi oggi.
      - Al completamento al 100% di una serie TV, `watchedAt` deve registrare il timestamp istantaneo attuale (`Instant.now().toString()`), preservando la fedeltà del momento di completamento.
    - **Simmetria Cromatica Pillole di Sessione (Serie vs Film & Rewatch)**:
      - Le capsule di sessione cronologica mantengono l'identità del tipo di media ancorata al tema attivo: Serie TV = `MaterialTheme.colorScheme.primary`, Film = `MaterialTheme.colorScheme.secondary`.
      - Il rewatch mantiene il colore proprio del media (Serie o Film) e si identifica tramite l'icona vettoriale circolare `ic_ricarica` (`🔁`) e l'etichetta (*"Rewatch"* / *"Rewatch #X"*), senza mai forzare tinte arancioni o dorate statiche che rompano l'armonia cromatica dell'interfaccia.
    - **Popup Contestuali Glassmorphic (`hazeGlass`)**:
      - I menu contestuali a comparsa per la gestione delle sessioni (modifica data / eliminazione) devono adottare `hazeGlass` con curvatura concentrica (`20.dp`), container scuro traslucido (`0xFF14171E` con alpha 0.85f), icone inscritte in cerchi con sfondi dedicati e propagazione di `hazeState` da Timeline, Calendario e BottomSheet. È tassativamente vietato usare `DropdownMenu` Material con sfondi grigi piatti o sagome spigolose.
    - **Clearance Top Bar Senza Cutout Ridondante**:
      - Il padding di testa per liste o schermate con `GlassyTopBar` galleggiante deve calcolare unicamente `statusBarTop + 46.dp + 8.dp`, evitando di sommare ridondantemente `displayCutout` (che sulle versioni moderne di Android è già assorbito nella status bar), scongiurando voragini asimmetriche di spazio vuoto sotto la barra.
    - **Riconciliazione Film su Cancellazione/Modifica Watch Session (Single Source of Truth)**:
      - Ogni qualvolta si elimina o si modifica una sessione in `WatchHistoryRepository` (sia da `HistoryViewModel` che da `MovieDetailViewModel`):
        1. Se non rimangono ulteriori sessioni di visione per quel film/serie: riconciliare immediatamente l'entità Room con `watched = false` e `watchedAt = null`.
        2. Se rimangono altre sessioni (rewatch): aggiornare `watchedAt` al timestamp della sessione più recente rimasta.

### Architettura di Sincronizzazione Cloud (Trakt & Servizi Esterni)

- **Chiavi Timestamp Composite per Entità Multiple (Anti-Elvis Trap)**:
   - Quando le API di un servizio esterno (es. Trakt `last_activities`) forniscono timestamp separati per diverse tipologie di media (es. `movies.watchlisted_at` e `shows.watchlisted_at`), è **severamente vietato** aggregarli con l'operatore Elvis (`movies ?: shows`). Tale pattern rende invisibili gli aggiornamenti della seconda entità ogniqualvolta la prima è valorizzata.
   - Generare sempre una chiave composita serializzata (es. `"${moviesTimestamp}_${showsTimestamp}"`) per tracciare contemporaneamente e in modo indipendente l'invalidazione della cache di tutte le categorie.
- **Propagazione e Rispetto del Flag `force` nei Worker di Sync**:
   - Quando l'utente preme "Sincronizza adesso" (`force = true`) o al primo collegamento (`isFirstSync = true`), **tutte** le fasi interne del worker devono bypassare i controlli di uguaglianza dei timestamp (`if (force || isFirstSync || remoteKey != localKey)`), evitando che singole fasi vengano saltate silenziosamente a causa di timestamp invariati.
- **Invariante Single Source of Truth negli Stati Mutuamente Esclusivi (Watchlist vs Watched)**:
   - In FlickTrove, uno stesso titolo non può coesistere contemporaneamente come "Da Vedere" (`favorite = true, !watched`) e "Visto" (`watched = true`).
   - Nella sincronizzazione da Watchlist remota: forzare sempre `favorite = true, watched = false` su ogni item scaricato, per impedire che vecchi stati o flag `watched = true` locali nascondano il titolo dalla schermata "Da Vedere".
   - Nella sincronizzazione della cronologia visti remota: forzare sempre `watched = true, favorite = false` per rimuovere coerentemente il titolo dalla watchlist locale.
- **Risoluzione Polimorfica degli ID Esterni (Fallback TMDb via IMDb)**:
   - Quando il payload remoto (es. item di watchlist o history Trakt) manca dell'identificatore nativo `ids.tmdb` ma contiene `ids.imdb`, non scartare l'elemento.
   - Eseguire sempre il lookup di recupero tramite `tmdbService.findByExternalId(imdbId, "imdb_id")` prima di ignorare l'elemento, validando che l'ID risolto sia plausibile ($> 100\text{L}$).
- **Semantica e Coerenza delle Metriche nelle Statistiche**:
   - I label delle metriche devono rispecchiare con precisione la popolazione censita. Non usare termini restrittivi o fuorvianti (es. "COMPLETATE") per aggregazioni che contano qualsiasi attività o progresso (es. serie con almeno un episodio visto).
   - Mantenere la simmetria tra categorie omologhe (Film: `DA VEDERE | VISTI` $\leftrightarrow$ Serie TV: `DA VEDERE | VISTE`).

### Backup, Import & Export Invariants (Cinemaniac Standard)

- **Riconoscimento e Rilevamento Intestazione**:
   - I backup di Cinemaniac possono presentarsi come file `.bak`, `.json` o archivi `.zip`. Il rilevamento (`isCinemaniacJson` / `isCinemaniacZip`) deve sempre verificare la presenza di `"app":"Cinemaniac"` o di campi univoci come `backupVersion`, accoppiati a `id_movie`/`seen` o `id_tv`. Non affidarsi mai esclusivamente all'estensione `.bak`.
- **Semantica di Stato Watchlist vs Visti (`seen`)**:
   - In Cinemaniac, `seen = 0` rappresenta semanticamente i titoli *"Da Vedere"* dell'utente: devono essere tassativamente importati come Watchlist (`watched = false, favorite = true`).
   - I titoli con `seen = 1` devono essere importati come Visti (`watched = true, favorite = false`), convertendo il timestamp epoch in millisecondi (`date`) in una data ISO-8601 UTC.
- **Decodifica Matrice 2D Stagioni/Episodi (`seasons`)**:
   - Nelle serie TV di Cinemaniac, la proprietà `seasons` è una matrice JSON bidimensionale (stringa o array) `[[1,1,1...],[1,1,0...]]` dove l'indice esterno $+ 1$ corrisponde al numero di stagione e l'indice interno $+ 1$ al numero di episodio (`1` = visto, `0` = non visto).
   - Questa struttura deve essere mappata rigorosamente in `watchedEpisodes: Map<String, List<Int>>` (es. `"1" -> [1, 2, 3...]`).
   - Calcolare e valorizzare sempre `numberOfEpisodes`, `numberOfSeasons` e `progress` (rapporto episodi visti / totali) sia in fase di import che nei punti di merge in `BackupRepository`.
- **Convenzione ID nei Voti Personali (`ratings`)**:
   - La lista `ratings` adotta ID con segno: ID **positivi** corrispondono ai Film, ID **negativi** alle Serie TV (`-id_tv`). L'importer deve separare le mappe e associare il `personalRating` all'entità corretta.

### Firestore Delta Sync & Tombstone Invariants (Zero-Waste Architecture)

- **Delta Sync Basato su `client_updated_at` (Anti-Polling a Strascico)**:
   - **Divieto Assoluto di Fetch a Tappeto**: È tassativamente vietato scaricare l'intera collezione `users/{uid}/favorite_movies` ad ogni sincronizzazione dell'app.
   - **Query Delta Incrementale**: `fetchFavoritesDelta(sinceTimestamp)` deve utilizzare la query incrementale `.whereGreaterThan("client_updated_at", sinceTimestamp)` su `Source.SERVER`. Se nulla è cambiato sul cloud, Firestore restituisce 0 documenti al costo di 1 singola lettura base.
   - **Fallback Primo Avvio**: Solo se `sinceTimestamp <= 0L` (primo avvio o nuovo dispositivo) è consentito il fetch completo a chunk paginati (`pageSize = 35L`) per prevenire OOM su librerie enormi.

- **Pattern Tombstone / Soft Delete Permanente a Costo Zero (`deleteMovie`)**:
   - **Divieto di `.delete()` Fisico Diretto**: È tassativamente vietato invocare `document.delete()` sui media in Firestore. Le cancellazioni fisiche immediate rompono il delta sync dei dispositivi secondari (tablet, web, multi-device), lasciando record fantasma orfani in Room per sempre.
   - **Lapide / Tombstone Atomico (100% Spark Free Tier)**: Ogni rimozione scrive un documento tombstone con `id`, `mediaType`, `sync_status = "deleted"`, `client_updated_at = now`, e `updatedAt = FieldValue.serverTimestamp()` tramite `SetOptions.merge()`. Nessun campo `expireAt` né dipendenza da policy TTL di Google Cloud (che richiedono piani a pagamento con carta di credito).
   - **Zero Letture Sprecate & Zero Costi Storage**:
     - Grazie al filtro `.whereGreaterThan("client_updated_at", sinceTimestamp)`, una volta che un dispositivo scarica il tombstone e aggiorna il proprio `lastSyncTimestamp`, quel documento non verrà mai più scaricato. Le letture future sono pari a 0.
     - Un documento tombstone pesa ~120 byte: 1 GB gratuito di Firebase Spark può ospitare oltre 8 milioni di tombstone prima di avvicinarsi ai limiti di archiviazione.
   - **Protezione Anti-Zombie NoSQL Distribuita**: Lasciare i tombstone persistenti a tempo indeterminato garantisce che un secondo dispositivo (es. un tablet riacceso dopo mesi) non re-inietti i film eliminati nel cloud credendoli nuovi.
   - **Riconciliazione in Room (Scudo Tombstone Locale)**: Al delta fetch (e al completamento di `pushPendingChanges`), il repository **non deve mai** cancellare fisicamente la riga da SQLite con `deleteById`. Preserva la lapide inserendola con `syncStatus = "deleted"`, azzerando esplicitamente `watched = false`, `favorite = false`, `reminder = false`, `dropped = false`, `personalRating = null`, `personalNote = null` e `watchedEpisodes = emptyMap()`.
   - **Isolamento UI & Persistenza Scudo**: Tutte le query visibili all'utente in `FavoriteDao` escludono le lapidi con `WHERE sync_status NOT IN ('pending_delete', 'deleted')`, mentre `getMovieIncludingDeleted()` interroga l'intera tabella trovando il tombstone ed evitando che Trakt o SIMKL risveglino film eliminati.
   - **Risoluzione Bug del Viaggiatore nel Tempo (`pushPendingChanges`)**: Quando il dispositivo torna online e carica le modifiche offline su Firestore, sovrascrive tassativamente `clientUpdatedAt = System.currentTimeMillis()`. In questo modo il timestamp sul cloud corrisponde all'istante reale dell'upload, garantendo che gli altri dispositivi scarichino regolarmente il delta anche se il timestamp del tap locale offline era antecedente all'ultimo sync del tablet.
   - **Opzionale Pulizia Client-Side**: Se in futuro si desidera ripulire vecchi tombstone, l'operazione può essere delegata on-demand al client (es. dentro "Ottimizza Cache" o task mensile con `whereEqualTo("sync_status", "deleted")` e data anteriore a 60-90 giorni), senza spendere per funzioni cloud a pagamento.

- **Debounce Anti-Concorrenza Cooldown (15 Secondi)**:
   - Per prevenire race condition o doppie chiamate di rete parallele quando più ViewModel si inizializzano contemporaneamente (es. `HomeViewModel` e `FavoritesViewModel`), applicare un debounce di 15 secondi (`15_000L`). Questo garantisce sincronizzazione immediata ad ogni ripresa dell'app da background, bloccando unicamente query concorrenti ravvicinate a meno di 15s.

- **Sincronizzazione Esterna (Trakt & SIMKL) e Scudo Anti-Resurrezione Inbound**:
   - **Outbound Atomico al Delete**: Ogni rimozione di un media da FlickTrove lancia parallelamente sia il tombstone su Firestore (`softDeleteFavorite`) sia le richieste di rimozione accodate su WorkManager verso Trakt (`TraktInstantWriteWorker`: remove history, watchlist, rating) e SIMKL (`SimklInstantWriteWorker`), garantendo disallineamento zero verso i cloud esterni.
   - **Scudo Inbound Anti-Zombie (`pending_delete` / `deleted`)**: Durante l'importazione o la sincronizzazione periodica da Trakt o SIMKL (`TraktSyncWorker`, `SimklSyncWorker`), è tassativo interrogare Room tramite `movieRepository.getMovieIncludingDeleted(id, mediaType)`. Se il record locale esiste ed è marcato con `syncStatus == "pending_delete"` o `syncStatus == "deleted"`, il titolo **deve essere rigorosamente ignorato** (`skip resurrezione`). La decisione di eliminazione presa dall'utente su FlickTrove ha sempre priorità assoluta rispetto alle risposte delle API esterne.
   - **Rimozione Diretta dal Browser su Trakt / SIMKL**: Se l'utente elimina un media direttamente dal sito web di Trakt o SIMKL, la fase di Two-Way Merge rileva l'assenza del titolo nella lista remota rispetto a Room e propaga automaticamente la rimozione sia localmente sia via tombstone su Firestore per mantenere allineati tutti i dispositivi secondari.

- **Watch History Delta Sync & Tombstones (`fetchWatchHistoryDelta`)**:
   - **Divieto di Full Scan su `watch_history`**: È tassativamente vietato eseguire `fetchAllWatchHistory()` a ogni invocazione di `syncWithFirebase()` (cooldown 15s). Su cronologie profonde con centinaia di sessioni, questo causava consumi catastrofici delle quote giornaliere di lettura.
   - **Delta Query Incrementale**: `fetchWatchHistoryDelta(sinceTimestamp)` applica `.whereGreaterThan("client_updated_at", sinceTimestamp)` sul cloud Firestore. Se la cronologia non ha subito modifiche, il costo è di 1 singola lettura base.
   - **Tombstone Permanente su Rewatch Eliminati (`softDeleteWatchHistory`)**: La cancellazione di una sessione di visione scrive un tombstone con `sync_status = "deleted"` e `client_updated_at = now` tramite `SetOptions.merge()`, preservando l'allineamento multi-device senza cancellazioni fisiche dirette.
   - **Riconciliazione Locale in Room**: Al delta pull, se `remoteEntry.syncStatus == "deleted"`, il record locale viene rimosso fisicamente solo per quella specifica sessione (`movieId` + `watchedAt`), eliminando il confronto distruttivo con l'intero storico cloud.

- **Throttle Locale su Sincronizzazione Cartelle (`FOLDER_SYNC_THROTTLE_MS = 20 minuti`)**:
   - Nel ciclo di `syncWithFirebase()`, il recupero delle cartelle cloud (`folderRepository.syncFolders()`) deve essere protetto da un throttle locale in memoria di almeno 20 minuti (`FOLDER_SYNC_THROTTLE_MS = 20 * 60 * 1000L`).
   - Evita di lanciare `.get(Source.SERVER)` a tappeto sulla collezione cartelle a ogni ripresa dell'app ogni 15 secondi, proteggendo la quota di lettura su entità a bassa frequenza di modifica.

- **Cache Corazzata `Mutex` su Collezioni Editoriali (`EditorialCollectionRepository`)**:
   - **Divieto di Query Firestore per Keystroke**: È severamente vietato invocare `firestore.collection("editorial_collections").get()` direttamente ad ogni carattere digitato nella barra di ricerca (`SearchViewModel`).
   - **Double-Checked Locking con Mutex & TTL 24 Ore**: `getAllCollections(forceRefresh: Boolean)` adotta un `Mutex` di coroutine con controllo a doppio stadio e una cache in RAM (`cachedCollections`) con TTL di 24 ore (`24 * 60 * 60 * 1000L`).
   - Tutte le operazioni di filtraggio testuale avvengono istantaneamente in RAM a costo 0 chiamate di rete e zero allocazioni ridondanti.

- **Hardening Top 10 FlickTrove e Global Movie Stats (Zero-Waste Real-Time)**:
   - **Clamping Query Fallback Settimanali (`fetchTop10MonthlyBoth`)**: La query di ripiego per le 4 settimane include tassativamente `.orderBy("view_count", Query.Direction.DESCENDING).limit(50)` per impedire scansioni complete dell'intera collezione globale aggregata garantendo un pool abbondante sia per i film sia per le serie TV.
   - **Cache in RAM 12 Ore per la Top 10**: `getTop10FlickTroveBoth()` memoizza la classifica in RAM protetta da `Mutex` con TTL di 12 ore, azzerando le riletture durante la navigazione nei caroselli della Home.
   - **Global Movie Stats On-Demand con `LruCache` (Anti-WebSocket)**:
     - Sostituire tassativamente `addSnapshotListener` con una lettura puntuale on-demand `.get()` su `global_movie_stats/{movieId}`. I contatori della community non necessitano di stream socket aperti perennemente.
     - Proteggere le letture con una `LruCache<String, Pair<GlobalMovieStats, Long>>(maxSize = 100)` con TTL di 30 minuti.
     - Invalidare puntualmente la cache locale (`globalStatsLruCache.remove(movieId)`) non appena l'utente invia o modifica il proprio rating o commento locale.

- **Protezione Username e Termini Riservati di Sistema (Anti-Impersonation & Brand Shield)**:
   - **Divieto Assoluto di Termini di Brand e Ruoli di Sistema**: È tassativamente vietato consentire agli utenti di registrare o scegliere nomi utente che contengano termini del brand (`flicktrove`, `cinetrack`, `commsuni`) o che assumano ruoli di sistema (`admin`, `administrator`, `moderator`, `support`, `staff`, `official`, `system`).
   - **Resistenza al Leetspeak & Normalizzazione Simboli**: La verifica (`containsReservedWords`) normalizza preventivamente il testo convertendo leetspeak (`1`/`!` -> `i`, `0` -> `o`, `3` -> `e`, `4`/`@` -> `a`, `5`/`$` -> `s`, `7`/`+` -> `t`, `8` -> `b`, `2` -> `z`) ed eliminando separatori speciali prima del controllo (`fl1cktr0ve`, `4dm1n`, ecc.).
   - **Damping Substring & Prevenzione Falsi Positivi**: I termini di brand (`flicktrove`) sono respinti su qualsiasi substring (`contains`). I ruoli di sistema sono respinti se corrispondono alla stringa intera, come prefisso, suffisso o delimitati da numeri/separatori (`_`, `.`, `-`), evitando falsi positivi su parole naturali non correlate.
   - **Validazione a Monte (Zero-Network Overhead)**: Sia in fase di cambio nome (`AccountModals.kt`) sia in fase di verifica disponibilità (`AuthViewModel.isUsernameAvailable`), la violazione dei termini riservati interrompe immediatamente l'elaborazione restituendo `false` e mostrando la stringa d'errore localizzata (`account_error_name_reserved` presente in tutte le 15 lingue), senza effettuare alcuna chiamata di rete a Firestore.
   - **Sanitizzazione Automatica al Signup**: In fase di registrazione (`generateUniqueUsername`), se l'email dell'utente o il nome Google contiene termini riservati o offensivi, il generatore converte automaticamente il prefisso in un neutro `User{random}`.

- **Architettura Diari di Visione, Glass Top Bar Isolation & Note Bounded Editing**:
    - **Isolamento Top Bar da Haze (Anti-Feedback Blur)**:
      - Nei layout con Top Bar galleggiante a vetro (`hazeGlass`), la Top Bar deve risiedere tassativamente **all'esterno** del container con `.haze(state)` come sibling in sovrimpressione con elevata `zIndex` (es. `20f`), separando lo strato di vetro (`Spacer.matchParentSize().hazeGlass(...)`) dal layer interattivo (`Row`).
      - È vietato annidare la Top Bar all'interno del modificatore `.haze()` o applicare `.hazeGlass()` direttamente sul container dei pulsanti e dei testi, per evitare sia testi sfocati sia feedback loop di campionamento che generano macchie nere/sfocate.
      - Utilizzare un `HazeState` dedicato e isolato (`internalHazeState = remember { HazeState() }`) e passare `contentPadding = PaddingValues(top = topBarClearance + 8.dp)` alla lista per permettere ai contenuti di scorrere liberamente *sotto* la barra curva a vetro anziché venire tagliati da padding rigidi su Box.
    - **Header di Raggruppamento Temporale (No Sticky Header su Glass Top Bar & No Gradienti Opaque)**:
      - Nelle viste a lista temporale (es. Timeline del Diario di Visione), i titoli di sezione (es. Mese/Anno) non devono essere implementati come `stickyHeader`, poiché rimangono incastrati sotto la barra di vetro e sotto il camera cutout. Utilizzare normali elementi `item` e non applicare gradienti verticali opachi (`Brush.verticalGradient`), lasciando lo sfondo trasparente per permettere alle sezioni di scorrere e sparire naturalmente sotto la sfocatura.
    - **Dialog Nota Personale (Bounded Height, 5.000 Chars Limit & Live Markdown)**:
      - I campi di inserimento per le note personali dell'utente (in `PersonalDialogs` e `QuickNoteBottomSheet`) devono applicare tassativamente un vincolo di altezza con scroll interno (`heightIn(min = 120.dp, max = 220.dp)` con `.verticalScroll(rememberScrollState())`) per impedire che testi lunghi deformino il dialog spingendo i pulsanti di azione fuori schermo.
      - Il limite massimo di caratteri è armonizzato a **5.000** caratteri (conforme ai worker di sync Trakt/backup e all'architettura Room), con contatore discreto in tempo reale (`X / 5000`) e clamp anche sui risultati di dettatura vocale (`.take(5000)`).
      - Abilitare sempre `visualTransformation = remember { MarkdownVisualTransformation() }` per formattare live grassetto, corsivo e barrato durante la digitazione.
    - **Modali di Filtro con Morphing dal Pulsante (`MorphGlassModal` & `triggerBounds`)**:
      - I pannelli di filtro secondari devono aprirsi tassativamente tramite morphing dal pulsante d'innesco, catturando la posizione a schermo tramite `onGloballyPositioned` (`triggerBounds = Rect(...)`) e delegando la transizione a `MorphGlassModal` per garantire una fisica fluida a molla identica a Home, Visti e Trofei.

- **Integrazione Card Profilo & Retrocompatibilità Modale Layout (`DashboardSettingsDialog`)**:
    - **Migrazione Trasparente DataStore**: Quando una nuova card del profilo (`AccountScreen`, es. `HistoryDiaryCard` per il Diario) viene resa personalizzabile, `PreferenceRepository.dashboardCardOrder` deve iniettare la nuova chiave (es. `"diary"`) in coda alle liste preesistenti senza azzerare o alterare l'ordine personalizzato già salvato dall'utente.
    - **Ciclo Completo nel Modale**: La card deve comparire in `DashboardSettingsDialog` con icona vettoriale nativa coerente (es. `R.drawable.ic_calendario`), switch reattivo (`showDiary`), supporto drag & drop per il riordinamento e inclusione nell'ordine predefinito del tasto reset (`stats`, `folders`, `trophies`, `flow`, `diary`).
    - **Reattività in UI**: In `AccountScreen`, la card adotta la posizione definita in `dashboardCardOrder` ed è protetta dal controllo `if (showDiary)`.
    - **Parità su 15 Lingue**: `settings_show_diary` e `settings_show_diary_desc` devono essere rigorosamente dichiarati su tutte le 15 lingue supportate in `res/values*/strings.xml`.

- **Standard Navigazione Schermate Figlie del Profilo (`Diario`, `Trofei`, `Cartelle`, `Stats`)**:
    - **Preservazione Bottom Bar Fissa**: Quando l'utente entra in una schermata di approfondimento aperta dal profilo (`HistoryScreen` / Diario, `TrophyRoomScreen`, `FoldersScreen`, `StatsScreen`), la `GlassyBottomBar` rimane fissa e visibile alla base dello schermo con `AccountTab` evidenziata.
    - **Clearance e Padding Inferiore Lista**: Per evitare collisioni tattili tra gli ultimi elementi scrollabili e la barra flottante, il container della lista deve applicare un padding inferiore di almeno **`104.dp`** (`80.dp` barra + `24.dp` respiro).
    - **Top Bar a Pillola Unificata (`GlassyTopBar`)**: Utilizzare la pillola galleggiante `GlassyTopBar` con titolo essenziale in minuscolo/capitalizzazione standard (es. *"Diario"*), back button che torna ad `AccountTab`, senza statistiche duplicate nell'header che soffocano il viewport.
    - **Transizioni Orizzontali Coerenti**: L'apertura e la chiusura della schermata devono adottare una transizione orizzontale fluida (`slideInHorizontally` da destra a sinistra in ingresso e `slideOutHorizontally` da sinistra a destra in uscita).

- **Purezza Cronologica del Diario di Bordo (Anti-Feature Trap)**:
    - **Sequenza Temporale Pura**: Il Diario di Visione / Cronologia è concettualmente un registro temporale delle sessioni di visione dell'utente. A differenza della schermata **Visti** (che funge da catalogo/libreria e richiede filtri per titolo, valutazione, genere o popolarità), il Diario ammette **esclusivamente l'ordinamento cronologico** (`DATE_DESC` o `DATE_ASC`).
    - **Divieto Filtri Alfabetici e per Voto**: È tassativamente vietato introdurre ordinamenti per titolo alfabetico o voto personale nel Diario: distruggono il modello mentale del "diario di bordo" cronologico, generano debito di design e creano sovrapposizioni confuse con la schermata Visti.

- **Parsing Timestamp ISO PostgREST / Supabase (`+00:00` vs `Z`) e Fusi Orari Locali**:
    - **Compatibilità con Offset UTC Espliciti**: PostgreSQL e PostgREST (Supabase) serializzano i campi `timestamptz` con l'offset esplicito `+00:00` (es. `2026-10-24T13:57:00+00:00`). `Instant.parse()` su Android/Java 8 fallisce con `DateTimeParseException` poiché si aspetta la notazione `Z`.
    - **Parser Robusto a Doppio Stadio**:
      ```kotlin
      val instant = try {
          Instant.parse(timestampStr)
      } catch (e: Exception) {
          try {
              java.time.OffsetDateTime.parse(timestampStr).toInstant()
          } catch (e2: Exception) {
              null
          }
      }
      ```
    - **Divieto di Fallback Distruttivi**: Non assegnare `Instant.MAX` o date future fittizie in caso di fallimento o assenza di scadenza su filtri temporali (es. `expires_at > now()`), per evitare che inviti scaduti o corrotti appaiano perennemente attivi.
    - **Visualizzazione nel Fuso Orario Locale dell'Utente**: Per la visualizzazione a schermo di date e orari provenienti dal cloud UTC, convertire sempre con `instant.atZone(java.util.TimeZone.getDefault().toZoneId()).toLocalDateTime()`. Formattare ore e minuti (`HH:mm`) preservando l'orario effettivo del dispositivo ed evitando scostamenti di ore dovuti all'UTC crudo.

- **Invariante Cartelle Condivise: Auto-Revert a Privata (Zero Cloud Waste & Data Preservation)**:
    - **Single Source of Truth & Integrità Locale**: Una cartella condivisa non deve rimanere in stato `isShared = true` se appartiene all'owner e non ha nessun altro collaboratore attivo (`otherMembers.isEmpty()`) né inviti validi pendenti (`activeInvites.isEmpty()`).
    - **Riconversione Automatica a Locale**:
      1. Viene riconvertita automaticamente a cartella privata locale (`isShared = false`, `syncStatus = "local"`), preservando al 100% tutti i film, le serie TV e i metadati Room esistenti.
      2. Le risorse remote su Supabase (`shared_folders`, `folder_members`, `folder_invites`) vengono epurate silenziosamente a costo zero per evitare costi di hosting e tabelle orfane sul cloud.
      3. L'utente viene notificato in-app con un feedback discreto (`folder_auto_reverted_private`), garantendo chiarezza sul nuovo stato locale della cartella.

- **Architettura Cartelle Condivise Collaborative (Supabase Real-Time, Inviti & Permessi)**:
    - **Limite Rigido a 25 Membri per Cartella (`MAX_MEMBERS = 25`)**:
      - Ogni cartella condivisa può ospitare al massimo 25 membri totali (Owner incluso).
      - Il controllo del limite deve essere applicato a monte nella UI (`FolderJoinDialog` e `FolderShareModal`), disabilitando la creazione di nuovi inviti e respingendo l'accesso se `memberCount >= 25`.
      - Lato database, il vincolo è presidiato dalle policy PostgreSQL RLS e/o vincoli di trigger.
    - **Formato Codici Invito Alfanumerici (`FT-XXXXX`)**:
      - I codici di invito generati adottano tassativamente il prefisso `FT-` seguito da 5 caratteri alfanumerici casuali in maiuscolo (es. `FT-AB12C`).
      - Per prevenire errori di digitazione e confusione visiva, il generatore esclude caratteri ambigui come `0` vs `O` e `1` vs `I`.
      - Nella dialog di inserimento (`FolderJoinDialog`), il campo accetta solo i 5 caratteri del corpo (filtrando prefisso se incollato) e il pulsante di unione si abilita rigorosamente solo quando `input.length == 5`.
    - **Separazione Rigorosa Ruoli e Permessi (`Owner` vs `Collaborator`)**:
      - *Owner*: è l'unico utente abilitato a eliminare l'intera cartella condivisa, revocare o modificare ruoli (`admin`/`collaborator`), espellere membri e generare o cancellare inviti.
      - *Collaboratore*: può aggiungere o rimuovere media (`folder_items`), ma non può eliminare la cartella né modificare i permessi di altri partecipanti.
    - **Aggiornamenti Ottimistici in Room (Zero Latency UX)**:
      - Quando un membro aggiunge o rimuove un film/serie da una cartella condivisa, l'entità SQLite locale in Room viene aggiornata **immediatamente** prima di qualsiasi chiamata di rete.
      - La sincronizzazione verso Supabase avviene in background in modo asincrono; in caso di errore di connessione, la modifica rimane persistente in locale e viene accodata per il retry senza bloccare l'interfaccia né mostrare toast di crash.
    - **Gestione Abbandono Cartella (Leave vs Delete)**:
      - Quando un collaboratore abbandona una cartella condivisa, la cartella viene rimossa localmente dal suo dispositivo (o trasformata in privata se richiesto) senza toccare la cartella remota né i contenuti degli altri 24 partecipanti.
      - Se invece l'Owner elimina la cartella, la cancellazione cancella a cascata i riferimenti cloud, notificando i membri al successivo sync delta.

- **Hardening Sincronizzazione Cloud Esterna (Trakt.tv & SIMKL Lifecycle)**:
    - **Gestione Trasparente Token Refresh (Zero-Crash su Scadenza Sessione)**:
      - Se un token OAuth 2.0 scade durante un job di sincronizzazione `WorkManager` (es. `TraktSyncWorker`, `SimklSyncWorker`), il worker deve intercettare l'errore HTTP 401 Unauthorized e tentare immediatamente il rinnovo tramite `refreshToken`.
      - Se anche il refresh fallisce (token revocato dall'utente sul web o credenziali invalide), marcare l'account come scollegato in DataStore in modo silenzioso, senza lanciare notifiche allarmanti né bloccare l'esperienza offline dell'app.
    - **Chunking Batch nelle Scritture Esterne (Anti-Rate-Limiting)**:
      - Quando si inviano batch di aggiunta/rimozione cronologia o watchlist verso Trakt o SIMKL, suddividere le richieste in chunk di 50-100 elementi (`chunked(50)`), rispettando i rate-limit delle rispettive API (es. 1 req/sec su Trakt free tier) per prevenire risposte `429 Too Many Requests`.

