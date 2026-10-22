# FlickTrove Guidelines: CommsUni Protocol & Community

- **Mirroring Firestore e Identità CommsUni (`isOnCommsUni`)**:
   - Quando un commento viene pubblicato sulla rete federata CommsUni, una copia mirror viene salvata su Firestore in `/media_comments/{mediaId}/comments/{newComment.id}` per consentire alla dashboard admin (Report Engagement / Ultimi Movimenti) di tracciare l'attività dell'utente nei 7 giorni con data, autore e titolo.
   - **Invariante UI/Dominio**: All'interno dell'app, il commento deve essere riconosciuto tassativamente come federato (`isOnCommsUni = true`). Non degradarlo MAI a commento locale FlickTrove.
   - Al caricamento (`refreshComments`), la deduplicazione 1:1 `fc.id == cc.id` unisce il record mantenendo `isOnCommsUni = true`. In `remainingFirestore`, `isOnCommsUni` deve preservare il suo valore effettivo (`fc.isOnCommsUni`) e non essere mai forzato a `false`.
   - **Deserializzazione Booleani Kotlin/Firestore**: Tutti i campi booleani con prefisso `is` (es. `isOnCommsUni`, `isSpoiler`) nel modello dati Kotlin devono essere esplicitamente annotati con `@get:PropertyName("...")` e `@set:PropertyName("...")` per evitare che la convenzione JavaBean del serializzatore Firestore tronchi il prefisso alterando i nomi delle proprietà.

- **Allegati Multimediali CommsUni (No Markdown Injection / Native Attachments)**:
   - Su CommsUni e nelle app partner della rete federata (MovieParadise, WatchForge, CommsUni Web), le GIF e le immagini NON devono MAI essere inserite concatenando markdown testuale (`![gif](url)` o `![foto](url)`).
   - Gli allegati devono essere trasmessi tassativamente nel payload JSON tramite l'array strutturato `attachments: List<CommsUniWriteAttachment>` con `url`, `contentType` (`image/gif`, `image/webp`, `image/png`, ecc.) e `provider` lowercase (`giphy`, `tenor`, `flicktrove`).
   - Il campo `text` può essere vuoto/omesso se è presente almeno 1 attachment (post solo-GIF o solo-immagine). In tal caso, il modello di dominio (`AppComment.isEffectivelyDeleted`) deve preservare il commento verificando `attachedMedia.isEmpty()`, per evitare che un commento privo di testo venga considerato cancellato.

- **Traduzione Commenti On-Demand & Robustezza Endpoint GTX (Anti-Gibberish & Anti-429)**:
   - **Divieto Assoluto di Pre-Traduzione Massiva o da Viewport**: È tassativamente vietato tradurre automaticamente i commenti all'apertura della schermata o al loro ingresso nel viewport durante lo scroll. L'auto-traduzione trasforma lo slang cinefilo e i titoli di film in "gibberish", crea fastidiosi salti di layout (*layout shift*) e innesca a raffica richieste HTTP che portano al ban dell'IP per rate limit (HTTP 429).
   - **Controllo Utente On-Demand (Standard UX Social)**: La traduzione deve avvenire esclusivamente in seguito al tocco esplicito dell'utente su *"Traduci"* per quel singolo commento, con feedback istantaneo (~150 ms) e possibilità di tornare al testo originale.
   - **Invarianti Tecniche Parsing GTX, Preservazione Formattazione & Schermatura URL**:
     - Ciclare su tutti i frammenti dell'array `json[0]` concatenando `sentences.getJSONArray(i).getString(0)` per non troncare commenti multi-riga o lunghi.
     - Codifica URL rigorosa con `StandardCharsets.UTF_8.name()` per supportare emoji, newline e caratteri speciali nel parametro `q`.
     - Mappatura fine delle varianti per cinese (`zh-CN` / `zh-TW`) e portoghese (`pt-BR` / `pt`).
     - **Divieto Assoluto di `Html.fromHtml()` sul Testo Intero**: Il parser SAX di Android `Html.fromHtml(..., FROM_HTML_MODE_LEGACY)` interpreta il testo come documento HTML, comprimendo sistematicamente tutti i ritorni a capo `\n` e `\n\n` in un singolo spazio `' '` e distruggendo paragrafi e formattazione. La decodifica delle entità HTML residue (`&quot;`, `&#39;`, `&amp;`, `&lt;`, `&gt;`) deve essere eseguita puntualmente tramite regex (`unescapeHtmlEntities`), applicando `fromHtml` esclusivamente al singolo token `&...;` trovato, preservando al 100% newline, spaziatura e indentazione.
     - **Schermatura Preventiva degli URL (`shieldUrls` / `restoreUrls`)**: Prima di inoltrare il commento all'API di traduzione, tutti gli indirizzi web (`https?://...`) devono essere sostituiti con token neutri invarianti (es. `__FT_URL_0__`) e ripristinati identici al termine. Questo impedisce ai motori di traduzione di tradurre parole e slug contenuti nei percorsi dei link (es. trasformando parti di URL in parole tradotte), garantendo l'integrità dei link ipertestuali.

