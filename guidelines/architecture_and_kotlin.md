# FlickTrove Guidelines: Architettura & Kotlin Invariants

## Deep Link & Saghe

- **Deep Link, App Link & Condivisione Saghe Editoriali vs TMDB (`MainDeepLinkHandler` & `CollectionDetailScreen`)**:
   - **Preservazione String ID per Collezioni**: Nei deep link (`isHttpsAppLink` e `isCustomScheme`), non convertire mai preventivamente l'intero parametro `id` con `toLongOrNull()` a monte. Le collezioni TMDB hanno ID numerici, ma le saghe editoriali Firestore possiedono ID alfanumerici (es. `"marvel_mcu"` o ID documento Firestore). Il parsing deve estrarre `idStr` e passarlo intatto a `CollectionDetailScreen(idStr, null)`, applicando `toLongOrNull()` unicamente ai branch `movie`, `tv` e `person`.
   - **ID Reale nella Condivisione**: Nella scheda saga (`CollectionDetailScreen`), generare il link di condivisione con `uiState.collectionId.ifBlank { uiState.collection?.id?.toString() ?: "" }`. È vietato usare direttamente `uiState.collection?.id` poiché per le saghe editoriali Firestore esso contiene un hashCode fittizio generato per mockare la risposta TMDB (`id.hashCode().toLong()`).
   - **Parametri URL `&t=` e `&p=` per Saghe e Persone**: Includere sempre il titolo encodato (`&t=`) e la locandina/profilo (`&p=`) anche nei link di saghe e persone (oltre a film e serie TV), affinché la pagina web `open.html` carichi immediatamente copertina e titolo senza dover effettuare fetch TMDB e fallire in 401/404 per contenuti editoriali.
   - **Localizzazione & Auto-Launch su `open.html`**: `open.html` deve supportare internazionalizzazione completa a 13 lingue native (IT, EN, HI, JA, KO, ZH, ID, TR, ES, FR, DE, PT, RU), estrazione cromatica dinamica dal poster via canvas, gestire gli ID alfanumerici non numerici senza tentare endpoint TMDB inesistenti, e sfruttare la sintassi Android Intent URI (`intent://media/...#Intent;scheme=flicktrove;package=com.cinetrack;end`) per consentire l'apertura automatica su browser Android.

### Architettura & Kotlin Invariants

- **Ordine di Inizializzazione Flow nei Repository**:
   - In Kotlin le proprietà vengono inizializzate in modo sequenziale. Qualsiasi blocco `init { ... }` o coroutine che avvia la raccolta di un `Flow`/`StateFlow` interno deve trovarsi tassativamente **dopo** la dichiarazione e inizializzazione di tale `Flow`, per prevenire `NullPointerException` fatali al lancio dell'app.
- **Integrità di Sincronizzazione & Anti-Data-Loss**:
   - **Mai marcare come `synced` a priori**: Non impostare mai lo stato `syncStatus = "synced"` su entità Room prima che la scrittura remota su Cloud (Firestore/Trakt/SIMKL) sia stata effettivamente completata con successo.
   - **Propagazione Eccezioni nei DataSource**: I DataSource remoti non devono silenziare le eccezioni di rete/database con blocchi `catch` vuoti o solo log. Devono rilanciare l'eccezione o restituire un `Result` formale, consentendo al Repository di contrassegnare l'elemento come `"pending"` per i retry offline.
   - **Protezione Riconciliazione Differenziale**: Nella sincronizzazione cloud ➔ locale, non eliminare mai record locali se la risposta remota è vuota o parziale a causa di errori/timeout di rete. La sincronizzazione deve abortire immediatamente per preservare la copia locale.
   - **Simmetria Naming Firestore vs Kotlinx Serialization (`camelCase` vs `snake_case`)**:
     - L'SDK Firebase Firestore serializza tramite reflection JavaBeans (generando campi `camelCase`, es. `logoPath`, `profilePath`) e ignora l'annotazione `@SerialName`.
     - Su tutte le entità e data class sincronizzate con Firestore:
       1. Usare esplicitamente sia `@get:PropertyName("snake_case")` che `@set:PropertyName("snake_case")`.
       2. Nei metodi manuali di deserializzazione o conversione mappa (`parseStudio`, `parsePerson`, ecc.), supportare sempre entrambe le convenzioni con fallback difensivo: `(item["snake_case"] ?: item["camelCase"])`, per evitare l'azzeramento silenzioso di campi dopo sync/restore.
- **Ciclo di Vita Coroutine per Operazioni Critiche**:
   - Le operazioni fondamentali di sincronizzazione, login merge o backup non devono mai dipendere dal `viewModelScope` di schermate effimere o transitorie (es. `SplashScreen`). La navigazione immediata (es. `navigator.replaceAll`) distrugge il ViewModel e cancella il job.
   - Utilizzare sempre scope a livello applicativo (`repositoryScope` / `applicationScope` con `SupervisorJob`) o affidarsi a `WorkManager`.
- **Verifica Registrazione Background Worker**:
   - Qualsiasi `Worker` creato per attività periodiche (es. `SyncWorker`) deve essere esplicitamente registrato e schedulato in `FlickTroveApplication.onCreate()`, evitando worker orfani mai eseguiti.
- **Configurazione Cache Firestore**:
   - Assicurarsi che `FirebaseFirestoreSettings` abbia la persistenza offline abilitata esplicitamente. Questo protegge le letture/scritture durante i micro-buchi di rete (es. utente in treno), permettendo all'app di aggiornare istantaneamente la UI e delegando all'SDK il compito di allineare i dati appena la connessione torna stabile.
