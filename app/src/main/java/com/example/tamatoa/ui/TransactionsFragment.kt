package com.example.tamatoa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tamatoa.databinding.FragmentTransactionsBinding

//Transaction fragment, placeholder currently will be fixed soon^(tm)
//information found at: https://www.youtube.com/watch?v=MWkplgwdY20
//and https://stackoverflow.com/questions/51968709/android-studio-bottom-bar-nav-how-do-i-add-to-each-fragment
//and https://www.youtube.com/watch?v=OV25x3a55pk
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}