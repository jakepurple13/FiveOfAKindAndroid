package com.programmersbox.fiveofakind

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.max
import kotlin.random.Random
import kotlin.random.nextInt

typealias ScoreClick = () -> Unit

private const val DOT_LOOK = "●"

val Emerald = Color(0xFF2ecc71)
val Sunflower = Color(0xFFf1c40f)
val Alizarin = Color(0xFFe74c3c)

enum class YahtzeeState { RollOne, RollTwo, RollThree, Stop }

class YahtzeeViewModel : ViewModel() {

    var rolling by mutableStateOf(false)

    var showGameOverDialog by mutableStateOf(true)

    var state by mutableStateOf(YahtzeeState.RollOne)

    var diceLook by mutableStateOf(true)

    val scores = YahtzeeScores()

    val hand = mutableStateListOf(
        Dice(0, location = "1"),
        Dice(0, location = "2"),
        Dice(0, location = "3"),
        Dice(0, location = "4"),
        Dice(0, location = "5")
    )

    val hold = mutableStateListOf<Dice>()

    fun reroll() {
        viewModelScope.launch {
            rolling = true
            (0 until hand.size).map { i ->
                async(Dispatchers.IO) {
                    if (hand[i] !in hold) {
                        hand[i].roll()
                    }
                }
            }.awaitAll()
            rolling = false
            state = when (state) {
                YahtzeeState.RollOne -> YahtzeeState.RollTwo
                YahtzeeState.RollTwo -> YahtzeeState.RollThree
                YahtzeeState.RollThree -> YahtzeeState.Stop
                YahtzeeState.Stop -> YahtzeeState.RollOne
            }
        }
    }

    fun placeOnes() {
        //scores.getOnes(hand)
        scores.getSmall(hand, HandType.Ones)
        reset()
    }

    fun placeTwos() {
        //scores.getTwos(hand)
        scores.getSmall(hand, HandType.Twos)
        reset()
    }

    fun placeThrees() {
        //scores.getThrees(hand)
        scores.getSmall(hand, HandType.Threes)
        reset()
    }

    fun placeFours() {
        //scores.getFours(hand)
        scores.getSmall(hand, HandType.Fours)
        reset()
    }

    fun placeFives() {
        //scores.getFives(hand)
        scores.getSmall(hand, HandType.Fives)
        reset()
    }

    fun placeSixes() {
        //scores.getSixes(hand)
        scores.getSmall(hand, HandType.Sixes)
        reset()
    }

    fun placeThreeOfKind() {
        scores.getThreeOfAKind(hand)
        reset()
    }

    fun placeFourOfKind() {
        scores.getFourOfAKind(hand)
        reset()
    }

    fun placeFullHouse() {
        scores.getFullHouse(hand)
        reset()
    }

    fun placeSmallStraight() {
        scores.getSmallStraight(hand)
        reset()
    }

    fun placeLargeStraight() {
        scores.getLargeStraight(hand)
        reset()
    }

    fun placeYahtzee() {
        scores.getYahtzee(hand)
        reset()
    }

    fun placeChance() {
        scores.getChance(hand)
        reset()
    }

    private fun reset() {
        hold.clear()
        hand.forEach { it.value = 0 }
        state = YahtzeeState.RollOne
    }

    fun resetGame() {
        reset()
        scores.resetScores()
        showGameOverDialog = true
    }

}

class Dice(value: Int = Random.nextInt(1..6), @Suppress("unused") val location: String) {
    var value by mutableIntStateOf(value)

    suspend fun roll(rollCount: Int = 5) {
        randomNumberAnimation(
            newValue = Random.nextInt(1..6),
            valueChange = { value = it },
            randomCount = rollCount
        )
    }

    @Composable
    fun ShowDice(
        useDots: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) = if (useDots) DiceDots(this, modifier, onClick) else Dice(this, modifier, onClick)

