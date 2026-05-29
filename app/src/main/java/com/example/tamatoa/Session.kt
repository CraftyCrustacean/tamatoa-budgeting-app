package com.example.tamatoa

import android.content.Context

//A helper to tell us which user is logged in, using the built in SharedPreferences
//to store a couple values between app launches
//information found at: https://developer.android.com/training/data-storage/shared-preferences
//and https://www.youtube.com/watch?v=55lMVVJdKRI

object Session {

    private const val PREFS_NAME = "tamatoa_session"
    private const val KEY_USER_ID = "user_id"
    private const val NOT_LOGGED_IN = -1L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUserId(context: Context): Long =
        prefs(context).getLong(KEY_USER_ID, NOT_LOGGED_IN)

    fun setUserId(context: Context, userId: Long) {
        prefs(context).edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        getUserId(context) != NOT_LOGGED_IN
}
