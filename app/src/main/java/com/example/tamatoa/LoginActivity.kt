package com.example.tamatoa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

//login screen where user can login or click a button to register instead
//information from: https://www.youtube.com/watch?v=8lQSa6bw1aI
// and https://www.youtube.com/watch?v=9Ta4OwNtw-4
// https://github.com/sdranju/RoomDbAndroid

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // go to main screen if someone is already logged in
        if (Session.isLoggedIn(this)) {
            Log.d(TAG, "on create: already logged in, skipping login")
            goToMain()
            return
        }

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // try to log user in, with some error checks
    private fun attemptLogin() {
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        //if no information is entered ask them to put it in
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter a username and password",
                Toast.LENGTH_SHORT).show()
            return
        }

        // call on the db to check of username and password match a registered user
        // in a try catch block to handle unknown errors
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@LoginActivity)
                val user = db.userDao().login(username, password)

                //if no match is found tell the user something is wrong
                if (user == null) {
                    Log.d(TAG, "attempted login: bad credentials for '$username'")
                    Toast.makeText(
                        this@LoginActivity,
                        "Wrong username or password",
                        Toast.LENGTH_SHORT
                    ).show()
                    //if user is found successful login
                } else {
                    Log.d(TAG, "attempted login: success for '$username' id=${user.id}")
                    Session.setUserId(this@LoginActivity, user.id)
                    goToMain()
                }
                //if something weird broke, prompt user to try again
            } catch (e: Exception) {
                Log.e(TAG, "attempted login: error", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}