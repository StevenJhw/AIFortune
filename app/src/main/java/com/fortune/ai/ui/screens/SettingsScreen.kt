package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onReRun: () -> Unit
) {
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
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("修改基本信息")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showReRunConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新全算（重新调用AI）")
            }
        }
    }
}
