package com.example.tamatoa.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tamatoa.AddExpenseActivity
import com.example.tamatoa.databinding.FragmentDashboardBinding

//Dashboard fragment, placeholder currently will be fixed soon^(tm)
//information found at: https://www.youtube.com/watch?v=MWkplgwdY20
//and https://stackoverflow.com/questions/51968709/android-studio-bottom-bar-nav-how-do-i-add-to-each-fragment
//and https://www.youtube.com/watch?v=OV25x3a55pk
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addExpenseFab.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}