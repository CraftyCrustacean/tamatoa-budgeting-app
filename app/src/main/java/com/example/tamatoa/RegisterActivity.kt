package com.example.tamatoa

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.data.UserEntity
import com.example.tamatoa.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

//Allows users to create an account
//information found at largely the same places as the login activity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    companion object {
        private const val TAG = "RegisterActivity"
        private const val MIN_PASSWORD_LEN = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.backButton.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmInput.text?.toString().orEmpty()

        // Error checks; username must exist and password must be 8 characters and match
        when {
            username.isEmpty() -> {
                toast("Please choose a username"); return
            }
            password.length < MIN_PASSWORD_LEN -> {
                toast("Password must be at least $MIN_PASSWORD_LEN characters"); return
            }
            password != confirm -> {
                toast("Passwords don't match"); return
            }
        }

        // try catch block to prevent wierdo errors and some common errors.
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@RegisterActivity)
                val newId = db.userDao().insert(UserEntity(username = username, password = password))

                //username must be unique
                if (newId == -1L) {
                    Log.d(TAG, "attempted register: '$username' already taken")
                    toast("That username is already taken")
                } else {
                    //if its unique the success
                    Log.d(TAG, "attempted register: created user '$username' id=$newId")
                    AppDatabase.seedDefaultCategories(db, newId)

                    // stash the login information to auto login next time
                    Session.setUserId(this@RegisterActivity, newId)
                    toast("Welcome, $username!")
                    startActivity(android.content.Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "attempted register: error", e)
                toast("Something went wrong, please try again")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}