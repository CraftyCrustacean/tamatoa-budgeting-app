package com.example.tamatoa.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class FirebaseRepository private constructor() {

    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val users get() = db.getReference("users")
    private val categories get() = db.getReference("categories")
    private val expenses get() = db.getReference("expenses")
    private val budgets get() = db.getReference("budgets")
    private val appOpens get() = db.getReference("appOpens")
    private val badges get() = db.getReference("badges")

    companion object {
        private const val TAG = "FirebaseRepository"

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
    }

    protected suspend fun readOnce(ref: DatabaseReference): DataSnapshot =
        suspendCoroutine { cont ->
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) =
                    cont.resume(snapshot)

                override fun onCancelled(error: DatabaseError) =
                    cont.resumeWithException(error.toException())
            })
        }

    protected suspend fun setValue(ref: DatabaseReference, value: Any?) =
        suspendCoroutine<Unit> { cont ->
            ref.setValue(value)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun createUser(userId: String, username: String) {
        val user = UserEntity(
            id = userId,
            username = username,
            createdAtMillis = System.currentTimeMillis()
        )
        setValue(users.child(userId), user)
        Log.d(TAG, "create user: $username (id=$userId)")
    }

    suspend fun getUser(userId: String): UserEntity? {
        val snapshot = readOnce(users.child(userId))
        val user = snapshot.getValue(UserEntity::class.java)
        Log.d(TAG, "get user: $userId -> ${user?.username}")
        return user
    }

    suspend fun updateUserCreatedAt(userId: String, newMillis: Long) {
        setValue(users.child(userId).child("createdAtMillis"), newMillis)
        Log.d(TAG, "update user created at: $userId -> $newMillis")
    }

    suspend fun insertCategory(userId: String, name: String, colour: String): String {
        val newRef = categories.child(userId).push()
        val id = newRef.key ?: throw IllegalStateException("Didnt get a key")
        val category = CategoryEntity(
            id = id,
            userId = userId,
            name = name,
            colour = colour
        )
        setValue(newRef, category)
        Log.d(TAG, "insert category: $name (id=$id)")
        return id
    }

    suspend fun getCategoriesForUser(userId: String): List<CategoryEntity> {
        val snapshot = readOnce(categories.child(userId))
        val list = mutableListOf<CategoryEntity>()
        for (child in snapshot.children) {
            child.getValue(CategoryEntity::class.java)?.let { list.add(it) }
        }
        Log.d(TAG, "get categories for user: $userId -> ${list.size}")
        return list
    }

    suspend fun getCategory(userId: String, categoryId: String): CategoryEntity? {
        val snapshot = readOnce(categories.child(userId).child(categoryId))
        return snapshot.getValue(CategoryEntity::class.java)
    }

    suspend fun seedDefaultCategories(userId: String) {
        val defaults = listOf(
            "Food" to "#D14B8F",
            "Transport" to "#4B8FE0",
            "Housing" to "#F2A03D",
            "Utilities" to "#3DBE6F",
            "Entertainment" to "#9B6FE0"
        )
        for ((name, colour) in defaults) {
            insertCategory(userId, name, colour)
        }
        Log.d(TAG, "seed default categories: ${defaults.size} categories for $userId")
    }

    suspend fun insertExpense(
        userId: String,
        categoryId: String,
        categoryName: String,
        categoryColour: String,
        dateMillis: Long,
        description: String,
        amount: Double,
        photoPath: String? = null,
        isRecurring: Boolean = false,
        recurringSourceId: String? = null
    ): String {
        val newRef = expenses.child(userId).push()
        val id = newRef.key ?: throw IllegalStateException("Didnt get a key")
        val expense = ExpenseEntity(
            id = id,
            userId = userId,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColour = categoryColour,
            dateMillis = dateMillis,
            description = description,
            amount = amount,
            photoPath = photoPath,
            isRecurring = isRecurring,
            recurringSourceId = recurringSourceId
        )
        setValue(newRef, expense)
        Log.d(TAG, "insert expense: $description amount=$amount id=$id")
        return id
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        setValue(expenses.child(userId).child(expenseId), null)
        Log.d(TAG, "delete expense: $expenseId")
    }

    suspend fun getAllExpensesForUser(userId: String): List<ExpenseEntity> {
        val snapshot = readOnce(expenses.child(userId))
        val list = mutableListOf<ExpenseEntity>()
        for (child in snapshot.children) {
            child.getValue(ExpenseEntity::class.java)?.let { list.add(it) }
        }
        list.sortByDescending { it.dateMillis }
        Log.d(TAG, "get all expenses for user: $userId -> ${list.size}")
        return list
    }

    suspend fun getBetween(
        userId: String, startMillis: Long, endMillis: Long
    ): List<ExpenseEntity> {
        return getAllExpensesForUser(userId)
            .filter { it.dateMillis in startMillis..endMillis }
    }

    suspend fun getTotalSpend(
        userId: String, startMillis: Long, endMillis: Long
    ): Double {
        val total = getBetween(userId, startMillis, endMillis).sumOf { it.amount }
        Log.d(TAG, "getTotalSpend: $userId [$startMillis..$endMillis] -> $total")
        return total
    }

    suspend fun getCategoryTotals(
        userId: String, startMillis: Long, endMillis: Long
    ): List<CategoryTotal> {
        val allCategories = getCategoriesForUser(userId)
        val rangeExpenses = getBetween(userId, startMillis, endMillis)

        val sums = rangeExpenses.groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val result = allCategories.map { cat ->
            CategoryTotal(
                categoryId = cat.id,
                name = cat.name,
                colour = cat.colour,
                total = sums[cat.id] ?: 0.0
            )
        }.sortedByDescending { it.total }

        Log.d(TAG, "get category totals: ${result.size} categories")
        return result
    }

    suspend fun getDailySpend(
        userId: String, startMillis: Long, endMillis: Long
    ): List<DailySpend> {
        val rangeExpenses = getBetween(userId, startMillis, endMillis)

        val byDay = rangeExpenses.groupBy { DateUtils.dayKey(it.dateMillis) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .map { (dayKey, total) -> DailySpend(dayKey, total) }
            .sortedBy { it.dayKey }

        Log.d(TAG, "get daily spend: ${byDay.size} days")
        return byDay
    }

    suspend fun getDistinctSpendDays(userId: String, startMillis: Long): List<String> {
        val all = getAllExpensesForUser(userId)
        val days = all
            .filter { it.dateMillis >= startMillis }
            .map { DateUtils.dayKey(it.dateMillis) }
            .toSet()
            .sorted()
        Log.d(TAG, "get distinct spend days: ${days.size}")
        return days
    }

    suspend fun getRecurringTemplates(userId: String): List<ExpenseEntity> {
        return getAllExpensesForUser(userId)
            .filter { it.isRecurring && it.recurringSourceId == null }
    }

    suspend fun findGeneratedCopy(
        userId: String, templateId: String, whenMillis: Long
    ): ExpenseEntity? {
        val monthStart = DateUtils.startOfMonth(whenMillis)
        val monthEnd = DateUtils.endOfMonth(whenMillis)
        return getBetween(userId, monthStart, monthEnd)
            .firstOrNull { it.recurringSourceId == templateId }
    }

    suspend fun getEarliestExpenseMillis(userId: String): Long? {
        val all = getAllExpensesForUser(userId)
        if (all.isEmpty()) return null
        return all.minOf { it.dateMillis }
    }

    suspend fun upsertBudget(userId: String, maxSpend: Double, minSave: Double) {
        val budget = BudgetEntity(
            userId = userId,
            maxSpend = maxSpend,
            minSave = minSave
        )
        setValue(budgets.child(userId), budget)
        Log.d(TAG, "upsert budget: max=$maxSpend min=$minSave")
    }

    suspend fun getBudget(userId: String): BudgetEntity? {
        val snapshot = readOnce(budgets.child(userId))
        val budget = snapshot.getValue(BudgetEntity::class.java)
        Log.d(TAG, "get budget: $userId -> max=${budget?.maxSpend}")
        return budget
    }

    suspend fun recordAppOpen(userId: String, dateKey: String) {
        setValue(appOpens.child(userId).child(dateKey), true)
        Log.d(TAG, "record app open: $userId $dateKey")
    }

    suspend fun countAppOpens(userId: String): Int {
        val snapshot = readOnce(appOpens.child(userId))
        val count = snapshot.childrenCount.toInt()
        Log.d(TAG, "count app opens: $userId -> $count")
        return count
    }

    suspend fun awardBadge(userId: String, badgeId: String) {
        val badge = BadgeEarnedEntity(
            userId = userId,
            badgeId = badgeId,
            earnedAtMillis = System.currentTimeMillis()
        )
        setValue(badges.child(userId).child(badgeId), badge)
        Log.d(TAG, "award badge: $userId $badgeId")
    }

    suspend fun getEarnedBadgeIds(userId: String): Set<String> {
        val snapshot = readOnce(badges.child(userId))
        val ids = mutableSetOf<String>()
        for (child in snapshot.children) {
            child.key?.let { ids.add(it) }
        }
        Log.d(TAG, "get earned badge Ids: $userId -> ${ids.size}")
        return ids
    }

    suspend fun clearUserData(userId: String) {
        setValue(categories.child(userId), null)
        setValue(expenses.child(userId), null)
        setValue(budgets.child(userId), null)
        setValue(appOpens.child(userId), null)
        setValue(badges.child(userId), null)
        Log.d(TAG, "clear user data: $userId")
    }
}