    @Composable
    operator fun invoke(
        useDots: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) = ShowDice(useDots, modifier, onClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YahtzeeScreen(vm: YahtzeeViewModel = viewModel()) {
    var newGameDialog by remember { mutableStateOf(false) }

    if (newGameDialog) {
        AlertDialog(
            onDismissRequest = { newGameDialog = false },
            title = { Text("Want to start a new game?") },
            text = { Text("You have ${vm.scores.totalScore} points. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.resetGame()
                        newGameDialog = false
                    }
                ) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { newGameDialog = false }) { Text("No") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yahtzee") },
                actions = {
                    IconButton(onClick = { newGameDialog = true }) { Icon(Icons.Default.Add, null) }
                    Dice(1, "").ShowDice(vm.diceLook, Modifier.size(40.dp)) { vm.diceLook = !vm.diceLook }
                }
            )
        },
        bottomBar = { BottomBarDiceRow(vm, vm.diceLook) },
    ) { p ->
        if (vm.scores.isGameOver && vm.showGameOverDialog) {
            AlertDialog(
                onDismissRequest = { vm.showGameOverDialog = false },
                title = { Text("Game Over") },
                text = { Text("You got a score of ${vm.scores.totalScore}") },
                confirmButton = { TextButton(onClick = vm::resetGame) { Text("Play Again") } },
                dismissButton = {
                    TextButton(
                        onClick = {
                            vm.showGameOverDialog = false
                        }
                    ) { Text("Stop Playing") }
                }
            )
        }

        Column(
            modifier = Modifier.padding(p),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                SmallScores(
                    smallScore = vm.scores.smallScore,
                    hand = vm.hand,
                    hasBonus = vm.scores.hasBonus,
                    isRolling = vm.rolling,
                    containsCheck = { vm.scores.scoreList.containsKey(it) },
                    scoreGet = { vm.scores.scoreList.getOrDefault(it, 0) },
                    onOnesClick = vm::placeOnes,
                    onTwosClick = vm::placeTwos,
                    onThreesClick = vm::placeThrees,
                    onFoursClick = vm::placeFours,
                    onFivesClick = vm::placeFives,
                    onSixesClick = vm::placeSixes,
                    modifier = Modifier.weight(1f)
                )
                LargeScores(
                    largeScore = vm.scores.largeScore,
                    isRolling = vm.rolling,
                    hand = vm.hand,
                    isNotRollOneState = vm.state != YahtzeeState.RollOne,
                    containsCheck = { vm.scores.scoreList.containsKey(it) },
                    scoreGet = { vm.scores.scoreList.getOrDefault(it, 0) },
                    canGetHand = {
                        when (it) {
                            HandType.ThreeOfAKind -> vm.scores.canGetThreeKind(vm.hand)
                            HandType.FourOfAKind -> vm.scores.canGetFourKind(vm.hand)
                            HandType.FullHouse -> vm.scores.canGetFullHouse(vm.hand)
                            HandType.SmallStraight -> vm.scores.canGetSmallStraight(vm.hand)
                            HandType.LargeStraight -> vm.scores.canGetLargeStraight(vm.hand)
                            HandType.Yahtzee -> vm.scores.canGetYahtzee(vm.hand)
                            else -> false
                        }
                    },
                    onThreeKindClick = vm::placeThreeOfKind,
                    onFourKindClick = vm::placeFourOfKind,
                    onFullHouseClick = vm::placeFullHouse,
                    onSmallStraightClick = vm::placeSmallStraight,
                    onLargeStraightClick = vm::placeLargeStraight,
                    onYahtzeeClick = vm::placeYahtzee,
                    onChanceClick = vm::placeChance,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Total Score: ${animateIntAsState(vm.scores.totalScore).value}")
        }
    }
}

@Composable
fun BottomBarDiceRow(vm: YahtzeeViewModel, diceLooks: Boolean) {
    BottomAppBar {
        vm.hand.forEach { dice ->
            dice(
                useDots = diceLooks,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(1f)
                    .border(
                        width = animateDpAsState(targetValue = if (dice in vm.hold) 4.dp else 0.dp).value,
                        color = animateColorAsState(targetValue = if (dice in vm.hold) Emerald else Color.Transparent).value,
                        shape = RoundedCornerShape(7.dp)
                    )
            ) { if (dice in vm.hold) vm.hold.remove(dice) else vm.hold.add(dice) }
        }

        IconButton(
            onClick = vm::reroll,
            modifier = Modifier.weight(1f),
            enabled = vm.state != YahtzeeState.Stop
        ) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = animateColorAsState(
                    when (vm.state) {
                        YahtzeeState.RollOne -> Emerald
                        YahtzeeState.RollTwo -> Sunflower
                        YahtzeeState.RollThree -> Alizarin
                        YahtzeeState.Stop -> LocalContentColor.current
                    }
                ).value
            )
        }
    }
}

