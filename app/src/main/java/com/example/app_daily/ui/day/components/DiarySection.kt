package com.example.app_daily.ui.day.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app_daily.data.local.entity.CardEntity
import com.example.app_daily.data.local.entity.DiaryEntity

@Composable
fun DiarySection(
    cards: List<CardEntity>,
    diaries: List<DiaryEntity>,
    currentCardIndex: Int,
    displayMode: String, // 新增：接收模式
    onModeChange: (String) -> Unit, // 新增：改变模式回调
    onSaveDiary: (content: String, cardId: Long?) -> Unit
) {


    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {

        // 顶部模式切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("日记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "跟随卡片",
                    color = if (displayMode == "PER_CARD") MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (displayMode == "PER_CARD") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier  = Modifier.clickable { onModeChange("PER_CARD") }
                )
                Text(
                    text = "整天记录",
                    color = if (displayMode == "PER_DAY") MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (displayMode == "PER_DAY") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onModeChange("PER_DAY") } // 改为回调
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))

        // 内容区
        if (displayMode == "PER_CARD") {
            PerCardDiaryView(cards, diaries, currentCardIndex, onSaveDiary)
        } else {
            PerDayDiaryView(cards, diaries, onSaveDiary)
        }
    }
}

@Composable
fun PerCardDiaryView(
    cards: List<CardEntity>,
    diaries: List<DiaryEntity>,
    currentCardIndex: Int,
    onSaveDiary: (content: String, cardId: Long?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val currentCard = cards.getOrNull(currentCardIndex)
    val currentDiary = diaries.find { it.cardId == currentCard?.id }

    var textState by remember(currentCard?.id, currentDiary?.content) {
        mutableStateOf(currentDiary?.content ?: "")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "卡片 ${currentCardIndex + 1} / ${cards.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicTextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onSaveDiary(textState, currentCard?.id)
                    }
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSaveDiary(textState, currentCard?.id)
                    focusManager.clearFocus()
                }
            ),
            decorationBox = { innerTextField ->
                if (textState.isEmpty()) {
                    Text("写点什么...", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun PerDayDiaryView(
    cards: List<CardEntity>,
    diaries: List<DiaryEntity>,
    onSaveDiary: (content: String, cardId: Long?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var newTextState by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 使用普通的 Column 遍历展示，避免内部滚动冲突
        Column(modifier = Modifier.fillMaxWidth()) {
            diaries.forEach { diary ->
                val cardIndex = cards.indexOfFirst { it.id == diary.cardId }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                    if (cardIndex != -1) {
                        Box(
                            modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${cardIndex + 1}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier.padding(top = 6.dp).size(8.dp).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(text = diary.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

        BasicTextField(
            value = newTextState,
            onValueChange = { newTextState = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(top = 4.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (newTextState.isNotBlank()) {
                        onSaveDiary(newTextState, null)
                        newTextState = ""
                    }
                    focusManager.clearFocus()
                }
            ),
            decorationBox = { innerTextField ->
                if (newTextState.isEmpty()) {
                    Text("追加今日总结...", color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
                }
                innerTextField()
            }
        )
    }
}