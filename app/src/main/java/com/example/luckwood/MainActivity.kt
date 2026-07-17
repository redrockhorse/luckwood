package com.example.luckwood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
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
    object Pick : Screen("pick", "选号")
    object Mine : Screen("mine", "号码本")
    object More : Screen("more", "更多")
    object ManualCheck : Screen("manual_check", "手动对号")
    object Football : Screen("football", "足球分析")
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Screen.Pick.route,
        Screen.Mine.route,
        Screen.More.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pick.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Pick.route) {
                PickNumberScreen(
                    onNavigateToSavedPicks = {
                        navController.navigate(Screen.Mine.route) {
                            popUpTo(Screen.Pick.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Mine.route) {
                SavedPicksScreen(
                    onManualCheck = { navController.navigate(Screen.ManualCheck.route) }
                )
            }
            composable(Screen.More.route) {
                MoreScreen(navController = navController)
            }
            composable(Screen.ManualCheck.route) {
                ManualCheckScreen(navController = navController)
            }
            composable(Screen.Football.route) {
                FootballScreen(navController = navController)
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
    val items = listOf(Screen.Pick, Screen.Mine, Screen.More)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (screen) {
                            Screen.Pick -> Icons.Default.Edit
                            Screen.Mine -> Icons.Default.List
                            Screen.More -> Icons.Default.MoreVert
                            else -> Icons.Default.Edit
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
fun MoreScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "更多",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "次要工具与分析入口",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.Football.route) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "足球分析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "查询赛程、查看推荐与历史对阵",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手动对号") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Kl8CheckScreen()
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("足球分析") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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
        
        // 查询比赛按钮
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

internal fun formatIssueDate(issueDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = inputFormat.parse(issueDate)
        if (date != null) outputFormat.format(date) else issueDate.take(10)
    } catch (_: Exception) {
        issueDate.take(10)
    }
}

// Deep blackish-red for KL8 balls — higher contrast with white digits for older readers.
internal val Kl8BallColor = Color(0xFF6B1414)
private val Kl8MissBallColor = Color.White
internal val Kl8ErrorColor = Color(0xFFE74C3C)

internal enum class Kl8BallStyle { Filled, Miss, Win, Hit }

private val Kl8BallSize = 30.dp
private val Kl8BallSpacing = 6.dp

@Composable
internal fun Kl8BallFlowRow(
    numbers: List<Int>,
    modifier: Modifier = Modifier,
    style: (Int) -> Kl8BallStyle = { Kl8BallStyle.Filled }
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxPerRow = ((maxWidth + Kl8BallSpacing) / (Kl8BallSize + Kl8BallSpacing))
            .toInt()
            .coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(Kl8BallSpacing)) {
            numbers.chunked(maxPerRow).forEach { rowNumbers ->
                Row(horizontalArrangement = Arrangement.spacedBy(Kl8BallSpacing)) {
                    rowNumbers.forEach { number ->
                        Kl8Ball(number, style(number))
                    }
                }
            }
        }
    }
}

@Composable
internal fun Kl8Ball(number: Int, style: Kl8BallStyle = Kl8BallStyle.Filled) {
    val modifier = when (style) {
        Kl8BallStyle.Filled, Kl8BallStyle.Win, Kl8BallStyle.Hit -> Modifier
            .size(30.dp)
            .background(Kl8BallColor, CircleShape)
        Kl8BallStyle.Miss -> Modifier
            .size(30.dp)
            .background(Kl8MissBallColor, CircleShape)
            .border(1.dp, Kl8BallColor, CircleShape)
    }
    val textColor = when (style) {
        Kl8BallStyle.Filled, Kl8BallStyle.Win, Kl8BallStyle.Hit -> Color.White
        Kl8BallStyle.Miss -> Kl8BallColor
    }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "%02d".format(number),
            fontSize = 13.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

internal fun LazyListScope.kl8Pick10ResultItems(tickets: List<List<Int>>) {
    val groupSize = 5
    val pricePerTicket = 2
    val groups = tickets.chunked(groupSize)

    item(key = "kl8-predict-header") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "金额：${tickets.size * pricePerTicket} 元 [1倍]",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "快乐8 选十 · 共${tickets.size}注 · 5注一组",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    groups.forEachIndexed { groupIndex, groupTickets ->
        item(key = "kl8-predict-group-$groupIndex") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F8F8))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "第${groupIndex + 1}组 · ${groupTickets.size}注",
                            fontSize = 13.sp,
                            color = Color(0xFF888888)
                        )
                    }
                    groupTickets.forEachIndexed { ticketIndex, numbers ->
                        Kl8BallFlowRow(
                            numbers = numbers.sorted(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                        if (ticketIndex < groupTickets.lastIndex) {
                            Divider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Kl8CheckScreen() {
    var winningInput by remember { mutableStateOf("") }
    var ticketsInput by remember { mutableStateOf("") }
    var float10Input by remember { mutableStateOf("") }
    var issueCode by remember { mutableStateOf("") }
    var apiCheckResult by remember { mutableStateOf<PickCheckResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isChecking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoadingSaved by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var lastDraw by remember { mutableStateOf<KL8LastDrawResponse?>(null) }
    var savedTicketsKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val lastDrawnCode = lastDraw?.code
    val canCheckIssue = LotteryPicksHelper.canCheckIssue(issueCode, lastDrawnCode)
    val checkBlockedReason = LotteryPicksHelper.checkUnavailableReason(issueCode, lastDrawnCode)
    val ticketsContentKey = remember(ticketsInput) {
        LotteryPicksHelper.contentKeyFromKl8Tickets(Kl8PrizeChecker.parseTickets(ticketsInput))
    }
    val alreadySavedTickets = savedTicketsKey == ticketsContentKey && ticketsContentKey.isNotBlank()

    LaunchedEffect(ticketsContentKey) {
        apiCheckResult = null
    }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            loadError = null
            val draw = RetrofitClient.apiService.getKL8LastDraw()
            lastDraw = draw
            issueCode = draw.code
            winningInput = Kl8PrizeChecker.formatNumbers(draw.numbers)
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            loadError = "加载最新开奖失败: ${LotteryPicksHelper.parseApiError(e)}"
        }
    }

    LaunchedEffect(apiCheckResult, errorMessage) {
        if (apiCheckResult != null) {
            listState.animateScrollToItem(maxOf(0, listState.layoutInfo.totalItemsCount - 1))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "status") {
            when {
                isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            "正在加载最新开奖...",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                loadError != null -> {
                    Text(loadError!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
                lastDraw != null -> {
                    Text(
                        text = "最新开奖 · 期号 ${lastDraw!!.code} · ${formatIssueDate(lastDraw!!.issueDate)}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }

        item(key = "issue-code") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "兑奖期号", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "仅已开奖期号可兑奖，最新已开 ${lastDrawnCode ?: "—"}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                OutlinedTextField(
                    value = issueCode,
                    onValueChange = { issueCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Kl8BallColor,
                        cursorColor = Kl8BallColor
                    )
                )
                if (!canCheckIssue && checkBlockedReason != null) {
                    Text(
                        text = checkBlockedReason,
                        fontSize = 12.sp,
                        color = Kl8BallColor
                    )
                }
            }
        }

        item(key = "winning-input") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "彩果", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "20 个开奖号码，空格或逗号分隔",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                OutlinedTextField(
                    value = winningInput,
                    onValueChange = { winningInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Kl8BallColor,
                        cursorColor = Kl8BallColor
                    )
                )
            }
        }

        item(key = "tickets-input") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "购买号码", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "每行一注，每注 10 个号码",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                OutlinedTextField(
                    value = ticketsInput,
                    onValueChange = { ticketsInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    placeholder = { Text("04 13 20 21 22 39 44 63 65 79", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Kl8BallColor,
                        cursorColor = Kl8BallColor
                    )
                )
            }
        }

        item(key = "float-input") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "选十浮动奖金", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "有中10时填写当期单注奖金，元",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                OutlinedTextField(
                    value = float10Input,
                    onValueChange = { float10Input = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("未中10可留空", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Kl8BallColor,
                        cursorColor = Kl8BallColor
                    )
                )
            }
        }

        item(key = "action-buttons") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (issueCode.isBlank()) {
                                errorMessage = "请先填写兑奖期号"
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                try {
                                    isLoadingSaved = true
                                    errorMessage = null
                                    statusMessage = null
                                    val response = RetrofitClient.apiService.getPicks(
                                        lotteryType = "kl8",
                                        issueCode = issueCode
                                    )
                                    ticketsInput = LotteryPicksHelper.picksToKl8TicketsText(response.picks)
                                    statusMessage = "已加载 ${response.count} 注"
                                    isLoadingSaved = false
                                } catch (e: Exception) {
                                    isLoadingSaved = false
                                    errorMessage = "加载已保存号码失败: ${LotteryPicksHelper.parseApiError(e)}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isLoadingSaved
                    ) {
                        Text(if (isLoadingSaved) "加载中" else "从已保存加载", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val tickets = Kl8PrizeChecker.parseTickets(ticketsInput)
                            val errors = Kl8PrizeChecker.validate(
                                Kl8PrizeChecker.parseNumbers(winningInput),
                                tickets
                            ).filterNot { it.startsWith("彩果") }
                            if (issueCode.isBlank()) {
                                errorMessage = "请先填写兑奖期号"
                                return@OutlinedButton
                            }
                            if (errors.isNotEmpty()) {
                                errorMessage = errors.joinToString("；")
                                return@OutlinedButton
                            }
                            if (alreadySavedTickets) {
                                errorMessage = "本批号码已保存，请修改号码后再保存"
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                try {
                                    isSaving = true
                                    errorMessage = null
                                    statusMessage = null
                                    RetrofitClient.apiService.savePicks(
                                        SavePicksRequest(
                                            lotteryType = "kl8",
                                            issueCode = issueCode,
                                            source = "generate",
                                            picks = LotteryPicksHelper.buildKl8Picks(tickets)
                                        )
                                    )
                                    savedTicketsKey = ticketsContentKey
                                    statusMessage = "已保存 ${tickets.size} 注"
                                    isSaving = false
                                } catch (e: Exception) {
                                    isSaving = false
                                    errorMessage = "保存失败: ${LotteryPicksHelper.parseApiError(e)}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isSaving && !alreadySavedTickets
                    ) {
                        Text(
                            when {
                                isSaving -> "保存中"
                                alreadySavedTickets -> "已保存"
                                else -> "保存本批"
                            },
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        statusMessage = null
                        apiCheckResult = null

                        if (issueCode.isBlank()) {
                            errorMessage = "请先填写兑奖期号"
                            return@Button
                        }
                        if (!canCheckIssue) {
                            errorMessage = checkBlockedReason ?: "该期尚未开奖，无法兑奖"
                            return@Button
                        }

                        val float10Amount = Kl8PrizeChecker.parseFloat10Input(float10Input)
                        if (float10Amount == Int.MIN_VALUE) {
                            errorMessage = "选十浮动奖金请输入有效数字"
                            return@Button
                        }

                        coroutineScope.launch {
                            try {
                                isChecking = true
                                val tickets = Kl8PrizeChecker.parseTickets(ticketsInput)
                                if (tickets.isNotEmpty()) {
                                    val ticketErrors = Kl8PrizeChecker.validate(
                                        Kl8PrizeChecker.parseNumbers(winningInput),
                                        tickets
                                    ).filterNot { it.startsWith("彩果") }
                                    if (ticketErrors.isNotEmpty()) {
                                        errorMessage = ticketErrors.joinToString("；")
                                        isChecking = false
                                        return@launch
                                    }
                                    if (!alreadySavedTickets) {
                                        RetrofitClient.apiService.savePicks(
                                            SavePicksRequest(
                                                lotteryType = "kl8",
                                                issueCode = issueCode,
                                                source = "generate",
                                                picks = LotteryPicksHelper.buildKl8Picks(tickets)
                                            )
                                        )
                                        savedTicketsKey = ticketsContentKey
                                    }
                                }

                                val response = RetrofitClient.apiService.checkPicks(
                                    lotteryType = "kl8",
                                    issueCode = issueCode
                                )
                                apiCheckResult = response
                                isChecking = false
                            } catch (e: Exception) {
                                isChecking = false
                                errorMessage = "对号失败: ${LotteryPicksHelper.parseApiError(e)}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    enabled = !isLoading && !isChecking && canCheckIssue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Kl8BallColor,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    )
                ) {
                    Text(
                        when {
                            isChecking -> "对号中..."
                            !canCheckIssue -> "待开奖"
                            else -> "对号（API）"
                        },
                        fontSize = 15.sp
                    )
                }
            }
        }

        statusMessage?.let { message ->
            item(key = "status-message") {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        errorMessage?.let { message ->
            item(key = "error") {
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Kl8ErrorColor,
                    lineHeight = 18.sp
                )
            }
        }

        apiCheckResult?.let { response ->
            val float10Amount = Kl8PrizeChecker.parseFloat10Input(float10Input)
            pickCheckResultItems(response, float10Amount)
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
// 双色球结果显示
@Composable
fun SSQResultsDisplay(response: SSQResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${response.lotteryType} - 共${response.count}注",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            response.results.forEachIndexed { index, result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "第${index + 1}注",
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        
                        // 显示号码
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 前区号码（红球）
                            result.frontNumbers.forEachIndexed { numIndex, number ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onError,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    // 显示遗漏次数
                                    if (numIndex < result.frontMissing.size) {
                                        Text(
                                            text = "${result.frontMissing[numIndex]}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            // 后区号码（蓝球）
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = result.backNumber.toString(),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                // 显示遗漏次数
                                Text(
                                    text = "${result.backMissing}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 大乐透结果显示
@Composable
fun DLTResultsDisplay(response: DLTResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${response.lotteryType} - 共${response.count}注",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            response.results.forEachIndexed { index, result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "第${index + 1}注",
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        
                        // 显示号码
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 前区号码
                            result.frontNumbers.forEachIndexed { numIndex, number ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onError,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    // 显示遗漏次数
                                    if (numIndex < result.frontMissing.size) {
                                        Text(
                                            text = "${result.frontMissing[numIndex]}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            // 后区号码
                            result.backNumbers.forEachIndexed { numIndex, number ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = number.toString(),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                    // 显示遗漏次数
                                    if (numIndex < result.backMissing.size) {
                                        Text(
                                            text = "${result.backMissing[numIndex]}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LuckwoodTheme {
        MainScreen()
    }
}