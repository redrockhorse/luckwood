package com.example.luckwood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Kl8GenerationTest {

    @Test
    fun generateOneBet_returnsSingleValidBet() {
        val result = LotteryPredictor.generateKl8Pick10Detailed(count = 1)

        assertTrue(result.completed)
        assertEquals(1, result.generatedCount)
        assertBetShape(result.bets)
    }

    @Test
    fun generateFiveBets_firstFiveBetsHaveNoOverlap() {
        val result = LotteryPredictor.generateKl8Pick10Detailed(count = 5)

        assertTrue(result.completed)
        assertEquals(5, result.generatedCount)
        assertBetShape(result.bets)
        assertMaxPairwiseOverlap(result.bets, expectedMax = 0)
    }

    @Test
    fun generateEightBets_coverAllNumbersExactlyOnce() {
        val result = LotteryPredictor.generateKl8Pick10Detailed(count = 8)

        assertTrue(result.completed)
        assertEquals(8, result.generatedCount)
        assertBetShape(result.bets)
        assertMaxPairwiseOverlap(result.bets, expectedMax = 0)

        val allNumbers = result.bets.flatten().sorted()
        assertEquals((1..80).toList(), allNumbers)
    }

    @Test
    fun generateTwentyBets_respectsGlobalOverlapConstraint() {
        val result = LotteryPredictor.generateKl8Pick10Detailed(count = 20)

        assertTrue(result.reason ?: "20 注生成失败", result.completed)
        assertEquals(20, result.generatedCount)
        assertBetShape(result.bets)
        assertMaxPairwiseOverlap(result.bets, expectedMax = 2)
    }

    @Test
    fun generateFiftyBets_respectsGlobalOverlapConstraint() {
        val result = LotteryPredictor.generateKl8Pick10Detailed(count = 50)

        assertTrue(result.reason ?: "50 注生成失败", result.completed)
        assertEquals(50, result.generatedCount)
        assertBetShape(result.bets)
        assertMaxPairwiseOverlap(result.bets, expectedMax = 2)
    }

    @Test
    fun repeatedTwentyBetGeneration_alwaysRespectsConstraint() {
        repeat(100) {
            val result = LotteryPredictor.generateKl8Pick10Detailed(count = 20)
            assertTrue(result.reason ?: "第 ${it + 1} 次 20 注生成失败", result.completed)
            assertEquals(20, result.generatedCount)
            assertMaxPairwiseOverlap(result.bets, expectedMax = 2)
        }
    }

    @Test
    fun repeatedGeneration_isNotAlwaysIdentical() {
        val first = LotteryPredictor.generateKl8Pick10Detailed(count = 20)
        val second = LotteryPredictor.generateKl8Pick10Detailed(count = 20)

        assertTrue(first.completed)
        assertTrue(second.completed)
        assertNotEquals(first.bets, second.bets)
    }

    private fun assertBetShape(bets: List<List<Int>>) {
        bets.forEach { bet ->
            assertEquals(10, bet.size)
            assertEquals(10, bet.toSet().size)
            assertTrue(bet.all { it in 1..80 })
            assertEquals(bet.sorted(), bet)
        }
    }

    private fun assertMaxPairwiseOverlap(bets: List<List<Int>>, expectedMax: Int) {
        for (i in bets.indices) {
            for (j in i + 1 until bets.size) {
                val overlap = bets[i].toSet().intersect(bets[j].toSet()).size
                assertTrue(
                    "第 ${i + 1} 注和第 ${j + 1} 注重复了 $overlap 个号码，超过允许值 $expectedMax",
                    overlap <= expectedMax
                )
            }
        }
    }
}
