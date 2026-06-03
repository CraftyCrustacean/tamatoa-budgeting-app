package com.example.tamatoa

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.data.DateUtils
import com.example.tamatoa.data.FirebaseRepository
import com.example.tamatoa.databinding.ActivityMainBinding
import com.example.tamatoa.ui.AchievementsFragment
import com.example.tamatoa.ui.DashboardFragment
import com.example.tamatoa.ui.TransactionsFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

//The thing that holds the nave buttons and swaps fragments into the content area
//information found at: https://developer.android.com/guide/navigation/navigation-getting-started
//and https://www.youtube.com/watch?v=MWkplgwdY20
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if no user is logged in and if somehow they get to this activity, redirect to login
        if (!Session.isLoggedIn(this)) {
            Log.d(TAG, "On Create: not logged in, redirecting to login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // When the app is first opened show the dashboard by default
        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
            tintNavForTab(R.id.nav_dashboard)
        }

        // connect the tabs to the fragments
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_achievements -> AchievementsFragment()
                else -> return@setOnItemSelectedListener false
            }
            Log.d(TAG, "tab selected: ${item.title}")
            showFragment(fragment)
            tintNavForTab(item.itemId)
            true
        }

        recordTodayAppOpen()

    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.contentArea.id, fragment)
            .commit()
    }

    //change the nav highlight pill colour to the matching accent colour
    private fun tintNavForTab(itemId: Int) {
        val colourRes = when (itemId) {
            R.id.nav_dashboard -> R.color.accent_dashboard
            R.id.nav_transactions -> R.color.accent_transactions
            R.id.nav_achievements -> R.color.accent_achievements
            else -> R.color.bg_card
        }
        val tint = ColorStateList.valueOf(ContextCompat.getColor(this, colourRes))
        val drawable = ContextCompat.getDrawable(this, R.drawable.bg_nav_item_selected)
        drawable?.setTintList(tint)
        binding.bottomNav.itemBackground = drawable
    }

//    function to track if the app was opened today for badge tracking
    private fun recordTodayAppOpen() {
    val userId = Session.getUserId(this) ?: return
    val today = DateUtils.dayKey(System.currentTimeMillis())
    try {
        lifecycleScope.launch  {
            FirebaseRepository.getInstance().recordAppOpen(userId, today)
        }
        Log.d(TAG, "record today app open: $today")
        } catch (e: Exception) {
            Log.e(TAG, "record today app open: error", e)
        }
    }
}