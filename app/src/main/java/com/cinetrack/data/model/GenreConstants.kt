package com.cinetrack.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class Genre(val id: Long = 0L, val name: String = "")

object GenreConstants {
    val MOVIE_GENRES = listOf(
        Genre(28, "Azione"),
        Genre(12, "Avventura"),
        Genre(16, "Animazione"),
        Genre(35, "Commedia"),
        Genre(80, "Crime"),
        Genre(99, "Documentario"),
        Genre(18, "Dramma"),
        Genre(10751, "Famiglia"),
        Genre(14, "Fantasy"),
        Genre(36, "Storia"),
        Genre(27, "Horror"),
        Genre(10402, "Musica"),
        Genre(9648, "Mistero"),
        Genre(10749, "Romance"),
        Genre(878, "Fantascienza"),
        Genre(10770, "Film TV"),
        Genre(53, "Thriller"),
        Genre(10752, "Guerra"),
        Genre(37, "Western")
    )

    val TV_GENRES = listOf(
        Genre(10759, "Action & Adventure"),
        Genre(16, "Animazione"),
        Genre(35, "Commedia"),
        Genre(80, "Crime"),
        Genre(99, "Documentario"),
        Genre(18, "Dramma"),
        Genre(10751, "Famiglia"),
        Genre(10762, "Kids"),
        Genre(9648, "Mistero"),
        Genre(10763, "News"),
        Genre(10764, "Reality"),
        Genre(10765, "Sci-Fi & Fantasy"),
        Genre(10766, "Soap"),
        Genre(10767, "Talk"),
        Genre(10768, "War & Politics"),
        Genre(37, "Western")
    )

    val ALL_GENRES = (MOVIE_GENRES + TV_GENRES).distinctBy { it.id }

    private val ENGLISH_NAMES = mapOf(
        28L to "Action",
        12L to "Adventure",
        16L to "Animation",
        35L to "Comedy",
        80L to "Crime",
        99L to "Documentary",
        18L to "Drama",
        10751L to "Family",
        14L to "Fantasy",
        36L to "History",
        27L to "Horror",
        10402L to "Music",
        9648L to "Mystery",
        10749L to "Romance",
        878L to "Science Fiction",
        10770L to "TV Movie",
        53L to "Thriller",
        10752L to "War",
        37L to "Western",
        10759L to "Action & Adventure",
        10762L to "Kids",
        10763L to "News",
        10764L to "Reality",
        10765L to "Sci-Fi & Fantasy",
        10766L to "Soap",
        10767L to "Talk",
        10768L to "War & Politics"
    )

    private val SPANISH_NAMES = mapOf(
        28L to "Acción",
        12L to "Aventura",
        16L to "Animación",
        35L to "Comedia",
        80L to "Crimen",
        99L to "Documental",
        18L to "Drama",
        10751L to "Familia",
        14L to "Fantasía",
        36L to "Historia",
        27L to "Terror",
        10402L to "Música",
        9648L to "Misterio",
        10749L to "Romance",
        878L to "Ciencia ficción",
        10770L to "Película de TV",
        53L to "Suspense",
        10752L to "Bélica",
        37L to "Western",
        10759L to "Acción y Aventura",
        10762L to "Kids",
        10763L to "Noticias",
        10764L to "Reality",
        10765L to "Sci-Fi y Fantasía",
        10766L to "Telenovela",
        10767L to "Talk Show",
        10768L to "Bélica y Política"
    )

    private val FRENCH_NAMES = mapOf(
        28L to "Action",
        12L to "Aventure",
        16L to "Animation",
        35L to "Comédie",
        80L to "Crime",
        99L to "Documentaire",
        18L to "Drame",
        10751L to "Familial",
        14L to "Fantastique",
        36L to "Histoire",
        27L to "Horreur",
        10402L to "Musique",
        9648L to "Mystère",
        10749L to "Romance",
        878L to "Science-Fiction",
        10770L to "Téléfilm",
        53L to "Thriller",
        10752L to "Guerre",
        37L to "Western",
        10759L to "Action & Aventure",
        10762L to "Kids",
        10763L to "Actualités",
        10764L to "Télé-réalité",
        10765L to "Sci-Fi & Fantastique",
        10766L to "Feuilleton",
        10767L to "Talk-show",
        10768L to "Guerre & Politique"
    )

