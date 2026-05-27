package com.example.tamatoa.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tamatoa.LoginActivity
import com.example.tamatoa.Session
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.data.BadgeEvaluator
import com.example.tamatoa.data.BadgeState
import com.example.tamatoa.databinding.FragmentAchievementsBinding
import kotlinx.coroutines.launch

//Achievements fragment, placeholder currently, will be fixed soon^(tm) (final poe)
//temporary(permanent) home of the logout button
//https://stackoverflow.com/questions/3473168/clear-the-entire-history-stack-and-start-a-new-activity-on-android

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BadgesAdapter

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

        adapter = BadgesAdapter(onItemClick = { state -> showBadgeDetails(state) })
        binding.badgesGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.badgesGrid.adapter = adapter

        binding.loadDemoButton.setOnClickListener { confirmLoadDemoData() }
        binding.logoutButton.setOnClickListener { confirmLogout() }
        binding.exportButton.setOnClickListener { exportCsv() }
    }

    override fun onResume() {
        super.onResume()
        evaluateAndShow()
    }

    private fun evaluateAndShow() {
        val userId = Session.getUserId(requireContext())
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val states = BadgeEvaluator.evaluate(db, userId)
                Log.d(TAG, "evaluate and show: ${states.count { it.earned }} of ${states.size} earned")
                adapter.submit(states)
            } catch (e: Exception) {
                Log.e(TAG, "evaluate and show: error", e)
            }
        }
    }

    // Confirms with the user before clearing the session. Intent flags wipe
    // the back stack so they can't return to the dashboard via the back button.
    // References:
    //   AlertDialog: https://developer.android.com/develop/ui/views/components/dialogs#AlertDialog
    //   Activity flags: https://developer.android.com/guide/components/activities/tasks-and-back-stack
    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out?")
            .setMessage("You'll need to log in again to see your data.")
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

    //confirm with the user before seeding the database
    private fun confirmLoadDemoData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Load demo data?")
            .setMessage("Adds months of expense history so the achievements can show as earned for the demo")
            .setPositiveButton("Load") { _, _ ->
                val userId = Session.getUserId(requireContext())
                lifecycleScope.launch {
                    try {
                        val db = AppDatabase.getInstance(requireContext())
                        com.example.tamatoa.data.DemoDataSeeder.seed(db, userId)
                        Log.d(TAG, "demo data seeded")
                        evaluateAndShow()
                        android.widget.Toast.makeText(
                            requireContext(), "demo data loaded", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "demo seed failed", e)
                        android.widget.Toast.makeText(
                            requireContext(), "demo seed failed", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //function to show the badge requirements when you tap on it
    private fun showBadgeDetails(state: BadgeState) {
        val def = state.definition

        //the popup shows the title of the achievement and its description
        AlertDialog.Builder(requireContext())
            .setTitle(def.title)
            .setMessage("${def.description}\n")
            .setPositiveButton("OK", null)
            .show()
    }

    //fetches every expense for the logged in user and writes it to a csv file
    //shows the file path as a toast
    private fun exportCsv() {
        val userId = Session.getUserId(requireContext())
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val rows = db.expenseDao().getAllForUser(userId)

                if (rows.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "You have no expenses to export",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val file = com.example.tamatoa.data.CsvExporter.export(requireContext(), rows)
                Log.d(TAG, "export csv: wrote ${rows.size} rows to ${file.absolutePath}")

                Toast.makeText(requireContext(),
                    "Saved ${rows.size} expenses to ${file.name}",
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "export csv: error", e)
                Toast.makeText(requireContext(),
                    "Export failed",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}