@Composable
fun SmallScores(
    smallScore: Int,
    hand: List<Dice>,
    hasBonus: Boolean,
    isRolling: Boolean,
    containsCheck: (HandType) -> Boolean,
    scoreGet: (HandType) -> Int,
    onOnesClick: ScoreClick,
    onTwosClick: ScoreClick,
    onThreesClick: ScoreClick,
    onFoursClick: ScoreClick,
    onFivesClick: ScoreClick,
    onSixesClick: ScoreClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        val groupedCheck by remember {
            derivedStateOf {
                hand.groupingBy { it.value }
                    .eachCount()
                    .toList()
                    .sortedWith(compareBy({ it.second }, { it.first }))
                    .reversed()
                    .map { it.first }
            }
        }

        val highest = groupedCheck.elementAtOrNull(0)
        val medium = groupedCheck.elementAtOrNull(1)
        val lowest = groupedCheck.elementAtOrNull(2)

        fun canScore(value: Int) = highest == value || medium == value || lowest == value
        fun scoreColor(value: Int) = when {
            highest == value -> Emerald
            medium == value -> Sunflower
            lowest == value -> Alizarin
            else -> Color.Transparent
        }

        ScoreButton(
            category = "Ones",
            enabled = !containsCheck(HandType.Ones),
            score = scoreGet(HandType.Ones),
            canScore = canScore(1) && !isRolling,
            customBorderColor = scoreColor(1),
            onClick = onOnesClick
        )

        ScoreButton(
            category = "Twos",
            enabled = !containsCheck(HandType.Twos),
            score = scoreGet(HandType.Twos),
            canScore = canScore(2) && !isRolling,
            customBorderColor = scoreColor(2),
            onClick = onTwosClick
        )

        ScoreButton(
            category = "Threes",
            enabled = !containsCheck(HandType.Threes),
            score = scoreGet(HandType.Threes),
            canScore = canScore(3) && !isRolling,
            customBorderColor = scoreColor(3),
            onClick = onThreesClick
        )

        ScoreButton(
            category = "Fours",
            enabled = !containsCheck(HandType.Fours),
            score = scoreGet(HandType.Fours),
            canScore = canScore(4) && !isRolling,
            customBorderColor = scoreColor(4),
            onClick = onFoursClick
        )

        ScoreButton(
            category = "Fives",
            enabled = !containsCheck(HandType.Fives),
            score = scoreGet(HandType.Fives),
            canScore = canScore(5) && !isRolling,
            customBorderColor = scoreColor(5),
            onClick = onFivesClick
        )

        ScoreButton(
            category = "Sixes",
            enabled = !containsCheck(HandType.Sixes),
            score = scoreGet(HandType.Sixes),
            canScore = canScore(6) && !isRolling,
            customBorderColor = scoreColor(6),
            onClick = onSixesClick
        )

        AnimatedVisibility(hasBonus) {
            Text("+35 for >= 63")
        }

        if (smallScore >= 63) {
            val score by animateIntAsState(targetValue = smallScore)
            Text("Small Score: ${score + 35} ($score)")
        } else {
            Text("Small Score: ${animateIntAsState(smallScore).value}")
        }
    }
}

