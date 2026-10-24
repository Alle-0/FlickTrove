# FlickTrove Guidelines: Strumenti, Tooling & Piattaforma

### Localizzazione & Dizionari Sottogeneri

- **Franchise e Termini Cinematografici Universali**:
   - Per franchise globali (es. *MCU*, *DCEU*) e sottogeneri cinematografici universalmente riconosciuti in lingua inglese (es. *Body Horror*, *Stop-Motion*, *Cyberpunk*, *Found Footage*, *Slasher*), non forzare traduzioni letterali o ridondanti nelle lingue che adottano correntemente i termini in originale.
- **Integrità Dizionari Metadati & Verifica Canonica TMDB (Anti-Duplication Guardrail)**:
   - **Verifica Preventiva di Esistenza**: Prima di inserire nuove chiavi in dizionari o mappe di lookup (`KeywordDictionary.kt`), verificare sempre che il termine o il suo sinonimo non sia già presente nella mappa. Vietato accodare ciecamente elementi (`mapOf` sovrascrive silenziosamente o genera duplicati).
   - **Verifica Canonica Endpoint TMDB**: Non inventare o assumere ID numerici TMDB a memoria o da snippet web non ufficiali (rischio di associare ID errati). Interrogare sempre l'endpoint ufficiale `/search/keyword` usando la chiave API di progetto per ottenere l'ID numerico esatto e la grafia canonica.
   - **Simmetria 1:1 e Chiavi Univoche**: Mantenere perfetta simmetria tra i dizionari linguistici (`italianToTmdbKeywordIds` ed `englishToTmdbKeywordIds`) condividendo gli stessi ID TMDB. Le varianti di scrittura dello stesso concetto (es. `aftercreditsstinger` vs `after credits stinger`) devono avere chiavi stringa distinte per intercettare qualsiasi forma restituita da TMDB.
- **Allineamento Simultaneo delle Risorse Stringa nelle 15 Lingue (Parità & Formattazione XML)**:
   - **Copertura Contestuale a 15 Lingue**: Ogni volta che vengono introdotte nuove chiavi di testo o modificate chiavi esistenti per componenti UI, dialoghi o impostazioni, aggiornare tassativamente e contestualmente tutti i 15 file di risorse localizzate (`values`, `values-it`, `values-de`, `values-es`, `values-fr`, `values-pt-rBR`, `values-ru`, `values-tr`, `values-ja`, `values-ko`, `values-zh`, `values-zh-rCN`, `values-id`, `values-in`, `values-hi`).
   - **Escaping Obbligatorio di Apostrofi ed Entità XML**:
     - Gli apostrofi singoli `'` non racchiusi da virgolette doppie esterne causano l'errore fatale di build AAPT2 (`AAPT: error: unescaped apostrophe in string`). Eseguire sempre l'escape: `\'` (es. `L\'attesa`, `Don\'t miss out`).
     - Le e-commerciali devono essere espresse rigorosamente come entità XML `&amp;` (vietato il carattere crudo `&`).
   - **Parità Rigorosa degli Specifier di Formattazione (`%1$s`, `%2$d`)**:
     - Quando una stringa del file base `values/strings.xml` contiene argomenti posizionali o numerati (es. `%1$s`, `%2$d`), tutte le 14 traduzioni localizzate devono contenere **esattamente gli stessi specifier**, con lo stesso tipo (`d` per interi, `s` per stringhe).
     - La mancanza o la mutazione di tipo di uno specifier (es. trasformare `%d` in `%s` o omettere `%2$s`) causa crash fatali a runtime (`UnknownFormatConversionException` o `MissingFormatArgumentException`) durante l'invocazione di `stringResource(...)`.
   - **Prevenzione Chiavi Orfane**:
     - Quando una stringa viene eliminata o rinominata in Kotlin, rimuoverla contestualmente da tutti i 15 file per evitare l'accumulo di debito tecnico e stringhe orfane.

### Legal & Asset Attribution Invariants

- **Sincronizzazione Attribuzioni Risorse Esterne (Regola dei 5 Touchpoint)**:
   - Quando vengono introdotte o aggiornate risorse grafiche, icone o dati di terze parti soggetti a licenza con obbligo di attribuzione (es. Game-Icons.net sotto licenza CC BY 3.0 by Lorc, Delapouite, etc.):
   - L'attribuzione formale con URL diretto deve essere tassativamente sincronizzata su tutti e **5 i touchpoint** del progetto:
     1. **In-App Attributions Modal** (`CreditsDialog.kt` / `CreditsSection`)
     2. **README.md** del repository
     3. **Privacy Policy (PP)**
     4. **Terms of Service (ToS)**
     5. **Sito Web / Documentazione pubblica** (`docs/`, landing page)
   - Nessun rilascio o commit di nuove icone/asset è considerato completo senza l'allineamento contestuale di questi 5 canali.

