# Proguard Rules for FlickTrove (Cinetrack)

# 1. Keep Attributes for Reflection, Generics, and Serialization
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault, *Annotation*

# 2. Scudo totale su tutti i modelli dati, API e Database locale
# Il doppio asterisco (**) copre in automatico anche /models, /api e /local
-keep class com.cinetrack.data.** { *; }

# 3. Room Database
-keep class * extends androidx.room.RoomDatabase { *; }

# 4. Hilt / Dagger DI
-keep class **_HiltModules* { *; }
-keep class **_HiltComponents* { *; }
