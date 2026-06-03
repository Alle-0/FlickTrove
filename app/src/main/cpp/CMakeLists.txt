# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.22.1)

project("cinetrack")

add_library(
        cinetrack
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        cinetrack
        ${log-lib})

target_compile_definitions(cinetrack PRIVATE
        _TMDB_API_KEY="${_TMDB_API_KEY}"
        _OMDB_API_KEY="${_OMDB_API_KEY}"
        _TRAKT_API_KEY="${_TRAKT_API_KEY}"
)
