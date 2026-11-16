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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Singleton
class LocalMoodSearchEngine @Inject constructor(
    private val tmdbService: TMDBService
) {

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
            triggersIt = listOf("time loop", "loop temporale", "giorno che si ripete", "rivivere lo stesso giorno", "viaggio nel tempo", "viaggi nel tempo", "paradosso temporale", "macchina del tempo"),
            triggersEn = listOf("time loop", "repeating day", "reliving the same day", "time travel", "temporal paradox", "time machine", "groundhog day style"),
            primaryGenres = listOf(878L),
            secondaryGenres = listOf(53L, 35L, 9648L),
            mandatoryKeywords = listOf(10854L, 4379L),
            secondaryKeywords = listOf(191132L, 207317L),
            anchorMovieIds = listOf(137L, 137113L, 587792L, 45612L, 26466L, 122906L, 63L),
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
            anchorMovieIds = listOf(593643L, 2062L, 286567L, 820067L, 212778L, 244786L),
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
            triggersIt = listOf("cyberpunk", "hacker", "megacorporazioni", "innesti bionici", "cyborg", "neon e pioggia", "futuro distopico", "blade runner style", "intelligenza artificiale ribelle"),
            triggersEn = listOf("cyberpunk", "hacker", "megacorporation", "bionic implants", "cyborg", "neon rain", "dystopian future", "blade runner style", "rogue ai"),
            primaryGenres = listOf(878L),
            secondaryGenres = listOf(28L, 53L),
            mandatoryKeywords = listOf(12190L, 2157L, 310L),
            secondaryKeywords = listOf(4565L, 350338L),
            anchorMovieIds = listOf(335984L, 603L, 9323L, 149L, 180L, 49049L, 500664L),
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
            triggersIt = listOf("neo-noir", "noir moderno", "metropoli notturna", "pioggia e asfalto", "detective disilluso", "corruzione metropolitana", "luci al neon", "drive style", "se7en style"),
            triggersEn = listOf("neo-noir", "modern noir", "nocturnal city", "rainy streets", "cynical detective", "urban decay", "neon-drenched", "drive style", "se7en style"),
            primaryGenres = listOf(53L, 80L),
            secondaryGenres = listOf(9648L, 18L),
            mandatoryKeywords = listOf(9016L, 12190L, 703L),
            secondaryKeywords = listOf(9826L),
            anchorMovieIds = listOf(329L, 64690L, 242582L, 807L, 2118L, 414906L),
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
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        // Detect whether the query is Italian, regardless of system locale settings
        val isItalian = detectItalian(cleanQuery, language)
        val tmdbLang = if (isItalian) "it-IT" else "en-US"
        val todayStr = LocalDate.now().toString()

        // 1. NLP Query Parsing: Time Horizons & Eras
        var specificYear: Int? = null
        var startYear: Int? = null
        var endYear: Int? = null

        val singleYearRegex = Regex("""\b(19\d{2}|20[0-2]\d)\b""")
        val decadeItRegex = Regex("""\banni\s*(')?(\d{2}|\d{4})\b""")
        val decadeEnRegex = Regex("""\b(\d{2}|\d{4})(')?s\b""")

        val singleYearMatch = singleYearRegex.find(cleanQuery)
        val decadeItMatch = decadeItRegex.find(cleanQuery)
        val decadeEnMatch = decadeEnRegex.find(cleanQuery)

        if (decadeItMatch != null) {
            val raw = decadeItMatch.groupValues[2]
            val yr = if (raw.length == 2) {
                val num = raw.toIntOrNull() ?: 80
                if (num >= 30) 1900 + num else 2000 + num
            } else raw.toIntOrNull() ?: 1980
            startYear = (yr / 10) * 10
            endYear = startYear + 9
        } else if (decadeEnMatch != null) {
            val raw = decadeEnMatch.groupValues[1]
            val yr = if (raw.length == 2) {
                val num = raw.toIntOrNull() ?: 80
                if (num >= 30) 1900 + num else 2000 + num
            } else raw.toIntOrNull() ?: 1980
            startYear = (yr / 10) * 10
            endYear = startYear + 9
        } else if (singleYearMatch != null) {
            specificYear = singleYearMatch.value.toIntOrNull()
        }

        val isVintageRequested = cleanQuery.contains("vintage") || cleanQuery.contains("classico") || cleanQuery.contains("bianco e nero") || cleanQuery.contains("black and white")
        if (isVintageRequested && startYear == null) {
            endYear = 1979
        }

        val isAnimationRequested = cleanQuery.contains("animazione") || cleanQuery.contains("cartone") || cleanQuery.contains("anime") || cleanQuery.contains("animation") || cleanQuery.contains("animated")

        // 2. Multi-Level Archetype & Atmosphere Detection (Check both Italian and English triggers)
        val isTransformationIntent = detectTransformationIntent(cleanQuery)
        val matchedArchetypes = archetypeCatalog.filter { archetype ->
            if (isTransformationIntent && archetype.id == "metamorphosis_mutation") {
                return@filter true
            }
            val triggers = if (isItalian) archetype.triggersIt else archetype.triggersEn
            val alternateTriggers = if (isItalian) archetype.triggersEn else archetype.triggersIt
            triggers.any { trigger -> matchesWordOrFuzzy(cleanQuery, trigger) } ||
            alternateTriggers.any { trigger -> matchesWordOrFuzzy(cleanQuery, trigger) }
        }

        val matchedAtmospheres = atmosphereCatalog.filter { atm ->
            val triggers = if (isItalian) atm.triggersIt else atm.triggersEn
            val alternateTriggers = if (isItalian) atm.triggersEn else atm.triggersIt
            triggers.any { trigger -> matchesWordOrFuzzy(cleanQuery, trigger) } ||
            alternateTriggers.any { trigger -> matchesWordOrFuzzy(cleanQuery, trigger) }
        }

        // 3. Keyword Extraction with Strict Boundaries & Dynamic TMDB Resolution
        val dictionary = KeywordDictionary.getDictionaryForLanguage(if (isItalian) "it" else "en")
        val fallbackDictionary = KeywordDictionary.getDictionaryForLanguage(if (isItalian) "en" else "it")
        val matchedKeywords = mutableListOf<Long>()
        val contentTokens = extractSignificantContentTokens(cleanQuery, isItalian)

        for (token in contentTokens) {
            val kwId = dictionary[token] ?: fallbackDictionary[token]
            if (kwId != null && !matchedKeywords.contains(kwId)) {
                matchedKeywords.add(kwId)
            }
        }

        // Dynamic Keyword Resolution via TMDB searchKeyword API (with Italian->English token mapping)
        val unmappedTokens = contentTokens.filter { token ->
            token.length >= 4 && !dictionary.containsKey(token) && !fallbackDictionary.containsKey(token)
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
                        try {
                            val kwRes = tmdbService.searchKeyword(searchToken)
                            kwRes.getList().firstOrNull()?.id
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                for (id in deferredKw.mapNotNull { it.await() }) {
                    if (!matchedKeywords.contains(id)) {
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
        targetKeywords.addAll(matchedKeywords)

        val distinctGenres = targetGenres.distinct()
        val distinctKeywords = targetKeywords.distinct()

        // 4. Multi-Stream Concurrent Candidate Generation
        val candidateMap = mutableMapOf<Long, Movie>()

        coroutineScope {
            // Stream A: Subgenre Discover (Quality focus, ONLY if genres or keywords matched!)
            val streamA = async {
                if (distinctGenres.isNotEmpty() || distinctKeywords.isNotEmpty()) {
                    try {
                        val opts = mutableMapOf<String, String>(
                            "language" to tmdbLang,
                            "include_adult" to "false",
                            "vote_count.gte" to "100",
                            "sort_by" to "vote_average.desc",
                            "primary_release_date.lte" to (endYear?.let { "$it-12-31" } ?: todayStr)
                        )
                        if (distinctGenres.isNotEmpty()) {
                            opts["with_genres"] = distinctGenres.take(2).joinToString("|")
                        }
                        if (distinctKeywords.isNotEmpty()) {
                            opts["with_keywords"] = distinctKeywords.take(4).joinToString("|")
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

            // Stream B: Subgenre Discover (Popularity & Crowd focus, ONLY if genres or keywords matched!)
            val streamB = async {
                if (distinctGenres.isNotEmpty() || distinctKeywords.isNotEmpty()) {
                    try {
                        val opts = mutableMapOf<String, String>(
                            "language" to tmdbLang,
                            "include_adult" to "false",
                            "vote_count.gte" to "80",
                            "sort_by" to "popularity.desc",
                            "primary_release_date.lte" to (endYear?.let { "$it-12-31" } ?: todayStr)
                        )
                        if (distinctGenres.isNotEmpty()) {
                            opts["with_genres"] = distinctGenres.take(2).joinToString("|")
                        }
                        if (distinctKeywords.isNotEmpty()) {
                            opts["with_keywords"] = distinctKeywords.take(4).joinToString("|")
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

            // Stream C: Curated Golden Anchor Movie Injector (Always fetch if archetype matched)
            val streamC = async {
                val anchorIds = matchedArchetypes.flatMap { it.anchorMovieIds }.distinct()
                val list = mutableListOf<Movie>()
                for (id in anchorIds.take(7)) {
                    try {
                        val details = tmdbService.getMovieBasicDetails(id)
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
                        list.add(m)
                    } catch (_: Exception) {
                        // ignore anchor fetch fail
                    }
                }
                list
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
                // Only search individual tokens if no archetypes matched and token is long enough (>= 5)
                if (matchedArchetypes.isEmpty()) {
                    for (token in contentTokens.take(2)) {
                        if (token.length >= 5) {
                            try {
                                val res = tmdbService.searchMovie(token, page = page)
                                list.addAll(res.results.take(4))
                            } catch (_: Exception) {
                                // ignore
                            }
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

        val scoredList = candidateMap.values.mapNotNull { m ->
            var thematicRelevanceScore = 0.0

            // A. Archetype & Genre Relevancy Score
            val movieGenres = m.genreIds ?: emptyList()
            if (primaryArchetype != null) {
                val isAnchor = primaryArchetype.anchorMovieIds.contains(m.id)
                if (isAnchor) thematicRelevanceScore += 50.0

                val primaryOverlap = movieGenres.count { primaryArchetype.primaryGenres.contains(it) }
                val secondaryOverlap = movieGenres.count { primaryArchetype.secondaryGenres.contains(it) }
                thematicRelevanceScore += (primaryOverlap * 14.0).coerceAtMost(28.0)
                thematicRelevanceScore += (secondaryOverlap * 6.0).coerceAtMost(12.0)

                // Penalty for unrequested animation in serious/gritty archetypes
                if (!isAnimationRequested && movieGenres.contains(16L)) {
                    thematicRelevanceScore -= 35.0
                }
            } else if (distinctGenres.isNotEmpty()) {
                val overlap = movieGenres.count { distinctGenres.contains(it) }
                thematicRelevanceScore += (overlap * 12.0).coerceAtMost(30.0)
            }

            if (isAnimationRequested && movieGenres.contains(16L)) {
                thematicRelevanceScore += 25.0
            }

            // B. Keyword Matches
            val movieKwOverlap = movieGenres.count { distinctKeywords.contains(it) }
            if (movieKwOverlap > 0) {
                thematicRelevanceScore += (movieKwOverlap * 15.0)
            }

            // C. Semantic Overview & Title Alignment
            val overviewLower = (m.overview ?: "").lowercase(Locale.ROOT)
            val titleLower = (m.title ?: "").lowercase(Locale.ROOT)
            var textMatchBonus = 0.0
            var matchedTokenCount = 0
            for (token in contentTokens) {
                if (token.length >= 4) {
                    if (overviewLower.contains(token)) {
                        textMatchBonus += 12.0
                        matchedTokenCount++
                    }
                    if (titleLower.contains(token)) {
                        textMatchBonus += 18.0
                        matchedTokenCount++
                    }
                }
            }
            thematicRelevanceScore += textMatchBonus.coerceAtMost(40.0)

            // HARD GATE: ZERO THEMATIC RELEVANCE -> STRICT EXCLUSION!
            // Quality score (Bayesian) must NEVER allow a completely unrelated movie to pass!
            // A movie must have substantive grounding: Anchor, Primary Genre Match, Keyword Match, or Text Token Match!
            val primaryOverlap = if (primaryArchetype != null) {
                movieGenres.count { primaryArchetype.primaryGenres.contains(it) }
            } else if (distinctGenres.isNotEmpty()) {
                movieGenres.count { distinctGenres.contains(it) }
            } else 0
            val isAnchor = primaryArchetype?.anchorMovieIds?.contains(m.id) == true
            val hasSubstantiveThematicGrounding = isAnchor || primaryOverlap > 0 || movieKwOverlap > 0 || matchedTokenCount > 0

            if (!hasSubstantiveThematicGrounding || thematicRelevanceScore <= 0.0) {
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
            val franchiseKey = detectFranchiseKey(m.title ?: "")
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
                aiReason = null
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

    private fun detectFranchiseKey(title: String): String? {
        val lower = title.lowercase(Locale.ROOT)
        for ((key, triggers) in franchiseSignatures) {
            if (triggers.any { lower.contains(it) }) {
                return key
            }
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
        val transformationMarkers = listOf(
            "become", "becomes", "became", "becoming", "turned into", "turns into", "turning into",
            "transformed", "transforming", "transformation", "mutates", "mutated", "mutation",
            "bitten by", "bit by", "diventa", "diventare", "diventano", "diventato", "diventata",
            "trasforma", "trasformarsi", "trasformano", "trasformato", "trasformazione",
            "muta", "mutare", "mutato", "morso da"
        )
        return transformationMarkers.any { lower.contains(it) }
    }

    private fun extractSignificantContentTokens(query: String, isItalian: Boolean): List<String> {
        val stopwordsIt = setOf(
            "un", "uno", "una", "dei", "degli", "delle", "il", "lo", "la", "i", "gli", "le",
            "in", "su", "a", "da", "con", "senza", "per", "tra", "fra", "di", "del", "dello", "della",
            "che", "chi", "cui", "dove", "come", "quando", "cosa", "perché", "perche",
            "film", "cinema", "pellicola", "storia", "racconto", "tipo", "stile", "genere", "simile",
            "alla", "allo", "agli", "alle", "vorrei", "vedere", "consigliami", "cercami", "mostrami",
            "trovami", "consigli", "qualcosa", "bello", "belli", "famoso", "famosi", "migliori", "migliore",
            "diventa", "diventare", "diventano", "essere", "stato", "sono", "ha", "hanno", "chi", "quale", "quali",
            "persone", "persona", "gente", "qualcuno", "alcuni", "alcune", "uomo", "uomini", "donna", "donne",
            "ragazzo", "ragazzi", "ragazza", "ragazze", "tutti", "tutto", "tutta", "tutte", "molti", "molte",
            "ogni", "ciascuno", "altro", "altri", "altre", "loro", "essi", "esse", "cose", "fanno", "fa", "fare", "guarda", "guardare"
        )
        val stopwordsEn = setOf(
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
            "bunch", "thing", "things", "stuff", "watch", "watching", "see", "seeing"
        )
        val stopwords = if (isItalian) stopwordsIt else stopwordsEn

        return Regex("""\b\w+\b""")
            .findAll(query.lowercase(Locale.ROOT))
            .map { it.value }
            .filter { it.length >= 3 && !stopwords.contains(it) }
            .toList()
    }

    private fun matchesWordOrFuzzy(query: String, target: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        val t = target.lowercase(Locale.ROOT)
        if (containsWholeWordOrPhrase(q, t)) return true

        // If multi-word trigger, check if all significant words match in sequence or proximity
        val tWords = t.split(" ").filter { it.length >= 3 }
        if (tWords.size > 1) {
            return tWords.all { word ->
                containsWholeWordOrPhrase(q, word) || fuzzyMatchTokenInQuery(q, word)
            }
        }

        // Fuzzy match single token only
        return fuzzyMatchTokenInQuery(q, t)
    }

    private fun fuzzyMatchTokenInQuery(query: String, target: String): Boolean {
        val queryTokens = Regex("""\b\w+\b""").findAll(query).map { it.value }
        for (token in queryTokens) {
            if (token == target) return true
            if (target.length >= 6 && token.startsWith(target.take(5))) return true
            if (token.length >= 5 && target.endsWith(token)) return true // e.g. 'greta' vs 'segreta'
            val dist = damerauLevenshtein(token, target)
            if (target.length in 4..6 && dist <= 1) return true
            if (target.length >= 7 && dist <= 2) return true
        }
        return false
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
}
