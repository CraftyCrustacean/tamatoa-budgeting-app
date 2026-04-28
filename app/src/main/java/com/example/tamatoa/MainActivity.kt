package com.example.tamatoa

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Sanity check - we should not be able to reach this screen unless logged in.
        if (!Session.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Temporary - we'll replace this whole layout in the next step.
        findViewById<TextView>(R.id.tempText)?.text =
            "Logged in as user ID ${Session.getUserId(this)}"
    }
}