- **Integrità del `compositeId` e Fallback `mediaType`**:
   - Il `compositeId` (usato ovunque per deduplicazione e filtri) è definito come `"${mediaType}_$id"`. I record salvati **prima dell'introduzione del supporto TV** possono avere `mediaType` vuoto, producendo identificativi come `"_1234"` invece di `"movie_1234"` e rompendo ogni confronto di filtraggio.
   - La proprietà computata `compositeId` nel modello `Movie` deve sempre applicare un fallback difensivo: `if (mediaType.isEmpty()) "movie" else mediaType`.
   - Non costruire mai la stringa `"${it.mediaType}_${it.id}"` inline nei ViewModel/UseCase. Usare sempre `it.compositeId` per garantire il fallback centralizzato e prevenire la comparsa di contenuti già visti nelle sezioni Consigliati, "Perché hai visto...", e "Sorprendimi".
- **Blacklist "Sorprendimi" — Visti/Abbandonati, non Watchlist**:
   - La funzione "Sorprendimi" deve escludere i film che l'utente ha già **visto o abbandonato** (`watched == true || dropped == true`), **NON** quelli nella watchlist.
   - Usare `watchedIds = localMovies.filter { it.watched || it.dropped }.map { it.id }.toSet()` come blacklist sui risultati TMDB.
   - Il fallback offline deve restituire un film non visto e non abbandonato dalla libreria locale.
   - Mai usare `unwatchedMovies` (la watchlist) come blacklist: impedirebbe di suggerire film che l'utente ha esplicitamente salvato per vedere, e lascerebbe passare quelli già visti.
- **Limitazioni Filtri API TMDB — `/recommendations` vs `/discover`**:
   - Gli endpoint `/movie/{id}/recommendations` e `/tv/{id}/recommendations` **non supportano** parametri di filtro avanzati (es. `vote_count.gte`, `vote_average.gte`, `with_genres`).
   - Tali parametri sono **esclusivi** di `/discover/movie` e `/discover/tv`.
   - Su ogni chiamata a `/recommendations`, applicare sempre un filtro difensivo client-side sui risultati grezzi prima di usarli nella UI:

     ```kotlin
     val qualityData = rawData.filter { movie ->
         (movie.voteCount ?: 0) >= 50 &&
         (movie.voteAverage ?: 0.0) >= 5.5 &&
         movie.posterPath != null
     }
     ```

- **Pattern Motore di Raccomandazione**:
   - **Anchor Seed Diversification**: scegliere i 3 seed con generi diversificati. Seed 1 = film più apprezzato, Seed 2 = minimo overlap generi col Seed 1, Seed 3 = casuale. Non sceglierli mai tutti casualmente.
   - **Separazione Seed cross-sezione**: le sezioni "Consigliati" e "Perché hai visto" devono usare seed mutuamente esclusivi per evitare duplicati nella stessa schermata Home.
   - **Quality Gate obbligatorio**: filtrare `rawData` con `voteCount ≥ 50`, `voteAverage ≥ 5.5`, `posterPath != null` prima di qualsiasi logica di scoring o deduplicazione.
   - **Cascata di soglie (Graceful Degradation)**: non usare mai "tutto o niente". Applicare prima la soglia primaria (65%), poi una intermedia (50%), poi il fallback completo ordinato per score. Questo previene schermate vuote per utenti con profili di nicchia.
- **Pattern Match Score & Formula 50/50 (User-Centric Scoring)**:
    - **Bypass Voto Personale (Priorità Assoluta)**: Se l'utente ha già assegnato un `personalRating`, la predizione statistica perde di senso. Il Match Score riflette la valutazione reale: `(personalRating * 10).toInt().coerceIn(10, 99)` (es. voto 10/10 ➔ 99% Match; 8.5 ➔ 85%).
    - **Anti-Diluizione Generi & Confidenza (Volume Shrinkage)**: Non usare la media aritmetica grezza sui generi. Applicare Max Affinity pesata da una curva di confidenza/saturazione basata sul volume dei titoli visti (`count / (count + 3f)`). Un genere esplorato con 1 solo film non può scavalcare generi consolidati con decine di titoli ad alto gradimento.
    - **Smorzamento Bayesiano su TMDB (Anti-Sparse-Data)**: Titoli con pochissimi voti (es. 2-3 voti con media 9.7 o 10.0) non devono prendere 50 punti pieni. Applicare la media ponderata bayesiana con prior di credibilità ($m = 50$, $C = 6.5$):
      $$\text{WeightedRating} = \frac{v \cdot R + 50 \cdot 6.5}{v + 50}$$
      Normalizzare poi il rating efficace su 8.5 per un massimo di 50 punti.
    - **Cap Predittivo al 95%**: Nessuna predizione algoritmica su titoli non ancora votati può eccedere il 95% di Match. La fascia 96%–99% è un club esclusivo riservato unicamente ai titoli a cui l'utente ha assegnato personalmente un voto 10/10.
    - **Soglie Consigliati (Filtri Feed)**: Soglia primaria al 70% (per mostrare solo match gratificanti 75%–95%), degradazione graceful al 55% prima del fallback sui migliori disponibili.
- **Prevenzione God Files e Modularizzazione UseCase**:
    - Non inserire mai nuove responsabilità complesse (parsing regex di ID esterni, chiamate API eterogenee, algoritmi matematici o mapping multipli) direttamente all'interno di ViewModel o Screen già estesi.
    - Creare sempre UseCase dedicati e focalizzati nel package `com.cinetrack.domain` (es. `SearchByIdUseCase`) o classi utility dedicate (es. `FuzzySearch.kt`), iniettandoli via Hilt. I ViewModel devono agire unicamente da orchestratori dello stato UI.
- **Integrità Rating / Check-in su Serie TV (Regola Almeno 1 Episodio)**:
    - Una serie TV può essere votata (nella scheda dettaglio o tramite menu rapido delle card) o registrata con check-in Vibe SOLO se l'intera serie è marcata come completata (`movie.watched == true`) oppure se è stato visto almeno un episodio (`movie.watchedEpisodes?.values?.any { it.isNotEmpty() } == true`).
    - Mai abilitare il voto o il check-in su serie TV basandosi solo sulla data di uscita (`isReleased`) o sullo stato preferito (`favorite`), se il conteggio degli episodi visti è pari a zero.
