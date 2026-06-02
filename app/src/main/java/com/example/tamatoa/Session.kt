package com.example.tamatoa

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

//A helper to tell us which user is logged in, using the built in SharedPreferences
//to store a couple values between app launches
//information found at: https://developer.android.com/training/data-storage/shared-preferences
//and https://www.youtube.com/watch?v=55lMVVJdKRI

object Session {

    //returns the currently logged in user
    fun getUserId(context: Context): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    fun isSignedIn(context: Context): Boolean = getUserId(context) != null

    //signs out the current user
    fun clear(context: Context) {
        FirebaseAuth.getInstance().signOut()
    }
}