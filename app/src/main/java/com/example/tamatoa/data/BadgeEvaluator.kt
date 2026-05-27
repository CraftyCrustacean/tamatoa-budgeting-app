package com.example.tamatoa.data

import android.util.Log
import java.util.Calendar

//logic to figure out badge project for a logged in user
object BadgeEvaluator {

    private const val TAG = "BadgeEvaluator"

    suspend fun evaluate(db: AppDatabase, userId: Long): List<BadgeState> {
        //what badges have already been earned
        val earnedIds = db.badgeDao().getEarnedBadgeIds(userId).toSet()
        Log.d(TAG, "user=$userId already earned=${earnedIds.size}")

        val states = ALL_BADGES.map { def ->
            val current = computeCurrent(db, userId, def.id)
            val alreadyEarned = def.id.value in earnedIds
            val nowEarned = current >= def.goal

            //give earned badge to the user
            if (!alreadyEarned && nowEarned) {
                Log.d(TAG, "awarding ${def.id.value} (count=$current)")
                db.badgeDao().award(
                    BadgeEarnedEntity(
                        userId = userId,
                        badgeId = def.id.value,
                        earnedAtTime = System.currentTimeMillis()
                    )
                )
            }

            val earned = alreadyEarned || nowEarned
            //for earned badges show full progress
            val displayedCurrent = if (earned) def.goal else current
            BadgeState(def, displayedCurrent, earned)
        }

        return states
    }

    //connects a badge to the correct function
    private suspend fun computeCurrent(
        db: AppDatabase, userId: Long, id: BadgeId
    ): Int = when (id) {
        BadgeId.NO_EXPENSE -> countNoExpenseDays(db, userId)
        BadgeId.POWER_USER -> db.appOpenDao().countForUser(userId)
        BadgeId.BUDGET_KING, BadgeId.GOBLIN ->
            countMonthsWithinBudget(db, userId)
        BadgeId.GETTING_THERE, BadgeId.SMAUG ->
            countMonthsMeetingSavings(db, userId)
    }


    private suspend fun countNoExpenseDays(db: AppDatabase, userId: Long): Int {
        val user = db.userDao().getById(userId) ?: return 0
        val startMillis = user.createdAtMillis

        val now = System.currentTimeMillis()
        val daysCompleted = daysBetween(startMillis, now)

        if (daysCompleted <= 0) return 0

        val daysWithSpend = db.expenseDao()
            .getDistinctSpendDays(userId, startMillis)
            .size
        val noSpendDays = (daysCompleted - daysWithSpend).coerceAtLeast(0)
        Log.d(TAG, "no-expense: completed=$daysCompleted withSpend=$daysWithSpend noSpend=$noSpendDays")
        return noSpendDays
    }

    //count past months where amount spent was within set budget
    private suspend fun countMonthsWithinBudget(db: AppDatabase, userId: Long): Int {
        val budget = db.budgetDao().get(userId) ?: return 0
        return countQualifyingPastMonths(db, userId) { spent ->
            spent <= budget.maxSpend
        }
    }

    //count past months where remaining budget is greater than set savings goal
    private suspend fun countMonthsMeetingSavings(db: AppDatabase, userId: Long): Int {
        val budget = db.budgetDao().get(userId) ?: return 0
        return countQualifyingPastMonths(db, userId) { spent ->
            (budget.maxSpend - spent) >= budget.minSave && budget.minSave > 0.0
        }
    }

    //function to count months in which the user has met whatever criteria there is for the badge
    private suspend fun countQualifyingPastMonths(
        db: AppDatabase,
        userId: Long,
        qualifies: (Double) -> Boolean
    ): Int {
        val earliest = getEarliestExpenseMillis(db, userId) ?: return 0
        val now = System.currentTimeMillis()
        val firstMonthStart = DateUtils.startOfMonth(earliest)
        val currentMonthStart = DateUtils.startOfMonth(now)

        //fallback in case no month has been completed yet
        if (firstMonthStart >= currentMonthStart) return 0

        var count = 0
        val cursor = Calendar.getInstance().apply { timeInMillis = firstMonthStart }

        while (cursor.timeInMillis < currentMonthStart) {
            val monthStart = DateUtils.startOfMonth(cursor.timeInMillis)
            val monthEnd = DateUtils.endOfMonth(cursor.timeInMillis)
            val spent = db.expenseDao().getTotalSpend(userId, monthStart, monthEnd)
            if (qualifies(spent)) count++

            cursor.add(Calendar.MONTH, 1)
        }
        Log.d(TAG, "qualifying past months: $count")
        return count
    }

    private suspend fun getEarliestAppOpenMillis(db: AppDatabase, userId: Long): Long? {
        val today = System.currentTimeMillis()
        return today
    }

    //simple and dirty way to get the first expense for a user
    //fetch every expense and return the smallest one
    private suspend fun getEarliestExpenseMillis(db: AppDatabase, userId: Long): Long? {
        val all = db.expenseDao().getBetween(userId, 0L, Long.MAX_VALUE)
        return all.minByOrNull { it.dateMillis }?.dateMillis
    }

    //function to get all days between two millisecond times
    private fun daysBetween(startMillis: Long, endMillis: Long): Int {
        val ms = endMillis - startMillis
        if (ms < 0) return 0
        return (ms / (24L * 60 * 60 * 1000)).toInt()
    }
}