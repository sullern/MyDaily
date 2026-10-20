package com.example.app_daily

import android.app.Application
import androidx.room.Room
import com.example.app_daily.data.local.AppDatabase
import com.example.app_daily.data.local.entity.CardEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CalendarApplication : Application() {

    // 数据库实例。用 lateinit 表示“稍后初始化”。
    lateinit var database: AppDatabase
        private set // 只允许在这个类内部修改

    override fun onCreate() {
        super.onCreate()

        // 1. 构建数据库实例
        // Room.databaseBuilder 需要：上下文、数据库类、数据库文件名
        // fallbackToDestructiveMigration() 表示：如果版本号变了且没写迁移逻辑，
        // 直接清空数据库重建（开发阶段很方便，上线前要去掉）。
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "calendar_database"
        ).fallbackToDestructiveMigration().build()

        // 2. 测试：往数据库插一条假数据
        // 数据库操作必须在子线程（后台线程）进行，所以我们用 CoroutineScope 启动一个协程。
        CoroutineScope(Dispatchers.IO).launch {
            val testCard = CardEntity(
                epochDay = 0, // 1970-01-01
                mediaUri = "content://test/photo/1",
                mediaType = "PHOTO",
                displayMode = "CARD",
                originalWidth = 1080,
                originalHeight = 1920,
                orderIndex = 0,
                createdAt = System.currentTimeMillis()
            )

            // 插入数据库
            database.cardDao().insert(testCard)

            // 再从数据库读出来，打印日志，验证是否成功
            database.cardDao().getAllCards().collect { cards ->
                android.util.Log.d("DB_TEST", "数据库里的卡片数量: ${cards.size}")
                cards.forEach {
                    android.util.Log.d("DB_TEST", "卡片ID: ${it.id}, Uri: ${it.mediaUri}, 尺寸: ${it.originalWidth}x${it.originalHeight}")
                }
            }
        }
    }
}