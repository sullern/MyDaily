package com.example.app_daily.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.app_daily.data.local.dao.CardDao
import com.example.app_daily.data.local.dao.DiaryDao
import com.example.app_daily.data.local.dao.WeatherDao
import com.example.app_daily.data.local.entity.CardEntity
import com.example.app_daily.data.local.entity.DiaryEntity
import com.example.app_daily.data.local.entity.WeatherEntity
/**
 * @Database 告诉 Room：这是整个数据库的入口。
 * entities = [CardEntity::class] 表示数据库里包含哪些表（现在只有 cards 表）。
 * version = 1 是数据库版本号。以后修改表结构（比如加字段），需要把版本号 +1，
 * 并且提供 Migration，否则 App 会崩溃。
 */
@Database(entities = [CardEntity::class,DiaryEntity::class, WeatherEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 把刚才写的 Dao 暴露出来，外部通过 db.cardDao() 就能操作数据库
    abstract fun cardDao(): CardDao

    // 稍后我们会在 Application 类里创建这个数据库的单例实例
    abstract fun diaryDao(): DiaryDao

    abstract fun weatherDao(): WeatherDao // 加了 WeatherDao
}