### Best Practice di Sviluppo e Strumenti

- **Uso Tool Nativi IDE**: Per ricerca e ispezione codice, preferire sempre i tool dedicati (`grep_search`, `view_file`, `replace_file_content`) evitando comandi shell grezzi come `findstr` o `grep`.
- **Vincolo PowerShell Windows & Quoting di `$_`**:
   - Non utilizzare mai l'operatore `&&` per concatenare comandi nella shell PowerShell.
   - Quando si eseguono script PowerShell inline contenenti la variabile di pipeline `$_` (es. `ForEach-Object { $_.FullName }` o `Where-Object { $_.Lines -gt 700 }`), racchiudere l'intero blocco sempre tra **apici singoli** (`powershell -Command '...'`). L'uso di virgolette doppie (`"..."`) provoca l'espansione errata di `$_` nella shell di invocazione, corrompendo il comando prima dell'esecuzione.
- **Generazione Baseline Profile e Firme APK**:
   - Il task `:app:generateBaselineProfile` compila la variante `nonMinifiedRelease`, la cui firma digitale differisce da quella di Debug.
   - Prima di avviare la generazione del profilo su emulatore o dispositivo, rimuovere sempre preventivamente le build esistenti (`adb uninstall com.cinetrack` e `adb uninstall com.cinetrack.baselineprofile`) per prevenire l'errore `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Risoluzione Blocchi KSP su Windows**: Se la compilazione Gradle (es. `:app:kspDebugKotlin`) entra in un loop infinito o fallisce misteriosamente con `NoSuchFileException` (specialmente dopo modifiche allo schema di Room), **non cercare bug nel codice**. La cache di KSP si è corrotta o bloccata. La procedura tassativa è: fermare i demoni con `./gradlew --stop`, eliminare brutalmente le cartelle di build (`Remove-Item -Recurse -Force app/build -ErrorAction SilentlyContinue`) e lanciare un `./gradlew clean build`.
- **Gesture Complesse in Compose (Drag & Tap)**: Per implementare gesture fluide che combinano tocchi e trascinamenti (es. drag-to-rate bar), evitare API a basso livello come `awaitEachGesture` o `awaitFirstDown` che spesso generano errori di risoluzione a seconda della versione di Compose. Concatenare sempre i modificatori standard in blocchi separati: `.pointerInput(Unit) { detectTapGestures { ... } } .pointerInput(Unit) { detectDragGestures { ... } }`.
- **Modifica di File con Forte Annidamento (Anti-Regex Trap)**:
   - Evitare assolutamente l'uso di script Python basati su Regex complesse o algoritmi grezzi di conteggio parentesi per estrarre o spostare blocchi di codice in file Jetpack Compose molto grandi e annidati. Questi approcci falliscono e corrompono l'AST (es. `Syntax error: Expecting '}'`) a causa delle variazioni di indentazione e di componenti omonimi ripetuti (es. molteplici `Box(56.dp)`).
   - Utilizzare esclusivamente i tool nativi (`replace_file_content`), assicurandosi di selezionare un `TargetContent` univoco. Se un'operazione corrompe la sintassi, usare `git checkout` per ripristinare il file piuttosto che tentare di riparare manualmente parentesi graffe mancanti.
   - **Anti-Script Rule**: Non creare mai script Python (o PowerShell) per eseguire sostituzioni in bulk su file sorgente Kotlin. Anche per modifiche su più file, usare esclusivamente `multi_replace_file_content` o `replace_file_content`. L'utente non deve essere costretto a eseguire script intermedi.
- **Priorità Feedback Visivo & Divieto Log-Spamming**:
   - Quando l'utente fornisce un feedback visivo immediato (es. screenshot ingrandito o indicazione di difetto UI come padding/radius), è vietato lanciare raffiche di comandi PowerShell/Bash per consultare i file JSONL di log o trascrizioni passate. L'attenzione deve convergere istantaneamente sull'analisi visiva dell'immagine e sull'ispezione mirata del codice sorgente del componente interessato.
