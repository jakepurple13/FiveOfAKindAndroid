package com.programmersbox.fiveofakind

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextInt

suspend fun randomNumberAnimation(
    newValue: Int,
    valueChange: (Int) -> Unit,
    delayAmountMs: Long = 50L,
    randomCount: Int = 5
) {
    repeat(randomCount) {
        delay(delayAmountMs)
        valueChange(Random.nextInt(1..6))
    }
    valueChange(newValue)
}

@Composable
fun <T> randomItemAnimation(
    initialValue: T,
    newValue: () -> T,
    randomValue: (Int) -> T,
    randomCount: Int = 5,
    delayAmountMs: Long = 50L,
    vararg keys: Any
) = produceState(initialValue = initialValue, keys = keys) {
    val endValue = newValue()
    repeat(randomCount) {
        delay(delayAmountMs)
        value = randomValue(it)
    }
    value = endValue
}

@Composable
fun randomNumberAnimation(
    initialValue: Int,
    newValue: Int,
    randomCount: Int = 5,
    delayAmountMs: Long = 50L,
    vararg keys: Any
) = randomItemAnimation(
    initialValue = initialValue,
    newValue = { newValue },
    randomValue = { Random.nextInt(1..6) },
    randomCount = randomCount,
    delayAmountMs = delayAmountMs,
    keys = keys
)