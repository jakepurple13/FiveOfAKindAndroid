package com.programmersbox.fiveofakind

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Arrays
import kotlin.math.max

enum class HandType(val isSmall: Boolean) {
    Ones(true),
    Twos(true),
    Threes(true),
    Fours(true),
    Fives(true),
    Sixes(true),
    ThreeOfAKind(false),
    FourOfAKind(false),
    FullHouse(false),
    SmallStraight(false),
    LargeStraight(false),
    Yahtzee(false) {
//        override fun canGet(dice: Collection<Dice>): Boolean {
//            return 5 in dice.groupingBy { it.value }.eachCount().values
//        }
//
//        override fun get(dice: Collection<Dice>): Int {
//            
//        }
    },
    Chance(false) {
        //override fun canGet(dice: Collection<Dice>): Boolean = true
        //override fun get(dice: Collection<Dice>): Int = dice.sumOf { it.value }
    };

    //abstract fun canGet(dice: Collection<Dice>): Boolean
    //abstract operator fun get(dice: Collection<Dice>): Int

    private fun longestSequence(a: Array<Dice>): Int {
        Arrays.sort(a, compareBy { it.value })
        var longest = 0
        var sequence = 0
        for (i in 1 until a.size) {
            when (a[i].value - a[i - 1].value) {
                0 -> Unit/*ignore duplicates*/
                1 -> sequence += 1
                else -> if (sequence > longest) {
                    longest = sequence
                    sequence = 0
                }
            }
        }
        return max(longest, sequence)
    }
}

class YahtzeeScores {
    val scoreList = mutableStateMapOf<HandType, Int>()

    val smallScore by derivedStateOf {
        scoreList
            .filter { it.key.isSmall }
            .values
            .sum()
    }

    val hasBonus by derivedStateOf { smallScore >= 63 }

    val largeScore by derivedStateOf {
        scoreList
            .filterNot { it.key.isSmall }
            .values
            .sum()
    }

    val totalScore by derivedStateOf {
        smallScore + largeScore + if (hasBonus) 35 else 0
    }

    private val placedThreeOfKind by derivedStateOf { scoreList.containsKey(HandType.ThreeOfAKind) }
    private val placedFourOfKind by derivedStateOf { scoreList.containsKey(HandType.FourOfAKind) }
    private val placedFullHouse by derivedStateOf { scoreList.containsKey(HandType.FullHouse) }
    private val placedSmallStraight by derivedStateOf { scoreList.containsKey(HandType.SmallStraight) }
    private val placedLargeStraight by derivedStateOf { scoreList.containsKey(HandType.LargeStraight) }
    private val placedYahtzee by derivedStateOf { scoreList.containsKey(HandType.Yahtzee) }
    private val placedChance by derivedStateOf { scoreList.containsKey(HandType.Chance) }

    private val placedOnes by derivedStateOf { scoreList.containsKey(HandType.Ones) }
    private val placedTwos by derivedStateOf { scoreList.containsKey(HandType.Twos) }
    private val placedThrees by derivedStateOf { scoreList.containsKey(HandType.Threes) }
    private val placedFours by derivedStateOf { scoreList.containsKey(HandType.Fours) }
    private val placedFives by derivedStateOf { scoreList.containsKey(HandType.Fives) }
    private val placedSixes by derivedStateOf { scoreList.containsKey(HandType.Sixes) }

    val isGameOver by derivedStateOf {
        placedYahtzee && placedChance &&
                placedLargeStraight && placedSmallStraight &&
                placedFullHouse &&
                placedFourOfKind && placedThreeOfKind &&
                placedOnes && placedTwos && placedThrees && placedFours && placedFives && placedSixes
    }

    private fun getSmallNum(dice: Collection<Dice>, num: Int): Int = dice.filter { it.value == num }.sumOf { it.value }

    fun getSmall(dice: Collection<Dice>, type: HandType) = getSmallNum(dice, type.ordinal + 1).apply {
        scoreList[type] = this
    }

    fun canGetThreeKind(dice: Collection<Dice>): Boolean {
        val values = dice.groupingBy { it.value }.eachCount().values
        return 3 in values || 4 in values || 5 in values
    }

    fun getThreeOfAKind(dice: Collection<Dice>): Int = if (canGetThreeKind(dice)) {
        dice.sumOf { it.value }
    } else {
        0
    }.apply {
        scoreList[HandType.ThreeOfAKind] = this
    }

    fun canGetFourKind(dice: Collection<Dice>): Boolean {
        val values = dice.groupingBy { it.value }.eachCount().values
        return 4 in values || 5 in values
    }

    fun getFourOfAKind(dice: Collection<Dice>): Int = if (canGetFourKind(dice)) {
        dice.sumOf { it.value }
    } else {
        0
    }.apply {
        scoreList[HandType.FourOfAKind] = this
    }

    fun canGetYahtzee(dice: Collection<Dice>): Boolean = 5 in dice.groupingBy { it.value }.eachCount().values

    fun getYahtzee(dice: Collection<Dice>): Int {
        val num = if (canGetYahtzee(dice)) {
            if (scoreList.containsKey(HandType.Yahtzee)) 100 else 50
        } else 0
        return num.apply {
            scoreList[HandType.Yahtzee] = scoreList.getOrDefault(HandType.Yahtzee, 0) + this
        }
    }

    fun canGetFullHouse(dice: Collection<Dice>): Boolean {
        val values = dice.groupingBy { it.value }.eachCount().values
        return 3 in values && 2 in values
    }

    fun getFullHouse(dice: Collection<Dice>): Int = (if (canGetFullHouse(dice)) 25 else 0).apply {
        scoreList[HandType.FullHouse] = this
    }

    fun canGetLargeStraight(dice: Collection<Dice>): Boolean {
        val filteredDice = dice.sortedBy { it.value }
        return longestSequence(filteredDice.toTypedArray()) == 4
    }

    fun getLargeStraight(dice: Collection<Dice>): Int = (if (canGetLargeStraight(dice)) 40 else 0).apply {
        scoreList[HandType.LargeStraight] = this
    }

    fun canGetSmallStraight(dice: Collection<Dice>): Boolean {
        val filteredDice = dice.sortedBy { it.value }
        return longestSequence(filteredDice.toTypedArray()) in 3..4
    }

    fun getSmallStraight(dice: Collection<Dice>): Int = (if (canGetSmallStraight(dice)) 30 else 0).apply {
        scoreList[HandType.SmallStraight] = this
    }

    fun getChance(dice: Collection<Dice>): Int = dice.sumOf { it.value }.apply {
        scoreList[HandType.Chance] = this
    }

    private fun longestSequence(a: Array<Dice>): Int {
        Arrays.sort(a, compareBy { it.value })
        var longest = 0
        var sequence = 0
        for (i in 1 until a.size) {
            when (a[i].value - a[i - 1].value) {
                0 -> Unit/*ignore duplicates*/
                1 -> sequence += 1
                else -> if (sequence > longest) {
                    longest = sequence
                    sequence = 0
                }
            }
        }
        return max(longest, sequence)
    }

    fun resetScores() {
        scoreList.clear()
    }
}