- **Risoluzione Rapida "No Internet" su Emulatore Android (Blocco DNS Virtuale)**:
    - Se l'emulatore Android su Windows mostra il Wi-Fi connesso ma senza accesso a internet (`AndroidWifi: Connected, no internet`), il router virtuale QEMU (`10.0.2.3`) non riesce a risolvere i domini host.
    - Anziché eseguire un distruttivo `Wipe Data` o chiudere l'emulatore, forzare il Private DNS di Google tramite ADB:

      ```powershell
      & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "settings put global private_dns_mode hostname; settings put global private_dns_specifier dns.google; svc wifi disable; sleep 1; svc wifi enable"
      ```

      Questo commuta immediatamente la rete sullo stato `IS_VALIDATED` con DNS sicuro over TLS (`8.8.8.8`), ripristinando istantaneamente la connettività di tutte le API.
- **Divieto Assoluto di Reflection nei Callback di Layout e Animazione (Anti-ANR)**:
   - È tassativamente vietato eseguire ispezioni riflessive (`Class.getDeclaredMethod`, `Class.getDeclaredField`, attraversamenti ricorsivi della gerarchia delle classi) all'interno di callback di layout o cicli di disegno (`OnLayoutChangeListener`, `ViewTreeObserver`, Compose `layout`, blocchi eseguiti ad ogni frame).
   - Durante le animazioni (es. espansione di un modale a 60fps), i listener di layout vengono scatenati decine di volte al secondo: eseguire reflection o catturare eccezioni ripetute genera centinaia di migliaia di fault di sistema (`>600k page faults`), saturando la CPU e causando un blocco irreparabile del main thread (**ANR**: `Input dispatching timed out waiting for MotionEvent`).
   - Per personalizzare o ritagliare elementi UI terzi, affidarsi esclusivamente ad API native e accelerate in hardware (`ViewOutlineProvider`, `clipToOutline = true`, `GradientDrawable`) e a controlli di tipo istantanei $O(1)$ (`view as? TargetView`).
- **Ciclo di Rilascio FlickTrove_Admin & Regole PowerShell su Windows**:
    - Ogni volta che vengono modificati file HTML, CSS o JS in `c:\Progetti\FlickTrove_Admin\src\`:
      1. Eseguire la compilazione di produzione: `npm run build`.
      2. Eseguire il deploy live immediato: `Push-Location 'C:\Progetti\FlickTrove_Admin'; firebase deploy --only hosting; Pop-Location`.
    - **Quoting PowerShell**: Quando si passano argomenti separati da virgola al flag `--only` (es. `hosting,firestore:rules`), racchiudere tassativamente la stringa tra apici singoli (`firebase deploy --only 'hosting,firestore:rules'`) per impedire a PowerShell di interpretare la virgola come separatore di array e fallire la build.
- **Conversione Asset Vettoriali APS→Android (Script Python)**:
    - Quando l'utente fornisce un file `.xml` in formato Microsoft APS/XPS (tag `<Aps>`, `<Page>`, `<PathFigure>`, `<PolyLine>`, `<Bezier>`), la procedura di conversione in Android Vector Drawable è sempre la stessa:
      1. Individuare il `<Path>` con `<Pen>` (non il `<Clip>`) e leggere i suoi `<PathFigure>`.
      2. Applicare `RenderTransform="1 0 0 -1 0 H"`: `x' = x`, `y' = H - y` (dove H è `Page Height`).
      3. Convertire `<PolyLine>` in comandi SVG `M/L` e `<Bezier>` in comandi `C`.
      4. Campionare ogni curva Bézier (11 punti) per trovare bounding box preciso; espandere di `strokeWidth/2` per includere lo stroke.
      5. Traslare tutte le coordinate di `(-minX, -minY)` → viewport parte da `(0,0)`.
      6. Scrivere il `<vector>` con `viewportWidth = maxX-minX`, `viewportHeight = maxY-minY` (nessun padding extra, nessun `<group translateX>`).
      7. Usare namespace `xmlns:ns1="http://schemas.android.com/aapt"` e `<ns1:attr name="android:strokeColor">` per il gradiente inline.

### Gestione Git, Customizzazioni e Skill Locali (.agents/)

- **Isolamento delle Skill e Configurazioni Locali**:
   - La cartella `.agents/` contiene skill, template e runbook locali per l'agente.
   - Deve rimanere tassativamente in `.gitignore` e NON deve mai essere inclusa nei commit o nei push su GitHub senza esplicita richiesta dell'utente.
