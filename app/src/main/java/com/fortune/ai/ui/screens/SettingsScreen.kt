package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.api.ApiKeyStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onReRun: () -> Unit
) {
    var apiKey by remember { mutableStateOf(ApiKeyStore.getApiKey()) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var showReRunConfirm by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    if (showReRunConfirm) {
        AlertDialog(
            onDismissRequest = { showReRunConfirm = false },
            title = { Text("重新全算") },
            text = { Text("将重新调用AI算命，覆盖之前的结果。确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showReRunConfirm = false
                    onReRun()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showReRunConfirm = false }) { Text("取消") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            // Edit profile
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("修改基本信息")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Re-run
            OutlinedButton(
                onClick = { showReRunConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新全算（重新调用AI）")
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // API Key
            Text("DeepSeek API Key", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "填写自己的 Key 可以不消耗默认额度",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    saved = false
                },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "隐藏" else "显示")
                }

                Button(onClick = {
                    ApiKeyStore.setApiKey(apiKey)
                    saved = true
                }) {
                    Text("保存")
                }
            }

            if (saved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("已保存", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
