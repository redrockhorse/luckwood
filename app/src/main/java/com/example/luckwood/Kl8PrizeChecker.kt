package com.example.luckwood

import java.text.NumberFormat
import java.util.Locale

data class Kl8TicketPrizeResult(
    val hitCount: Int,
    val prize: Int?,
    val label: String
)

data class Kl8PrizeBreakdownItem(
    val hitCount: Int,
    val count: Int,
    val unitLabel: String,
    val subtotal: Int?
)

data class Kl8PrizeSummary(
    val fixedTotal: Int,
    val floatCount: Int,
    val floatTotal: Int,
    val total: Int,
    val breakdown: List<Kl8PrizeBreakdownItem>,
    val hasUnknownFloat: Boolean
)

data class Kl8CheckResult(
    val winning: List<Int>,
    val tickets: List<List<Int>>,
    val ticketResults: List<Kl8TicketPrizeResult>,
    val summary: Kl8PrizeSummary
)

object Kl8PrizeChecker {
    const val WINNING_COUNT = 20
    const val PICK_COUNT = 10
    const val GROUP_SIZE = 5
    const val FLOAT_CAP = 5_000_000

    val prizeRulesText = listOf(
        "中10：浮动奖，单注最高封顶500万元",
        "中9：8000元",
        "中8：800元",
        "中7：80元",
        "中6：5元",
        "中5：3元",
        "中0：2元",
        "（中1~4个无奖，规则来源：官网）"
    ).joinToString(" · ")

    private sealed class PrizeRule {
        object FloatPrize : PrizeRule()
        data class Fixed(val amount: Int) : PrizeRule()
    }

    private val prizeRules: Map<Int, PrizeRule> = mapOf(
        10 to PrizeRule.FloatPrize,
        9 to PrizeRule.Fixed(8000),
        8 to PrizeRule.Fixed(800),
        7 to PrizeRule.Fixed(80),
        6 to PrizeRule.Fixed(5),
        5 to PrizeRule.Fixed(3),
        0 to PrizeRule.Fixed(2)
    )

    fun formatMoney(yuan: Int): String {
        return "${NumberFormat.getNumberInstance(Locale.CHINA).format(yuan)} 元"
    }

    fun formatNumbers(numbers: List<Int>): String {
        return numbers.joinToString(" ") { "%02d".format(it) }
    }

    fun parseNumbers(str: String): List<Int> {
        return str.trim()
            .split(Regex("[\\s,，、]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                token.toIntOrNull()?.takeIf { it in 1..80 }
            }
    }

    fun parseTickets(input: String): List<List<Int>> {
        return input.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parseNumbers(it) }
    }

    fun parseFloat10Input(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val value = trimmed.toIntOrNull() ?: return Int.MIN_VALUE
        if (value < 0) return Int.MIN_VALUE
        return minOf(value, FLOAT_CAP)
    }

    fun validate(winning: List<Int>, tickets: List<List<Int>>): List<String> {
        val errors = mutableListOf<String>()

        if (winning.size != WINNING_COUNT) {
            errors.add("彩果需要 $WINNING_COUNT 个号码，当前 ${winning.size} 个")
        }
        if (winning.size != winning.toSet().size) {
            errors.add("彩果存在重复号码")
        }
        if (tickets.isEmpty()) {
            errors.add("请至少输入一注购买号码")
        }
        tickets.forEachIndexed { index, nums ->
            if (nums.size != PICK_COUNT) {
                errors.add("第 ${index + 1} 注需要 $PICK_COUNT 个号码，当前 ${nums.size} 个")
            }
            if (nums.size != nums.toSet().size) {
                errors.add("第 ${index + 1} 注存在重复号码")
            }
        }
        return errors
    }

    private fun getTicketPrize(hitCount: Int, float10Amount: Int?): Kl8TicketPrizeResult {
        val rule = prizeRules[hitCount]
        if (rule == null) {
            return Kl8TicketPrizeResult(hitCount, 0, "未中奖")
        }
        return when (rule) {
            is PrizeRule.FloatPrize -> {
                val prize = float10Amount
                Kl8TicketPrizeResult(
                    hitCount = hitCount,
                    prize = prize,
                    label = if (prize != null) formatMoney(prize) else "浮动奖（最高500万）"
                )
            }
            is PrizeRule.Fixed -> Kl8TicketPrizeResult(
                hitCount = hitCount,
                prize = rule.amount,
                label = formatMoney(rule.amount)
            )
        }
    }

    private fun summarizePrizes(
        results: List<Kl8TicketPrizeResult>,
        float10Amount: Int?
    ): Kl8PrizeSummary {
        val byHit = results.groupingBy { it.hitCount }.eachCount()
        var fixedTotal = 0
        var floatCount = 0
        var floatTotal = 0

        results.forEach { result ->
            if (result.hitCount == 10) {
                floatCount += 1
                result.prize?.let { floatTotal += it }
            } else if ((result.prize ?: 0) > 0) {
                fixedTotal += result.prize ?: 0
            }
        }

        val breakdown = byHit.keys
            .filter { prizeRules.containsKey(it) }
            .sortedDescending()
            .map { hitCount ->
                val count = byHit[hitCount] ?: 0
                val rule = prizeRules[hitCount]!!
                when (rule) {
                    is PrizeRule.Fixed -> {
                        Kl8PrizeBreakdownItem(
                            hitCount = hitCount,
                            count = count,
                            unitLabel = formatMoney(rule.amount),
                            subtotal = rule.amount * count
                        )
                    }
                    is PrizeRule.FloatPrize -> {
                        Kl8PrizeBreakdownItem(
                            hitCount = hitCount,
                            count = count,
                            unitLabel = if (float10Amount != null) formatMoney(float10Amount) else "浮动奖",
                            subtotal = float10Amount?.let { it * count }
                        )
                    }
                }
            }

        val total = fixedTotal + floatTotal
        val hasUnknownFloat = floatCount > 0 && float10Amount == null

        return Kl8PrizeSummary(
            fixedTotal = fixedTotal,
            floatCount = floatCount,
            floatTotal = floatTotal,
            total = total,
            breakdown = breakdown,
            hasUnknownFloat = hasUnknownFloat
        )
    }

    fun checkTickets(
        winning: List<Int>,
        tickets: List<List<Int>>,
        float10Amount: Int?
    ): Kl8CheckResult {
        val winningSet = winning.toSet()
        val ticketResults = tickets.map { nums ->
            val hitCount = nums.count { it in winningSet }
            getTicketPrize(hitCount, float10Amount)
        }
        return Kl8CheckResult(
            winning = winning.sorted(),
            tickets = tickets,
            ticketResults = ticketResults,
            summary = summarizePrizes(ticketResults, float10Amount)
        )
    }
}
