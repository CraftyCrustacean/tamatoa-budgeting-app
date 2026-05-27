package com.example.tamatoa.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tamatoa.R
import com.example.tamatoa.data.BadgeState

//uses the recycler view similarly to the transactions screen
class BadgesAdapter(
    private var items: List<BadgeState> = emptyList(),
    private val onItemClick: (BadgeState) -> Unit = {}
) : RecyclerView.Adapter<BadgesAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: View = itemView.findViewById(R.id.badgeIcon)
        val tick: ImageView = itemView.findViewById(R.id.earnedTick)
        val title: TextView = itemView.findViewById(R.id.badgeTitle)
        val progress: TextView = itemView.findViewById(R.id.badgeProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val state = items[position]
        val def = state.definition

        holder.title.text = def.title
        //only show the tick if the achievement has been earned
        holder.tick.visibility = if (state.earned) View.VISIBLE else View.GONE

        //if the badge is earned say so, otherwise show progress
        holder.progress.text = if (state.earned) {
            "Earned"
        } else {
            "${state.current} / ${def.goal} ${def.unit}"
        }

        //change the ring colour to the achievement colour
        val colour = try {
            Color.parseColor(def.iconColourHex)
        } catch (e: Exception) {
            Color.GRAY
        }
        val ring = holder.icon.background as? GradientDrawable
        ring?.setStroke(8, colour)
        holder.tick.imageTintList = android.content.res.ColorStateList.valueOf(colour)
        holder.icon.alpha = if (state.earned) 1.0f else 0.45f

        holder.itemView.setOnClickListener { onItemClick(state) }
    }

    fun submit(newItems: List<BadgeState>) {
        items = newItems
        notifyDataSetChanged()
    }
}