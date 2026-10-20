package com.example.app_daily.ui.calendar
//管数据
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app_daily.data.local.entity.WeatherEntity
import com.example.app_daily.data.repository.CardRepository
import com.example.app_daily.data.repository.DiaryRepository
import com.example.app_daily.data.repository.UserPreferencesRepository
import com.example.app_daily.data.repository.WeatherRepository
import com.example.app_daily.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarViewModel(
    private val cardRepository: CardRepository,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModel() {

    // 日历打点逻辑（保持不变）
    val recordedDays: StateFlow<Set<Long>> = combine(
        cardRepository.getAllCardDays(),
        diaryRepository.getAllDiaryDays()
    ) { cardDays, diaryDays ->
        (cardDays + diaryDays).toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    // ====== 天气与 AI 建议状态 ======
    private val _currentWeather = MutableStateFlow<WeatherEntity?>(null)
    val currentWeather: StateFlow<WeatherEntity?> = _currentWeather.asStateFlow()

    private val _displayDate = MutableStateFlow(LocalDate.now())
    val displayDate: StateFlow<LocalDate> = _displayDate.asStateFlow()

    suspend fun loadWeatherForDate(epochDay: Long) {
        val date = LocalDate.ofEpochDay(epochDay)
        _displayDate.value = date

        // 如果是今天，就调用 syncWeather() 拉取最新网络数据
        if (date == LocalDate.now()) {
            syncWeather()
        } else {
            // 如果是历史日期，直接从本地数据库查缓存
            val cached = weatherRepository.getWeatherByDay(epochDay).first()
            _currentWeather.value = cached

            if (cached != null) {

                _concreteAdvice.value = "正在生成建议..."
                _steelAdvice.value = "正在生成建议..."
                generateAdvice(cached)
            } else {
                _concreteAdvice.value = "该日期无天气记录，无法生成建议。"
                _steelAdvice.value = "该日期无天气记录，无法生成建议。"
            }
        }
    }

    private val _concreteAdvice = MutableStateFlow<String>("正在生成建议...")
    val concreteAdvice: StateFlow<String> = _concreteAdvice.asStateFlow()

    private val _steelAdvice = MutableStateFlow<String>("正在生成建议...")
    val steelAdvice: StateFlow<String> = _steelAdvice.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow<Boolean>(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()

    init {
        // 【核心改动】：监听 API Key、API Host 和 手动城市的变化
        viewModelScope.launch {
            combine(
                prefsRepository.weatherApiKeyFlow,
                prefsRepository.weatherApiHostFlow,
                prefsRepository.selectedCityLatLonFlow
            ) { apiKey, apiHost, latLon ->
                Triple(apiKey, apiHost, latLon)
            }
                .distinctUntilChanged() // 现在比较的是 Triple，只要 Key、Host 或 城市有一个变化，就会触发
                .collect { (apiKey, apiHost, latLon) ->
                    if (apiKey.isNotBlank() && apiHost.isNotBlank()) {
                        delay(300) // 稍微等一下，防止连续弹窗导致多次触发
                        syncWeather()
                    }
                }
        }
    }

    fun syncWeather() {
        viewModelScope.launch {
            _isLoadingWeather.value = true

            val apiKey = prefsRepository.weatherApiKeyFlow.first()
            val apiHost = prefsRepository.weatherApiHostFlow.first()

            // 1. 获取优先级最高的经纬度
            val manualLatLon = prefsRepository.selectedCityLatLonFlow.first()
            val latLon = if (manualLatLon != null) {
                manualLatLon
            } else {
                val autoLoc = LocationHelper.getCurrentLocation(context)

                if (autoLoc != null) {
                    prefsRepository.saveWeatherLatLng(autoLoc.first, autoLoc.second)
                    autoLoc
                } else {

                    val cachedLatLon = prefsRepository.weatherLatLngFlow.first()
                    if (cachedLatLon != null) {
                        cachedLatLon
                    } else {
                        // 🚨 彻底没招了：没手动选，没开定位，也没历史坐标
                        _isLoadingWeather.value = false
                        _concreteAdvice.value = "无法获取定位，请在设置中开启定位权限或手动搜索城市。"
                        _steelAdvice.value = "无法获取定位，请在设置中开启定位权限或手动搜索城市。"
                        _currentWeather.value = null
                        return@launch
                    }
                }
            }

            // 2. 拉取并保存天气
            val weather = weatherRepository.fetchDailyWeather(apiKey, apiHost, latLon.first, latLon.second)

            if (weather != null) {
                _currentWeather.value = weather
                generateAdvice(weather)
            } else {
                // 如果网络失败，尝试读取本地缓存
                val todayEpoch = LocalDate.now().toEpochDay()
                val cached = weatherRepository.getWeatherByDay(todayEpoch).first()
                if (cached != null) {
                    _currentWeather.value = cached
                    generateAdvice(cached)
                } else {
                    _concreteAdvice.value = "无法获取天气数据，请检查设置。"
                    _steelAdvice.value = "无法获取天气数据，请检查设置。"
                }
            }
            _isLoadingWeather.value = false
        }
    }

    private suspend fun generateAdvice(weather: WeatherEntity) {
        _concreteAdvice.value = weatherRepository.getAdvice(weather, isConcrete = true)
        _steelAdvice.value = weatherRepository.getAdvice(weather, isConcrete = false)
    }
}


// 工厂类（保持不变）
class CalendarViewModelFactory(
    private val cardRepository: CardRepository,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CalendarViewModel(
            cardRepository,
            diaryRepository,
            weatherRepository,
            prefsRepository,
            context
        ) as T
    }
}

//CalendarScreen（Composable）
//│
//│ 手里有：cardRepo、diaryRepo、weatherRepo、prefsRepo、context
//│
//▼
//CalendarViewModelFactory(cardRepo, diaryRepo, weatherRepo, prefsRepo, context)
//│  ← 这 5 个值被存进工厂对象的 5 个私有字段
//│
//│ 调用 viewModel(factory = 这个工厂)
//│  → 缓存里有 CalendarViewModel 吗？
//│      有 → 直接返回旧的，工厂根本不用出场
//│      没有 → 调用工厂的 create()
//▼
//create(CalendarViewModel::class.java)
//│
//│  return CalendarViewModel(cardRepo, diaryRepo, weatherRepo, prefsRepo, context) as T
//│  ← 把 5 个字段原样转交给 ViewModel 的构造函数
//▼
//CalendarViewModel 实例
//│  （它的构造函数正是接收这 5 个参数）
//▼
//被存进 ViewModelStore，之后 Screen 里通过 viewModel.cardsState 等访问