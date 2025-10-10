package com.example.luckwood

import kotlin.random.Random

data class LotteryPrediction(
    val redBalls: List<Int>,
    val blueBall: Int
)

object LotteryPredictor {
    
    /**
     * 生成5个1-16之间不重复的蓝球号码
     */
    private fun generateBlueBalls(): List<Int> {
        val balls = (1..16).toMutableList()
        balls.shuffle()
        return balls.take(5)
    }
    
    /**
     * 处理双色球号码预测算法
     * @param lastNumbers 最近一期的6个号码列表
     * @return 5组预测号码，每组包含红球和蓝球
     */
    fun processDoubleColorBall(lastNumbers: List<Int>): List<LotteryPrediction> {
        // 验证输入
        if (lastNumbers.size != 6) {
            throw IllegalArgumentException("输入必须是包含6个号码的列表")
        }
        
        // 创建1-33的完整号码列表（数组B）
        val allNumbers = (1..33).toList()
        
        // 从B中去掉A的元素得到数组C
        val remainingNumbers = allNumbers.filter { it !in lastNumbers }.toMutableList()
        
        // 随机打乱剩余号码
        remainingNumbers.shuffle()
        
        // 提取前18个数，分成3组D,E,F
        val D = remainingNumbers.take(6).toMutableList()
        val E = remainingNumbers.drop(6).take(6).toMutableList()
        val F = remainingNumbers.drop(12).take(6).toMutableList()
        
        // 剩下的元素作为数组G
        val G = remainingNumbers.drop(18).toMutableList()
        
        // 从A中随机取出3个数放到G中
        val tempLastNumbers = lastNumbers.toMutableList()
        tempLastNumbers.shuffle()
        G.addAll(tempLastNumbers.take(3))
        
        // 将G乱序后分成两个6个数一组的数组H,I
        G.shuffle()
        val H = G.take(6).toMutableList()
        val I = G.drop(6).take(6).toMutableList()
        
        // 对所有数组进行排序
        D.sort()
        E.sort()
        F.sort()
        H.sort()
        I.sort()
        
        // 为每组生成一个对应的蓝球号码
        val blueBalls = generateBlueBalls()
        
        return listOf(
            LotteryPrediction(D, blueBalls[0]),
            LotteryPrediction(E, blueBalls[1]),
            LotteryPrediction(F, blueBalls[2]),
            LotteryPrediction(H, blueBalls[3]),
            LotteryPrediction(I, blueBalls[4])
        )
    }
    
    /**
     * 处理大乐透号码预测算法
     * @param lastNumbers 最近一期的5个号码列表
     * @return 多组预测号码，每组包含5个红球和2个蓝球
     */
    fun processDaLeTou(lastNumbers: List<Int>): List<LotteryPrediction> {
        // 验证输入
        if (lastNumbers.size != 5) {
            throw IllegalArgumentException("大乐透输入必须是包含5个号码的列表")
        }
        
        // 创建1-35的完整号码列表
        val allNumbers = (1..35).toList()
        
        // 去掉最近一期的号码
        val remainingNumbers = allNumbers.filter { it !in lastNumbers }.toMutableList()
        
        // 随机打乱剩余号码
        remainingNumbers.shuffle()
        
        // 将号码分组，每组5个，并排序
        val redBallGroups = mutableListOf<List<Int>>()
        for (i in remainingNumbers.indices step 5) {
            if (i + 5 <= remainingNumbers.size) {
                val combo = remainingNumbers.subList(i, i + 5).toMutableList()
                combo.sort() // 对每个组合进行排序
                redBallGroups.add(combo)
            }
        }
        
        // 为每一注生成两个蓝球特殊号码
        val blueBallCombinations = generateUniqueBlueBalls(redBallGroups.size)
        
        // 将红球和蓝球组合
        val predictions = mutableListOf<LotteryPrediction>()
        for (i in redBallGroups.indices) {
            val redBalls = redBallGroups[i]
            val blueBalls = blueBallCombinations[i]
            
            // 组合红球和蓝球
            val allBalls = redBalls + blueBalls
            predictions.add(LotteryPrediction(allBalls, blueBalls.last()))
        }
        
        return predictions
    }
    
    /**
     * 生成蓝球组合：将1-12的数组随机乱序后每次取两个
     * @param numCombinations 需要生成的组合数量
     * @return 包含多个蓝球组合的列表，每个组合包含2个号码
     */
    private fun generateUniqueBlueBalls(numCombinations: Int): List<List<Int>> {
        val result = mutableListOf<List<Int>>()
        
        // 创建1-12的数组
        val blueNumbers = (1..12).toMutableList()
        
        // 随机打乱数组
        blueNumbers.shuffle()
        
        // 每次取两个号码
        for (i in blueNumbers.indices step 2) {
            if (i + 1 < blueNumbers.size) {
                // 取两个号码并排序
                val combo = listOf(blueNumbers[i], blueNumbers[i + 1]).sorted()
                result.add(combo)
                
                // 如果已经生成了足够的组合，就停止
                if (result.size >= numCombinations) {
                    break
                }
            }
        }
        
        // 如果第一次打乱后不够，继续重新打乱并取号码
        while (result.size < numCombinations) {
            // 重新打乱1-12的数组
            blueNumbers.shuffle()
            
            for (i in blueNumbers.indices step 2) {
                if (i + 1 < blueNumbers.size) {
                    val combo = listOf(blueNumbers[i], blueNumbers[i + 1]).sorted()
                    result.add(combo)
                    
                    if (result.size >= numCombinations) {
                        break
                    }
                }
            }
        }
        
        return result.take(numCombinations)
    }
} 