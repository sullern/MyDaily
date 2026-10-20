package com.example.app_daily.data.repository

import com.example.app_daily.data.local.dao.WeatherDao
import com.example.app_daily.data.local.entity.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeatherRepository(
    private val weatherDao: WeatherDao,
    private val prefsRepository: UserPreferencesRepository
) {
    private val client = OkHttpClient()

    fun getWeatherByDay(epochDay: Long): Flow<WeatherEntity?> = weatherDao.getWeatherByDay(epochDay)

    // 1. 搜索城市
    suspend fun searchCity(keyword: String, apiKey: String, apiHost: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiHost.isBlank() || keyword.isBlank()) return@withContext emptyList()
        try {
            val cleanHost = apiHost.removePrefix("https://").removePrefix("http://")
            val url = "https://$cleanHost/geo/v2/city/lookup".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("location", keyword)?.build() ?: return@withContext emptyList()
            val request = Request.Builder().url(url).addHeader("X-QW-Api-Key", apiKey).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val json = JSONObject(response.body?.string() ?: "")
            if (json.getString("code") == "200") {
                val locations = json.getJSONArray("location")
                val results = mutableListOf<CitySearchResult>()
                for (i in 0 until locations.length()) {
                    val loc = locations.getJSONObject(i)
                    val adm1 = loc.optString("adm1", "")
                    val name = loc.getString("name")
                    val displayName = if (adm1.isNotBlank() && adm1 != name) "$adm1 $name" else name
                    results.add(CitySearchResult(displayName, loc.getString("lat"), loc.getString("lon")))
                }
                results
            } else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // 2. 获取每日天气并保存
    suspend fun fetchDailyWeather(apiKey: String, apiHost: String, lat: String, lon: String): WeatherEntity? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiHost.isBlank()) {
            android.util.Log.e("WeatherRepo", "❌ API Key 或 Host 为空")
            return@withContext null
        }
        try {
            val cleanHost = apiHost.removePrefix("https://").removePrefix("http://")
            val url = "https://$cleanHost/weather/v1/daily/$lat/$lon"

            // 打印出真实的请求地址，方便核对
            android.util.Log.d("WeatherRepo", "📡 正在请求: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("X-QW-Api-Key", apiKey)
                .build()

            val response = client.newCall(request).execute()

            // 如果 HTTP 状态码不是 200，打印出来
            if (!response.isSuccessful) {
                android.util.Log.e("WeatherRepo", "❌ HTTP请求失败, 状态码: ${response.code}, 信息: ${response.message}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: ""
            android.util.Log.d("WeatherRepo", "✅ 收到响应: $bodyString")

            val json = JSONObject(bodyString)

            // 检查和风天气的返回码
            if (json.has("code") && json.getString("code") != "200") {
                android.util.Log.e("WeatherRepo", "❌ 和风天气API报错, code: ${json.getString("code")}")
                return@withContext null
            }

            val days = json.getJSONArray("days")
            if (days.length() == 0) {
                android.util.Log.e("WeatherRepo", "❌ API返回的 days 数组为空")
                return@withContext null
            }

            val today = days.getJSONObject(0)
            val maxTemp = today.getJSONObject("temperatureMax").getDouble("value").toInt()
            val minTemp = today.getJSONObject("temperatureMin").getDouble("value").toInt()
            val humidity = (today.getJSONObject("daytime").getDouble("humidity") * 100).toInt()
            val condition = today.getJSONObject("daytime").getJSONObject("condition").getString("text")
            val iconCode = today.getJSONObject("daytime").getJSONObject("condition").getString("code")
            val todayEpoch = LocalDate.now().toEpochDay()
            val entity = WeatherEntity(
                epochDay = todayEpoch,
                maxTemp = maxTemp,
                minTemp = minTemp,
                humidity = humidity,
                condition = condition,
                iconCode = iconCode, // 👈 存入数据库
                updatedAt = System.currentTimeMillis()
            )

            weatherDao.insertWeather(entity)
            weatherDao.deleteOldWeather(todayEpoch - 30)

            android.util.Log.d("WeatherRepo", "🎉 天气保存成功: $condition, $minTemp~$maxTemp℃")
            entity
        } catch (e: Exception) {
            android.util.Log.e("WeatherRepo", "💥 发生异常: ${e.message}")
            e.printStackTrace() // 打印完整的堆栈信息
            null
        }
    }
    // 3. 生成 AI 建议（含本地降级）
    // 3. 生成 AI 建议（含本地降级）
    suspend fun getAdvice(weather: WeatherEntity, isConcrete: Boolean): String = withContext(Dispatchers.IO) {

        // 直接从 Flow 里取第一个值
        val aiEnabled = prefsRepository.aiEnabledFlow.first()
        val aiKey = prefsRepository.aiApiKeyFlow.first()
        val aiBaseUrl = prefsRepository.aiBaseUrlFlow.first()
        val aiModel = prefsRepository.aiModelNameFlow.first()
        val promptFlow = if (isConcrete) {
            prefsRepository.aiPromptConcreteFlow
        } else {
            prefsRepository.aiPromptSteelFlow
        }
        val promptTemplate = promptFlow.first()

        val weatherStr = "天气:${weather.condition}, 温度:${weather.minTemp}~${weather.maxTemp}℃, 湿度:${weather.humidity}%"

        android.util.Log.d("AiRepo", "--- 开始生成建议 ---")
        android.util.Log.d("AiRepo", "AI开关: $aiEnabled, Key是否为空: ${aiKey.isBlank()}, BaseUrl: $aiBaseUrl, Model: $aiModel")

        // 尝试 AI 生成
        if (aiEnabled && aiKey.isNotBlank()) {
            android.util.Log.d("AiRepo", "✅ 满足条件，准备发起 AI 请求！")
            try {
                val prompt = promptTemplate.replace("{weather}", weatherStr)
                val jsonBody = JSONObject().apply {
                    put("model", aiModel)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }))
                    put("temperature", 0.3)
                }

                android.util.Log.d("AiRepo", "请求 URL: ${aiBaseUrl.trimEnd('/')}/chat/completions")

                val request = Request.Builder()
                    .url("${aiBaseUrl.trimEnd('/')}/chat/completions")
                    .addHeader("Authorization", "Bearer $aiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()

                val resBody = response.body?.string() ?: ""
                android.util.Log.d("AiRepo", "HTTP 状态码: ${response.code}, 返回: $resBody")

                if (response.isSuccessful) {
                    val resJson = JSONObject(resBody)
                    val content = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    return@withContext content.trim()
                }
            } catch (e: Exception) {
                android.util.Log.e("AiRepo", "💥 请求异常: ${e.message}")
                e.printStackTrace()
            }
        } else {
            // 🚨 强效日志 3：为什么没进去？
            android.util.Log.e("AiRepo", "❌ 没进条件：开关是 $aiEnabled，Key是空的吗？${aiKey.isBlank()}")
        }

        // AI 不可用或失败，走本地规则降级
        if (isConcrete) {
            when {
                weather.minTemp < 5 -> "气温低于5℃，应覆盖保温，严禁洒水养护。"
                weather.humidity < 50 -> "空气干燥，需加强洒水或覆盖保湿。"
                else -> "温湿度适宜，进行正常养护即可。"
            }
        } else {
            val avgTemp = (weather.maxTemp + weather.minTemp) / 2
            if (avgTemp in 15..23) "当前温度适宜合拢，注意风速影响。"
            else "温度不在15~23℃区间，建议等待最佳温度合拢。"
        }
    }

    data class CitySearchResult(val name: String, val lat: String, val lon: String)
}