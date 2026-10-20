package com.example.app_daily.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue // 必须有
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.app_daily.data.repository.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onTimeout: () -> Unit // 当倒计时结束时，通知外面跳转到主界面
) {
    val context = LocalContext.current
    val prefsRepo = UserPreferencesRepository(context)

    // 用来存放读取到的封面 Uri
    var splashUri by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var isLoading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    // 启动时，读取一次 DataStore
    LaunchedEffect(Unit) {
        val uri = prefsRepo.splashImageUriFlow.first() // 获取当前的值
        splashUri = uri
        isLoading = false
    }

    // 如果读取完毕了，就开始倒计时 2 秒，然后进入主界面
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(1000) // 2000 毫秒 = 2 秒
            onTimeout()
        }
    }

    // 画面显示
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // 使用你的 GitHub 风格底色
        contentAlignment = Alignment.BottomCenter
    ) {
        if (!isLoading) {
            if (splashUri != null) {
                // 如果有自定义封面，用 Coil 全屏铺满
                AsyncImage(
                    model = splashUri,
                    contentDescription = "启动封面",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // 裁剪填满全屏
                )
                Text(
                    text = "MyDaily",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 64.dp)
                )


            } else {
                // 如果没设置，显示默认文字（你可以换成 App 的 Logo 图标）
                Text(
                    text = "MyDaily",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 64.dp)
                )
            }
        }
    }
}