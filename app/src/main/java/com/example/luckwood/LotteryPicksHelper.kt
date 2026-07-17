package com.example.luckwood

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.HttpException

object LotteryPicksHelper {
    private val gson = Gson()
    const val KL8_PLAY_TYPE = 10

    fun lotteryTypeCode(displayName: String): String = when (displayName) {
        "双色球" -> "ssq"
        "大乐透" -> "dlt"
        "快乐8" -> "kl8"
        else -> displayName
    }

    fun lotteryDisplayName(code: String): String = when (code.lowercase()) {
        "ssq", "双色球" -> "双色球"
        "dlt", "大乐透" -> "大乐透"
        "kl8", "快乐8" -> "快乐8"
        else -> code
    }

    fun isKl8Type(codeOrName: String): Boolean {
        val lower = codeOrName.lowercase()
        return lower == "kl8" || codeOrName == "快乐8"
    }

    fun nextIssueCode(lastCode: String): String {
        val numeric = lastCode.toLongOrNull()
        return if (numeric != null) (numeric + 1).toString() else lastCode
    }

    fun normalizeTypeCode(codeOrName: String): String = when (codeOrName.lowercase()) {
        "ssq", "双色球" -> "ssq"
        "dlt", "大乐透" -> "dlt"
        "kl8", "快乐8" -> "kl8"
        else -> codeOrName.lowercase()
    }

    /** 仅当该期已有开奖数据时可兑奖（issue_code <= 最新已开奖期号） */
    fun canCheckIssue(issueCode: String, lastDrawnIssueCode: String?): Boolean {
        if (lastDrawnIssueCode.isNullOrBlank()) return false
        val issueNum = issueCode.toLongOrNull()
        val lastNum = lastDrawnIssueCode.toLongOrNull()
        return if (issueNum != null && lastNum != null) {
            issueNum <= lastNum
        } else {
            issueCode <= lastDrawnIssueCode
        }
    }

    fun checkUnavailableReason(issueCode: String, lastDrawnIssueCode: String?): String? {
        if (lastDrawnIssueCode.isNullOrBlank()) {
            return "无法获取最新开奖期号"
        }
        if (canCheckIssue(issueCode, lastDrawnIssueCode)) return null
        return "期号 $issueCode 尚未开奖（最新已开 $lastDrawnIssueCode）"
    }

    fun contentKey(picks: List<PickItemRequest>): String {
        return picks.joinToString("|") { pick ->
            buildString {
                pick.frontNumbers?.let { append("f:").append(it.joinToString(",")) }
                pick.backNumbers?.let { append(" b:").append(it.joinToString(",")) }
                pick.numbers?.let { append("n:").append(it.joinToString(",")) }
                pick.playType?.let { append(" p:$it") }
            }
        }
    }

    fun contentKeyFromKl8Tickets(tickets: List<List<Int>>): String {
        return buildKl8Picks(tickets).let { contentKey(it) }
    }

