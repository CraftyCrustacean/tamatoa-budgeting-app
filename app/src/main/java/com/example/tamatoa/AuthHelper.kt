package com.example.tamatoa

object AuthHelper {

    //instead of reworking the login screen just append a fake domain onto entered usernames
    //firebase requires emails so doing this solves that issue
    private const val FAKE_DOMAIN = "@tamatoa.co.za"

    //emails are only allowed certain characters so we just take whatever is invalid and replace
    //it with an underscore
    fun toFakeEmail(rawUsername: String): String {
        val cleaned = rawUsername.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]"), "_")
        return cleaned + FAKE_DOMAIN
    }

    //before sending the unsername to firebase check that everything is in order
    fun validateUsername(rawUsername: String): String? {
        val trimmed = rawUsername.trim()
        //cant be empty
        if (trimmed.isEmpty()) return "Please enter a username"
        //must be more than 3 characters
        if (trimmed.length < 3) return "Username must be at least 3 characters"
        return null
    }
}