    private val GERMAN_NAMES = mapOf(
        28L to "Action",
        12L to "Abenteuer",
        16L to "Animation",
        35L to "Komödie",
        80L to "Krimi",
        99L to "Dokumentarfilm",
        18L to "Drama",
        10751L to "Familie",
        14L to "Fantasy",
        36L to "Historie",
        27L to "Horror",
        10402L to "Musik",
        9648L to "Mystery",
        10749L to "Liebesfilm",
        878L to "Science Fiction",
        10770L to "TV-Film",
        53L to "Thriller",
        10752L to "Kriegsfilm",
        37L to "Western",
        10759L to "Action & Adventure",
        10762L to "Kids",
        10763L to "Nachrichten",
        10764L to "Reality",
        10765L to "Sci-Fi & Fantasy",
        10766L to "Soap",
        10767L to "Talkshow",
        10768L to "Krieg & Politik"
    )

    private val PORTUGUESE_NAMES = mapOf(
        28L to "Ação",
        12L to "Aventura",
        16L to "Animação",
        35L to "Comédia",
        80L to "Crime",
        99L to "Documentário",
        18L to "Drama",
        10751L to "Família",
        14L to "Fantasia",
        36L to "História",
        27L to "Terror",
        10402L to "Música",
        9648L to "Mistério",
        10749L to "Romance",
        878L to "Ficção científica",
        10770L to "Cinema TV",
        53L to "Thriller",
        10752L to "Guerra",
        37L to "Faroeste",
        10759L to "Ação e Aventura",
        10762L to "Kids",
        10763L to "Notícias",
        10764L to "Reality",
        10765L to "Sci-Fi e Fantasia",
        10766L to "Novela",
        10767L to "Talk-show",
        10768L to "Guerra e Política"
    )

    private val RUSSIAN_NAMES = mapOf(
        28L to "Боевик",
        12L to "Приключения",
        16L to "Мультфильм",
        35L to "Комедия",
        80L to "Криминал",
        99L to "Документальный",
        18L to "Драма",
        10751L to "Семейный",
        14L to "Фэнтези",
        36L to "История",
        27L to "Ужасы",
        10402L to "Музыка",
        9648L to "Детектив",
        10749L to "Мелодрама",
        878L to "Фантастика",
        10770L to "Телефильм",
        53L to "Триллер",
        10752L to "Военный",
        37L to "Вестерн",
        10759L to "Боевик и Приключения",
        10762L to "Детский",
        10763L to "Новости",
        10764L to "Реалити-шоу",
        10765L to "НФ и Фэнтези",
        10766L to "Мыльная опера",
        10767L to "Ток-шоу",
        10768L to "Война и политика"
    )

    private val JAPANESE_NAMES = mapOf(
        28L to "アクション",
        12L to "アドベンチャー",
        16L to "アニメーション",
        35L to "コメディ",
        80L to "犯罪",
        99L to "ドキュメンタリー",
        18L to "ドラマ",
        10751L to "ファミリー",
        14L to "ファンタジー",
        36L to "歴史",
        27L to "ホラー",
        10402L to "音楽",
        9648L to "ミステリー",
        10749L to "ロマンス",
        878L to "SF",
        10770L to "テレビ映画",
        53L to "スリラー",
        10752L to "戦争",
        37L to "西部劇",
        10759L to "アクション＆アドベンチャー",
        10762L to "キッズ",
        10763L to "ニュース",
        10764L to "リアリティ",
        10765L to "SF & ファンタジー",
        10766L to "ソープ",
        10767L to "トーク",
        10768L to "軍事＆政治"
    )

    private val KOREAN_NAMES = mapOf(
        28L to "액션",
        12L to "모험",
        16L to "애니메이션",
        35L to "코미디",
        80L to "범죄",
        99L to "다큐멘터리",
        18L to "드라마",
        10751L to "가족",
        14L to "판타지",
        36L to "역사",
        27L to "공포",
        10402L to "음악",
        9648L to "미스터리",
        10749L to "로맨스",
        878L to "SF",
        10770L to "TV 영화",
        53L to "스릴러",
        10752L to "전쟁",
        37L to "서부",
        10759L to "액션 & 어드벤처",
        10762L to "키즈",
        10763L to "뉴스",
        10764L to "리얼리티",
        10765L to "SF & 판타지",
        10766L to "연속극",
        10767L to "토크",
        10768L to "전쟁 & 정치"
    )

