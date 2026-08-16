package com.wannianli.app

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MonthAdapter
    private var currentMonth: LocalDate = LocalDate.now().withDayOfMonth(1)
    private var selectedDate: LocalDate = LocalDate.now()

    private var pendingEvent: PendingEvent? = null

    private data class PendingEvent(
        val date: LocalDate,
        val hour: Int,
        val minute: Int,
        val title: String,
        val withAlarm: Boolean
    )

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
        findViewById<View>(R.id.btnAddEvent).setOnClickListener {
            showAddEventDialog(selectedDate)
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

    private fun showAddEventDialog(date: LocalDate) {
        val view = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val cbAlarm = view.findViewById<CheckBox>(R.id.cbAlarm)

        var hour = LocalTime.now().hour
        var minute = LocalTime.now().minute
        tvTime.text = String.format("%02d:%02d", hour, minute)
        tvTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, m ->
                    hour = h
                    minute = m
                    tvTime.text = String.format("%02d:%02d", h, m)
                },
                hour, minute, true
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("添加日程 · ${date.monthValue}月${date.dayOfMonth}日")
            .setView(view)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = etTitle.text.toString().trim()
                        if (title.isEmpty()) {
                            etTitle.error = "请输入日程内容"
                            return@setOnClickListener
                        }
                        saveEvent(PendingEvent(date, hour, minute, title, cbAlarm.isChecked))
                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun saveEvent(event: PendingEvent) {
        if (!CalendarSync.hasWritePermission(this)) {
            pendingEvent = event
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_CALENDAR),
                CalendarSync.REQUEST_CODE
            )
            return
        }
        doSaveEvent(event)
    }

    private fun doSaveEvent(event: PendingEvent) {
        val err = CalendarSync.addEvent(this, event.date, event.hour, event.minute, event.title)
        if (err != null) {
            CalendarSync.toast(this, err)
            return
        }
        CalendarSync.toast(this, "已同步到系统日历")
        if (event.withAlarm) {
            val ok = CalendarSync.openSystemAlarm(this, event.hour, event.minute, event.title)
            if (!ok) CalendarSync.toast(this, "未找到可用的闹钟应用")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CalendarSync.REQUEST_CODE) {
            val event = pendingEvent ?: return
            pendingEvent = null
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                doSaveEvent(event)
            } else {
                CalendarSync.toast(this, "需要日历权限才能添加日程")
            }
        }
    }
}
