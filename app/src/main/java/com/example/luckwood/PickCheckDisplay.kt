package com.example.luckwood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrizeHighlightColor = Color(0xFFE74C3C)

@Composable
private fun LotteryNumberBall(
    number: Int,
    isHit: Boolean,
    isBack: Boolean,
    size: androidx.compose.ui.unit.Dp = 30.dp
) {
    val filledColor = if (isBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val onFilledColor = if (isBack) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
    val modifier = if (isHit) {
        Modifier
            .size(size)
            .background(filledColor, CircleShape)
    } else {
        Modifier
            .size(size)
            .background(Color.White, CircleShape)
            .border(1.dp, filledColor, CircleShape)
    }
    val textColor = if (isHit) onFilledColor else filledColor
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = if (size <= 24.dp) number.toString() else "%02d".format(number),
            fontSize = if (size <= 24.dp) 11.sp else 13.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PickCheckResultSection(
    response: PickCheckResponse,
    float10Amount: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (LotteryPicksHelper.isKl8Type(response.lotteryType)) {
            val kl8Result = LotteryPicksHelper.toKl8CheckResult(response, float10Amount)
            if (kl8Result != null) {
                Kl8CheckResultSection(result = kl8Result)
            }
        } else {
            val isSsq = response.lotteryType.contains("双色") ||
                response.lotteryType.equals("ssq", ignoreCase = true)

            PickCheckSummaryCard(response)
            Text(
                text = "开奖号码 · ${response.issueCode}",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                response.draw.frontNumbers?.forEach { number ->
                    LotteryNumberBall(number = number, isHit = true, isBack = false)
                }
                if (isSsq && response.draw.backNumber != null) {
                    Text(text = "+", fontSize = 14.sp)
                    LotteryNumberBall(
                        number = response.draw.backNumber,
                        isHit = true,
                        isBack = true
                    )
                } else {
                    response.draw.backNumbers?.forEach { number ->
                        Text(text = "+", fontSize = 14.sp)
                        LotteryNumberBall(number = number, isHit = true, isBack = true)
                    }
                }
            }
            Text(
                text = "我的号码",
                fontSize = 14.sp,
                color = Color(0xFF888888)
            )
            response.results.forEachIndexed { index, result ->
                SsqDltCheckResultCard(index = index, result = result, isSsq = isSsq)
            }
        }
    }
}

@Composable
private fun Kl8CheckResultSection(result: Kl8CheckResult) {
    val summary = result.summary
    val winningSet = result.winning.toSet()
    val groups = result.tickets.chunked(Kl8PrizeChecker.GROUP_SIZE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("共中奖 ")
                    withStyle(SpanStyle(color = Kl8ErrorColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                        append(Kl8PrizeChecker.formatMoney(summary.total))
                    }
                },
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            if (summary.hasUnknownFloat) {
                Text(
                    text = "含 ${summary.floatCount} 注中10（浮动奖未计入，单注最高500万元）",
                    fontSize = 12.sp,
                    color = Kl8BallColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else if (summary.floatCount > 0) {
                Text(
                    text = "含 ${summary.floatCount} 注中10，浮动奖 ${Kl8PrizeChecker.formatMoney(summary.floatTotal)}",
                    fontSize = 12.sp,
                    color = Kl8BallColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            summary.breakdown.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "中 ${item.hitCount} 个 · ${item.count} 注 × ${item.unitLabel}",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.subtotal?.let { Kl8PrizeChecker.formatMoney(it) } ?: "待公布",
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color(0xFFEEEEEE)
            )
            Text(
                text = Kl8PrizeChecker.prizeRulesText,
                fontSize = 11.sp,
                color = Color(0xFFAAAAAA),
                lineHeight = 16.sp
            )
        }
    }

    Text(text = "彩果", fontSize = 14.sp, color = Color(0xFF888888))
    Kl8BallFlowRow(numbers = result.winning)
    Text(text = "我的号码", fontSize = 14.sp, color = Color(0xFF888888))

    groups.forEachIndexed { groupIndex, groupTickets ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F8F8))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "第 ${groupIndex + 1} 组 · ${groupTickets.size} 注",
                        fontSize = 13.sp,
                        color = Color(0xFF888888)
                    )
                }
                groupTickets.forEachIndexed { ticketIndex, numbers ->
                    val globalIndex = groupIndex * Kl8PrizeChecker.GROUP_SIZE + ticketIndex
                    val prizeResult = result.ticketResults[globalIndex]
                    val sorted = numbers.sorted()
                    val isNoPrize = prizeResult.label == "未中奖"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第 ${globalIndex + 1} 注",
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "中 ${prizeResult.hitCount} 个",
                                    fontSize = 12.sp,
                                    color = Kl8BallColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "  ${prizeResult.label}",
                                    fontSize = 12.sp,
                                    color = if (isNoPrize) Color(0xFFCCCCCC) else Kl8ErrorColor,
                                    fontWeight = if (isNoPrize) FontWeight.Normal else FontWeight.Medium
                                )
                            }
                        }
                        Kl8BallFlowRow(numbers = sorted) { number ->
                            if (number in winningSet) Kl8BallStyle.Hit else Kl8BallStyle.Miss
                        }
                    }
                    if (ticketIndex < groupTickets.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = Color(0xFFDDDDDD)
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.pickCheckResultItems(
    response: PickCheckResponse,
    float10Amount: Int? = null
) {
    item(key = "pick-check-section-${response.issueCode}") {
        PickCheckResultSection(response = response, float10Amount = float10Amount)
    }
}

@Composable
private fun PickCheckSummaryCard(response: PickCheckResponse) {
    val summary = response.summary
    val totalPrize = summary?.totalPrize
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("共中奖 ")
                    withStyle(
                        SpanStyle(
                            color = PrizeHighlightColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    ) {
                        append(LotteryPicksHelper.formatPrizeAmount(totalPrize))
                    }
                },
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            summary?.let {
                Text(
                    text = "${it.winningPicks ?: 0} 注中奖 · 投注 ${it.totalBet?.toInt() ?: 0} 元",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SsqDltCheckResultCard(
    index: Int,
    result: PickCheckResultItem,
    isSsq: Boolean
) {
    val frontMatched = result.frontMatched?.toSet() ?: emptySet()
    val backMatched = result.backMatched?.toSet() ?: emptySet()
    val frontNumbers = result.frontNumbers ?: emptyList()
    val backNumbers = result.backNumbers ?: emptyList()
    val frontHit = result.frontMatchCount ?: frontMatched.size
    val backHit = result.backMatchCount ?: backMatched.size
    val prizeLabel = when {
        !result.prizeName.isNullOrBlank() && result.prizeName != "未中奖" -> {
            result.prizeAmount?.let { LotteryPicksHelper.formatPrizeAmount(it) }
                ?: result.prizeName
        }
        (result.totalPrize ?: 0.0) > 0 -> LotteryPicksHelper.formatPrizeAmount(result.totalPrize)
        else -> "未中奖"
    }
    val isWinner = prizeLabel != "未中奖"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第 ${index + 1} 注",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSsq) {
                            "中 ${frontHit}+${if (result.backHit == true || backHit > 0) 1 else 0}"
                        } else {
                            "前${frontHit} 后${backHit}"
                        },
                        fontSize = 12.sp,
                        color = Kl8BallColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "  $prizeLabel",
                        fontSize = 12.sp,
                        color = if (isWinner) PrizeHighlightColor else Color(0xFFCCCCCC),
                        fontWeight = if (isWinner) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                frontNumbers.forEach { number ->
                    LotteryNumberBall(
                        number = number,
                        isHit = number in frontMatched,
                        isBack = false,
                        size = 26.dp
                    )
                }
                if (backNumbers.isNotEmpty()) {
                    Text(text = "+", fontSize = 12.sp)
                    backNumbers.forEach { number ->
                        LotteryNumberBall(
                            number = number,
                            isHit = number in backMatched || result.backHit == true,
                            isBack = true,
                            size = 26.dp
                        )
                    }
                }
            }
        }
    }
}
