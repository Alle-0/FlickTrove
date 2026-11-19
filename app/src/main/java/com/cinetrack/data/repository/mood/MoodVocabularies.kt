package com.cinetrack.data.repository.mood

import java.util.Locale

/**
 * Static vocabulary, stopwords, animal triggers, and franchise dictionaries
 * used by LocalMoodSearchEngine.
 */
object MoodVocabularies {

    val animalAndNonHumanTriggers: Set<String> = setOf(
        // Italian
        "topo", "topi", "topolino", "topolina", "topolini", "ratto", "ratti", "roditore", "roditori",
        "pesce", "pesci", "pesciolino", "squalo", "squali", "balena", "balene", "delfino", "delfini",
        "leone", "leoni", "leoncino", "tigre", "tigri", "pantera", "pantere", "leopardo",
        "cane", "cani", "cagnolino", "cucciolo", "cuccioli", "gatto", "gatti", "gattino", "gattini",
        "uccello", "uccelli", "uccellino", "aquila", "aquile", "falco", "gufo", "gufi", "pappagallo", "pappagalli",
        "orso", "orsi", "orsetto", "volpe", "volpi", "lupo", "lupi", "lupacchiotto",
        "coniglio", "conigli", "coniglietto", "lepre",
        "panda", "elefante", "elefanti", "giraffa", "giraffe", "ippopotamo",
        "scimmia", "scimmie", "gorilla", "scimpanze",
        "dinosauro", "dinosauri", "drago", "draghi",
        "insetto", "insetti", "ape", "api", "formica", "formiche", "coccinella", "ragno", "ragni",
        "animale", "animali", "animaletto", "animaletti", "bestia", "bestie",
        "giocattolo", "giocattoli", "pupazzo", "pupazzi", "burattino", "burattini",
        "robot", "mostro", "mostri", "mostriciattolo",

        // English
        "mouse", "mice", "rat", "rats", "rodent", "rodents",
        "fish", "fishes", "shark", "sharks", "whale", "whales", "dolphin", "dolphins",
        "lion", "lions", "tiger", "tigers", "panther", "leopard",
        "dog", "dogs", "puppy", "puppies", "cat", "cats", "kitten", "kittens",
        "bird", "birds", "eagle", "hawk", "owl", "parrot",
        "bear", "bears", "fox", "foxes", "wolf", "wolves",
        "rabbit", "rabbits", "bunny", "bunnies", "hare",
        "panda", "elephant", "elephants", "giraffe",
        "monkey", "monkeys", "gorilla", "chimpanzee",
        "dinosaur", "dinosaurs", "dragon", "dragons",
        "insect", "insects", "bug", "bugs", "bee", "bees", "ant", "ants", "spider", "spiders",
        "animal", "animals", "creature", "creatures", "beast", "beasts",
        "toy", "toys", "puppet", "puppets",
        "robot", "robots", "monster", "monsters"
    )

    fun detectAnimalOrNonHumanSubject(query: String): Boolean {
        val tokens = Regex("""\b[\w-]+\b""").findAll(query.lowercase(Locale.ROOT)).map { it.value.trim('-') }
        return tokens.any { animalAndNonHumanTriggers.contains(it) }
    }

    val stopwordsIt: Set<String> = setOf(
        "un", "uno", "una", "dei", "degli", "delle", "il", "lo", "la", "i", "gli", "le",
        "in", "su", "a", "da", "con", "senza", "per", "tra", "fra", "di", "del", "dello", "della",
        "sul", "sullo", "sulla", "sui", "sugli", "sulle", "nel", "nello", "nella", "nei", "negli", "nelle",
        "dal", "dallo", "dalla", "dai", "dagli", "dalle", "al", "allo", "alla", "agli", "alle", "col",
        "che", "chi", "cui", "dove", "come", "quando", "cosa", "perché", "perche",
        "film", "cinema", "pellicola", "storia", "racconto", "tipo", "stile", "genere", "simile",
        "vorrei", "vedere", "consigliami", "cercami", "mostrami",
        "trovami", "consigli", "qualcosa", "bello", "belli", "famoso", "famosi", "migliori", "migliore",
        "diventa", "diventare", "diventano", "essere", "stato", "sono", "ha", "hanno", "chi", "quale", "quali",
        "persone", "persona", "gente", "qualcuno", "alcuni", "alcune", "uomo", "uomini", "donna", "donne",
        "ragazzo", "ragazzi", "ragazza", "ragazze", "tutti", "tutto", "tutta", "tutte", "molti", "molte",
        "ogni", "ciascuno", "altro", "altri", "altre", "loro", "essi", "esse", "cose", "fanno", "fa", "fare", "guarda", "guardare",
        "parte", "parti", "partire", "partono", "partito", "partita", "partiti", "partite",
        "va", "vanno", "andare", "andato", "andata", "andati", "andate",
        "fa", "fanno", "fare", "fatto", "fatta",
        "inizia", "iniziano", "iniziare", "iniziato", "iniziata",
        "sta", "stanno", "stare", "sembra", "sembrano", "sembrare",
        "arriva", "arrivare", "arrivano", "arrivato",
        "esce", "uscire", "escono", "uscito",
        "porta", "portare", "portano", "portato",
        "trova", "trovare", "trovano", "trovato",
        "cerca", "cercare", "cercano", "cercato",
        "scopre", "scoprire", "scoprono", "scoperto",
        "inizia", "iniziare", "iniziano", "iniziato",
        "prova", "provare", "provano", "provato",
        "torna", "tornare", "tornano", "tornato", "tornerà", "tornera",
        "lascia", "lasciare", "lasciano", "lasciato",
        "vive", "vivere", "vivono", "vissuto",
        "mette", "mettere", "mettono", "messo",
        "decide", "decidere", "decidono", "deciso",
        "vuole", "volere", "possono", "potere", "dovrebbe"
    )

