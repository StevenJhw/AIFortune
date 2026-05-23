package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    result: FortuneResult,
    onBack: () -> Unit,
    onChat: () -> Unit
) {
    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(result.method.displayName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Plain language summary at top
            if (result.plainSummary.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "大白话说",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            result.plainSummary,
                            fontSize = 16.sp,
                            lineHeight = 26.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Detailed analysis
            Text(
                "详细解读",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                result.detail,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }

        // Chat button at bottom
        Button(
            onClick = onChat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
        ) {
            Text("💬 追问大师")
        }
    }
}
