package com.wannianli.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * 与系统日历/闹钟联动。
 */
object CalendarSync {

    const val REQUEST_CODE = 1001

    fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    /** 找一个可写入的系统日历,找不到返回 null。 */
    private fun findWritableCalendar(context: Context): Long? {
        val uri = Calendars.CONTENT_URI
        val projection = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME)
        val selection = "${Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        try {
            context.contentResolver.query(uri, projection, selection, args, null)?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
            // 兜底:任意日历
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    /**
     * 添加系统日历日程。
     * 返回 null 表示成功,否则返回错误提示文本。
     */
    fun addEvent(context: Context, date: LocalDate, hour: Int, minute: Int, title: String): String? {
        if (!hasWritePermission(context)) return "缺少日历写入权限"
        val calId = findWritableCalendar(context) ?: return "未找到可写入的系统日历"
        val start = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = start + 60 * 60 * 1000L // 默认1小时

        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, calId)
            put(Events.TITLE, title)
            put(Events.DTSTART, start)
            put(Events.DTEND, end)
            put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        return try {
            context.contentResolver.insert(Events.CONTENT_URI, values)
            null
        } catch (e: Exception) {
            "添加失败:${e.message}"
        }
    }

    /** 打开系统闹钟设置界面,联动闹钟提醒。返回是否成功打开。 */
    fun openSystemAlarm(context: Context, hour: Int, minute: Int, message: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
        }
        if (intent.resolveActivity(context.packageManager) == null) return false
        context.startActivity(intent)
        return true
    }

    fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
