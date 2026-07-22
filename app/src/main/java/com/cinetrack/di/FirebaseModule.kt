package com.cinetrack.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        
        // Use memory cache settings to eliminate background SQLiteRemoteDocumentCache Protobuf decoding OOM.
        // Room (favoriteDao/folderDao) serves as our permanent local bunker.
        val settings = firestoreSettings {
            setLocalCacheSettings(memoryCacheSettings {})
        }
        firestore.firestoreSettings = settings
        
        return firestore
    }
}
