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

    private const val KL8_POOL_MIN = 1
    private const val KL8_POOL_MAX = 80
    private const val KL8_PICK_COUNT = 10
    private const val KL8_GROUP_COUNT = 8
    private const val KL8_SHUFFLE_TIMES_DEFAULT = 3
    private const val KL8_DEFAULT_BET_COUNT = 20
    private const val KL8_MIN_BET_COUNT = 1
    private const val KL8_MAX_OUTER_ROUNDS = 12
    private const val KL8_MAX_CANDIDATE_TRIES_PER_BET = 800
    private const val KL8_MAX_OVERLAP = 2

    data class Kl8GenerationResult(
        val requestedCount: Int,
        val generatedCount: Int,
        val bets: List<List<Int>>,
        val completed: Boolean,
        val reason: String? = null
    )

    /** Bitmask for numbers 1..80 (bit 0 unused). */
    private data class Kl8Mask(val lo: Long, val hi: Long) {
        fun overlapCount(other: Kl8Mask): Int {
            return java.lang.Long.bitCount(lo and other.lo) +
                java.lang.Long.bitCount(hi and other.hi)
        }
    }

    private fun kl8MaskOf(numbers: List<Int>): Kl8Mask {
        var lo = 0L
        var hi = 0L
        for (n in numbers) {
            when {
                n in 1..63 -> lo = lo or (1L shl n)
                n in 64..80 -> hi = hi or (1L shl (n - 64))
            }
        }
        return Kl8Mask(lo, hi)
    }

    private fun shuffleKl8Pool(shuffleTimes: Int, random: Random = Random.Default): List<Int> {
        val pool = (KL8_POOL_MIN..KL8_POOL_MAX).toMutableList()
        repeat(shuffleTimes) { pool.shuffle(random) }
        return pool
    }

    internal fun createKl8BaseGroups(
        pool: List<Int>,
        groupCount: Int = KL8_GROUP_COUNT,
        pickCount: Int = KL8_PICK_COUNT
    ): List<List<Int>> {
        val expected = groupCount * pickCount
        require(pool.size == expected) { "号码池长度应为 $expected，实际为 ${pool.size}" }
        return (0 until groupCount).map { i ->
            pool.subList(i * pickCount, (i + 1) * pickCount).toList()
        }
    }

    internal fun getKl8IntersectionCount(first: List<Int>, second: List<Int>): Int {
        return kl8MaskOf(first).overlapCount(kl8MaskOf(second))
    }

    private fun isKl8Compatible(candidate: Kl8Mask, existing: List<Kl8Mask>): Boolean {
        for (mask in existing) {
            if (candidate.overlapCount(mask) > KL8_MAX_OVERLAP) return false
        }
        return true
    }

    private fun hasKl8GlobalOverlapViolation(bets: List<List<Int>>): Boolean {
        val masks = bets.map { kl8MaskOf(it) }
        for (i in 0 until masks.size - 1) {
            for (j in i + 1 until masks.size) {
                if (masks[i].overlapCount(masks[j]) > KL8_MAX_OVERLAP) return true
            }
        }
        return false
    }

    /**
     * Build one bet by taking 2 numbers from two base groups and 1 from each of the rest
     * (8 groups → 2+2+1*6 = 10). Prefer under-used numbers for balance.
     */
    private fun generateKl8GroupCandidate(
        baseGroups: List<List<Int>>,
        usage: IntArray,
        random: Random
    ): List<Int> {
        val doubleGroups = (0 until KL8_GROUP_COUNT).shuffled(random).take(2).toSet()
        val picked = ArrayList<Int>(KL8_PICK_COUNT)
        baseGroups.forEachIndexed { index, group ->
            val need = if (index in doubleGroups) 2 else 1
            val preferred = group.sortedWith(
                compareBy<Int> { usage[it] }.thenBy { random.nextInt() }
            )
            picked.addAll(preferred.take(need))
        }
        return picked.sorted()
    }

    /** Weighted random 10-number sample; lower usage ⇒ higher weight. */
    private fun generateKl8WeightedCandidate(usage: IntArray, random: Random): List<Int> {
        val weights = DoubleArray(KL8_POOL_MAX + 1)
        var total = 0.0
        for (n in KL8_POOL_MIN..KL8_POOL_MAX) {
            val w = 1.0 / (1.0 + usage[n])
            weights[n] = w
            total += w
        }
        val chosen = LinkedHashSet<Int>(KL8_PICK_COUNT)
        var guard = 0
        while (chosen.size < KL8_PICK_COUNT && guard < 200) {
            guard++
            var r = random.nextDouble() * total
            var picked = KL8_POOL_MIN
            for (n in KL8_POOL_MIN..KL8_POOL_MAX) {
                r -= weights[n]
                if (r <= 0.0) {
                    picked = n
                    break
                }
            }
            if (chosen.add(picked)) {
                total -= weights[picked]
                weights[picked] = 0.0
            }
        }
        // Fallback if floating error left us short.
        if (chosen.size < KL8_PICK_COUNT) {
            for (n in (KL8_POOL_MIN..KL8_POOL_MAX).shuffled(random)) {
                chosen.add(n)
                if (chosen.size == KL8_PICK_COUNT) break
            }
        }
        return chosen.sorted()
    }

    private fun registerKl8Usage(bet: List<Int>, usage: IntArray) {
        for (n in bet) usage[n]++
    }

    /**
     * 快乐8选十低重复选号：
     * - 前 8 注：1–80 洗牌切分为 8 组（两两不重复）
     * - 后续注：分组构造 + 加权随机采样，用 bitset 校验任意两注最多重复 2 个号码
     */
    fun generateKl8Pick10Detailed(
        count: Int = KL8_DEFAULT_BET_COUNT,
        shuffleTimes: Int = KL8_SHUFFLE_TIMES_DEFAULT
    ): Kl8GenerationResult {
        require(count >= KL8_MIN_BET_COUNT) { "生成注数必须大于等于 1" }
        require(shuffleTimes > 0) { "打乱次数必须大于 0" }

        var bestBets = emptyList<List<Int>>()
        val random = Random.Default

        repeat(KL8_MAX_OUTER_ROUNDS) {
            val baseGroups = createKl8BaseGroups(shuffleKl8Pool(shuffleTimes, random))
            if (count <= KL8_GROUP_COUNT) {
                val bets = baseGroups.take(count).map { it.sorted() }
                return Kl8GenerationResult(count, bets.size, bets, completed = true)
            }

            val bets = baseGroups.map { it.sorted() }.toMutableList()
            val masks = bets.map { kl8MaskOf(it) }.toMutableList()
            val usage = IntArray(KL8_POOL_MAX + 1)
            bets.forEach { registerKl8Usage(it, usage) }

            var stuck = 0
            while (bets.size < count) {
                var accepted: List<Int>? = null
                for (tryIndex in 0 until KL8_MAX_CANDIDATE_TRIES_PER_BET) {
                    val candidate = if (tryIndex % 2 == 0) {
                        generateKl8GroupCandidate(baseGroups, usage, random)
                    } else {
                        generateKl8WeightedCandidate(usage, random)
                    }
                    if (candidate.size != KL8_PICK_COUNT || candidate.toSet().size != KL8_PICK_COUNT) {
                        continue
                    }
                    val mask = kl8MaskOf(candidate)
                    if (isKl8Compatible(mask, masks)) {
                        accepted = candidate
                        break
                    }
                }

                val chosen = accepted
                if (chosen != null) {
                    bets.add(chosen)
                    masks.add(kl8MaskOf(chosen))
                    registerKl8Usage(chosen, usage)
                    stuck = 0
                } else {
                    stuck++
                    // Light backtrack: drop a few recent remix bets and retry.
                    if (stuck <= 3 && bets.size > KL8_GROUP_COUNT) {
                        val removeCount = minOf(3, bets.size - KL8_GROUP_COUNT)
                        repeat(removeCount) {
                            val removed = bets.removeAt(bets.lastIndex)
                            masks.removeAt(masks.lastIndex)
                            for (n in removed) usage[n]--
                        }
                        stuck = 0
                    } else {
                        break
                    }
                }
            }

            if (bets.size > bestBets.size) bestBets = bets.toList()
            if (bets.size == count && !hasKl8GlobalOverlapViolation(bets)) {
                return Kl8GenerationResult(count, bets.size, bets, completed = true)
            }
        }

        return Kl8GenerationResult(
            requestedCount = count,
            generatedCount = bestBets.size,
            bets = bestBets,
            completed = false,
            reason = "无法在当前搜索限制内继续生成满足任意两注最多重复2个号码的组合"
        )
    }

    fun generateKl8Pick10Matrix(
        count: Int = KL8_DEFAULT_BET_COUNT,
        shuffleTimes: Int = KL8_SHUFFLE_TIMES_DEFAULT
    ): List<List<Int>> {
        val result = generateKl8Pick10Detailed(count, shuffleTimes)
        if (!result.completed) {
            throw IllegalStateException(result.reason)
        }
        return result.bets
    }
} 