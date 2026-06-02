package com.example.tamatoa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

//login screen where user can login or click a button to register instead
//information from: https://www.youtube.com/watch?v=8lQSa6bw1aI
// and https://www.youtube.com/watch?v=9Ta4OwNtw-4
// https://github.com/sdranju/RoomDbAndroid

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If already signed in skip the log in screen
        if (auth.currentUser != null) {
            Log.d(TAG, "already signed in as ${auth.currentUser?.uid}")
            goToDashboard()
            return
        }

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val rawUsername = binding.usernameInput.text?.toString().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        AuthHelper.validateUsername(rawUsername)?.let {
            toast(it)
            return
        }
        if (password.isEmpty()) {
            toast("Please enter your password")
            return
        }

        val email = AuthHelper.toFakeEmail(rawUsername)
        Log.d(TAG, "attempt login: signing in as $email")

        binding.loginButton.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                Log.d(TAG, "signed in as ${result.user?.uid}")
                goToDashboard()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "sign-in failed", e)
                binding.loginButton.isEnabled = true
                toast("Login failed: ${(e)}")
            }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}