package com.example.app_daily.ui.day

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.app_daily.CalendarApplication
import com.example.app_daily.data.local.entity.CardEntity
import com.example.app_daily.data.repository.CardRepository
import com.example.app_daily.data.repository.DiaryRepository
import com.example.app_daily.ui.day.components.DiarySection
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.absoluteValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import com.example.app_daily.data.repository.UserPreferencesRepository
import com.example.app_daily.data.repository.WeatherRepository

// 注意这里加了 ExperimentalFoundationApi，防止 combinedClickable 报错
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DayScreen(
    epochDay: Long,
    initialCardId: Long? = null,
    autoLaunchCamera: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CalendarApplication
    val svgImageLoader = remember {
        coil.ImageLoader.Builder(context)
            .components { add(coil.decode.SvgDecoder.Factory()) }
            .build()
    }
    val cardRepo = remember { CardRepository(app.database.cardDao()) }
    val diaryRepo = remember { DiaryRepository(app.database.diaryDao()) }

    val prefsRepo = remember { UserPreferencesRepository(context) }
    val weatherRepo = remember { WeatherRepository(app.database.weatherDao(), prefsRepo) }

    val viewModel: DayViewModel = viewModel(
        factory = DayViewModelFactory(cardRepo, diaryRepo,weatherRepo, prefsRepo, epochDay)
    )

    val cards by viewModel.cardsState.collectAsState()
    val diaries by viewModel.diariesState.collectAsState()
    val diaryMode by viewModel.diaryMode.collectAsState()
    val weather by viewModel.weatherState.collectAsState()

    val dateText = LocalDate.ofEpochDay(epochDay).toString()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val playSpeedMs by viewModel.playSpeedMs.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    var cardToDelete by remember { mutableStateOf<CardEntity?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) { e.printStackTrace() }

            // 判断是图片还是视频
            val mimeType = context.contentResolver.getType(uri)
            val isVideo = mimeType?.startsWith("video") == true
            val mediaType = if (isVideo) "VIDEO" else "PHOTO"

            var width = 1080
            var height = 1920 // 给个默认值

            if (!isVideo) {
                // 图片解析尺寸
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                width = options.outWidth
                height = options.outHeight
            } else {
                // 视频解析尺寸
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    width = w?.toIntOrNull() ?: 1080
                    height = h?.toIntOrNull() ?: 1920
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    retriever.release()
                }
            }

            viewModel.addMedia(uri.toString(), mediaType, width, height)
        }
    }

    // ===== 相机相关逻辑 =====
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraUri != null) {
            val uri = currentCameraUri!!
            // 获取尺寸
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            viewModel.addMedia(uri.toString(), "PHOTO", options.outWidth, options.outHeight)
        }
    }

    // 用 val 声明一个 lambda，避免不必要的重组
    val launchCamera = {
        val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        currentCameraUri = uri
        cameraLauncher.launch(uri)
    }

    // 启动时自动拉起相机
    LaunchedEffect(Unit) {
        if (autoLaunchCamera) {
            launchCamera()
        }
    }
    // =========================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically){
                        Text(dateText)
                        if (weather != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AsyncImage(
                                model = "file:///android_asset/weather_icons/${weather!!.iconCode}.svg",
                                imageLoader = svgImageLoader,
                                contentDescription = weather!!.condition,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            // 【核心修复】：把两个按钮放入 Column，并去掉多余的大括号
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp) // 两个按钮的间距
            ) {
                SmallFloatingActionButton(
                    onClick = { launchCamera() },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant, // 副色调
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "拍照")
                }

                FloatingActionButton(onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加照片")
                }
            }
        }
    ) { innerPadding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有卡片，点右下角添加照片吧！")
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % cards.size,
                pageCount = { Int.MAX_VALUE }
            )

            LaunchedEffect(cards, initialCardId) {
                if (cards.isNotEmpty() && initialCardId != null) {
                    val targetIndex = cards.indexOfFirst { it.id == initialCardId }
                    if (targetIndex != -1) {
                        // 计算目标页（因为无限循环）
                        val targetPage = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % cards.size + targetIndex
                        pagerState.scrollToPage(targetPage)
                    }
                }
            }

            LaunchedEffect(isPlaying, playSpeedMs) {
                if (isPlaying && cards.size > 1) {
                    while (true) {
                        delay(playSpeedMs)
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 500,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        )
                    }
                }
            }

            val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
            LaunchedEffect(isDragged) {
                if (isDragged && isPlaying) {
                    viewModel.togglePlayState(false) // 持久化关闭
                }
            }

            // 使用 Column + verticalScroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // 卡片区
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 600.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val realIndex = page % cards.size
                        val card = cards[realIndex]

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CardItem(
                                card = card,
                                isActive = page == pagerState.currentPage,
                                onToggleMode = { viewModel.toggleDisplayMode(card) },
                                onLongClick = { cardToDelete = card },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp)
                                    .graphicsLayer {
                                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                        val scale = 1f - (pageOffset * 0.2f).coerceIn(0f, 0.2f)
                                        val alpha = 1f - (pageOffset * 0.5f).coerceIn(0f, 1f)
                                        scaleX = scale
                                        scaleY = scale
                                        this.alpha = alpha
                                    }
                            )
                        }
                    }
                }

                // 日记区
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val realIndex = pagerState.currentPage % cards.size
                    DiarySection(
                        cards = cards,
                        diaries = diaries,
                        currentCardIndex = realIndex,
                        displayMode = diaryMode,
                        onModeChange = { viewModel.setDiaryMode(it) },
                        onSaveDiary = { content, cardId ->
                            viewModel.saveDiary(content, cardId)
                        }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("播放设置") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动播放")
                        Switch(
                            checked = isPlaying,
                            onCheckedChange = { viewModel.togglePlayState(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text("速度", style = MaterialTheme.typography.bodyLarge)

                        Spacer(modifier = Modifier.weight(1f))

                        Slider(
                            value = playSpeedMs.toFloat(),
                            onValueChange = { viewModel.changePlaySpeed(it.toLong()) },
                            valueRange = 500f..5000f,
                            modifier = Modifier.width(140.dp)
                                .height(24.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumb = {
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            },
                            track = { sliderState ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .align(Alignment.CenterStart)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    )

                                    val fraction = (sliderState.value - sliderState.valueRange.start) /
                                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(2.dp)
                                            .align(Alignment.CenterStart)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${String.format("%.1f", playSpeedMs / 1000f)}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    // ========== 删除确认弹窗 ==========
    if (cardToDelete != null) {
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("删除卡片") },
            text = { Text("确定要删除这张卡片吗？对应的日记也会一起删除哦。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cardToDelete?.let { viewModel.deleteCard(it) }
                        cardToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CardItem(
    card: CardEntity,
    isActive: Boolean,
    onToggleMode: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOriginal = card.displayMode == "ORIGINAL" && card.originalHeight > 0 && card.originalWidth > 0
    val aspectRatio = if (isOriginal) {
        card.originalWidth.toFloat() / card.originalHeight.toFloat()
    } else {
        1f
    }

    Card(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (card.mediaType == "VIDEO") {
                com.example.app_daily.ui.day.components.VideoPlayer(
                    videoUri = card.mediaUri,
                    isVisible = isActive,
                    onLongClick = onLongClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else {
                AsyncImage(
                    model = card.mediaUri,
                    contentDescription = "照片卡片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (isOriginal) ContentScale.Fit else ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                IconButton(onClick = onToggleMode) {
                    Text(
                        text = if (card.displayMode == "CARD") "原图" else "卡片",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}