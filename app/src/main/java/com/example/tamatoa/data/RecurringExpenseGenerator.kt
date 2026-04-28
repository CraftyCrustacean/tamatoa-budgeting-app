package com.example.tamatoa.data

import android.util.Log

//Create a copy of any recurring payment for this month if it doesnt already exist
//Called everytime the user opens the dashboard

object RecurringExpenseGenerator {

    private const val TAG = "RecurringGenerator"

    suspend fun generateForCurrentMonth(db: AppDatabase, userId: Long) {
        val now = System.currentTimeMillis()
        val monthStart = DateUtils.startOfMonth(now)
        val monthEnd = DateUtils.endOfMonth(now)

        val expenseDao = db.expenseDao()
        val templates = expenseDao.getRecurringTemplates(userId)
        Log.d(TAG, "generateForCurrentMonth: user=$userId templates=${templates.size}")

        var created = 0
        //dont generate a duplicate, check if the template already exists
        for (template in templates) {
            if (template.dateMillis in monthStart..monthEnd) {
                Log.d(TAG, "  template ${template.id} was created this month, skipping")
                continue
            }
            val existing = expenseDao.findGeneratedCopy(template.id, monthStart, monthEnd)
            if (existing != null) {
                Log.d(TAG, "  template ${template.id} already has copy ${existing.id}")
                continue
            }

            val copy = template.copy(
                id = 0,
                dateMillis = DateUtils.sameDayThisMonth(template.dateMillis),
                isRecurring = false, //the copy musnt duplicate itself
                recurringSourceId = template.id
            )
            val newId = expenseDao.insert(copy)
            Log.d(TAG, "  generated copy id=$newId from template ${template.id}")
            created++
        }

        Log.d(TAG, "generateForCurrentMonth: created $created new entries")
    }
}