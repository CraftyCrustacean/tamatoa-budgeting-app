package com.example.tamatoa.data

enum class BadgeId(val value: String) {
    NO_EXPENSE("no_expense"),
    BUDGET_KING("budget_king"),
    GOBLIN("goblin"),
    GETTING_THERE("getting_there"),
    SMAUG("smaug"),
    POWER_USER("power_user");

    companion object {
        fun fromValue(v: String): BadgeId? = values().firstOrNull { it.value == v }
    }
}

data class BadgeDefinition(
    val id: BadgeId,
    val title: String,
    val description: String,
    val goal: Int,
    val unit: String,
    val iconColourHex: String
)

data class BadgeState(
    val definition: BadgeDefinition,
    val current: Int,
    //boolean to track if a user has earned the badge, it stays consistent even if they go back
    //and edit an expense
    val earned: Boolean
) {
    //convert user progress into a percentage for display
    val progressPercent: Int
        get() = if (definition.goal == 0) 100
        else ((current.toDouble() / definition.goal) * 100).toInt().coerceIn(0, 100)
}

//list of badges and their info in their display order
val ALL_BADGES: List<BadgeDefinition> = listOf(
    BadgeDefinition(
        id = BadgeId.BUDGET_KING,
        title = "Budget King",
        description = "Stay within budget for 6 months.",
        goal = 6,
        unit = "months",
        iconColourHex = "#D14B8F"
    ),

    BadgeDefinition(
        id = BadgeId.GOBLIN,
        title = "Goblin",
        description = "Stay within budget for a year.",
        goal = 12,
        unit = "months",
        iconColourHex = "#E0C04B"
    ),

    BadgeDefinition(
        id = BadgeId.GETTING_THERE,
        title = "Getting There",
        description = "Meet your savings goal for a month.",
        goal = 1,
        unit = "months",
        iconColourHex = "#D14B8F"
    ),

    BadgeDefinition(
        id = BadgeId.SMAUG,
        title = "Smaug",
        description = "Meet your savings goal for a year.",
        goal = 12,
        unit = "months",
        iconColourHex = "#3DBE6F"
    ),

    BadgeDefinition(
        id = BadgeId.NO_EXPENSE,
        title = "No Expense",
        description = "Go 7 days without logging an expense.",
        goal = 7,
        unit = "days",
        iconColourHex = "#4B8FE0"
    ),

    BadgeDefinition(
        id = BadgeId.POWER_USER,
        title = "Power User",
        description = "Open the app daily for a month.",
        goal = 30,
        unit = "days",
        iconColourHex = "#F2A03D"
    )
)