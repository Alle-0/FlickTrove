# FlickTrove Guidelines: UI & Jetpack Compose

## Regole Chiave da Anti-Feature Trap

- **Fallback Ricorsivo Personaggi e Filtro a Monte Titoli Vuoti (Modalità Avatar/Backdrop)**:
   - In modalità di selezione Avatar o Backdrop personalizzati (`AvatarSelectionModal`), escludere preventivamente dai risultati di ricerca i documentari (`genreIds.contains(99L)`), che tipicamente mancano di cast fittizio e costumi dedicati su TVDB.
   - Se TVDB non possiede costumi o personaggi per il titolo selezionato (`characterImagesMap.isEmpty()`), applicare sempre il fallback automatico sul ritratto del cast TMDB (`character.profilePath`), scartando gli elementi privi di immagini per non mostrare mai elenchi vuoti ("Nessun personaggio trovato").

- **Riconoscimento e Mappatura Certificazioni Internazionali (Singapore IMDA, TV & Age Ratings Multi-Country)**:
    - **Mappatura Semantica Fine (Fasce 12+, 14+, 15+, 16+, 17+, 18+)**: Non raggruppare arbitrariamente certificazioni di fasce d'età differenti sotto lo stesso ombrello di stringhe (es. mai forzare `16+` o `NC16` a `VM 14` o `R`, né omettere i rating TV nazionali come `VM12`). Mantenere categorie e risorse stringa dedicate su tutte le 15 lingue:
      - *Fascia 12+ / VM12* (`VM12` codice autoregolamentazione TV italiana, `12`, `12+`, `PG-13`, `PG12`, `FSK 12`, colore `#FF9800`): adotta `rating_vm12_label` (*"NON ADATTO SOTTO I 12"*) e descrizione `detail_rating_it_12` (*"Non adatto ai minori di 12 anni."*), mappato esplicitamente nella dialog legenda (`DetailRatingInfoDialog`) e nella Parents Guide (`DetailParentsGuide`).
      - *Divieto Legale 14* (`VM14` ministeriale italiano, colore `#FF7043`): adotta tassativamente il titolo dedicato `rating_vm14_label` (*"VIETATO AI MINORI DI 14"*) e descrizione `detail_rating_it_14` (*"Vietato ai minori di 14 anni"*), non degradandolo ad avviso consultivo.
      - *Parental Advisory 14+* (`TV-14`, `14`, `14+`, colore `#FF7043`): adotta `rating_tv14_label` (*"SCONSIGLIATO SOTTO I 14"*) e `rating_tv14_desc` (*"Questo programma contiene materiale che molti genitori..."*).
      - *Fascia 15+* (`rating_15_label` / `rating_15_desc`, colore `#FF5722`): `15`, `15+` (standard Corea del Sud KMRB, Regno Unito BBFC, Australia, ecc.).
      - *Fascia 16+* (`rating_16_label` / `rating_16_desc`, colore `#E64A19`): `16`, `16+`, `NC16`, `NC-16`, `VM16`, `FSK 16`.
      - *Fascia 17+ / R* (`rating_r_label` / `rating_r_desc`, colore `#F44336`): `R`, `17+`, `A`, `R15+`, `RESTRICTED` (in italiano "ACCESSO LIMITATO (17+)", mai etichettarlo "VM 14").
      - *Fascia Adulti / 18+* (`rating_nc17_label` / `rating_nc17_desc` o `detail_rating_it_18` per `VM18`, colore `#B71C1C`): `NC-17`, `18`, `18+`, `19`, `21+`, `VM18`, `FSK 18`, `M18`, `R21`.
    - **Architettura Single Source of Truth (`AgeRatingResolver`) & Zero-Allocation**:
      - Tutti i colori, titoli estesi, descrizioni e profili di severità della Parents Guide risiedono unicamente in `AgeRatingResolver.kt`.
      - Le verifiche non devono allocare liste dinamiche in memoria durante la ricomposizione; utilizzare set immutabili pre-allocati (`ALL_AGES_CERTS`, `GUIDANCE_CERTS`, `TEEN_12_CERTS`, ecc.) per lookup $O(1)$ a zero GC pressure.
      - `getCertificationColor` delega direttamente a `resolveHeaderRatingInfo(cert).color`, garantendo che l'header, la card espansa, la Parents Guide e la legenda (`DetailRatingInfoDialog`) condividano matematicamente la stessa identica palette cromatica.

### UI & Jetpack Compose Invariants

- **Card Composte & Atomicità Visiva**:
   - Nei componenti composti da più elementi (es. locandina + vassoio informazioni inferiore), qualsiasi stato di transizione, alpha o esplosione (`isExploding`, `cardAlpha`, `cardScale`) deve essere applicato sul **container genitore radice**, mai isolato a un solo sotto-componente.
   - Le dimensioni e coordinate trasmesse a menu popup contestuali e overlay di animazione (es. `MovieActionsManager`) devono coprire l'intera sagoma (`fullCardPosition`, `fullCardSize`).
   - Gli offset di tocco sui sotto-elementi inferiori devono essere corretti sommando l'altezza degli elementi che li precedono per garantire l'ancoraggio perfetto del popup.
- **Chiavi nelle Liste per `animateItem`**:
   - Nei `LazyRow` e `LazyColumn` che utilizzano `Modifier.animateItem(...)`, usare sempre `items(items = list, key = { it.id })` (o chiave composita univoca) anziché `items(count)`. Senza chiavi, le rimozioni non vengono rilevate correttamente e la lista non anima le uscite in modo fluido.
- **Pillole, Badge e Pulsanti Compatti (`CircleShape`)**:
   - Tutti i pulsanti a pillola (es. *"Scrivi"*, filtri rapidi, chip d'azione) e i badge devono adottare tassativamente `CircleShape` anziché `RoundedCornerShape` a raggio basso (es. `12.dp`), garantendo curvature perfette e uniformi a qualsiasi dimensione.
   - Quando un badge a pillola (`CircleShape`) è posizionato all'estremità di un container anch'esso a pillola, il padding verso il bordo curvo esterno deve coincidere con il padding verticale (es. `top = 3.dp, bottom = 3.dp` ➔ `end = 3.dp`). Evitare padding orizzontali fissi e simmetrici che creano spazi vuoti asimmetrici e spezzano la concentricità delle curve.
- **Gradienti di Fading su Backdrop & Caroselli**:
   - Le sfumature sovrapposte a immagini e backdrop devono seguire una progressione rigorosamente monotona da trasparente a scuro (`Color.Transparent` ➔ `cardBottomColor`). Mai introdurre tappe di colore intermedie chiare o desaturate che creano velature o aloni opachi.
- **Standard Badge di Certificazione & Rating**:
   - Tutti i badge di rating e certificazione età (header dettaglio film, dialog informativi, legende) devono adottare la sagoma a pillola (`CircleShape`) con padding compatto e testo proporzionato.
   - **Respiro Geometrico Capsule / Pill (`RatingPill`)**:
     - Quando una capsula arrotondata ad altezza fissa $H = 36\text{dp}$ (`CircleShape`, raggio dei semicerchi laterali $R = 18\text{dp}$) ospita un testo secondario o d'azione traslato verticalmente (es. `TOCCA PER ALTRO` a $y = 11\text{dp}$), la parete curva della capsula rientra verso l'interno di $\Delta x = R - \sqrt{R^2 - y^2} = 18 - \sqrt{18^2 - 11^2} \approx 3.75\text{dp}$.
     - È vietato utilizzare padding orizzontali compressi (es. `7.dp`) che lasciano una clearance visiva minima di soli $\approx 3.25\text{dp}$, soffocando il testo contro l'arco curvo.
     - Applicare tassativamente `padding(horizontal = 11.dp)` (o `12.dp`) e `minWidth = 72.dp`, garantendo oltre $7.2\text{dp}$ di respiro ottico effettivo sul perimetro curvo.
     - Nella colonna affiancata dei metadati (data, stagioni, status in `DetailHeader`), garantire una clearance simmetrica costante di $16\text{dp}$ dal raggio esterno di $28\text{dp}$ del container (`padding(end = 10.dp)` compensato con il padding perimetrale).
- **HazeGlass e Modali (No Native Dialogs & Global ModalHost)**:
   - Quando si utilizza `hazeGlass` o la libreria Haze per sfocare lo sfondo *dietro* a un modale/popup, **NON** utilizzare `androidx.compose.ui.window.Dialog`. I dialog nativi aprono una finestra separata, rompendo il contesto dell'`hazeState` e causando la sfocatura erronea del testo interno del dialog stesso (o sfondi solidi neri).
   - Implementare sempre questi modali come `Box` in overlay a tutto schermo (`Modifier.fillMaxSize()`) posti alla radice della schermata (es. attorno o in fondo allo `Scaffold`), così da condividere la stessa gerarchia di View per il calcolo corretto del blur sottostante.
   - **Pattern Global ModalHost (`LocalModalHost`)**:
     - I modali invocati dai tab figli (es. `MonthYearPickerDialog`, `FlickTroveDatePickerModal`, dialog di conferma) **non devono mai essere renderizzati inline all'interno del composable del tab**, perché risulterebbero bloccati al di sotto degli zIndex di `GlassyTopBar`, `GlassyBottomBar` e `MainSearchFab`.
     - Delegare sempre l'apertura del modale a `LocalModalHost.current.show { ... }`. Lo slot di montaggio risiede in [FlickTroveApp.kt](file:///c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/FlickTroveApp.kt) a `zIndex(250000f)`: in questo modo lo scrim nero del modale copre fisicamente e uniformemente il 100% dello schermo (incluse barre e floating buttons), disabilitando i tocchi sottostanti in modo naturale e senza richiedere micro-scrim `isDimmed` duplicati nei singoli componenti di navigazione.
   - **Isolamento dello Scope di Haze ed Evitamento del Self-Blur Ricorsivo**:
     - Un componente che adotta `hazeChild(state = X)` o `hazeGlass(state = X)` (es. `TopAppBar`, `CommentInputBar`, sticky headers) **non deve mai essere un discendente/figlio all'interno di un nodo con `Modifier.haze(state = X)`**.
     - Se un child si trova dentro il container catturato da Haze con lo stesso state, Haze cattura il rendering del child (testo, icone, tinta scura), lo sfoca nei frame successivi e lo ricattura, innescando un feedback loop esponenziale che oblitera l'elemento trasformandolo in una macchia nera sfocata e illeggibile.
     - **Regola Architetturale**: `Modifier.haze(localHazeState)` va applicato **esclusivamente** al layer sorgente sottostante (il container con backdrop e `LazyColumn` di scorrimento). Le barre flottanti (`TopAppBar`, barre di input) devono risiedere sempre come **sibling** al di fuori del nodo `haze(localHazeState)`, campionando lo sfondo senza mai venire incluse nella sua cattura. Lo stato globale (`globalHazeState`) rimane riservato ai modali a schermo intero montati al vertice della radice.
- **State Hoisting Precauzionale nei Refactoring**:
   - Prima di spostare un blocco UI fuori da un contenitore (es. spostare un `Canvas` fuori da un `Box`), verificare sempre se quel blocco consuma variabili di stato locali (`var state by remember { ... }`) definite all'interno del contenitore sorgente.
   - Se c'è dipendenza da uno stato, è tassativo eseguire prima lo *State Hoisting*, spostando la dichiarazione `remember` nel contenitore genitore comune (es. un `Row` esterno), prima di riposizionare l'elemento UI. Questo previene gli errori critici di compilazione come `Unresolved reference`.
- **Ordine Strutturale dei Modifier**:
   - In Compose, l'ordine di concatenazione dei modificatori non è stilistico, ma strutturale e sequenziale. Applicare `padding` prima di `clickable` riduce l'area di tocco; applicarlo dopo la espande. La regola generale è: `Modifier.size/fill` ➔ `background` (o grafica/clip) ➔ `clickable` (interazione) ➔ `padding` (margini interni).
   - **`bounceClick` su Bottoni e Container Geometrici**: Nei pulsanti con sagoma visibile (es. cerchi `CircleShape`, pillole o badge con background e border), applicare `bounceClick` **immediatamente dopo le dimensioni** e **prima** di `.clip()`, `.background()` e `.border()`. In questo modo l'intera sagoma geometrica, il contorno e lo sfondo scalano armoniosamente assieme durante il rimbalzo e ricevono il tocco su tutto il perimetro (es. `Modifier.size(34.dp).bounceClick { ... }.clip(CircleShape).background(...).border(...)`).
   - **Targeting del Click Modifier su Pillole Composte (Icona + Testo Interattivo)**:
     - Quando una pillola o badge composto contiene un'icona fissa e un'etichetta di stato o azione (es. `[ 🎛️ Chronological ]` in `CollectionDetailScreen`), non applicare `bounceClick` o click modifiers sull'intero `Box` genitore della pillola.
     - Mantenere la pillola genitore come container grafico passivo (`clip`, `hazeGlass`, `border`, `padding`) e applicare il modifier di interazione (`bounceClick` e tracking coordinate `onGloballyPositioned`) **esclusivamente sul `Text`** interno, preservando la geometria e la stabilità visiva dell'icona e del badge perimetrale.
- **Uso di `rememberSaveable` per stati effimeri ma critici**:
   - Se l'utente sta scrivendo una nota, ha un filtro aperto, o sta scrollando una lista profonda, usare `rememberSaveable` anziché un semplice `remember`. Questo garantisce che lo stato sopravviva alla Process Death (quando il sistema operativo chiude l'app in background per liberare RAM) e alle rotazioni dello schermo.
