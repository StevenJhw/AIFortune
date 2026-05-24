package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.remote.SupabaseClient
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary
import com.fortune.ai.ui.theme.TextTertiary
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    method: FortuneMethod,
    initialResult: FortuneResult,
    onBack: () -> Unit,
    onSendMessage: (String, (String) -> Unit) -> Unit
) {
    BackHandler { onBack() }

    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isWaiting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val history = try {
            SupabaseClient.loadChatHistory(method)
        } catch (_: Exception) { emptyList() }

        if (history.isNotEmpty()) {
            messages = history.map { ChatMessage(it.content, it.isUser) }
        } else {
            val initialMsg = buildString {
                append("以下是${method.displayName}的解读结果：\n\n")
                if (initialResult.plainSummary.isNotBlank()) {
                    append(initialResult.plainSummary)
                    append("\n\n")
                }
                append(initialResult.detail)
            }
            messages = listOf(ChatMessage(initialMsg, isUser = false))
            try {
                SupabaseClient.saveChatMessage(method, initialMsg, isUser = false)
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
    ) {
        TopAppBar(
            title = {
                Text(
                    "追问 · ${method.displayName}",
                    color = MysticGold,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MysticGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MysticDarkPurple
            )
        )

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MysticGold)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                if (isWaiting) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MysticGold,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "大师思考中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MysticDarkPurple)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("继续追问...", color = TextTertiary) },
                modifier = Modifier.weight(1f),
                enabled = !isWaiting && !isLoading,
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MysticGold,
                    unfocusedBorderColor = MysticPurple,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = MysticGold
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    val userMsg = inputText.trim()
                    if (userMsg.isNotBlank()) {
                        messages = messages + ChatMessage(userMsg, isUser = true)
                        inputText = ""
                        isWaiting = true

                        scope.launch {
                            try {
                                SupabaseClient.saveChatMessage(method, userMsg, isUser = true)
                            } catch (_: Exception) {}
                            listState.animateScrollToItem(messages.size - 1)
                        }

                        onSendMessage(userMsg) { reply ->
                            messages = messages + ChatMessage(reply, isUser = false)
                            isWaiting = false
                            scope.launch {
                                try {
                                    SupabaseClient.saveChatMessage(method, reply, isUser = false)
                                } catch (_: Exception) {}
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isWaiting && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MysticGold,
                    contentColor = MysticBlack
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("发送", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MysticPurple else MysticDarkPurple

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            if (message.isUser) "你" else "大师",
            style = MaterialTheme.typography.labelSmall,
            color = if (message.isUser) TextTertiary else MysticGold.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            SelectionContainer {
                Text(
                    message.content,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
