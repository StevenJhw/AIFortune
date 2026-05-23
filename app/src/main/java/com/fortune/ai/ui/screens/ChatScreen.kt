package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.remote.SupabaseClient
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("追问 · ${method.displayName}") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
                if (isWaiting) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("大师思考中...", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("继续追问...") },
                modifier = Modifier.weight(1f),
                enabled = !isWaiting && !isLoading,
                singleLine = false,
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val userMsg = inputText.trim()
                    if (userMsg.isNotBlank()) {
                        messages = messages + ChatMessage(userMsg, isUser = true)
                        inputText = ""
                        isWaiting = true

                        scope.launch {
                            // Save user message first, await completion
                            try {
                                SupabaseClient.saveChatMessage(method, userMsg, isUser = true)
                            } catch (_: Exception) {}
                            listState.animateScrollToItem(messages.size - 1)
                        }

                        onSendMessage(userMsg) { reply ->
                            messages = messages + ChatMessage(reply, isUser = false)
                            isWaiting = false
                            scope.launch {
                                // Save AI reply, await completion
                                try {
                                    SupabaseClient.saveChatMessage(method, reply, isUser = false)
                                } catch (_: Exception) {}
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isWaiting && !isLoading
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            if (message.isUser) "你" else "大师",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}
