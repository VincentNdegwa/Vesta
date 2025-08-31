package com.example.vesta.di

import android.content.Context
import androidx.room.Room
import com.example.vesta.data.local.FinvestaDatabase
import com.example.vesta.data.local.dao.*
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
    fun provideUserSettingsDao(database: FinvestaDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: FinvestaDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: FinvestaDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideBudgetDao(database: FinvestaDatabase): BudgetDao {
        return database.budgetDao()
    }
    
    @Provides
    @Singleton
    fun provideBillReminderDao(database: FinvestaDatabase): BillReminderDao {
        return database.billReminderDao()
    }
    
    @Provides
    @Singleton
    fun provideUserProfileDao(database: FinvestaDatabase): UserProfileDao {
        return database.userProfileDao()
    }
    
    @Provides
    @Singleton
    fun provideAccountDao(database: FinvestaDatabase): AccountDao {
        return database.accountDao()
    }
    
    @Provides
    @Singleton
    fun provideCategoryDao(database: FinvestaDatabase): CategoryDao {
        return database.categoryDao()
    }
    
    @Provides
    @Singleton
    fun provideGoalDao(database: FinvestaDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideSavingsGoalDao(database: FinvestaDatabase): SavingsGoalDao {
        return database.savingsGoalDao()
    }

    @Provides
    @Singleton
    fun provideSavingsContributionDao(database: FinvestaDatabase): SavingsContributionDao {
        return database.savingsContributionDao()
    }

    @Provides
    @Singleton
    fun provideSavingsRuleDao(database: FinvestaDatabase): SavingsRuleDao {
        return database.savingsRuleDao()
    }

    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
