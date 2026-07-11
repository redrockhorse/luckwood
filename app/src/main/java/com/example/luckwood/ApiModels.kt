package com.example.luckwood

import com.google.gson.annotations.SerializedName

// API请求数据类
data class MatchRequest(
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String
)

// API响应数据类
data class ApiResponse(
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String,
    @SerializedName("total_matches")
    val totalMatches: Int,
    @SerializedName("successful_analysis")
    val successfulAnalysis: Int,
    @SerializedName("matches")
    val matches: List<MatchData>
)

// 比赛数据
data class MatchData(
    @SerializedName("match_info")
    val matchInfo: MatchInfo,
    @SerializedName("summary")
    val summary: Summary?,
    @SerializedName("data_quality")
    val dataQuality: DataQuality?,
    @SerializedName("best_recommendation")
    val bestRecommendation: BestRecommendation?,
    @SerializedName("home_matches")
    val homeMatches: List<HistoricalMatch>?,
    @SerializedName("away_matches")
    val awayMatches: List<HistoricalMatch>?,
    @SerializedName("home_analysis")
    val homeAnalysis: TeamAnalysis?,
    @SerializedName("away_analysis")
    val awayAnalysis: TeamAnalysis?
)

// 比赛信息
data class MatchInfo(
    @SerializedName("stime")
    val stime: String,
    @SerializedName("hname")
    val hname: String,
    @SerializedName("gname")
    val gname: String,
    @SerializedName("win")
    val win: Double,
    @SerializedName("draw")
    val draw: Double,
    @SerializedName("lost")
    val lost: Double,
    @SerializedName("round")
    val round: String,
    @SerializedName("season")
    val season: String,
    @SerializedName("league")
    val league: String
)

// 摘要信息
data class Summary(
    @SerializedName("home_team")
    val homeTeam: String,
    @SerializedName("away_team")
    val awayTeam: String,
    @SerializedName("win_odds_range")
    val winOddsRange: String,
    @SerializedName("lost_odds_range")
    val lostOddsRange: String,
    @SerializedName("home_matches_count")
    val homeMatchesCount: Int,
    @SerializedName("away_matches_count")
    val awayMatchesCount: Int
)

// 数据质量
data class DataQuality(
    @SerializedName("level")
    val level: String,
    @SerializedName("confidence")
    val confidence: Double,
    @SerializedName("message")
    val message: String
)

// 最佳推荐
data class BestRecommendation(
    @SerializedName("match_time")
    val matchTime: String,
    @SerializedName("home_team")
    val homeTeam: String,
    @SerializedName("away_team")
    val awayTeam: String,
    @SerializedName("outcome")
    val outcome: String,
    @SerializedName("odds")
    val odds: Double,
    @SerializedName("probability")
    val probability: Double,
    @SerializedName("expected_return")
    val expectedReturn: Double
)

// 历史比赛
data class HistoricalMatch(
    @SerializedName("stime")
    val stime: String,
    @SerializedName("hscore")
    val hscore: Int?,
    @SerializedName("gscore")
    val gscore: Int?,
    @SerializedName("hname")
    val hname: String,
    @SerializedName("gname")
    val gname: String,
    @SerializedName("win")
    val win: Double,
    @SerializedName("draw")
    val draw: Double,
    @SerializedName("lost")
    val lost: Double,
    @SerializedName("round")
    val round: String,
    @SerializedName("season")
    val season: String,
    @SerializedName("league")
    val league: String,
    @SerializedName("is_same_opponent")
    val isSameOpponent: Boolean
)

// 球队分析
data class TeamAnalysis(
    @SerializedName("team_name")
    val teamName: String,
    @SerializedName("total_matches")
    val totalMatches: Int,
    @SerializedName("wins")
    val wins: Int,
    @SerializedName("draws")
    val draws: Int,
    @SerializedName("losses")
    val losses: Int,
    @SerializedName("win_prob")
    val winProb: Double,
    @SerializedName("draw_prob")
    val drawProb: Double,
    @SerializedName("loss_prob")
    val lossProb: Double,
    @SerializedName("win_expected")
    val winExpected: Double,
    @SerializedName("draw_expected")
    val drawExpected: Double,
    @SerializedName("loss_expected")
    val lossExpected: Double
)

// 幸运选号 - 双色球结果
data class SSQResult(
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>,
    @SerializedName("back_number")
    val backNumber: Int,
    @SerializedName("front_missing")
    val frontMissing: List<Int>,
    @SerializedName("back_missing")
    val backMissing: Int
)

// 幸运选号 - 大乐透结果
data class DLTResult(
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>,
    @SerializedName("front_missing")
    val frontMissing: List<Int>,
    @SerializedName("back_missing")
    val backMissing: List<Int>
)

