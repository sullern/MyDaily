package com.example.app_daily.data.local.dao

import androidx.room.Dao//@Dao是定义怎么增删改查的
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app_daily.data.local.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(diary: DiaryEntity)

    // 查询某一天的所有日记
    @Query("SELECT * FROM diaries WHERE epochDay = :epochDay ORDER BY updatedAt ASC")
    fun getDiariesByDay(epochDay: Long): Flow<List<DiaryEntity>>

    // 查询某张卡片对应的日记
    @Query("SELECT * FROM diaries WHERE cardId = :cardId LIMIT 1")
    fun getDiaryByCard(cardId: Long): Flow<DiaryEntity?>

    // 删除某张卡片时，顺便删除它的日记（可选，为了数据整洁）
    @Query("DELETE FROM diaries WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("SELECT * FROM diaries WHERE content LIKE '%' || :query || '%' ORDER BY epochDay DESC")
    fun searchDiaries(query: String): Flow<List<DiaryEntity>>

    // 查询所有有日记的日期（去重）
    @Query("SELECT DISTINCT epochDay FROM diaries")
    fun getAllDiaryDays(): Flow<List<Long>>
}