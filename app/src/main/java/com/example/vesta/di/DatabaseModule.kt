package com.example.vesta.di

import android.content.Context
import androidx.room.Room
import com.example.vesta.data.local.FinvestaDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideFinvestaDatabase(@ApplicationContext context: Context): FinvestaDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FinvestaDatabase::class.java,
            "finvesta_database"
        )
        .fallbackToDestructiveMigration() // Remove this in production
        .build()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
