package com.example.tamatoa.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tamatoa.R
import com.example.tamatoa.data.CategoryTotal

//shows the total amount spent per category for the current selected period
//uses recylerView similar to the transaction list
//information found from: https://www.youtube.com/watch?v=nuLyv70ViLc
//https://www.youtube.com/watch?v=opT7iABvisY
class CategoryTotalsAdapter(
    //create a list for all the totals of each category
    private var items: List<CategoryTotal> = emptyList()
) : RecyclerView.Adapter<CategoryTotalsAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colourDot: View = itemView.findViewById(R.id.colourDot)
        val name: TextView = itemView.findViewById(R.id.categoryName)
        val total: TextView = itemView.findViewById(R.id.totalText)
    }

    //create the rows that will be displayed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_total, parent, false)
        return Holder(view)
    }

    //get how many entries are in the list
    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.total.text = "R %.2f".format(item.total)
        //try get the category colour, if it doesnt work (or there isnt one) make it grey
        try {
            holder.colourDot.background.setTint(Color.parseColor(item.colour))
        } catch (e: Exception) {
            holder.colourDot.background.setTint(Color.GRAY)
        }
    }

    fun submit(newItems: List<CategoryTotal>) {
        items = newItems
        notifyDataSetChanged()
    }
}