- **Pattern Ricerca per ID e Fuzzy Matching Locale a 2 Livelli**:
    - **ID con Prefisso Esplicito**: Per non rompere la ricerca di titoli numerici storici (es. *1917*, *300*, *2012*), gli ID numerici TMDB/TVDB richiedono un prefisso esplicito (`#`, `id:`, `tmdb:`, `tvdb:`). I codici IMDb (`tt...`) sono invece intercettati in automatico.
    - **Fuzzy Search a 2 Livelli**:
      - *Livello 1 (Priorità Assoluta)*: Ricerca esatta/sottostringa `contains(query)`. Finché ci sono risultati validi, nessun algoritmo fuzzy viene eseguito (zero overhead e zero rumore).
      - *Livello 2 (Fallback difensivo)*: Solo con 0 risultati e `query.length >= 4`, applicare la distanza di Levenshtein con soglia dinamica: tolleranza = 1 per 4..6 caratteri, tolleranza = 2 per $\ge 7$ caratteri.
- **Isolamento Rigoroso delle Categorie nella Ricerca & Risoluzione ID (Zero-Leakage)**:
    - Quando l'utente naviga all'interno di una categoria specifica (es. tab **FILM** o **SERIE TV**), ogni livello di ricerca (query testuale, fuzzy search locale su Room, e risoluzione per ID TMDB/IMDb/TVDB) deve rispettare tassativamente il perimetro della categoria attiva.
    - **Divieto Assoluto di Fallback Cross-Categoria**: Se un ID o una query non trova corrispondenze nella categoria attiva (es. digitando l'ID di una serie TV nella scheda Film), non eseguire mai chiamate di fallback verso l'altro media type (es. vietato chiamare `/tv/{id}` se `/movie/{id}` fallisce in 404 quando l'utente si trova in Film). Una serie TV non deve mai inquinare la scheda Film e viceversa.
- **Proprietà nel Body dei Data Class e Copia Oggetti (`copy` vs `apply`)**:
    - In Kotlin, il metodo generato `.copy(...)` include tassativamente **solo** i campi definiti nel costruttore primario del `data class`.
    - Nel modello `Movie`, proprietà come `logoPath`, `matchScore`, `extractedWatchDates` sono dichiarate all'interno del corpo (`body`) della classe. Tentare `movie.copy(logoPath = ...)` causa l'errore di compilazione `No parameter with name 'logoPath' found`.
    - Per aggiornare tali proprietà durante una duplicazione, usare rigorosamente la forma con `apply`:

      ```kotlin
      val movieWithColor = movie.copy(
          accentColor = dominantColor?.toHexString() ?: movie.accentColor
      ).apply {
          this.logoPath = localLogoPath ?: movie.logoPath
      }
      ```

- **Limite Batch Firestore (500 Ops) & Idempotenza Notifiche Social**:
    - **Chunking Tassativo a 500 Ops**: In Firebase Firestore, qualsiasi `WriteBatch` fallisce con eccezione fatale se supera il limite rigido di **500 operazioni**. Nelle operazioni di massa (es. segnare come lette tutte le notifiche o i bundle di ID aggregati), suddividere sempre la collezione con `.chunked(500)` ed eseguire il commit di ogni batch in sequenza o parallelo controllato.
    - **ID Deterministico Idempotente**: Per interazioni univoche (es. like a un commento), generare tassativamente l'ID del documento con formula deterministica `${commentId}_${userId}_like` utilizzando `set(data, SetOptions.merge())`. Questo rende l'operazione intrinsecamente idempotente a costo di 1 sola scrittura, azzerando le race condition e i duplicati da spam-click o retry offline.
    - **Payload Embedding (Anti-N+1 Reads)**: Le notifiche devono incorporare al momento della creazione remota un estratto essenziale del contenuto a cui si riferiscono (`commentSnippet`). Vietato costringere il client a effettuare query secondarie per ogni singola notifica mostrata in lista per recuperare il testo originale.
- **Pattern Architetturale Importer Esterni & Guardrail di Migrazione (Movie Paradise, Bingers, Sofa, Refract, Seenr)**:
    - **Instradamento ZIP a Impronta Digitale (`zipEntries.keys`)**:
      - Ispezionare la lista dei file all'interno dello ZIP prima di instradare verso l'importer dedicato. Rispettare sempre una sequenza di fingerprint rigida (`isTvTimeGdpr` -> `isTraktExport` -> `isMovieParadiseZip` -> `isBingersZip` -> `isSofaZip` -> `isRefractZip` -> Fallback file singoli) per evitare collisioni di file comuni (`library.csv`, `items.csv`, `data.json`).
    - **Parsing CSV RFC-4180 (Anti-Virgole-Nei-Titoli)**:
      - Mai usare `line.split(",")` per il parsing di file CSV (es. `items.csv`, `library.csv`, Seenr). I titoli (es. *"Il Buono, il Brutto, il Cattivo"*), le note personali e i nomi delle cartelle contengono spesso virgole. Usare sempre `ImporterUtils.parseCsvLine` (o parser RFC-4180 con gestione degli apici/virgolette) per prevenire lo sfalsamento delle colonne e crash a runtime.
    - **Streaming JSON su Alberi Profondi (Anti-OOM con `JsonReader`)**:
      - Per formati JSON con annidamenti profondi (es. Refract con centinaia di serie TV e migliaia di stagioni ed episodi), vietato caricare l'intero file in memoria RAM con parser DOM in-memory (`readText()` o AST completo). Utilizzare sempre lo streaming con `com.squareup.moshi.JsonReader`, leggendo nodi, stagioni ed episodi al volo e processando la risoluzione TMDB in batch asincroni controllati (es. 20 elementi).
    - **Validazione Rigorosa della Fonte (`api_source == "tmdb"`)**:
      - Non assumere mai che gli ID esterni numerici o stringa siano ID TMDB validi a meno che `api_source` non sia esplicitamente `"tmdb"` (o documentato formalmente come tale). Ignorare tassativamente ID appartenenti ad altri database (TVDB, Trakt, iTunes, IGDB) per prevenire collisioni letali su titoli storici (es. *1917*, *300*). Filtrare a monte le categorie non video (es. Audiobooks, Videogiochi, Podcast).
    - **Disaccoppiamento per Testabilità Offline (`MovieLookupService`)**:
      - Nei nuovi importer che necessitano di risolvere metadati (IMDb, TVDB, titolo), non iniettare direttamente il God-Object `MovieRepository` come classe concreta final nei costruttori di test. Estrarre un'interfaccia dedicata (`MovieLookupService`) implementata da `MovieRepository` ed esporre un costruttore secondario `@Inject` per Hilt. Questo permette di testare gli importer in test unitari JVM offline al 100% senza dover istanziare database Room o client di rete.
- **Pattern di Smembramento God Files & Delegazione nei Repository (Zero-Breaking-Changes)**:
    - **Conservazione del Package (`Same-Package Extraction`)**: Quando si suddivide un file UI o modale monolitico (es. `SettingsDialogs.kt` ➔ 10 file, `StatsCharts.kt` ➔ 4 file), mantenere tassativamente lo stesso package originario (es. `com.cinetrack.ui.components.settings`, `com.cinetrack.ui.components.stats`). Grazie alla risoluzione dei simboli a livello di package in Kotlin, i file chiamanti (Screen, ViewModel, Dialog adiacenti) compilano all'istante senza dover riscrivere import o rompere riferimenti.
    - **Delegazione Trasparente nei Repository (`Facade Delegation`)**: Quando si scorporano sottodomini ortogonali (es. Cartelle, Watch History, Ricerche recenti, Box Office) da un God Repository centrale (`MovieRepository`):
      1. Creare repository dedicati annotati con `@Singleton` iniettando i rispettivi DAO e remote DataSource.
      2. Mantenere invariate le firme pubbliche nel repository principale, iniettando il nuovo repository delegato e inoltrando le chiamate. Questo disaccoppia la persistenza e le logiche di sincronizzazione cloud preservando la piena retrocompatibilità per le decine di ViewModel già esistenti.
    - **Divieto Assoluto di Dipendenze Cross-Screen**: Vietato importare `@Composable` privati o componenti UI interni direttamente dal file di un altro Screen (es. `FeedbackDialog` o `DetailComments` che importano da `CommentsScreen`). Se un componente o un parser (es. Markdown) serve a più schermate, estrarlo sempre in un file/package condiviso (`com.cinetrack.ui.utils`, `com.cinetrack.ui.components.shared`, `com.cinetrack.ui.components.home`).
    - **Isolamento Scope & Receiver in Compose**: In gerarchie annidate (es. `ColumnScope` o blocchi Box dinamici), invocazioni dirette di `AnimatedVisibility` possono generare ambiguità di receiver. Qualificare sempre in modo esplicito (`androidx.compose.animation.AnimatedVisibility`) o isolare i blocchi animati in composable dedicati con transizioni indipendenti (es. `AnimatedHomeSection`).
- **Gerarchia Crediti TMDB: Serie TV vs Film (`created_by` vs `Director`)**:
    - **Asimmetria di Dominio**: Nei Film l'autore principale è il Regista (`job == "Director"` in `credits.crew`). Nelle Serie TV la regia varia per ogni singolo episodio; a livello di serie TMDB restituisce gli showrunner/autori primari nel campo radice **`created_by`**.
    - **Fallback nel Mapping**: In `MovieMapper` e `DetailUiStateMapper`, per le serie TV o dove mancano registi (`job == "Director"`), mappare tassativamente i membri di `created_by` come autori in `directorData` / `directorName` e iniettarli nella troupe con `job = "Creator"`.
    - **Dinamismo Titolo Sezione (`DetailCast`)**: La sezione autori visualizza dinamicamente `REGIA` per soli registi, `IDEATORE` / `IDEATORI` per soli creatori, o `IDEATORI / REGIA` se entrambi, mostrando il badge di ruolo appropriato sotto l'avatar.
    - **Disaccoppiamento del Pulsante Troupe**: Il pulsante **`TROUPE >`** non deve mai dipendere dalla presenza di registi (`directors.isNotEmpty()`). Se la sezione autori è presente, il pulsante è posizionato in alto a destra; se non ci sono registi né creatori ma la serie possiede altri reparti (produzione, scrittura, musiche), il pulsante si ancora all'header del **`CAST`**.
- **Pattern di Sincronizzazione Cloud, OAuth2 PKCE e Resilienza DTO (Simkl & Trakt Invariants)**:
    - **Blindatura Security State nel PKCE (Anti-CSRF)**:
      - Nei flussi OAuth 2.0 con PKCE (`S256`), generare crittograficamente sia il `code_verifier` (64 caratteri Base64 URL-safe senza padding) sia un parametro `state` casuale.
      - Salvare entrambi in storage persistente (DataStore/Preferences) **prima** di avviare il browser di sistema (`/oauth2/authorize`).
      - Al ritorno del deep-link (`flicktrove://...`), validare rigorosamente byte-for-byte lo `state` ricevuto. Se nullo o difforme, abortire immediatamente l'operazione con errore. Solo se valido, consumare il `code_verifier` per lo scambio token.
    - **Rinnovo Token Disaccoppiato (Anti-Deadlock Authenticator)**:
      - L'OkHttp `Authenticator` per il rinnovo automatico (`401 Unauthorized`) tramite `refresh_token` deve utilizzare un'istanza Retrofit/OkHttp disaccoppiata (es. `@Named("simkl_refresh_service")`), priva sia di `AuthInterceptor` che di `Authenticator`, per scongiurare loop infiniti e deadlock in caso di token revocato o non valido.
      - In contesti sincroni (`runBlocking`), eseguire il salvataggio dei nuovi token all'interno del medesimo blocco prima di restituire la nuova `Request` con l'header `Authorization: Bearer <token>` aggiornato.
    - **Pacing e Rate Limiting Difensivo nei Background Sync Worker (Anti-Ban IP TMDB)**:
      - Quando si sincronizzano librerie o liste remote (Trakt / Simkl) verso Room locale, il download di titoli mancanti da TMDB deve includere un delay difensivo (es. `delay(60L)`) tra le richieste singole, garantendo un throughput controllato (max ~16 req/sec, ben inferiore al cap di 50 req/sec di TMDB) e raggruppando i salvataggi tramite `saveMoviesBulk`.
    - **Graceful Degradation e Polimorfismo Difensivo nei Modelli DTO (Free vs PRO/VIP & ID Ibridi)**:
      - Nei servizi con tier a pagamento (es. Simkl Custom Lists), se un utente Free effettua la richiesta, le API possono rispondere con `HTTP 200` ma con corpo `{"error": "premium_only"}` privo dell'array atteso.
      - Tutti i campi dei modelli DTO devono essere opzionali/nullable con default `null` ed esporre `error: String? = null`. Controllare sempre `response.error` prima di accedere agli elementi per evitare eccezioni di deserializzazione (`JsonDecodingException`).
      - Per ID esterni che possono alternare tipi numerici o stringhe nei JSON (`tmdb`, `tvdb`), utilizzare `@SerialName` con `JsonElement` e un getter helper (`.toLongOrNull()`), scongiurando eccezioni di tipo a runtime.
- **Integrità Dati Importer & Auto-Healing Copertine**:
    - Nei costruttori `Movie(...)` all'interno degli importer, quando un record viene risolto su TMDb (per IMDb ID o ricerca), è tassativo mappare tutti i campi visivi: `posterPath = it.posterPath`, `backdropPath = it.backdropPath`, `overview`, `genreIds`, `voteAverage`.
    - Per gli importer basati su formati terzi che esportano solo ID (Trakt ZIP, Bingers, Sofa):
      1. Non eseguire chiamate di rete sincrone massive durante l'import (rischio rate-limit e blocchi su file con migliaia di righe).
      2. Delegare l'arricchimento al worker in background tramite `repository.healMissingPosters()`, che recupera progressivamente locandine e metadati mancanti senza degradare la reattività dell'app.
- **Resilienza Firestore Android SDK & Flussi di Sincronizzazione Auth (Anti-Hang Invariants)**:
    - **Offline Queue Hang Trap su `.await()`**:
      - Nell'SDK Firestore per Android, quando la persistenza offline è abilitata o la connessione gRPC è debole/in riconnessione, le chiamate di scrittura coroutine (`batch.commit().await()`, `doc.set().await()`, `doc.delete().await()`) **non vanno in timeout nativamente e non lanciano eccezioni**, poiché accodano l'operazione nella cache SQLite in attesa della conferma del server.
      - **Divieto Assoluto di `.await()` Senza Timeout**: Tutte le operazioni Firestore nei Remote DataSource (`FirebaseRemoteDataSource`) devono essere racchiuse tassativamente in `withTimeoutOrNull(...)` (es. 6.000ms per letture/scritture atomiche, 10.000–12.000ms per batch di massa), consentendo al DataSource di fallire con grazia o restituire un fallback senza appendere indefinitamente la coroutine.
    - **Integrità Flusso di Sincronizzazione nei Repository**:
      - Operazioni preliminari di allineamento (es. `pushPendingChanges()`) devono essere eseguite all'interno di blocchi `try/catch` con timeout dedicato (es. `withTimeoutOrNull(15_000L)`). Un fallimento o timeout del push offline non deve mai abortire né impedire la sincronizzazione principale dei dati cloud.
    - **Reset di Stato UI Tassativo (`try ... finally`)**:
      - Nei ViewModel (es. `AuthViewModel`), qualsiasi operazione asincrona che commuta lo stato della UI in modalità attesa/caricamento (`_processState = AuthProcessState.Uploading...`) deve racchiudere la logica in `try { ... } finally { ... }`. Lo stato di attesa deve essere azzerato o confermato in qualsiasi scenario di uscita (completamento, timeout o eccezione).
    - **Safety Escape Hatch nelle Schermate di Blocco**:
      - Nelle schermate di autenticazione o caricamento iniziale con UI bloccante, prevedere sempre un timer di sicurezza (5–6 secondi) che svela un pulsante di scampo con `CircleShape` e `bounceClick` (*"Continua comunque"* / `auth_sync_continue_anyway`), consentendo all'utente di entrare nell'app e delegare il sync al worker in background in caso di rete instabile.
    - **Sanitizzazione Slash (`/`) negli ID Documento**:
      - Il carattere `/` è riservato in Firestore come delimitatore di percorso gerarchico (`collection/doc/subcollection/doc`). Non usare mai stringhe non sanificate contenenti slash (es. compositi stagione/episodio) come ID di documento; applicare sempre `.replace('/', '_')` prima di invocare `.document(...)`.
- **Canonical Emotional Vibes & Auto Self-Healing negli Importer e ViewModel**:
    - **Dominio Canonico Chiuso**: Il sistema delle emozioni (Vibes) di FlickTrove è un dominio rigorosamente controllato e definito in `ALL_VIBES` (12 codici: `MASTERPIECE`, `MIND_BLOWING`, `IN_TEARS`, `HYPED`, `COZY`, `FEELS_GOOD`, `FUNNY`, `MEH`, `WEIRD`, `SCARY`, `DISAPPOINTED`, `BORING`).
    - **Normalizzazione negli Importer Esterni**: Qualsiasi flusso di importazione o sincronizzazione da backup terzi (Movie Paradise, SIMKL, Trakt) deve eseguire tassativamente la normalizzazione delle etichette testuali (es. `shocked` ➔ `MIND_BLOWING`, `thrilled` ➔ `HYPED`, `touched` ➔ `IN_TEARS`) tramite `normalizeVibeCode()` prima della scrittura su Room o Firestore. Vietato persistere codici grezzi non censiti.
    - **Resilienza e Auto Self-Healing nei ViewModel**: I ViewModel che consumano le vibes (es. `FlowViewModel`) devono proteggere l'esperienza utente contro record storici o non normalizzati:
      - Utilizzare sempre `findVibe(code)` che applica la normalizzazione difensiva al posto del lookup rigido.
      - Implementare routine di auto self-healing asincrone in background all'avvio per aggiornare direttamente nel database Room eventuali record pregressi corrotti, eliminando alla radice fallback rotti (es. `❓ x45`) o attribuzioni errate della personalità cinematografica.
- **Standard Sistema Badge & Sala dei Trofei (Trophy Showcase)**:
    - **Tassonomia dei Tiers Cinematografici (No Metalli Arcade)**: È vietato adottare cliché da videogioco (bronzo, argento, oro, diamante). Utilizzare rigorosamente la gerarchia storica dei formati e delle onorificenze cinematografiche:
      1. **Super 8** (`#F59E0B` - Ambra Vintage): Primi passi amatoriali, scoperte e curiosità iniziale.
      2. **16mm** (`#10B981` - Smeraldo FlickTrove): Cinema indipendente, cortometraggi e prime maratone (colore brand dell'app).
      3. **35mm** (`#60A5FA` - Azzurro Zaffiro Brillante): Il formato standard della sala cinematografica e la costanza di visione (alta saturazione e vivacità parificata agli altri formati).
      4. **70mm** (`#EAB308` - Oro Regale): Il grande formato epico, proiezioni prestigiose e dedizione cinefila.
      5. **The Final Cut** (`#EC4899` - Ametista & Magenta Profondo): Il montaggio definitivo, l'apice leggendario della cineteca.
      6. **Lost Reel** (`#8B5CF6` - Viola Nebulosa & Nero Onice): La bobina perduta per Easter eggs, misteri e badge segreti.
    - **Atomicità ed Evoluzione Dinamica (No Duplicati nella Griglia)**:
      - I traguardi progressivi (es. film visti, ore, serie, saghe, voti, commenti) non devono creare card separate per ciascuna soglia numerica. Condividono tassativamente un'**unica card dinamica per tema** che evolve di aspetto, colore e grado al raggiungimento del Tier successivo.
      - La griglia trofei deve rimanere pulita, ordinata e priva di ridondanze visive (circa 20-25 card totali).
    - **Anti-Clutter & Zero Ridondanze Visive**:
      - **Divieto Etichette Decorative Futili**: Vietato inserire testi di stato puramente decorativi o ridondanti nelle card dei trofei (es. *"Opera Compiuta"*, *"Onorificenza Sbloccata"*, *"Traguardo Conquistato"*, *"Mistero Svelato"*). Lo stato di sblocco e avanzamento è già chiaramente comunicato dalla micro-barra progressiva, dalla lucentezza dell'emblema e dal lucchetto. Mantenere le card minimali e riservare lo spazio unicamente ai valori numerici effettivi (`corrente/target`).
      - **Deduplicazione Metriche e Rarità**:
        - La percentuale di rarità globale della community (`communityUnlockRate`) deve comparire un'unica volta come chip pillola nell'header del modale di dettaglio (`BadgeDetailModal`), mai duplicata o ripetuta nel footer informativo.
        - Nei testi descrittivi di progressione, mai ripetere indicazioni tra parentesi (es. `(10 serie)`) se l'unità e la soglia sono già esplicitate nella chip o nella barra di progresso sovrastante.
    - **Filosofia Anti-Spam & Zero Frustrazione**:
      - Vietato introdurre badge futili o premiare singoli clic iniziali (es. primo tocco, apertura app, cambio tema istantaneo) che fanno apparire l'app come un software promozionale o invasivo. Le soglie devono premiare la passione e l'uso naturale e autentico.
      - Vietati modali bloccanti a tutto schermo durante il flusso di utilizzo: l'ottenimento di un badge notifica l'utente in modo discreto (accento/dot sulla tab Account o snackbar non invasiva).
    - **Retroattività One-Shot & Zero Notifiche a Raffica**:
      - Per gli utenti storici già presenti, il calcolo dei traguardi pregressi deve avvenire all'avvio in background tramite scansione Room ($\le 40\text{ ms}$) preservando le date reali storiche (`watchedAt` / `clientUpdatedAt`).
      - Tutti i badge sbloccati nella scansione retroattiva devono essere pre-impostati con `isNotified = true` per non inondare l'utente di avvisi all'avvio.
    - **Divieto Assoluto di Emoji & Iconografia Vettoriale FlickTrove**:
      - È tassativamente vietato utilizzare emoji di sistema per i badge. Adottare esclusivamente icone vettoriali Compose originali disegnate su misura, con geometrie cinematografiche pulite, spessori armoniosi (1.5–2.0 dp) e bagliori d'accento.
    - **Standard Modale di Dettaglio (`BadgeDetailModal`)**:
      - Implementare il modale esclusivamente con `FlickTroveModal` (overlay full-screen con `hazeGlass`, mai `Dialog` nativi) a raggio `32.dp`.
      - Pulsante di chiusura unificato `ModalCloseButton` posizionato concentricamente a `16.dp` dai bordi.
      - **Metadati Affiancati (Rarità + Data)**: Sotto al titolo del trofeo, mostrare un'unica riga orizzontale (`Row` con `spacedBy(8.dp)`) contenente due chip gemelle a fondo neutro traslucido (`CircleShape`, `Color.White.copy(alpha = 0.07f)`, testo `Color(0xFF94A3B8)` `11.sp` SemiBold): la percentuale di rarità (`Sbloccato dal X% dei cinefili`) e la data di sblocco effettiva (`18 Lug 2026`). Vietato collocare la data in box colorati o pillole isolate in calce al modale.
      - **Ciclo di Vita della Timeline Formati (Anti-Ridondanza)**: Nei trofei progressivi, mostrare la scalata dei formati (`BadgeTierEvolutionTrack`) tassativamente **solo se il trofeo è in corso di avanzamento** (`!isMaxTier`). Quando si raggiunge il traguardo massimo (*The Final Cut* o step apicale), la timeline va omessa per mantenere la modale compatta ed elegante.
    - **Prestigio dei Trofei Segreti (Lost Reel in Cima)**:
      - Nel calcolo del rank dei tier (`getTierRank`), assegnare tassativamente a `PreviewBadgeTier.LOST_REEL` il rango massimo (`7`, superiore a `THE_FINAL_CUT` che è `6`). In questo modo, nell'ordinamento predefinito della Sala dei Trofei (Tier decrescente), i trofei segreti sbloccati risiedono in cima alla griglia come prime opere d'onore.
      - Nel modale dei filtri (`TrophyFilterModal`), posizionare `LOST REEL` al vertice della sezione *"FORMATO PELLICOLA"*.
    - **Trasparenza Card e Divieto Blur nei Componenti Interni**:
      - Le card dei trofei (`TrophyBadgeCard`) devono adottare superfici traslucide neutre (`Color.White.copy(alpha = 0.05f)` con bordo a `0.15f`), evitando sfondi opachi/solidi scuri che oscurano i gradienti ambientali di `CinematicBackground`.
      - È vietato applicare blur o `hazeGlass` su card o griglia: il blur è riservato esclusivamente agli overlay fluttuanti di sistema (`GlassyTopBar`, `GlassyBottomBar` e modali).
    - **Celebrazione In-App (Juice Visivo & BadgeUnlockedToast)**:
      - Quando un badge viene sbloccato in real-time, mostrare una notifica in-app discreta con `BadgeUnlockedToast` che cala dall'alto con molla fluida, bagliore al neon del tier conquistato e micro-tick aptico (`VibrationHelper.vibrateClick`), senza interrompere la navigazione né bloccare i tocchi.
      - Al tap sul toast si apre direttamente il `BadgeDetailModal`.
      - Nella scansione retroattiva iniziale dei dati storici, `isNotified = true` previene tassativamente raffiche di toast all'avvio.
    - **Condivisione Social 9:16 (Flex Cinefilo)**:
      - Nel `BadgeDetailModal`, inserire il pulsante Condividi per generare una card cinematografica in formato 9:16 (1080x1920) ottimizzata per Instagram/WhatsApp Stories: gradiente del tier, icona gigante con bagliore, titolo, rarità globale della community e watermark FlickTrove, condivisa tramite `FileProvider` con `Intent.ACTION_SEND`.
    - **Archetipi Onboarding e Longevità**:
      - Premiare il ciclo di vita dell'utente con *Il Trasloco* (`badge_onboarding_sync` al primo import o sync SIMKL/Trakt) e *Tessera del Cineclub* (`badge_veteran_club` evolutivo da Super 8 a The Final Cut in base all'anzianità dell'account).
    - **Internazionalizzazione Simultanea Obbligatoria (15 Lingue)**:
      - Tutti i titoli dei badge, descrizioni, formati e teaser misteriosi devono essere localizzati tassativamente in tutti i 15 file `strings.xml` di FlickTrove (`values`, `values-it`, `values-de`, `values-es`, `values-fr`, `values-pt-rBR`, `values-ru`, `values-tr`, `values-ja`, `values-ko`, `values-zh`, `values-zh-rCN`, `values-id`, `values-in`, `values-hi`).
    - **Centratura Ottica Mirata & Divieto di Offset Globali Ciechi**:
      - È tassativamente vietato applicare traslazioni o offset cartesiani (es. `-X, +Y`) globalmente a tutte le icone del catalogo o ai lucchetti per correggere impressioni visive circoscritte.
      - Applicare un offset correttivo verso l'alto (`offset(y = -size * 0.032f)`) **esclusivamente** alle icone a forte asimmetria verticale o con base pesante (es. cloche `ic_badge_hot_meal`, piedistalli `ic_badge_star_altar`, treppiedi `ic_badge_film_projector`, televisori `ic_badge_tv`, lanterne `ic_badge_lantern_flame`, bilance `ic_badge_scales`), neutralizzando la loro tendenza naturale ad apparire schiacciate sul fondo della lente. Viceversa, applicare un offset correttivo verso il basso (`offset(y = size * 0.028f)`) a icone con barra orizzontale pesante in cima (es. medaglia a stella `ic_badge_star_medal` per *La Giuria*) per evitare che tocchino il margine superiore dell'arco.
    - **Rapporto di Scala Differenziato e Ritaglio Concentrico (`CircleShape`)**:
      - L'area centrale della lente deve incapsulare l'icona all'interno di un contenitore circolare ritagliato: `Box(modifier = Modifier.size(innerCircleSize).clip(CircleShape), contentAlignment = Alignment.Center)`.
      - Non ingrandire indistintamente tutte le icone: quelle compatte mantengono la scala standard (`size * 0.46f / 0.52f`) per respirare armoniosamente. Le icone con ampi spazi negativi, fasci o braccia estese (*Scenografo Personale*, *Notte delle Ombre*, *Il Produttore Esecutivo*) adottano una scala maggiorata dedicata (`innerCircleSize * 1.85f`) per riempire la lente e farsi ritagliare in modo organico e cinematografico dal bordo circolare.
    - **Margini d'Aria nei Titoli Card (`TrophyRoomBadgeCard`)**:
      - Il contenitore del titolo nelle card della griglia deve includere tassativamente un `padding(horizontal = 8.dp)`, che combinato con il margine della card garantisce almeno `14.dp` di respiro su ciascun lato. Questo costringe titoli articolati (es. *"Tessera del Cineclub"*) ad andare a capo precocemente su due righe bilanciate, evitando sgradevoli contiguità con i bordi esterni della card.
    - **Integrità Semantica 1:1 delle Risorse SVG**:
      - Ogni badge del catalogo deve mappare in modo rigoroso e biunivoco una risorsa vettoriale dedicata (`CustomBadgeIconKind` ➔ file `ic_badge_*.xml`). È vietato riciclare la medesima icona (es. `ticket.svg`) come segnaposto condiviso per concetti differenti (*Signore delle Saghe*, *Déjà-Vu*, *Tessera del Cineclub*).
- **Indipendenza da Stringhe Localizzate nel Dominio (Anti-Localization Trap)**:
    - È severamente vietato verificare o dedurre stati di business logic (es. commento eliminato, stato di errore, tipologia entità) tramite il confronto di stringhe di testo tradotte o localizzate (es. `text.contains("[Comment deleted]") || text.contains("[Commento eliminato]")`).
    - Questa pratica fallisce sistematicamente non appena l'applicazione viene eseguita in una delle altre lingue supportate (15 lingue).
    - La Single Source of Truth dello stato deve poggiare unicamente su flag booleani strutturati (`isDeleted: Boolean`), identificatori tecnici o codici di stato enumerati. I testi localizzati appartengono rigorosamente al livello di presentazione UI tramite `stringResource`.
- **Mappatura Efficace Generi TV e Asimmetria TMDB (`GenreConstants.getEffectiveGenreIds`)**:
    - TMDB non prevede generi nativi TV per "Thriller" (ID 53), "Horror" (ID 27) o "Romance" (ID 10749), classificandoli sotto tassonomie aggregate come Crime (80) & Mistero (9648), Sci-Fi & Fantasy (10765) & Mistero, o Dramma (18) & Soap (10766).
    - Per evitare che il filtro per generi TV restituisca elenchi vuoti e per rispettare le aspettative dei cinefili, `TV_GENRES` include questi generi e `GenreConstants.getEffectiveGenreIds(genreId, isTv)` mappa ciascun genere nei rispettivi ID canonici TMDB.
    - Nelle query di esplorazione esterna (`DiscoverViewModel`), espandere gli ID alternativi con delimitatore pipe `|` (disgiunzione OR), mentre nei casi d'uso locali (`GetHomeUiStateUseCase`, `GetVistiUiStateUseCase`, `FolderDetailViewModel`) verificare l'appartenenza a uno qualsiasi degli ID equivalenti.
- **Filtro Paese di Origine e Localizzazione Dinamica (`CountryUtils`)**:
    - Il filtro per paese di origine (`originCountry: List<String>?`) interroga la colonna persistente `origin_country` in Room e il parametro TMDB `with_origin_country`.
    - I nomi dei paesi non devono mai essere memorizzati come stringhe hardcodate o tradotte manualmente in dizionari proprietari: devono essere risolti tramite `CountryUtils.getLocalizedCountryName(code, locale)` utilizzando `Locale.Builder().setRegion(code).build().getDisplayCountry(locale)`. In questo modo tutti i 200+ paesi del mondo sono istantaneamente e fedelmente localizzati nelle 15 lingue supportate dall'app senza overhead di memoria.

### Kotlin Coroutines, Smart-Casting & Null-Safety Invariants

- **Smart-Casting di Variabili Mutabili (`var`) in Coroutine e Closure**:
   - Quando una variabile mutabile locale (`var foo: String? = null`) viene popolata (es. all'interno di una transazione o ciclo) e deve essere usata in un blocco asincrono (`launch { ... }`, `async`, callback):
   - **MAI** affidarsi allo smart-cast di una `var` né forzare con asserzione `foo!!`. In Kotlin le `var` non mantengono lo smart-cast oltre i confini di closure concorrenti.
   - **Tassativo**: Assegnare la variabile a una costante locale immutabile (`val stableFoo = foo`) PRIMA del controllo `if (stableFoo != null)`. L'immutabilità della `val` garantisce lo smart-cast non-null in qualsiasi coroutine figlia senza type mismatch.

- **Guard Pattern per State Nullabili in Callback Compose Annidate**:
   - All'interno di event handler o callback annidate (es. `onItemSelected = { ... }`), non accedere a stati nullable del composition scope (es. `currentUser`) confidando su smart-cast esterni o `!!`.
   - Adottare tassativamente il guard pattern all'inizio della lambda:

     ```kotlin
     val user = currentUser ?: return@onItemSelected
     // 'user' è ora un riferimento non-nullable sicuro al 100%
     ```
- **Aggiornamento Room Destructive Migration**:
   - In Room 2.7+, `.fallbackToDestructiveMigration()` senza parametri è deprecato. Utilizzare tassativamente l'overload esplicito `.fallbackToDestructiveMigration(true)` per evitare warning e indicare esplicitamente il drop delle tabelle.

- **Ordine di Esecuzione nei Popup e Prevenzione Nullificazione Callback**:
   - `MovieActionsManager` gestisce popup galleggianti e resetta le sue proprietà (`onDeleteWatchSessionCallback = null`) durante `closeAll()`. Se un pulsante esegue `onDismiss()` prima di invocare il callback, o se la lambda legge lo stato a runtime dopo il dismiss, il callback viene annullato silenziosamente e l'azione non viene mai eseguita.
   - Nei popup contestuali e menu ad azione singola:
     1. Invocare sempre prima l'azione di business e poi la chiusura del popup: `onClick = { onAction(target); onDismiss() }`.
     2. Se un `Manager` pulisce i riferimenti su `closeAll()`, catturare il callback in una variabile locale immutabile (`val action = manager.actionCallback`) prima di costruire la lambda, garantendo che non venga letto come `null` a runtime.

- **Isolamento di `GlobalErrorHandler` e Divieto su Azioni Utente Intenzionali**:
   - `GlobalErrorHandler.emitError` mostra un toast galleggiante rosso/arancione ad alto impatto con pulsante `RIPROVA` sopra l'intera applicazione. Non deve mai essere attivato da transizioni di stato conseguenti a un'azione volontaria dell'utente (es. cancellazione di una cartella o film).
   - 1. Vietato usare `GlobalErrorHandler.emitError` per segnalare risorse mancanti dovute a cancellazioni locali dell'utente.
   - 2. Le schermate di dettaglio devono gestire l'assenza della risorsa localmente (con `UiState.Error` o tornando indietro) e resettare gli ID osservati prima della cancellazione (`_folderId.value = null`), senza allertare l'utente con finti messaggi di errore a livello globale.
