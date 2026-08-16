package com.wannianli.app

import java.time.LocalDate

/**
 * 简化版黄历:基于建除十二神推导宜忌,并给出冲煞信息。
 * 仅供传统文化参考,非专业择吉依据。
 */
object Huangli {

    private val zhiShenNames = arrayOf("建日", "除日", "满日", "平日", "定日", "执日", "破日", "危日", "成日", "收日", "开日", "闭日")

    private val yiMap = mapOf(
        0 to listOf("祭祀", "祈福", "出行", "求财", "见贵"),
        1 to listOf("祭祀", "祈福", "出行", "求嗣", "纳采", "嫁娶", "安床"),
        2 to listOf("祭祀", "祈福", "开光", "求嗣", "出行"),
        3 to listOf("祭祀", "祈福", "修坟", "安葬", "入殓"),
        4 to listOf("祭祀", "祈福", "嫁娶", "纳采", "订盟", "安床"),
        5 to listOf("祭祀", "祈福", "出行", "开市", "求财"),
        6 to listOf("破屋", "坏垣", "求医"),
        7 to listOf("祭祀", "祈福", "安葬", "入殓"),
        8 to listOf("祭祀", "祈福", "嫁娶", "开市", "交易", "入宅", "安床"),
        9 to listOf("祭祀", "祈福", "求财", "纳财"),
        10 to listOf("开市", "交易", "入宅", "嫁娶", "出行", "动土"),
        11 to listOf("祭祀", "祈福", "安葬", "入殓")
    )

    private val jiMap = mapOf(
        0 to listOf("开仓", "动土", "破土", "安葬"),
        1 to listOf("动土", "开仓"),
        2 to listOf("嫁娶", "安葬", "开仓", "动土"),
        3 to listOf("开市", "动土", "嫁娶"),
        4 to listOf("开市", "动土"),
        5 to listOf("嫁娶", "安葬"),
        6 to listOf("嫁娶", "出行", "安葬", "动土"),
        7 to listOf("出行", "开市"),
        8 to listOf("动土", "破土"),
        9 to listOf("开市", "动土", "安葬"),
        10 to listOf("安葬", "破土"),
        11 to listOf("开市", "出行", "动土")
    )

    data class Info(
        val zhiShen: String,
        val chong: String,
        val sha: String,
        val yi: List<String>,
        val ji: List<String>
    )

    fun of(date: LocalDate): Info {
        val dayBranch = CalendarLogic.sexagenaryDay(date) % 12
        val monthBranch = CalendarLogic.monthBranch(date)
        val jc = Math.floorMod(dayBranch - monthBranch, 12)

        val chong = CalendarLogic.ZODIACS[(dayBranch + 6) % 12]

        // 煞:按三合局定方位(申子辰煞午、寅午戌煞子、亥卯未煞酉、巳酉丑煞卯)
        val shaBranch = when (dayBranch) {
            8, 0, 4 -> 6   // 申子辰 -> 午
            2, 6, 10 -> 0  // 寅午戌 -> 子
            11, 3, 7 -> 9  // 亥卯未 -> 酉
            else -> 3      // 巳酉丑 -> 卯
        }
        val sha = CalendarLogic.DIRECTIONS[shaBranch]

        return Info(
            zhiShen = zhiShenNames[jc],
            chong = chong,
            sha = sha,
            yi = yiMap[jc] ?: emptyList(),
            ji = jiMap[jc] ?: emptyList()
        )
    }
}
