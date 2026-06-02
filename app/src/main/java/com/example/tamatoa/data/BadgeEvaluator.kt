package com.example.tamatoa.data

import android.util.Log
import java.util.Calendar

//logic to figure out badge project for a logged in user
object BadgeEvaluator {

    private const val TAG = "BadgeEvaluator"

    suspend fun evaluate(repo: FirebaseRepository, userId: String): List<BadgeState> {
        //what badges have already been earned
        val earnedIds = repo.getEarnedBadgeIds(userId)
        Log.d(TAG, "user=$userId already earned=${earnedIds.size}")

        val states = ALL_BADGES.map { def ->
            val current = computeCurrent(repo, userId, def.id)
            val alreadyEarned = def.id.value in earnedIds
            val nowEarned = current >= def.goal

            //give earned badge to the user
            if (!alreadyEarned && nowEarned) {
                Log.d(TAG, "awarding ${def.id.value} (count=$current)")
                repo.awardBadge(userId, def.id.value)
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
        repo: FirebaseRepository, userId: String, id: BadgeId
    ): Int = when (id) {
        BadgeId.NO_EXPENSE -> countNoExpenseDays(repo, userId)
        BadgeId.POWER_USER -> repo.countAppOpens(userId)
        BadgeId.BUDGET_KING, BadgeId.GOBLIN ->
            countMonthsWithinBudget(repo, userId)
        BadgeId.GETTING_THERE, BadgeId.SMAUG ->
            countMonthsMeetingSavings(repo, userId)
    }


    private suspend fun countNoExpenseDays(repo: FirebaseRepository, userId: String): Int {
        val user = repo.getUser(userId) ?: return 0
        val startMillis = user.createdAtMillis

        val now = System.currentTimeMillis()
        val daysCompleted = daysBetween(startMillis, now)

        if (daysCompleted <= 0) return 0

        val daysWithSpend = repo.getDistinctSpendDays(userId, startMillis).size
        val noSpendDays = (daysCompleted - daysWithSpend).coerceAtLeast(0)
        Log.d(TAG, "no-expense: completed=$daysCompleted withSpend=$daysWithSpend noSpend=$noSpendDays")
        return noSpendDays
    }

    //count past months where amount spent was within set budget
    private suspend fun countMonthsWithinBudget(repo: FirebaseRepository, userId: String): Int {
        val budget = repo.getBudget(userId) ?: return 0
        return countQualifyingPastMonths(repo, userId) { spent ->
            spent <= budget.maxSpend
        }
    }

    //count past months where remaining budget is greater than set savings goal
    private suspend fun countMonthsMeetingSavings(repo: FirebaseRepository, userId: String): Int {
        val budget = repo.getBudget(userId) ?: return 0
        return countQualifyingPastMonths(repo, userId) { spent ->
            (budget.maxSpend - spent) >= budget.minSave && budget.minSave > 0.0
        }
    }

    //function to count months in which the user has met whatever criteria there is for the badge
    private suspend fun countQualifyingPastMonths(
        repo: FirebaseRepository,
        userId: String,
        qualifies: (Double) -> Boolean
    ): Int {
        val earliest = repo.getEarliestExpenseMillis(userId) ?: return 0
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
            val spent = repo.getTotalSpend(userId, monthStart, monthEnd)
            if (qualifies(spent)) count++

            cursor.add(Calendar.MONTH, 1)
        }
        Log.d(TAG, "qualifying past months: $count")
        return count
    }

    //function to get all days between two millisecond times
    private fun daysBetween(startMillis: Long, endMillis: Long): Int {
        val ms = endMillis - startMillis
        if (ms < 0) return 0
        return (ms / (24L * 60 * 60 * 1000)).toInt()
    }
}