    private val CHINESE_NAMES = mapOf(
        28L to "动作",
        12L to "冒险",
        16L to "动画",
        35L to "喜剧",
        80L to "犯罪",
        99L to "纪录",
        18L to "剧情",
        10751L to "家庭",
        14L to "奇幻",
        36L to "历史",
        27L to "恐怖",
        10402L to "音乐",
        9648L to "悬疑",
        10749L to "爱情",
        878L to "科幻",
        10770L to "电视电影",
        53L to "惊悚",
        10752L to "战争",
        37L to "西部",
        10759L to "动作冒险",
        10762L to "儿童",
        10763L to "新闻",
        10764L to "真人秀",
        10765L to "科幻与奇幻",
        10766L to "肥皂剧",
        10767L to "脱口秀",
        10768L to "战争与政治"
    )

    private val TURKISH_NAMES = mapOf(
        28L to "Aksiyon",
        12L to "Macera",
        16L to "Animasyon",
        35L to "Komedi",
        80L to "Suç",
        99L to "Belgesel",
        18L to "Dram",
        10751L to "Aile",
        14L to "Fantastik",
        36L to "Tarih",
        27L to "Korku",
        10402L to "Müzik",
        9648L to "Gizem",
        10749L to "Romantik",
        878L to "Bilim-Kurgu",
        10770L to "TV Filmi",
        53L to "Gerilim",
        10752L to "Savaş",
        37L to "Vahşi Batı",
        10759L to "Aksiyon & Macera",
        10762L to "Çocuklar",
        10763L to "Haberler",
        10764L to "Realite",
        10765L to "Bilim Kurgu & Fantazi",
        10766L to "Pembe Dizi",
        10767L to "Talk Show",
        10768L to "Savaş & Politik"
    )

    private val INDONESIAN_NAMES = mapOf(
        28L to "Aksi",
        12L to "Petualangan",
        16L to "Animasi",
        35L to "Komedi",
        80L to "Kejahatan",
        99L to "Dokumenter",
        18L to "Drama",
        10751L to "Keluarga",
        14L to "Fantasi",
        36L to "Sejarah",
        27L to "Horor",
        10402L to "Musik",
        9648L to "Misteri",
        10749L to "Romantis",
        878L to "Fiksi Ilmiah",
        10770L to "Film Televisi",
        53L to "Sensasi",
        10752L to "Perang",
        37L to "Koboi",
        10759L to "Aksi & Petualangan",
        10762L to "Anak-anak",
        10763L to "Berita",
        10764L to "Realitas",
        10765L to "Fiksi Ilmiah & Fantasi",
        10766L to "Sabun",
        10767L to "Obrolan",
        10768L to "Perang & Politik"
    )

    private val HINDI_NAMES = mapOf(
        28L to "एक्शन",
        12L to "रोमांच",
        16L to "एनीमेशन",
        35L to "कॉमेडी",
        80L to "अपराध",
        99L to "वृत्तचित्र",
        18L to "ड्रामा",
        10751L to "पारिवारिक",
        14L to "काल्पनिक",
        36L to "इतिहास",
        27L to "हॉरर",
        10402L to "संगीत",
        9648L to "मिस्ट्री",
        10749L to "रोमांस",
        878L to "साइंस फिक्शन",
        10770L to "टीवी फिल्म",
        53L to "थ्रिलर",
        10752L to "युद्ध",
        37L to "वेस्टर्न",
        10759L to "एक्शन और एडवेंचर",
        10762L to "किड्स",
        10763L to "समाचार",
        10764L to "रियलिटी",
        10765L to "साइंस-फिक्शन और फैंटेसी",
        10766L to "सोप",
        10767L to "टॉक",
        10768L to "युद्ध और राजनीति"
    )

    fun getLocalizedName(id: Long, languageCode: String, defaultName: String): String {
        val lang = languageCode.lowercase()
        return when {
            lang.startsWith("it") -> defaultName
            lang.startsWith("es") -> SPANISH_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("fr") -> FRENCH_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("de") -> GERMAN_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("pt") -> PORTUGUESE_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("ru") -> RUSSIAN_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("ja") -> JAPANESE_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("ko") -> KOREAN_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("zh") -> CHINESE_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("tr") -> TURKISH_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("id") || lang.startsWith("in") -> INDONESIAN_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            lang.startsWith("hi") -> HINDI_NAMES[id] ?: ENGLISH_NAMES[id] ?: defaultName
            else -> ENGLISH_NAMES[id] ?: defaultName
        }
    }
}
