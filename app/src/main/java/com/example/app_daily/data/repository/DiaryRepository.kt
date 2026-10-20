package com.example.app_daily.data.repository

import com.example.app_daily.data.local.dao.DiaryDao
import com.example.app_daily.data.local.entity.DiaryEntity
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {
    fun getDiariesByDay(epochDay: Long): Flow<List<DiaryEntity>> = diaryDao.getDiariesByDay(epochDay)
    fun getDiaryByCard(cardId: Long): Flow<DiaryEntity?> = diaryDao.getDiaryByCard(cardId)
    suspend fun saveDiary(diary: DiaryEntity) = diaryDao.insertOrUpdate(diary)
    fun searchDiaries(query: String): Flow<List<DiaryEntity>> = diaryDao.searchDiaries(query)

    suspend fun deleteDiaryByCardId(cardId: Long) {
        diaryDao.deleteByCardId(cardId)
    }
    fun getAllDiaryDays(): Flow<List<Long>> = diaryDao.getAllDiaryDays()
}