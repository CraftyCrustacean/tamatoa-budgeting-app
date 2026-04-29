package com.example.tamatoa.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tamatoa.R
import com.example.tamatoa.data.DateUtils
import com.example.tamatoa.data.ExpenseWithCategory

//helper to put rows of info into the recyclerview so that it has something to populate with
//information found from: https://www.youtube.com/watch?v=nuLyv70ViLc
//https://www.youtube.com/watch?v=opT7iABvisY
class ExpensesAdapter(
    private var items: List<ExpenseWithCategory> = emptyList(),
    private val onItemClick: (ExpenseWithCategory) -> Unit,
    private val onItemLongClick: (ExpenseWithCategory) -> Unit
) : RecyclerView.Adapter<ExpensesAdapter.Holder>() {

    //find the data for each row
    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colourDot: View = itemView.findViewById(R.id.colourDot)

        val category: TextView = itemView.findViewById(R.id.categoryName)
        val date: TextView = itemView.findViewById(R.id.dateText)
        val amount: TextView = itemView.findViewById(R.id.amountText)
        val recurringMark: TextView = itemView.findViewById(R.id.recurringMark)
        val photoMarc: TextView = itemView.findViewById(R.id.photoMarc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expense_row, parent, false)
        return Holder(view)
    }

    //size of the list
    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]

        // fetch the category name for title, and accompanying information
        holder.category.text = item.categoryName
        holder.date.text = DateUtils.formatShort(item.dateMillis)
        holder.amount.text = "-R %.2f".format(item.amount)

        // make the dot the category colour
        try {
            holder.colourDot.background.setTint(Color.parseColor(item.categoryColour))
        } catch (e: Exception) {
            holder.colourDot.background.setTint(Color.GRAY)
        }

        // show the recurring mark for original template and copies
        val showRecurring = item.isRecurring || item.recurringSourceId != null
        holder.recurringMark.visibility = if (showRecurring) View.VISIBLE else View.GONE

        //show the among u -- i mean paperclip icon for entries with attached photo
        holder.photoMarc.visibility =
            if (item.photoPath != null) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { onItemLongClick(item); true }
    }

    //Replace data and refresh the list
    fun submit(newItems: List<ExpenseWithCategory>) {
        items = newItems
        notifyDataSetChanged()
    }
}