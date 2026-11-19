package com.cinetrack.data.repository.mood

/**
 * The catalog of cinematic archetypes and atmospheric modifiers recognized
 * by LocalMoodSearchEngine.
 */
object MoodArchetypes {

    val archetypeCatalog: List<CinematicArchetype> = listOf(
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

    val atmosphereCatalog: List<AtmosphericModifier> = listOf(
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
}
