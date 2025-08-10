package com.example.vesta.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.vesta.data.local.converters.Converters
import com.example.vesta.data.local.dao.*
import com.example.vesta.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BillReminderEntity::class,
        UserProfileEntity::class,
        UserSettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        GoalEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinvestaDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun billReminderDao(): BillReminderDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    
    companion object {
        @Volatile
        private var INSTANCE: FinvestaDatabase? = null

        fun getDatabase(context: Context): FinvestaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinvestaDatabase::class.java,
                    "finvesta_database"
                )
                    .fallbackToDestructiveMigration() // Remove this in production
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): FinvestaDatabase = getDatabase(context)
    }
}
