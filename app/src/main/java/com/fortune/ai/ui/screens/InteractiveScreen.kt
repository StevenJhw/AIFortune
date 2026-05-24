package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fortune.ai.data.remote.InteractiveHistoryRecord
import com.fortune.ai.data.remote.SupabaseClient
import com.fortune.ai.ui.components.FortuneTextContent
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary
import com.fortune.ai.ui.theme.TextTertiary
import com.fortune.ai.ui.theme.CautionRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveScreen(
    method: FortuneMethod,
    onBack: () -> Unit,
    onSubmit: (question: String, extra: String) -> Unit,
    result: String?,
    onReset: () -> Unit
) {
    var question by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<InteractiveHistoryRecord>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(method) {
        try {
            history = SupabaseClient.loadInteractiveHistory(method)
        } catch (_: Exception) {}
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
    ) {
        TopAppBar(
            title = {
                Text(
                    method.displayName,
                    color = MysticGold,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MysticGold)
                }
            },
            actions = {
                if (history.isNotEmpty()) {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(
                            if (showHistory) "新占卜" else "历史(${history.size})",
                            color = MysticGold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MysticDarkPurple
            )
        )

        if (showHistory) {
            HistoryList(
                history = history,
                onDelete = { record ->
                    scope.launch {
                        try {
                            SupabaseClient.deleteInteractiveRecord(record.id)
                            history = history.filter { it.id != record.id }
                        } catch (_: Exception) {}
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("你想问什么？", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !isSubmitted,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MysticGold,
                        unfocusedBorderColor = MysticPurple,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = MysticGold,
                        cursorColor = MysticGold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSubmitted) {
                    when (method) {
                        FortuneMethod.TAROT -> TarotInteraction { extra = it }
                        FortuneMethod.LIUYAO -> LiuyaoInteraction { extra = it }
                        FortuneMethod.MEIHUA -> MeihuaInteraction { extra = it }
                        FortuneMethod.CEZI -> CeziInteraction { extra = it }
                        FortuneMethod.RUNES -> RunesInteraction { extra = it }
                        FortuneMethod.DICE -> DiceInteraction { extra = it }
                        FortuneMethod.PENDULUM -> {}
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSubmitted) {
                    val extraReady = when (method) {
                        FortuneMethod.TAROT -> extra.split("、").size >= 3
                        FortuneMethod.LIUYAO -> extra.split("\n").size >= 6
                        FortuneMethod.MEIHUA -> extra.isNotBlank() && extra.all { it.isDigit() }
                        FortuneMethod.CEZI -> extra.isNotBlank()
                        FortuneMethod.RUNES -> extra.isNotBlank()
                        FortuneMethod.DICE -> extra.isNotBlank()
                        FortuneMethod.PENDULUM -> true
                        else -> true
                    }

                    val validationHint = when {
                        question.isBlank() -> "请先输入问题"
                        !extraReady -> when (method) {
                            FortuneMethod.TAROT -> "请抽满3张牌"
                            FortuneMethod.LIUYAO -> "请摇满6次卦"
                            FortuneMethod.MEIHUA -> "请输入数字"
                            FortuneMethod.CEZI -> "请输入一个字"
                            FortuneMethod.RUNES -> "请抽取符文"
                            FortuneMethod.DICE -> "请掷骰子"
                            else -> ""
                        }
                        else -> null
                    }

                    if (validationHint != null) {
                        Text(
                            validationHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CautionRed.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            isSubmitted = true
                            onSubmit(question, extra)
                        },
                        enabled = question.isNotBlank() && extraReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MysticGold,
                            contentColor = MysticBlack
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("开始占卜", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                if (isSubmitted) {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (result == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MysticGold,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "大师正在解读...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        SelectionContainer {
                            FortuneTextContent(rawText = result)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = {
                                isSubmitted = false
                                question = ""
                                extra = ""
                                onReset()
                                scope.launch {
                                    delay(500)
                                    try {
                                        history = SupabaseClient.loadInteractiveHistory(method)
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MysticGold)
                        ) {
                            Text("再算一卦", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: List<InteractiveHistoryRecord>,
    onDelete: (InteractiveHistoryRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        history.forEach { record ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MysticDarkPurple),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "问：${record.question}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MysticGold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDelete(record) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontSize = 16.sp, color = CautionRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        record.result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 6,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TarotInteraction(onResult: (String) -> Unit) {
    val majorArcana = listOf(
        "愚者", "魔术师", "女祭司", "女皇", "皇帝", "教皇",
        "恋人", "战车", "力量", "隐者", "命运之轮", "正义",
        "倒吊人", "死神", "节制", "恶魔", "塔", "星星",
        "月亮", "太阳", "审判", "世界"
    )
    var selectedCards by remember { mutableStateOf<List<String>>(emptyList()) }

    Text("点击抽取3张牌：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedCards.size < 3) {
        Button(
            onClick = {
                val available = majorArcana - selectedCards.map { it.substringBefore(" (") }.toSet()
                val card = available.random()
                val reversed = Random.nextBoolean()
                val cardText = if (reversed) "$card (逆位)" else "$card (正位)"
                selectedCards = selectedCards + cardText
                onResult(selectedCards.joinToString("、"))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticPurple,
                contentColor = MysticGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🎴 抽第${selectedCards.size + 1}张牌")
        }
    }

    selectedCards.forEachIndexed { index, card ->
        Spacer(modifier = Modifier.height(4.dp))
        Text("第${index + 1}张：$card", fontSize = 16.sp, color = MysticGold)
    }
}

@Composable
private fun LiuyaoInteraction(onResult: (String) -> Unit) {
    var yaos by remember { mutableStateOf<List<String>>(emptyList()) }

    Text("摇卦（共6次）：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))

    if (yaos.size < 6) {
        Button(
            onClick = {
                val coins = List(3) { Random.nextInt(2) }
                val sum = coins.sum()
                val yao = when (sum) {
                    0 -> "老阴 ✗✗"
                    1 -> "少阳 ——"
                    2 -> "少阴 — —"
                    3 -> "老阳 ○○"
                    else -> ""
                }
                yaos = yaos + yao
                onResult(yaos.joinToString("\n"))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticPurple,
                contentColor = MysticGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("☰ 第${yaos.size + 1}次摇卦")
        }
    }

    yaos.forEachIndexed { index, yao ->
        Spacer(modifier = Modifier.height(2.dp))
        Text("第${index + 1}爻：$yao", color = TextPrimary)
    }
}

@Composable
private fun MeihuaInteraction(onResult: (String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Text("输入一个数字（随意）：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = number,
        onValueChange = { input ->
            if (input.isEmpty() || input.all { it.isDigit() }) {
                number = input
                error = false
                onResult(input)
            } else {
                error = true
            }
        },
        label = { Text("数字", color = TextTertiary) },
        isError = error,
        supportingText = if (error) {{ Text("只能输入数字", color = CautionRed) }} else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MysticGold,
            unfocusedBorderColor = MysticPurple,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = MysticGold
        )
    )
}

@Composable
private fun CeziInteraction(onResult: (String) -> Unit) {
    var character by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Text("写一个字：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = character,
        onValueChange = { input ->
            if (input.isEmpty()) {
                character = ""
                error = null
                onResult("")
            } else if (input.length == 1 && input.first().code > 0x4E00) {
                character = input
                error = null
                onResult(input)
            } else if (input.length > 1) {
                error = "只能输入一个字"
            } else {
                error = "请输入汉字"
            }
        },
        label = { Text("一个汉字", color = TextTertiary) },
        isError = error != null,
        supportingText = error?.let { { Text(it, color = CautionRed) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MysticGold,
            unfocusedBorderColor = MysticPurple,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = MysticGold
        )
    )
}

@Composable
private fun RunesInteraction(onResult: (String) -> Unit) {
    val runes = listOf(
        "ᚠ Fehu(财富)", "ᚢ Uruz(力量)", "ᚦ Thurisaz(巨人)", "ᚨ Ansuz(神谕)",
        "ᚱ Raido(旅途)", "ᚲ Kenaz(火炬)", "ᚷ Gebo(礼物)", "ᚹ Wunjo(喜悦)",
        "ᚺ Hagalaz(冰雹)", "ᚾ Nauthiz(需要)", "ᛁ Isa(冰)", "ᛃ Jera(收获)",
        "ᛇ Eihwaz(紫杉)", "ᛈ Perthro(命运)", "ᛉ Algiz(保护)", "ᛊ Sowilo(太阳)",
        "ᛏ Tiwaz(战神)", "ᛒ Berkano(桦树)", "ᛖ Ehwaz(马)", "ᛗ Mannaz(人)",
        "ᛚ Laguz(水)", "ᛜ Ingwaz(丰饶)", "ᛞ Dagaz(黎明)", "ᛟ Othala(遗产)"
    )
    var selected by remember { mutableStateOf<String?>(null) }

    Text("抽取一个符文：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))

    if (selected == null) {
        Button(
            onClick = {
                selected = runes.random()
                onResult(selected!!)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticPurple,
                contentColor = MysticGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ᚱ 抽取符文")
        }
    } else {
        Text("你抽到了：$selected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MysticGold)
    }
}

@Composable
private fun DiceInteraction(onResult: (String) -> Unit) {
    var diceResult by remember { mutableStateOf<List<Int>>(emptyList()) }

    Text("掷3个骰子：", fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))

    if (diceResult.isEmpty()) {
        Button(
            onClick = {
                diceResult = List(3) { Random.nextInt(1, 7) }
                onResult(diceResult.joinToString(", "))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MysticPurple,
                contentColor = MysticGold
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🎲 掷骰子")
        }
    } else {
        Text(
            "结果：${diceResult.joinToString("  ") { "🎲$it" }}",
            fontSize = 18.sp,
            color = MysticGold
        )
    }
}