// 幸运选号 - 双色球响应
data class SSQResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("results")
    val results: List<SSQResult>
)

// 幸运选号 - 大乐透响应
data class DLTResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("results")
    val results: List<DLTResult>
)

// 最新一期开奖 - 双色球
data class SSQLastDrawResponse(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("issue_date")
    val issueDate: String,
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>,
    @SerializedName("back_number")
    val backNumber: Int
)

// 最新一期开奖 - 大乐透
data class DLTLastDrawResponse(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("issue_date")
    val issueDate: String,
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>
)

// 最新一期开奖 - 快乐8
data class KL8LastDrawResponse(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("issue_date")
    val issueDate: String,
    @SerializedName("numbers")
    val numbers: List<Int>,
    @SerializedName("numbers_ordered")
    val numbersOrdered: List<Int>? = null
)

// --- 保存号码 ---

data class PickItemRequest(
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>? = null,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>? = null,
    @SerializedName("numbers")
    val numbers: List<Int>? = null,
    @SerializedName("play_type")
    val playType: Int? = null,
    @SerializedName("bet_amount")
    val betAmount: Double? = null,
    @SerializedName("multiplier")
    val multiplier: Int? = null
)

data class SavePicksRequest(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("issue_code")
    val issueCode: String,
    @SerializedName("source")
    val source: String = "generate",
    @SerializedName("picks")
    val picks: List<PickItemRequest>
)

data class LotteryPick(
    @SerializedName("id")
    val id: Int,
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("issue_code")
    val issueCode: String,
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>? = null,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>? = null,
    @SerializedName("numbers")
    val numbers: List<Int>? = null,
    @SerializedName("play_type")
    val playType: Int? = null,
    @SerializedName("bet_amount")
    val betAmount: Double? = null,
    @SerializedName("multiplier")
    val multiplier: Int? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("batch_id")
    val batchId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class SavePicksResponse(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("issue_code")
    val issueCode: String,
    @SerializedName("batch_id")
    val batchId: String? = null,
    @SerializedName("count")
    val count: Int,
    @SerializedName("picks")
    val picks: List<LotteryPick>
)

data class PicksListResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("picks")
    val picks: List<LotteryPick>
)

data class DeletePickResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("id")
    val id: Int
)

data class PickCheckDraw(
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("issue_date")
    val issueDate: String? = null,
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>? = null,
    @SerializedName("back_number")
    val backNumber: Int? = null,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>? = null,
    @SerializedName("numbers")
    val numbers: List<Int>? = null
)

data class PickCheckResultItem(
    @SerializedName("pick_id")
    val pickId: Int? = null,
    @SerializedName("front_numbers")
    val frontNumbers: List<Int>? = null,
    @SerializedName("back_numbers")
    val backNumbers: List<Int>? = null,
    @SerializedName("numbers")
    val numbers: List<Int>? = null,
    @SerializedName("front_matched")
    val frontMatched: List<Int>? = null,
    @SerializedName("front_match_count")
    val frontMatchCount: Int? = null,
    @SerializedName("back_matched")
    val backMatched: List<Int>? = null,
    @SerializedName("back_match_count")
    val backMatchCount: Int? = null,
    @SerializedName("matched")
    val matched: List<Int>? = null,
    @SerializedName("match_count")
    val matchCount: Int? = null,
    @SerializedName("back_hit")
    val backHit: Boolean? = null,
    @SerializedName("prize_level")
    val prizeLevel: Int? = null,
    @SerializedName("prize_name")
    val prizeName: String? = null,
    @SerializedName("prize_amount")
    val prizeAmount: Double? = null,
    @SerializedName("total_prize")
    val totalPrize: Double? = null,
    @SerializedName("bet_amount")
    val betAmount: Double? = null,
    @SerializedName("multiplier")
    val multiplier: Int? = null,
    @SerializedName("play_type")
    val playType: Int? = null
)

data class PickCheckSummary(
    @SerializedName("total_picks")
    val totalPicks: Int? = null,
    @SerializedName("winning_picks")
    val winningPicks: Int? = null,
    @SerializedName("total_bet")
    val totalBet: Double? = null,
    @SerializedName("total_prize")
    val totalPrize: Double? = null
)

data class PickCheckResponse(
    @SerializedName("lottery_type")
    val lotteryType: String,
    @SerializedName("issue_code")
    val issueCode: String,
    @SerializedName("draw")
    val draw: PickCheckDraw,
    @SerializedName("results")
    val results: List<PickCheckResultItem>,
    @SerializedName("summary")
    val summary: PickCheckSummary? = null
)
