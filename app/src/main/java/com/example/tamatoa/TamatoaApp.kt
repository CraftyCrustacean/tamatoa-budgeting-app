package com.example.tamatoa

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class TamatoaApp : Application() {

    companion object {
        private const val TAG = "TamatoaApp"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            //enable offline persistence before anything else happens, anything cached is used
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            Log.d(TAG, "Firebase offline persistence on")
        } catch (e: Exception) {
            Log.w(TAG, "Persistence already on, ignoring: ${e.message}")
        }
    }
}