    val stopwordsEn: Set<String> = setOf(
        "a", "an", "the", "in", "on", "at", "to", "for", "with", "without", "by", "from",
        "about", "into", "through", "of", "movie", "movies", "film", "films", "cinema",
        "flick", "flicks", "show", "story", "something", "like", "type", "kind", "style",
        "similar", "want", "looking", "recommend", "find", "best", "good", "great", "top",
        "that", "this", "these", "those", "who", "whom", "whose", "which", "what", "where",
        "when", "why", "how", "become", "becomes", "became", "becoming", "turned", "turns",
        "turning", "get", "gets", "got", "getting", "is", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "having", "do", "does", "did", "doing", "can", "could",
        "will", "would", "shall", "should", "may", "might", "must",
        "some", "people", "someone", "somebody", "anyone", "anybody", "everyone", "everybody",
        "person", "persons", "man", "men", "woman", "women", "boy", "boys", "girl", "girls",
        "guy", "guys", "folk", "folks", "they", "them", "their", "theirs", "one", "ones",
        "many", "much", "few", "all", "any", "every", "other", "others", "lot", "lots",
        "bunch", "thing", "things", "stuff", "watch", "watching", "see", "seeing",
        "brings", "bring", "brought", "finds", "find", "found",
        "seeks", "seek", "sought", "discovers", "discover", "discovered",
        "begins", "begin", "begun", "tries", "try", "tried",
        "returns", "return", "returned", "leaves", "leave", "left",
        "part", "parts", "departs", "depart", "departing", "departed",
        "goes", "go", "going", "gone", "starts", "start", "starting", "started",
        "stays", "stay", "staying", "seems", "seem", "seeming",
        "arrives", "arrive", "arriving", "arrived",
        "lives", "live", "lived", "decides", "decide", "decided"
    )

    val franchiseSignatures: Map<String, List<String>> = mapOf(
        "james_bond" to listOf("007", "james bond", "casino royale", "skyfall", "spectre", "no time to die", "quantum of solace", "goldeneye", "dr. no", "goldfinger"),
        "mission_impossible" to listOf("mission: impossible", "mission impossible", "ethan hunt"),
        "kingsman" to listOf("kingsman"),
        "men_in_black" to listOf("men in black", "mib"),
        "mcu_antman" to listOf("ant-man", "antman"),
        "mcu_spiderman" to listOf("spider-man", "spiderman", "far from home", "homecoming", "no way home"),
        "mcu_avengers" to listOf("avengers", "endgame", "infinity war"),
        "star_wars" to listOf("star wars", "l'impero colpisce", "il ritorno dello jedi", "la minaccia fantasma"),
        "star_trek" to listOf("star trek"),
        "harry_potter" to listOf("harry potter", "animali fantastici"),
        "fast_furious" to listOf("fast & furious", "fast and furious", "tokyo drift"),
        "john_wick" to listOf("john wick"),
        "ocean" to listOf("ocean's eleven", "ocean's twelve", "ocean's thirteen", "ocean's 8"),
        "die_hard" to listOf("die hard", "trappola di cristallo"),
        "alien" to listOf("alien", "aliens", "prometheus", "covenant", "alien: romulus"),
        "terminator" to listOf("terminator", "il giorno del giudizio"),
        "jurassic" to listOf("jurassic park", "jurassic world"),
        "the_matrix" to listOf("matrix", "the matrix"),
        "lord_of_the_rings" to listOf("lord of the rings", "signore degli anelli", "lo hobbit", "the hobbit")
    )

    // Genre guards for short/ambiguous franchise triggers (length <= 8).
    // If the candidate's genres don't overlap with these, it's NOT part of the franchise.
    val franchiseGenreGuards: Map<String, Set<Long>> = mapOf(
        "alien"        to setOf(27L, 878L, 28L),
        "the_matrix"   to setOf(878L, 28L, 12L),
        "star_wars"    to setOf(878L, 12L, 28L, 14L),
        "star_trek"    to setOf(878L, 12L, 28L),
        "terminator"   to setOf(878L, 28L, 27L),
        "jurassic"     to setOf(878L, 28L, 12L, 27L),
        "mcu_avengers" to setOf(28L, 878L, 12L),
        "mcu_spiderman" to setOf(28L, 878L, 12L),
        "ocean"        to setOf(80L, 35L, 28L)
    )
}
