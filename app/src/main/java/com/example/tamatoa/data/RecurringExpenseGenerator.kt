package com.example.tamatoa.data

import android.util.Log

//Create a copy of any recurring payment for this month if it doesnt already exist
//Called everytime the user opens the dashboard
//information found from: https://www.youtube.com/watch?v=nuLyv70ViLc
//https://www.youtube.com/watch?v=opT7iABvisY

object RecurringExpenseGenerator {

    private const val TAG = "RecurringGenerator"

    suspend fun generateForCurrentMonth(repo: FirebaseRepository, userId: String) {
        val templates = repo.getRecurringTemplates(userId)
        if (templates.isEmpty()) return

        val now = System.currentTimeMillis()
        val thisMonthStart = DateUtils.startOfMonth(now)

        for (template in templates) {
            if (template.dateMillis >= thisMonthStart) continue

            val existingCopy = repo.findGeneratedCopy(userId, template.id, now)
            if (existingCopy != null) {
                Log.d(TAG, "skip ${template.description}: already generated")
                continue
            }

            val copyDate = DateUtils.sameDayThisMonth(template.dateMillis)

            repo.insertExpense(
                userId = userId,
                categoryId = template.categoryId,
                categoryName = template.categoryName,
                categoryColour = template.categoryColour,
                dateMillis = copyDate,
                description = template.description,
                amount = template.amount,
                photoPath = null,
                isRecurring = false,
                recurringSourceId = template.id
            )
            Log.d(TAG, "generated copy of ${template.description}")
        }
    }
}