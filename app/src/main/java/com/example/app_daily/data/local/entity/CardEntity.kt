package com.example.app_daily.data.local.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
/**
 * @Entity 告诉 Room：这是一个数据库表。
 * tableName = "cards" 指定表名叫 cards。不写的话默认是类名。
 */
@Entity(tableName = "cards")
data class CardEntity (
// @PrimaryKey(autoGenerate = true) 表示 id 是主键，且自动增长。
    // 每次插入一条新数据，id 会自动 +1。
    @PrimaryKey(autoGenerate = true)
    val id: Long=0,//0表示未赋值，数据库是从1开始的主键，0是默认值

        // 所属日期。存的是从 1970-01-01 开始算的天数，方便排序和查询。
    val epochDay: Long,

    // 图片或视频的 Uri 字符串（比如 content://media/external/...）
    val mediaUri: String,

    // 媒体类型："PHOTO" 或 "VIDEO"
    val mediaType: String,

    // 显示模式："CARD"（卡片尺寸） 或 "ORIGINAL"（原图尺寸）
    val displayMode: String,

    // 原图的宽高，用来支持“按原图尺寸显示”这个功能
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,

    // 当天的排序序号，用来控制卡片顺序
    val orderIndex: Int,

    // 创建时间戳
    val createdAt: Long
)

//这里边表示的都是表中的字段，也就是每一个变量都是表中的一列，横着的一行表示一条记录

//class CardEntity { }：这是普通类。大括号里只能写方法、成员变量（不能带逗号）。
//
//data class CardEntity( ... )：这是数据类。必须有 ()，小括号里是主构造函数的参数。属性之间用逗号 , 隔开，最后一个参数后面不加逗号（虽然较新的 Kotlin 允许尾随逗号，但为了不出错，建议不加）