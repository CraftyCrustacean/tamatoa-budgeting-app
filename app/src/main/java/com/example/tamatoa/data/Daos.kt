package com.example.tamatoa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//Data access object defining the methods that are needed to manipulate the database
//Information found at: https://developer.android.com/training/data-storage/room/accessing-data
//https://www.youtube.com/watch?v=bOd3wO0uFr8
//https://www.youtube.com/watch?v=sWOmlDvz_3U

// User methods
@Dao
interface UserDao {

    //insert new user into the table
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: UserEntity): Long

    //fetch the user that matches an entered username and password
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): UserEntity?

    //fetch a specific user
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: Long): UserEntity?

    //change the date a users account was created, used by the seeder
    @Query("UPDATE users SET createdAtMillis = :newMillis WHERE id = :userId")
    suspend fun updateCreatedAt(userId: Long, newMillis: Long)
}

//Category Methods

@Dao
interface CategoryDao {

    //insert new category into the table
    @Insert
    suspend fun insert(category: CategoryEntity): Long
    //fetch the categories tied to a user id (logged in user)
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    suspend fun getForUser(userId: Long): List<CategoryEntity>
}

// Expense methods

// values for the total expense per category search
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val colour: String,
    val total: Double
)

// expenses joined with categories for the transaction list
data class ExpenseWithCategory(
    val id: Long,
    val categoryId: Long,
    val dateMillis: Long,
    val description: String,
    val amount: Double,
    val photoPath: String?,
    val isRecurring: Boolean,
    val recurringSourceId: Long?,
    val categoryName: String,
    val categoryColour: String
)

data class DailySpend(
    val dayKey: String,
    val total: Double
)

@Dao
interface ExpenseDao {

    //insert new expense
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    //delete entry by its id
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    //get expenses and thier categories between two dates
    @Query("""
        SELECT e.id, e.categoryId, e.dateMillis, e.description, e.amount, e.photoPath,
               e.isRecurring, e.recurringSourceId,
               c.name AS categoryName, c.colour AS categoryColour
        FROM expenses e
        INNER JOIN categories c ON c.id = e.categoryId
        WHERE e.userId = :userId AND e.dateMillis BETWEEN :startMillis AND :endMillis
        ORDER BY e.dateMillis DESC
    """)
    suspend fun getBetween(
        userId: Long,
        startMillis: Long,
        endMillis: Long
    ): List<ExpenseWithCategory>

    //get total expense per category between two dates
    @Query("""
        SELECT c.id AS categoryId, c.name, c.colour,
               COALESCE(SUM(e.amount), 0) AS total
        FROM categories c
        LEFT JOIN expenses e
            ON e.categoryId = c.id
            AND e.dateMillis BETWEEN :startMillis AND :endMillis
        WHERE c.userId = :userId
        GROUP BY c.id
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(
        userId: Long,
        startMillis: Long,
        endMillis: Long
    ): List<CategoryTotal>

    //get the total amount spent between two dates
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE userId = :userId AND dateMillis BETWEEN :startMillis AND :endMillis
    """)
    suspend fun getTotalSpend(
        userId: Long,
        startMillis: Long,
        endMillis: Long
    ): Double

    //get the expenses originally entered by the user where they selected recurring
    @Query("SELECT * FROM expenses WHERE userId = :userId AND isRecurring = 1")
    suspend fun getRecurringTemplates(userId: Long): List<ExpenseEntity>

    //get the recurring payments for a time period, used to check if its recurred already
    //this month
    @Query("""
        SELECT * FROM expenses
        WHERE recurringSourceId = :templateId
          AND dateMillis BETWEEN :startMillis AND :endMillis
        LIMIT 1 """)
    suspend fun findGeneratedCopy(
        templateId: Long,
        startMillis: Long,
        endMillis: Long
    ): ExpenseEntity?

    //find days where the user logged an expense, used by the no expense badge to see when they
    //didnt log any
    @Query("""
    SELECT DISTINCT strftime('%Y-%m-%d', dateMillis / 1000, 'unixepoch', 'localtime') AS dayKey
    FROM expenses
    WHERE userId = :userId AND dateMillis >= :startMillis
    ORDER BY dayKey ASC """)
    suspend fun getDistinctSpendDays(userId: Long, startMillis: Long): List<String>

    @Query("""
    SELECT e.id, e.categoryId, e.dateMillis, e.description, e.amount, e.photoPath,
           e.isRecurring, e.recurringSourceId,
           c.name AS categoryName, c.colour AS categoryColour
    FROM expenses e
    INNER JOIN categories c ON c.id = e.categoryId
    WHERE e.userId = :userId
    ORDER BY e.dateMillis ASC """)
    suspend fun getAllForUser(userId: Long): List<ExpenseWithCategory>

    //used to get monthly spending for the line graph
    @Query("""
    SELECT strftime('%Y-%m-%d', dateMillis / 1000, 'unixepoch', 'localtime') AS dayKey,
           COALESCE(SUM(amount), 0) AS total
    FROM expenses
    WHERE userId = :userId AND dateMillis BETWEEN :startMillis AND :endMillis
    GROUP BY dayKey
    ORDER BY dayKey ASC""")
    suspend fun getDailySpend(userId: Long, startMillis: Long, endMillis: Long): List<DailySpend>
}

//Budget methods
@Dao
interface BudgetDao {

    //add a budget for a user, if one exists replace it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity): Long

    //get budget for logged in user
    @Query("SELECT * FROM budgets WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: Long): BudgetEntity?
}

@Dao
interface AppOpenDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun record(open: AppOpenEntity): Long

    //count how many times a user has opened the app
    @Query("SELECT COUNT(*) FROM app_opens WHERE userId = :userId")
    suspend fun countForUser(userId: Long): Int
}

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun award(badge: BadgeEarnedEntity): Long

    //get what badges have been earned by the logged in user
    @Query("SELECT badgeId FROM badges_earned WHERE userId = :userId")
    suspend fun getEarnedBadgeIds(userId: Long): List<String>

    //get how many badges have been earned by the user
    @Query("SELECT COUNT(*) > 0 FROM badges_earned WHERE userId = :userId AND badgeId = :badgeId")
    suspend fun hasBadge(userId: Long, badgeId: String): Boolean
}