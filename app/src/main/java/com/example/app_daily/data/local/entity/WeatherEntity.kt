package com.example.app_daily.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 天气记录表。
 * 以 epochDay 为主键，保证每天只有一条记录（如果多次拉取，会覆盖更新）。
 */
@Entity(tableName = "weather_records")
data class WeatherEntity(
    @PrimaryKey
    val epochDay: Long,
    val maxTemp: Int,       // 最高温
    val minTemp: Int,       // 最低温
    val humidity: Int,      // 湿度（0-100）
    val condition: String,  // 天气状况（如：晴、多云）
    val iconCode: String,
    val updatedAt: Long     // 更新时间
)