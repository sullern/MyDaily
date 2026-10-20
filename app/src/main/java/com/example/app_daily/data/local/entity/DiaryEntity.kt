package com.example.app_daily.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diaries")
data class DiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // 关联的卡片 ID。可以为空（如果以后支持写“不配图的纯文字日记”）
    val cardId: Long?,
    val epochDay: Long,
    val content: String,
    val updatedAt: Long
)