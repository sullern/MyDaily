package com.example.app_daily.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.app_daily.data.repository.UserPreferencesRepository
import com.example.app_daily.data.repository.WeatherRepository
import com.example.app_daily.util.LocationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // 1. 读取所有状态
    val currentSplashUri by prefsRepo.splashImageUriFlow.collectAsState(initial = null)
    val isGlobalAutoPlay by prefsRepo.globalAutoPlayFlow.collectAsState(initial = false)
    val autoOpenCamera by prefsRepo.autoOpenCameraFlow.collectAsState(initial = false)

    val weatherKey by prefsRepo.weatherApiKeyFlow.collectAsState(initial = "")
    val weatherHost by prefsRepo.weatherApiHostFlow.collectAsState(initial = "")
    val selectedCity by prefsRepo.selectedCityFlow.collectAsState(initial = "")
    val currentLatLng by prefsRepo.weatherLatLngFlow.collectAsState(initial = null)

    val aiEnabled by prefsRepo.aiEnabledFlow.collectAsState(initial = false)
    val aiKey by prefsRepo.aiApiKeyFlow.collectAsState(initial = "")
    val aiBaseUrl by prefsRepo.aiBaseUrlFlow.collectAsState(initial = "")
    val promptConcrete by prefsRepo.aiPromptConcreteFlow.collectAsState(initial = "")
    val promptSteel by prefsRepo.aiPromptSteelFlow.collectAsState(initial = "")

    // 2. 弹窗控制状态
    var showWeatherKeyDialog by remember { mutableStateOf(false) }
    var showWeatherHostDialog by remember { mutableStateOf(false) }
    var showCitySearchDialog by remember { mutableStateOf(false) }

    var showAiKeyDialog by remember { mutableStateOf(false) }
    var showAiBaseUrlDialog by remember { mutableStateOf(false) }
    var showPromptConcreteDialog by remember { mutableStateOf(false) }
    var showPromptSteelDialog by remember { mutableStateOf(false) }
    val aiModelName by prefsRepo.aiModelNameFlow.collectAsState(initial = "deepseek-flash")
    var showAiModelDialog by remember { mutableStateOf(false) }
    // 3. 相册选择器 (封面)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) { e.printStackTrace() }
            coroutineScope.launch { prefsRepo.saveSplashImageUri(uri.toString()) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ============ 模块 1：个性化 ============
            item {
                Text("个性化", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    Column {
                        SettingRow(
                            title = "自定义启动封面",
                            subtitle = if (currentSplashUri == null) "当前：默认封面" else "当前：已自定义",
                            onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                        if (currentSplashUri != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Text(
                                text = "恢复默认封面",
                                modifier = Modifier.fillMaxWidth().clickable { coroutineScope.launch { prefsRepo.saveSplashImageUri(null) } }.padding(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchRow(title = "启动自动打开相机", subtitle = "开启后直接进入今日并拉起相机", checked = autoOpenCamera, onCheckedChange = { coroutineScope.launch { prefsRepo.saveAutoOpenCamera(it) } })
                    }
                }
            }

            // ============ 模块 2：播放控制 ============
            item {
                Text("播放控制", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    SwitchRow(
                        title = "全局自动播放",
                        subtitle = if (isGlobalAutoPlay) "所有日期默认开启" else "所有日期默认关闭",
                        checked = isGlobalAutoPlay,
                        onCheckedChange = { coroutineScope.launch { prefsRepo.setAllAutoPlay(it) } }
                    )
                }
            }

            // ============ 模块 3：天气与定位 ============
            item {
                Text("天气与定位", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    Column {
                        SettingRow("和风天气 API Key", if (weatherKey.isEmpty()) "未配置" else "已配置", { showWeatherKeyDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow("天气 API Host", if (weatherHost.isEmpty()) "未配置" else weatherHost, { showWeatherHostDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow(
                            title = "天气位置",
                            subtitle = when{
                                selectedCity.isNotEmpty() -> "当前：$selectedCity（手动）"
                                currentLatLng != null -> "当前：自动定位 (${currentLatLng!!.first}, ${currentLatLng!!.second})"
                                else -> "当前：尚未获取到定位"
                            },
                            onClick = { showCitySearchDialog = true },
                            trailing = {
                                if (selectedCity.isNotEmpty()) {
                                    IconButton(onClick = { coroutineScope.launch { prefsRepo.clearSelectedCity()
                                        val loc = LocationHelper.getCurrentLocation(context)
                                        if (loc != null) prefsRepo.saveWeatherLatLng(loc.first, loc.second)

                                    } }) {
                                        Icon(Icons.Default.Close, "清除", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // ============ 模块 4：AI 施工建议 ============
            item {
                Text("AI 施工建议", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    Column {
                        SwitchRow(
                            title = "启用 AI 生成建议",
                            subtitle = "关闭时使用本地规则引擎兜底",
                            checked = aiEnabled,
                            onCheckedChange = { coroutineScope.launch { prefsRepo.saveAiEnabled(it) } }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow("AI API Key", if (aiKey.isEmpty()) "未配置" else "已配置", { showAiKeyDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow("AI Base URL", aiBaseUrl, { showAiBaseUrlDialog = true })
                        SettingRow("AI 模型名称", aiModelName, { showAiModelDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow("混凝土养护提示词", "点击编辑", { showPromptConcreteDialog = true })
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SettingRow("钢结构合拢提示词", "点击编辑", { showPromptSteelDialog = true })
                    }
                }
            }
        }
    }

    // ================== 各种弹窗逻辑 ==================
    if (showWeatherKeyDialog) InputDialog("配置天气 API Key", weatherKey, { showWeatherKeyDialog = false }) { coroutineScope.launch { prefsRepo.saveWeatherApiKey(it) } }
    if (showWeatherHostDialog) InputDialog("配置天气 API Host", weatherHost, { showWeatherHostDialog = false }) { coroutineScope.launch { prefsRepo.saveWeatherApiHost(it) } }
    if (showAiKeyDialog) InputDialog("配置 AI API Key", aiKey, { showAiKeyDialog = false }) { coroutineScope.launch { prefsRepo.saveAiApiKey(it) } }
    if (showAiBaseUrlDialog) InputDialog("配置 AI Base URL", aiBaseUrl, { showAiBaseUrlDialog = false }) { coroutineScope.launch { prefsRepo.saveAiBaseUrl(it) } }
    if (showPromptConcreteDialog) InputDialog("编辑混凝土提示词", promptConcrete, { showPromptConcreteDialog = false }, multiLine = true) { coroutineScope.launch { prefsRepo.saveAiPromptConcrete(it) } }
    if (showPromptSteelDialog) InputDialog("编辑钢结构提示词", promptSteel, { showPromptSteelDialog = false }, multiLine = true) { coroutineScope.launch { prefsRepo.saveAiPromptSteel(it) } }
    if (showAiModelDialog) InputDialog("配置 AI 模型名称", aiModelName, { showAiModelDialog = false }) { coroutineScope.launch { prefsRepo.saveAiModelName(it) } }
    if (showCitySearchDialog) {
        val app = context.applicationContext as com.example.app_daily.CalendarApplication
        val weatherRepo = remember { WeatherRepository(app.database.weatherDao(), prefsRepo) }
        CitySearchDialog(
            prefsRepo = prefsRepo,
            weatherRepo = weatherRepo,
            onDismiss = { showCitySearchDialog = false }
        )
    }
}

// ================== 辅助组件 ==================

@Composable
fun SettingRow(title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun InputDialog(title: String, initialValue: String, onDismiss: () -> Unit, multiLine: Boolean = false, onSave: (String) -> Unit) {
    var tempValue by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = tempValue,
                onValueChange = { tempValue = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiLine,
                minLines = if (multiLine) 4 else 1
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tempValue.trim())
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchDialog(
    prefsRepo: UserPreferencesRepository,
    weatherRepo: WeatherRepository,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<WeatherRepository.CitySearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val apiKey by prefsRepo.weatherApiKeyFlow.collectAsState(initial = "")
    val apiHost by prefsRepo.weatherApiHostFlow.collectAsState(initial = "")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索城市") },
        text = {
            Column {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("输入汉字，如：上海") }, singleLine = true)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    if (searchQuery.isNotBlank()) {
                        coroutineScope.launch {
                            isSearching = true
                            searchResults = weatherRepo.searchCity(searchQuery, apiKey, apiHost)
                            isSearching = false
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (isSearching) "搜索中..." else "搜索") }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(searchResults) { city ->
                        Text(
                            text = city.name,
                            modifier = Modifier.fillMaxWidth().clickable {
                                coroutineScope.launch {
                                    prefsRepo.saveSelectedCity(city.name, city.lat, city.lon)
                                    onDismiss()
                                }
                            }.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}