package com.example.luckwood


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class PickMethod {
    LocalPredict,
    LuckyMissing
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PickNumberScreen(onNavigateToSavedPicks: () -> Unit = {}) {
    var selectedLottery by remember { mutableStateOf("双色球") }
    var pickMethod by remember { mutableStateOf(PickMethod.LocalPredict) }
    var predictions by remember { mutableStateOf<List<LotteryPrediction>>(emptyList()) }
    var kl8Predictions by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var kl8BetCount by remember { mutableIntStateOf(20) }
    var showPredictions by remember { mutableStateOf(false) }
    var showKl8Predictions by remember { mutableStateOf(false) }
    var ssqLuckyResults by remember { mutableStateOf<SSQResponse?>(null) }
    var dltLuckyResults by remember { mutableStateOf<DLTResponse?>(null) }
    var ssqLastDraw by remember { mutableStateOf<SSQLastDrawResponse?>(null) }
    var dltLastDraw by remember { mutableStateOf<DLTLastDrawResponse?>(null) }
    var kl8LastDraw by remember { mutableStateOf<KL8LastDrawResponse?>(null) }
    var targetIssueCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isKl8 = selectedLottery == "快乐8"
    val methodSupportsLucky = !isKl8
    val effectiveMethod = if (isKl8) PickMethod.LocalPredict else pickMethod

    LaunchedEffect(selectedLottery) {
        if (isKl8 && pickMethod == PickMethod.LuckyMissing) {
            pickMethod = PickMethod.LocalPredict
        }
        predictions = emptyList()
        kl8Predictions = emptyList()
        ssqLuckyResults = null
        dltLuckyResults = null
        showPredictions = false
        showKl8Predictions = false
        errorMessage = null
        isLoading = true
        try {
            when (selectedLottery) {
                "双色球" -> {
                    dltLastDraw = null
                    kl8LastDraw = null
                    ssqLastDraw = RetrofitClient.apiService.getSSQLastDraw()
                    targetIssueCode = LotteryPicksHelper.nextIssueCode(ssqLastDraw!!.code)
                }
                "大乐透" -> {
                    ssqLastDraw = null
                    kl8LastDraw = null
                    dltLastDraw = RetrofitClient.apiService.getDLTLastDraw()
                    targetIssueCode = LotteryPicksHelper.nextIssueCode(dltLastDraw!!.code)
                }
                else -> {
                    ssqLastDraw = null
                    dltLastDraw = null
                    kl8LastDraw = RetrofitClient.apiService.getKL8LastDraw()
                    targetIssueCode = LotteryPicksHelper.nextIssueCode(kl8LastDraw!!.code)
                }
            }
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "加载期号失败: ${e.message}"
            ssqLastDraw = null
            dltLastDraw = null
            kl8LastDraw = null
        }
    }

    val frontNumbers = when (selectedLottery) {
        "双色球" -> ssqLastDraw?.frontNumbers
        "大乐透" -> dltLastDraw?.frontNumbers
        else -> null
    }
    val lastDrawCode = when (selectedLottery) {
        "双色球" -> ssqLastDraw?.code
        "大乐透" -> dltLastDraw?.code
        "快乐8" -> kl8LastDraw?.code
        else -> null
    }
    val lastDrawDate = when (selectedLottery) {
        "双色球" -> ssqLastDraw?.issueDate
        "大乐透" -> dltLastDraw?.issueDate
        "快乐8" -> kl8LastDraw?.issueDate
        else -> null
    }

    val methodHint = when (effectiveMethod) {
        PickMethod.LocalPredict -> if (isKl8) {
            "按 1–80 洗牌切分生成，任意两注最多重复 2 个号码"
        } else {
            "按上期开奖号码打乱分区生成"
        }
        PickMethod.LuckyMissing -> "按遗漏次数与概率模型生成幸运号"
    }

    fun clearResults() {
        predictions = emptyList()
        kl8Predictions = emptyList()
        ssqLuckyResults = null
        dltLuckyResults = null
        showPredictions = false
        showKl8Predictions = false
    }

    fun generate() {
        coroutineScope.launch {
            try {
                isGenerating = true
                errorMessage = null
                clearResults()
                when {
                    isKl8 -> {
                        // Heavy constraint search — never run on Main (ANR risk).
                        val betCount = kl8BetCount
                        val result = withContext(Dispatchers.Default) {
                            LotteryPredictor.generateKl8Pick10Detailed(count = betCount)
                        }
                        if (!result.completed) {
                            errorMessage = result.reason
                                ?: "仅生成 ${result.generatedCount}/${result.requestedCount} 注"
                        } else {
                            kl8Predictions = result.bets
                            showKl8Predictions = true
                        }
                    }
                    effectiveMethod == PickMethod.LuckyMissing -> {
                        if (selectedLottery == "双色球") {
                            ssqLuckyResults = RetrofitClient.apiService.getSSQLuckyNumbers(n = 5)
                        } else {
                            dltLuckyResults = RetrofitClient.apiService.getDLTLuckyNumbers(n = 3)
                        }
                    }
                    else -> {
                        val inputNumbers = frontNumbers ?: run {
                            errorMessage = "上期号码未加载"
                            return@launch
                        }
                        val lottery = selectedLottery
                        predictions = withContext(Dispatchers.Default) {
                            if (lottery == "双色球") {
                                LotteryPredictor.processDoubleColorBall(inputNumbers)
                            } else {
                                LotteryPredictor.processDaLeTou(inputNumbers)
                            }
                        }
                        showPredictions = true
                    }
                }
            } catch (e: Exception) {
                clearResults()
                errorMessage = e.message ?: "生成失败"
            } finally {
                isGenerating = false
            }
        }
    }

    val canGenerate = !isLoading && !isGenerating && when {
        isKl8 -> true
        effectiveMethod == PickMethod.LuckyMissing -> true
        else -> frontNumbers != null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "选号",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "选彩种与算法，生成后保存到号码本",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "lottery-type") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "彩种",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("双色球", "大乐透", "快乐8").forEach { type ->
                        FilterChip(
                            selected = selectedLottery == type,
                            onClick = { selectedLottery = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        if (methodSupportsLucky) {
            item(key = "pick-method") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "算法",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = pickMethod == PickMethod.LocalPredict,
                            onClick = {
                                pickMethod = PickMethod.LocalPredict
                                clearResults()
                            },
                            label = { Text("本地预测") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                        FilterChip(
                            selected = pickMethod == PickMethod.LuckyMissing,
                            onClick = {
                                pickMethod = PickMethod.LuckyMissing
                                clearResults()
                            },
                            label = { Text("幸运遗漏") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                    Text(
                        text = methodHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item(key = "kl8-method-hint") {
                Text(
                    text = methodHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "last-draw") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isKl8) "最新期号" else "上期开奖",
                        style = MaterialTheme.typography.titleMedium
                    )
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text("正在加载…", fontSize = 12.sp)
                        }
                        lastDrawCode != null -> {
                            Text(
                                text = "期号 $lastDrawCode",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (lastDrawDate != null) {
                                Text(
                                    text = "开奖 ${formatIssueDate(lastDrawDate)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isKl8 && frontNumbers != null) {
                                LastDrawBallsRow(
                                    frontNumbers = frontNumbers,
                                    selectedLottery = selectedLottery,
                                    ssqLastDraw = ssqLastDraw,
                                    dltLastDraw = dltLastDraw
                                )
                            }
                            if (isKl8) {
                                kl8LastDraw?.numbers?.let { nums ->
                                    Kl8BallFlowRow(numbers = nums.sorted())
                                }
                            }
                        }
                        errorMessage != null && frontNumbers == null && !isKl8 -> {
                            Text(
                                text = errorMessage!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (isKl8) {
            item(key = "kl8-bet-count") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "生成注数：$kl8BetCount 注",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = kl8BetCount.toFloat(),
                        onValueChange = { kl8BetCount = it.toInt().coerceIn(1, 20) },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = Kl8BallColor,
                            activeTrackColor = Kl8BallColor
                        )
                    )
                    Text(
                        text = "可选 1–20 注，默认 20 注",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item(key = "generate-btn") {
            Button(
                onClick = { generate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = canGenerate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isKl8) Kl8BallColor else MaterialTheme.colorScheme.primary
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("生成号码", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (errorMessage != null && !isLoading) {
            item(key = "error") {
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (!isKl8 && effectiveMethod == PickMethod.LocalPredict &&
            showPredictions && predictions.isNotEmpty()
        ) {
            item(key = "local-predictions") {
                LocalPredictionsCard(
                    selectedLottery = selectedLottery,
                    predictions = predictions
                )
            }
            item(key = "save-local") {
                val pickItems = if (selectedLottery == "双色球") {
                    LotteryPicksHelper.buildSsqPicksFromPredictions(predictions)
                } else {
                    LotteryPicksHelper.buildDltPicksFromPredictions(predictions)
                }
                SavePicksBar(
                    contentKey = LotteryPicksHelper.contentKey(pickItems),
                    issueCode = targetIssueCode,
                    pickCount = pickItems.size,
                    onIssueCodeChange = { targetIssueCode = it },
                    onSave = {
                        RetrofitClient.apiService.savePicks(
                            SavePicksRequest(
                                lotteryType = LotteryPicksHelper.lotteryTypeCode(selectedLottery),
                                issueCode = targetIssueCode,
                                source = "generate",
                                picks = pickItems
                            )
                        )
                    },
                    onSaved = onNavigateToSavedPicks
                )
            }
        }

        if (!isKl8 && effectiveMethod == PickMethod.LuckyMissing && ssqLuckyResults != null) {
            item(key = "ssq-lucky") {
                SSQResultsDisplay(ssqLuckyResults!!)
            }
            item(key = "save-ssq-lucky") {
                val pickItems = LotteryPicksHelper.buildSsqPicks(ssqLuckyResults!!.results)
                SavePicksBar(
                    contentKey = LotteryPicksHelper.contentKey(pickItems),
                    issueCode = targetIssueCode,
                    pickCount = ssqLuckyResults!!.results.size,
                    onIssueCodeChange = { targetIssueCode = it },
                    onSave = {
                        RetrofitClient.apiService.savePicks(
                            SavePicksRequest(
                                lotteryType = "ssq",
                                issueCode = targetIssueCode,
                                source = "generate",
                                picks = pickItems
                            )
                        )
                    },
                    onSaved = onNavigateToSavedPicks
                )
            }
        }

        if (!isKl8 && effectiveMethod == PickMethod.LuckyMissing && dltLuckyResults != null) {
            item(key = "dlt-lucky") {
                DLTResultsDisplay(dltLuckyResults!!)
            }
            item(key = "save-dlt-lucky") {
                val pickItems = LotteryPicksHelper.buildDltPicks(dltLuckyResults!!.results)
                SavePicksBar(
                    contentKey = LotteryPicksHelper.contentKey(pickItems),
                    issueCode = targetIssueCode,
                    pickCount = dltLuckyResults!!.results.size,
                    onIssueCodeChange = { targetIssueCode = it },
                    onSave = {
                        RetrofitClient.apiService.savePicks(
                            SavePicksRequest(
                                lotteryType = "dlt",
                                issueCode = targetIssueCode,
                                source = "generate",
                                picks = pickItems
                            )
                        )
                    },
                    onSaved = onNavigateToSavedPicks
                )
            }
        }

        if (isKl8 && showKl8Predictions && kl8Predictions.isNotEmpty()) {
            kl8Pick10ResultItems(kl8Predictions)
            item(key = "save-kl8") {
                val pickItems = LotteryPicksHelper.buildKl8Picks(kl8Predictions)
                SavePicksBar(
                    contentKey = LotteryPicksHelper.contentKey(pickItems),
                    issueCode = targetIssueCode,
                    pickCount = pickItems.size,
                    onIssueCodeChange = { targetIssueCode = it },
                    onSave = {
                        RetrofitClient.apiService.savePicks(
                            SavePicksRequest(
                                lotteryType = "kl8",
                                issueCode = targetIssueCode,
                                source = "generate",
                                picks = pickItems
                            )
                        )
                    },
                    onSaved = onNavigateToSavedPicks,
                    buttonColor = Kl8BallColor
                )
            }
        }
    }
}

@Composable
private fun LastDrawBallsRow(
    frontNumbers: List<Int>,
    selectedLottery: String,
    ssqLastDraw: SSQLastDrawResponse?,
    dltLastDraw: DLTLastDrawResponse?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        frontNumbers.forEach { number ->
            NumberBall(
                number = number,
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(text = "+", fontSize = 14.sp)
        if (selectedLottery == "双色球" && ssqLastDraw != null) {
            NumberBall(
                number = ssqLastDraw.backNumber,
                container = MaterialTheme.colorScheme.secondary,
                content = MaterialTheme.colorScheme.onSecondary
            )
        }
        if (selectedLottery == "大乐透" && dltLastDraw != null) {
            dltLastDraw.backNumbers.forEach { number ->
                NumberBall(
                    number = number,
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun NumberBall(number: Int, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = container, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            fontSize = 14.sp,
            color = content,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocalPredictionsCard(
    selectedLottery: String,
    predictions: List<LotteryPrediction>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "预测号码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                predictions.forEachIndexed { index, prediction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "第${index + 1}组",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                val redBallCount = if (selectedLottery == "双色球") 6 else 5
                                prediction.redBalls.take(redBallCount).forEach { ball ->
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .padding(horizontal = 1.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ball.toString(),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = " + ",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                if (selectedLottery == "双色球") {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prediction.blueBall.toString(),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                } else {
                                    prediction.redBalls.drop(redBallCount).take(2).forEach { ball ->
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .padding(horizontal = 1.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ball.toString(),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
