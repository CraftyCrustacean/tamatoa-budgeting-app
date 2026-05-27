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
import android.graphics.Color
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.tamatoa.data.DailySpend
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale

//Home screen, houses: budget tracker and category totals, will also have graphs in p3

//information found at: https://www.youtube.com/watch?v=MWkplgwdY20
//and https://stackoverflow.com/questions/51968709/android-studio-bottom-bar-nav-how-do-i-add-to-each-fragment
//and https://www.youtube.com/watch?v=OV25x3a55pk
//https://bterczynski.medium.com/android-clipdrawable-897a308ca58
//https://www.youtube.com/watch?v=vFziVSSIX6Q

//graph info found at: https://www.youtube.com/watch?v=5XOabSpW_Os
//https://www.youtube.com/watch?v=DD1CxoVONFE
//https://www.youtube.com/watch?v=MiVx3AQD_PI
//Heatmap based on information from:https://medium.com/@ali.zahid050/%EF%B8%8F-building-a-custom-calendarview-in-android-from-scratch-11d03b7b18f4
//https://www.alanzucconi.com/2016/01/06/colour-interpolation/
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

        //replaced the recycler view with new pie chart
        totalsAdapter = CategoryTotalsAdapter()
        binding.pieLegend.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.pieLegend.adapter = totalsAdapter
        configurePieChart()
        configureLineChart()

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
                val monthStart = DateUtils.startOfMonth(System.currentTimeMillis())
                val monthEnd = DateUtils.endOfMonth(System.currentTimeMillis())
                val dailySpend = db.expenseDao().getDailySpend(userId, monthStart, monthEnd)

                Log.d(TAG, "refresh: budget=${budget?.maxSpend} spent=$spend categories=${totals.size}")
                renderBudgetCard(budget, spend)
                renderTotals(totals)
                renderLineChart(dailySpend, budget)
                renderHeatmap(dailySpend)
            } catch (e: Exception) {
                Log.e(TAG, "refresh: error", e)
            }
        }
    }


    //feeds the pue chart and the legend list the same data
    private fun renderTotals(totals: List<com.example.tamatoa.data.CategoryTotal>) {
        val nonZero = totals.filter { it.total > 0.0 }

        if (nonZero.isEmpty()) {
            binding.pieEmpty.visibility = View.VISIBLE
            binding.pieChart.visibility = View.GONE
            binding.pieLegend.visibility = View.GONE
            return
        }

        binding.pieEmpty.visibility = View.GONE
        binding.pieChart.visibility = View.VISIBLE
        binding.pieLegend.visibility = View.VISIBLE

        // Build pie entries and colours from the per-category totals.
        val entries = nonZero.map { PieEntry(it.total.toFloat(), it.name) }
        val sliceColours = nonZero.map { runCatching { Color.parseColor(it.colour) }.getOrDefault(Color.GRAY) }

        val dataSet = PieDataSet(entries, "").apply {
            colors = sliceColours
            // Don't draw values on slices - the legend below carries the numbers.
            setDrawValues(false)
            sliceSpace = 2f
        }

        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.invalidate()

        // Legend / totals list.
        totalsAdapter.submit(nonZero)
    }

    private fun configurePieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            //no need for built in legend
            legend.isEnabled = false
            //show totals as is, not as a percent
            setDrawEntryLabels(false)
            setUsePercentValues(false)
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(false)
            holeRadius = 55f
            transparentCircleRadius = 58f
            animateY(700, Easing.EaseInOutQuad)
        }
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


    private fun configureLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            //cant zoom or scroll
            setTouchEnabled(false)
            setDrawGridBackground(false)
            setNoDataText("")

            //x axis shows days of the month
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.parseColor("#B0B0B0")
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f

            //y axis shows how much is spent
            axisLeft.textColor = Color.parseColor("#B0B0B0")
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.parseColor("#22FFFFFF")
            axisLeft.axisMinimum = 0f

            //hide the right y axis
            axisRight.isEnabled = false
        }
    }


    //plots the cumulative spend for the current month
    //adds a two dashed lines to show budget and savings goals
    private fun renderLineChart(dailySpend: List<DailySpend>, budget: BudgetEntity?) {
        val chart = binding.lineChart

        if (dailySpend.isEmpty()) {
            binding.lineEmpty.visibility = View.VISIBLE
            chart.visibility = View.GONE
            return
        }
        binding.lineEmpty.visibility = View.GONE
        chart.visibility = View.VISIBLE

        //fetch how many days are in the current month
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        //go day by day and count spent, if there is a gap fill it with the previous spend for a flat line
        val byDay = dailySpend.associateBy { it.dayKey }
        val monthPrefix = DateUtils.dayKey(System.currentTimeMillis()).substring(0, 7) // "YYYY-MM"

        val entries = mutableListOf<Entry>()
        var runningTotal = 0f

        for (day in 1..daysInMonth) {
            val dayKey = "%s-%02d".format(monthPrefix, day)
            val dayTotal = byDay[dayKey]?.total?.toFloat() ?: 0f
            runningTotal += dayTotal
            entries.add(Entry(day.toFloat(), runningTotal))
        }

        val dataSet = LineDataSet(entries, "Expenses").apply {
            color = Color.parseColor("#F2A03D")
            lineWidth = 2.5f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#F2A03D")
            fillAlpha = 60
            mode = LineDataSet.Mode.LINEAR
        }
        chart.data = LineData(dataSet)

        //lines for budget and savings goals
        chart.axisLeft.removeAllLimitLines()
        chart.axisLeft.resetAxisMaximum()
        budget?.let {
            if (it.maxSpend > 0.0) {

                chart.axisLeft.axisMaximum = it.maxSpend.toFloat() * 1.1f

                chart.axisLeft.addLimitLine(LimitLine(it.maxSpend.toFloat(), "Budget").apply {
                    lineColor = Color.parseColor("#E04848")
                    lineWidth = 1.5f
                    enableDashedLine(10f, 6f, 0f)
                    textColor = Color.parseColor("#E04848")
                    textSize = 10f
                })
            }
            if (it.minSave > 0.0 && it.maxSpend > it.minSave) {
                val saveCeiling = (it.maxSpend - it.minSave).toFloat()
                chart.axisLeft.addLimitLine(LimitLine(saveCeiling, "Savings goal").apply {
                    lineColor = Color.parseColor("#3DBE6F")
                    lineWidth = 1.5f
                    enableDashedLine(10f, 6f, 0f)
                    textColor = Color.parseColor("#3DBE6F")
                    textSize = 10f
                })
            }
        }

        chart.invalidate()
    }

    //the big heatmap function
    private fun renderHeatmap(dailySpend: List<DailySpend>) {
        val ctx = requireContext()

        //current date as the header
        val now = System.currentTimeMillis()
        val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.heatmapMonthLabel.text = monthLabelFormat.format(java.util.Date(now))

        //labels for each day
        binding.heatmapWeekdays.removeAllViews()
        for (label in listOf("M", "T", "W", "T", "F", "S", "S")) {
            binding.heatmapWeekdays.addView(makeWeekdayLabel(ctx, label))
        }

        //calculate total spent on each day of the month that has expenses
        val byDayOfMonth = mutableMapOf<Int, Double>()
        for (entry in dailySpend) {
            //daykey is yyyy-mm-dd so we only take the last 2
            val day = entry.dayKey.takeLast(2).toIntOrNull() ?: continue
            byDayOfMonth[day] = entry.total
        }

        //figure out what weekday the 1st is on and how many days are in that month
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        //some black magic math to get monday to equal 0 and sunday to equal 6
        val firstWeekday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        //fetch the most expensive day to scale the rest of the heatmap off of
        val maxSpendDay = byDayOfMonth.values.maxOrNull() ?: 0.0

        val grid = binding.heatmapGrid
        grid.removeAllViews()
        //fill spaces with empty cells until its the first day of the month
        for (i in 0 until firstWeekday) {
            grid.addView(makeEmptyCell(ctx))
        }
        //grid for the rest of the days
        for (day in 1..daysInMonth) {
            val spent = byDayOfMonth[day] ?: 0.0
            grid.addView(makeDayCell(ctx, day, spent, maxSpendDay))
        }

    }

    //header label for the cells (M, T, etc) for each column
    private fun makeWeekdayLabel(ctx: android.content.Context, text: String): TextView {
        return TextView(ctx).apply {
            this.text = text
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            gravity = Gravity.CENTER
            val lp = GridLayout.LayoutParams()
            lp.width = 0
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = lp
        }
    }

    //empty cells for the potentially empty days at the start of the month
    private fun makeEmptyCell(ctx: android.content.Context): View {
        return View(ctx).apply {
            val lp = GridLayout.LayoutParams()
            val cellSize = dpToPx(ctx, 36)
            lp.width = cellSize
            lp.height = cellSize
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            lp.setMargins(dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 2))
            layoutParams = lp
        }
    }

    //cells for the days of each month
    private fun makeDayCell(
        ctx: android.content.Context,
        day: Int,
        spent: Double,
        maxSpend: Double
    ): View {
        val frame = android.widget.FrameLayout(ctx)
        val lp = GridLayout.LayoutParams()
        val cellSize = dpToPx(ctx, 36)
        lp.width = cellSize
        lp.height = cellSize
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        lp.setMargins(dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 2))
        frame.layoutParams = lp

        //background gradient colour that becomes more intense based on where the most money was spent
        val intensity = if (maxSpend <= 0.0) 0.0 else (spent / maxSpend) //intensity is divided by the max spend day that month to scale
        val bg = ContextCompat.getDrawable(ctx, R.drawable.hm_cell)?.mutate() as? GradientDrawable
        bg?.setColor(heatColour(ctx, intensity))
        frame.background = bg

        //day number is placed on top of the cell
        val numLabel = TextView(ctx).apply {
            text = day.toString()
            textSize = 12f
            //the background is dark so light text is needed, however when the instensity of the
            //gradient increases it becomes hard to see light numbers so it gets swapped to black
            setTextColor(ContextCompat.getColor(ctx,
                if (intensity > 0.5) R.color.bg_app else R.color.text_secondary))
            gravity = Gravity.CENTER
            val numLp = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = numLp
        }
        frame.addView(numLabel)
        return frame
    }

    private fun heatColour(ctx: android.content.Context, intensity: Double): Int {
        //intensity is measured between 0 and 1 as a float, floats are weird but kotlin has coerceIn
        //which makes sure no rounding issues happen
        val stay = intensity.coerceIn(0.0, 1.0)
        //the starting point for the gradient is the background colour
        val cold = ContextCompat.getColor(ctx, R.color.bg_card)
        //the end point is the orange colour of the dashboard
        val hot = ContextCompat.getColor(ctx, R.color.accent_dashboard)

        val r = howMuchBlend(Color.red(cold), Color.red(hot), stay)
        val g = howMuchBlend(Color.green(cold), Color.green(hot), stay)
        val b = howMuchBlend(Color.blue(cold), Color.blue(hot), stay)
        return Color.rgb(r, g, b)
    }


    //this function does the math to tell heatColour how much it should blend the hot and cold
    private fun howMuchBlend(start: Int, end: Int, t: Double): Int =
        (start + (end -start) * t).toInt()

    private fun dpToPx(ctx: android.content.Context, dp: Int): Int {
        val density = ctx.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}