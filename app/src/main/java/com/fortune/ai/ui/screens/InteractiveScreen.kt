package com.fortune.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveScreen(
    method: FortuneMethod,
    onBack: () -> Unit,
    onSubmit: (question: String, extra: String) -> Unit,
    result: String?
) {
    var question by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(method.displayName) },
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
            // Question input
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("你想问什么？") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Method-specific interaction
            when (method) {
                FortuneMethod.TAROT -> TarotInteraction { extra = it }
                FortuneMethod.LIUYAO -> LiuyaoInteraction { extra = it }
                FortuneMethod.MEIHUA -> MeihuaInteraction { extra = it }
                FortuneMethod.CEZI -> CeziInteraction { extra = it }
                FortuneMethod.RUNES -> RunesInteraction { extra = it }
                FortuneMethod.DICE -> DiceInteraction { extra = it }
                FortuneMethod.PENDULUM -> {} // No extra needed
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isSubmitted) {
                Button(
                    onClick = {
                        isSubmitted = true
                        onSubmit(question, extra)
                    },
                    enabled = question.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("开始占卜")
                }
            }

            // Result
            if (isSubmitted) {
                Spacer(modifier = Modifier.height(24.dp))
                if (result == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("大师正在解读...")
                    }
                } else {
                    Text(result, fontSize = 15.sp, lineHeight = 24.sp)
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

    Text("点击抽取3张牌：", fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (selectedCards.size < 3) {
        Button(onClick = {
            val available = majorArcana - selectedCards.toSet()
            val card = available.random()
            val reversed = Random.nextBoolean()
            val cardText = if (reversed) "$card (逆位)" else "$card (正位)"
            selectedCards = selectedCards + cardText
            onResult(selectedCards.joinToString("、"))
        }) {
            Text("🎴 抽第${selectedCards.size + 1}张牌")
        }
    }

    selectedCards.forEachIndexed { index, card ->
        Text("第${index + 1}张：$card", fontSize = 16.sp)
    }
}

@Composable
private fun LiuyaoInteraction(onResult: (String) -> Unit) {
    var yaos by remember { mutableStateOf<List<String>>(emptyList()) }

    Text("摇卦（共6次）：", fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (yaos.size < 6) {
        Button(onClick = {
            val coins = List(3) { Random.nextInt(2) } // 0=背 1=面
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
        }) {
            Text("☰ 第${yaos.size + 1}次摇卦")
        }
    }

    yaos.forEachIndexed { index, yao ->
        Text("第${index + 1}爻：$yao")
    }
}

@Composable
private fun MeihuaInteraction(onResult: (String) -> Unit) {
    var number by remember { mutableStateOf("") }

    Text("输入一个数字（随意）：", fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = number,
        onValueChange = {
            number = it
            onResult(it)
        },
        label = { Text("数字") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CeziInteraction(onResult: (String) -> Unit) {
    var character by remember { mutableStateOf("") }

    Text("写一个字：", fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = character,
        onValueChange = {
            if (it.length <= 1) {
                character = it
                onResult(it)
            }
        },
        label = { Text("一个汉字") },
        modifier = Modifier.fillMaxWidth()
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

    Text("抽取一个符文：", fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (selected == null) {
        Button(onClick = {
            selected = runes.random()
            onResult(selected!!)
        }) {
            Text("ᚱ 抽取符文")
        }
    } else {
        Text("你抽到了：$selected", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiceInteraction(onResult: (String) -> Unit) {
    var diceResult by remember { mutableStateOf<List<Int>>(emptyList()) }

    Text("掷3个骰子：", fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    if (diceResult.isEmpty()) {
        Button(onClick = {
            diceResult = List(3) { Random.nextInt(1, 7) }
            onResult(diceResult.joinToString(", "))
        }) {
            Text("🎲 掷骰子")
        }
    } else {
        Text("结果：${diceResult.joinToString("  ") { "🎲$it" }}", fontSize = 18.sp)
    }
}
