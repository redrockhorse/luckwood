package com.example.luckwood

// 简单的数据管理器，用于在列表页和详情页之间共享数据
object MatchDataManager {
    private var matchesData: List<MatchData> = emptyList()
    
    fun setMatches(matches: List<MatchData>) {
        matchesData = matches
    }
    
    fun getMatch(index: Int): MatchData? {
        return matchesData.getOrNull(index)
    }
    
    fun getAllMatches(): List<MatchData> {
        return matchesData
    }
}

