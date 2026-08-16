package com.wannianli.app

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.floorMod

data class LunarDate(val year: Int, val month: Int, val isLeap: Boolean, val day: Int)

data class Ganzhi(val stem: Int, val branch: Int) {
    val text: String
        get() = CalendarLogic.HEAVENLY_STEMS[stem] + CalendarLogic.EARTHLY_BRANCHES[branch]
}

/**
 * 公历 <-> 农历、干支、节气、节日计算。
 * 农历数据表覆盖 1900-2100 年。
 */
object CalendarLogic {

    const val BASE_YEAR = 1900

    val HEAVENLY_STEMS = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val EARTHLY_BRANCHES = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val ZODIACS = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    val DIRECTIONS = arrayOf("北", "东北", "东北", "东", "东南", "东南", "南", "西南", "西南", "西", "西北", "西北")
    val MONTH_NAMES = arrayOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    val DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    val SOLAR_TERM_NAMES = arrayOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
        "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    // 农历数据表:每年(1900-2100)用一个 32 位整数描述闰月、大小月信息
    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, // 2040-2049
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, // 2050-2059
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, // 2060-2069
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, // 2070-2079
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, // 2080-2089
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, // 2090-2099
        0x0d520 // 2100
    )

    // 24 节气:相对 1900-01-06 02:05(小寒)的分钟偏移
    private val sTermInfo = intArrayOf(
        0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
        173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
        353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    )

    private val solarFestivals = mapOf(
        101 to "元旦", 214 to "情人节", 307 to "女生节", 308 to "妇女节",
        312 to "植树节", 401 to "愚人节", 501 to "劳动节", 504 to "青年节",
        601 to "儿童节", 701 to "建党节", 801 to "建军节", 910 to "教师节",
        1001 to "国庆节", 1225 to "圣诞节"
    )

    private val lunarFestivals = mapOf(
        101 to "春节", 115 to "元宵节", 202 to "龙抬头", 505 to "端午节",
        707 to "七夕", 715 to "中元节", 815 to "中秋节", 909 to "重阳节",
        1208 to "腊八节", 1223 to "小年"
    )

    fun leapMonth(year: Int): Int = lunarInfo[year - BASE_YEAR] and 0xf

    fun monthDays(year: Int, month: Int): Int =
        if (lunarInfo[year - BASE_YEAR] and (0x10000 shr month) != 0) 30 else 29

    fun leapDays(year: Int): Int =
        if (leapMonth(year) == 0) 0
        else if (lunarInfo[year - BASE_YEAR] and 0x10000 != 0) 30 else 29

    fun yearDays(year: Int): Int {
        var sum = 0
        for (m in 1..12) sum += monthDays(year, m)
        return sum + leapDays(year)
    }

    /** 公历日期转农历。支持 1900-01-31 之后。 */
    fun solarToLunar(date: LocalDate): LunarDate {
        val base = LocalDate.of(1900, 1, 31)
        var offset = ChronoUnit.DAYS.between(base, date).toInt()
        require(offset >= 0) { "仅支持1900年1月31日之后" }

        var year = BASE_YEAR
        while (true) {
            val yd = yearDays(year)
            if (offset < yd) break
            offset -= yd
            year++
        }

        val leap = leapMonth(year)
        var month = 1
        while (month <= 12) {
            val md = monthDays(year, month)
            if (offset < md) return LunarDate(year, month, false, offset + 1)
            offset -= md
            if (leap > 0 && month == leap) {
                val ld = leapDays(year)
                if (offset < ld) return LunarDate(year, month, true, offset + 1)
                offset -= ld
            }
            month++
        }
        return LunarDate(year, 12, false, offset + 1)
    }

    /** 日干支(60 甲子索引,0=甲子)。以 1949-10-01(甲子日)为基准。 */
    fun sexagenaryDay(date: LocalDate): Int =
        floorMod(ChronoUnit.DAYS.between(LocalDate.of(1949, 10, 1), date).toInt(), 60)

    /** 节气所在日。index 0=小寒,2=立春,... 23=冬至。 */
    fun solarTermDay(year: Int, index: Int): Int {
        val epoch = LocalDateTime.of(1900, 1, 6, 2, 5, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val ms = epoch + 31556925974L * (year - BASE_YEAR) + sTermInfo[index] * 60000L
        return Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).dayOfMonth
    }

    /** 年干支(以立春为年界)。 */
    fun yearGanzhi(date: LocalDate): Ganzhi {
        var y = date.year
        if (date.monthValue == 1 ||
            (date.monthValue == 2 && date.dayOfMonth < solarTermDay(y, 2))
        ) y--
        val idx = floorMod(y - 4, 60)
        return Ganzhi(idx % 10, idx % 12)
    }

    /** 月支索引(0=子)。以节气划月。 */
    fun monthBranch(date: LocalDate): Int {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        return if (m == 1) {
            if (d < solarTermDay(y, 0)) 0 else 1
        } else {
            var b = m % 12
            val lead = solarTermDay(y, (m - 2) * 2 + 2)
            if (d < lead) b = floorMod(b - 1, 12)
            b
        }
    }

    /** 月干支(五虎遁)。 */
    fun monthGanzhi(date: LocalDate): Ganzhi {
        val yg = yearGanzhi(date)
        val yinStem = (yg.stem % 5) * 2 + 2
        val b = monthBranch(date)
        return Ganzhi(floorMod(yinStem + (b - 2), 10), b)
    }

    /** 农历年干支(以正月初一为界)。 */
    fun lunarYearGanzhi(lunarYear: Int): Ganzhi {
        val idx = floorMod(lunarYear - 4, 60)
        return Ganzhi(idx % 10, idx % 12)
    }

    /** 是否除夕。 */
    fun isChuXi(lunar: LunarDate): Boolean =
        !lunar.isLeap && lunar.month == 12 && lunar.day == monthDays(lunar.year, 12)

    /** 公历/农历节日或节气名,无则 null。 */
    fun festivalOf(date: LocalDate): String? {
        solarFestivals[date.monthValue * 100 + date.dayOfMonth]?.let { return it }
        val lunar = solarToLunar(date)
        if (isChuXi(lunar)) return "除夕"
        if (!lunar.isLeap) {
            lunarFestivals[lunar.month * 100 + lunar.day]?.let { return it }
        }
        val t1 = solarTermDay(date.year, date.monthValue * 2 - 2)
        val t2 = solarTermDay(date.year, date.monthValue * 2 - 1)
        if (date.dayOfMonth == t1) return SOLAR_TERM_NAMES[date.monthValue * 2 - 2]
        if (date.dayOfMonth == t2) return SOLAR_TERM_NAMES[date.monthValue * 2 - 1]
        return null
    }

    /** 农历月日文字,如 七月初四 / 闰二月 / 初十。 */
    fun lunarMonthDayText(lunar: LunarDate): String {
        val prefix = if (lunar.isLeap) "闰" else ""
        return if (lunar.day == 1) prefix + MONTH_NAMES[lunar.month - 1]
        else DAY_NAMES[lunar.day - 1]
    }
}
