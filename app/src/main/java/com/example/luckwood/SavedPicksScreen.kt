package com.example.luckwood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SavedPicksScreen() {
    var lotteryFilter by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var batches by remember { mutableStateOf<List<LotteryPicksHelper.PickBatch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }
    var expandedBatchKey by remember { mutableStateOf<String?>(null) }
    var checkResults by remember { mutableStateOf<Map<String, PickCheckResponse>>(emptyMap()) }
    var checkingBatchKey by remember { mutableStateOf<String?>(null) }
    var deletingBatchKey by remember { mutableStateOf<String?>(null) }
    var lastDrawnIssueCodes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val codes = mutableMapOf<String, String>()
        try {
            codes["ssq"] = RetrofitClient.apiService.getSSQLastDraw().code
        } catch (_: Exception) {
        }
        try {
            codes["dlt"] = RetrofitClient.apiService.getDLTLastDraw().code
        } catch (_: Exception) {
        }
        try {
            codes["kl8"] = RetrofitClient.apiService.getKL8LastDraw().code
        } catch (_: Exception) {
        }
        lastDrawnIssueCodes = codes
    }

    fun reloadPicks() {
        coroutineScope.launch {
            try {
                isLoading = true
                loadErrorMessage = null
                val response = RetrofitClient.apiService.getPicks(
                    lotteryType = lotteryFilter,
                    status = statusFilter
                )
                batches = LotteryPicksHelper.groupPicksByBatch(response.picks)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                loadErrorMessage = "加载失败: ${LotteryPicksHelper.parseApiError(e)}"
            }
        }
    }

    LaunchedEffect(lotteryFilter, statusFilter) {
        reloadPicks()
    }

    val visibleBatches = remember(batches) {
        batches.filter { it.picks.isNotEmpty() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "title") {
            Text(
                text = "我的号码",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item(key = "lottery-filter") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(null to "全部", "ssq" to "双色球", "dlt" to "大乐透", "kl8" to "快乐8").forEach { (code, label) ->
                    FilterChip(
                        selected = lotteryFilter == code,
                        onClick = { lotteryFilter = code },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }
        }

        item(key = "status-filter") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(null to "全部", "pending" to "待开奖", "settled" to "已兑奖").forEach { (code, label) ->
                    FilterChip(
                        selected = statusFilter == code,
                        onClick = { statusFilter = code },
                        label = { Text(label, fontSize = 13.sp) }
                    )
                }
            }
        }

        item(key = "action-error") {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (actionErrorMessage != null) {
                    Text(
                        text = actionErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item(key = "list-status") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (visibleBatches.isEmpty()) 80.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading && visibleBatches.isEmpty() -> CircularProgressIndicator()
                    loadErrorMessage != null && visibleBatches.isEmpty() -> {
                        Text(
                            text = loadErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                    visibleBatches.isEmpty() -> {
                        Text(
                            text = "暂无保存记录",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        items(
            items = visibleBatches,
            key = { batch ->
                val pickId = batch.picks.first().id
                "${batch.batchId ?: pickId}-${batch.issueCode}-${batch.lotteryType}"
            }
        ) { batch ->
            val pickId = batch.picks.first().id
            val batchKey = "${batch.batchId ?: pickId}-${batch.issueCode}"
            SavedPickBatchCard(
                batch = batch,
                batchKey = batchKey,
                isExpanded = expandedBatchKey == batchKey,
                checkResult = checkResults[batchKey],
                isChecking = checkingBatchKey == batchKey,
                isDeleting = deletingBatchKey == batchKey,
                lastDrawnIssueCodes = lastDrawnIssueCodes,
                onToggleExpand = {
                    expandedBatchKey = if (expandedBatchKey == batchKey) null else batchKey
                    if (expandedBatchKey == batchKey) {
                        actionErrorMessage = null
                    }
                },
                onCheck = {
                    if (checkResults[batchKey] != null) return@SavedPickBatchCard
                    if (expandedBatchKey != batchKey) {
                        expandedBatchKey = batchKey
                    }
                    checkingBatchKey = batchKey
                    coroutineScope.launch {
                        try {
                            actionErrorMessage = null
                            val result = RetrofitClient.apiService.checkPicks(
                                lotteryType = batch.lotteryType,
                                issueCode = batch.issueCode
                            )
                            checkResults = checkResults + (batchKey to result)
                        } catch (e: Exception) {
                            actionErrorMessage = "兑奖失败: ${LotteryPicksHelper.parseApiError(e)}"
                        } finally {
                            checkingBatchKey = null
                        }
                    }
                },
                onDelete = {
                    deletingBatchKey = batchKey
                    coroutineScope.launch {
                        try {
                            batch.picks.forEach { pick ->
                                RetrofitClient.apiService.deletePick(pick.id)
                            }
                            checkResults = checkResults - batchKey
                            if (expandedBatchKey == batchKey) {
                                expandedBatchKey = null
                            }
                            reloadPicks()
                        } catch (e: Exception) {
                            actionErrorMessage = "删除失败: ${LotteryPicksHelper.parseApiError(e)}"
                        } finally {
                            deletingBatchKey = null
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedPickBatchCard(
    batch: LotteryPicksHelper.PickBatch,
    batchKey: String,
    isExpanded: Boolean,
    checkResult: PickCheckResponse?,
    isChecking: Boolean,
    isDeleting: Boolean,
    lastDrawnIssueCodes: Map<String, String>,
    onToggleExpand: () -> Unit,
    onCheck: () -> Unit,
    onDelete: () -> Unit
) {
    val totalBet = batch.picks.sumOf { (it.betAmount ?: 2.0).toInt() * (it.multiplier ?: 1) }
    val displayName = LotteryPicksHelper.lotteryDisplayName(batch.lotteryType)
    val status = batch.picks.firstOrNull()?.status
    val typeCode = LotteryPicksHelper.normalizeTypeCode(batch.lotteryType)
    val lastDrawnCode = lastDrawnIssueCodes[typeCode]
    val canCheck = LotteryPicksHelper.canCheckIssue(batch.issueCode, lastDrawnCode)
    val checkBlockedReason = LotteryPicksHelper.checkUnavailableReason(
        batch.issueCode,
        lastDrawnCode
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$displayName · 期号 ${batch.issueCode}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${batch.picks.size} 注 · ${totalBet} 元 · ${LotteryPicksHelper.formatCreatedAt(batch.createdAt)}",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = if (!canCheck) {
                        "待开奖 · 期号 ${batch.issueCode}"
                    } else {
                        LotteryPicksHelper.statusLabel(status)
                    },
                    fontSize = 12.sp,
                    color = if (canCheck && status == "settled") {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFEF7B77)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (!canCheck && checkBlockedReason != null) {
                        Text(
                            text = checkBlockedReason,
                            fontSize = 11.sp,
                            color = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isExpanded) "收起" else "查看", fontSize = 14.sp)
                }

                Button(
                    onClick = onCheck,
                    modifier = Modifier.weight(1f),
                    enabled = canCheck && !isChecking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Kl8BallColor,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (canCheck) "兑奖" else "待开奖",
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !isDeleting
                ) {
                    Text(if (isDeleting) "删除中" else "删除", fontSize = 14.sp)
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                if (isExpanded) {
                    SavedBatchPickPreview(batch = batch)
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isExpanded && !canCheck) {
                    Text(
                        text = "该期尚未开奖，开奖后可点「兑奖」查看命中与奖金",
                        fontSize = 12.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isExpanded && checkResult != null) {
                    PickCheckResultSection(
                        response = checkResult,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedBatchPickPreview(batch: LotteryPicksHelper.PickBatch) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "保存号码", fontSize = 13.sp, color = Color(0xFF888888))
        batch.picks.forEachIndexed { index, pick ->
            if (LotteryPicksHelper.isKl8Type(pick.lotteryType)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "第 ${index + 1} 注", fontSize = 12.sp, color = Color(0xFF999999))
                    Kl8BallFlowRow(numbers = pick.numbers?.sorted() ?: emptyList())
                }
            } else {
                SavedSsqDltPickRow(index = index, pick = pick)
            }
        }
    }
}

@Composable
private fun SavedSsqDltPickRow(index: Int, pick: LotteryPick) {
    val isSsq = pick.lotteryType.equals("ssq", ignoreCase = true) ||
        pick.lotteryType.contains("双色")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "第 ${index + 1} 注",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(end = 4.dp)
        )
        pick.frontNumbers?.forEach { number ->
            Kl8Ball(number, Kl8BallStyle.Filled)
        }
        pick.backNumbers?.forEach { number ->
            Text(text = "+", fontSize = 12.sp)
            Kl8Ball(number, if (isSsq) Kl8BallStyle.Win else Kl8BallStyle.Hit)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePicksBar(
    contentKey: String,
    issueCode: String,
    pickCount: Int,
    onIssueCodeChange: (String) -> Unit,
    onSave: suspend () -> Unit,
    onSaved: () -> Unit = {},
    modifier: Modifier = Modifier,
    buttonColor: Color = MaterialTheme.colorScheme.primary
) {
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var savedContentKey by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val alreadySaved = savedContentKey == contentKey

    LaunchedEffect(contentKey) {
        if (savedContentKey != null && savedContentKey != contentKey) {
            saveMessage = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "目标期号",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            androidx.compose.material3.OutlinedTextField(
                value = issueCode,
                onValueChange = onIssueCodeChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            isSaving = true
                            saveMessage = null
                            onSave()
                            savedContentKey = contentKey
                            saveMessage = "已保存 $pickCount 注"
                        } catch (e: Exception) {
                            saveMessage = "保存失败: ${LotteryPicksHelper.parseApiError(e)}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !alreadySaved && issueCode.isNotBlank() && pickCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (alreadySaved) {
                            "已保存本批 ($pickCount 注)"
                        } else {
                            "保存本批号码 ($pickCount 注)"
                        },
                        fontSize = 14.sp
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if (alreadySaved) {
                    Text(
                        text = "重新生成号码后可再次保存",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                saveMessage?.let { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = if (message.startsWith("保存失败")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                Color(0xFF2E7D32)
                            }
                        )
                        if (!message.startsWith("保存失败")) {
                            TextButton(onClick = onSaved) {
                                Text("查看我的号码", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
