package com.fortune.ai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.ui.components.FortuneTextContent
import com.fortune.ai.ui.components.TtsPlayerBar
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticLightGold
import com.fortune.ai.ui.theme.MysticPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    result: FortuneResult,
    onBack: () -> Unit,
    onChat: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MysticDarkPurple, MysticBlack, MysticBlack)
                )
            )
    ) {
        TopAppBar(
            title = {
                Text(
                    result.method.displayName,
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
                containerColor = MysticDarkPurple.copy(alpha = 0.95f)
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.plainSummary.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MysticPurple),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "✧ 简要解读",
                                    fontSize = 12.sp,
                                    color = MysticGold,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    result.plainSummary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MysticLightGold,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    TtsPlayerBar(
                        text = buildString {
                            if (result.plainSummary.isNotBlank()) {
                                append(result.plainSummary)
                                append("。")
                            }
                            append(result.detail)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    FortuneTextContent(rawText = result.detail)
                }
            }
        }

        Surface(
            color = MysticDarkPurple,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = onChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MysticGold,
                    contentColor = MysticBlack
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "✦ 追问大师",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