- **Fisica Elastica (Jelly Effect) e Collisioni (Anti-Clamping)**:
    - **Soft Boundary Penetration**: Per simulare materiali morbidi (gomma) contro i confini di un container, non "clippare" o bloccare rigidamente le coordinate dell'oggetto (`coerceIn`). Permettere sempre una leggera fuoriuscita calcolata con curve asintotiche (es. `maxPenetration * (1f - exp(-overshoot / factor))`) in modo che la resistenza aumenti ma permetta un piccolo rigonfiamento organico ("bulge").
    - **Isolamento Scala vs Spostamento**: Se l'elemento prevede un'espansione per feedback tattile (scale up alla pressione), i muri matematici di collisione (boundaries) **devono espandersi dinamicamente** di pari passo (es. `adjustedBoundary = boundary + extraHeight/2`). Altrimenti, il semplice ingrandirsi sul posto attiverà i calcoli di schiacciamento contro i bordi, costringendo l'elemento a spostarsi in modo asimmetrico o annullando la scala sull'asse interessato.
    - **Compressione Limite**: Lo schiacciamento fisico contro un bordo non deve mai appiattire l'oggetto a zero (effetto sottiletta). Garantire sempre una conservazione minima dell'altezza (es. 85-95% dell'originale); la percezione di sforzo si ottiene piuttosto con l'espansione orizzontale compensatoria.
- **Pattern Drag & Drop e Modali di Riordino (FlickTrove Standard)**:
    - **Container**: Utilizzare tassativamente `GlassmorphicModal` con `hazeGlass`, raggio `32.dp`, `dimBackground = true`, `dismissOnClickOutside = true` e altezza proporzionata (`heightIn(max = screenHeightDp * 0.76f)`).
    - **Header e Azioni**: Titolo `titleLarge` Bold, sottotitolo esplicativo `bodySmall` a 0.6f. A destra, pulsanti circolari compatti da `34.dp` con `bounceClick` esteso all'intero cerchio: Salva (`ic_tick`) e Chiudi (`ic_x`).
    - **Struttura Card**:
      - Drag Handle a inizio riga (`Icons.Rounded.DragHandle`).
      - Locandina / Thumbnail arrotondata a 8.dp.
      - Titolo `bodyMedium` SemiBold e metadati su `Modifier.weight(1f)`.
      - **Numerazione sequenza a destra**: indicatore ordinale (`#1`, `#2`, `#3`...) posizionato all'estremità destra (`labelLarge` Bold, `14.sp`, discreto a riposo e `primary` durante il trascinamento).
    - **Motore di Animazione a Slot**:
      - Tracciare `dragOffset` e calcolare `visualTargetIndex = (draggedIndex + (dragOffset / itemHeightPx).roundToInt()).coerceIn(...)`.
      - Spostare le altre card con `Animatable` e molla `Spring.StiffnessMediumLow` di `±itemHeightPx`.
      - Emettere tick aptico (`VibrationHelper.vibrateTick`) al cambio di slot e applicare lo swap effettivo della lista soltanto al termine del gesto (`onDragEnd`).
- **Pattern Accordion e Header Espandibili (Icone, Fisica e No Falsi Bagliori)**:
    - Nei container o header espandibili/collassabili (es. "Generi e temi suggeriti", accordi stagioni, sezioni crediti/filtri, `SettingsSection`):
      - **Feedback Tattile & No Rettangoli Ripple**: Applicare sempre `bounceClick` sul container cliccabile dell'header, evitando `Modifier.clickable` standard che genera rettangoli grigi o ripple estesi a tutta larghezza.
      - **Icona Standard di Progetto**: Usare tassativamente l'asset vettoriale dell'app `R.drawable.ic_right`, animato fluidamente con `animateFloatAsState` ($0^\circ$ verso destra da chiuso/collassato $\rightarrow$ $90^\circ$ verso il basso da aperto/espanso con durata sincronizzata a 360ms `FastOutSlowInEasing`). Mai importare o utilizzare icone generiche di Material come `KeyboardArrowDown` o `KeyboardArrowUp`.
      - **Fisica di Espansione & Divieto Spring su Altezze Variabili (Anti-Snap)**:
        - Non utilizzare `spring` per animare `expandVertically` e `shrinkVertically` su container con altezza variabile o elevata ($>300\text{ dp}$). La forza elastica ($F = -k \cdot \Delta x$) scala con la distanza, facendo scattare violentemente l'apertura nei primi millisecondi ("di botto").
        - Adottare tassativamente curve a durata fissa con decelerazione organica: `expandVertically(expandFrom = Alignment.Top, animationSpec = tween(360, easing = FastOutSlowInEasing))` abbinato a `fadeIn(tween(240))` per l'entrata, e `shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(280, easing = FastOutSlowInEasing))` abbinato a `fadeOut(tween(180))` per l'uscita.
      - **Divieto Bagliori e Riflessi Artificiali nel Vetro (No Fake Lights / Hot-Spots)**:
        - Vietato inserire gradienti radiali o bagliori di luce interna (`Brush.radialGradient` o velature colorate ad-hoc) all'apertura degli accordion. Tali effetti creano antiestetici punti focali luminosi ("hot spots") che rompono la coerenza del tema scuro. Preservare superfici pulite (`Color.White.copy(alpha = 0.04f)`) con bordi statici e traslucidi.
      - **Stabilità Tipografica nei Placeholder**: I testi di supporto e i placeholder (es. nella barra di ricerca) devono mantenere una dimensione font fissa e consistente (es. `14.sp`), senza salti dimensionali condizionali legati alla comparsa/scomparsa di pulsanti adiacenti.
- **Spotlight Effect nei Caroselli (Oscuramento Dinamico Card Non Principali)**:
    - Nei caroselli orizzontali `HorizontalPager` con peeking laterale (es. `HeroSpotlightCarousel`), applicare sempre l'effetto spotlight tramite `.drawWithContent` sull'intera sagoma della card:

      ```kotlin
      .drawWithContent {
          drawContent()
          val dimAlpha = (pageOffset * 0.58f).coerceIn(0f, 0.65f)
          if (dimAlpha > 0.005f) {
              drawRect(Color.Black.copy(alpha = dimAlpha))
          }
      }
      ```

    - Attenuare proporzionalmente anche l'alfa del bordo (`color = cardBottomColor.copy(alpha = (1f - pageOffset * 0.5f).coerceIn(0.35f, 1f))`). In questo modo l'attenzione dell'utente è guidata con decisione sull'elemento attivo, mentre le card laterali recedono nello sfondo in modo fluido e continuo a 60fps durante lo swipe.
    - **Integrità Visiva Card nei Caroselli Hero (No Inner Parallax Disalignment)**:
      - Nei caroselli `HorizontalPager` a card con peeking laterale (es. `HeroSpotlightCarousel`), evitare di applicare traslazioni di parallasse interno all'immagine (`AsyncImage`) rispetto al container.
      - Anche con fattori di scala protettivi (es. 1.18x–1.35x), il moto relativo espone le card laterali semivisibili a disallineamenti di crop o tagli antiestetici sui bordi curvi.
      - Preservare il riempimento nativo `fillMaxSize()` con `ContentScale.Crop`, delegando l'esperienza di profondità unicamente alla scala della card genitore e all'effetto Spotlight (`drawWithContent` con dimming progressivo a 60 FPS).
- **Standard Modali e Dialoghi (Raggio 32.dp & Layout Bottoni Concentrico)**:
    - **Raggio Unificato**: Tutti i modali di sistema, overlay e dialoghi (`GlassmorphicModal`, `MorphGlassModal`, `PeekABooCheckInDrawer`, `OnboardingDialog`, dialoghi impostazioni) devono adottare tassativamente `RoundedCornerShape(32.dp)`.
    - **Layout Bottoni Orizzontale (`Row`)**: I pulsanti di azione nei dialoghi (Annulla / Conferma) devono essere disposti rigorosamente su un'unica riga orizzontale (`Row` con Annulla a sinistra e Conferma a destra). Vietate pile verticali di pulsanti o pulsanti di annullamento posizionati in alto.
    - **Sagoma e Tipografia Bottoni**:
      - Sagoma a raggio `16.dp` (`RoundedCornerShape(16.dp)`) per garantire perfetta concentricità geometrica con il container a `32.dp`.
      - Testo con `maxLines = 1`, `overflow = TextOverflow.Ellipsis` e padding orizzontale di sicurezza per prevenire testi a capo nelle lingue a parole lunghe (es. tedesco, russo).
    - **Chevron nei `SettingsItem`**: Mostrare tassativamente la freccia `ic_right` su tutte le righe di impostazione interattive (`isClickable = true`).
    - **Separazione Netta Sfondo Glass vs Contenuto nei Modali (`FlickTroveModal` Standard)**:
      - Non applicare MAI `.hazeGlass()` direttamente sul container che racchiude testo, icone e controlli interattivi (specialmente con `useOffscreenStrategy = true`). Questa combinazione invia i controlli stessi nel buffer di sfocatura, rendendo l'intero modale illeggibile.
      - Adottare tassativamente `FlickTroveModal`, dove il vetro è reso su un `Box` fratello posteriore (`.matchParentSize().hazeGlass(...)`) mentre il contenuto risiede su un layer pulito non soggetto a sfocatura.
      - **Posizionamento Radice Fuori dall'Ambito `.haze()`**: I modali non devono mai essere annidati all'interno di un `Box` che funge da sorgente `.haze()`. Devono essere montati alla radice della schermata per evitare che Haze campioni il modale stesso in un loop di auto-blur.
      - **Sincronizzazione Dimmer per Top Bar e Bottom Bar (`LocalModalHostDimmed`)**: I modali aperti all'interno di schede e tab innestati (es. `HistoryTab`, `UpdatesScreen`) devono segnalare il proprio stato di apertura tramite `LocalModalHostDimmed.current.value = true`. Questo assicura che `GlassyTopBar` e `GlassyBottomBar` in `MainScreen` vengano opportunamente oscurate (`isDimmed = true`) e disattivate (`enabled = !isDimmed`) fino alla chiusura del modale.
    - **Pulsanti di Chiusura e Indietro Unificati (`ModalCloseButton` e `ModalBackButton`)**:
      - Utilizzare tassativamente `ModalCloseButton` e `ModalBackButton` (`com.cinetrack.ui.components.shared`) per qualsiasi modale, dialog, bottom sheet o overlay.
      - **Icone Nude Senza Cerchi**: È vietato applicare cerchi di sfondo opachi/scuri o bordi ad-hoc attorno all'icona di chiusura (`ic_x`) o alla freccia indietro (`ic_left`). I componenti adottano icone "nude" ad alpha proporzionato (`0.5f` per chiusura, `0.7f` per back), container touch target da `32.dp` concentrico con il bordo curvo del modale e micro-interazione `bounceClick`. Entrambi supportano sia `onClick` che l'alias semantico (`onClose` / `onBack`).
      - Quando il tasto Indietro è nascosto (es. al primo step di un wizard o modale a vista singola), utilizzare un segnaposto simmetrico `Spacer(Modifier.size(32.dp))` per mantenere il titolo perfettamente centrato rispetto al pulsante di chiusura a destra.
    - **No Doppio Padding in FlickTroveModal**: Poiché `FlickTroveModal` incapsula già il padding interno di sicurezza (`horizontal = 22.dp, vertical = 24.dp`), il `Column` radice passato nel content non deve mai aggiungere un ulteriore padding perimetrale (es. vietato `.padding(24.dp)`). Deve usare unicamente `Modifier.fillMaxWidth()`.
    - **Scrollbar su Griglie con Card Multimediali (Gutter Esterno Simmetrico)**:
      - Quando una griglia di immagini o artwork (es. `DetailCoverSelectionModal`) include una scrollbar (`premiumScrollbar`), è vietato posizionarla a filo o sovrapposta agli artwork ("in mezzo al cavolo").
      - Applicare sempre un `contentPadding` orizzontale simmetrico sulla griglia (es. `start = 8.dp, end = 8.dp`) e configurare `premiumScrollbar(gridState, width = 2.5f, paddingEnd = 2f, paddingVertical = 12f)`. In questo modo la barra di scorrimento alloggia perfettamente nello spazio di rispetto destro esterno alle card (clearance di 3-4dp) senza toccare le immagini, e la griglia rimane bilanciata e centrata. Abbinare a `verticalFadingEdges` per la dissolvenza in testa e in coda.
    - **Origine Morfica Modale Personalizzazione Profilo & Staticità Header**:
      - Quando `AccountScreen` consente l'apertura del modale di personalizzazione profilo (`EditProfileSubScreen`) tramite tocco su elementi di testata (Avatar, Nome Utente, Backdrop o spaziatore superiore), il modale `MorphGlassModal` deve ancorarsi come punto di espansione (`triggerBounds`) tassativamente alle coordinate della matita (`ic_pencil`) della top bar.
      - Gli elementi di contenuto dell'header (avatar, pillola del nome, backdrop) non devono mai adottare `bounceClick`, ripple o modificatori di deformazione visiva al tocco: mantengono la loro integrità geometrica statica tramite `clickable(indication = null, interactionSource = remember { MutableInteractionSource() })`.
- **Pattern Modali di Filtro (Selezione Multipla Universale & Simmetria 1:1)**:
    - **Divieto di Modali di Filtro Ad-Hoc (Single Source of Truth UI)**:
      - È vietato creare dialoghi o file modali di filtro duplicati per singole schermate (es. vietato creare `CollectionFilterModal`, `CommentsSortDialog`, ecc.).
      - Utilizzare ed estendere sempre l'infrastruttura standard unificata `HomeFilterModal` (`com.cinetrack.ui.components.dialog.HomeFilterModal`), configurata tramite flag semantici (`isCollectionFilter`, `isCommentsFilter`, `hasTvSeries`) e modellata su `SortConfig`.
      - In questo modo l'intera app condivide le stesse curve fisiche, l'espansione morphing da coordinate d'origine (`triggerBounds`), il sistema di ghost measurement per il dynamic height e lo stile `hazeGlass` unificato.
    - **Selezione Multipla Obbligatoria su Qualsiasi Modale (`Set<T>`)**:
      - In qualsiasi modale o pannello di filtro dell'app (Home, Esplora, Sala dei Trofei/Badge, Libreria), ogni filtro sfaccettato o categorico (Generi, Emotional Vibes, Provider, Status, Formati Pellicola/Tier, Categorie) **deve essere tassativamente modellato come `Set<T>`**, mai con campi scalari singoli (`String` o `T?`).
    - **Convenzione di Stato & Gestione "TUTTI"**:
      - **Insieme Vuoto = TUTTI**: Lo stato iniziale e di reset è `emptySet()`, che rappresenta semanticamente l'assenza di restrizioni (accetta tutti gli elementi).
      - **Comportamento Chip "TUTTI"**: È visivamente selezionato se e solo se `set.isEmpty()`. Cliccando su "TUTTI" si azzera la selezione (`copy(facet = emptySet())`).
      - **Toggle sui Chip Specifici**: Cliccando su un elemento attivo lo si rimuove (`set - item`); cliccando su uno inattivo lo si aggiunge (`set + item`).
      - **Predicato di Filtraggio (Disgiunzione OR)**: La logica di filtro deve verificare:

        ```kotlin
        val matchesFacet = config.facetSet.isEmpty() || item.property in config.facetSet
        // oppure per logiche complesse: config.facetSet.any { facet -> matches(item, facet) }
        ```

    - **Header Bar Standard (1:1 con `HomeFilterModal`)**:
      - Titolo: `headlineSmall`, `FontWeight.Black`, `letterSpacing = 3.sp`, `padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 28.dp)`.
      - Pulsante Reset: Pillola a raggio `24.dp` con `ic_ricarica` + testo `RESET`, sfondo e bordo glassmorphic `primary` con `bounceClick`.
      - Pulsante Chiudi: `Box(24.dp)` con icona `ic_x` ad alpha `0.5f` (mai cerchi scuri o sfondi opachi ad-hoc).
    - **Badge Contatore su Sezioni**: Tutte le sezioni espandibili (`ExpandableSection`) devono collegare il contatore attivo alla dimensione reale del set: `badgeCount = localConfig.selectedSet.size` (es. `CATEGORIA 2`, `FORMATO PELLICOLA 3`).
    - **Pulsante Applica**: `RoundedCornerShape(16.dp)`, altezza `56.dp`, tipografia `titleSmall` ExtraBold, `letterSpacing = 2.sp`.
    - **Fluidità Animazioni Espansione/Chiusura & Chip Toggle Unificato**:
      - Le sezioni a fisarmonica (`ExpandableSection`) non devono mai usare salti di altezza o `scaleIn` senza espansione verticale: adottano tassativamente `expandVertically(expandFrom = Alignment.Top)` e `shrinkVertically(shrinkTowards = Alignment.Top)` con fisica `spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)` per srotolarsi morbidamente verso il basso e ripiegarsi verso l'alto a 60 FPS coordinati con la freccia.
      - Nelle sezioni con pulsante di toggle interno (es. Generi e Paesi con *Mostra tutto* / *Mostra meno*), il flusso di chip è gestito tramite `AnimatedContent` con `SizeTransform(clip = true)` per animare la variazione di altezza con clipping perimetrale pulito. I pulsanti di toggle condividono il componente riusabile `ShowMoreLessToggleChip` per evitare duplicazioni di codice.
    - **Dati Serie TV nel Tasto d'Azione Centrale (`DetailActions`)**:
      - Quando una serie TV si trova nello stato iniziale `WatchState.NONE` (o `RICORDAMELO`), il pulsante centrale include un sottotitolo con il numero totale di episodi e la durata media stimata (es. `24 EPISODI • ~45 MIN`), con fallback su singole metriche se parziali.
      - Il calcolo dei metadati è memoizzato tramite `remember(movie.id, ...)` prima dell'`AnimatedContent`, per azzerare ricalcoli e traversali di liste ad ogni recomposition del progress bar animato.
      - La dimensione del font del titolo adotta `12.sp` sia prima che dopo il tap verso `WatchState.BOOKMARKED` (*SELEZIONA EPISODI* / *0% COMPLETATO*), azzerando qualsiasi salto dimensionale (*layout shift*).
- **Transizioni Schermo Voyager & No Predictive Scale su Pagine Figlie**:
    - Lo stack di navigazione di FlickTrove (basato su Voyager) gestisce centralmente le transizioni in `FlickTroveApp.kt`: le schermate dettaglio entrano dal basso con `slideInVertically` ed escono verso il basso con `slideOutVertically`.
    - Evitare di inserire trasformazioni grafiche di scale o translation legate a `PredictiveBackHandler` direttamente sul container radice delle schermate secondarie (es. `MovieDetailScreen`), poiché confliggono con il ciclo di vita e con lo `slideOutVertically` di Voyager, causando comportamenti a scatti o animazioni doppie al pop dello stack.
    - Delegare sempre le transizioni di ingresso e uscita a `ScreenTransition` in `FlickTroveApp.kt`.
- **Concentricità e Margini nei Container Arrotondati (`RoundedCornerShape(32.dp)`)**:
    - Nei container, dialoghi o header a vetro che adottano curve ampie (es. `RoundedCornerShape(32.dp)` o `RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)`), gli elementi interni terminali (pulsanti circolari `CircleShape`, selettori a pillola, pulsanti di azione o di chiusura) non devono mai avere padding asimmetrici o compressi (es. `8.dp`, `10.dp`, o `vertical = 20.dp` con `horizontal = 16.dp`).
    - **Regola di Concentricità 16/16**: Il padding lungo il perimetro curvo deve eguagliare il padding orizzontale (`padding(16.dp)` su tutti i lati, o `padding(horizontal = 16.dp, bottom = 16.dp)`). In questo modo il centro geometrico dell'elemento terminale a raggio `16.dp` o circolare da `32.dp` si allinea perfettamente con il centro di curvatura del raggio a `32.dp` del container, garantendo una clearance diagonale e perimetrale costante e uniforme di **`16.dp`** ($32\text{ dp} - 16\text{ dp} = 16\text{ dp}$) che evita angoli schiacciati o "strozzati".
    - **Card e Accordion Annidati (Sub-Cards)**: Qualsiasi card o pannello collassabile interno (es. *Credits & Attributions*) posizionato all'interno di un container a $32\text{ dp}$ deve rispettare la clearance geometrica uniforme $P = 16\text{ dp}$ su tutti i lati (compensando con `padding(horizontal = 4.dp)` se il genitore ha padding $12\text{ dp}$) e adottare un raggio di curvatura rigorosamente concentrico:
      $$R_{\text{interno}} = R_{\text{esterno}} - P = 32\text{ dp} - 16\text{ dp} = 16\text{ dp}$$
      Vietato applicare raggi sproporzionati (es. $20\text{ dp}$) che riducono asimmetricamente la distanza negli angoli diagonali.
    - **Pulsante di Chiusura Concentrico**: Nei modali a 32.dp, il pulsante di chiusura unificato `ModalCloseButton` in alto a destra deve adottare `size(32.dp)` (raggio 16.dp) con padding `top = 16.dp, end = 16.dp`, così da condividere il medesimo centro d'arco con il perimetro esterno del container a `32.dp`.
    - **Divieto Offset Artificiali**: Evitare l'uso di `.offset(y = -10.dp)` combinato con `.padding(top = 10.dp)` sui container `hazeGlass`: tali hack spingono l'intero contenuto verso il fondo del vetro, rompendo la simmetria verticale. Usare sempre `statusBarsPadding()` / `displayCutoutPadding()` sulla `Column` interna con padding espliciti e bilanciati.
    - **Divieto Vincoli Minimi Arbitrari e Buffer Fittizi (`minAllowedHeight`)**: Nei modali con altezza dinamica o espansione animata (es. `SurpriseMeOverlay`), è vietato imporre valori minimi di altezza arbitrari (es. `minAllowedHeight = 260.dp`) o sommare buffer fittizi di pixel (`+ 16.dp` a `targetHeightPx`). Tali vincoli forzano il contenitore ad allungarsi oltre il necessario, generando baratri asimmetrici di spazio vuoto sul fondo e distruggendo la simmetria del padding (`padding(20.dp)`). L'altezza animata deve tracciare con fedeltà millimetrica l'altezza reale del contenuto misurato.
    - **Menu Popup Contestuali e Voci Terminali (Concentricità 24.dp / Anti-Pinch Inferiore)**:
      - Nei menu contestuali a comparsa o popup ancorati (es. menu a tre puntini in `MovieCard` con `RoundedCornerShape(24.dp)`):
      - Il padding inferiore del container non deve mai essere simmetrico o compresso (es. `6.dp` o `8.dp`), altrimenti l'ultima voce d'azione tocca o collide diagonalmente con il raggio curvo di 24.dp.
      - Applicare sempre una clearance maggiorata verso il fondo (`padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)`) e far rientrare eventuali divisori orizzontali (`padding(horizontal = 6.dp)`), preservando una clearance diagonale costante di 14-16dp.
    - **Clearance e Padding Inferiore per Griglie in Modali a 32.dp**:
      - Quando un modale arrotondato a `32.dp` racchiude una griglia scorrevole (`LazyVerticalGrid`), le card terminali devono adottare `RoundedCornerShape(16.dp)` con badge concentrici a `8.dp` ($16 - 8 = 8\text{ dp}$).
      - Per prevenire angoli strozzati o contatti visivi con la curva del container, garantire una clearance complessiva dal fondo $\ge 32\text{ dp}$ (combinando `contentPadding = PaddingValues(bottom = 12.dp)`, `Spacer(Modifier.height(8.dp))` e il padding verticale nativo del modale).
- **Pattern Indicatore Pager Elastico (`SymbiontPagerIndicator`)**:
    - **Componente Unificato Obbligatorio**: Utilizzare tassativamente `SymbiontPagerIndicator` da `com.cinetrack.ui.components.shared`. Vietato duplicare logiche di disegno Canvas, barre `Row` con `animateDpAsState` inline o formule di rimbalzo ad-hoc nei singoli screen.
    - **Fisica Integrata nel Movimento (Anti-Pop)**:
      - Mai lanciare allungamenti o rimbalzi dopo che la pagina si è fermata (`isScrollInProgress == false`), per prevenire la sgradevole pausa "si ferma e poi si allunga".
      - La deformazione d'atterraggio deve essere integrata nella frazione finale del moto ($0.70 \rightarrow 1.00$), proporzionale alla velocità di scorrimento (`currentSpeedPagesPerSec`), azzerandosi nei movimenti lenti o manuali controllati.
      - Altezza del worm rigorosamente fissa a `dotSize` (`6.dp`), eliminando qualsiasi appiattimento verticale che scopre i pallini sottostanti.
    - **Semantica Wizard vs Carosello**:
      - *Caroselli continui (es. Home Hero)*: `persistPreviousDots = false`, solo il worm attivo è colorato.
      - *Wizard / Onboarding / Check-in*: `persistPreviousDots = true`, tutti i pallini dei passi già completati (`i < currentPage`) rimangono accesi in `accentColor`, mentre i futuri restano grigi (`alpha = 0.2f`).
    - **Supporto Duale**: Utilizzare l'overload con `PagerState` per i caroselli continui a 60fps con drag touch, e l'overload con `currentPage: Int` (a doppia molla asincrona reattiva testa/coda) per wizard a step discreti.
- **Micro-Interazioni di Conferma & Feedback Visivo di Salvataggio (Checkmark Morph & Radial Sparkles)**:
    - Nelle azioni di salvataggio o check-in ad alto valore (es. `PeekABooCheckInDrawer`), non limitarsi a un dismiss immediato e secco del modale.
    - Implementare il pattern standard di conferma visiva:
      1. Morphing del pulsante da gradiente neutro/accent a verde smeraldo (`#10B981` ➔ `#059669`).
      2. Comparsa animata della spunta di successo (`ic_tick`) con `AnimatedContent` e molla elastica.
      3. Esplosione radiale di micro-particelle (`SaveBurstParticle` con traiettorie angolari casuali e sfumatura d'alfa) e shockwave circolare ad anello espandibile (`drawCircle`).
      4. Impulso tattile aptico (`VibrationHelper.vibrateClick`) e dismiss ritardato a 450-480ms per consentire all'utente di percepire la conferma prima della transizione dello schermo.
- **Standard Switch e Toggle (`FlickTroveSwitch`) & Divieto Material 3 Switch**:
    - **Divieto Switch Nativo**: È tassativamente vietato utilizzare `androidx.compose.material3.Switch` nell'app. Qualsiasi interruttore o opzione binaria deve adottare esclusivamente `FlickTroveSwitch` (`com.cinetrack.ui.components.common.FlickTroveSwitch`).
    - **Micro-interazioni Obbligatorie**: `FlickTroveSwitch` include tassativamente:
      1. *Spring Physics*: Molla fluida `Spring.StiffnessMedium` con damping ratio per movimento organico.
      2. *Bagliore Reattivo (Glow)*: Halo con `accentColor` posteriore al thumb quando attivo.
      3. *Elastic Press*: Alla pressione prolungata prima del rilascio (`collectIsPressedAsState()`), il thumb si allunga orizzontalmente (da `24.dp` a `28.dp` verso l'interno) simulando la fisica gommosa dei toggle iOS / Material You.
      4. *Hover State & Pointer Icon*: Al passaggio del cursore (`collectIsHoveredAsState()`), attivare l'alone concentrico traslucido attorno al thumb, intensificare il bordo del tracciato e applicare `.pointerHoverIcon(PointerIcon.Hand)` per mouse e trackpad.
      5. *Feedback Aptico*: Emettere sempre `VibrationHelper.vibrateClick(context)` al cambio di stato.
- **Curvatura e Concentricità Geometrica delle Card di Lista (24.dp / 12.dp)**:
    - I box o card orizzontali che compongono elenchi e classifiche (es. `BoxOfficeMovieRow`, card di riepilogo) devono adottare tassativamente `RoundedCornerShape(24.dp)` anziché raggi spigolosi o bassi (es. `16.dp`), armonizzandosi con la morbidezza visiva dell'app.
    - Le locandine o miniature interne (es. $48 \times 70\text{ dp}$ immerse in un padding di $10\text{ dp}$) devono adottare un raggio di **`12.dp`** (`RoundedCornerShape(12.dp)`). In questo modo viene rigorosamente rispettata la regola della concentricità geometrica: $24\text{ dp} - 10\text{ dp} \approx 12\text{ dp}-14\text{ dp}$, garantendo curve parallele e clearance impeccabile.
- **Scannabilità e Anti-Overdesign nei Widget di Riepilogo**:
    - Nelle card dashboard e nei widget di statistica rapida (es. `GeneralStatsCard`), preservare la purezza della griglia simmetrica $2 \times 2$ basata sulla gerarchia immediata `Label` (subtle) + `Valore` (bold).
    - Evitare il sovraccarico visivo: non inserire micro-icone ridondanti accanto a testi già auto-esplicativi e non incastrare mini-barre o grafici bicolore in spazi ridotti che rompono il baricentro verticale della griglia. Se una metrica è una proporzione, esprimerla come rapporto testuale pulito (es. `71% / 29%`).
- **Standard Banner Informativo nei Modali di Import/Migrazione**:
    - Qualsiasi finestra di dialogo che avvia un'operazione di importazione dati o ripristino (`BackupDialog`, `ExternalMigrationDialog`) deve esporre prima dei tasti d'azione il banner standard di avviso:
      - Sfondo `MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)` a raggio `12.dp`.
      - Icona `Icons.Rounded.Info` color `primary` da `18.dp`.
      - Stringa localizzata `settings_import_tip_sync`.
- **Raggruppamento e Spaziatura nelle Sezioni Impostazioni Dense (Anti-Divider & Raggruppamento Tipografico)**:
    - Nei container a fisarmonica o card impostazioni che ospitano molteplici azioni eterogenee (es. `SettingsAccountSection` con Sicurezza & Dati/Pericolo, o `SettingsSyncBackupSection` con Cloud Tracking e Backup Locale):
      - **Divieto Linee Divisorie su Superfici Glassmorphic**: Non frammentare la superficie continua di `hazeGlass` con linee orizzontali sottili (`HorizontalDivider`). I tagli visivi artificiali sporcano il vetro e interrompono la fluidità estetica della card.
      - **Sottogruppi Tipografici e Spazio Negativo**: Suddividere le sezioni dense tramite micro-header semantici maiuscoli (`labelSmall` Bold con `letterSpacing = 1.2.sp` e colore d'accento o di pericolo `primary.copy(alpha = 0.85f)` / `Color(0xFFFF5252).copy(alpha = 0.85f)`), distanziati con spazio negativo organico (`Spacer(14.dp)`).
      - **Condizionalità delle Voci di Allarme/Scarto**: Non mostrare voci con icone rosse di avvertimento o alert permanenti se il contatore degli elementi scartati o problematici è pari a zero (`unmatchedCount == 0`). Mostrare la riga d'azione unicamente in presenza di elementi reali (`if (unmatchedCount > 0)`).
      - **Simmetria di Gerarchia (Titolo + Descrizione)**: Tutte le opzioni interattive di una lista o gruppo devono mantenere un pattern simmetrico a due livelli (Titolo chiaro + Descrizione sintetica su 1 riga), evitando titoli nudi isolati o testi descrittivi prolissi a più paragrafi che rompono la scansione visiva verticale.
- **Divieto di Modificatori `.layout { bottomTrim }` su Item in `LazyColumn`**:
    - È vietato inserire modificatori di layout che alterano o sottraggono artificialmente altezza misurata rispetto a quella disegnata (es. `.layout { measurable, constraints -> val bottomTrim = ...; layout(placeable.width, placeable.height - bottomTrim) { placeable.placeRelative(0, 0) } }`) all'interno di elementi di liste a scorrimento verticale (`LazyColumn`).
    - Questa pratica rompe il ritmo verticale gestito da `Arrangement.spacedBy`, soffoca gli elementi terminali (come indicatori di pagina a pallini o badge) contro le sezioni successive e crea collisioni visive.
    - Se una sezione (es. carosello hero) occupa troppo spazio verticale, ridurre proporzionalmente l'altezza della card (`460.dp` anziché `500.dp`) garantendo contemporaneamente il corretto respiro a monte e a valle dei pallini (`padding(top = 16.dp, bottom = 4.dp)` con `indicatorHeight = 12.dp`).
- **Personalizzazione e Attribuzione SDK Esterni (Standard GIPHY Dialog)**:
    - **Attribuzione Inline**: Posizionare l'asset ufficiale (`powered_by_giphy`) all'interno della barra di ricerca a destra del campo di testo (`gph_search_bar.xml`), mantenendo una clearance diagonale di sicurezza di `16dp` dalla curva destra della capsula quando il pulsante clear è nascosto (`app:layout_goneMarginEnd="16dp"`).
    - **Pill Shape Senza Sovraccarico**: Sagomare la barra di ricerca tramite `ViewOutlineProvider` (`setRoundRect` a metà altezza con `clipToOutline = true`) e `GradientDrawable` a capsula (`cornerRadius = 1000f`), con padding simmetrico per le icone di testa e di coda (`12dp`).
    - **Arrotondamento GIF**: Allineare le GIF al raggio delle card di FlickTrove (`14dp` anziché il default di `4dp`) impostando `GifView.CORNER_RADIUS` all'avvio in `MainActivity` e applicando `clipToOutline = true` con raggio `14dp` sui singoli elementi agganciati alla lista (`OnChildAttachStateChangeListener`).
- **Morfismo Modale per Sotto-Schermate & Divieto Modali Annidati a Cascata**:
    - **Divieto Modali Sovrapposti**: Quando un modale presenta azioni che conducono a flussi secondari o schermate di selezione (es. Modifica Profilo ➔ Scelta Avatar / Cambio Nome), **è severamente vietato aprire un secondo `Dialog`, `Modal` o overlay sopra al primo**. L'apertura a cascata rompe il render context di Haze (sfondi neri o blur incorretto) e disorienta l'utente.
    - **Container Unico Dinamico (`MorphGlassModal`)**:
      - La navigazione tra la schermata principale e i sottomenu deve avvenire all'interno dello **stesso container** `MorphGlassModal`.
      - Le dimensioni del modale (larghezza `targetWidthFraction`/`targetMaxWidth` e altezza `fixedModalHeightFraction`) devono adattarsi fluidamente tra gli stati tramite molle fisiche `spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)`.
    - **Transizioni Interne & Back Navigation**:
      - Il passaggio tra schermate interne avviene tramite `AnimatedContent` con slide orizzontale e dissolvenza incrociata (`slideInHorizontally + fadeIn` ➔ `slideOutHorizontally + fadeOut`).
      - Intercettare sempre il pulsante indietro con `BackHandler(enabled = currentSubScreen != Root)` per ritornare alla vista principale del modale prima di chiuderlo.
    - **Indicatori di Navigazione**: Ogni riga o opzione che apre un sottomenu deve mostrare all'estremità destra la freccia di navigazione `ic_right`.
- **Card Asimmetriche su Backdrop e Scrim Tri-Assiale (`TrovePickCard` Standard)**:
    - **Divieto Duplicazione Titolo (Poster vs Logo)**: Nelle card asimmetriche che presentano una locandina/poster verticale a sinistra (es. `TrovePickCard`), non inserire a destra loghi o scritte che ripetono il medesimo titolo già stampato sul poster. Riservare la colonna destra per badge identificativo e metadati puliti.
    - **Integrità Visiva del Backdrop (No Black Holes & No Alpha Dropping)**:
      - Non abbassare l'alpha del backdrop (`alpha = 0.35f`) né applicare overlay opachi al 90-97%: distruggono la fotografia del film trasformandola in una scatola nera anonima. Il backdrop deve rimanere al 100% di opacità.
      - La leggibilità si ottiene unicamente tramite una terna di gradienti calibrati:
        1. *Orizzontale (sinistra ➔ destra)*: nero all'85% dietro al poster che sfuma verso la trasparenza a destra per valorizzare la scena cinematografica.
        2. *Verticale (basso ➔ alto)*: progressivo e profondo (90-98% sul fondo) per dare contrasto netto alle pillole di metadati e punteggio.
        3. *Top Vignette (alto ➔ basso)*: leggera sfumatura nei primi 18% (55% ➔ 0%) per dare leggibilità al badge di sezione superiore (`CURATED FOR YOU`) senza spegnere il cielo/sfondo.
    - **Container Dark Neutro (No Monocromatismi Forzati)**: Evitare di usare il colore d'accento estratto per tingere lo sfondo dell'intero contenitore. Il container principale deve rimanere dark neutro (`Color(0xFF0E0F14)`), lasciando che l'accento viva nei badge, nelle pillole e nell'Ambilight glow posteriore.
- **Android Glance & RemoteViews Spacing (Divieto Padding Margin Hack)**:
    - In Android Glance (widget), non concatenare modificatori `.padding(...)` prima di `.background(...)` nel tentativo di simulare margini esterni: Glance fonde tutti i padding nel `setPadding` nativo della View, causando pillole anormalmente alte, deformate e asimmetriche.
    - Spaziare sempre gli elementi RemoteViews tramite `Spacer(GlanceModifier.height/width(...))` espliciti posti al di fuori del container con sfondo.
- **Standard Skeleton (Monocromatismo Neutro & Dock Unificato)**:
    - **Divieto Colori d'Accento nei Placeholder**: Gli stati di caricamento skeleton (es. `DetailSkeleton`, `HomeFeedSkeletons`) non devono **MAI** includere tinte o sfumature di colore d'accento (es. pillole rating gialle, percentuali verdi, sezioni arancioni). I placeholder colorati appaiono caotici e rompono l'astrazione dello scheletro. Usare esclusivamente **monocromatismo neutro** (`Color.White.copy(alpha = 0.05f .. 0.22f)` su dark glass `Color(0xFF14151E)`).
    - **Dock Inferiore a Pillola Unificata**: Nelle schermate con dock fluttuante inferiore (es. dettaglio film), lo scheletro della barra di controllo deve essere un unico container a pillola unificato a piena larghezza (`fillMaxWidth()`, padding `24.dp`, altezza `56.dp`, sagoma `RoundedCornerShape(28.dp)`), evitando la frammentazione in pillole e cerchi isolati.
    - **Divieto Progressive Loading Ibrido (No "Frankenstein" Skeleton)**: È severamente vietato renderizzare contenuto reale (immagini vere tramite `AsyncImage`, testi reali del titolo) all'interno di uno stato skeleton, anche se i dati preloaded sono disponibili come parametri (es. `preloadedTitle`, `preloadedBackdropPath`). Mostrare contenuto reale accanto a blocchi shimmer grigi crea un effetto visivo incoerente e perturbante ("Frankenstein"). Le **uniche due alternative accettabili** sono:
      1. **Skeleton puro al 100%**: tutti gli elementi del layout, inclusi immagine e titolo, sono blocchi shimmer neutri incondizionali. L'intera schermata si svela tutta insieme al termine del caricamento con crossfade.
      2. **Shared Element Transition**: quando si vuole preservare la continuità visiva foto+titolo dal punto di origine (card sorgente), implementare rigorosamente `SharedTransitionScope` di Jetpack Compose. In tal caso i parametri preloaded alimentano la transizione animata, non il rendering diretto nello skeleton.
    - **Parametri Preloaded come "Promise"**: I parametri preloaded (es. `preloadedTitle: String?`, `preloadedBackdropPath: String?`) devono essere conservati nella firma del composable skeleton come *promessa* per la futura shared transition, ma **non devono essere letti o usati per il rendering** finché la shared transition non è implementata.
- **Parallasse su Sezioni Intermedie & Container con Liste Scorrevoli**:
    - **Divieto Parallasse Orizzontale su Contenitori con Liste**:
      - Quando una sezione o card contiene una lista orizzontale scorrevole (`LazyRow`, caroselli di locandine o crediti), è severamente vietato applicare parallasse orizzontale (`translationX` collegato allo scroll della lista interna) al backdrop di sfondo del contenitore.
      - Lo scorrimento degli elementi interni non deve traslare lo sfondo. Mantenere `scaleX = 1.0f` senza zoom laterale forzato che provochi ritagli antiestetici sui lati dell'immagine.
    - **Fisica Parallasse Verticale Continua (Anti-Clamping e Attivazione Immediata all'Ingresso)**:
      - Nei contenitori intermedi con parallasse verticale a finestra inseriti in uno scroll principale (es. backdrop di sezione nel dettaglio film), **è vietato** calcolare il moto con distanze grezze dal centro schermo bloccate da clamping a pixel fissi (es. `(-delta * factor).coerceIn(-80f, 80f)`). Tali formule restano bloccate al limite estremo durante la prima metà dello scorrimento, facendo sembrare l'immagine congelata e attivando il movimento in ritardo ("di colpo") solo quando la sezione è già avanzata a centro schermo.
      - La traslazione verticale deve essere sempre normalizzata con un progresso lineare continuo $p \in [0, 1]$ calcolato sull'intero intervallo di visibilità nel viewport:

        ```kotlin
        val currentTop = staticY - scrollState.value
        val totalDistance = screenHeightPx + containerHeightPx
        val progress = ((screenHeightPx - currentTop) / totalDistance).coerceIn(0f, 1f)
        translationY = -maxTravelPx + (2f * maxTravelPx * progress)
        ```

      - In questo modo il parallasse parte fluidamente dal primissimo istante in cui la sommità della sezione e i suoi testi di intestazione entrano dal fondo dello schermo, procedendo a velocità costante senza scatti né zone morte.
- **Standard Micro-Pillole e Badge di Metrica in Dark Theme (Anti-Muddy & No Crowding)**:
    - Quando si racchiudono coppie di metrica compatte (es. Formato + Conteggio, Categoria + Valore) in micro-pillole (`CircleShape`):
      1. **Divieto Sfondi Torbidi (Anti-Muddy)**: È vietato applicare colori d'accento o del tier a bassissima opacità su sfondi scuri (es. `tierColor.copy(alpha = 0.08f)`), poiché creano aloni cupi, spenti e "fangosi". Utilizzare superfici neutre in vetro satinato traslucido (`Color.White.copy(alpha = 0.05f)`).
      2. **Divieto Doppio Segnale Cromatico (No Inner Dot con Bordo Colorato)**: Se la sagoma della pillola adotta già un contorno satinato con il colore d'accento (`border(1.dp, tierColor.copy(alpha = 0.40f), CircleShape)`), è tassativamente vietato inserire un pallino circolare colorato interno (`size(6.dp)`). Il pallino sottrae spazio orizzontale vitale (12–14 dp), costringendo il testo a comprimersi o a tagliare le parole.
      3. **Tipografia ad Alto Contrasto & Dimensioni Minime**:
         - Testo descrittivo: rigorosamente bianco satinato (`Color.White.copy(alpha = 0.90f)`), peso `FontWeight.Bold`, dimensione minima $\ge 10.5\text{ sp}$ per garantire immediata leggibilità su display ad alta densità (vietati testi in grigio scuro $<10\text{ sp}$).
         - Cifra numerica o valore: illuminata nel colore d'accento vivo del tema/tier (`tier.primaryColor`), peso `FontWeight.Black`, dimensione $11.5\text{ sp}$ – $12\text{ sp}$.
      4. **Griglie a Matrice Uniforme**: Nei container a righe multiple (es. 2 righe da 3 pillole), applicare sempre `Modifier.weight(1f)` su ciascuna pillola e `spacedBy(6.dp)` tra gli elementi, garantendo larghezze e altezze identiche su tutta la griglia.

- **Inviolabilità di `CinematicBackground` (Zero Modifiche al Componente Radice)**:
    - `CinematicBackground` costituisce il motore cromatico e d'atmosfera fondamentale di FlickTrove. È severamente vietato modificarne i gradienti, alterarne i valori di colore o inserire sovrascritture/hack interni per "risolvere" dominanti o tinte indesiderate in specifiche schermate o modali (es. riflessi bluastri o variazioni di tonalità).
    - Qualsiasi correzione cromatica deve essere applicata rigorosamente all'interno del container della schermata o del modale specifico (es. neutralizzando card interne o rimuovendo velature sovrapposte), preservando intatta la purezza di `CinematicBackground`.
- **Standard Chip di Filtro Categorici con Identità Cromatica (Tier & Palette di Dominio)**:
    - Nei modali e pannelli di filtro in cui le opzioni appartengono a una tassonomia con palette cromatica definita (es. Formati Pellicola / PreviewBadgeTier: Super 8 ambra, 16mm verde, 35mm blu, 70mm oro, Final Cut magenta, Lost Reel viola):
      1. **Divieto Monocromatismo Anonimo**: È vietato renderizzare tali opzioni come semplici chip grigi o legati unicamente al colore primario dell'app.
      2. **Triplo Segnale Visivo Coordinato**:
         - *Indicatore a pallino*: `dotColor = tierColor` con sagoma circolare (`size(7.dp)`, `CircleShape`) e spaziatura `6.dp` a sinistra del testo.
         - *Bordo satinato a riposo*: `unselectedBorderColor = tierColor.copy(alpha = 0.35f)`.
         - *Attivazione reattiva*: alla selezione, il chip assume lo sfondo vivace `tierColor.copy(alpha = 0.22f)`, bordo solido `tierColor.copy(alpha = 0.7f)` e testo `tierColor`.
      3. **Opzione Globale ("TUTTI")**: L'opzione neutra di azzeramento/tutti non adotta pallini e segue lo stile standard del tema (`MaterialTheme.colorScheme.primary`).
- **Standard Widget di Sistema Glance & RemoteViews (Header, Pillole Data e Banner Canvas)**:
    - **Header Bar Standard dei Widget**:
      - Tutti i widget con testata devono adottare la composizione a tre elementi:
        1. *Barra d'accento verticale*: `width(4.dp)`, `height(16.dp)`, `cornerRadius(2.dp)`.
        2. *Titolo di sezione*: testo in maiuscolo grassetto (`13.sp`, `FontWeight.Bold`, day: `#333333`, night: `Color.White`).
        3. *Pulsante Cerca Rapido Circolare a destra*: `Box(size = 32.dp)` con sfondo circolare `@drawable/widget_search_bg` e icona lente centrata `@drawable/ic_lente` (`16.dp`, tinta bianca).
      - **Codice Colore d'Identità per Tipologia di Widget**:
        - *Episodi TV / Next Up*: Rosso d'accento (`#E50914` / `Color(0xFFE50914)`).
        - *In Riproduzione / Now Watching*: Verde vivo (`#1DB954` / `Color(0xFF1DB954)`).
        - *Film da Vedere / Movies Watchlist*: Giallo Cinema (`#FFC107` / `Color(0xFFFFC107)`).
    - **Pillole Data per Film ed Episodi (Divieto Plain Text)**:
      - Nelle liste verticali dei widget (es. `FlickTroveListWidget`, `FlickTroveTvEpisodesWidget`), le date di uscita o trasmissione non devono mai essere mostrate come semplice testo grigio.
      - Devono essere incapsulate nella pillola sagomata standard (`cornerRadius(16.dp)`, `padding(horizontal = 8.dp, vertical = 3.dp)`):
        - Icona calendario `@drawable/ic_calendario` da `10.dp`.
        - Formattazione dinamica: giorno abbreviato per le uscite imminenti (`OGGI • 29 SET`, `DOM • 4 OTT`) o data estesa (`2 OTT 2026`).
        - Tonalità semantica dello sfondo: oggi (rosso tenue `#38E50914`), domani (arancio/ambra tenue `#33FF9800`), prossimamente (vetro scuro `#25252B` night / `#EEEEF2` day).
    - **Composizione Banner su Canvas & Nitidezza ad Alta Densità**:
      - *Risoluzione Nativa 1:1*: Per card da `68.dp` di altezza, non disegnare a risoluzioni inferiori a `540×162 px`. Risoluzioni basse (es. 360×108) costringono RemoteViews a un upscaling che causa sfocature visibili su schermi ad alta densità (FHD+/QHD+).
      - *Filtro Bilineare e Dithering*: Impostare sempre `isFilterBitmap = true` e `isDither = true` sui `Paint` di backdrop, velatura e logo per prevenire scalettature e color banding sui gradienti scuri.
      - *Proporzioni Logo*: Contenere il riquadro di disegno del logo ufficiale entro il **48% di larghezza** e **42% di altezza** del banner, garantendo margini puliti attorno al marchio.
      - *Separazione Inter-Film*: RemoteViews/Glance tende a collassare gli `Spacer` tra elementi riciclati: applicare sempre `padding(bottom = 14.dp)` sul container `Box` di ciascuna card per garantire stacco fisico visibile.
      - *Elaborazione Concorrente Parallela*: Quando si caricano feed ricchi (fino a 25 elementi), non iterare in modo sequenziale. Eseguire sempre il recupero e rendering tramite `coroutineScope { items.map { async { ... } }.awaitAll() }`.
    - **Dimensioni Griglia Launcher & Scalabilità (`AppWidgetProviderInfo`)**:
      - *Dimensione Base Compatta (2×2)*: Tutti i widget a feed/elenco o card devono supportare come fattore di forma minimo e iniziale il formato **2×2**:

        ```xml
        android:minWidth="120dp"
        android:minHeight="110dp"
        android:minResizeWidth="120dp"
        android:minResizeHeight="110dp"
        android:targetCellWidth="2"
        android:targetCellHeight="2"
        android:resizeMode="horizontal|vertical"
        ```

      - *Divieto Minimi Rigidi Sopra 2×2*: Non impostare `minHeight` > `110dp` (es. `140dp`) o `minWidth` > `120dp` per widget a lista: valori superiori forzano i launcher standard (4×4 o 5×5) a bloccare il widget su 3 righe (2×3 o 3×3), impedendo all'utente di posizionarli in layout home compatti.
      - *Adattamento Fluidità Contenuto*: L'alberatura composabile interna deve supportare la visualizzazione sia a 2×2 (1-2 card visibili con scorrimento) che espansa fino a tutto schermo (4×2, 4×3, 5×5) sfruttando `defaultWeight()` e `fillMaxWidth()`.
- **Stabilità dello Slot di Composizione nei Widget Animati (Anti-Branching Animatable)**:
    - Nei widget con animazioni continue o state-driven (come `LiquidStarIcon`, icone con morphing, progress bar animate), **non racchiudere mai il widget animato dentro rami condizionali `if/else` basati sullo stato che deve animare**.
    - Esempio: se un contatore passa da `0` a `> 0`, non mostrare un'icona statica nel ramo `likes == 0` e il widget animato nel ramo `likes > 0`. Questo distrugge e ricrea lo slot di composizione, resettando lo stato interno (`isFirstComposition = true`) e annullando l'effetto dell'animazione (`LaunchedEffect(isLiked)`).
    - Mantenere sempre una chiamata incondizionata al widget animato (`LiquidStarIcon(isLiked = ...)`) e variare solo i parametri o il testo adiacente, garantendo che lo slot di composizione rimanga stabile durante le transizioni di stato.

- **Integrità Strutturale & Ribilanciamento Bento Grid (No Slot Vuoti)**:
    - Quando una card, metrica o sezione viene rimossa o decommissionata da un layout CSS Bento Grid (es. dashboard Admin):
      - È tassativamente vietato lasciare buchi o slot orfani che spezzano la geometria della griglia (es. 2 card da 2 colonne su una riga da 6).
      - Ribilanciare sempre l'architettura della griglia (es. base a 12 colonne con 2 Hero cards da 50% / `span 6` e 4 Metric cards da 25% / `span 3` su una singola riga perfettamente allineata).
      - Rimuovere classi di larghezza superflue (`.wide`) e garantire la simmetria a 2 colonne su tablet (`<= 1200px`) e 1 colonna su mobile (`<= 768px`).
- **Integrità Asset Brand & Dualità tra Icona Adattiva e Glifo Compatto (`ic_flicktrove_logo`)**:
    - **Dualità degli Asset di Brand**:
      1. **Launcher & Contesti Ampi (`ic_launcher_foreground_vector.xml`)**: Adotta il viewport 768×768 con clearance circolare al 50% prescritta da Google per le Adaptive Icon. Si utilizza tassativamente a `size(32.dp)` o superiore (TopBar, Personal Zone, Splash Screen).
      2. **Badge, Pillole e Chip Inline (`ic_flicktrove_logo.xml`)**: Per contenitori compatti (pillole provider nei commenti, chip di stato con altezza $\le 22\text{ dp}$), usare tassativamente `R.drawable.ic_flicktrove_logo` con **viewport stretto** calcolato sul bounding box reale dei path (es. `319.2 × 244.8`): niente `<group android:translateX>` di riposizionamento, niente padding invisibili che gonfiano il container pill. Dimensionare l'`Icon` rispettando l'aspect ratio effettivo del viewport (es. `width = 16.dp, height = 12.dp` per ratio ≈ 1.30:1).
    - **Conversione da Formati Vettoriali Proprietari (es. Microsoft APS/XPS)**:
      Quando l'utente fornisce un `.xml` in formato Microsoft APS (`<Aps>`, `<Page>`, `<Path>`, `<PathFigure>`, `<PolyLine>`, `<Bezier>`):
      1. **NON** usare `<group android:translateX/Y>` come hack — lascia padding invisibili nel viewport.
      2. Scrivere uno script Python che: estrae i `<PathFigure>` dal `<Path>` con `<Pen>` (non il `<Clip>`); applica il `RenderTransform="1 0 0 -1 0 H"` (`y' = H - y`); converte `<PolyLine>` in `M/L` e `<Bezier>` in `C`; campiona le curve per il bounding box preciso (incluso `strokeWidth/2`); trasla tutte le coordinate di `(-minX, -minY)`; genera il `<vector>` con viewport stretto sul contenuto.
      3. Usare sempre `xmlns:ns1="http://schemas.android.com/aapt"` e `<ns1:attr name="android:strokeColor">` (prefisso `ns1`, non `aapt`) per allineamento con il resto del progetto.
- **`graphicsLayer` API Exactness — Nessuna Proprietà Shorthand `scale`**:
    - In `Modifier.graphicsLayer { ... }` la proprietà shorthand `scale` **non esiste** e causa `Unresolved reference` a compile time. Usare sempre le due proprietà esplicite:

      ```kotlin
      // ❌ Non valido
      scale = 1.25f          // → Unresolved reference
      scaleY = 1.25f         // → distorce l'aspect ratio su immagini 16:9

      // ✅ Scala uniforme (preserva le proporzioni)
      scaleX = 1.25f
      scaleY = 1.25f
      ```

    - Nei **backdrop image con effetto parallasse**, usare sempre `scaleX = scaleY` (stesso valore su entrambi gli assi). Scalare solo `scaleY` su immagini 16:9 in container a altezza dinamica le deforma visibilmente (le "schiaccia" verticalmente quando l'altezza del container supera quella nativa dell'immagine).
- **Barra di Input Commenti: Espansione Reattiva (IME & Focus) & Concentricità Adattiva (`CommentInputBar`)**:
    - **Rilevamento IME Reattivo**: Non basare l'espansione della barra di input esclusivamente su `onFocusChanged` di `BasicTextField`, soggetto a perdite di focus transitorie durante l'animazione di comparsa della tastiera o a intercettazioni da container padre. Calcolare tassativamente lo stato di espansione combinando `WindowInsets.ime.getBottom(LocalDensity.current) > 0` (tastiera fisicamente a schermo) con `isFocused`, testo non vuoto, media allegati o `replyingTo != null`.
    - **Geometria Adattiva & Concentricità**:
      - *Stato Compatto (Idle/A riposo)*:
        - **Pulsante Send & Casella Testo**: entrambi dimensionati ad altezza minima **`44.dp`** con raggio **`22.dp`** (`CircleShape`), garantendo presenza tattile generosa e icona send da **`28.dp`** (compensando il padding interno del vettore per riempire armoniosamente il cerchio, contrasto inattivo a `0.38f` e bordo concentrico a `1.dp`).
        - **Concentricità Isotropa (Regola $R_{\text{esterno}} = R_{\text{interno}} + P$)**: il padding interno del container deve essere rigorosamente simmetrico su tutti i lati (**`horizontal = 8.dp, vertical = 8.dp`**). Il raggio esterno del container è tassativamente **`30.dp`** ($22\text{ dp} + 8\text{ dp} = 30\text{ dp}$ su altezza totale di $60\text{ dp}$), garantendo una clearance uniforme di esattamente **`8.0 dp`** a $360^\circ$ attorno all'intero arco del pulsante Send e della casella testo, senza alcuna compressione o strozzatura diagonale.
      - *Stato Espanso (In scrittura/Tastiera aperta)*:
        - **Concentricità Rigorosa (Regola 28dp / 16dp)**: Il container esterno passa a `RoundedCornerShape(28.dp)` con padding simmetrico isotropo `horizontal = 12.dp, vertical = 12.dp`. La casella di testo interna adotta `RoundedCornerShape(16.dp)` e padding `horizontal = 14.dp, vertical = 10.dp`. Viene così rispettata la legge di concentricità esatta:
          $$R_{\text{esterno}} = R_{\text{interno}} + P = 16\text{ dp} + 12\text{ dp} = 28\text{ dp}$$
        - **Clearance Action Toolbar Inferiore (Anti-Pinch)**: La riga inferiore dei controlli (pulsanti circolari d'azione da `34.dp`, switch spoiler, reply chip) deve includere un margine di sicurezza interno `padding(top = 8.dp, bottom = 4.dp, start = 2.dp, end = 2.dp)`. Questo assicura una clearance netta di $16\text{ dp}$ dal bordo inferiore curvo ($12\text{ dp} + 4\text{ dp} = 16\text{ dp}$) e $14\text{ dp}$ dai lati, impedendo ai cerchi e badge di toccare o deformare la curvatura a `28.dp` del contenitore.
        - **Spoiler a Switch**: implementato mediante gruppo semantico [FlickTroveSwitch](file:///c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/components/common/FlickTroveSwitch.kt) affiancato dall'icona occhio reattiva (`ic_eye` / `ic_eye_off`), che attiva il glow d'accento ed estende la thumb elastica con tick aptico al tocco di entrambi gli elementi.
      - *Tocchi sul Box*: Per evitare che il wrapper del campo testo rubi il focus a `BasicTextField`, impostare sempre `isInputExpanded = true` contestualmente alla richiesta `focusRequester.requestFocus()`.
- **Standard Skeleton & Shimmer per Temi Dark OLED (Anti-Glare & Alpha Minime)**:
    - È tassativamente vietato sovrapporre sfondi bianchi ad alta opacità (`Color.White.copy(alpha = 0.16f..0.35f)`) sotto ai modificatori `.shimmerEffect()`. Su sfondi neri OLED (`#000000` o `#10111A`), valori superiori al 10% trasformano i blocchi di caricamento in fari grigio chiaro accecanti ("troppo chiaro") che distruggono l'eleganza dark di FlickTrove.
    - I placeholder dello skeleton devono adottare livelli di vetro scuro traslucido minimali:
      - Titoli e contenitori principali: max `Color.White.copy(alpha = 0.06f..0.08f)`.
      - Righe di testo secondarie, sottotitoli e card: `Color.White.copy(alpha = 0.03f..0.05f)`.
      - Bordi e separatori: `Color.White.copy(alpha = 0.06f..0.08f)`.
    - *Calibrazione Shimmer Wave*: L'onda di luce dell'animazione shimmer deve mantenere un'escursione delicata, partendo dal 3% (`alpha = 0.03f`) e culminando al massimo al 10% (`alpha = 0.10f`), simulando una pura riflessione specchiata su vetro scuro senza velature lattiginose.
- **Ritenzione dello Stato durante il Fade-Out dei Modali (Exit Animation State Protection)**:
    - Nei modali composabili con animazione di uscita a durata fissa (es. `FlickTroveModal` con 300ms di shrink/fade-out), è tassativamente vietato azzerare istantaneamente l'oggetto dati target (es. `commentToReport = null`) nel momento in cui viene innescata la chiusura.
    - Se l'oggetto diventa `null` prima che l'animazione di uscita sia completata, il blocco di contenuto interno scompare all'istante, lasciando visibile una scatola scura vuota e troncata che si ritrae antiesteticamente.
    - Pattern obbligatorio: memorizzare sempre una copia locale persistente dell'ultimo valore non nullo:

      ```kotlin
      var displayItem by remember { mutableStateOf(targetItem) }
      LaunchedEffect(targetItem) { if (targetItem != null) displayItem = targetItem }
      FlickTroveModal(isVisible = targetItem != null, onDismissRequest = onDismiss) {
          val item = displayItem ?: return@FlickTroveModal
          // Renderizza sempre usando `item`
      }
      ```

- **Standard Icone SVG Native per FlickTrove_Admin (Divieto Assoluto di Emoji e Icone Generiche)**:
    - Nel pannello web `FlickTrove_Admin`, è severamente vietato utilizzare:
      1. **Emoji per azioni, badge o stati** (es. vietato usare `🛡️`, `⚠️`, `👁️`, `🗑️`, `🚫`, `✅`).
      2. **Icone generiche di terze parti** (es. icone Lucide/Feather con aste geometriche rette e fredde).
    - Tutti i componenti (sidebar navigation, bento cards, badge motivo, bottoni d'azione, empty states) devono adottare **esclusivamente le icone SVG vettoriali originali di FlickTrove**, derivate dagli asset XML di progetto (`ic_flag`, `ic_scudo_successo`, `ic_error`, `ic_eye`, `ic_trash`, `ic_block`, `ic_tick`, `ic_ricarica`, `ic_comment`).
    - **Dashboard Bento Grid**: Le code operative di moderazione attiva (**Segnalazioni** su `/reports.html` con stile `stat-danger` e **Feedback Totali** su `/feedback.html` con stile `stat-warning`) devono essere sempre presenti come card di primo piano nella seconda riga della Bento Grid, alimentate dai rispettivi contatori in tempo reale via `getCountFromServer`.
- **Modali Contestuali su Item di Lista vs Header/Top-Bar (Regola di Isolamento del Morphing)**:
    - **Isolamento dell'Eccezione Centrata (Solo Item di Lista Dinamica)**:
      - Quando un'azione di moderazione o menu è ancorata a un'icona microscopica o scrollabile all'interno di una lista (es. icona bandierina `ic_flag` da `14.dp` in `CommentReportModal`), l'uso di `triggerBounds` rischia di produrre residui di blur sullo sfondo a causa del disallineamento delle coordinate durante il riciclo della vista. In questo specifico scenario è obbligatorio adottare **`FlickTroveModal`** centrato.
      - **Divieto di Overgeneralizzazione**: È tassativamente vietato rimuovere `MorphGlassModal` dai modali ancorati a pillole o pulsanti fissi dell'interfaccia (es. pillola CommsUni con `commsUniButtonBounds`, filtri con `sortButtonBounds`). Tali componenti preservano l'espansione e contrazione a molla organica dal punto di origine.
    - **Anti-Ghosting Geometrico nel Vetro (`MorphGlassModal` / `HomeFilterModal`)**:
      - Quando `triggerBounds == null`, è vietato collassare il rettangolo d'inizio in cerchi arbitrari (es. `center - 20f`); applicare sempre scala centrale geometrica (`startRect = targetRect`).
      - Trasmettere sempre `alpha = modalAlpha` all'invocazione di `Modifier.hazeGlass`: garantisce che il blur shader (`blurRadius * alpha`) e la tinta sfumino a zero, scongiurando cerchi o aloni congelati nel layer di composizione.
    - **Contesto Visivo Obbligatorio (Citazione Cinefila Unboxed)**:
      - È severamente vietato presentare un modale di azione privo del contesto visivo dell'entità target (rischio cognitivo di disorientamento).
      - In cima al corpo del modale, inserire l'anteprima del commento/recensione trattata come una **citazione cinefila elegante e unboxed**:
        1. *Testo tra virgolette*: `“$text”` in corsivo `bodyMedium` a `0.85f` di contrasto, testo centrato con `maxLines = 3` ed ellissi.
        2. *Attribuzione Autore & Piattaforma*: `— $authorName` affiancato al badge a pillola (`CircleShape`) del circuito di provenienza (es. CommsUni, TV Time, FlickTrove).
        3. *Superficie Pulita (No Heavy Nested Boxes)*: Evitare contenitori o box grigi rettangolari annidati che appesantiscono visivamente la scheda del modale.
- **Geometria Concentrica e Stabilità nelle Floating Input Bar (`CommentInputBar`)**:
    - **Stabilità Assoluta del Raggio Esterno (`outerCorner = 30.dp`)**:
      - Nelle barre di input flottanti a capsula/vetro, il raggio di curvatura esterno (`outerCorner`) deve rimanere invariato e stabile a **`30.dp`** sia in stato compatto/a riposo sia in stato espanso/in digitazione. È tassativamente vietato ridurre o animare il raggio a valori disallineati (es. mutare a 28dp) durante l'espansione.
    - **Simmetria Rigorosa del Padding Perimetrale ($P = 14\text{ dp}$)**:
      - Nello stato espanso, il padding interno del container deve essere rigorosamente identico su tutti e 4 i lati: `top = 14.dp`, `bottom = 14.dp`, `start = 14.dp`, `end = 14.dp`.
      - È severamente vietato introdurre asimmetrie (es. `top = 12.dp` o `bottom = 4.dp`), poiché rompono la clearance diagonale negli angoli curvi a 45°, producendo antiestetici schiacciamenti ("pinch") tra gli elementi interni e l'arco esterno.
    - **Standardizzazione dei Controlli Toolbar a `32.dp` (Formula Concentrica $R_{\text{outer}} = R_{\text{inner}} + P$)**:
      - Tutti i pulsanti circolari della toolbar inferiore (*Foto, GIF, Matita Markdown*) devono adottare dimensione standard di **`32.dp`** (`CircleShape`, $R_{\text{inner}} = 16\text{ dp}$) con icone proporzionate a `16.dp` e progress indicator a `16.dp`.
      - Tutte le pillole di stato e toggle (*Spoiler*, switch destinazione) devono adottare altezza standard di **`32.dp`** (`CircleShape`, $R_{\text{inner}} = 16\text{ dp}$) con padding orizzontale di sicurezza a `11.dp`.
      - In questo modo la formula di concentricità $16\text{ dp} (R_{\text{inner}}) + 14\text{ dp} (P) = \mathbf{30\text{ dp}} (R_{\text{outer}})$ è soddisfatta con precisione millimetrica su tutti gli angoli inferiori e superiori.
    - **Interlinea Equilibrata tra Riga Input e Toolbar**:
      - La spaziatura verticale tra la riga di testo e la toolbar d'azione inferiore deve essere impostata a **`10.dp`**, garantendo un respiro visivo armonioso e bilanciato rispetto ai `14.dp` perimetrali.
- **Clearance Asimmetrica nei TopBar con Icone Multiple (Anti-Overlap)**:
    - Nelle barre di navigazione superiori (`SimpleMorphingTopBar`, `DetailMorphingTopBar`, `PersonMorphingTopBar`, ecc.), quando i lati ospitano un numero asimmetrico di azioni (es. Sinistra: Back 44dp; Destra: Filtri 44dp + Condividi 44dp = 92dp), è vietato usare un padding orizzontale simmetrico ridotto (es. `64.dp`).
    - Calcolare sempre clearance asimmetriche dedicate: `titleStartPadding` (minimo 8dp oltre i pulsanti di sinistra, es. 52dp o 92dp con Home FAB) e `titleEndPadding` (minimo 8dp oltre i pulsanti di destra, es. 100dp con filtri). In questo modo il testo lungo in qualsiasi lingua viene troncato con ellissi prima di toccare qualsiasi icona.
- **Divieto di animateContentSize con Alignment.BottomCenter su Barre Espandibili (Anti-Tagli Netti)**:
    - Nei container di input espandibili (es. `CommentInputBar`), non applicare `Modifier.animateContentSize(alignment = Alignment.BottomCenter)` se il layout interno contiene già sezioni animate con `AnimatedVisibility(expandVertically(...))`.
    - Il doppio sistema di animazione crea un ritaglio rettangolare provvisorio che taglia nettamente sia la cima (spinta a $Y < 0$) che il fondo. Lasciare che il container genitore tracci la misura naturale dei figli e applicare unicamente `.clip(boxShape)` per preservare i bordi curvi del vetro a 30dp/32dp in ogni frame.
- **Stato Attivo su Barre di Ricerca a Pillola (Accent Border & Focus Propagation)**:
    - Nelle barre di ricerca a pillola (`CircleShape`), il tocco su qualsiasi punto del container deve propagare il focus al campo di testo tramite `FocusRequester`.
    - Quando attiva/focalizzata (`onFocusChanged`), il contorno deve animarsi fluidamente verso il colore accento primario (`accentColor`) con spessore a `1.5.dp` e l'icona della lente deve tingersi del colore accento, offrendo immediata reattività visiva.
- **Emblemi Trofei & Badge 3D (Anatomia a 3 Livelli, Oblò e Ghiera Sopraelevata)**:
    - **Layering Tridimensionale**: Gli emblemi circolari complessi (`FlickTroveBadgeEmblem`) adottano una struttura rigorosa a tre livelli ottici:
      1. *Layer A (Sfondo & Ghiera Esterna)*: Drop shadow di contatto morbida sotto l'emblema (offset $+Y$ a $0.045f$), base nera uniforme che copre fino all'anello esterno (`lensRadius` allineato all'anello esterno per evitare stacchi o la "fine della parte nera"), tacche ottiche graduate e perimetro satinato.
      2. *Layer B (Oblò Interno con Icona Recessa)*: Box a diametro `innerCircleSize` con tassativo `.clip(CircleShape)`. Le icone oversize devono rimanere clippate al bordo interno dell'oblò senza invadere le tacche e la ghiera esterna. Parallasse inverso (`-tiltX`) senza drop shadow fittizie sotto l'icona (che la farebbero sembrare uno sticker sollevato anziché incisa in profondità).
      3. *Layer C (Vetro di Primo Piano & Bezel Inner Shadow)*: Disegnato *sopra* l'icona. Include l'ombra di incasso ad anello radiale (da trasparente a nero profondo) lungo `innerRingR` che fa percepire la ghiera esterna come fisicamente più alta, e il fascio di luce diagonale periodico calmo a intervallo lungo (~9s).
    - **Firme Compose per Gradienti con Stop**: Utilizzare tassativamente `Brush.radialGradient(vararg Pair<Float, Color>)` (es. `0.45f to Color.Transparent, 1.0f to Color.Black`) anziché tentare parametri denominati inesistenti `stops`.
- **Intercettazione Click su Header Sospesi & Ampiezza Touch Target Barra di Ricerca (`SearchHeader`)**:
    - **Blocco Leakage dei Click su Header Fluttuanti**: Quando un header con `hazeGlass` o `zIndex` elevato è sovrapposto a una griglia o lista scorrevole (es. `SearchHeader` sopra i poster di `SearchContentGrid`), il container principale dell'header deve consumare tassativamente i tocchi non gestiti applicando `.clip(headerShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* Intercept */ }`. Senza questo blocco, qualsiasi tap casuale negli spazi vuoti, tra i pulsanti o nell'area status bar passa ai componenti sottostanti, aprendo erroneamente le schede dei film che scorrono sotto l'header.
    - **Touch Target Generoso sulla Barra di Ricerca a Pillola**: Il container esterno a pillola da `44.dp` (`CircleShape`) deve essere interamente cliccabile (`.clip(CircleShape).clickable(...)`), richiedendo il focus (`focusRequester.requestFocus()`) e mostrando la tastiera (`keyboardController?.show()`). In questo modo l'utente non è costretto a mirare con precisione millimetrica la stretta area di testo di `BasicTextField`, ma può toccare l'icona della lente, il placeholder o qualsiasi margine della pillola per iniziare subito a cercare.
- **Preservazione dello Scroll State su Liste di Overlay/Schermate Secondarie (`UpdatesScreen` / `Reminders`)**:
    - **Anti-Reset su Rientro da Navigazione**: Quando una schermata o overlay apre un dettaglio (es. `MovieDetailScreen`) e l'utente torna indietro:
      1. Evitare tassativamente `LaunchedEffect(categoryTab) { listState.scrollToItem(0) }`. In Compose, `LaunchedEffect` viene rieseguito a ogni ingresso nella composizione (anche con lo stesso valore della chiave), azzerando forzatamente lo scroll verso la cima a ogni `pop()` del navigator.
      2. Lo scroll al top deve essere innescato **esclusivamente** sul callback esplicito di cambio tab (`onOptionClick`).
      3. Hoistare i `LazyListState` nel rispettivo `ViewModel` (come già avviene in `SearchViewModel` e `RecommendationsViewModel`) garantendo che il tracciamento di `firstVisibleItemIndex` e `firstVisibleItemScrollOffset` sopravviva intatto alla ricomposizione dell'overlay.
- **Ciclo di Vita dello `ScrollToTopFab` e Divieto di `onDispose` Distruttivo**:
    - Nei composable che gestiscono griglie o liste scorrevoli (`HomeScreen`, `VistiScreen`, `FolderDetailScreen`), la sincronizzazione con `LocalScrollToTopState` deve avvenire tassativamente tramite `LaunchedEffect(showScrollToTop, ...)`.
    - **Divieto di `onDispose` Distruttivo**: È severamente vietato inserire blocchi `onDispose { scrollToTopStateHolder.value = ScrollToTopState(isVisible = false) }` legati a `showScrollToTop`. Quando l'utente naviga verso uno schermo di dettaglio (es. `MovieDetailScreen`) e torna indietro, il valore di `showScrollToTop` rimane `true`; un azzeramento in `onDispose` lascerebbe il FAB disattivato per sempre.
    - **Invariante Tab Tracking in `MainScreen`**: In `MainScreen`, `_lastTab` e `previousTab` devono essere inizializzati sempre a `currentTab` (`var _lastTab by remember { mutableStateOf<Tab>(currentTab) }`). Il reset dello stato di scroll to top deve verificarsi **esclusivamente** quando l'utente cambia intenzionalmente tab (`if (currentTab != _lastTab)`).
- **State Hoisting dello Scroll nei Modali Multi-Step**:
    - Nei modali che prevedono navigazione tra viste diverse (es. ricerca film -> selezione avatar o cover), istanziare sempre `rememberLazyListState()` e `rememberLazyGridState()` nel contenitore genitore di primo livello (`AvatarSelectionContent`).
    - Mai dichiarare lo stato dello scroll dentro rami condizionali alternati (`if (selectedMedia == null)`): all'apertura dello step secondario e al successivo ritorno indietro, lo stato verrebbe ricreato da capo perdendo la posizione dello scroll.
    - La chiamata imperativa `.scrollToItem(0)` va eseguita unicamente al lancio di una nuova query di ricerca (`searchQuery`).
- **Skeleton Shimmering Coordinato vs Spinner Generici nei Modali**:
    - È vietato mostrare spinner `CircularProgressIndicator` isolati a tutto schermo per mascherare il caricamento di elenchi e griglie.
    - Fornire sempre skeleton loader con `Modifier.shimmerEffect()` che replicano fedelmente la griglia o lista finale (es. griglia a 3 colonne per avatar circolari a 80.dp, griglia a 2 colonne per backdrop con aspect ratio dinamico, e card con locandina 40x60dp per i risultati di ricerca).
- **Concentricità e Bilanciamento Spaziature nei Modali Glassmorphic**:
    - I modali con raggio di curvatura importante (es. `RoundedCornerShape(32.dp)` in `GlassmorphicModal`) devono mantenere un rapporto di spaziature proporzionato alla curvatura.
    - Applicare la regola della concentricità geometrica: con modale a 32dp e card interne con raggio tra 16dp e 18dp, il padding orizzontale ottimale è `16.dp`.
    - Il padding inferiore deve essere di almeno `24.dp` (con `Spacer(height = 6.dp)` al fondo della colonna), impedendo che gli elementi inferiori collidano o tocchino precocemente la curva d'angolo a 32dp del modale.
- **Prevenzione Crash da `NaN` e `Infinite` nei Calcoli di Moto e Parallasse Compose (`safeRoundToInt` / `safeCoerceTilt`)**:
    - Nei componenti che calcolano traslazioni, tilt, offset, gradienti o rotazioni alimentati da sensori fisici (giroscopio, accelerometro, `SensorEventListener`) o gesture touch (`awaitPointerEvent`, trascinamenti, molle `Animatable`):
      1. **Valori Non Calibrati o Irregolari**: All'avvio dell'app o al cambio orientamento dello schermo, le matrici di rotazione vettoriale e le accelerazioni possono produrre valori transitori `Float.NaN` o `Float.POSITIVE_INFINITY` / `Float.NEGATIVE_INFINITY` (es. divisioni per zero nei delta o clamping difettosi).
      2. **Divieto di `.roundToInt()` Diretto su Offset e Layout**: In Kotlin `Float.roundToInt()` lancia tassativamente `IllegalArgumentException: Cannot round NaN value.` causando il crash fatale dell'app durante il rendering (`OffsetPxNode.measure`).
      3. **Sanitizzazione Obbligatoria**: Utilizzare sempre funzioni di estensione difensive (es. `Float.safeRoundToInt(): Int` e `Float.safeCoerceTilt(): Float`) che intercettano `isNaN()` e `isInfinite()` convertendoli in modo trasparente e silenzioso a `0` o `0f`. Verificare e scartare a monte gli eventi dei sensori con pitch/roll non validi prima di aggiornare gli stati Compose (`SnapshotState`).
- **Isolamento dei Modificatori di Click/Scala dai Container con `hazeGlass` (Anti-Haze-Deformation)**:
    - È tassativamente vietato applicare modificatori con trasformazioni di scala o deformazioni elastiche (`graphicsLayer` con `scaleX`/`scaleY`, come `.bounceClick`) direttamente sul container che ospita `.hazeGlass(...)`.
    - Quando un container adotta `hazeGlass` e subisce una variazione di scala (es. scaling down a `0.92f`), le coordinate e l'area del buffer di campionamento del blur vengono distorte e deformate nel rendering dei frame successivi, producendo sfarfallii, stiramenti e artefatti visivi sgradevoli sul frosted glass.
    - Il modificatore `.bounceClick` deve essere posizionato tassativamente sul **Box di contenuto interno**, mantenendo il Box esterno con `.hazeGlass`, `.clip` e `.border` perfettamente statico e geometricamente invariato.
- **Scroll to Top ad Alte Prestazioni — Pattern 'Fast Jump + Smooth Glide' (`fastScrollToTop`)**:
    - Nei container a scorrimento pigro (`LazyColumn`, `LazyVerticalGrid`, `LazyStaggeredGrid`), l'invocazione di `animateScrollToItem(0)` da posizioni molto avanzate (es. decine o centinaia di card salvate) costringe Compose a comporre, misurare e disporre a schermo ogni singolo elemento intermedio a velocità supersonica, inondando il thread UI e Coil di richieste d'immagine e allocazioni GC (causando vistosi drop di frame e scatti).
    - È tassativamente vietato eseguire `animateScrollToItem(0)` incondizionato dai pulsanti 'Scroll to Top' o FAB.
    - Adottare sempre l'estensione `fastScrollToTop(glideItemIndex)`: se la lista è oltre l'indice soglia (es. 10-12), esegue prima un teletrasporto istantaneo senza animazione `scrollToItem(glideItemIndex)`, seguito immediatamente da `animateScrollToItem(0)`. Questo garantisce un atterraggio morbido e fluido a 60/120 FPS fissi, con zero frame drops e zero allocazioni superflue.
- **Preservazione Rigorosa della Formattazione nei Motori di Traduzione (`translatePreservingNewlines`)**:
    - I modelli di Machine Translation on-device (come Google ML Kit NMT) e gli endpoint API di traduzione trattano per default i caratteri di a capo (`\n` e `\n\n`) come semplici spazi bianchi o delimitatori di token, collassando recensioni e commenti multi-paragrafo in un unico 'muro di testo' continuo privo di spaziatura e formattazione.
    - È tassativamente vietato inviare testi multi-riga direttamente al traduttore senza preservarne la struttura geometrica.
    - Adottare sempre la pipeline asincrona `translatePreservingNewlines`:
      1. Identifica se il testo contiene `\n`.
      2. Suddivide il testo preservando le righe vuote (`line.isBlank()`).
      3. Traduce in parallelo (`async`) solo i blocchi di testo non vuoti (singoli paragrafi ed elenchi puntati con emoji).
      4. Ricostruisce il testo originale ricomponendolo con `\n`. In questo modo le interlinee, i doppi a capo, gli elenchi con emoji e le citazioni rimangono intatti al 100%.
- **Distacco Fluido e Viscoelastico Stile Simbionte per Pulsanti Flottanti (`Symbiote Detachment` / FAB)**:
    - Nei pulsanti flottanti ausiliari associati o posizionati in stretta prossimità di un FAB primario (es. `MainScrollToTopFab` sopra il `MainSearchFab`):
      1. **Frame di Coordinate Unificato & Ancoraggio Verticale (`104.dp` Box)**:
         - Quando il FAB secondario deve emergere fisicamente dal FAB primario ed essere connesso da un metaball o ponte liquido, racchiudere il FAB secondario in un container verticale unificato che copre l'intera escursione (es. `padding(bottom = 90.dp, end = 20.dp).size(width = 52.dp, height = 104.dp)`).
         - In questo frame locale:
           - $Y = 0$: sommità del container (a `194.dp` da fondo schermo).
           - $Y_{\text{rest}} = 24\text{ dp}$: centro del pulsante secondario a riposo (a `170.dp` da fondo schermo).
           - $Y_{\text{fab\_top}} = 52\text{ dp}$: apice/calotta superiore del FAB primario (a `142.dp` da fondo schermo).
           - $Y_{\text{fab\_center}} = 78\text{ dp}$: centro del FAB primario (a `116.dp` da fondo schermo).
      2. **Direzionalità del Ponte & Divieto di Invasione Interna**:
         - Il centro della goccia emergente `cyDrop` viaggia tassativamente **verso l'alto** da $Y_{\text{fab\_top}} = 52\text{ dp}$ a $Y_{\text{rest}} = 24\text{ dp}$ (`cyDrop = 52.dp - 28.dp * t`).
         - La base del ponte liquido si àncora alla calotta superiore ($Y \approx 54\text{ dp}$), garantendo che `cyDrop < cyFabTop` in ogni istante del moto. È severamente vietato posizionare `cyDrop` sotto l'apice del FAB ($Y > 52\text{ dp}$), per evitare che il ponte si inverta proiettandosi verso il basso e oscurando l'icona interna del FAB primario.
      3. **Z-Ordering Rigoroso**:
         - In Compose, dichiarare il composable del FAB secondario/ponte (`MainScrollToTopFab`) **prima** del FAB primario (`MainSearchFab`), in modo che la base del ponte e la goccia emergano naturalmente da dietro il profilo circolare del FAB principale senza mai coprirne il bordo o la lente.
      4. **Innesco Autonomo a Snap & Transizione Fluida (No Finger Tracking)**:
         - Il distacco simbiotico non vincola la salita al trascinamento continuo del dito: si innesca in modo del tutto autonomo appena la visibilità diventa attiva (`visible = true` calcolato con la soglia `firstVisibleItemIndex > 3 || (firstVisibleItemIndex > 0 && firstVisibleItemScrollOffset > 150)`).
         - L'animazione di risalita esegue una corsa a 350ms con `FastOutSlowInEasing`, producendo la gemmazione liquida e l'allungamento del collo. Al raggiungimento di $p = 0.68$, avviene lo snap del filamento con rinculo e stabilizzazione del pulsante su `hazeGlass` e freccia rotata.
         - Nessun feedback aptico ridondante per non disturbare lo scorrimento naturale dell'utente.
      5. **Deformazione del FAB di Ricerca via Layout (Anti-Border Detachment)**:
         - Quando la goccia emerge e si stacca, il FAB di ricerca sottostante (`MainSearchFab`) subisce un allungamento viscoelastico verso l'alto seguito da un rimbalzo a molla smorzato (`searchBounceY`).
         - **Divieto Assoluto di `graphicsLayer { scaleY, translationY }`**: I layer grafici hardware disaccoppiano il bordo vettoriale dalla superficie sfocata di `hazeGlass`, facendo apparire il contorno staccato dal cerchio durante il rimbalzo.
         - La deformazione deve essere applicata rigorosamente alterando l'altezza reale del layout (`height(searchHeightDp)` da `52.dp` a `56.5.dp`) e la posizione del Box (`offset(y = searchBounceY.dp)`), preservando la perfetta concentricità e adesione millimetrica del bordo al frosted glass.
      6. **Copertura Globale e Unificazione di `LocalScrollToTopState`**:
         - Le schermate dell'app con liste a scorrimento verticale (`HomeScreen`, `VistiScreen`, `DiscoverScreen`, `RecommendationsScreen`, `BoxOfficeScreen`, `NewsScreen`, `FoldersScreen`, `FolderDetailScreen`) devono registrarsi a `LocalScrollToTopState.current` (escludendo viste panoramiche/dashboard come Home Feed e Trophy Room).
         - Impostare sempre la soglia standard `firstVisibleItemIndex > 3 || (firstVisibleItemIndex > 0 && firstVisibleItemScrollOffset > 150)` e l'azione `scope.launch { listState.fastScrollToTop() }`.
         - Proteggere sempre il ciclo di vita tramite `DisposableEffect(Unit) { onDispose { scrollToTopStateHolder.value = ScrollToTopState() } }` per azzerare istantaneamente lo stato all'uscita o pop della schermata.
- **Fusione e Rigurgito Simbiontico nei Controlli di Testata (`SymbiontSearchBarWithFilters`)**:
    - Nelle barre di ricerca di testata con filtri e controlli di layout contestuali (`SearchHeader`):
      1. **Divieto di Shrink/Fade Generico (`AnimatedVisibility`)**: Non nascondere o mostrare i pulsanti di filtro tramite generiche animazioni di dissolvenza/ritaglio orizzontale a rettangolo rigido.
      2. **Fusione Cromatica e Geometrica Isomorfa (44dp / CircleShape)**:
         - Sia la barra di ricerca a capsula che i pulsanti d'azione (filtro e layout toggle) condividono la medesima altezza di **`44.dp`**, sagoma a pillola **`CircleShape`** e sfondo **`MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)`**.
         - Questa perfetta isomorfia consente una fusione su `Canvas` fluida e impercettibile, senza giunzioni, difetti di texture o interruzioni di blur.
      3. **Dinamica di Ingestione (Category Switch "person" / "collection")**:
         - Quando la categoria non prevede filtri, la barra si allunga verso destra assorbendo i controlli.
         - Le icone sfumano progressivamente verso l'interno, il ponte liquido si richiude e la barra esegue un morbido assestamento elastico di arrivo a fine corsa ($p \rightarrow 0$).
      4. **Dinamica di Rigurgito e Distacco (Category Switch "movie" / "tv")**:
         - Quando i filtri ridiventano attivi, la barra si ritrae verso sinistra e "rigurgita" i controlli nella loro posizione di riposo.
         - Tra la calotta della barra che si ritrae e il cerchio del pulsante si stira un ponte viscoelastico con strozzatura progressiva del punto vita.
         - Alla soglia di distacco positivo ($p = 0.88$), il filamento si spezza: il pulsante filtri subisce un rimbalzo elastico a molla orizzontale (`bounceOffsetX`) e di scala (`bounceScale`), e la barra subisce un rinculo smorzato a sinistra (`searchBounceX`), rivelando le icone e l'eventuale anello d'accento attivo.
      5. **Eliminazione del Doppio Alpha e Fusione Monolitica (`CompositingStrategy.Offscreen`)**:
         - Disegnare sia la barra di ricerca a capsula che il ponte liquido all'interno del medesimo `Canvas` configurato con `graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, alpha = 0.05f)` e colore `onSurface` solido ad alpha $1.0$.
         - È tassativamente vietato applicare un `.background(symbioteColor)` separato al `Box` della barra di ricerca: l'intersezione tra due geometrie semi-trasparenti moltiplica l'opacità ($0.05 + 0.05 \times (1-0.05) \approx 0.0975$), generando una sagoma più chiara a cuneo all'interno della calotta della barra.
         - Con il buffer offscreen hardware, la sovrapposizione dei pixel nel buffer $1.0 \text{ over } 1.0$ produce rigorosamente $1.0$, scalato poi uniformemente ad `alpha = 0.05f` su tutta la sagoma composita, garantendo l'assoluta assenza di giunzioni o artefatti interni di chiaroscuro.
      6. **Geometria Sferica della Goccia ed Evitamento dell'Effetto Ottagono (Arco da 240° a 330°)**:
         - È tassativamente vietato ancorare il filamento liquido al vertice superiore ($cy - rDrop$) e inferiore ($cy + rDrop$) della goccia limitando `arcTo` al solo semicerchio frontale a $180^\circ$. Tale impostazione appiattisce il soffitto e il fondo della goccia in linee orizzontali rette che, congiungendosi al collo inclinato e alla calotta frontale, trasformano visivamente la sfera in un poligono a 8 faccette (ottagono).
         - Calcolare tassativamente l'angolo polare d'attacco del filamento sulla goccia $\theta \in [15^\circ, 60^\circ]$ tramite $\cos(\theta)$ e $\sin(\theta)$.
         - `arcTo` deve coprire l'arco sferico continuo da $-180^\circ + \theta$ con ampiezza $360^\circ - 2\theta$ (da $240^\circ$ fino a $330^\circ$ di cerchio), garantendo che la goccia rimanga un corpo rotondo perfettamente sferico in ogni istante dell'allungamento.
      7. **Soglia di Distacco Positiva & Divieto di Residuo del Filamento (`nubPath`)**:
         - La soglia di distacco deve scattare a $p = 0.88$, momento in cui il gap fisico tra barra e pulsante è rigorosamente positivo ($\ge 5.3\text{ dp}$), scongiurando qualsiasi compenetrazione geometrica a riposo.
         - È vietato disegnare nubi o corni triangolari residui (`nubPath`) sulla calotta della barra dopo lo snap: la tensione superficiale istantanea impone che entrambe le superfici ritornino cerchi perfetti senza spine o filamenti appuntiti residui.

- **Snellimento Scheda Dettaglio: Top Bar a 3 Pillole Espandibili Verso l'Alto & Trailer in Overlay Modale**:
    - **Progressive Disclosure per Informazioni Secondarie**: Le sezioni a consultazione occasionale (*Guida Genitori* e *Dettagli Tecnici*) non devono appesantire la colonna principale di scorrimento verticale, costringendo l'utente a lunghe scrollate. Vengono rimosse dal corpo della pagina e spostate a scomparsa nella barra superiore in `DetailHeader`.
    - **Fisica di Espansione Verso l'Alto (Upward Layout Placement)**:
      - Nei contenitori con espansione accordion in testata (come la barra fused di `DetailHeader`), l'espansione **non deve spingere verso il basso il resto della pagina** (`.animateContentSize()` vietato in questo contesto).
      - Deve adottare il layout modifier di shift verticale `placeable.place(0, -heightToSubtract)` con `stableHeight = placeable.height - heightToSubtract`: in questo modo il perimetro inferiore del container rimane perfettamente fermo ancorato ai controlli sottostanti, e l'espansione si proietta armonicamente verso l'alto sopra il backdrop sfumato.
    - **Barra Superiore Tri-Partita ad Espansione Accordion**:
      - La barra fused in testata ospita 3 pulsanti pillola compatti:
        1. *Rating TMDB (`RatingPill`)*: mantiene rigorosamente l'identità visiva originale (sfondo pieno `accentColor`, curvatura `48.dp`, microcopy "TAP FOR MORE" / "TAP FOR LESS", voto e stella). Cliccandola si espande la sezione aggregata dei rating esterni (IMDb, Rotten Tomatoes, Metacritic, Trakt, Box Office). Il Rated è rimosso da questa vista per non creare duplicazioni.
        2. *Rated / Guida Genitori (`HeaderRatedPill`)*: pillola dinamica con il codice di certificazione (es. `TV-14`, `PG-13`, `VM14`, `R`, `T`). Cliccandola si espande la Guida Genitori verso l'alto: la prima riga mostra la card di dettaglio specifica per quella classificazione (titolo, codice ed età consigliata), mentre le righe sottostanti mostrano i meter delle 5 categorie (`CategoryMeterRow`: Violenza, Sesso, Linguaggio, Sostanze, Scene Intense) con scorrimento verticale difensivo.
        3. *Dettagli Tecnici (`HeaderTechnicalPill`)*: pillola con icona ciak cinematografico (`ic_ciak`). Cliccandola si espande verso l'alto la griglia delle specifiche tecniche (Titolo originale, Status, Data di uscita, Budget, Lingua originale, Incassi, Produzione, Paese) con scorrimento interno.
      - Il tocco su ciascuna pillola commuta tra la sezione attiva e `DetailHeaderExpandedSection.NONE`, garantendo la mutua esclusività delle viste espanse e preservando il contenuto durante il fade-out (`activeDisplaySection`).
    - **Trailer Esclusivo in Overlay Modale (`DetailTrailerModal`)**:
      - La sezione statica carosello `DetailTrailers` viene rimossa dal corpo della scheda dettaglio.
      - Il tocco sull'icona Trailer nella barra d'azione superiore apre `DetailTrailerModal`, un overlay a tutto schermo con `hazeGlass(rootHazeState)`, YouTube Player incorporato, carosello orizzontale di chip per la selezione del video se multipli, e chiusura immediata via `ModalCloseButton` e `BackHandler`.
- **Invariante Skeleton Loader (Purezza Neutra e Divieto di Frankenstein View)**:
    - Uno skeleton screen deve rappresentare un'astrazione wireframe pulita, rigorosamente neutra e monocromatica (sfondi ardesia profondi e layer `Color.White` a bassissima opacità tra `0.03f` e `0.08f` con `shimmerEffect()`).
    - È tassativamente vietato renderizzare parzialmente dati reali precaricati (es. `AsyncImage` su backdrop/poster, tipografia reale per il titolo o gradienti colorati con `accentColor`) all'interno di uno skeleton loader. Unire asset reali e wireframe generici genera una vista ibrida confusa ("Frankenstein") che degrada la qualità visiva dell'esperienza utente. La transizione agli asset reali deve avvenire esclusivamente al passaggio definitivo a `DetailUiState.Success`.
- **Invariante Haze State per Modali Annidati, Tendine Flottanti e Mascheramento Bordi (Dual-Layer Haze Blur & Deep Tint)**:
    - Quando una tendina o un menu contestuale flottante (es. `DetailTrailersMenuModal`) risiede sopra i contenuti di un modale (es. `MorphGlassModal`), e tale menu può estendersi o sporgere oltre i confini del modale stesso sovrapponendosi allo schermo generale sottostante:
      1. **Scope Isolation**: Il modale deve istanziare un proprio stato Haze locale isolato: `val modalHazeState = remember { HazeState() }`, applicato con `.haze(state = modalHazeState, style = HazeStyles.PremiumDark)` sul container dei propri contenuti.
      2. **Dual-Layer Architecture**: La tendina flottante (posizionata come sibling all'esterno del nodo haze) campiona `rootHazeState` sul layer di sfondo inferiore e campiona `modalHazeState` sul layer superiore, garantendo blur a 360° senza buchi sia sopra il modale sia sullo schermo generale.
      3. **Direzione Dinamica (`openUpY`)**: Se il pulsante di innesco risiede nella metà inferiore dello schermo (`triggerY > screenHeight / 2`), la tendina deve aprirsi obbligatoriamente verso l'alto (`openUpY`), scongiurando overflow e clipping oltre il fondo dello schermo.
      4. **Mascheramento Bordi & Tinta Scura**: Per evitare che i contorni del modale sottostante traspaiano come aloni chiari o lattiginosi attraverso il vetro, il container del menu adotta una tinta scura densa (`Color(0xFF0C0C0F).copy(alpha = 0.94f)`) con `blurAlpha = 1f` su entrambi i layer, preservando un autentico frosted glass scuro.
- **Modali a Espansione Morfica (`isVisible` e `triggerBounds`)**:
    - Un modale che emerge da un pulsante fisico o icona contestuale (es. ciak trailer con `ic_ciak` / `ic_open_ciak`) non deve apparire bruscamente a schermo tramite `if (showModal)` se ciò distrugge l'interpolazione geometrica di entrata/uscita.
    - Il composable del modale deve rimanere montato e ricevere lo stato `isVisible` unitamente ai `triggerBounds` registrati tramite `onGloballyPositioned`, animando scala, alpha e curvatura a partire dalla sagoma esatta del pulsante di origine.
- **Centratura Ottica di Icone Direzionali (`ic_play`)**:
    - Le icone vettoriali triangolari orientate a destra (`ic_play`) possiedono un bounding box con asse centrale geometrico disallineato rispetto al baricentro visivo percepito.
    - Quando si posiziona `ic_play` al centro di una card (es. miniatura preview trailer 16:9), è vietato applicare offset positivi verso destra (`+1.dp`), che accentuano l'asimmetria spingendo la punta fuori dal centro.
    - Applicare sempre una lieve compensazione ottica a sinistra (`offset(x = (-1.5).dp)`) per allineare millimetricamente il baricentro percepito del triangolo con l'asse mediano della superficie.
- **Intervento Minimo su Animazioni Fisiche ed Elastiche Calibrate**:
    - Le animazioni procedurali viscoelastiche e a filamento fluido (es. distacco e fusione fra `MainSearchFab` e `MainScrollToTopFab`) costituiscono sistemi con calibrazione millimetrica altamente sensibile.
    - Quando viene richiesta la modifica di una coordinata o di un ancoraggio (es. abbassare la base del triangolo / `cyFab`), limitare l'intervento rigorosamente al parametro geometrico specificato. È severamente vietato alterare o riscrivere formule di recoil, curve di espansione o soglie di distacco senza esplicita richiesta dell'utente.
- **Congruenza Geometrica 1:1 tra Skeleton Placeholder e Componente Reale (`DetailHeader` / `DetailSkeleton`)**:
    - I blocchi shimmer di caricamento (`DetailSkeleton`) devono replicare esattamente le dimensioni, la proporzione e la forma dei componenti reali che andranno a sostituire.
    - Se un pulsante d'azione o chip è circolare (es. `HeaderTechnicalPill` a 36.dp con `CircleShape`), il corrispondente placeholder di caricamento deve adottare tassativamente `.size(36.dp).clip(CircleShape)`, vietando forme a pillola allungate (`width(62.dp)`) o dimensioni approssimate che generano sfarfallii visivi ("frankenstein layout") al completamento del caricamento.
- **I 3 Archetipi Canonici dei Modali (Morph vs Center vs BottomSheet)**:
    - **1. Modali da Pulsante (`MorphGlassModal`)**: Si aprono da un'ancora fisica (`triggerBounds: Rect`) ed espandono la superficie con morphing elastico dalle coordinate del tocco verso il centro schermo.
    - **2. Modali Centrati a Schermo Intero (`FlickTroveModal`)**: Appaiono direttamente a centro schermo con scala morbida (`0.92f ➔ 1.0f`) e dissolvenza.
    - **3. Fogli dal Basso (`FlickTroveBottomSheet`)**: Scivolano dal fondo dello schermo con `DragHandle` per contenuti lunghi e liste scorrevoli.
- **Parità Visiva Assoluta 1:1 tra Modali Morfici e Centrati (Single Surface Spec)**:
    - A regime aperto, la superficie dei modali di Tipo 1 (`MorphGlassModal`) e Tipo 2 (`FlickTroveModal`) deve essere **visivamente identica e indistinguibile**:
      - Stessa curvatura: `RoundedCornerShape(32.dp)`.
      - Stesso blur e tint: `HazeStyles.glassmorphicDialog` (`blurRadius = 24.dp`, `tint = Color.Black.copy(alpha = 0.58f)`).
      - Stesso bordo: `1.dp` con `Color.White.copy(alpha = HazeStyles.ModalBorderAlpha)`.
      - Stesso scrim di fondo: `Color.Black.copy(alpha = HazeStyles.ModalScrimAlpha)`.
    - L'unica differenza ammessa è la fisica di transizione (espansione da coordinate per il primo, scala elastica centrale per il secondo).
- **Divieto di Finestre Native (`androidx.compose.ui.window.Dialog`)**:
    - È tassativamente vietato creare modali aprendo una finestra nativa Android (`Dialog`). Questa frammentazione rompe il campionamento Haze e isola il modale dall'albero grafico. Tutti i dialog di sistema e cartelle devono risiedere in `FlickTroveModal` su `LocalModalHost`.
- **Global ModalHost (`LocalModalHost`) & Divieto di Scrim Multipli (Anti-Double-Dimming)**:
    - Tutti i modali di dialogo e selettori devono essere mostrati tramite il delegato root `LocalModalHost.current.show { ... }` montato ad apice gerarchia in `FlickTroveApp.kt` (`zIndex(250000f)`).
    - Lo scrim scuro nativo di `FlickTroveModal` copre già fisicamente il 100% dell'app window (incluse `GlassyTopBar`, `GlassyBottomBar` e `MainSearchFab`).
    - È tassativamente vietato riattivare contemporaneamente oscuramenti artificiali interni sulle singole barre (`animatedDimAlpha = 0.7f`) dai tab figli: tale duplicazione sommerebbe due veli scuri ($0.70 + 0.65$), causando un soffocamento quasi totale ($\sim 90\%$) della luminosità e rendendo le barre illeggibili.
- **Divieto Assoluto di `android.widget.Toast` & Toast Standard Glassmorphic (`UndoToast`)**:
    - È severamente vietato l'uso di `android.widget.Toast.makeText(...)` all'interno dell'applicazione.
    - I toast nativi di sistema rompono la coerenza visiva glassmorphism e dark mode di FlickTrove, oltre a generare su Android 13+ notifiche di sistema non coordinate e doppie.
    - Utilizzare sempre `val feedbackManager = rememberActionFeedbackManager()` ed emettere feedback tramite `feedbackManager?.emit(UiText.StringResource(R.string....))`, che attiva l'in-app `UndoToast` a molla elastica e sfondo frosted glass.
- **Contrasto Dinamico del Testo (`color.luminance() > 0.45f`)**:
    - Quando un pulsante, badge o contenitore adotta uno sfondo dinamico o personalizzabile dall'utente (es. `folderColor`, `accentColor`, palette estratta da artwork/backdrop):
      ```kotlin
      val textColor = if (containerColor.luminance() > 0.45f) Color.Black else Color.White
      ```
    - Questo previene testi bianchi su sfondi luminosi (giallo, ciano, lime, bianco) o testi scuri su sfondi scuri, garantendo leggibilità e contrasto WCAG AAA in ogni condizione cromatica.
- **Formula Concentricità Geometrica Angoli Annidati ($R_{in} = R_{out} - \text{Padding}$) & Divieto `CircleShape` su Bottoni Modali**:
    - **Pulsanti di Azione Orizzontali a Fondo Modale**:
      - Nei modali standard a raggio ampio (es. `FlickTroveModal` con $R = 32\text{dp}$), è vietato usare `CircleShape` per i pulsanti di azione a tutta larghezza disposti sul fondo: la sagoma a pillola piena dentro un rettangolo arrotondato genera distorsioni visive e "strozzature" diagonali asimmetriche a 45°.
      - Applicare rigorosamente la formula matematica di concentricità degli angoli annidati:
        $$R_{\text{interno}} = R_{\text{esterno}} - \text{Padding}$$
      - Per un modale con raggio $32\text{dp}$ e padding perimetrale uniforme di $16\text{dp}$ (`horizontal = 16.dp, bottom = 16.dp`), i pulsanti d'azione orizzontali devono adottare tassativamente `RoundedCornerShape(16.dp)` ($32\text{dp} - 16\text{dp} = 16\text{dp}$).
    - **Elementi Sagomati e Icon Container negli Angoli Superiori (Quick Action BottomSheet & Card)**:
      - Quando un elemento interno (badge, icona quadrata/rettangolare, card figlia) risiede nell'angolo di un container con bordi arrotondati (es. `QuickFolderBottomSheet`, `QuickRatingBottomSheet`, `QuickNoteBottomSheet` con outer radius $32.dp$):
        1. Il padding orizzontale e verticale nell'angolo deve essere rigorosamente uguale ($P_{start} = P_{top} = P$, es. `16.dp`).
        2. Il raggio di curvatura dell'elemento interno deve seguire la formula $R_{inner} = R_{outer} - P$.
        3. *Esempio*: per un container con $R_{outer} = 32.dp$ e padding $16.dp$, il raggio dell'icon container interno deve essere tassativamente $16.dp$ ($32 - 16 = 16$), mai valori disallineati come $14.dp$ o $20.dp$ che appaiono eccentrici o deformati.
- **Riuso del Selettore Fisico (`CategoryTabSelector`)**:
    - Per qualsiasi tab bar o selettore segmentato all'interno di schermate, pannelli o modali, utilizzare il componente canonico `CategoryTabSelector` con `fillWidth = true` (e `uppercase = false` se necessario), ereditando la fisica inerziale asimmetrica unificata (jelly spring, flight dynamics e gesture drag).
- **Dissolvenza Unificata Contenitore + Contenuto e Rimpicciolimento Testo su Scroll (Anti-Ghost Box)**:
    - Quando un elemento comprimibile o collassabile varia la sua altezza e trasparenza durante lo scorrimento verticale (es. card di aggiunta cartelle, barra di ricerca, descrizione lista):
      1. **Anti-Ghost Box**: Il modificatore `graphicsLayer { alpha = alphaProgress }` deve essere applicato tassativamente al **contenitore esterno** (che include sfondo e bordo), e **non** soltanto al testo interno. Se applicato solo al testo, il bordo e lo sfondo del box rimangono visibili come una scatola vuota fantasma opaca.
      2. **Rimpicciolimento Testo e Ancoraggio (`textScale` & `TransformOrigin`)**: Contemporaneamente alla dissolvenza d'alfa e alla riduzione di altezza del contenitore, scalare la dimensione visiva del testo e delle icone interne con `textScale = 0.75f + 0.25f * alphaProgress`, ancorato tramite `TransformOrigin` idoneo (`Center` per le card, `(0f, 0.5f)` per barre orizzontali, `(0f, 0f)` per blocchi testo a scendere), azzerando salti visivi e donando un collasso tridimensionale naturale.
      3. **Curva di Dissolvenza Anticipata a Metà Corsa (`alpha = 0` a `progress = 0.5f`)**: Per evitare che testi e contorni rimangano visibili schiacciati a metà altezza, calcolare tassativamente `alphaProgress = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)`. L'elemento diventa completamente trasparente già al 50% dell'altezza, lasciando la restante metà dello scorrimento per il collasso dello spazio fisico senza ingombri visivi residui.
- **Morphing Dimensionale dei Modali su Cambio Tab (`AnimatedContent` + `SizeTransform` + `verticalFadingEdges`)**:
    - Nei modali o drawer con tab interni di altezza eterogenea (es. `FolderMembersModal` con lista membri vs inviti attivi):
      1. Racchiudere il selettore e la lista all'interno di `AnimatedContent` configurato con transizione morphing di dimensione:
         ```kotlin
         transitionSpec = {
             fadeIn(animationSpec = tween(220, delayMillis = 60)) togetherWith
                 fadeOut(animationSpec = tween(160)) using
                 SizeTransform(
                     clip = true,
                     sizeAnimationSpec = { _, _ ->
                         spring(dampingRatio = 0.82f, stiffness = 380f)
                     }
                 )
         }
         ```
      2. Non imporre altezze minime rigide (es. `min = 180.dp`) che creano spazi vuoti asimmetrici quando la lista ha un solo elemento; consentire il collasso naturale con vincolo superiore proporzionato (`heightIn(max = 340.dp)`).
      3. Applicare `Modifier.verticalFadingEdges(topEdgeHeight = 20.dp, bottomEdgeHeight = 20.dp)` su ciascuna lista scrollabile con proprio `LazyListState` dedicato, assicurando gradienti di dissolvenza morbidi sia in testa che in coda.

### Pattern Statistiche e Podio

- **No Ridondanze col Podio**: Quando è presente un podio per i primi 3 elementi (oro, argento, bronzo), la lista scorrevole sottostante deve sempre escluderli (`items.drop(3)`).
- **Liste Annidate e Maschere Gradiente**: Le liste con scroll interno in schermate scorrevoli devono includere sfumature ai bordi (gradiente `BlendMode.DstIn`) e consumare lo scroll residuo (`nestedScroll`) per non far sobbalzare la pagina genitore durante lo swipe.
- **Superfici Glassmorphic per Badge, Stemmi e Distribuzioni (Divieto Colori Solidi Scuri)**:
   - Nelle sezioni di statistiche, grafici, caroselli o distribuzioni (es. loghi delle case cinematografiche in `StudioDistributionSection`, generi, decadi), è tassativamente vietato racchiudere loghi o stemmi in container con sfondi opachi/solidi scuri o tinte arbitrarie (es. `Color(0xFF161922)`).
   - Utilizzare rigorosamente le superfici glassmorphic canoniche (`CircleShape`, `background(Color.White.copy(alpha = 0.06f))`, `border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)`), preservando la trasparenza autentica, il blur e la coerenza cromatica con lo sfondo cinematico dell'app.

### Home Widget & Promemoria Uscite (FlickTroveWidget)

- **Semantica Rigorosa Reminder & Anti-Anacronismo**:
   - Il widget singolo per i titoli in arrivo (`FlickTroveWidget`) deve filtrare esclusivamente i film salvati con reminder attivo (`movie.reminder == true && !movie.watched`) la cui data di uscita sia futura o odierna (`releaseDate >= todayIso`).
   - È tassativamente vietato introdurre fallback generici verso film già usciti o verso la watchlist generica.
   - Quando non ci sono titoli in uscita salvati con reminder, mostrare un empty state curato: icona calendario (`ic_calendario`), testo localizzato `widget_no_releases` e pillola o sfondo dark coerente.
   - Per le date future, mostrare un badge a pillola moderno (`cornerRadius(12.dp)`) con data localizzata, senza prefissi verbali ridondanti (es. "Releasing on:").
