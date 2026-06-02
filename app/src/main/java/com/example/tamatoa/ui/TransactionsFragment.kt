package com.example.tamatoa.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tamatoa.AddExpenseActivity
import com.example.tamatoa.PhotoViewerActivity
import com.example.tamatoa.Session
import com.example.tamatoa.data.DateUtils
import com.example.tamatoa.data.ExpenseWithCategory
import com.example.tamatoa.data.FirebaseRepository
import com.example.tamatoa.data.RecurringExpenseGenerator
import com.example.tamatoa.databinding.FragmentTransactionsBinding
import kotlinx.coroutines.launch
import java.util.Calendar

//show list of expenses for the logged in user, filterable by time period
//tapping a row with a photo opens it
//information found from: https://www.youtube.com/watch?v=nuLyv70ViLc
//https://www.youtube.com/watch?v=opT7iABvisY
//https://www.youtube.com/watch?v=DugmDbggwcQ
//https://www.youtube.com/watch?v=MWkplgwdY20
//https://stackoverflow.com/questions/51968709/android-studio-bottom-bar-nav-how-do-i-add-to-each-fragment
//https://www.youtube.com/watch?v=OV25x3a55pk
//https://www.youtube.com/watch?v=u26D04J7Q-E
class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExpensesAdapter

    //what time period is currently selected
    private var rangeStart: Long = 0L
    private var rangeEnd: Long = 0L

    companion object {
        private const val TAG = "TransactionsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExpensesAdapter(
            onItemClick = { expense ->
                if (expense.photoPath != null) {
                    val i = Intent(requireContext(), PhotoViewerActivity::class.java)
                    i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_PATH, expense.photoPath)
                    startActivity(i)
                }
            },
            onItemLongClick = { expense ->
                confirmDelete(expense)
            }
        )

        binding.expensesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.expensesRecycler.adapter = adapter

        binding.periodLabel.setOnClickListener { showPeriodPicker() }
        binding.addExpenseFab.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        //by default show what entries were made this month
        applyThisMonth()
    }

    //when the user comes back to this screen, also run the recurring generator to generate
    //recurring expenses
    override fun onResume() {
        super.onResume()
        runRecurringGeneratorThenReload()
    }

    private fun runRecurringGeneratorThenReload() {
        val userId = Session.getUserId(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val repo = FirebaseRepository.getInstance()
                RecurringExpenseGenerator.generateForCurrentMonth(repo, userId)
            } catch (e: Exception) {
                Log.e(TAG, "run recurring generator: error", e)
            }
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        val userId = Session.getUserId(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val repo = FirebaseRepository.getInstance()
                val items = repo.getBetween(userId, rangeStart, rangeEnd)
                Log.d(TAG, "load expenses: ${items.size} items in range")

                adapter.submit(items)
                binding.emptyState.root.visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "load expenses: error", e)
            }
        }
    }
    //period pricker, showing options for this month, last month, and a custom range
    private fun showPeriodPicker() {
        val options = arrayOf("This month", "Last month", "Custom range...")
        AlertDialog.Builder(requireContext())
            .setTitle("Show expenses from")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyThisMonth()
                    1 -> applyLastMonth()
                    2 -> showCustomRangePicker()
                }
            }
            .show()
    }

    //if they select the first one start fom the beginning of this month to the end of it
    private fun applyThisMonth() {
        val now = System.currentTimeMillis()
        rangeStart = DateUtils.startOfMonth(now)
        rangeEnd = DateUtils.endOfMonth(now)
        binding.periodLabel.text = "This month ▾"
        loadExpenses()
    }

    //same as above but now for the previous month
    private fun applyLastMonth() {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) } //math to -1 month
        rangeStart = DateUtils.startOfMonth(cal.timeInMillis)
        rangeEnd = DateUtils.endOfMonth(cal.timeInMillis)
        binding.periodLabel.text = "Last month ▾"
        loadExpenses()
    }

    private fun showCustomRangePicker() {
        //date pickers for the start and end dates
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, sy, sm, sd ->
                val start = Calendar.getInstance().apply {
                    set(sy, sm, sd, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)}.timeInMillis

                DatePickerDialog(
                    requireContext(),
                    { _, ey, em, ed ->
                        val end = Calendar.getInstance().apply {
                            set(ey, em, ed, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)}.timeInMillis

                        //if they picked a start thats after the end swap them around
                        //otherwise continue
                        if (end < start) {
                            Log.w(TAG, "custom range: end before start?? Swapped")
                            rangeStart = end
                            rangeEnd = start
                        } else {
                            rangeStart = start
                            rangeEnd = end
                        }
                        binding.periodLabel.text =
                            "${DateUtils.formatShort(rangeStart)} – ${DateUtils.formatShort(rangeEnd)} ▾"
                        loadExpenses()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("End date")
                    show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Start date")
            show()
        }
    }

    //a way to delete expenses in the list
    private fun confirmDelete(expense: ExpenseWithCategory) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete this expense?")
            .setMessage("This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val userId = Session.getUserId(requireContext()) ?: return@setPositiveButton
                lifecycleScope.launch {
                    try {
                        //call the delete dao and delete the selected expense by id
                        val repo = FirebaseRepository.getInstance()
                        repo.deleteExpense(userId, expense.id)
                        Log.d(TAG, "deleted expense id=${expense.id}")
                        loadExpenses()
                    } catch (e: Exception) {
                        Log.e(TAG, "delete: error", e)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}