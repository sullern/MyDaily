package com.example.app_daily.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    // ================= 原有设置 =================
    private val DIARY_MODE_KEY = stringPreferencesKey("diary_display_mode")
    private val SPLASH_URI_KEY = stringPreferencesKey("splash_image_uri")
    private val PLAY_SPEED_KEY = longPreferencesKey("play_speed_ms")
    private val GLOBAL_AUTO_PLAY_KEY = booleanPreferencesKey("global_default_auto_play")
    private val AUTO_OPEN_CAMERA_KEY = booleanPreferencesKey("auto_open_camera")

    val diaryModeFlow: Flow<String> = context.dataStore.data.map { it[DIARY_MODE_KEY] ?: "PER_CARD" }
    suspend fun saveDiaryMode(mode: String) = context.dataStore.edit { it[DIARY_MODE_KEY] = mode }

    val splashImageUriFlow: Flow<String?> = context.dataStore.data.map { it[SPLASH_URI_KEY] }
    suspend fun saveSplashImageUri(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(SPLASH_URI_KEY) else it[SPLASH_URI_KEY] = uri
    }

    val autoOpenCameraFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_OPEN_CAMERA_KEY] ?: false }
    suspend fun saveAutoOpenCamera(isOpen: Boolean) = context.dataStore.edit { it[AUTO_OPEN_CAMERA_KEY] = isOpen }

    val playSpeedFlow: Flow<Long> = context.dataStore.data.map { it[PLAY_SPEED_KEY] ?: 1000L }
    suspend fun savePlaySpeed(speed: Long) = context.dataStore.edit { it[PLAY_SPEED_KEY] = speed }

    val globalAutoPlayFlow: Flow<Boolean> = context.dataStore.data.map { it[GLOBAL_AUTO_PLAY_KEY] ?: false }

    fun getAutoPlayFlow(epochDay: Long): Flow<Boolean> {
        val key = booleanPreferencesKey("auto_play_$epochDay")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: preferences[GLOBAL_AUTO_PLAY_KEY] ?: false
        }
    }

    suspend fun saveAutoPlay(epochDay: Long, isPlaying: Boolean) {
        val key = booleanPreferencesKey("auto_play_$epochDay")
        context.dataStore.edit { preferences -> preferences[key] = isPlaying }
    }

    suspend fun setAllAutoPlay(isPlaying: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GLOBAL_AUTO_PLAY_KEY] = isPlaying
            val keysToRemove = preferences.asMap().keys.filter { it.name.startsWith("auto_play_") }
            keysToRemove.forEach { preferences.remove(it) }
        }
    }

    // ================= 新增：天气与定位 =================
    private val WEATHER_API_KEY = stringPreferencesKey("weather_api_key")
    private val WEATHER_API_HOST = stringPreferencesKey("weather_api_host")
    private val WEATHER_LATITUDE = stringPreferencesKey("weather_latitude")
    private val WEATHER_LONGITUDE = stringPreferencesKey("weather_longitude")

    private val SELECTED_CITY_NAME = stringPreferencesKey("selected_city_name")
    private val SELECTED_CITY_LAT = stringPreferencesKey("selected_city_lat")
    private val SELECTED_CITY_LON = stringPreferencesKey("selected_city_lon")

    val weatherApiKeyFlow: Flow<String> = context.dataStore.data.map { it[WEATHER_API_KEY] ?: "" }
    suspend fun saveWeatherApiKey(key: String) = context.dataStore.edit { it[WEATHER_API_KEY] = key.trim() }

    val weatherApiHostFlow: Flow<String> = context.dataStore.data.map { it[WEATHER_API_HOST] ?: "" }
    suspend fun saveWeatherApiHost(host: String) = context.dataStore.edit { it[WEATHER_API_HOST] = host.trim() }

    val weatherLatLngFlow: Flow<Pair<String, String>?> = context.dataStore.data.map {
        val lat = it[WEATHER_LATITUDE]
        val lon = it[WEATHER_LONGITUDE]
        // 只有两个都有值才返回，否则返回 null
        if (lat != null && lon != null) Pair(lat, lon) else null
    }
    suspend fun saveWeatherLatLng(lat: String, lon: String) = context.dataStore.edit {
        it[WEATHER_LATITUDE] = lat
        it[WEATHER_LONGITUDE] = lon
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_CITY_NAME] ?: "" }
    val selectedCityLatLonFlow: Flow<Pair<String, String>?> = context.dataStore.data.map {
        val name = it[SELECTED_CITY_NAME]
        val lat = it[SELECTED_CITY_LAT]
        val lon = it[SELECTED_CITY_LON]
        if (!name.isNullOrBlank() && !lat.isNullOrBlank() && !lon.isNullOrBlank()) Pair(lat, lon) else null
    }
    suspend fun saveSelectedCity(name: String, lat: String, lon: String) = context.dataStore.edit {
        it[SELECTED_CITY_NAME] = name
        it[SELECTED_CITY_LAT] = lat
        it[SELECTED_CITY_LON] = lon
    }
    suspend fun clearSelectedCity() = context.dataStore.edit {
        it.remove(SELECTED_CITY_NAME)
        it.remove(SELECTED_CITY_LAT)
        it.remove(SELECTED_CITY_LON)
    }

    // ================= 新增：AI 施工建议 =================
    private val AI_ENABLED = booleanPreferencesKey("ai_enabled")
    private val AI_API_KEY = stringPreferencesKey("ai_api_key")
    private val AI_BASE_URL = stringPreferencesKey("ai_base_url")
    private val AI_PROMPT_CONCRETE = stringPreferencesKey("ai_prompt_concrete")
    private val AI_PROMPT_STEEL = stringPreferencesKey("ai_prompt_steel")

    private val AI_MODEL_NAME = stringPreferencesKey("ai_model_name")

    val aiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AI_ENABLED] ?: false }
    suspend fun saveAiEnabled(enabled: Boolean) = context.dataStore.edit { it[AI_ENABLED] = enabled }

    val aiApiKeyFlow: Flow<String> = context.dataStore.data.map { it[AI_API_KEY] ?: "" }
    suspend fun saveAiApiKey(key: String) = context.dataStore.edit { it[AI_API_KEY] = key.trim() }

    val aiBaseUrlFlow: Flow<String> = context.dataStore.data.map { it[AI_BASE_URL] ?: "https://api.deepseek.com/v1" }
    suspend fun saveAiBaseUrl(url: String) = context.dataStore.edit { it[AI_BASE_URL] = url.trim() }

    // 默认提示词
    val aiPromptConcreteFlow: Flow<String> = context.dataStore.data.map {
        it[AI_PROMPT_CONCRETE] ?: "你是一个严谨的土木工程专家。根据以下实时天气数据：{weather}。请判断今日混凝土养护的注意事项。要求：字数不超过30字，直接给出结论和具体操作（如：覆盖保温、洒水保湿、正常养护等），严禁长篇大论。"
    }
    suspend fun saveAiPromptConcrete(prompt: String) = context.dataStore.edit { it[AI_PROMPT_CONCRETE] = prompt }

    val aiPromptSteelFlow: Flow<String> = context.dataStore.data.map {
        it[AI_PROMPT_STEEL] ?: "你是一个严谨的钢结构工程专家。根据以下实时天气数据：{weather}。请判断今日是否适合钢结构合拢。要求：字数不超过30字，判断当前温度是否在15℃~23℃的合拢最佳区间，直接给出合拢或等待的结论，严禁长篇大论。"
    }
    suspend fun saveAiPromptSteel(prompt: String) = context.dataStore.edit { it[AI_PROMPT_STEEL] = prompt }

    val aiModelNameFlow: Flow<String> = context.dataStore.data.map {
        it[AI_MODEL_NAME] ?: "deepseek-flash"
    }
    // 保存模型名称
    suspend fun saveAiModelName(name: String) = context.dataStore.edit {
        it[AI_MODEL_NAME] = name.trim()
    }
}