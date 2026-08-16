package com.wannianli.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class MonthAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<MonthAdapter.VH>() {

    data class Cell(
        val date: LocalDate?,
        val dayText: String,
        val subText: String,
        val isToday: Boolean,
        val isSelected: Boolean
    )

    private val cells = mutableListOf<Cell>()

    fun setMonth(month: LocalDate, selected: LocalDate, today: LocalDate) {
        cells.clear()
        val first = month.withDayOfMonth(1)
        val leading = first.dayOfWeek.value % 7 // 周日为0
        repeat(leading) { cells.add(Cell(null, "", "", false, false)) }

        val daysInMonth = month.lengthOfMonth()
        for (d in 1..daysInMonth) {
            val date = first.plusDays((d - 1).toLong())
            val lunar = CalendarLogic.solarToLunar(date)
            val festival = CalendarLogic.festivalOf(date)
            val sub = when {
                festival != null -> festival
                lunar.day == 1 -> (if (lunar.isLeap) "闰" else "") + CalendarLogic.MONTH_NAMES[lunar.month - 1]
                else -> CalendarLogic.DAY_NAMES[lunar.day - 1]
            }
            cells.add(Cell(date, d.toString(), sub, date == today, date == selected))
        }
        while (cells.size % 7 != 0) cells.add(Cell(null, "", "", false, false))
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = cells.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(cells[position], onDayClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay = itemView.findViewById<TextView>(R.id.tvDay)
        private val tvLunar = itemView.findViewById<TextView>(R.id.tvLunar)

        fun bind(cell: Cell, onDayClick: (LocalDate) -> Unit) {
            tvDay.text = cell.dayText
            tvLunar.text = cell.subText

            val rv = itemView.parent as? RecyclerView
            if (rv != null) {
                val rows = (cells.size / 7).coerceAtLeast(1)
                val targetH = if (rv.height > 0) rv.height / rows
                else itemView.resources.getDimensionPixelSize(R.dimen.day_cell_fallback_height)
                val lp = itemView.layoutParams
                if (lp != null && lp.height != targetH) {
                    lp.height = targetH
                    itemView.layoutParams = lp
                }
            }

            val date = cell.date ?: return

            itemView.setOnClickListener { onDayClick(date) }
            val ctx = itemView.context
            val dayColor: Int
            val bgRes: Int
            when {
                cell.isToday -> {
                    bgRes = R.drawable.bg_today
                    dayColor = ctx.getColor(R.color.day_today_text)
                }
                cell.isSelected -> {
                    bgRes = R.drawable.bg_selected
                    dayColor = ctx.getColor(R.color.colorPrimary)
                }
                else -> {
                    bgRes = 0
                    dayColor = ctx.getColor(R.color.day_text)
                }
            }
            tvDay.setBackgroundResource(bgRes)
            tvDay.setTextColor(dayColor)
            tvLunar.setTextColor(ctx.getColor(R.color.lunar_text))
        }
    }
}
