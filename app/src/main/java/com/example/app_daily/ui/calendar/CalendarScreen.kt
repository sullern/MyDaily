package com.example.app_daily.ui.calendar
//管界面
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app_daily.CalendarApplication
import com.example.app_daily.data.local.entity.WeatherEntity
import com.example.app_daily.data.repository.CardRepository
import com.example.app_daily.data.repository.DiaryRepository
import com.example.app_daily.data.repository.UserPreferencesRepository
import com.example.app_daily.data.repository.WeatherRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable//带 @Composable 注解的函数，Compose 会把它渲染成 UI）
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onDateClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CalendarApplication//找到程序的入口
    //as 是 Kotlin 的类型强转（cast）

    val cardRepo = remember { CardRepository(app.database.cardDao()) }
    val diaryRepo = remember { DiaryRepository(app.database.diaryDao()) }
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val weatherRepo = remember { WeatherRepository(app.database.weatherDao(), prefsRepo) }

    //创建自己视图模型
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(
            cardRepo, diaryRepo, weatherRepo, prefsRepo, context
        )
    )
    //拿到定位权限
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.syncWeather() // 拿到权限后，重新拉取天气
        }
    }
//属于是协同执行，就是compose变化的时候，这不会改变，除非传入的key变化才会进行改变
    LaunchedEffect(Unit) {
        // 检查是否有权限，没有就弹窗申请
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
//// 用法1：进页面执行一次（权限检查）
//    LaunchedEffect(Unit) {
//        if (没权限) launcher.launch(...)
//    }
//
//// 用法2：监听某个值变化（ViewModel 里也有类似的）
//    LaunchedEffect(epochDay) {
//        viewModel.loadWeatherForDate(epochDay)
//    }
    val recordedDays by viewModel.recordedDays.collectAsState()//by属于是委托属性
    val weather by viewModel.currentWeather.collectAsState()
    val concreteAdvice by viewModel.concreteAdvice.collectAsState()
    val steelAdvice by viewModel.steelAdvice.collectAsState()
    val isLoadingWeather by viewModel.isLoadingWeather.collectAsState()

    // 获取当前展示的日期
    val displayDate by viewModel.displayDate.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("我的日历") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)//避开系统栏，流出系统栏的空间
                .fillMaxSize()
                .padding(horizontal = 16.dp)//左右流出16个像素宽
        ) {
            item {
                Spacer(Modifier.height(16.dp))//定义一个16像素高的空盒子
                MonthNavigator(
                    currentMonth = currentMonth,
                    onPrev = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(Modifier.height(16.dp))
                WeekHeader()

                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    recordedDays = recordedDays,
                    onDateClick = { date ->
                        selectedDate = date
                        onDateClick(date.toEpochDay()) // 单击进日记
                    },
                    onDateLongClick = { date ->
                        // 长按：更新下方的天气卡片
                        selectedDate = date
                        coroutineScope.launch {
                            viewModel.loadWeatherForDate(date.toEpochDay())
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 天气与工程建议区域
            item {
                if (isLoadingWeather) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (weather != null) {
                    // 把日期文字传给 WeatherCard
                    WeatherCard(
                        weather = weather!!,
                        dateText = if (displayDate == LocalDate.now()) "今日天气" else displayDate.format(DateTimeFormatter.ofPattern("MM月dd日")) + "天气"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AdviceCard("🏗️ 混凝土养护建议", concreteAdvice)
                    Spacer(modifier = Modifier.height(8.dp))
                    AdviceCard("🔩 钢结构合拢意见", steelAdvice)
                } else {
                    Text(
                        text = "暂无该日期的天气数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ================== UI 组件 ==================

@Composable
fun WeatherCard(weather: WeatherEntity, dateText: String) {
    val context = LocalContext.current
    val svgImageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .build()
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // 这里使用传入的日期文字
                Text(dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically){
                    Text(weather.condition, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = "file:///android_asset/weather_icons/${weather.iconCode}.svg",
                        imageLoader = svgImageLoader,
                        contentDescription = weather.condition,
                        modifier = Modifier.size(20.dp) // 卡片里图标可以稍微大一点
                    )

                }

            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("${weather.minTemp}℃ ~ ${weather.maxTemp}℃", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("湿度 ${weather.humidity}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdviceCard(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MonthNavigator(currentMonth: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月")
        }
        Text(
            text = "${currentMonth.year}年${currentMonth.monthValue}月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun WeekHeader() {
    val weekDays = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        weekDays.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    recordedDays: Set<Long>,
    onDateClick: (LocalDate) -> Unit,
    onDateLongClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val emptyCellsBefore = firstDayOfMonth.dayOfWeek.value - 1

    val cells = mutableListOf<LocalDate?>()
    repeat(emptyCellsBefore) { cells.add(null) }
    for (day in 1..daysInMonth) {
        cells.add(currentMonth.atDay(day))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date == null) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                hasRecord = recordedDays.contains(date.toEpochDay()),
                                onClick = { onDateClick(date) },
                                onLongClick = { onDateLongClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (hasRecord) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    com.example.app_daily.ui.theme.APP_DailyTheme {
        CalendarScreen()
    }
}