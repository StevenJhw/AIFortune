package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextPrimary

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
            title = { Text("重新全算", color = MysticGold) },
            text = { Text("将重新调用AI算命，覆盖之前的结果。确定吗？", color = TextPrimary) },
            containerColor = MysticDarkPurple,
            confirmButton = {
                TextButton(onClick = {
                    showReRunConfirm = false
                    onReRun()
                }) { Text("确定", color = MysticGold) }
            },
            dismissButton = {
                TextButton(onClick = { showReRunConfirm = false }) { Text("取消", color = TextPrimary) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
    ) {
        TopAppBar(
            title = { Text("设置", color = MysticGold, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MysticGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MysticDarkPurple)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MysticGold)
            ) {
                Text("修改基本信息")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showReRunConfirm = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MysticGold)
            ) {
                Text("重新全算（重新调用AI）")
            }
        }
    }
}
