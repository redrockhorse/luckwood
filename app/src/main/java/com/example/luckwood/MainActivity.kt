package com.example.luckwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.luckwood.ui.theme.LuckwoodTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// 定义导航路由
sealed class Screen(val route: String, val title: String) {
    object Football : Screen("football", "足球")
    object Lottery : Screen("lottery", "彩票")
    object FootballMatchList : Screen("football_match_list/{startDate}/{startHour}/{endDate}/{endHour}", "比赛列表") {
        fun createRoute(startDate: String, startHour: Int, endDate: String, endHour: Int) = 
            "football_match_list/$startDate/$startHour/$endDate/$endHour"
    }
    object FootballDetail : Screen("football_detail/{matchId}", "比赛详情") {
        fun createRoute(matchId: Int) = "football_detail/$matchId"
    }
}

// 足球比赛数据类（扩展版本）
data class FootballMatch(
    val id: Int,
    val matchTime: String,
    val homeTeam: String,
    val awayTeam: String,
    val league: String = "",
    val recommendation: String? = null,
    val confidence: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LuckwoodTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Football.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Football.route) {
                FootballScreen(navController = navController)
            }
            composable(Screen.Lottery.route) {
                LotteryScreen()
            }
            composable(
                route = Screen.FootballMatchList.route,
                arguments = listOf(
                    navArgument("startDate") { type = NavType.StringType },
                    navArgument("startHour") { type = NavType.IntType },
                    navArgument("endDate") { type = NavType.StringType },
                    navArgument("endHour") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val startDate = backStackEntry.arguments?.getString("startDate") ?: ""
                val startHour = backStackEntry.arguments?.getInt("startHour") ?: 0
                val endDate = backStackEntry.arguments?.getString("endDate") ?: ""
                val endHour = backStackEntry.arguments?.getInt("endHour") ?: 0
                FootballMatchListScreen(
                    startDate = startDate,
                    startHour = startHour,
                    endDate = endDate,
                    endHour = endHour,
                    navController = navController
                )
            }
            composable(
                route = Screen.FootballDetail.route,
                arguments = listOf(navArgument("matchId") { type = NavType.IntType })
            ) { backStackEntry ->
                val matchId = backStackEntry.arguments?.getInt("matchId") ?: 0
                FootballDetailScreen(matchId = matchId, navController = navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Football,
        Screen.Lottery
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.Football -> Icons.Default.Home
                            Screen.Lottery -> Icons.Default.DateRange
                            else -> Icons.Default.Home
                        },
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballScreen(navController: NavHostController) {
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var startHour by remember { mutableStateOf(0) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var endHour by remember { mutableStateOf(23) }
    
    var showStartDateTimePicker by remember { mutableStateOf(false) }
    var showEndDateTimePicker by remember { mutableStateOf(false) }
    
    val dateTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "足球比赛查询",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 开始时间
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "开始时间", fontSize = 16.sp)
                
                OutlinedButton(
                    onClick = { showStartDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (startDateMillis != null) {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = startDateMillis!!
                            calendar.set(Calendar.HOUR_OF_DAY, startHour)
                            dateTimeFormatter.format(calendar.time)
                        } else {
                            "选择日期和时间"
                        }
                    )
                }
            }
        }
        
        // 结束时间
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "结束时间", fontSize = 16.sp)
                
                OutlinedButton(
                    onClick = { showEndDateTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (endDateMillis != null) {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = endDateMillis!!
                            calendar.set(Calendar.HOUR_OF_DAY, endHour)
                            dateTimeFormatter.format(calendar.time)
                        } else {
                            "选择日期和时间"
                        }
                    )
                }
            }
        }
        
        // 查询按钮
        Button(
            onClick = {
                val startDate = if (startDateMillis != null) dateFormatter.format(Date(startDateMillis!!)) else ""
                val endDate = if (endDateMillis != null) dateFormatter.format(Date(endDateMillis!!)) else ""
                
                // 导航到比赛列表页面
                navController.navigate(
                    Screen.FootballMatchList.createRoute(startDate, startHour, endDate, endHour)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = startDateMillis != null && endDateMillis != null
        ) {
            Text("查询比赛", fontSize = 16.sp)
        }
    }
    
    // 开始日期时间选择器
    if (showStartDateTimePicker) {
        DateTimePickerDialog(
            initialDateMillis = startDateMillis,
            initialHour = startHour,
            onDateTimeSelected = { millis, hour ->
                startDateMillis = millis
                startHour = hour
                showStartDateTimePicker = false
            },
            onDismiss = { showStartDateTimePicker = false }
        )
    }
    
    // 结束日期时间选择器
    if (showEndDateTimePicker) {
        DateTimePickerDialog(
            initialDateMillis = endDateMillis,
            initialHour = endHour,
            onDateTimeSelected = { millis, hour ->
                endDateMillis = millis
                endHour = hour
                showEndDateTimePicker = false
            },
            onDismiss = { showEndDateTimePicker = false }
        )
    }
}

