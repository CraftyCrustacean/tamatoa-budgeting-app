package com.example.tamatoa.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tamatoa.AddExpenseActivity
import com.example.tamatoa.R
import com.example.tamatoa.Session
import com.example.tamatoa.data.AppDatabase
import com.example.tamatoa.data.BudgetEntity
import com.example.tamatoa.data.DateUtils
import com.example.tamatoa.data.RecurringExpenseGenerator
import com.example.tamatoa.databinding.FragmentDashboardBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar

//Home screen, houses: budget tracker and category totals, will also have graphs in p3

//information found at: https://www.youtube.com/watch?v=MWkplgwdY20
//and https://stackoverflow.com/questions/51968709/android-studio-bottom-bar-nav-how-do-i-add-to-each-fragment
//and https://www.youtube.com/watch?v=OV25x3a55pk
//https://bterczynski.medium.com/android-clipdrawable-897a308ca58
//https://www.youtube.com/watch?v=vFziVSSIX6Q
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    //currently selected date range
    private var rangeStart: Long = 0L
    private var rangeEnd: Long = 0L

    private lateinit var totalsAdapter: CategoryTotalsAdapter

    companion object {
        private const val TAG = "DashboardFragment"

        //percent at which the budget bar should change colour
        private const val WARN_THRESHOLD = 75
        private const val OVER_THRESHOLD = 100
    }

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

        //turn the recycler view on
        totalsAdapter = CategoryTotalsAdapter()
        binding.totalsRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.totalsRecycler.adapter = totalsAdapter

        binding.budgetCard.setOnClickListener { showSetBudgetDialog() }
        binding.periodLabel.setOnClickListener { showPeriodPicker() }
        binding.addExpense.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        applyThisMonth()
    }

    //rerun the recurring payment generator and refresh when this screen is shown
    override fun onResume() {
        super.onResume()
        runRecurringGeneratorThenReload()
    }

    private fun runRecurringGeneratorThenReload() {
        val userId = Session.getUserId(requireContext())
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                RecurringExpenseGenerator.generateForCurrentMonth(db, userId)
            } catch (e: Exception) {
                Log.e(TAG, "run recurring generator: error", e)
            }
            refresh()
        }
    }

    //get the budget of logged in user and how much theyve spent over the current date range
    //also get the total amount spent per category in the current date range
    private fun refresh() {
        val userId = Session.getUserId(requireContext())
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val budget = db.budgetDao().get(userId)
                val spend = db.expenseDao().getTotalSpend(userId, rangeStart, rangeEnd)
                val totals = db.expenseDao().getCategoryTotals(userId, rangeStart, rangeEnd)
                Log.d(TAG, "refresh: budget=${budget?.maxSpend} spent=$spend categories=${totals.size}")
                renderBudgetCard(budget, spend)
                renderTotals(totals)
            } catch (e: Exception) {
                Log.e(TAG, "refresh: error", e)
            }
        }
    }

    //only show categories that had money spent in them
    private fun renderTotals(totals: List<com.example.tamatoa.data.CategoryTotal>) {
        //total must be greater than 0
        val nonZero = totals.filter { it.total > 0.0 }
        totalsAdapter.submit(nonZero)
        //if the list is empty then show the empty totals view (hide the list)
        binding.totalsEmpty.visibility = if (nonZero.isEmpty()) View.VISIBLE else View.GONE
        //if the list isnt empty show the list (hide the empty totals)
        binding.totalsRecycler.visibility = if (nonZero.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun renderBudgetCard(budget: BudgetEntity?, spent: Double) {
        if (budget == null) {
            binding.budgetSummary.text = "Set a budget"
            binding.budgetPercent.text = ""
            binding.budgetProgress.progress = 0
            binding.budgetMinSave.visibility = View.GONE
            return
        }


        val max = budget.maxSpend
        //math the budget percent
        val percent = if (max <= 0.0) 0 else ((spent / max) * 100).toInt()

        binding.budgetSummary.text = "R %.0f / %.0f".format(spent, max)
        binding.budgetPercent.text = "$percent%"


        //fill the bar between 0-100% and cap at full even though user can keep going over
        binding.budgetProgress.progress = percent.coerceIn(0, 100)

        //when the budget reaches 75% go yellow and at 100% go red
        val fillColourRes = when {
            percent >= OVER_THRESHOLD -> R.color.progress_over
            percent >= WARN_THRESHOLD -> R.color.progress_warn
            else -> R.color.progress_ok
        }
        colourProgressFill(fillColourRes)

        //show the saving goal if it is greater than 0 (dunno why we need a saving goal its odd)
        if (budget.minSave > 0.0) {
            binding.budgetMinSave.text = "Min save goal: R %.0f".format(budget.minSave)
            binding.budgetMinSave.visibility = View.VISIBLE
        } else {
            binding.budgetMinSave.visibility = View.GONE
        }
    }


    //colour the bar
    private fun colourProgressFill(colourRes: Int) {
        val colour = ContextCompat.getColor(requireContext(), colourRes)
        val layered = binding.budgetProgress.progressDrawable as? android.graphics.drawable.LayerDrawable
        val fill = layered?.findDrawableByLayerId(android.R.id.progress)
        fill?.setTint(colour)
    }

    //pop up with the form to update/add a budget/saving goal
    private fun showSetBudgetDialog() {
        val view = layoutInflater.inflate(R.layout.set_budget, null)
        val maxInput = view.findViewById<TextInputEditText>(R.id.maxSpendInput)
        val minInput = view.findViewById<TextInputEditText>(R.id.minSaveInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Monthly budget")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val maxSpend = maxInput.text?.toString()?.toDoubleOrNull()
                val minSave = minInput.text?.toString()?.toDoubleOrNull() ?: 0.0 //allow saving to be optional

                //if null or 0 or below throw an error
                if (maxSpend == null || maxSpend <= 0.0) {
                    toast("Please enter a max spend greater than zero")
                    return@setPositiveButton
                }
                //cant have min save be to spend
                if (minSave < 0.0) {
                    toast("Min save can't be negative")
                    return@setPositiveButton
                }
                saveBudget(maxSpend, minSave)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //write the budget to the database
    private fun saveBudget(maxSpend: Double, minSave: Double) {
        val userId = Session.getUserId(requireContext())
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                db.budgetDao().upsert(
                    BudgetEntity(userId = userId, maxSpend = maxSpend, minSave = minSave)
                )
                Log.d(TAG, "save budget: max=$maxSpend min=$minSave")
                toast("Budget saved")
                refresh()
            } catch (e: Exception) {
                Log.e(TAG, "save budget: error", e)
                toast("Could not save")
            }
        }
    }

    //date range picker copied straight from the transaction fragment
    private fun showPeriodPicker() {
        val options = arrayOf("This month", "Last month", "Custom range...")
        AlertDialog.Builder(requireContext())
            .setTitle("Show data from")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyThisMonth()
                    1 -> applyLastMonth()
                    2 -> showCustomRangePicker()
                }
            }
            .show()
    }

    private fun applyThisMonth() {
        val now = System.currentTimeMillis()
        rangeStart = DateUtils.startOfMonth(now)
        rangeEnd = DateUtils.endOfMonth(now)
        binding.periodLabel.text = "This month ▾"
        refresh()
    }

    private fun applyLastMonth() {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        rangeStart = DateUtils.startOfMonth(cal.timeInMillis)
        rangeEnd = DateUtils.endOfMonth(cal.timeInMillis)
        binding.periodLabel.text = "Last month ▾"
        refresh()
    }

    private fun showCustomRangePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, sy, sm, sd ->
                val start = Calendar.getInstance().apply {
                    set(sy, sm, sd, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                DatePickerDialog(
                    requireContext(),
                    { _, ey, em, ed ->
                        val end = Calendar.getInstance().apply {
                            set(ey, em, ed, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        if (end < start) {
                            rangeStart = end; rangeEnd = start
                        } else {
                            rangeStart = start; rangeEnd = end
                        }
                        binding.periodLabel.text =
                            "${DateUtils.formatShort(rangeStart)} – ${DateUtils.formatShort(rangeEnd)} ▾"
                        refresh()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).apply { setTitle("End date"); show() }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply { setTitle("Start date"); show() }
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}