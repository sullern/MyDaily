package com.example.app_daily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.app_daily.ui.theme.APP_DailyTheme
import com.example.app_daily.ui.navigation.AppNavigation
import com.example.app_daily.ui.navigation.MainScreen
import com.example.app_daily.ui.SplashScreen

/**
 * MainActivity 是整个 App 的入口。
 * 它继承自 ComponentActivity，这是 Compose 时代最常用的 Activity 基类。
 */
class MainActivity : ComponentActivity() {
    /**
     * onCreate 是 Activity 的生命周期方法。
     * 系统创建这个屏幕时会调用它。你所有的初始化都写在这里。
     * savedInstanceState 用于屏幕旋转时恢复状态，现在不用管。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)// 必须调用父类的，别删
        // enableEdgeToEdge()：让内容延伸到状态栏/导航栏下面
        enableEdgeToEdge()
        // setContent {}：这是 Compose 的入口。
        // 大括号里写的所有东西都是"界面"，不是 XML，是 Kotlin 函数
        setContent {
            // APP_DailyTheme 是我们自己的主题（颜色、字体），
            // 定义在 ui/theme/Theme.kt 里
            APP_DailyTheme {
                var showSplash by remember { mutableStateOf(true) }
                // Scaffold 是一个"脚手架"布局，提供内边距等默认处理
                if (showSplash) {
                    // 显示启动页
                    SplashScreen(
                        onTimeout = { showSplash = false } // 2 秒后切换为 false
                    )
                } else {
                    // 显示主界面（包含底部导航）
                    MainScreen()
                }
            }
        }
    }
}
/**
 * @Composable 注解表示这是一个"界面函数"。
 * 这种函数的特点：
 *   1. 名字首字母大写（约定）
 *   2. 只描述界面长什么样，不写业务逻辑
 *   3. 可以被其它 @Composable 调用
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
/**
 * @Preview 让 Android Studio 右上角能预览这个函数的效果
 * 不需要运行整个 App 就能看到界面长什么样。
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    APP_DailyTheme {
        Greeting("Android")
    }
}