// 比赛列表界面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballMatchListScreen(
    startDate: String,
    startHour: Int,
    endDate: String,
    endHour: Int,
    navController: NavHostController
) {
    var allMatches by remember { mutableStateOf<List<Pair<Int, MatchData>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // 过滤和排序状态
    var selectedLeagues by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sortAscending by remember { mutableStateOf(true) }
    
    // 格式化API请求时间
    val apiStartTime = "$startDate ${startHour.toString().padStart(2, '0')}:00:00"
    val apiEndTime = "$endDate ${endHour.toString().padStart(2, '0')}:00:00"
    
    // 调用API
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val request = MatchRequest(apiStartTime, apiEndTime)
                val response = RetrofitClient.apiService.getFutureMatches(request)
                
                // 存储完整数据到管理器
                MatchDataManager.setMatches(response.matches)
                
                // 保存原始数据和索引
                allMatches = response.matches.mapIndexed { index, matchData ->
                    index to matchData
                }
                
                // 默认选中所有联赛
                val leagues = response.matches.map { it.matchInfo.league }.toSet()
                selectedLeagues = leagues
                
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "加载失败: ${e.message}"
            }
        }
    }
    
    // 过滤和排序后的数据
    val filteredAndSortedMatches = remember(allMatches, selectedLeagues, sortAscending) {
        val filtered = allMatches.filter { (_, matchData) ->
            selectedLeagues.isEmpty() || matchData.matchInfo.league in selectedLeagues
        }
        
        val sorted = if (sortAscending) {
            filtered.sortedBy { (_, matchData) -> matchData.matchInfo.stime }
        } else {
            filtered.sortedByDescending { (_, matchData) -> matchData.matchInfo.stime }
        }
        
        sorted.map { (index, matchData) ->
            val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val displayFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            
            val displayTime = try {
                val date = timeFormatter.parse(matchData.matchInfo.stime)
                date?.let { displayFormatter.format(it) } ?: matchData.matchInfo.stime
            } catch (e: Exception) {
                matchData.matchInfo.stime
            }
            
            FootballMatch(
                id = index,
                matchTime = displayTime,
                homeTeam = matchData.matchInfo.hname,
                awayTeam = matchData.matchInfo.gname,
                league = matchData.matchInfo.league,
                recommendation = matchData.bestRecommendation?.outcome,
                confidence = matchData.dataQuality?.message
            )
        }
    }
    
    // 获取所有可用的联赛
    val availableLeagues = remember(allMatches) {
        allMatches.map { (_, matchData) -> matchData.matchInfo.league }.distinct().sorted()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("比赛列表", fontSize = 18.sp)
                        Text(
                            "$startDate ${startHour.toString().padStart(2, '0')}:00 - $endDate ${endHour.toString().padStart(2, '0')}:00",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    // 排序按钮
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Text(
                            text = if (sortAscending) "↑" else "↓",
                            fontSize = 20.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // 加载中状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("加载比赛数据中...", fontSize = 16.sp)
                    }
                }
                errorMessage != null -> {
                    // 错误状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // 重新加载
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    val request = MatchRequest(apiStartTime, apiEndTime)
                                    val response = RetrofitClient.apiService.getFutureMatches(request)
                                    MatchDataManager.setMatches(response.matches)
                                    allMatches = response.matches.mapIndexed { index, matchData ->
                                        index to matchData
                                    }
                                    val leagues = response.matches.map { it.matchInfo.league }.toSet()
                                    selectedLeagues = leagues
                                    isLoading = false
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "加载失败: ${e.message}"
                                }
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
                filteredAndSortedMatches.isEmpty() -> {
                    // 空数据状态
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "没有符合条件的比赛",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    // 显示过滤器和比赛列表
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 联赛过滤器
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "联赛筛选",
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                    
                                    TextButton(onClick = {
                                        selectedLeagues = if (selectedLeagues.size == availableLeagues.size) {
                                            emptySet()
                                        } else {
                                            availableLeagues.toSet()
                                        }
                                    }) {
                                        Text(
                                            text = if (selectedLeagues.size == availableLeagues.size) "清空" else "全选",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 联赛标签
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableLeagues) { league ->
                                        FilterChip(
                                            selected = league in selectedLeagues,
                                            onClick = {
                                                selectedLeagues = if (league in selectedLeagues) {
                                                    selectedLeagues - league
                                                } else {
                                                    selectedLeagues + league
                                                }
                                            },
                                            label = { Text(league, fontSize = 13.sp) }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 比赛数量提示
                        Text(
                            text = "共 ${filteredAndSortedMatches.size} 场比赛",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        // 比赛列表
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredAndSortedMatches) { match ->
                                MatchListItem(match = match) {
                                    navController.navigate(Screen.FootballDetail.createRoute(match.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchListItem(match: FootballMatch, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 时间和联赛
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.matchTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                if (match.league.isNotEmpty()) {
                    Text(
                        text = match.league,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            
            // 球队对阵
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.homeTeam,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "VS",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Text(
                    text = match.awayTeam,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            
            // 推荐信息
            if (match.recommendation != null) {
                Text(
                    text = "推荐：${match.recommendation}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballDetailScreen(matchId: Int, navController: NavHostController) {
    val matchData = MatchDataManager.getMatch(matchId)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("比赛详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (matchData == null) {
            // 数据不存在
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "数据加载失败",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. 比赛基本信息
                item {
                    MatchBasicInfoCard(matchData)
                }
                
                // 2. AI推荐卡片
                item {
                    if (matchData.bestRecommendation != null) {
                        RecommendationCard(matchData.bestRecommendation)
                    }
                }
                
                // 3. 数据质量
                item {
                    if (matchData.dataQuality != null) {
                        DataQualityCard(matchData.dataQuality)
                    }
                }
                
                // 4. 主队分析
                item {
                    if (matchData.homeAnalysis != null) {
                        TeamAnalysisCard(
                            teamAnalysis = matchData.homeAnalysis,
                            isHome = true
                        )
                    }
                }
                
                // 5. 客队分析
                item {
                    if (matchData.awayAnalysis != null) {
                        TeamAnalysisCard(
                            teamAnalysis = matchData.awayAnalysis,
                            isHome = false
                        )
                    }
                }
                
                // 6. 主队历史比赛
                item {
                    if (!matchData.homeMatches.isNullOrEmpty()) {
                        HistoricalMatchesCard(
                            title = "${matchData.matchInfo.hname} 历史比赛",
                            matches = matchData.homeMatches
                        )
                    }
                }
                
                // 7. 客队历史比赛
                item {
                    if (!matchData.awayMatches.isNullOrEmpty()) {
                        HistoricalMatchesCard(
                            title = "${matchData.matchInfo.gname} 历史比赛",
                            matches = matchData.awayMatches
                        )
                    }
                }
            }
        }
    }
}

// 1. 比赛基本信息卡片
@Composable
fun MatchBasicInfoCard(matchData: MatchData) {
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    val displayFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    val displayTime = try {
        val date = timeFormatter.parse(matchData.matchInfo.stime)
        date?.let { displayFormatter.format(it) } ?: matchData.matchInfo.stime
    } catch (e: Exception) {
        matchData.matchInfo.stime
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 联赛和轮次
            Text(
                text = "${matchData.matchInfo.league} - 第${matchData.matchInfo.round}轮",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 球队对阵
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = matchData.matchInfo.hname,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "VS",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Text(
                    text = matchData.matchInfo.gname,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 比赛时间
            Text(
                text = displayTime,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 赔率信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OddsItem("主胜", matchData.matchInfo.win)
                OddsItem("平局", matchData.matchInfo.draw)
                OddsItem("主负", matchData.matchInfo.lost)
            }
        }
    }
}

@Composable
fun OddsItem(label: String, odds: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = String.format("%.2f", odds),
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// 2. AI推荐卡片
@Composable
fun RecommendationCard(recommendation: BestRecommendation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 AI推荐",
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "推荐结果",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = recommendation.outcome,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "赔率",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format("%.2f", recommendation.odds),
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("概率", "${String.format("%.1f", recommendation.probability * 100)}%")
                InfoItem("期望回报", String.format("%.2f", recommendation.expectedReturn))
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

// 3. 数据质量卡片
@Composable
fun DataQualityCard(quality: DataQuality) {
    val backgroundColor = when (quality.level) {
        "high" -> MaterialTheme.colorScheme.secondaryContainer
        "medium" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.errorContainer
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "数据质量",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Text(
                    text = "可信度: ${String.format("%.0f", quality.confidence * 100)}%",
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = quality.message,
                fontSize = 13.sp
            )
        }
    }
}

// 4. 球队分析卡片
@Composable
fun TeamAnalysisCard(teamAnalysis: TeamAnalysis, isHome: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHome) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${if (isHome) "🏠" else "✈️"} ${teamAnalysis.teamName} 分析",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 战绩统计
            Text(
                text = "历史战绩（${teamAnalysis.totalMatches}场）",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecordItem("胜", teamAnalysis.wins, MaterialTheme.colorScheme.tertiary)
                RecordItem("平", teamAnalysis.draws, MaterialTheme.colorScheme.primary)
                RecordItem("负", teamAnalysis.losses, MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 概率统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProbabilityItem("胜率", teamAnalysis.winProb)
                ProbabilityItem("平率", teamAnalysis.drawProb)
                ProbabilityItem("负率", teamAnalysis.lossProb)
            }
        }
    }
}

@Composable
fun RecordItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ProbabilityItem(label: String, probability: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "${String.format("%.1f", probability * 100)}%",
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

// 5. 历史比赛卡片
@Composable
fun HistoricalMatchesCard(title: String, matches: List<HistoricalMatch>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Text(
                    text = if (expanded) "▼" else "▶",
                    fontSize = 12.sp
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // 排序：is_same_opponent 为 true 的排在前面
                val sortedMatches = matches.sortedByDescending { it.isSameOpponent }
                
                sortedMatches.forEach { match ->
                    HistoricalMatchItem(match)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = "共 ${matches.size} 场比赛",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun HistoricalMatchItem(match: HistoricalMatch) {
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    val displayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())
    
    val displayTime = try {
        val date = timeFormatter.parse(match.stime)
        date?.let { displayFormatter.format(it) } ?: match.stime
    } catch (e: Exception) {
        match.stime
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isSameOpponent) 
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 第一行：时间和赛季
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayTime,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${match.season} ${if (match.isSameOpponent) "⭐" else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // 第二行：对阵和比分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.hname,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                
                if (match.hscore != null && match.gscore != null) {
                    Text(
                        text = "${match.hscore} : ${match.gscore}",
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        text = "VS",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                Text(
                    text = match.gname,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // 第三行：赔率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallOddsItem("胜", match.win)
                SmallOddsItem("平", match.draw)
                SmallOddsItem("负", match.lost)
            }
        }
    }
}

@Composable
fun SmallOddsItem(label: String, odds: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = String.format("%.2f", odds),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotteryScreen() {
    NumberInputScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberInputScreen() {
    var numbers by remember { mutableStateOf(List(7) { "" }) }
    var selectedLottery by remember { mutableStateOf("双色球") }
    var predictions by remember { mutableStateOf<List<LotteryPrediction>>(emptyList()) }
    var showPredictions by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "彩票号码预测器",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 彩票类型选择
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "选择彩票类型：",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLottery == "双色球",
                            onClick = { 
                                selectedLottery = "双色球"
                                // 清空预测结果和输入框
                                predictions = emptyList()
                                showPredictions = false
                                numbers = List(7) { "" }
                            }
                        )
                        Text(
                            text = "双色球",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLottery == "大乐透",
                            onClick = { 
                                selectedLottery = "大乐透"
                                // 清空预测结果和输入框
                                predictions = emptyList()
                                showPredictions = false
                                numbers = List(7) { "" }
                            }
                        )
                        Text(
                            text = "大乐透",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        // 动态输入框 - 根据彩票类型调整数量
        val inputCount = if (selectedLottery == "双色球") 6 else 5
        Text(
            text = "请输入${if (selectedLottery == "双色球") "6个" else "5个"}号码",
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        if (selectedLottery == "双色球") {
            // 双色球：两行显示，每行3个
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 第一行：前3个输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    numbers.take(3).forEachIndexed { index, value ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                                    numbers = numbers.toMutableList().apply {
                                        this[index] = newValue
                                    }
                                }
                            },
                            label = { Text("${index + 1}") },
                            placeholder = { Text("00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(90.dp)
                                .height(60.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
                
                // 第二行：后3个输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    numbers.drop(3).take(3).forEachIndexed { index, value ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                                    numbers = numbers.toMutableList().apply {
                                        this[index + 3] = newValue
                                    }
                                }
                            },
                            label = { Text("${index + 4}") },
                            placeholder = { Text("00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .width(90.dp)
                                .height(60.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            // 大乐透：一行显示5个
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                numbers.take(inputCount).forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                                numbers = numbers.toMutableList().apply {
                                    this[index] = newValue
                                }
                            }
                        },
                        label = { Text("${index + 1}") },
                        placeholder = { Text("00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(70.dp)
                            .height(60.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
        
        // 预测按钮
        Button(
            onClick = {
                try {
                    val inputNumbers = numbers.filter { it.isNotEmpty() }.map { it.toInt() }
                    predictions = if (selectedLottery == "双色球") {
                        if (inputNumbers.size == 6) {
                            LotteryPredictor.processDoubleColorBall(inputNumbers)
                        } else {
                            emptyList()
                        }
                    } else {
                        if (inputNumbers.size == 5) {
                            LotteryPredictor.processDaLeTou(inputNumbers)
                        } else {
                            emptyList()
                        }
                    }
                    showPredictions = true
                } catch (e: Exception) {
                    // 处理错误
                    predictions = emptyList()
                    showPredictions = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            enabled = {
                val requiredCount = if (selectedLottery == "双色球") 6 else 5
                val filledCount = numbers.take(requiredCount).count { it.isNotEmpty() }
                filledCount == requiredCount
            }()
        ) {
            Text("生成预测号码", fontSize = 14.sp)
        }
        
        // 显示输入的数字
        if (numbers.any { it.isNotEmpty() }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "已选择：$selectedLottery",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "已输入的数字：",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = numbers.filter { it.isNotEmpty() }.joinToString("  "),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // 显示预测结果
        if (showPredictions && predictions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(), // 只用fillMaxWidth，高度自适应
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "预测号码：",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 使用Column显示所有预测结果
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        predictions.forEachIndexed { index, prediction ->
                            val groupNames = if (selectedLottery == "双色球") {
                                listOf("第1组", "第2组", "第3组", "第4组", "第5组")
                            } else {
                                listOf("第1组", "第2组", "第3组", "第4组", "第5组", "第6组")
                            }
                            val groupName = if (index < groupNames.size) groupNames[index] else "第${index + 1}组"
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = groupName,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        // 显示红球
                                        val redBallCount = if (selectedLottery == "双色球") 6 else 5
                                        prediction.redBalls.take(redBallCount).forEach { ball ->
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .padding(horizontal = 1.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = ball.toString(),
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onError
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = " + ",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                        
                                        // 显示蓝球
                                        if (selectedLottery == "双色球") {
                                            // 双色球：显示1个蓝球
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = prediction.blueBall.toString(),
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        } else {
                                            // 大乐透：显示2个蓝球
                                            val blueBallCount = 2
                                            prediction.redBalls.drop(redBallCount).take(blueBallCount).forEach { ball ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .padding(horizontal = 1.dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ball.toString(),
                                                        fontSize = 8.sp,
                                                        color = MaterialTheme.colorScheme.onPrimary
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
    }
}

// 日期时间选择器对话框（组合日期和时间）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDateMillis: Long?,
    initialHour: Int,
    onDateTimeSelected: (Long?, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: 选择日期, 2: 选择时间
    var selectedDateMillis by remember { mutableStateOf(initialDateMillis) }
    var selectedHour by remember { mutableStateOf(initialHour) }
    
    when (step) {
        1 -> {
            // 第一步：选择日期
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
            
            androidx.compose.material3.DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        step = 2 // 进入时间选择
                    }) {
                        Text("下一步")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        2 -> {
            // 第二步：选择时间
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("选择小时") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(24) { hour ->
                                val isSelected = hour == selectedHour
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 16.dp)
                                        .clickable { selectedHour = hour },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Text(
                                        text = "${hour.toString().padStart(2, '0')}:00",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 18.sp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDateTimeSelected(selectedDateMillis, selectedHour)
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { step = 1 }) {
                            Text("上一步")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NumberInputScreenPreview() {
    LuckwoodTheme {
        MainScreen()
    }
}