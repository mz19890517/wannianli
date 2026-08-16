package com.wannianli.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MonthAdapter
    private var currentMonth: LocalDate = LocalDate.now().withDayOfMonth(1)
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = MonthAdapter { date ->
            selectedDate = date
            updateDetail(date)
        }
        val rv = findViewById<RecyclerView>(R.id.rvMonth)
        rv.layoutManager = GridLayoutManager(this, 7)
        rv.adapter = adapter

        findViewById<View>(R.id.btnPrev).setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            reload()
        }
        findViewById<View>(R.id.btnNext).setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            reload()
        }
        findViewById<View>(R.id.btnToday).setOnClickListener {
            val today = LocalDate.now()
            currentMonth = today.withDayOfMonth(1)
            selectedDate = today
            reload()
        }

        reload()
    }

    private fun reload() {
        findViewById<TextView>(R.id.tvMonthTitle).text =
            "${currentMonth.year}年${currentMonth.monthValue}月"
        adapter.setMonth(currentMonth, selectedDate, LocalDate.now())
        updateDetail(selectedDate)
    }

    private fun updateDetail(date: LocalDate) {
        val lunar = CalendarLogic.solarToLunar(date)
        val yg = CalendarLogic.yearGanzhi(date)
        val mg = CalendarLogic.monthGanzhi(date)
        val dgIdx = CalendarLogic.sexagenaryDay(date)
        val dg = "${CalendarLogic.HEAVENLY_STEMS[dgIdx % 10]}${CalendarLogic.EARTHLY_BRANCHES[dgIdx % 12]}"
        val weekdays = arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
        val info = Huangli.of(date)

        findViewById<TextView>(R.id.tvSolar).text =
            "${date.year}年${date.monthValue}月${date.dayOfMonth}日 " +
                    "${weekdays[date.dayOfWeek.value - 1]}"
        findViewById<TextView>(R.id.tvLunar).text =
            "农历 ${CalendarLogic.lunarYearGanzhi(lunar.year).text}年 ${CalendarLogic.lunarMonthDayText(lunar)}"
        findViewById<TextView>(R.id.tvGanzhi).text =
            "干支 $yg 年 $mg 月 $dg 日"

        val term = CalendarLogic.festivalOf(date)
            ?.takeIf { it in CalendarLogic.SOLAR_TERM_NAMES }

        findViewById<TextView>(R.id.tvHuangli).text = buildString {
            append("值神:${info.zhiShen}  冲${info.chong}煞${info.sha}")
            if (term != null) append("  节气:$term")
            append("\n宜:${info.yi.joinToString("、")}")
            append("\n忌:${info.ji.joinToString("、")}")
        }
    }
}
