package com.fortune.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneCategory
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    results: Map<FortuneMethod, FortuneResult>,
    overallSummary: String?,
    isGeneratingSummary: Boolean,
    onMethodClick: (FortuneMethod) -> Unit,
    onInteractiveClick: (FortuneMethod) -> Unit,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("中式", "西式", "问一卦")

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with settings
        TopAppBar(
            title = { Text("🔮 AI 算命") },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }
        )

        // Overall summary card at top - compact, clickable
        OverallSummaryCard(overallSummary, isGeneratingSummary, onSummaryClick)

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content based on tab
        when (selectedTab) {
            0 -> AutoResultsList(results, FortuneCategory.CHINESE, onMethodClick)
            1 -> AutoResultsList(results, FortuneCategory.WESTERN, onMethodClick)
            2 -> InteractiveList(onInteractiveClick)
        }
    }
}

@Composable
private fun OverallSummaryCard(summary: String?, isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(enabled = summary != null) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "命运总评",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                when {
                    isLoading -> Text("正在汇总各方大师意见...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    summary != null -> Text("点击查看完整总评", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    else -> Text("等待所有算命完成...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else if (summary != null) {
                Text("→", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun AutoResultsList(
    results: Map<FortuneMethod, FortuneResult>,
    category: FortuneCategory,
    onMethodClick: (FortuneMethod) -> Unit
) {
    val filtered = results.filter { it.key.category == category }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filtered.entries.toList()) { (method, result) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                ResultCard(method, result, onClick = { onMethodClick(method) })
            }
        }
    }
}

@Composable
private fun ResultCard(method: FortuneMethod, result: FortuneResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = result.isComplete) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(method.displayName, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (result.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("推演中...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    Text(
                        result.summary,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }
            if (result.isComplete) {
                Text("→", fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun InteractiveList(onInteractiveClick: (FortuneMethod) -> Unit) {
    val methods = FortuneMethod.interactiveMethods()
    val icons = mapOf(
        FortuneMethod.TAROT to "🎴",
        FortuneMethod.LIUYAO to "☰",
        FortuneMethod.MEIHUA to "🌸",
        FortuneMethod.CEZI to "✍️",
        FortuneMethod.RUNES to "ᚱ",
        FortuneMethod.DICE to "🎲",
        FortuneMethod.PENDULUM to "🔮"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(methods) { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInteractiveClick(method) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icons[method] ?: "🔮", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(method.displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("→", fontSize = 18.sp)
                }
            }
        }
    }
}
