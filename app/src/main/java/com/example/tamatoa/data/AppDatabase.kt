package com.example.tamatoa.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//Class to define the database config and serves as the access point for the app
//Information found at: https://developer.android.com/training/data-storage/room#database
@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "tamatoa.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            Log.d(TAG, "build: creating Room database")
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }


        //Some default common categories to populate the dropdown menu for a new user
        suspend fun seedDefaultCategories(db: AppDatabase, userId: Long) {
            Log.d(TAG, "seedDefaultCategories: user=$userId")
            val defaults = listOf(
                "Food" to "#D14B8F",
                "Transport" to "#4B8FE0",
                "Housing" to "#F2A03D",
                "Utilities" to "#3DBE6F",
                "Entertainment" to "#9B59B6"
            )
            val dao = db.categoryDao()
            for ((name, colour) in defaults) {
                dao.insert(CategoryEntity(userId = userId, name = name, colour = colour))
            }
        }
    }
}