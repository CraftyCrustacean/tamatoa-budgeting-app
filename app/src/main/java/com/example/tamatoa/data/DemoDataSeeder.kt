package com.example.tamatoa.data

import android.util.Log
import java.util.Calendar

//This populates the database with data so the achievements can be demonstrated, some of them
//take months to earn otherwise.

//information found at: https://developer.android.com/training/data-storage/room
//and https://stackoverflow.com/questions/67003049/how-to-insert-test-data-in-room-database
object DemoDataSeeder {

    private const val TAG = "DemoDataSeeder"

    //a random budget for the data
    private const val DEMO_MAX_SPEND = 20000.0
    //savings goal/min spend for the data
    private const val DEMO_MIN_SAVE = 5000.0

    private val MONTHLY_TOTALS = listOf(
        //four months of the user being over budget
        22000.0, 22000.0, 22000.0, 22000.0,
        //one month of the user being within budget but missing their savings goal
        17000.0,
        //seven months of the user meeting the savings and budget goal
        12000.0, 12000.0, 12000.0, 12000.0, 12000.0, 12000.0, 12000.0
    )

    //function to seed the data
    suspend fun seed(db: AppDatabase, userId: Long) {
        Log.d(TAG, "seed: starting for user=$userId")

        ensureBudget(db, userId)
        val categories = ensureSeedCategories(db, userId)
        makeOldAccountCreation(db, userId)
        seedOldMonths(db, userId, categories)
        maekOldAppOpens(db, userId)

        Log.d(TAG, "seed: done")
    }


    //function to set the users budget and savings goal
    private suspend fun ensureBudget(db: AppDatabase, userId: Long) {
        if (db.budgetDao().get(userId) == null) {
            db.budgetDao().upsert(
                BudgetEntity(userId = userId, maxSpend = DEMO_MAX_SPEND, minSave = DEMO_MIN_SAVE)
            )
        }
    }

    //fetch the first 4 categories for the fake expenses
    private suspend fun ensureSeedCategories(
        db: AppDatabase, userId: Long
    ): List<CategoryEntity> = db.categoryDao().getForUser(userId).take(4)

    //make the users account older
    private suspend fun makeOldAccountCreation(db: AppDatabase, userId: Long) {
        val user = db.userDao().getById(userId) ?: return
        val sixtyDaysAgo = System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000
        //if the users account is younger than 60 days, rewrite it to be 60 days older
        if (user.createdAtMillis > sixtyDaysAgo) {
            db.userDao().updateCreatedAt(userId, sixtyDaysAgo)
            Log.d(TAG, "make old account creation: moved to 60 days ago")
        }
    }

    // -- Historical expenses --

    //create old expenses
    private suspend fun seedOldMonths(
        db: AppDatabase,
        userId: Long,
        categories: List<CategoryEntity>
    ) {
        //start from the beginning of the monthly totals we defined earlier
        val cursor = Calendar.getInstance()
        cursor.add(Calendar.MONTH, -MONTHLY_TOTALS.size)

        MONTHLY_TOTALS.forEach { monthTotal ->
            cursor.add(Calendar.MONTH, 1)
            val monthStart = DateUtils.startOfMonth(cursor.timeInMillis)
            val monthEnd = DateUtils.endOfMonth(cursor.timeInMillis)

            //if a month already has data in it skip it and carry on
            val existing = db.expenseDao().getBetween(userId, monthStart, monthEnd)
            if (existing.isNotEmpty()) {
                Log.d(TAG, "skip month ${DateUtils.formatShort(monthStart)} has data")
                return@forEach
            }
            //insert the four fake expenses
            insertExpensesForMonth(db, userId, cursor.timeInMillis, monthTotal, categories)
        }
    }


    //splits the spending for a month into four expenses
    private suspend fun insertExpensesForMonth(
        db: AppDatabase,
        userId: Long,
        anyMillisInMonth: Long,
        monthTotal: Double,
        categories: List<CategoryEntity>
    ) {
        //split the total spent into four expenses
        val splits = listOf(0.25, 0.25, 0.25, 0.25).map { it * monthTotal }
        val descriptions = listOf("Rent", "rent 2: electric boogaloo",
            "Rent again??", "This landord is a scammer, 4 rents in one month?!")

        //get whatever month the current system says it is and make it midday
        val cal = Calendar.getInstance().apply {
            timeInMillis = anyMillisInMonth
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        //for each expense add the previous defined information
        splits.forEachIndexed { index, amount ->
            //take the date defined above and push it forward by some days, hits math sort of
            //evenly spaces out the expenses throughout the month
            cal.set(Calendar.DAY_OF_MONTH, (index * 5) + 3)
            db.expenseDao().insert(
                ExpenseEntity(
                    userId = userId,
                    categoryId = categories[index].id,
                    dateMillis = cal.timeInMillis,
                    description = descriptions[index],
                    amount = amount,
                    photoPath = null,
                    isRecurring = false,
                    recurringSourceId = null
                )
            )
        }
    }

    //seed 30 app opens for the power user achievement
    private suspend fun maekOldAppOpens(db: AppDatabase, userId: Long) {
        val cal = Calendar.getInstance()
        //repeat this 30 times for the 30 required days
        repeat(30) {
            //fetch the current day
            val key = DateUtils.dayKey(cal.timeInMillis)
            //record that the app was opened for this user on this day
            db.appOpenDao().record(AppOpenEntity(userId = userId, dateKey = key))
            //go one day back in the past
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        Log.d(TAG, "maek old app opens: 30 days seeded")
    }
}