- **Protocollo Federato CommsUni & Rilevamento Intelligente Lingua (`detectCommentLanguage`)**:
   - All'invio di commenti o risposte (`CommsUniViewModel.postComment`), rilevare la lingua reale del testo tramite ML Kit on-device (`translationManager.detectCommentLanguage(cleanText)`, ~5 ms, 0 chiamate di rete).
   - Se il commento è lungo a sufficienza ($\ge 6$ caratteri) e scritto in una lingua diversa da quella del telefono (es. utente italiano che scrive una recensione in inglese), il commento viene correttamente etichettato con la lingua effettiva del testo (es. `"en"`, `"es"`, `"fr"`).
   - Se il testo è troppo breve o ambiguo (solo emoji, numeri, o "und"), applica il fallback trasparente sulla lingua del dispositivo (`Locale.getDefault().language`).
   - Valorizzare e preservare sempre `language` sia verso la rete CommsUni (`createComment`, `createReply`), sia nell'`AppComment` ottimistico, sia nel mirror Firestore e nei commenti salvati su Firestore locale (`CommentRepository.addCommentAndGetId`).

### CommsUni Protocol, Identity & Moderation Invariants

- **Profile Overlay & Pre-send Identity (`PUT /v1/authors/me/profile`)**:
   - CommsUni assegna di default a ogni `Actor-ID` una persona generata casuale con avatar geometrico.
   - Prima di consentire la pubblicazione o all'accesso alla sezione commenti, sincronizzare il profilo dell'utente autenticato (display name da Firebase/Google e avatar) tramite l'endpoint dedicato `PUT /v1/authors/me/profile`.
- **Fallback Graceful su Avatar Allowlist (`400 invalid_avatar_url`)**:
   - Il server CommsUni valida gli avatar tramite una regex di allowlist su CDN e formati ammessi.
   - Se l'avatar dell'utente si trova su un dominio non riconosciuto o un formato non valido e il server restituisce `400 invalid_avatar_url`, il client deve effettuare un fallback automatico ritentando la chiamata con `avatarUrl = null`. In questo modo il `displayName` reale viene comunque registrato e preservato, evitando il blocco totale del profilo utente.
- **Semantica del Filtro Sorgente (`?source=`)**:
   - Il parametro di query `source` sulle API di CommsUni deve seguire la seguente convenzione:
     - `null` (o assente): Mostra tutti i commenti della rete federata CommsUni.
     - `"flicktrove"`: Restringe la visualizzazione esclusivamente ai commenti generati all'interno dell'app FlickTrove.
   - Tutte le label e descrizioni del filtro sorgente devono essere allineate e tradotte simultaneamente in tutte le 15 lingue supportate dall'applicazione.
