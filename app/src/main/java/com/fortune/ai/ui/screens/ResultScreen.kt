package com.fortune.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneCategory
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticLightGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary
import com.fortune.ai.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    results: Map<FortuneMethod, FortuneResult>,
    overallSummary: String?,
    isGeneratingSummary: Boolean,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onMethodClick: (FortuneMethod) -> Unit,
    onInteractiveClick: (FortuneMethod) -> Unit,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val tabs = listOf("中式", "西式", "问一卦")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
    ) {
        TopAppBar(
            title = {
                Text(
                    "✦ AI 算命",
                    color = MysticGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Text("⚙️", fontSize = 20.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MysticDarkPurple
            )
        )

        OverallSummaryCard(overallSummary, isGeneratingSummary, onSummaryClick)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MysticDarkPurple,
            contentColor = MysticGold,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .height(3.dp)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(MysticGold)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabChange(index) },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) MysticGold else TextTertiary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

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
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(enabled = summary != null) { onClick() },
        colors = CardDefaults.cardColors(containerColor = MysticPurple),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☯", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "命运总评",
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                when {
                    isLoading -> Text(
                        "正在汇总各方大师意见...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                    summary != null -> Text(
                        "点击查看完整总评",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    else -> Text(
                        "等待所有算命完成...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary.copy(alpha = 0.6f)
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MysticGold,
                    strokeWidth = 2.dp
                )
            } else if (summary != null) {
                Text("›", fontSize = 22.sp, color = MysticGold)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val methodIcons = mapOf(
        FortuneMethod.BAZI to "🏮",
        FortuneMethod.ZIWEI to "⭐",
        FortuneMethod.NAME_STUDY to "📜",
        FortuneMethod.ZODIAC to "🐉",
        FortuneMethod.QIMEN to "🧭",
        FortuneMethod.ASTROLOGY to "♈",
        FortuneMethod.NATAL_CHART to "🌌",
        FortuneMethod.NUMEROLOGY to "🔢",
        FortuneMethod.VEDIC to "🕉️",
        FortuneMethod.HUMAN_DESIGN to "🧬",
        FortuneMethod.MAYAN to "🌀",
        FortuneMethod.CELTIC_TREE to "🌳",
        FortuneMethod.BLOOD_TYPE to "🩸"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = result.isComplete) { onClick() },
        colors = CardDefaults.cardColors(containerColor = MysticDarkPurple),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(methodIcons[method] ?: "🔮", fontSize = 26.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    method.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (result.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = MysticGold,
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "推演中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                } else {
                    Text(
                        result.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (result.isComplete) {
                Text("›", fontSize = 22.sp, color = MysticGold.copy(alpha = 0.7f))
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
    val descriptions = mapOf(
        FortuneMethod.TAROT to "抽取三张牌，揭示过去现在未来",
        FortuneMethod.LIUYAO to "摇卦六次，以天地之数断吉凶",
        FortuneMethod.MEIHUA to "一念成数，数中藏机",
        FortuneMethod.CEZI to "一字之间，窥见天机",
        FortuneMethod.RUNES to "北欧古老符文的指引",
        FortuneMethod.DICE to "三骰定乾坤",
        FortuneMethod.PENDULUM to "是与否的灵性感应"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(methods) { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInteractiveClick(method) },
                colors = CardDefaults.cardColors(containerColor = MysticDarkPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icons[method] ?: "🔮", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            method.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            descriptions[method] ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                    Text("›", fontSize = 22.sp, color = MysticGold.copy(alpha = 0.7f))
                }
            }
        }
    }
}
