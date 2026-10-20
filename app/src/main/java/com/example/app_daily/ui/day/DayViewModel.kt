package com.example.app_daily.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app_daily.data.repository.CardRepository
import com.example.app_daily.data.local.entity.CardEntity
import com.example.app_daily.data.local.entity.DiaryEntity
import com.example.app_daily.data.local.entity.WeatherEntity
import com.example.app_daily.data.repository.DiaryRepository
import com.example.app_daily.data.repository.UserPreferencesRepository
import com.example.app_daily.data.repository.WeatherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayViewModel(
    private val cardRepository: CardRepository,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository,
    private val prefsRepository: UserPreferencesRepository, // 新增
    private val epochDay: Long
) : ViewModel() {

    // 卡片流
    val cardsState: StateFlow<List<CardEntity>> = cardRepository.getCardsByDay(epochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当天所有日记流
    val diariesState: StateFlow<List<DiaryEntity>> = diaryRepository.getDiariesByDay(epochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weatherState: StateFlow<WeatherEntity?> = weatherRepository.getWeatherByDay(epochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    // 暴露日记模式给 UI（默认 PER_CARD）
    val diaryMode = prefsRepository.diaryModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "PER_CARD"
    )
    // 读取这一天的自动播放状态
    val isPlaying: StateFlow<Boolean> = prefsRepository.getAutoPlayFlow(epochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 读取全局播放速度
    val playSpeedMs: StateFlow<Long> = prefsRepository.playSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1000L)

    // 保存日记模式
    fun setDiaryMode(mode: String) {
        viewModelScope.launch {
            prefsRepository.saveDiaryMode(mode)
        }
    }
    fun togglePlayState(newState: Boolean) {
        viewModelScope.launch {
            prefsRepository.saveAutoPlay(epochDay, newState)
        }
    }

    // 改变播放速度并保存
    fun changePlaySpeed(speed: Long) {
        viewModelScope.launch {
            prefsRepository.savePlaySpeed(speed)
        }
    }
    // 保存/更新日记
    fun saveDiary(content: String, cardId: Long?) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val existing = diariesState.value.find { it.cardId == cardId }
            val diary = existing?.copy(content = content, updatedAt = System.currentTimeMillis())
                ?: DiaryEntity(
                    cardId = cardId,
                    epochDay = epochDay,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            diaryRepository.saveDiary(diary)
        }
    }

    fun addMedia(uriString: String, mediaType: String, width: Int, height: Int) {
        viewModelScope.launch {
            val newCard = CardEntity(
                epochDay = epochDay,
                mediaUri = uriString,
                mediaType = mediaType,
                displayMode = "CARD",
                originalWidth = width,
                originalHeight = height,
                orderIndex = cardsState.value.size,
                createdAt = System.currentTimeMillis()
            )
            cardRepository.insertCard(newCard)
        }
    }

    fun toggleDisplayMode(card: CardEntity) {
        val newMode = if (card.displayMode == "CARD") "ORIGINAL" else "CARD"
        viewModelScope.launch {
            cardRepository.updateDisplayMode(card.id, newMode)
        }
    }

    // 删除卡片
    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            diaryRepository.deleteDiaryByCardId(card.id)
            cardRepository.deleteCard(card.id)
        }
    }

}

class DayViewModelFactory(
    private val cardRepository: CardRepository,
    private val diaryRepository: DiaryRepository,
    private val weatherRepository: WeatherRepository, // 👈 新增
    private val prefsRepository: UserPreferencesRepository,
    private val epochDay: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return DayViewModel(cardRepository, diaryRepository, weatherRepository, prefsRepository, epochDay) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