- **Moderazione e Single Source of Truth dei Commenti**:
   - FlickTrove non deve mantenere un database Firestore duplicato dei commenti creati: l'istanza CommsUni è l'unica Source of Truth del grafo di discussione.
   - La moderazione e la gestione dei contenuti si basano sui seguenti pilastri:
     - **Moderazione preventiva pre-send**: filtri client-side per parole d'odio, spam e pattern malevoli prima dell'invio.
     - **Cancellazione canonicamente delegata**: rimozione immediata con `DELETE /v1/comments/{id}` tramite l'Actor-ID autore.
     - **Segnalazione centralizzata**: `POST /v1/comments/{id}/reports` per violazioni della community.
     - **Blacklist locale**: memorizzazione locale leggera su DataStore/Room unicamente per gli ID autore bloccati dall'utente (`blockedUserIds`) e per gli utenti sospesi dal circuito FlickTrove.
   - **Procedura di Purge Account (Diritto all'Oblio GDPR & Minimo di Chiamate)**:
     - Quando l'utente elimina il profilo (`AuthViewModel.deleteAccount` / `deleteAccountWithReauth`), l'app esegue una bonifica totale senza lasciare record orfani:
       1. **CommsUni (Chunking Anti-429)**: Recupera gli ID commenti creati (`my_comment_ids_$uid`). Se presenti, li cancella a blocchi paralleli di massimo 4 richieste concorrenti (`chunked(4)` con micro-delay di 60ms se $>4$ commenti), rispettando il limite del connection pool di OkHttp (`maxRequestsPerHost = 5`) e azzerando il rischio di blocco per rate-limiting `HTTP 429 Too Many Requests`. Se l'utente non ha mai commentato, non viene effettuata alcuna chiamata HTTP.
       2. **Firestore (Singolo WriteBatch Atomico)**: Con 1 sola scrittura commit cancella `users/{uid}`, `usernames/{displayName}`, `commsuni_authors/{authorId}` e `user_social_notifications/{uid}` con i relativi item di notifica.
       3. **Pulizia Locale e Auth**: Rimuove le SharedPreferences locali dell'utente, elimina l'utenza Firebase Auth (`user.delete()`) e azzera il database Room locale con `movieRepository.clearAllData()`.
- **Dinamicità Totale dei Provider & Divieto Fallback Hardcoded (Zero Hardcoded Slugs)**:
   - Nessun provider o sorgente federata (es. `purplecast`, `tvtime`, o futuri circuiti di discussione) deve avere slug, label, colori, URL o asset grafici hardcodati in client code, mapper o ViewModel.
   - Il catalogo sorgenti deve essere interamente alimentato e interrogato dinamicamente a partire dall'endpoint `/v1/sources` di CommsUni, garantendo la totale indipendenza del client dall'aggiunta, rinomina o rimozione di provider terzi lato backend.
- **Pattern UX Moderazione Commenti (Anti-Affollamento Card, Blocco Integrato & Dismissal Standard)**:
   - **Anti-Affollamento Card**: È vietato sovraccaricare la action bar delle card dei commenti con pulsanti multipli di moderazione (es. pulsante separato di blocco utente). Sulla card a riposo è visibile esclusivamente la bandierina di segnalazione (`ic_flag`, con alpha discreto `0.5f`). L'azione *"Blocca utente"* risiede all'interno del modale di segnalazione (`CommentReportModal`), separata visivamente con un divisore ed evidenziata con `ic_block`.
   - **Sintesi Categorie di Segnalazione (Anti-Choice Overload)**: Le opzioni di segnalazione nel modale devono essere essenziali, scansionabili e raggruppate in un massimo di 4 macro-categorie chiare (`spoiler`, `abuse`, `spam`, `other`), evitando liste verticali dispersive.
   - **Dismissal Standard di Progetto**: I modali di selezione singola o azione informativa non devono mai presentare un pulsante "Annulla" esteso a tutta larghezza sul fondo (il layout bottoni inferiore è riservato esclusivamente ai dialoghi di conferma a due pulsanti orizzontali `Row` Annulla/Conferma come da Invariante 14). La chiusura avviene unicamente tramite `ModalCloseButton` (`ic_x`) nudo in alto a destra, tocco fuori dal contenitore o back gesture.
- **Architettura di Moderazione Duale (FlickTrove Nativo vs Rete Federata CommsUni)**:
   - **Canale CommsUni (🌐)**: I commenti pubblicati su CommsUni risiedono interamente sull'infrastruttura remota. Le segnalazioni vengono instradate alle API di CommsUni (`POST /v1/comments/{id}/reports`) e gestite dal relativo network federato.
   - **Canale FlickTrove Nativo (🔒)**: I commenti salvati unicamente su FlickTrove risiedono in Cloud Firestore (`media_comments/{mediaId}/comments/{commentId}`). Le segnalazioni generate dagli utenti vengono salvate nella collezione `comment_reports/{reportId}` con cooldown 24h e auto-flag spoiler a quota $\ge 3$.
   - **Pannello Admin di Moderazione**: La moderazione manuale dei contenuti nativi è presidiata dal pannello web proprietario `FlickTrove_Admin` (`/reports.html`), che consente all'amministratore di eliminare commenti, forzare lo stato spoiler, archiviare segnalazioni e applicare il ban dell'autore (`bannedFromCommenting`).

- **Identità Persona di Seeding & Divieto Bot Non Ufficiali (Mark & Giux Standard)**:
   - In FlickTrove le uniche identità bot/persona ufficialmente previste e consentite per il seeding di contenuti, commenti e interazioni simulate sono:
     - **Mark** (`fake_user_mark`): cinefilo colto, madrelingua inglese, tono analitico.
     - **Giux** (`fake_user_giux`): appassionato sanguigno ("tossico"), madrelingua italiano, tono diretto ed enfatico.
   - È tassativamente vietato introdurre, inventare o re-introdurre personas fittizie arbitrarie (es. "Elena", "Cinephile").
   - **Profilo Pubblico Credibile**: Su CommsUni (e in qualsiasi contesto pubblico o federato), il profilo degli attori di seeding deve essere sincronizzato via `PUT /v1/authors/me/profile` con `displayName` puliti e naturali (*"Mark"*, *"Giux"*) e avatar validi; l'ID tecnico `fake_user_*` è strettamente confinato a chiave interna e non deve mai trasparire nelle interazioni pubbliche.
- **Identità Ibrida e Federazione Azioni Utente (Multi-ID Matching & Propagazione Like)**:
   - Nei sistemi federati o ibridi (es. Firebase + CommsUni), l'identità dell'utente non è rappresentata da una singola chiave: coesistono il Firebase UID (`auth.currentUser?.uid`), il CommsUni Author ID (`currentAuthorId`) e l'Actor ID di sessione (`currentActorId`).
   - **Matching Universale**: I predicati di ownership o interazione (es. `isCommentLikedByMe`) devono verificare la presenza di uno qualsiasi degli ID validi associati all'utente corrente.
   - **Fusione Liste**: Nella sincronizzazione o merge differenziale di liste di interazione (es. `likedBy`), fondere sempre gli insiemi preservando l'univocità (`(listA + listB).distinct()`).
   - **Propagazione Azioni Federate**: È vietato applicare guardrail client-side basati sull'origine locale per bloccare le chiamate API (es. `if (originSlug != "flicktrove")`): qualsiasi interazione su un'entità federata (come il like a un commento esterno) deve sempre essere inviata all'endpoint remoto CommsUni (`POST /v1/comments/{id}/like`), garantendo la simmetria multipiattaforma.

### CommsUni & Community Invariants

- **Cancellazione Simmetrica Commenti (Locale & Remota)**:
   - Quando l'utente elimina un proprio commento, la cancellazione deve avvenire contemporaneamente sia su Firestore che sul backend CommsUni (`commsUniRepository.deleteComment(commentId)`), senza escludere i commenti con `isFlickTrove == true`.
   - L'endpoint `DELETE /v1/comments/{id}` esige l'header di autenticazione `X-TVTA-Actor-Id` generato via HMAC-SHA256 (`COMMSUNI_HMAC_SECRET`) a partire dal Firebase UID dell'utente autenticato; senza questo header o con actor errato, l'operazione fallisce con 400 (`actor_required`) o 404 (`not_found`).
