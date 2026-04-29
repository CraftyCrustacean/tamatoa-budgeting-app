package com.example.tamatoa.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.tamatoa.LoginActivity
import com.example.tamatoa.Session
import com.example.tamatoa.databinding.FragmentAchievementsBinding

//Achievements fragment, placeholder currently, will be fixed soon^(tm) (final poe)
//temporary home of the logout button
//https://stackoverflow.com/questions/3473168/clear-the-entire-history-stack-and-start-a-new-activity-on-android
class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "AchievementsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoutButton.setOnClickListener { confirmLogout() }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out?")
            .setMessage("Youll need to log in again to see your information.")
            .setPositiveButton("Log out") { _, _ ->
                Log.d(TAG, "logging out user ${Session.getUserId(requireContext())}")
                Session.clear(requireContext())
                val i = Intent(requireContext(), LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}