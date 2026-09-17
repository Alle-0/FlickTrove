# FlickTrove Guidelines & Product Rules

### Valutazione Feature e Product Design (Anti-Feature Trap)
Quando valuti nuove feature, suggerimenti degli utenti o modifiche architetturali:
1. **Non limitarti alla fattibilità tecnica immediata**: Il fatto che una modifica "si possa fare in 10 righe di codice" non significa che abbia senso a livello di prodotto.
2. **Preserva la Single Source of Truth**: Gli stati fondamentali del media tracker (Visto/Non visto, Preferito, Watchlist, Rating) devono rimanere sacri e coerenti ovunque nell'app. Mai creare "universi paralleli" o stati locali isolati per singola lista/cartella.
3. **Verifica la simmetria con i servizi Cloud (Trakt / SIMKL)**: Non introdurre modelli dati o stati proprietari che non possano essere rappresentati o sincronizzati fedelmente con le API esterne con cui l'app si integra.
4. **Considera le entità complesse (Serie TV vs Film)**: Non progettare comportamenti basandoti solo sui film se la feature rischia di collassare nella complessità di stagioni ed episodi.
5. **Previeni il Feature Bloat**: FlickTrove deve rimanere un'app scattante, pulita ed elegante. Abbi sempre il coraggio di dire "NO" alle feature che suonano bene sulla carta ma distruggono il modello mentale dell'utente o generano debito di design.