@Composable
fun LargeScores(
    largeScore: Int,
    isRolling: Boolean,
    hand: List<Dice>,
    isNotRollOneState: Boolean,
    containsCheck: (HandType) -> Boolean,
    scoreGet: (HandType) -> Int,
    canGetHand: (HandType) -> Boolean,
    onThreeKindClick: ScoreClick,
    onFourKindClick: ScoreClick,
    onFullHouseClick: ScoreClick,
    onSmallStraightClick: ScoreClick,
    onLargeStraightClick: ScoreClick,
    onYahtzeeClick: ScoreClick,
    onChanceClick: ScoreClick,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        ScoreButton(
            category = "Three of a Kind",
            enabled = !containsCheck(HandType.ThreeOfAKind),
            score = scoreGet(HandType.ThreeOfAKind),
            canScore = canGetHand(HandType.ThreeOfAKind) && isNotRollOneState && !isRolling,
            onClick = onThreeKindClick
        )

        ScoreButton(
            category = "Four of a Kind",
            enabled = !containsCheck(HandType.FourOfAKind),
            score = scoreGet(HandType.FourOfAKind),
            canScore = canGetHand(HandType.FourOfAKind) && isNotRollOneState && !isRolling,
            onClick = onFourKindClick
        )

        ScoreButton(
            category = "Full House",
            enabled = !containsCheck(HandType.FullHouse),
            score = scoreGet(HandType.FullHouse),
            canScore = canGetHand(HandType.FullHouse) && isNotRollOneState && !isRolling,
            onClick = onFullHouseClick
        )

        ScoreButton(
            category = "Small Straight",
            enabled = !containsCheck(HandType.SmallStraight),
            score = scoreGet(HandType.SmallStraight),
            canScore = canGetHand(HandType.SmallStraight) && isNotRollOneState && !isRolling,
            onClick = onSmallStraightClick
        )

        ScoreButton(
            category = "Large Straight",
            enabled = !containsCheck(HandType.LargeStraight),
            score = scoreGet(HandType.LargeStraight),
            canScore = canGetHand(HandType.LargeStraight) && isNotRollOneState && !isRolling,
            onClick = onLargeStraightClick
        )

        ScoreButton(
            category = "Yahtzee",
            enabled = !containsCheck(HandType.Yahtzee) ||
                    canGetHand(HandType.Yahtzee) &&
                    hand.none { it.value == 0 },
            score = scoreGet(HandType.Yahtzee),
            canScore = canGetHand(HandType.Yahtzee) && isNotRollOneState && !isRolling,
            onClick = onYahtzeeClick
        )

        ScoreButton(
            category = "Chance",
            enabled = !containsCheck(HandType.Chance),
            score = scoreGet(HandType.Chance),
            onClick = onChanceClick
        )

        Text("Large Score: ${animateIntAsState(largeScore).value}")
    }
}

@Composable
fun ScoreButton(
    category: String,
    enabled: Boolean,
    canScore: Boolean = false,
    customBorderColor: Color = Emerald,
    score: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(
            width = ButtonDefaults.outlinedButtonBorder.width,
            color = animateColorAsState(
                when {
                    canScore && enabled -> customBorderColor
                    enabled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
            ).value
        )
    ) { Text("$category: ${animateIntAsState(score).value}") }
}

@Composable
fun Dice(dice: Dice, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        tonalElevation = 4.dp,
        enabled = dice.value != 0,
        border = BorderStroke(1.dp, contentColorFor(MaterialTheme.colorScheme.surface)),
        modifier = modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (dice.value == 0) "" else dice.value.toString(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DiceDots(dice: Dice, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        tonalElevation = 4.dp,
        enabled = dice.value != 0,
        border = BorderStroke(1.dp, contentColorFor(MaterialTheme.colorScheme.surface)),
        modifier = modifier.size(56.dp)
    ) {
        when (dice.value) {
            1 -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                    Text(
                        DOT_LOOK,
                        textAlign = TextAlign.Center
                    )
                }
            }

            2 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.BottomStart), textAlign = TextAlign.Center)
                }
            }

            3 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.TopEnd), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.BottomStart), textAlign = TextAlign.Center)
                }
            }

            4 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }

            5 -> {
                Box(modifier = Modifier.padding(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Text(DOT_LOOK, modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(DOT_LOOK, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
            }

            6 -> {
                val fontSize = LocalTextStyle.current.fontSize
                val fontColor = LocalContentColor.current
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spaceBetweenWidthDots = size.width / 3
                    val spaceBetweenHeightDots = size.height / 4
                    repeat(6) {
                        drawCircle(
                            color = fontColor,
                            radius = fontSize.toPx() / 4,
                            center = Offset(
                                spaceBetweenWidthDots * (it % 2 + 1),
                                spaceBetweenHeightDots * (it % 3 + 1)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun DicePreview() {
    Row {
        DiceDots(dice = Dice(1, "1"))
        DiceDots(dice = Dice(2, "2"))
        DiceDots(dice = Dice(3, "3"))
        DiceDots(dice = Dice(4, "4"))
        DiceDots(dice = Dice(5, "5"))
        DiceDots(dice = Dice(6, "6"))
    }
}

@Preview
@Composable
fun YahtzeePreview() {
    YahtzeeScreen()
}