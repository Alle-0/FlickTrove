#include <jni.h>
#include <string>

#define STR_EXPAND(tok) #tok
#define STR(tok) STR_EXPAND(tok)

extern "C" JNIEXPORT jstring JNICALL
Java_com_cinetrack_utils_Keys_getTmdbKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = STR(_TMDB_API_KEY);
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_cinetrack_utils_Keys_getOmdbKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = STR(_OMDB_API_KEY);
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_cinetrack_utils_Keys_getTraktKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = STR(_TRAKT_API_KEY);
    return env->NewStringUTF(key.c_str());
}
