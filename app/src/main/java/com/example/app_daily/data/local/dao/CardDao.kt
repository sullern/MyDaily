package com.example.app_daily.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app_daily.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

/**
 * @Dao 标记这是一个数据访问接口。
 * Room 会自动实现里面的方法，不需要你写 SQL 的底层代码。
 */
@Dao
interface CardDao {
    /**
     * 插入一条卡片数据。
     * OnConflictStrategy.REPLACE 表示如果主键冲突（比如 id 一样），就覆盖旧数据。
     * suspend 关键字表示这是一个挂起函数，必须在协程里调用，避免卡住界面。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity)

    /**
     * 查询某一天的所有卡片。
     * :epochDay 是占位符，会自动替换成方法参数里的 epochDay。
     * ORDER BY orderIndex ASC 表示按 orderIndex 升序排列。
     * 返回值是 Flow<List<CardEntity>>，这意味着：以后数据库里 cards 表发生了变化，
     * 这个 Flow 会自动发出最新的数据，界面可以自动刷新，非常强大！
     */
    @Query("SELECT * FROM cards WHERE epochDay = :epochDay ORDER BY orderIndex ASC")
    fun getCardsByDay(epochDay: Long): Flow<List<CardEntity>>

    /**
     * 查询数据库里所有的卡片（现在用于测试）。
     */
    @Query("SELECT * FROM cards")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("UPDATE cards SET displayMode = :mode WHERE id = :id")
    suspend fun updateDisplayMode(id: Long, mode: String)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("SELECT DISTINCT epochDay FROM cards")
    fun getAllCardDays(): Flow<List<Long>>

}