package com.example.app_daily.data.repository

import com.example.app_daily.data.local.dao.CardDao
import com.example.app_daily.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository（仓库）的作用是给 ViewModel 提供数据。
 * 它不关心界面，只关心数据从哪来（数据库/网络）。
 */

//为什么有的函数时suspend fun，有的时fun
//fun是使用函数不耗时的时候，耗时的时候就用suspend fun 意思是我这里可能会耗时，让线程去干点别的，别傻等
class CardRepository(private val cardDao: CardDao){
//Flow是类型，是持续发送的数据流
    fun getCardsByDay(epochDay: Long):Flow<List<CardEntity>>{
        return cardDao.getCardsByDay(epochDay)//将调用原封不动的转给Dao
    }
    suspend fun insertCard(card: CardEntity){
        cardDao.insert(card);
    }
    suspend fun updateDisplayMode(id: Long, mode: String) {
        cardDao.updateDisplayMode(id, mode)
    }
    suspend fun deleteCard(id: Long) {
        cardDao.deleteCardById(id)
    }
    fun getAllCardDays(): Flow<List<Long>> = cardDao.getAllCardDays()

}