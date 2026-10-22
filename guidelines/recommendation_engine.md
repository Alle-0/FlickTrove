# FlickTrove Guidelines: Recommendation Engine & Taste Matrix

- **Motore di Raccomandazione & Taste Matrix Multi-Vettoriale (`CalculateMatchScoreUseCase`)**:
   - **Isolamento per Tipo di Media**: Il profilo utente (`UserGenreProfile`) deve essere costruito tassativamente filtrando per `mediaType` (`matching = if (type == "movie") favorites.filter { it.mediaType != "tv" } else ...`). Mai passare l'intero database mischiando Film e Serie TV, per evitare che anime o serie animate inquinino l'affinità dei film live-action.
   - **Massa Zero (Onboarding Fallback)**: Sotto la soglia di storicità minima (es. $<3$ titoli), il motore personalizzato deve restituire `null` innescando sempre un layer di fallback globale (es. Titoli Trending + Localizzazione) per stimolare i primi tap.
   - **Micro-Cluster di Generi (Zero-Allocation & Bit-Shifting)**: Il calcolo premia le sinergie di coppie di generi. Le chiavi composite nei dizionari (es. Genre A + Genre B) **non devono mai** usare interpolazione di stringhe in loop intensivi (`"${min}_${max}"`). Utilizzare tassativamente il bit-shifting su primitive a 64-bit: `(minId.toLong() shl 32) or (maxId.toLong() and 0xFFFFFFFFL)`. Questo previene GC jank.
   - **Decadimento Temporale (Recency Half-Life)**: Qualsiasi formula di decadimento (es. $0.5^{\Delta/240}$) deve essere esplicitamente arginata con limiti inferiori (`coerceIn` / `max`) per evitare asintoti a zero che distruggano preferenze consolidate negli anni.
   - **Segnali Repulsivi & Paradosso del "Guilty Pleasure"**: I titoli con voto personale $\le 5.0$ o `DROPPED` generano vettori repulsivi. Tuttavia, deve applicarsi la regola `Engagement > Rating`: se un genere rappresenta un volume di visione alto (es. $\ge 12\%$), la repulsione va azzerata (Guilty Pleasure). La mitigazione scala proporzionalmente scendendo all'8%.
   - **Damping Organico per Formati di Nicchia (TF-IDF)**: I formati stilistici distintivi (Animazione 16, Documentari 99, Musical 10402, Western 37, TV Movie 10770) subiscono un'attenuazione organica progressiva (da -14 a -35 punti) proporzionale alla frequenza di visione effettiva dell'utente (`freq < 0.04f` o `freq == 0f` con $\ge 6$ titoli visti). Se l'utente apprezza abitualmente il formato ($\ge 8\%$), non viene applicata alcuna penalità.
   - **Smorzamento Bayesiano della Qualità (IMDb Weighted Mean)**: Il voto grezzo TMDB viene corretto con prior mean globale ($C = 6.4$, $m = 45$) per abbattere outlier gonfiati con pochissimi voti.
   - **Bilanciamento Base Quality vs Taste Locale (Anti-Top 250)**: Il peso matematico del voto globale (Bayesian Mean TMDB) non deve mai schiacciare o sovrastare i punti dell'affinità personale. Assicurare che il tetto del Bayesian Score (es. 30/95) permetta alla Personal Genre Affinity (es. 50/95) di ribaltare le classifiche. Questo impedisce ai blockbuster generici di affossare le raccomandazioni cucite su misura.
   - **Divieto di Semi Totalmente Divergenti (`minByOrNull` overlap)**: Quando si estraggono i semi (Anchor) da inviare all'API `/recommendations` di TMDB, non forzare mai la rottura estrema della bolla (es. cercando il titolo nella top 20 dell'utente con *zero* generi in comune rispetto all'ancora). Questo fetcherebbe titoli estranei che inevitabilmente si schiantano contro i filtri di scoring del profilo. Utilizzare invece una selezione *random* su un pool ristretto di titoli altamente votati per garantire varietà sicura.
   - **Graceful Degradation & Divieto di "Full Fallback" Incondizionato**: Nelle pipeline di filtraggio dei match score (es. 70% $\rightarrow$ 55%), è tassativamente vietato concludere la catena con un fallback incondizionato (es. `take(10)` puro) per "salvare l'interfaccia vuota". Il motore deve imporre un pavimento di qualità minimo non negoziabile (es. $\ge 40\%$). Riempire la UI con "garbage" irrilevante distrugge la fiducia nell'algoritmo più di una lista vuota.

### Recommendation Engine & User Profile Invariants

- **Fallback Obbligatorio su `genreIds` nella Profilazione Utente**:
   - Nei film archiviati in locale o importati da servizi terzi (Room DB, Cinemaniac, Trakt, Sofa), la lista `genres: List<Genre>?` è spesso `null` e i metadati risiedono esclusivamente in `genreIds: List<Long>?`.
   - Qualsiasi algoritmo o UseCase che costruisce il profilo di affinità dell'utente (es. `CalculateMatchScoreUseCase.buildUserProfile`) deve tassativamente estrarre i generi tramite fallback polimorfico:

     ```kotlin
     val movieGenreIds = m.genres?.map { it.id.toLong() } ?: m.genreIds ?: emptyList()
     ```

     È severamente vietato ciclare unicamente su `m.genres`, poiché causa il silenzioso azzeramento del profilo per la quasi totalità dei film importati o salvati.
- **Anti-Format Dilution & Trappola del `maxOfOrNull` (Animazione vs Live-Action)**:
   - **Il Problema del `maxOfOrNull`**: Calcolare l'affinità generi prendendo unicamente il massimo (`currentMovieGenreIds.maxOfOrNull { profile.genreAffinities[id] }`) fa sì che un film con più tag (es. *Spider-Man: Un nuovo universo* con *Animazione (16)*, *Azione (28)* e *Sci-Fi (878)*) riceva il bonus massimo (+50%) solo grazie all'Azione, ignorando completamente che appartiene a un genere/formato che l'utente non ha mai visto.
   - **Penalità di Formato per l'Animazione (TMDB ID 16)**:
     - L'animazione non è solo un genere tematico, ma un medium visivo/formato.
     - Se la cronologia dell'utente (su una libreria consolidata $\ge 15$ titoli) contiene una percentuale trascurabile di animazione ($< 3\text{--}5\%$ o 0 titoli), il motore di raccomandazione deve applicare un forte damping/penalità sui titoli animati o escluderli dai caroselli principali.
     - Questo impedisce all'algoritmo di TMDB (che ha una fortissima co-occorrenza tra blockbuster e capolavori animati con voti stellari $\ge 8.2$) di saturare le raccomandazioni di utenti cinefili live-action.
