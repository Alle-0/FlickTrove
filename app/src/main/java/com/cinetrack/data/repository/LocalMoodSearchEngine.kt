package com.cinetrack.data.repository

import com.cinetrack.data.api.TMDBService
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.KeywordDictionary
import com.cinetrack.data.model.Movie
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class LocalMoodSearchEngine @Inject constructor(
    private val tmdbService: TMDBService
) {

    // Thread-safe in-memory cache to eliminate network latency and prevent rate-limiting
    private val anchorDetailsCache = ConcurrentHashMap<String, Movie>()
    private val keywordResolutionCache = ConcurrentHashMap<String, Long>()

    private val animalAndNonHumanTriggers = setOf(
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

    private fun detectAnimalOrNonHumanSubject(query: String): Boolean {
        val tokens = Regex("""\b[\w-]+\b""").findAll(query.lowercase(Locale.ROOT)).map { it.value.trim('-') }
        return tokens.any { animalAndNonHumanTriggers.contains(it) }
    }

    private val stopwordsIt = setOf(
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

    private val stopwordsEn = setOf(
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

    data class ParsedQueryIntent(
        val positiveText: String,
        val negatedText: String?,
        val negatedGenres: List<Long>,
        val negatedKeywords: List<Long>,
        val negatedTokens: List<String>
    )

    data class CinematicArchetype(
        val id: String,
        val nameIt: String,
        val nameEn: String,
        val triggersIt: List<String>,
        val triggersEn: List<String>,
        val primaryGenres: List<Long>,
        val secondaryGenres: List<Long> = emptyList(),
        val mandatoryKeywords: List<Long> = emptyList(),
        val secondaryKeywords: List<Long> = emptyList(),
        val negativeGenres: List<Long> = emptyList(),
        val anchorMovieIds: List<Long> = emptyList(),
        val rationaleTemplateIt: String,
        val rationaleTemplateEn: String
    )

    data class AtmosphericModifier(
        val id: String,
        val labelIt: String,
        val labelEn: String,
        val triggersIt: List<String>,
        val triggersEn: List<String>,
        val keywordIds: List<Long> = emptyList(),
        val genreBonus: List<Long> = emptyList(),
        val genrePenalty: List<Long> = emptyList()
    )

    private val archetypeCatalog: List<CinematicArchetype> = listOf(
        // --- VIAGGIO DELL'EROE, ODISSEA & RITORNO A CASA ---
        CinematicArchetype(
            id = "hero_quest_journey_home",
            nameIt = "Viaggio dell'Eroe & Ritorno a Casa",
            nameEn = "The Hero's Quest & Epic Journey Home",
            triggersIt = listOf(
                "viaggio epico", "epico", "epica", "odissea", "viaggio verso casa",
                "ritorno a casa", "viaggio a casa", "porta a casa", "viaggio dell'eroe",
                "missione epica", "avventura epica", "lungo viaggio per tornare", "strada di casa"
            ),
            triggersEn = listOf(
                "epic journey", "epic quest", "odyssey", "journey home", "quest",
                "hero's journey", "epic adventure", "way back home", "epic voyage", "voyage home"
            ),
            primaryGenres = listOf(12L), // Adventure
            secondaryGenres = listOf(14L, 878L, 18L), // Fantasy, Sci-Fi, Drama
            mandatoryKeywords = listOf(209374L, 372264L, 9935L), // hero's journey, epic adventure, travel
            secondaryKeywords = listOf(188270L, 4379L, 3801L), // epic battle, time travel, space travel
            anchorMovieIds = listOf(120L, 122L, 157336L, 568L, 8358L, 424L), // LotR Fellowship, Return of the King, Interstellar, Apollo 13, Cast Away, O Brother
            rationaleTemplateIt = "Una grandiosa odissea di avventura e coraggio in cui il viaggio stesso trasforma l'eroe verso la salvezza o il ritorno a casa.",
            rationaleTemplateEn = "A majestic odyssey of discovery and courage where the journey itself transforms the hero in an epic quest toward home."
        ),

        // --- MAGIA, ILLUSIONISMO & PRESTIGIATORI ---
        CinematicArchetype(
            id = "magic_illusionist",
            nameIt = "Magia, Illusionismo & Prestigiatori",
            nameEn = "Magicians & Master Illusionists",
            triggersIt = listOf("mago", "maghi", "prestigiatore", "prestigiatori", "illusionista", "illusionisti", "prestidigitazione", "trucco di magia", "trucchi di magia", "grande illusione", "spettacolo di magia", "prestige style", "the prestige", "gioco di prestigio"),
            triggersEn = listOf("magician", "magicians", "illusionist", "illusionists", "prestidigitation", "magic trick", "magic tricks", "stage magic", "sleight of hand", "grand illusion", "prestige style", "the prestige"),
            primaryGenres = listOf(18L, 9648L, 53L),
            secondaryGenres = listOf(14L),
            mandatoryKeywords = listOf(11487L, 2343L, 10817L),
            secondaryKeywords = listOf(210024L, 9758L),
            anchorMovieIds = listOf(1124L, 1491L, 75656L, 291805L, 244506L, 14049L, 13997L),
            rationaleTemplateIt = "Un'ossessione viscerale per l'illusione perfetta, rivalità implacabili e trucchi di prestigio che sfidano la realtà.",
            rationaleTemplateEn = "Visceral obsession with the ultimate stage illusion, fierce rivalries, and prestidigitation that defies reality."
        ),

        // --- SPIONAGGIO & GADGET ---
        CinematicArchetype(
            id = "spy_gadgets",
            nameIt = "Spionaggio & High-Tech Gadget",
            nameEn = "Spy & High-Tech Espionage",
            triggersIt = listOf("spionaggio", "spia", "spie", "agente segreto", "agenti segreti", "agenzia segreta", "gadget", "gadget fantastici", "tecnologia spionaggio", "servizi segreti", "operazione segreta", "infiltrato", "infiltrati", "007", "licenza di uccidere"),
            triggersEn = listOf("spy", "spies", "secret agent", "secret agents", "secret agency", "gadgets", "spy gadgets", "espionage", "secret service", "covert ops", "undercover agent"),
            primaryGenres = listOf(28L, 53L),
            secondaryGenres = listOf(878L, 12L),
            mandatoryKeywords = listOf(4289L, 470L, 10683L),
            secondaryKeywords = listOf(14555L, 352434L, 3278L),
            anchorMovieIds = listOf(207703L, 607L, 56298L, 36557L, 203801L, 431693L, 816L),
            rationaleTemplateIt = "Classico dello spionaggio moderno con agenzie segrete, tecnologia avanzata e gadget letali.",
            rationaleTemplateEn = "Quintessential modern espionage featuring secret agencies, advanced tech, and clever gadgets."
        ),

        // --- TIME LOOP & PARADOSSI TEMPORALI ---
        CinematicArchetype(
            id = "time_loop",
            nameIt = "Time Loop & Paradossi Temporali",
            nameEn = "Time Loop & Paradoxes",
            triggersIt = listOf(
                "time loop", "loop temporale", "giorno che si ripete", "rivivere lo stesso giorno",
                "viaggio nel tempo", "viaggi nel tempo", "paradosso temporale", "macchina del tempo",
                "spazio-temporale", "spaziotemporale", "spazio temporale", "viaggio spazio-temporale",
                "viaggio spaziotemporale", "viaggi spazio-temporali", "viaggio temporale", "viaggi temporali",
                "viaggiatore nel tempo", "viaggiatori nel tempo", "linea temporale", "continuum temporale"
            ),
            triggersEn = listOf(
                "time loop", "repeating day", "reliving the same day", "time travel", "temporal paradox",
                "time machine", "groundhog day style", "space-time", "spacetime", "space-time travel",
                "spacetime travel", "temporal travel", "time traveler", "time travelers", "timeline"
            ),
            primaryGenres = listOf(878L),
            secondaryGenres = listOf(53L, 35L, 9648L),
            mandatoryKeywords = listOf(10854L, 4379L),
            secondaryKeywords = listOf(191132L, 207317L, 5455L),
            anchorMovieIds = listOf(105L, 137L, 137113L, 587792L, 45612L, 26466L, 122906L, 63L),
            rationaleTemplateIt = "Brillante rompicapo temporale in cui i protagonisti devono decifrare le regole di un continuum impazzito.",
            rationaleTemplateEn = "Brilliant temporal puzzle where characters must unravel the rules of a looping reality."
        ),

        // --- WHODUNIT & GIALLO DEDUTTIVO ---
        CinematicArchetype(
            id = "whodunit",
            nameIt = "Giallo Deduttivo (Whodunit)",
            nameEn = "Whodunit Murder Mystery",
            triggersIt = listOf("whodunit", "whodunnit", "giallo deduttivo", "chi è l'assassino", "delitto nella villa", "agatha christie", "cena con delitto", "omicidio misterioso", "detective geniale"),
            triggersEn = listOf("whodunit", "whodunnit", "murder mystery", "locked room mystery", "agatha christie style", "mansion murder", "who is the killer", "detective investigation"),
            primaryGenres = listOf(9648L),
            secondaryGenres = listOf(80L, 53L, 35L),
            mandatoryKeywords = listOf(12570L, 10714L, 703L),
            secondaryKeywords = listOf(9826L, 2908L),
            anchorMovieIds = listOf(546554L, 15196L, 392044L, 273248L, 5279L, 773867L),
            rationaleTemplateIt = "Giallo classico con un cast di sospettati d'eccezione, indizi disseminati e un finale sbalorditivo.",
            rationaleTemplateEn = "Classic deductive mystery with an ensemble of suspects, layered clues, and a shocking reveal."
        ),

        // --- CUCINA, CHEF & RISTORANTI ---
        CinematicArchetype(
            id = "culinary_chef",
            nameIt = "Alta Cucina & Ristoranti Stellati",
            nameEn = "Haute Cuisine & Obsessive Chefs",
            triggersIt = listOf("chef", "cucina", "ristorante", "ristorante stellato", "cuoco", "brigata di cucina", "cucina gourmet", "the menu style", "the bear style"),
            triggersEn = listOf("chef", "cooking", "restaurant", "haute cuisine", "fine dining", "kitchen brigade", "gourmet chef", "the menu style", "the bear style"),
            primaryGenres = listOf(18L),
            secondaryGenres = listOf(35L, 53L),
            mandatoryKeywords = listOf(10594L, 179431L, 207317L),
            secondaryKeywords = listOf(1701L),
            anchorMovieIds = listOf(593643L, 2062L, 286567L, 820067L, 212778L),
            rationaleTemplateIt = "La tensione febbrile e l'ossessione perversa per la perfezione tra i fornelli dell'alta gastronomia.",
            rationaleTemplateEn = "Feverish tension and the relentless obsession with culinary perfection inside high-end kitchens."
        ),

        // --- GENIO, MATEMATICA & OSSESSIONE MENTALE ---
        CinematicArchetype(
            id = "genius_obsession",
            nameIt = "Mente Brillante & Genio Ossessivo",
            nameEn = "Brilliant Minds & Obsession",
            triggersIt = listOf("genio", "mente brillante", "matematico", "matematica", "scienziato geniale", "talento prodigio", "scacchi", "ossessione intellettuale", "beautiful mind style"),
            triggersEn = listOf("genius", "brilliant mind", "mathematician", "prodigy", "chess master", "obsessive intellectual", "beautiful mind style", "good will hunting style"),
            primaryGenres = listOf(18L),
            secondaryGenres = listOf(36L, 9648L),
            mandatoryKeywords = listOf(10741L, 10817L, 11038L),
            secondaryKeywords = listOf(41329L),
            anchorMovieIds = listOf(453L, 489L, 37799L, 205596L, 11412L, 43900L),
            rationaleTemplateIt = "Il peso e la solitudine del talento assoluto, dove genialità intellettuale e fragilità umana si scontrano.",
            rationaleTemplateEn = "The profound weight of extraordinary talent, where intellectual mastery clashes with human vulnerability."
        ),

        // --- THRILLER CLAUSTROFOBICO & SINGLE-LOCATION ---
        CinematicArchetype(
            id = "claustrophobic_thriller",
            nameIt = "Thriller Claustrofobico & Spazio Chiuso",
            nameEn = "Claustrophobic & Single-Location Thriller",
            triggersIt = listOf("claustrofobico", "spazio chiuso", "una stanza", "bunker", "sepolto", "bloccati dentro", "tensione claustrofobica", "quattro mura", "rifugio isolato"),
            triggersEn = listOf("claustrophobic", "single location", "one room", "bunker", "trapped inside", "buried alive", "confined space", "isolated cabin"),
            primaryGenres = listOf(53L),
            secondaryGenres = listOf(9648L, 27L, 18L),
            mandatoryKeywords = listOf(2800L, 10582L, 1562L),
            secondaryKeywords = listOf(12565L, 3358L),
            anchorMovieIds = listOf(333371L, 1091L, 1700L, 4547L, 44363L, 503919L),
            rationaleTemplateIt = "Tensione psicologica soffocante e claustrofobia pura ambientata in uno spazio senza via di fuga.",
            rationaleTemplateEn = "Suffocating psychological tension and raw claustrophobia confined within inescapable walls."
        ),

        // --- SOTTOMARINO & ABISSI MARINI ---
        CinematicArchetype(
            id = "submarine_deep_sea",
            nameIt = "Sottomarino & Abissi Oceanici",
            nameEn = "Submarine & Deep Sea Thriller",
            triggersIt = listOf("sottomarino", "sottomarini", "abissi marini", "profondità marine", "battaglia sottomarina", "fossa delle marianne", "siluri", "immersione profonda"),
            triggersEn = listOf("submarine", "submarines", "deep sea", "ocean abyss", "underwater", "torpedo", "sonar", "deep dive"),
            primaryGenres = listOf(53L, 28L),
            secondaryGenres = listOf(10752L, 878L),
            mandatoryKeywords = listOf(1416L, 1826L, 11029L),
            secondaryKeywords = listOf(9053L),
            anchorMovieIds = listOf(1667L, 8963L, 387L, 2756L, 9053L),
            rationaleTemplateIt = "Guerra di nervi e metallo a centinaia di metri sotto il livello del mare, tra sonar e siluri.",
            rationaleTemplateEn = "High-stakes battle of nerves deep underwater, filled with crushing pressure and sonar suspense."
        ),

        // --- CYBERPUNK & DISTOPIA HIGH-TECH ---
        CinematicArchetype(
            id = "cyberpunk",
            nameIt = "Cyberpunk & Distopia High-Tech",
            nameEn = "Cyberpunk & High-Tech Dystopia",
            triggersIt = listOf(
                "cyberpunk", "hacker", "megacorporazioni", "innesti bionici", "cyborg",
                "neon e pioggia", "pioggia e neon", "neon", "futuro distopico", "blade runner style",
                "intelligenza artificiale ribelle", "implanti neurali", "luci al neon",
                "metropoli neon", "retrofuturismo", "synthwave", "distopia tecnologica"
            ),
            triggersEn = listOf(
                "cyberpunk", "hacker", "megacorporation", "bionic implants", "cyborg",
                "neon rain", "rain and neon", "neon lights", "dystopian future", "blade runner style",
                "rogue ai", "neural implants", "neon metropolis", "retrofuturistic", "synthwave",
                "tech dystopia", "cybernetic"
            ),
            primaryGenres = listOf(878L),
            secondaryGenres = listOf(28L, 53L, 18L),
            mandatoryKeywords = listOf(12190L, 2157L, 310L),
            secondaryKeywords = listOf(4565L, 350338L, 256181L),
            // Blade Runner (78) e Blade Runner 2049 (335984) vengono messi PRIMI:
            // sono il manifesto assoluto del cyberpunk con pioggia e neon.
            anchorMovieIds = listOf(78L, 335984L, 603L, 9323L, 149L, 180L, 49049L, 500664L),
            rationaleTemplateIt = "Futuro visivamente ipnotico dove tecnologia avanzata, metropoli al neon e corporazioni governano l'anima.",
            rationaleTemplateEn = "Hypnotic vision of high tech and low life, with neon metropolises, cybernetics, and corporate decay."
        ),

        // --- HEIST & RAPINA PERFETTA ---
        CinematicArchetype(
            id = "heist_caper",
            nameIt = "Heist Movie (Colpo Perfetto)",
            nameEn = "Heist & Caper Movie",
            triggersIt = listOf("heist", "film di rapina", "colpo grosso", "rapina in banca", "ladri professionisti", "piano impossibile", "scassinatori", "ocean style"),
            triggersEn = listOf("heist", "heist movie", "bank robbery", "caper", "master thieves", "impossible heist", "safecracker", "ocean's eleven style"),
            primaryGenres = listOf(80L, 53L),
            secondaryGenres = listOf(28L, 35L),
            mandatoryKeywords = listOf(10051L, 15363L, 642L),
            secondaryKeywords = listOf(10291L),
            anchorMovieIds = listOf(161L, 949L, 388L, 339403L, 9654L, 399170L),
            rationaleTemplateIt = "Pianificazione millimetrica, imprevisti al cardiopalma e stile da vendere nel colpo del secolo.",
            rationaleTemplateEn = "Meticulous planning, razor-sharp execution, and ultra-stylish thrill in the robbery of the century."
        ),

        // --- THRILLER PSICOLOGICO & MIND-BENDER ---
        CinematicArchetype(
            id = "psychological_thriller",
            nameIt = "Thriller Psicologico & Mind-Bender",
            nameEn = "Psychological Thriller & Mind Bender",
            triggersIt = listOf("thriller psicologico", "psicologico", "paranoia", "manipolazione mentale", "realtà o finzione", "follia", "plot twist", "colpo di scena finale", "mente disturbata", "allucinazioni"),
            triggersEn = listOf("psychological thriller", "psychological", "mind bender", "paranoia", "gaslighting", "mental manipulation", "reality or illusion", "twist ending", "hallucination"),
            primaryGenres = listOf(53L, 9648L),
            secondaryGenres = listOf(18L),
            mandatoryKeywords = listOf(12565L, 2800L, 154802L),
            secondaryKeywords = listOf(41329L, 222L),
            anchorMovieIds = listOf(11324L, 44214L, 210577L, 77L, 550L, 146233L),
            rationaleTemplateIt = "Labirinto psicologico destabilizzante che gioca con le certezze dello spettatore fino all'ultimo secondo.",
            rationaleTemplateEn = "Destabilizing psychological labyrinth playing with the viewer's grip on reality until the very end."
        ),

        // --- GIALLO ALL'ITALIANA ---
        CinematicArchetype(
            id = "giallo_italiano",
            nameIt = "Giallo all'Italiana",
            nameEn = "Italian Giallo",
            triggersIt = listOf("giallo all'italiana", "giallo italiano", "dario argento", "guanti neri", "delitto argentiano", "assassino con rasoio", "mario bava", "lucio fulci"),
            triggersEn = listOf("giallo", "italian giallo", "dario argento style", "black gloves killer", "mario bava", "razor killer"),
            primaryGenres = listOf(9648L, 53L),
            secondaryGenres = listOf(27L),
            mandatoryKeywords = listOf(190479L, 12570L, 10714L),
            secondaryKeywords = listOf(10292L),
            anchorMovieIds = listOf(11054L, 20531L, 2119L, 27838L),
            rationaleTemplateIt = "Atmosfera barocca, cromatismi accesi, colonna sonora indimenticabile e il fascino macabro del giallo nostrano.",
            rationaleTemplateEn = "Vivid color palettes, iconic hypnotic scores, and the unmistakable macabre charm of Italian cult cinema."
        ),

        // --- SPAGHETTI WESTERN ---
        CinematicArchetype(
            id = "spaghetti_western",
            nameIt = "Spaghetti Western",
            nameEn = "Spaghetti Western",
            triggersIt = listOf("spaghetti western", "western all'italiana", "sergio leone", "duello", "cacciatore di taglie", "pistolero solitario", "mezzogiorno di fuoco", "morricone"),
            triggersEn = listOf("spaghetti western", "sergio leone style", "bounty hunter western", "lone gunslinger", "mexican standoff"),
            primaryGenres = listOf(37L),
            secondaryGenres = listOf(28L, 18L),
            mandatoryKeywords = listOf(156212L, 801L, 14819L),
            secondaryKeywords = listOf(14643L),
            anchorMovieIds = listOf(429L, 185L, 391L, 10772L, 23565L),
            rationaleTemplateIt = "Sguardi di piombo, polvere, silenzi carichi di piombo e l'immortale epica western di Sergio Leone.",
            rationaleTemplateEn = "Gritty shootouts, iconic stare-downs, dust, and the timeless cinematic mythology of the frontier."
        ),

        // --- CASA INFESTATA & ORRORE GHOST ---
        CinematicArchetype(
            id = "haunted_house",
            nameIt = "Casa Infestata & Presenze Oscure",
            nameEn = "Haunted House & Supernatural Horror",
            triggersIt = listOf("casa infestata", "poltergeist", "fantasma", "fantasmi", "spiriti", "maniero infestato", "presenze oscure", "presenze demoniache", "conjuring style"),
            triggersEn = listOf("haunted house", "poltergeist", "ghost", "ghosts", "haunted mansion", "malevolent spirits", "conjuring style", "paranormal investigation"),
            primaryGenres = listOf(27L),
            secondaryGenres = listOf(9648L, 53L),
            mandatoryKeywords = listOf(3358L, 162846L, 10224L),
            secondaryKeywords = listOf(9713L),
            anchorMovieIds = listOf(138843L, 694L, 1933L, 49018L, 609L),
            rationaleTemplateIt = "Brividi lungo la schiena, scricchiolii nel buio e presenze spettrali che infestano le stanze.",
            rationaleTemplateEn = "Chilling atmosphere, creaking corridors, and restless spirits haunting an isolated estate."
        ),

        // --- SPACE OPERA & FANTASCIENZA DEGLI SPAZI ---
        CinematicArchetype(
            id = "space_opera",
            nameIt = "Space Opera & Odissea Spaziale",
            nameEn = "Space Opera & Cosmic Odyssey",
            triggersIt = listOf("space opera", "odissea nello spazio", "astronave", "pianeta alieno", "galassie", "stelle", "viaggio interstellare", "esplorazione spaziale", "interstellar style"),
            triggersEn = listOf("space opera", "cosmic odyssey", "spaceship", "alien planet", "interstellar travel", "space exploration", "deep cosmos", "interstellar style"),
            primaryGenres = listOf(878L, 12L),
            secondaryGenres = listOf(18L),
            mandatoryKeywords = listOf(195114L, 279614L, 9882L),
            secondaryKeywords = listOf(1612L),
            anchorMovieIds = listOf(157336L, 438631L, 1891L, 118340L, 329865L),
            rationaleTemplateIt = "Viaggio spettacolare ai confini dell'universo conosciuto, tra maestosità visiva e riflessione cosmica.",
            rationaleTemplateEn = "Majestic and visually breathtaking journey across the stars, blending epic scope with cosmic wonder."
        ),

        // --- VENDETTA SPIETATA (REVENGE) ---
        CinematicArchetype(
            id = "revenge_action",
            nameIt = "Film di Vendetta Spietata",
            nameEn = "Visceral Revenge Thriller",
            triggersIt = listOf("vendetta", "film di vendetta", "regolamento di conti", "giustiziere", "vendicare la famiglia", "john wick style", "nessuna pietà"),
            triggersEn = listOf("revenge", "revenge thriller", "vengeance", "payback", "avenging", "john wick style", "vigilante justice"),
            primaryGenres = listOf(28L, 53L),
            secondaryGenres = listOf(80L),
            mandatoryKeywords = listOf(9748L, 7002L),
            secondaryKeywords = listOf(10291L),
            anchorMovieIds = listOf(245891L, 24L, 670L, 11324L, 273481L),
            rationaleTemplateIt = "Corsa inarrestabile alimentata dal furore della vendetta, dove ogni colpo è una sentenza definitiva.",
            rationaleTemplateEn = "Relentless adrenaline-fueled quest for vengeance where nothing can stop the protagonist's retribution."
        ),

        // --- DRAMMA GIUDIZIARIO (COURTROOM) ---
        CinematicArchetype(
            id = "courtroom_drama",
            nameIt = "Dramma Giudiziario & Processuale",
            nameEn = "Courtroom & Legal Drama",
            triggersIt = listOf("processuale", "tribunale", "aula di tribunale", "avvocato", "avvocati", "giudice", "giuria", "verdetto", "processo legale", "12 angry men style"),
            triggersEn = listOf("courtroom", "legal drama", "trial", "lawyer", "attorney", "jury", "judge", "cross-examination", "verdict"),
            primaryGenres = listOf(18L),
            secondaryGenres = listOf(80L, 53L),
            mandatoryKeywords = listOf(6148L, 11038L),
            secondaryKeywords = listOf(9826L),
            anchorMovieIds = listOf(389L, 865L, 1592L, 595L, 556574L, 9800L),
            rationaleTemplateIt = "Scontro dialettico ad altissima caratura tra accusa e difesa, dove la verità si nasconde tra le pieghe della legge.",
            rationaleTemplateEn = "Riveting battle of wits and morals inside the courtroom, fighting for justice against the clock."
        ),

        // --- AVVENTURA RAGAZZI ANNI 80 / COMING OF AGE ---
        CinematicArchetype(
            id = "teen_adventure",
            nameIt = "Avventura Ragazzi & Spirito Anni '80",
            nameEn = "Coming-of-Age 80s Adventure",
            triggersIt = listOf("ragazzi in bicicletta", "avventura per ragazzi", "goonies style", "spirito anni 80", "mistero estivo", "gruppo di amici d'infanzia", "stranger things style"),
            triggersEn = listOf("kids on bikes", "80s kids adventure", "goonies style", "childhood friends", "summer mystery", "coming of age adventure", "stranger things style"),
            primaryGenres = listOf(12L, 10751L),
            secondaryGenres = listOf(35L, 878L, 9648L),
            mandatoryKeywords = listOf(10683L, 9717L, 11504L),
            secondaryKeywords = listOf(37686L),
            anchorMovieIds = listOf(9340L, 235L, 37686L, 601L, 207L),
            rationaleTemplateIt = "La magia senza tempo dell'amicizia giovanile, scoperte indimenticabili e pedalate verso l'avventura.",
            rationaleTemplateEn = "The timeless nostalgia of youth, camaraderie, discovery, and danger in an unforgettable retro adventure."
        ),

        // --- COMMEDIA ROMANTICA BRILLANTE / COZY ---
        CinematicArchetype(
            id = "cozy_romance",
            nameIt = "Commedia Romantica Brillante & Cozy",
            nameEn = "Witty & Feel-Good Romance",
            triggersIt = listOf("commedia romantica", "storia d'amore brillante", "dialoghi brillanti d'amore", "innamorarsi a parigi", "cozy romance", "feel-good romantico", "amelie style", "about time style"),
            triggersEn = listOf("romantic comedy", "rom-com", "witty romance", "falling in love", "feel-good romance", "cozy love story", "amelie style", "about time style"),
            primaryGenres = listOf(10749L, 35L),
            secondaryGenres = listOf(18L),
            mandatoryKeywords = listOf(9799L, 240L, 1605L),
            secondaryKeywords = listOf(10183L),
            anchorMovieIds = listOf(194L, 59436L, 122906L, 1581L, 76L, 426L),
            rationaleTemplateIt = "Dialoghi scintillanti, calore umano e quella magia irresistibile che fa sorridere e credere all'amore.",
            rationaleTemplateEn = "Charming dialogue, heartfelt tenderness, and that irresistible warmth of genuine human connection."
        ),

        // --- POST-APOCALITTICO & TERRA DESOLATA ---
        CinematicArchetype(
            id = "post_apocalyptic",
            nameIt = "Post-Apocalittico & Terra Desolata",
            nameEn = "Post-Apocalyptic Survival",
            triggersIt = listOf("post-apocalittico", "terra desolata", "fine del mondo", "sopravvissuti apocalisse", "mad max style", "deserto post nucleare", "collasso della civiltà"),
            triggersEn = listOf("post-apocalyptic", "wasteland", "end of the world", "fall of civilization", "nuclear winter", "mad max style", "apocalypse survivors"),
            primaryGenres = listOf(878L, 28L),
            secondaryGenres = listOf(53L, 12L),
            mandatoryKeywords = listOf(359337L, 10349L, 4565L),
            secondaryKeywords = listOf(14819L),
            anchorMovieIds = listOf(76341L, 9693L, 20766L, 6479L, 447332L),
            rationaleTemplateIt = "Sopravvivenza estrema tra le rovine fumanti della civiltà, dove l'umanità combatte per una scintilla di speranza.",
            rationaleTemplateEn = "Gritty survival across the ruins of civilization, pushed to the absolute edge of human endurance."
        ),

        // --- NOIR METROPOLITANO & NEO-NOIR ---
        CinematicArchetype(
            id = "neo_noir",
            nameIt = "Neo-Noir & Notte Metropolitana",
            nameEn = "Neo-Noir & Urban Night",
            triggersIt = listOf("neo-noir", "noir moderno", "metropoli notturna", "pioggia e asfalto", "detective disilluso", "corruzione metropolitana", "luci al neon", "drive style", "se7en style", "pioggia battente", "pioggia torrenziale", "detective pioggia", "detective nella pioggia", "thriller piovoso", "investigatore privato"),
            triggersEn = listOf("neo-noir", "modern noir", "nocturnal city", "rainy streets", "cynical detective", "urban decay", "neon-drenched", "drive style", "se7en style", "rain-drenched", "rain-soaked", "rain-slicked", "rain drenched", "detective thriller", "rainy detective", "rain detective", "rainy thriller"),
            primaryGenres = listOf(53L, 80L),
            secondaryGenres = listOf(9648L, 18L),
            mandatoryKeywords = listOf(9016L, 12190L, 703L),
            secondaryKeywords = listOf(9826L, 3840L),
            anchorMovieIds = listOf(807L, 114472L, 64690L, 242582L, 2118L, 414906L),
            rationaleTemplateIt = "Asfalto bagnato di pioggia, ambiguità morale e fascino magnetico nei recessi oscuri della città.",
            rationaleTemplateEn = "Rain-slicked streets, deep moral ambiguity, and hypnotic atmospheric dread in the urban underbelly."
        ),

        // --- GUERRA CRUDA & TRINCEE ---
        CinematicArchetype(
            id = "war_epic",
            nameIt = "Epica Bellica & Fango delle Trincee",
            nameEn = "Gritty War & Battle Epic",
            triggersIt = listOf("guerra", "film di guerra", "trincee", "seconda guerra mondiale", "sbarco in normandia", "vietnam", "soldati al fronte", "fratellanza sotto il fuoco", "soldati", "soldato", "linee nemiche", "dietro le linee nemiche", "intrappolati", "linee del fronte"),
            triggersEn = listOf("war", "war movie", "trenches", "world war", "ww2", "vietnam war", "soldiers on the frontline", "brotherhood in arms", "behind enemy lines", "soldiers", "trapped soldiers", "enemy lines"),
            primaryGenres = listOf(10752L),
            secondaryGenres = listOf(18L, 28L, 36L),
            mandatoryKeywords = listOf(1956L, 1631L, 1968L, 13065L),
            secondaryKeywords = listOf(1327L, 10349L),
            anchorMovieIds = listOf(857L, 28L, 530915L, 600L, 49046L, 792L, 9361L),
            rationaleTemplateIt = "Realismo implacabile, polvere da sparo e il sacrificio straziante di uomini messi alla prova dal fronte.",
            rationaleTemplateEn = "Raw, visceral immersion into the chaos and profound human cost of warfare on the front lines."
        ),

        // --- GLADIATORI, ARENA & TORNEI MORTALI ---
        CinematicArchetype(
            id = "gladiator_arena",
            nameIt = "Gladiatori, Arena & Tornei Mortali",
            nameEn = "Gladiator, Arena & Survival Tournaments",
            triggersIt = listOf("gladiatore", "gladiatori", "arena", "combattimento nell'arena", "combattimenti nell'arena", "combattere nell'arena", "torneo mortale", "colosseo", "lotta per la vita", "lotta per la gloria", "combattere per la vita", "combattere per la gloria", "spartacus style", "gladiator style", "giochi gladiatori"),
            triggersEn = listOf("gladiator", "gladiators", "arena", "fight in an arena", "fight in a arena", "arena fight", "arena combat", "death match", "tournament to the death", "fight for life and glory", "battle for glory", "colosseum", "colosseum fight", "survival tournament", "hunger games style", "gladiator style", "spartacus style"),
            primaryGenres = listOf(28L, 36L, 12L),
            secondaryGenres = listOf(18L),
            mandatoryKeywords = listOf(1394L, 1395L, 282498L),
            secondaryKeywords = listOf(1160L, 1721L),
            anchorMovieIds = listOf(98L, 70160L, 967L, 3176L, 59440L, 382272L, 2133L),
            rationaleTemplateIt = "Scontri all'ultimo sangue nell'arena, gladiatori e sfide mortali in cui solo uno sopravvive per la gloria.",
            rationaleTemplateEn = "Epic arena battles, gladiators, and deadly survival tournaments where only the strongest survive for glory."
        ),

        // --- METAMORFOSI & MUTAZIONE UOMO-INSETTO ---
        CinematicArchetype(
            id = "metamorphosis_mutation",
            nameIt = "Metamorfosi, Insetti & Mutazioni",
            nameEn = "Metamorphosis & Insect Mutation",
            triggersIt = listOf("metamorfosi", "trasformazione", "uomo insetto", "ragazzo insetto", "diventa un insetto", "diventa una libellula", "diventa un ragno", "mutazione genetica", "morso da un ragno", "morso da un insetto", "morso da", "superhero movie", "la mosca", "uomo mosca"),
            triggersEn = listOf("metamorphosis", "transformation", "turn into an insect", "turns into an insect", "become a dragonfly", "becomes a dragonfly", "become a spider", "becomes a spider", "become an insect", "bitten by a spider", "bitten by an insect", "bitten by", "genetic mutation", "insect human", "body mutation", "superhero movie", "the fly"),
            primaryGenres = listOf(878L, 35L),
            secondaryGenres = listOf(28L, 27L),
            mandatoryKeywords = listOf(10283L, 9717L),
            secondaryKeywords = listOf(4379L, 18035L),
            anchorMovieIds = listOf(11918L, 9426L, 557L, 17654L, 9799L),
            rationaleTemplateIt = "Mutazioni genetiche prodigiose o surreali in cui il confine tra essere umano e insetto svanisce.",
            rationaleTemplateEn = "Wild genetic mutations and transformations where the boundary between human and insect blurs."
        ),

        // --- RUGGENTI ANNI '20, OPULENZA & CADUTA DI UN MAGNATE ---
        CinematicArchetype(
            id = "roaring_twenties_wealth",
            nameIt = "Ruggenti Anni '20 & Magnati",
            nameEn = "Roaring Twenties & Wealth",
            triggersIt = listOf("anni 20", "anni '20", "anni venti", "anni ruggenti", "i 20", "i '20", "i venti", "uomo ricco", "grande gatsby", "gatsby", "magnate", "roaring twenties", "quarto potere"),
            triggersEn = listOf("roaring twenties", "1920s", "the 20s", "the 20's", "twenties", "rich man 1920s", "wealthy tycoon", "great gatsby", "gatsby style", "citizen kane style"),
            primaryGenres = listOf(18L, 36L, 10749L),
            secondaryGenres = listOf(80L),
            mandatoryKeywords = listOf(207876L, 10329L, 257265L),
            secondaryKeywords = listOf(34079L, 9398L),
            anchorMovieIds = listOf(64682L, 15L, 7345L, 11034L, 804095L),
            rationaleTemplateIt = "Feste sfarzose, l'opulenza effimera dei ruggenti anni '20 e la tragica caduta di un magnate enigmatico.",
            rationaleTemplateEn = "Lavish parties, fleeting Roaring Twenties wealth, and the tragic demise of an enigmatic tycoon."
        ),

        // --- FINALE TRAGICO & MORTE DEL PROTAGONISTA ---
        CinematicArchetype(
            id = "tragic_hero_demise",
            nameIt = "Finale Tragico & Eroi che Cadono",
            nameEn = "Tragic Ending & Fall of the Hero",
            triggersIt = listOf("muore alla fine", "muore alla fine del film", "morte alla fine", "muore alla fine della storia", "finale tragico", "morte del protagonista", "il protagonista muore", "sacrificio finale", "tragedia finale"),
            triggersEn = listOf("dies at the end", "dies at the end of the movie", "dies at the end of the film", "death at the end", "tragic ending", "hero dies at the end", "protagonist dies", "final sacrifice"),
            primaryGenres = listOf(18L),
            secondaryGenres = listOf(80L, 53L, 36L),
            mandatoryKeywords = listOf(34079L, 209592L),
            secondaryKeywords = listOf(9872L, 10744L),
            anchorMovieIds = listOf(64682L, 98L, 903L, 14L, 101L, 1422L, 597L),
            rationaleTemplateIt = "Un arco narrativo straziante e memorabile, culminante in un epilogo drammatico e indimenticabile.",
            rationaleTemplateEn = "A poignant and heartbreaking narrative arc culminating in a devastating, unforgettable finale."
        ),

        // --- SUPEREROI & FUMETTI ---
        CinematicArchetype(
            id = "superhero_comic",
            nameIt = "Supereroi & Fumetti",
            nameEn = "Superhero & Comic Book",
            triggersIt = listOf(
                "supereroe", "supereroi", "superpotere", "superpoteri", "fumetto", "fumetti",
                "eroe con superpoteri", "uomo ragno", "uomo pipistrello", "uomo d'acciaio",
                "uomo di ferro", "marvel", "dc", "dc comics", "avengers", "batman style",
                "superman style", "da un altro pianeta diventa", "viene da un altro pianeta",
                "alien hero", "eroe alieno"
            ),
            triggersEn = listOf(
                "superhero", "superheroes", "superpowers", "super powers", "comic book", "comics",
                "hero with powers", "spider-man", "batman", "superman", "iron man",
                "marvel", "dc", "dc comics", "avengers", "cape movie", "caped hero",
                "from another planet becomes", "alien becomes hero", "gains powers", "gains superpowers"
            ),
            primaryGenres = listOf(28L, 878L),
            secondaryGenres = listOf(12L, 14L, 35L),
            mandatoryKeywords = listOf(9715L, 9717L),
            secondaryKeywords = listOf(1701L, 310L, 14544L),
            anchorMovieIds = listOf(1924L, 1726L, 155L, 299536L, 284054L, 299534L, 100402L, 271110L),
            rationaleTemplateIt = "Poteri straordinari, mantelli e la battaglia eterna tra bene e male: il cinema che fa sognare generazioni.",
            rationaleTemplateEn = "Extraordinary powers, capes, and the eternal battle between good and evil that has defined generations of cinema."
        )
    )

    private val atmosphereCatalog: List<AtmosphericModifier> = listOf(
        AtmosphericModifier(
            id = "snow_mountain",
            labelIt = "Neve & Montagna Isolata",
            labelEn = "Snow & Isolated Mountain",
            triggersIt = listOf("neve", "bufera", "ghiaccio", "montagna", "alpi", "baita", "rifugio alpino", "freddo polare", "inverno rigido"),
            triggersEn = listOf("snow", "blizzard", "ice", "mountain", "alps", "cabin in the snow", "freezing cold", "winter landscape"),
            keywordIds = listOf(3358L, 6091L)
        ),
        AtmosphericModifier(
            id = "dark_disturbing",
            labelIt = "Atmosfera Cupa & Disturbante",
            labelEn = "Dark & Disturbing Atmosphere",
            triggersIt = listOf("cupo", "disturbante", "angosciante", "incubo", "inquietante", "macabro", "senza speranza", "tetro"),
            triggersEn = listOf("dark", "disturbing", "unsettling", "nightmarish", "creepy", "macabre", "hopeless", "grim"),
            genreBonus = listOf(53L, 27L),
            genrePenalty = listOf(35L, 10751L)
        ),
        AtmosphericModifier(
            id = "adrenaline_fast",
            labelIt = "Adrenalina & Ritmo Serrato",
            labelEn = "Adrenaline & Fast Paced",
            triggersIt = listOf("adrenalinico", "mozzafiato", "ritmo serrato", "non ti fa respirare", "col fiato sospeso", "inseguimenti", "esplosioni", "azione pura"),
            triggersEn = listOf("adrenaline", "breathtaking", "fast paced", "edge of your seat", "car chase", "explosions", "pure action"),
            genreBonus = listOf(28L, 53L)
        ),
        AtmosphericModifier(
            id = "cozy_warm",
            labelIt = "Confortante & Feel-Good",
            labelEn = "Cozy & Feel-Good",
            triggersIt = listOf("confortante", "feel good", "feel-good", "rilassante", "scalda il cuore", "spensierato", "dolce", "fa stare bene"),
            triggersEn = listOf("cozy", "feel good", "feel-good", "relaxing", "heartwarming", "wholesome", "uplifting"),
            genreBonus = listOf(35L, 10749L, 10751L),
            genrePenalty = listOf(27L)
        ),
        AtmosphericModifier(
            id = "melancholic_poetic",
            labelIt = "Malinconico & Poetico",
            labelEn = "Melancholic & Poetic",
            triggersIt = listOf("malinconico", "poetico", "filosofico", "riflessivo", "d'autore", "intimo", "struggente", "esistenziale"),
            triggersEn = listOf("melancholic", "poetic", "philosophical", "thought provoking", "arthouse", "intimate", "heartbreaking", "existential"),
            genreBonus = listOf(18L)
        )
    )

    private val franchiseSignatures: Map<String, List<String>> = mapOf(
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

    // Genre guards for short/ambiguous franchise triggers (length ≤ 8).
    // If the candidate's genres don't overlap with these, it's NOT part of the franchise.
    // Prevents "alien" (immigration doc, genre=99 Documentary) being grouped with Ridley Scott's Alien.
    // Prevents "matrix" (business doc) being penalized as a sequel to The Matrix.
    // 28=Action, 878=SciFi, 27=Horror, 12=Adventure, 14=Fantasy, 10749=Romance, 99=Documentary
    private val franchiseGenreGuards: Map<String, Set<Long>> = mapOf(
        "alien"     to setOf(27L, 878L, 28L),    // must be Horror/SciFi/Action — not Documentary
        "the_matrix" to setOf(878L, 28L, 12L),   // must be SciFi/Action/Adventure
        "star_wars"  to setOf(878L, 12L, 28L, 14L),
        "star_trek"  to setOf(878L, 12L, 28L),
        "terminator" to setOf(878L, 28L, 27L),
        "jurassic"   to setOf(878L, 28L, 12L, 27L),
        "mcu_avengers" to setOf(28L, 878L, 12L),
        "mcu_spiderman" to setOf(28L, 878L, 12L),
        "ocean"      to setOf(80L, 35L, 28L)     // Crime/Comedy/Action — not a nature documentary
    )

    fun isSupportedLanguage(language: String): Boolean {
        val lang = language.lowercase().take(2)
        return lang == "it" || lang == "en"
    }

    suspend fun searchLocalMood(
        query: String,
        language: String,
        page: Int = 1,
        isQuotaExhaustedFallback: Boolean = false
    ): MoodSearchResultData {
        val cleanQuery = query.trim().take(1000).lowercase(Locale.ROOT)
        // Detect whether the query is Italian, regardless of system locale settings
        val isItalian = detectItalian(cleanQuery, language)
        val tmdbLang = if (isItalian) "it-IT" else "en-US"
        val todayStr = LocalDate.now().toString()

        val dictionary = KeywordDictionary.getDictionaryForLanguage(if (isItalian) "it" else "en")
        val fallbackDictionary = KeywordDictionary.getDictionaryForLanguage(if (isItalian) "en" else "it")
        val querySynonyms = KeywordDictionary.getQuerySynonymsForLanguage(if (isItalian) "it" else "en")
        val fallbackQuerySynonyms = KeywordDictionary.getQuerySynonymsForLanguage(if (isItalian) "en" else "it")

        // Parse negation clauses (e.g. "senza viaggi nel tempo", "without romance", "no gore")
        val parsedIntent = parseQueryNegations(cleanQuery, isItalian, querySynonyms, dictionary)
        val positiveQuery = parsedIntent.positiveText

        // Detect iconic directors or actors (e.g. "Nolan", "Tarantino", "Scorsese", "DiCaprio")
        val detectedPerson = detectIconicPerson(positiveQuery)

        // 1. NLP Query Parsing: Time Horizons & Eras (Only constrain release date if explicitly requested as production era)
        var specificYear: Int? = null
        var startYear: Int? = null
        var endYear: Int? = null

        val isExplicitReleaseYearRequested = listOf(
            "uscito nel", "uscita nel", "usciti nel", "uscite nel",
            "del 19", "del 20", "film del 19", "film del 20", "film del 18",
            "released in", "release in", "from 19", "from 20", "movie from", "film from"
        ).any { positiveQuery.contains(it) }

        if (isExplicitReleaseYearRequested) {
            val singleYearRegex = Regex("""\b(19\d{2}|20[0-2]\d)\b""")
            val decadeItRegex = Regex("""\banni\s*(')?(\d{2}|\d{4})\b""")
            val decadeEnRegex = Regex("""\b(\d{2}|\d{4})(')?s\b""")

            val singleYearMatch = singleYearRegex.find(positiveQuery)
            val decadeItMatch = decadeItRegex.find(positiveQuery)
            val decadeEnMatch = decadeEnRegex.find(positiveQuery)

            if (decadeItMatch != null) {
                val raw = decadeItMatch.groupValues[2]
                val yr = if (raw.length == 2) {
                    val num = raw.toIntOrNull() ?: 80
                    1900 + num
                } else raw.toIntOrNull() ?: 1980
                startYear = (yr / 10) * 10
                endYear = startYear + 9
            } else if (decadeEnMatch != null) {
                val raw = decadeEnMatch.groupValues[1]
                val yr = if (raw.length == 2) {
                    val num = raw.toIntOrNull() ?: 80
                    1900 + num
                } else raw.toIntOrNull() ?: 1980
                startYear = (yr / 10) * 10
                endYear = startYear + 9
            } else if (singleYearMatch != null) {
                specificYear = singleYearMatch.value.toIntOrNull()
            }
        }

        val isVintageRequested = positiveQuery.contains("vintage") || positiveQuery.contains("classico") || positiveQuery.contains("bianco e nero") || positiveQuery.contains("black and white")
        if (isVintageRequested && startYear == null) {
            endYear = 1979
        }

        val isAnimationRequested = (positiveQuery.contains("animazione") || positiveQuery.contains("cartone") || positiveQuery.contains("anime") || positiveQuery.contains("animation") || positiveQuery.contains("animated")) && !parsedIntent.negatedGenres.contains(16L)
        val hasAnimalOrNonHumanSubject = detectAnimalOrNonHumanSubject(positiveQuery)

        // 2. Multi-Level Archetype & Atmosphere Detection (Check positive query)
        val isTransformationIntent = detectTransformationIntent(positiveQuery)
        val matchedArchetypes = archetypeCatalog.filter { archetype ->
            if (isTransformationIntent && archetype.id == "metamorphosis_mutation") {
                return@filter true
            }
            val triggers = if (isItalian) archetype.triggersIt else archetype.triggersEn
            val alternateTriggers = if (isItalian) archetype.triggersEn else archetype.triggersIt
            triggers.any { trigger -> matchesArchetypeTriggerExact(positiveQuery, trigger, isItalian) } ||
            alternateTriggers.any { trigger -> matchesArchetypeTriggerExact(positiveQuery, trigger, !isItalian) }
        }

        val matchedAtmospheres = atmosphereCatalog.filter { atm ->
            val triggers = if (isItalian) atm.triggersIt else atm.triggersEn
            val alternateTriggers = if (isItalian) atm.triggersEn else atm.triggersIt
            triggers.any { trigger -> matchesArchetypeTriggerExact(positiveQuery, trigger, isItalian) } ||
            alternateTriggers.any { trigger -> matchesArchetypeTriggerExact(positiveQuery, trigger, !isItalian) }
        }

        // 3. Keyword & Genre Extraction with Strict Boundaries, Morphological Stemming & Fuzzy Typo Tolerance
        val matchedKeywords = mutableListOf<Long>()
        val matchedGenres = mutableListOf<Long>()
        val contentTokens = extractSignificantContentTokens(positiveQuery, isItalian)

        // Aesthetic Intent Detection: user is searching by visual texture (neon, rain, dark) rather than plot narrative.
        // The Hard Gate is relaxed in this mode and keyword bonuses are doubled.
        val aestheticVocabulary = setOf(
            "neon", "pioggia", "rain", "dark", "cupo", "gritty", "grintoso",
            "piovoso", "rainy", "nebbia", "foggy", "fog", "fumo", "smoky",
            "synthwave", "retrofuturismo", "retrofuturistic", "noir", "moody"
        )
        val isAestheticIntent = contentTokens.any { aestheticVocabulary.contains(it) } ||
            positiveQuery.lowercase(java.util.Locale.ROOT).let { q ->
                aestheticVocabulary.any { q.contains(it) }
            }

        for (token in contentTokens) {
            val genreId = mapTokenToGenreId(token)
            if (genreId != null && !matchedGenres.contains(genreId) && !parsedIntent.negatedGenres.contains(genreId)) {
                matchedGenres.add(genreId)
            }
            val stemmed = stemWord(token, isItalian)
            val kwId = querySynonyms[token]
                ?: (if (stemmed != token) querySynonyms[stemmed] else null)
                ?: dictionary[token]
                ?: (if (stemmed != token) dictionary[stemmed] else null)
                ?: findFuzzyDictionaryMatch(token, querySynonyms)
                ?: findFuzzyDictionaryMatch(token, dictionary)
                ?: fallbackQuerySynonyms[token]
                ?: fallbackDictionary[token]
            if (kwId != null && !matchedKeywords.contains(kwId) && !parsedIntent.negatedKeywords.contains(kwId)) {
                matchedKeywords.add(kwId)
            }
        }

        // Dynamic Keyword Resolution via TMDB searchKeyword API (with memory cache and Italian->English token mapping)
        val unmappedTokens = contentTokens.filter { token ->
            !token.contains(" ") && token.length >= 4 &&
                !querySynonyms.containsKey(token) &&
                !dictionary.containsKey(token) &&
                !fallbackQuerySynonyms.containsKey(token) &&
                !fallbackDictionary.containsKey(token)
        }.take(3)

        if (unmappedTokens.isNotEmpty()) {
            coroutineScope {
                val deferredKw = unmappedTokens.map { token ->
                    async {
                        val searchToken = if (isItalian) {
                            KeywordDictionary.italianToEnglishSearchTokens[token] ?: token
                        } else {
                            token
                        }
                        keywordResolutionCache[searchToken] ?: try {
                            val kwRes = tmdbService.searchKeyword(searchToken)
                            val resolvedId = kwRes.getList().firstOrNull()?.id
                            if (resolvedId != null) {
                                keywordResolutionCache[searchToken] = resolvedId
                            }
                            resolvedId
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                for (id in deferredKw.mapNotNull { it.await() }) {
                    if (!matchedKeywords.contains(id) && !parsedIntent.negatedKeywords.contains(id)) {
                        matchedKeywords.add(id)
                    }
                }
            }
        }

        // Aggregate Target Genres & Keywords
        val targetGenres = mutableListOf<Long>()
        val targetKeywords = mutableListOf<Long>()

        for (arch in matchedArchetypes) {
            targetGenres.addAll(arch.primaryGenres)
            targetGenres.addAll(arch.secondaryGenres)
            targetKeywords.addAll(arch.mandatoryKeywords)
            targetKeywords.addAll(arch.secondaryKeywords)
        }

        for (atm in matchedAtmospheres) {
            targetGenres.addAll(atm.genreBonus)
            targetKeywords.addAll(atm.keywordIds)
        }
        targetGenres.addAll(matchedGenres)
        targetKeywords.addAll(matchedKeywords)

        // If animal or non-human protagonist is searched, animation is a primary cinematic medium:
        // include Animation (16L) in target genres so discover streams surface both live-action and animated candidates
        if (hasAnimalOrNonHumanSubject && !targetGenres.contains(16L) && !parsedIntent.negatedGenres.contains(16L)) {
            targetGenres.add(16L)
        }

        val distinctGenres = targetGenres.distinct()
        val distinctKeywords = targetKeywords.distinct()

        // DESPERATION MODE: If the engine has zero genres AND zero keywords, it means the query was
        // not understood at all (e.g. "un uomo da un altro pianeta diventa un supereroe" before superhero archetype).
        // Track this state to relax the Hard Gate downstream so Stream D BM25 results are allowed through.
        val isDesperationMode = distinctGenres.isEmpty() && distinctKeywords.isEmpty() && matchedArchetypes.isEmpty() && detectedPerson == null

        // 4. Multi-Stream Concurrent Candidate Generation
        val candidateMap = mutableMapOf<Long, Movie>()

        coroutineScope {
            // Stream A: Subgenre Discover (Quality focus, ONLY if genres, keywords, or person matched!)
            val streamA = async {
                if (distinctGenres.isNotEmpty() || distinctKeywords.isNotEmpty() || detectedPerson != null) {
                    try {
                        val opts = mutableMapOf<String, String>(
                            "language" to tmdbLang,
                            "include_adult" to "false",
                            "vote_count.gte" to "100",
                            "sort_by" to "vote_average.desc",
                            "primary_release_date.lte" to (endYear?.let { "$it-12-31" } ?: todayStr)
                        )
                        if (distinctGenres.isNotEmpty()) {
                            opts["with_genres"] = distinctGenres.take(3).joinToString("|")
                        }
                        if (distinctKeywords.isNotEmpty()) {
                            opts["with_keywords"] = distinctKeywords.take(4).joinToString("|")
                        }
                        if (detectedPerson != null) {
                            opts["with_people"] = detectedPerson.second.toString()
                        }
                        if (parsedIntent.negatedGenres.isNotEmpty()) {
                            opts["without_genres"] = parsedIntent.negatedGenres.joinToString(",")
                        }
                        if (parsedIntent.negatedKeywords.isNotEmpty()) {
                            opts["without_keywords"] = parsedIntent.negatedKeywords.take(3).joinToString(",")
                        }
                        applyDateBounds(opts, specificYear, startYear, endYear)
                        tmdbService.discoverMovies(page = page, options = opts).results
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            // Stream B: Subgenre Discover (Popularity & Crowd focus, ONLY if genres, keywords, or person matched!)
            val streamB = async {
                if (distinctGenres.isNotEmpty() || distinctKeywords.isNotEmpty() || detectedPerson != null) {
                    try {
                        val opts = mutableMapOf<String, String>(
                            "language" to tmdbLang,
                            "include_adult" to "false",
                            "vote_count.gte" to "80",
                            "sort_by" to "popularity.desc",
                            "primary_release_date.lte" to (endYear?.let { "$it-12-31" } ?: todayStr)
                        )
                        if (distinctGenres.isNotEmpty()) {
                            opts["with_genres"] = distinctGenres.take(3).joinToString("|")
                        }
                        if (distinctKeywords.isNotEmpty()) {
                            opts["with_keywords"] = distinctKeywords.take(4).joinToString("|")
                        }
                        if (detectedPerson != null) {
                            opts["with_people"] = detectedPerson.second.toString()
                        }
                        if (parsedIntent.negatedGenres.isNotEmpty()) {
                            opts["without_genres"] = parsedIntent.negatedGenres.joinToString(",")
                        }
                        if (parsedIntent.negatedKeywords.isNotEmpty()) {
                            opts["without_keywords"] = parsedIntent.negatedKeywords.take(3).joinToString(",")
                        }
                        applyDateBounds(opts, specificYear, startYear, endYear)
                        tmdbService.discoverMovies(page = page, options = opts).results
                    } catch (_: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            // Stream C: Curated Golden Anchor Movie Injector (Always fetch if archetype matched, with coroutine parallelism & caching)
            val streamC = async {
                val anchorIds = matchedArchetypes.flatMap { it.anchorMovieIds }.distinct().take(7)
                coroutineScope {
                    anchorIds.map { id ->
                        async {
                            val cacheKey = "$id-$tmdbLang"
                            anchorDetailsCache[cacheKey] ?: try {
                                val details = tmdbService.getMovieBasicDetails(id, language = tmdbLang)
                                val m = Movie(
                                    id = details.id,
                                    title = details.title,
                                    posterPath = details.posterPath,
                                    backdropPath = details.backdropPath,
                                    voteAverage = details.voteAverage,
                                    voteCount = details.voteCount ?: 500,
                                    releaseDate = details.releaseDate,
                                    genreIds = details.genres?.map { it.id } ?: emptyList(),
                                    overview = details.overview
                                )
                                anchorDetailsCache[cacheKey] = m
                                m
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }.mapNotNull { it.await() }
                }
            }

            // Stream D: Thematic & Phrase Token Search on TMDB
            val streamD = async {
                val list = mutableListOf<Movie>()
                // Search multi-word phrase if available
                if (contentTokens.size >= 2) {
                    try {
                        val phrase = contentTokens.take(3).joinToString(" ")
                        val res = tmdbService.searchMovie(phrase, page = page)
                        list.addAll(res.results.take(6))
                    } catch (_: Exception) {
                        // ignore
                    }
                }
                // Also search individual salient tokens if token is long enough (>= 4)
                val tokensToSearch = if (matchedArchetypes.isEmpty()) {
                    contentTokens.take(2)
                } else {
                    // Even if archetype matched, search non-archetype salient subject tokens (e.g. "topo")
                    contentTokens.filter { token ->
                        token.length >= 4 && !matchedArchetypes.any { arch ->
                            (arch.triggersIt + arch.triggersEn).contains(token)
                        }
                    }.take(2)
                }
                for (token in tokensToSearch) {
                    if (token.length >= 4) {
                        try {
                            val res = tmdbService.searchMovie(token, page = page)
                            list.addAll(res.results.take(4))
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }
                list
            }

            val resA = streamA.await()
            val resB = streamB.await()
            val resC = streamC.await()
            val resD = streamD.await()

            for (m in (resC + resA + resB + resD)) {
                // Hard Gates: Valid ID, valid poster image, minimum 20 votes, and already released
                val hasPoster = !m.posterPath.isNullOrBlank()
                val minVotes = (m.voteCount ?: 0) >= 20
                if (m.id > 0L && hasPoster && minVotes && isMovieReleased(m.releaseDate) && !candidateMap.containsKey(m.id)) {
                    candidateMap[m.id] = m
                }
            }
        }

        // Emergency Graceful Relaxation if still completely empty
        if (candidateMap.isEmpty()) {
            try {
                val fallbackResults = tmdbService.discoverMovies(
                    page = page,
                    options = mapOf(
                        "language" to tmdbLang,
                        "sort_by" to "popularity.desc",
                        "vote_count.gte" to "200",
                        "primary_release_date.lte" to todayStr
                    )
                ).results
                for (m in fallbackResults) {
                    val hasPoster = !m.posterPath.isNullOrBlank()
                    val minVotes = (m.voteCount ?: 0) >= 20
                    if (m.id > 0L && hasPoster && minVotes && isMovieReleased(m.releaseDate) && !candidateMap.containsKey(m.id)) {
                        candidateMap[m.id] = m
                    }
                }
            } catch (_: Exception) {
                // Return empty
            }
        }

        // 5. Deep Cinematic Reranking Matrix
        data class ScoredCandidate(
            val movie: Movie,
            val score: Double,
            val franchiseKey: String?,
            val bespokeReason: String
        )

        val primaryArchetype = matchedArchetypes.firstOrNull()

        val bm25Scores = computeBm25Scores(
            candidates = candidateMap.values.toList(),
            queryTokens = contentTokens,
            isItalian = isItalian
        )

        val scoredList = candidateMap.values.mapNotNull { m ->
            var thematicRelevanceScore = 0.0

            // A. Archetype & Genre Relevancy Score
            val movieGenres = m.genreIds ?: emptyList()
            val isAnchor = matchedArchetypes.any { it.anchorMovieIds.contains(m.id) }
            if (isAnchor) {
                val hasDivergingQueryGenres = distinctGenres.isNotEmpty() && distinctGenres.none { movieGenres.contains(it) }
                val anchorBonus = if (hasDivergingQueryGenres) 20.0 else 50.0
                thematicRelevanceScore += anchorBonus
            }

            if (primaryArchetype != null) {
                val primaryOverlap = movieGenres.count { primaryArchetype.primaryGenres.contains(it) }
                val secondaryOverlap = movieGenres.count { primaryArchetype.secondaryGenres.contains(it) }
                thematicRelevanceScore += (primaryOverlap * 14.0).coerceAtMost(28.0)
                thematicRelevanceScore += (secondaryOverlap * 6.0).coerceAtMost(12.0)

                // Penalty for unrequested animation in serious/gritty archetypes.
                // NEUTRALIZED when query mentions animals or non-human subjects!
                if (!isAnimationRequested && !hasAnimalOrNonHumanSubject && movieGenres.contains(16L)) {
                    thematicRelevanceScore -= 35.0
                }
            } else if (distinctGenres.isNotEmpty()) {
                val overlap = movieGenres.count { distinctGenres.contains(it) }
                thematicRelevanceScore += (overlap * 12.0).coerceAtMost(30.0)
            }

            // Negation hard penalties
            if (parsedIntent.negatedGenres.isNotEmpty() && movieGenres.any { parsedIntent.negatedGenres.contains(it) }) {
                thematicRelevanceScore -= 80.0
            }

            if (movieGenres.contains(16L)) {
                if (isAnimationRequested) {
                    thematicRelevanceScore += 25.0
                } else if (hasAnimalOrNonHumanSubject) {
                    thematicRelevanceScore += 18.0
                }
            }

            // C. Semantic Overview & Title Alignment + BM25 Information Retrieval Score
            val overviewLower = (m.overview ?: "").lowercase(Locale.ROOT)
            val titleLower = (m.title ?: "").lowercase(Locale.ROOT)
            var textMatchBonus = 0.0
            var matchedTokenCount = 0
            var matchedTitleTokensCount = 0
            for (token in contentTokens) {
                if (token.length >= 4) {
                    val isOverviewMatch = if (token.length in 4..5 && !token.contains(" ")) {
                        Regex("""(?i)\b${Regex.escape(token)}\b""").containsMatchIn(overviewLower)
                    } else {
                        overviewLower.contains(token)
                    }
                    if (isOverviewMatch) {
                        textMatchBonus += 12.0
                        matchedTokenCount++
                    }
                    val isTitleMatch = if (token.length in 4..5 && !token.contains(" ")) {
                        Regex("""(?i)\b${Regex.escape(token)}\b""").containsMatchIn(titleLower)
                    } else {
                        titleLower.contains(token)
                    }
                    if (isTitleMatch) {
                        matchedTitleTokensCount++
                        matchedTokenCount++
                    }
                }
            }
            if (parsedIntent.negatedTokens.any { it.length >= 4 && overviewLower.contains(it) }) {
                thematicRelevanceScore -= 35.0
            }

            // Calibrated title bonus: avoid letting 1-word titles matching a single token
            // in a long descriptive query steal the entire ranking.
            val titleBonus = when {
                matchedTitleTokensCount == 0 -> 0.0
                contentTokens.size >= 2 && matchedTitleTokensCount == 1 -> 5.0
                else -> 18.0 * matchedTitleTokensCount
            }
            textMatchBonus += titleBonus
            thematicRelevanceScore += textMatchBonus.coerceAtMost(40.0)

            // Inject BM25 text relevance score directly into thematic ranking.
            // In Aesthetic Intent Mode, BM25 is amplified since synopses may lack visual keywords
            // (e.g. Blade Runner's synopsis never says "neon" but the keyword tag does).
            val bm25 = bm25Scores[m.id] ?: 0.0
            val bm25Multiplier = if (isAestheticIntent) 2.5 else 1.5
            thematicRelevanceScore += (bm25 * bm25Multiplier).coerceAtMost(60.0)

            // Bonus for detected iconic directors/actors explicitly searched
            if (detectedPerson != null) {
                thematicRelevanceScore += 25.0
            }

            // HARD GATE: ZERO THEMATIC RELEVANCE -> STRICT EXCLUSION!
            // Quality score (Bayesian) must NEVER allow a completely unrelated movie to pass!
            // A movie must have substantive grounding: Anchor, Primary Genre Match, Text Token Match, BM25 match, or Iconic Person match!
            val primaryOverlap = if (primaryArchetype != null) {
                movieGenres.count { primaryArchetype.primaryGenres.contains(it) }
            } else if (distinctGenres.isNotEmpty()) {
                movieGenres.count { distinctGenres.contains(it) }
            } else 0
            val hasSubstantiveThematicGrounding = isAnchor || primaryOverlap > 0 || matchedTokenCount > 0 || bm25 >= 1.5 || detectedPerson != null

            // DESPERATION MODE: When the engine has zero genres/keywords (complete parse failure),
            // relax the Hard Gate. Any candidate with BM25 > 0 or any text match passes through.
            // It's always better to show 10 imperfect results than a black screen.
            val passesGate = if (isDesperationMode) {
                bm25 > 0.0 || matchedTokenCount > 0 || thematicRelevanceScore > 0.0
            } else {
                hasSubstantiveThematicGrounding && thematicRelevanceScore > 0.0
            }

            if (!passesGate) {
                return@mapNotNull null
            }

            var totalScore = thematicRelevanceScore

            // D. Bayesian Quality Boost (ONLY for thematically relevant movies!)
            val v = (m.voteCount ?: 0).toDouble()
            val r = (m.voteAverage ?: 0.0)
            val c = 6.4
            val minVotesThreshold = 120.0
            val bayesianRating = if (v > 0) ((v * r) + (minVotesThreshold * c)) / (v + minVotesThreshold) else c
            val voteWeight = (v / 80.0).coerceIn(0.2, 1.0)
            val qualityBoost = (((bayesianRating - 5.0).coerceAtLeast(0.0) / 4.0) * 25.0) * voteWeight
            totalScore += qualityBoost

            // E. Era & Decade Alignment
            val year = extractYearFromDate(m.releaseDate)
            if (year != null) {
                if (specificYear != null && year == specificYear) {
                    totalScore += 15.0
                } else if (startYear != null && endYear != null) {
                    if (year in startYear..endYear) {
                        totalScore += 15.0
                    } else {
                        val dist = if (year < startYear) startYear - year else year - endYear
                        totalScore -= (dist * 1.5).coerceAtMost(25.0)
                    }
                }
            }

            // F. Detect Franchise & Rationale
            val franchiseKey = detectFranchiseKey(m.title ?: "", m.genreIds ?: emptyList())
            val reason = generateBespokeReason(
                movie = m,
                archetype = primaryArchetype,
                isItalian = isItalian,
                query = query
            )

            ScoredCandidate(m, totalScore, franchiseKey, reason)
        }

        // 6. Franchise Anti-Monopoly & Title Deduplication
        // Maximum 1 movie per franchise gets full points; subsequent entries receive heavy penalties
        val franchiseGroupCounts = mutableMapOf<String, Int>()
        val deduplicatedSorted = scoredList.sortedByDescending { it.score }.map { candidate ->
            val fKey = candidate.franchiseKey
            if (fKey != null) {
                val count = franchiseGroupCounts.getOrDefault(fKey, 0)
                franchiseGroupCounts[fKey] = count + 1
                val penalty = when (count) {
                    0 -> 0.0
                    1 -> 35.0
                    else -> 70.0
                }
                candidate.copy(score = candidate.score - penalty)
            } else {
                candidate
            }
        }.sortedByDescending { it.score }

        // Strict Title Anti-Monopoly: Never recommend multiple movies with the identical normalized title
        val titleSeen = mutableSetOf<String>()
        val titleDeduplicated = mutableListOf<ScoredCandidate>()
        for (cand in deduplicatedSorted) {
            val normTitle = normalizeTitleForDeduplication(cand.movie.title)
            if (normTitle.length >= 3) {
                if (!titleSeen.contains(normTitle)) {
                    titleSeen.add(normTitle)
                    titleDeduplicated.add(cand)
                }
            } else {
                titleDeduplicated.add(cand)
            }
        }

        // Filter out candidates with severe negative scores (e.g. random movies with 0 keyword matches)
        val filteredResults = titleDeduplicated.filter { it.score >= 10.0 }
        val finalCandidateList = if (filteredResults.isNotEmpty()) filteredResults else titleDeduplicated

        // 7. Map to final results
        val finalMovies = finalCandidateList.take(15).map { item ->
            TMDBSearchResult.MovieResult(
                id = item.movie.id,
                title = item.movie.title,
                posterPath = item.movie.posterPath,
                backdropPath = item.movie.backdropPath,
                voteAverage = item.movie.voteAverage,
                releaseDate = item.movie.releaseDate,
                genreIds = item.movie.genreIds ?: emptyList(),
                overview = item.movie.overview,
                aiReason = item.bespokeReason
            )
        }

        // 8. Final Results (In algorithm mode, vibeTitle and vibeDescription are omitted to avoid synthetic text)
        return MoodSearchResultData(
            vibeTitle = "",
            vibeDescription = "",
            totalCount = finalMovies.size,
            totalPages = 1,
            movies = finalMovies
        )
    }

    private fun detectItalian(query: String, language: String): Boolean {
        if (language.lowercase().startsWith("it")) return true
        val lower = query.lowercase(Locale.ROOT)
        val italianMarkers = setOf(
            "un", "una", "uno", "il", "lo", "la", "gli", "le", "dei", "degli", "delle",
            "del", "della", "dello", "con", "senza", "per", "tra", "fra", "che", "chi",
            "cosa", "dove", "come", "quando", "perché", "perche", "mago", "maghi",
            "prestigiatore", "prestigiatori", "vuole", "essere", "non", "più", "piu", "tutto", "tutti",
            "storia", "racconto", "bello", "belli", "quello", "quella", "questo", "questa",
            "dopo", "prima", "durante", "contro", "insieme", "sempre", "mai"
        )
        val tokens = Regex("""\b\w+\b""").findAll(lower).map { it.value }
        val matchCount = tokens.count { italianMarkers.contains(it) }
        return matchCount >= 1
    }

    private fun detectFranchiseKey(title: String, genreIds: List<Long> = emptyList()): String? {
        val lower = title.lowercase(Locale.ROOT)
        for ((key, triggers) in franchiseSignatures) {
            val titleMatch = triggers.any { lower.contains(it) }
            if (!titleMatch) continue

            // Guard against omonimia: short ambiguous triggers (≤8 chars) require that
            // the candidate shares at least one genre with known franchise genres to avoid
            // "Star Wars" ≠ "Star Trek", "Alien" (franchise) ≠ "Alien" (immigration doc), etc.
            val matchedTrigger = triggers.first { lower.contains(it) }
            if (matchedTrigger.length <= 8) {
                val franchiseGenres = franchiseGenreGuards[key]
                if (franchiseGenres != null && franchiseGenres.isNotEmpty()) {
                    val hasGenreOverlap = genreIds.any { franchiseGenres.contains(it) }
                    if (!hasGenreOverlap) continue   // Omonimia: genere incompatibile → skip
                }
            }

            return key
        }
        return null
    }

    private fun generateBespokeReason(
        movie: Movie,
        archetype: CinematicArchetype?,
        isItalian: Boolean,
        query: String
    ): String {
        val year = extractYearFromDate(movie.releaseDate)
        val yearStr = if (year != null) " ($year)" else ""
        val ratingStr = movie.voteAverage?.let { String.format(Locale.US, "%.1f", it) }

        if (archetype != null) {
            val base = if (isItalian) archetype.rationaleTemplateIt else archetype.rationaleTemplateEn
            return if (ratingStr != null && (movie.voteAverage ?: 0.0) >= 7.5) {
                if (isItalian) "$base · Acclamato con voto $ratingStr su TMDB." else "$base · Acclaimed with a $ratingStr rating on TMDB."
            } else {
                base
            }
        }

        return if (isItalian) {
            "Film consigliato$yearStr per affinità con l'atmosfera e i temi ricercati."
        } else {
            "Recommended film$yearStr aligning with your requested mood and themes."
        }
    }

    private fun detectTransformationIntent(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT)
        val directMetamorphosisWords = listOf(
            "metamorfosi", "trasformazione", "mutazione", "morso da", "morso di",
            "metamorphosis", "transformation", "mutation", "mutant", "bitten by", "bit by"
        )
        if (directMetamorphosisWords.any { lower.contains(it) }) return true

        val becomingWords = listOf(
            "become", "becomes", "became", "becoming", "turned into", "turns into", "turning into",
            "diventa", "diventare", "diventano", "diventato", "diventata", "trasforma", "trasformarsi"
        )
        val creatureTargets = listOf(
            "insetto", "insetti", "mosca", "ragno", "ragni", "mostro", "mostri", "creatura", "creature",
            "vampiro", "lupo mannaro", "licantropo", "zombie", "mutante", "mutanti",
            "insect", "insects", "fly", "spider", "spiders", "monster", "monsters", "creature", "creatures",
            "vampire", "werewolf"
        )
        val hasBecoming = becomingWords.any { lower.contains(it) }
        val hasCreatureTarget = creatureTargets.any { lower.contains(it) }
        return hasBecoming && hasCreatureTarget
    }

    private fun extractSignificantContentTokens(query: String, isItalian: Boolean): List<String> {
        val stopwords = if (isItalian) stopwordsIt else stopwordsEn

        val allWords = Regex("""\b[\w-]+\b""")
            .findAll(query.lowercase(Locale.ROOT))
            .map { it.value.trim('-') }
            .filter { it.isNotBlank() }
            .toList()

        val rawWords = allWords.filterIndexed { index, word ->
            word.length > 2 || word.matches(Regex("""\d{2}""")) || (word.length == 1 && index + 1 < allWords.size && allWords[index + 1].matches(Regex("""\d{2}""")))
        }

        val unigrams = mutableListOf<String>()
        for (w in rawWords) {
            if (!stopwords.contains(w)) {
                unigrams.add(w)
                if (w.contains("-")) {
                    val joint = w.replace("-", "")
                    if (joint.isNotBlank() && !stopwords.contains(joint)) {
                        unigrams.add(joint)
                    }
                    val parts = w.split("-").filter { it.length > 2 && !stopwords.contains(it) }
                    unigrams.addAll(parts)
                }
            }
        }
        val bigrams = rawWords.windowed(2).map { it.joinToString(" ") }
        val trigrams = rawWords.windowed(3).map { it.joinToString(" ") }

        return (unigrams + bigrams + trigrams).distinct()
    }

    /**
     * Hard-Lock Trigger Matching for Archetypes & Atmospheres.
     * Archetypes inject high-impact Golden Anchors (+50 pts) and dominate candidate generation.
     * To prevent lethal false positives (e.g. "drench" -> "trench" injecting WW2 war movies into rain-drenched detective thrillers),
     * Archetypes and Atmospheres MUST NEVER trigger via fuzzy/Levenshtein matching.
     * Only exact phrases, exact tokens, or strict morphological stems are permitted.
     */
    private fun matchesArchetypeTriggerExact(query: String, target: String, isItalian: Boolean = true): Boolean {
        val q = query.lowercase(Locale.ROOT)
        val t = target.lowercase(Locale.ROOT)

        // 1. Exact phrase or whole-word match
        if (containsWholeWordOrPhrase(q, t)) return true

        // 2. Exact phrase match with hyphen normalization ("rain-drenched" <-> "rain drenched")
        val qNormalized = q.replace("-", " ")
        val tNormalized = t.replace("-", " ")
        if (containsWholeWordOrPhrase(qNormalized, tNormalized)) return true

        // 3. Strict token & morphological stem matching (NO Levenshtein distance)
        val tWords = tNormalized.split(" ").filter { it.isNotBlank() }
        val qTokens = Regex("""\b\w+\b""").findAll(qNormalized).map { it.value }.filter { it.isNotBlank() }.toList()
        if (tWords.isEmpty() || qTokens.isEmpty()) return false

        return if (tWords.size > 1) {
            // Multi-word trigger: every word in trigger must be present in query (either exact or same morphological stem)
            tWords.all { tWord ->
                val tStem = stemWord(tWord, isItalian)
                qTokens.any { qTok ->
                    qTok == tWord || stemWord(qTok, isItalian) == tStem
                }
            }
        } else {
            // Single-word trigger: must match query token exactly or via identical morphological stem
            val tWord = tWords.first()
            val tStem = stemWord(tWord, isItalian)
            qTokens.any { qTok ->
                qTok == tWord || stemWord(qTok, isItalian) == tStem
            }
        }
    }

    private fun damerauLevenshtein(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val d = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) d[i][0] = i
        for (j in 0..len2) d[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = min(
                    d[i - 1][j] + 1,
                    min(d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                )
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    d[i][j] = min(d[i][j], d[i - 2][j - 2] + cost)
                }
            }
        }
        return d[len1][len2]
    }

    private fun containsWholeWordOrPhrase(text: String, phrase: String): Boolean {
        if (phrase.isBlank()) return false
        val regex = Regex("""(?i)\b${Regex.escape(phrase)}\b""")
        return regex.containsMatchIn(text)
    }

    private fun applyDateBounds(
        opts: MutableMap<String, String>,
        specificYear: Int?,
        startYear: Int?,
        endYear: Int?
    ) {
        val todayStr = LocalDate.now().toString()
        if (specificYear != null) {
            opts["primary_release_year"] = specificYear.toString()
        } else if (startYear != null && endYear != null) {
            opts["primary_release_date.gte"] = "$startYear-01-01"
            opts["primary_release_date.lte"] = "$endYear-12-31"
        } else if (endYear != null) {
            opts["primary_release_date.lte"] = "$endYear-12-31"
        } else if (startYear != null) {
            opts["primary_release_date.gte"] = "$startYear-01-01"
            opts["primary_release_date.lte"] = todayStr
        } else {
            opts["primary_release_date.lte"] = todayStr
        }
    }

    private fun extractYearFromDate(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank() || dateStr.length < 4) return null
        return dateStr.take(4).toIntOrNull()
    }

    private fun isMovieReleased(releaseDate: String?): Boolean {
        if (releaseDate.isNullOrBlank()) return false
        return try {
            val date = LocalDate.parse(releaseDate.take(10))
            !date.isAfter(LocalDate.now())
        } catch (_: Exception) {
            val yr = extractYearFromDate(releaseDate)
            yr != null && yr <= LocalDate.now().year
        }
    }

    private fun normalizeTitleForDeduplication(title: String?): String {
        if (title.isNullOrBlank()) return ""
        return title.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9]"""), "")
    }

    private fun capitalizeWords(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    private fun parseQueryNegations(
        query: String,
        isItalian: Boolean,
        querySynonyms: Map<String, Long>,
        dictionary: Map<String, Long>
    ): ParsedQueryIntent {
        val lower = query.lowercase(Locale.ROOT)
        val negationTriggers = if (isItalian) {
            listOf("senza", "tranne", "ma non", "eccetto", "escludendo", "no")
        } else {
            listOf("without", "except", "but not", "excluding", "no")
        }

        var splitIndex = -1
        var matchedTrigger = ""
        for (trigger in negationTriggers) {
            val idx = lower.indexOf(" $trigger ")
            if (idx != -1 && (splitIndex == -1 || idx < splitIndex)) {
                splitIndex = idx
                matchedTrigger = trigger
            } else if (lower.startsWith("$trigger ")) {
                if (splitIndex == -1 || 0 < splitIndex) {
                    splitIndex = 0
                    matchedTrigger = trigger
                }
            }
        }

        if (splitIndex == -1) {
            return ParsedQueryIntent(
                positiveText = lower,
                negatedText = null,
                negatedGenres = emptyList(),
                negatedKeywords = emptyList(),
                negatedTokens = emptyList()
            )
        }

        val positivePart = if (splitIndex == 0) "" else lower.substring(0, splitIndex).trim()
        val afterTrigger = lower.substring(splitIndex + matchedTrigger.length + (if (splitIndex == 0) 1 else 2)).trim()

        val clauseEnd = listOf(",", ";", " e ", " and ").map { afterTrigger.indexOf(it) }.filter { it != -1 }.minOrNull()
        val negatedClause = if (clauseEnd != null) afterTrigger.substring(0, clauseEnd).trim() else afterTrigger

        val negatedTokens = extractSignificantContentTokens(negatedClause, isItalian)
        val negGenres = mutableListOf<Long>()
        val negKeywords = mutableListOf<Long>()

        for (token in negatedTokens) {
            val genreId = mapTokenToGenreId(token)
            if (genreId != null) {
                negGenres.add(genreId)
            }
            val kwId = querySynonyms[token] ?: dictionary[token]
            if (kwId != null) {
                negKeywords.add(kwId)
            }
        }

        return ParsedQueryIntent(
            positiveText = positivePart.ifBlank { lower },
            negatedText = negatedClause,
            negatedGenres = negGenres.distinct(),
            negatedKeywords = negKeywords.distinct(),
            negatedTokens = negatedTokens
        )
    }

    private fun mapTokenToGenreId(token: String): Long? {
        return when (token) {
            "azione", "action" -> 28L
            "avventura", "adventure" -> 12L
            "animazione", "animation", "cartone", "cartoni", "anime" -> 16L
            "commedia", "comedy", "comico", "comici", "divertente" -> 35L
            "crime", "crimine", "poliziesco", "gangster" -> 80L
            "documentario", "documentary" -> 99L
            "dramma", "drammatico", "drama" -> 18L
            "famiglia", "family" -> 10751L
            "fantasy", "fantastico" -> 14L
            "storia", "storico", "history" -> 36L
            "horror", "orrore", "paura" -> 27L
            "musica", "musical", "music" -> 10402L
            "mistero", "mystery", "giallo" -> 9648L
            "romantico", "romance", "amore", "sentimentale" -> 10749L
            "fantascienza", "sci-fi", "scifi", "science fiction",
            "spazio-temporale", "spaziotemporale", "spazio temporale",
            "viaggio spazio-temporale", "viaggio spaziotemporale",
            "viaggio temporale", "viaggi temporali", "viaggio nel tempo", "viaggi nel tempo",
            "time travel", "space-time", "spacetime" -> 878L
            "thriller", "suspense" -> 53L
            "guerra", "war", "bellico" -> 10752L
            "western" -> 37L
            else -> null
        }
    }

    private fun findFuzzyDictionaryMatch(token: String, dict: Map<String, Long>): Long? {
        // Safety guard: Never fuzzy match short words (<= 5 chars) to prevent accidental collisions (e.g. drench/trench, che/chef, bat/cat)
        if (token.length <= 5) return null
        val maxDist = if (token.length in 6..7) 1 else 2
        for ((key, id) in dict) {
            if (key.length <= 5) continue
            if (abs(key.length - token.length) <= maxDist) {
                if (damerauLevenshtein(token, key) <= maxDist) {
                    return id
                }
            }
        }
        return null
    }

    private fun stemWord(word: String, isItalian: Boolean): String {
        val w = word.trim().lowercase(Locale.ROOT)
        if (w.length <= 3) return w
        return if (isItalian) stemItalian(w) else stemEnglish(w)
    }

    private fun stemItalian(word: String): String {
        var w = word
        // Remove common Italian suffixes (longest first)
        val suffixes = listOf(
            "amente", "amento", "amenti", "azione", "azioni", "atore", "atori",
            "atrice", "atrici", "issimo", "issima", "issimi", "issime", "evole",
            "evoli", "abile", "abili", "anza", "anze", "enza", "enze", "ando",
            "endo", "ante", "ente", "anti", "enti", "arono", "erono", "irono",
            "avano", "evano", "ivano", "eranno", "iranno", "ebbe", "ebbero",
            "ammo", "emmo", "immo", "asse", "assero", "esse", "essero",
            "isse", "issero", "are", "ere", "ire", "ato", "ata", "ati", "ate",
            "uto", "uta", "uti", "ute", "ito", "ita", "iti", "ite"
        )
        for (suffix in suffixes) {
            if (w.endsWith(suffix) && (w.length - suffix.length) >= 3) {
                w = w.substring(0, w.length - suffix.length)
                break
            }
        }
        // Vocalic suffix reduction: strip trailing vowel if length > 3
        if (w.length > 3 && (w.endsWith("e") || w.endsWith("o") || w.endsWith("a") || w.endsWith("i"))) {
            w = w.substring(0, w.length - 1)
        }
        return w
    }

    private fun stemEnglish(word: String): String {
        var w = word
        // Plural / s-ending
        if (w.endsWith("sses")) w = w.dropLast(2)
        else if (w.endsWith("ies") && w.length > 4) w = w.dropLast(2)
        else if (!w.endsWith("ss") && w.endsWith("s") && w.length > 3) w = w.dropLast(1)

        val suffixes = listOf(
            "ational", "tional", "ization", "fulness", "ousness", "bility",
            "lessly", "ement", "ment", "able", "ible", "ness", "tion", "sion",
            "izer", "ator", "ality", "ing", "ed", "ly"
        )
        for (suffix in suffixes) {
            if (w.endsWith(suffix) && (w.length - suffix.length) >= 3) {
                w = w.substring(0, w.length - suffix.length)
                break
            }
        }
        return w
    }

    private val iconicPeopleCatalog: Map<String, Long> = mapOf(
        // Directors
        "christopher nolan" to 525L,
        "nolan" to 525L,
        "quentin tarantino" to 138L,
        "tarantino" to 138L,
        "martin scorsese" to 1032L,
        "scorsese" to 1032L,
        "stanley kubrick" to 240L,
        "kubrick" to 240L,
        "steven spielberg" to 488L,
        "spielberg" to 488L,
        "denis villeneuve" to 137427L,
        "villeneuve" to 137427L,
        "david fincher" to 7467L,
        "fincher" to 7467L,
        "alfred hitchcock" to 2636L,
        "hitchcock" to 2636L,
        "ridley scott" to 578L,
        "wes anderson" to 5655L,
        "hayao miyazaki" to 608L,
        "miyazaki" to 608L,
        "guillermo del toro" to 10828L,
        "del toro" to 10828L,
        "james cameron" to 2710L,
        "tim burton" to 510L,
        "bong joon-ho" to 21684L,
        "bong joon ho" to 21684L,
        "francis ford coppola" to 1776L,
        "coppola" to 1776L,
        "federico fellini" to 4415L,
        "fellini" to 4415L,
        "sergio leone" to 4385L,
        "paolo sorrentino" to 11517L,
        "sorrentino" to 11517L,
        "dario argento" to 10959L,
        "david lynch" to 5602L,
        "greta gerwig" to 63834L,
        "jordan peele" to 97844L,
        "yorgos lanthimos" to 228138L,
        "lanthimos" to 228138L,
        "woody allen" to 1243L,
        "clint eastwood" to 190L,
        "peter jackson" to 108L,
        "george lucas" to 1L,
        "akira kurosawa" to 5026L,
        "kurosawa" to 5026L,
        "ingmar bergman" to 6650L,
        "bergman" to 6650L,
        "pedro almodovar" to 444L,
        "almodovar" to 444L,
        "paul thomas anderson" to 4762L,
        "guy ritchie" to 956L,
        "sam raimi" to 7623L,
        "damien chazelle" to 1310663L,
        "chazelle" to 1310663L,

        // Actors
        "leonardo dicaprio" to 6193L,
        "dicaprio" to 6193L,
        "brad pitt" to 287L,
        "tom cruise" to 500L,
        "johnny depp" to 85L,
        "robert de niro" to 380L,
        "de niro" to 380L,
        "al pacino" to 1158L,
        "pacino" to 1158L,
        "keanu reeves" to 6384L,
        "christian bale" to 3894L,
        "ryan gosling" to 30614L,
        "tom hanks" to 31L,
        "denzel washington" to 5292L,
        "morgan freeman" to 192L,
        "joaquin phoenix" to 73421L,
        "cillian murphy" to 2037L,
        "matthew mcconaughey" to 10297L,
        "mcconaughey" to 10297L,
        "margot robbie" to 234352L,
        "scarlett johansson" to 1245L,
        "emma stone" to 54693L,
        "meryl streep" to 5064L,
        "natalie portman" to 524L,
        "florence pugh" to 1373737L,
        "timothee chalamet" to 1190668L,
        "chalamet" to 1190668L,
        "zendaya" to 505710L,
        "robert downey jr" to 3223L,
        "harrison ford" to 3L,
        "arnold schwarzenegger" to 1100L,
        "schwarzenegger" to 1100L,
        "sylvester stallone" to 976L,
        "stallone" to 976L
    )

    fun detectIconicPerson(query: String): Pair<String, Long>? {
        val lower = query.lowercase(Locale.ROOT)
        val sortedEntries = iconicPeopleCatalog.entries.sortedByDescending { it.key.length }
        for ((name, id) in sortedEntries) {
            val pattern = Regex("""(?i)\b${Regex.escape(name)}\b""")
            if (pattern.containsMatchIn(lower)) {
                return Pair(name, id)
            }
        }
        return null
    }

    private fun computeBm25Scores(
        candidates: List<Movie>,
        queryTokens: List<String>,
        isItalian: Boolean
    ): Map<Long, Double> {
        if (candidates.isEmpty() || queryTokens.isEmpty()) return emptyMap()

        val k1 = 1.2
        // b=0.55: TMDB overviews have wildly inconsistent lengths (2 lines vs epic essays).
        // Standard b=0.75 would over-penalize films with long synopses despite high relevance.
        // 0.55 relaxes length normalization without disabling it entirely.
        val b = 0.55
        val nDocs = candidates.size.toDouble()

        data class DocTokens(
            val id: Long,
            val titleTokens: List<String>,
            val overviewTokens: List<String>,
            val length: Int,
            val rawOverview: String,
            val rawTitle: String
        )

        val docList = candidates.map { m ->
            val titleStr = (m.title ?: "").lowercase(Locale.ROOT)
            val overviewStr = (m.overview ?: "").lowercase(Locale.ROOT)

            val tTokens = titleStr.split(Regex("""[^\w-]+"""))
                .map { it.trim('-') }
                .filter { it.length > 2 || it.matches(Regex("""\d{2}""")) }
                .flatMap { listOf(stemWord(it, isItalian), stemWord(it, false)) }

            val oTokens = overviewStr.split(Regex("""[^\w-]+"""))
                .map { it.trim('-') }
                .filter { it.length > 2 || it.matches(Regex("""\d{2}""")) }
                .flatMap { listOf(stemWord(it, isItalian), stemWord(it, false)) }

            DocTokens(
                id = m.id,
                titleTokens = tTokens,
                overviewTokens = oTokens,
                length = tTokens.size * 2 + oTokens.size,
                rawOverview = overviewStr,
                rawTitle = titleStr
            )
        }

        val avgDocLen = docList.map { it.length }.average().coerceAtLeast(1.0)
        val expandedQueryTokens = queryTokens.flatMap { token ->
            val en = KeywordDictionary.italianToEnglishSearchTokens[token]
            val extra = mutableListOf<String>()
            if (en != null && en != token) extra.add(en)
            if (token == "morire" || token == "muore" || token == "morto" || token == "morte") {
                extra.addAll(listOf("death", "dies", "dead"))
            }
            if (token == "ricco" || token == "ricchi" || token == "ricchezza") {
                extra.addAll(listOf("rich", "wealth", "tycoon"))
            }
            if (token == "guerra" || token == "bellico") {
                extra.addAll(listOf("war", "battle", "soldier"))
            }
            if (token == "spazio" || token == "spaziale") {
                extra.addAll(listOf("space", "wormhole", "galaxy"))
            }
            if (token == "spazio-temporale" || token == "spaziotemporale" || token == "spazio temporale" ||
                token == "viaggio spazio-temporale" || token == "viaggio spaziotemporale" ||
                token == "viaggio temporale" || token == "viaggi temporali" ||
                token == "viaggio nel tempo" || token == "viaggi nel tempo" || token == "temporale") {
                extra.addAll(listOf("time travel", "temporal", "timeloop", "spacetime", "space-time", "continuum", "paradox", "paradosso"))
            }
            if (token == "topo" || token == "topi" || token == "ratto" || token == "ratti" || token == "topolino") {
                extra.addAll(listOf("mouse", "rat", "rodent", "mice", "rats", "rodents"))
            }
            if (token == "mouse" || token == "mice" || token == "rat" || token == "rats" || token == "rodent") {
                extra.addAll(listOf("topo", "topi", "ratto", "ratti", "roditore", "roditori"))
            }
            if (token == "chef" || token == "cuoco" || token == "cuochi" || token == "cucina" || token == "cucinare") {
                extra.addAll(listOf("chef", "cook", "cooking", "restaurant", "ristorante", "culinary"))
            }
            listOf(token) + extra
        }
        val qStems = (expandedQueryTokens.map { stemWord(it.lowercase(Locale.ROOT), isItalian) } +
                      expandedQueryTokens.map { stemWord(it.lowercase(Locale.ROOT), false) }).distinct()
        val distinctStems = qStems

        val docFreq = mutableMapOf<String, Int>()
        for (stem in distinctStems) {
            val count = docList.count { doc ->
                doc.titleTokens.contains(stem) || doc.overviewTokens.contains(stem)
            }
            docFreq[stem] = count
        }

        val scores = mutableMapOf<Long, Double>()

        for (doc in docList) {
            var docScore = 0.0

            val isMultiWordQuery = queryTokens.size >= 2
            val matchedTitleStems = distinctStems.filter { stem -> doc.titleTokens.contains(stem) }
            val matchedTitleStemCount = matchedTitleStems.size
            val titleCoverage = if (doc.titleTokens.isNotEmpty()) matchedTitleStemCount.toDouble() / doc.titleTokens.size.toDouble() else 0.0

            // Calibrated Robertson–Spärck Jones title weighting:
            // Single generic words in a multi-word descriptive query (e.g. "chef" in "un topo diventa uno chef")
            // must NOT receive an inflated 3x title boost when matching short titles (like 2014's "Chef").
            // Title multiplier is depowered to 1.0 (or 0.75 for 1-word titles on queries with 3+ tokens),
            // and only activates to 2.5x when multiple query terms match the title or in short 1-word queries.
            val titleMultiplier = when {
                !isMultiWordQuery -> 2.5
                matchedTitleStemCount >= 2 -> 2.5
                titleCoverage >= 0.8 && doc.titleTokens.size >= 2 -> 2.0
                doc.titleTokens.size == 1 && queryTokens.size >= 3 -> 0.75
                else -> 1.0
            }

            for (stem in distinctStems) {
                val df = (docFreq[stem] ?: 0).toDouble()
                if (df == 0.0) continue

                val idf = ln(1.0 + (nDocs - df + 0.5) / (df + 0.5)).coerceAtLeast(0.1)
                val tfTitle = doc.titleTokens.count { it == stem }.toDouble()
                val tfOverview = doc.overviewTokens.count { it == stem }.toDouble()
                val effectiveTf = (tfTitle * titleMultiplier) + tfOverview

                if (effectiveTf > 0.0) {
                    val numerator = effectiveTf * (k1 + 1.0)
                    val denominator = effectiveTf + k1 * (1.0 - b + b * (doc.length.toDouble() / avgDocLen))
                    docScore += idf * (numerator / denominator)
                }
            }

            // Multi-word N-gram scoring: if bigrams or trigrams from the query appear contiguously
            // in title or overview, apply massive bonus (defeats movies that only have scattered words)
            val multiWordPhrases = queryTokens.filter { it.contains(" ") && it.length >= 5 }.distinct()
            for (phrase in multiWordPhrases) {
                val phraseEn = KeywordDictionary.italianToEnglishSearchTokens[phrase]
                val searchTargets = listOfNotNull(phrase, phraseEn).map { it.lowercase(Locale.ROOT) }
                for (target in searchTargets) {
                    if (doc.rawTitle.contains(target)) {
                        docScore += 22.0
                    } else if (doc.rawOverview.contains(target)) {
                        docScore += 16.0
                    }
                }
            }

            scores[doc.id] = docScore
        }

        return scores
    }
}
