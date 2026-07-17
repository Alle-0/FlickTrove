package com.cinetrack.ui.model

import com.cinetrack.data.model.Movie

fun Movie.generateBadges(showAdvancedBadges: Boolean = false): List<MovieBadge> {
    val badges = mutableListOf<MovieBadge>()
    val voteAvg = voteAverage ?: 0.0
    val votes = voteCount ?: 0
    val newEps = newEpisodesFound ?: 0
    
    if (newEps > 0 || isUpcoming == true) badges.add(MovieBadge.NEW)
    
    if (voteAvg >= 8.8 && votes > 2000) badges.add(MovieBadge.MASTERPIECE)
    else if (voteAvg >= 8.5 && votes > 300) badges.add(MovieBadge.BEST)
    else if (votes > 3000) badges.add(MovieBadge.HOT)
    else if (voteAvg >= 8.0 && votes > 1000) badges.add(MovieBadge.WOW)
    else if (voteAvg >= 7.5 && votes in 50..500) badges.add(MovieBadge.HIDDEN_GEM)
    
    if (votes > 1000 && voteAvg >= 5.0 && voteAvg <= 6.5) badges.add(MovieBadge.DIVISIVE)
    
    val rYear = releaseYear?.toIntOrNull() ?: releaseDate?.take(4)?.toIntOrNull() ?: firstAirDate?.take(4)?.toIntOrNull() ?: 9999
    if (rYear < 1970) badges.add(MovieBadge.VINTAGE)
    else if (rYear < 1990 && voteAvg >= 7.0) badges.add(MovieBadge.CLASSIC)
    else if (rYear in 1990..2010 && voteAvg >= 8.0) badges.add(MovieBadge.CULT)

    if (showAdvancedBadges) {
        val rev = revenue ?: 0L
        if (rev > 500_000_000L) badges.add(MovieBadge.BLOCKBUSTER)
        else if ((budget ?: 0L) in 1L..5_000_000L && voteAvg >= 7.0) badges.add(MovieBadge.INDIE)

        val rt = runtime ?: 0
        if (rt > 160) badges.add(MovieBadge.EPIC)
        else if (mediaType != "tv" && rt in 1..89) badges.add(MovieBadge.QUICK)

        if ((numberOfSeasons ?: 0) >= 5 || (numberOfEpisodes ?: 0) > 50) badges.add(MovieBadge.BINGE)
        else if (mediaType == "tv" && (episodeRunTime?.firstOrNull() ?: 0) in 1..25) badges.add(MovieBadge.SNACK)
    }

    val genresStr = genreNamesString ?: ""
    val hasGenre = { name: String -> 
        genresStr.contains(name, ignoreCase = true) || 
        genres?.any { it.name?.equals(name, ignoreCase = true) == true } == true
    }

    var genreBadgeAdded = false
    if (hasGenre("Horror")) {
        badges.add(MovieBadge.HORROR)
        genreBadgeAdded = true
    }
    if (hasGenre("Thriller")) {
        badges.add(MovieBadge.THRILLER)
        genreBadgeAdded = true
    }
    
    if (!genreBadgeAdded) {
        if (hasGenre("Animation") || hasGenre("Anime") || hasGenre("Animazione")) badges.add(MovieBadge.ANIMAZIONE)
        else if (hasGenre("Science Fiction") || hasGenre("Sci-Fi") || hasGenre("Fantascienza")) badges.add(MovieBadge.SCI_FI)
        else if ((hasGenre("Comedy") || hasGenre("Commedia")) && voteAvg >= 7.0) badges.add(MovieBadge.COMEDY)
        else if (hasGenre("Documentary") || hasGenre("Documentario")) badges.add(MovieBadge.DOCU)
        else if (hasGenre("Family") || hasGenre("Famiglia")) badges.add(MovieBadge.FAMILY)
    }

    return badges.take(3)
}