    fun parseApiError(e: Exception): String {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                try {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    json.get("error")?.asString?.let { return it }
                } catch (_: Exception) {
                }
            }
            return when (e.code()) {
                404 -> "未找到对应数据（该期可能尚未开奖）"
                400 -> "请求参数无效"
                else -> "HTTP ${e.code()}"
            }
        }
        return e.message ?: "未知错误"
    }

    fun batchDisplayStatus(
        status: String?,
        canCheck: Boolean,
        hasCheckResult: Boolean
    ): String = when {
        hasCheckResult -> "已验奖"
        status == "settled" -> "已兑奖"
        status == "cancelled" -> "已取消"
        canCheck -> "待验奖"
        else -> statusLabel(status)
    }

    /** 同一批内按 id 升序，与兑奖接口返回的「我的号码」顺序一致 */
    fun sortPicksForDisplay(picks: List<LotteryPick>): List<LotteryPick> {
        return picks.sortedBy { it.id }
    }

    fun statusLabel(status: String?): String = when (status) {
        "pending" -> "待开奖"
        "settled" -> "已兑奖"
        "cancelled" -> "已取消"
        else -> status ?: "未知"
    }

    fun buildSsqPicks(results: List<SSQResult>): List<PickItemRequest> {
        return results.map { result ->
            PickItemRequest(
                frontNumbers = result.frontNumbers,
                backNumbers = listOf(result.backNumber)
            )
        }
    }

    fun buildDltPicks(results: List<DLTResult>): List<PickItemRequest> {
        return results.map { result ->
            PickItemRequest(
                frontNumbers = result.frontNumbers,
                backNumbers = result.backNumbers
            )
        }
    }

    fun buildSsqPicksFromPredictions(predictions: List<LotteryPrediction>): List<PickItemRequest> {
        return predictions.map { prediction ->
            PickItemRequest(
                frontNumbers = prediction.redBalls.take(6),
                backNumbers = listOf(prediction.blueBall)
            )
        }
    }

    fun buildDltPicksFromPredictions(predictions: List<LotteryPrediction>): List<PickItemRequest> {
        return predictions.map { prediction ->
            PickItemRequest(
                frontNumbers = prediction.redBalls.take(5),
                backNumbers = prediction.redBalls.drop(5).take(2)
            )
        }
    }

    fun buildKl8Picks(tickets: List<List<Int>>, playType: Int = KL8_PLAY_TYPE): List<PickItemRequest> {
        return tickets.map { numbers ->
            PickItemRequest(
                numbers = numbers.sorted(),
                playType = playType,
                betAmount = 2.0
            )
        }
    }

    fun picksToKl8TicketsText(picks: List<LotteryPick>): String {
        return picks.mapNotNull { pick ->
            pick.numbers?.let { numbers -> Kl8PrizeChecker.formatNumbers(numbers) }
        }.joinToString("\n")
    }

    fun formatPrizeAmount(amount: Double?): String {
        if (amount == null) return "待公布"
        return Kl8PrizeChecker.formatMoney(amount.toInt())
    }

    fun formatCreatedAt(createdAt: String?): String {
        if (createdAt.isNullOrBlank()) return ""
        return if (createdAt.length >= 16) {
            createdAt.substring(0, 16).replace('T', ' ')
        } else {
            createdAt
        }
    }

    data class PickBatch(
        val batchId: String?,
        val lotteryType: String,
        val issueCode: String,
        val picks: List<LotteryPick>,
        val createdAt: String?
    )

    fun groupPicksByBatch(picks: List<LotteryPick>): List<PickBatch> {
        return picks
            .groupBy { pick ->
                pick.batchId ?: "single-${pick.id}"
            }
            .map { (_, groupPicks) ->
                val first = groupPicks.first()
                PickBatch(
                    batchId = first.batchId,
                    lotteryType = first.lotteryType,
                    issueCode = first.issueCode,
                    picks = sortPicksForDisplay(groupPicks),
                    createdAt = groupPicks.maxByOrNull { it.createdAt ?: "" }?.createdAt
                )
            }
            .sortedByDescending { it.createdAt ?: "" }
    }

    fun toKl8CheckResult(response: PickCheckResponse, float10Amount: Int?): Kl8CheckResult? {
        if (!isKl8Type(response.lotteryType)) return null
        val winning = response.draw.numbers ?: return null
        val tickets = response.results.map { it.numbers ?: emptyList() }
        val ticketResults = response.results.map { item ->
            val hitCount = item.matchCount ?: item.frontMatchCount ?: 0
            val prizeAmount = item.prizeAmount?.toInt()
            val totalPrize = item.totalPrize?.toInt()
            val label = when {
                !item.prizeName.isNullOrBlank() && item.prizeName != "未中奖" -> {
                    when {
                        prizeAmount != null && prizeAmount > 0 -> Kl8PrizeChecker.formatMoney(prizeAmount)
                        hitCount == 10 && float10Amount != null -> Kl8PrizeChecker.formatMoney(float10Amount)
                        prizeAmount == null && hitCount == 10 -> "浮动奖（最高500万）"
                        else -> item.prizeName
                    }
                }
                hitCount == 10 -> {
                    float10Amount?.let { Kl8PrizeChecker.formatMoney(it) } ?: "浮动奖（最高500万）"
                }
                (totalPrize ?: 0) > 0 -> Kl8PrizeChecker.formatMoney(totalPrize!!)
                else -> "未中奖"
            }
            Kl8TicketPrizeResult(
                hitCount = hitCount,
                prize = totalPrize ?: prizeAmount,
                label = label
            )
        }

        val apiTotal = response.summary?.totalPrize?.toInt()
        if (apiTotal != null && apiTotal > 0) {
            val summary = Kl8PrizeSummary(
                fixedTotal = ticketResults.filter { it.hitCount != 10 }.sumOf { it.prize ?: 0 },
                floatCount = ticketResults.count { it.hitCount == 10 },
                floatTotal = ticketResults.filter { it.hitCount == 10 }.sumOf { it.prize ?: 0 },
                total = apiTotal,
                breakdown = buildKl8Breakdown(ticketResults),
                hasUnknownFloat = ticketResults.any { it.hitCount == 10 && it.prize == null }
            )
            return Kl8CheckResult(
                winning = winning.sorted(),
                tickets = tickets,
                ticketResults = ticketResults,
                summary = summary
            )
        }

        return Kl8PrizeChecker.checkTickets(winning, tickets, float10Amount)
    }

    private fun buildKl8Breakdown(
        ticketResults: List<Kl8TicketPrizeResult>
    ): List<Kl8PrizeBreakdownItem> {
        return ticketResults
            .groupingBy { it.hitCount }
            .eachCount()
            .keys
            .sortedDescending()
            .mapNotNull { hitCount ->
                val count = ticketResults.count { it.hitCount == hitCount }
                if (count == 0) return@mapNotNull null
                val sample = ticketResults.first { it.hitCount == hitCount }
                Kl8PrizeBreakdownItem(
                    hitCount = hitCount,
                    count = count,
                    unitLabel = sample.label,
                    subtotal = sample.prize?.let { it * count }
                )
            }
    }
}
