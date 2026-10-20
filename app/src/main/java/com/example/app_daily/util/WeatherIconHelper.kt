package com.example.app_daily.util

object WeatherIconHelper {
    /**
     * 返回 assets 文件夹里图片的路径。
     * 如果 API 返回了 code 比如 "100"，那就拼成 "file:///android_asset/weather_icons/100.png"
     */
    fun getIconPath(iconCode: String): String {
        // 如果 code 是空的，给个默认的晴天图（假设你有 100.png）
        val safeCode = if (iconCode.isBlank()) "100" else iconCode
        return "file:///android_asset/weather_icons/$safeCode.svg"
    }
}