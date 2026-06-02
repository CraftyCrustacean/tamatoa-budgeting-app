package com.example.tamatoa.data

//This file outlines the tables that the database requires
//Information on how each table should be made was found at:
//https://developer.android.com/training/data-storage/room/defining-data
//https://www.youtube.com/watch?v=bOd3wO0uFr8
//https://www.youtube.com/watch?v=sWOmlDvz_3U

data class UserEntity(
    var id: String = "",
    var username: String = "",
    var createdAtMillis: Long = 0L
)

data class CategoryEntity(
    var id: String = "",
    var userId: String = "",
    var name: String = "",
    var colour: String = "#888888"
)

data class ExpenseEntity(
    var id: String = "",
    var userId: String = "",
    var categoryId: String = "",
    var categoryName: String = "",
    var categoryColour: String = "",
    var dateMillis: Long = 0L,
    var description: String = "",
    var amount: Double = 0.0,
    var photoPath: String? = null,
    var isRecurring: Boolean = false,
    var recurringSourceId: String? = null
)

data class BudgetEntity(
    var userId: String = "",
    var maxSpend: Double = 0.0,
    var minSave: Double = 0.0
)

data class BadgeEarnedEntity(
    var userId: String = "",
    var badgeId: String = "",
    var earnedAtMillis: Long = 0L
)

typealias ExpenseWithCategory = ExpenseEntity

data class CategoryTotal(
    val categoryId: String,
    val name: String,
    val colour: String,
    val total: Double
)

data class DailySpend(
    val dayKey: String,
    val total: Double
)