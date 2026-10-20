package com.example.app_daily.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app_daily.data.local.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    // 插入或更新天气（如果这天已经有记录，直接覆盖）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    // 获取某天的天气
    @Query("SELECT * FROM weather_records WHERE epochDay = :epochDay")
    fun getWeatherByDay(epochDay: Long): Flow<WeatherEntity?>

    // 【核心修改】：清理30天前的数据，但如果那天有日记（diaries）或卡片（cards），则保留！
    @Query("""
        DELETE FROM weather_records 
        WHERE epochDay < :limitDay 
        AND epochDay NOT IN (SELECT DISTINCT epochDay FROM diaries)
        AND epochDay NOT IN (SELECT DISTINCT epochDay FROM cards)
    """)
    suspend fun deleteOldWeather(limitDay: Long)
}