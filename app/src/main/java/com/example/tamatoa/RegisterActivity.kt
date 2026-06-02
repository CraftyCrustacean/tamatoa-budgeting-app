package com.example.tamatoa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tamatoa.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

//Allows users to create an account
//information found at largely the same places as the login activity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.backButton.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val rawUsername = binding.usernameInput.text?.toString().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmInput.text?.toString().orEmpty()

        AuthHelper.validateUsername(rawUsername)?.let {
            toast(it)
            return
        }
        if (password.length < 6) {
            //firebase requires a 6 character minimum length
            toast("Password must be at least 6 characters")
            return
        }
        if (password != confirm) {
            toast("Passwords dont match")
            return
        }

        val email = AuthHelper.toFakeEmail(rawUsername)
        Log.d(TAG, "attempt register: creating $email")

        binding.registerButton.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                Log.d(TAG, "registered ${result.user?.uid}")
                goToDashboard()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "register failed", e)
                binding.registerButton.isEnabled = true
                toast("Could not register: ${(e)}")
            }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }


    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}