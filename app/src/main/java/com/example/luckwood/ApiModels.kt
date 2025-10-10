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

