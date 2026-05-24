package com.fortune.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.data.model.UserProfile
import com.fortune.ai.data.remote.SupabaseClient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.fortune.ai.ui.components.TtsPlayerBar
import com.fortune.ai.ui.theme.MysticLightGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.screens.*
import com.fortune.ai.ui.theme.AiFortuneTheme
import com.fortune.ai.ui.theme.MysticBlack
import com.fortune.ai.ui.theme.MysticDarkPurple
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.TextPrimary
import com.fortune.ai.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseClient.init(this)
        setContent {
            AiFortuneTheme {
                AiFortuneApp()
            }
        }
    }
}

@Composable
fun AiFortuneApp(viewModel: MainViewModel = viewModel()) {
    val profile by viewModel.profile.collectAsState()
    val results by viewModel.results.collectAsState()
    val overallSummary by viewModel.overallSummary.collectAsState()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var selectedResult by remember { mutableStateOf<FortuneResult?>(null) }
    var interactiveMethod by remember { mutableStateOf<FortuneMethod?>(null) }
    var interactiveResult by remember { mutableStateOf<String?>(null) }
    var pendingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showUpdateConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.hasSavedData { hasData ->
            currentScreen = if (hasData) Screen.Results else Screen.Profile
        }
    }

    BackHandler(enabled = currentScreen != Screen.Results && currentScreen != Screen.Profile && currentScreen != Screen.Loading) {
        currentScreen = when (currentScreen) {
            Screen.Detail -> Screen.Results
            Screen.Chat -> Screen.Detail
            Screen.Interactive -> Screen.Results
            Screen.Summary -> Screen.Results
            Screen.Settings -> Screen.Results
            Screen.EditProfile -> Screen.Settings
            else -> Screen.Results
        }
    }

    if (showUpdateConfirm && pendingProfile != null) {
        AlertDialog(
            onDismissRequest = {
                showUpdateConfirm = false
                pendingProfile = null
            },
            title = { Text("修改基本信息", color = MysticGold) },
            text = { Text("修改信息后将重新算命，之前的算命结果和问卦历史都会被清除。确定吗？", color = TextPrimary) },
            containerColor = MysticDarkPurple,
            confirmButton = {
                TextButton(onClick = {
                    showUpdateConfirm = false
                    viewModel.updateProfile(pendingProfile!!)
                    pendingProfile = null
                    currentScreen = Screen.Results
                }) { Text("确定", color = MysticGold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateConfirm = false
                    pendingProfile = null
                }) { Text("取消", color = TextPrimary) }
            }
        )
    }

    when (currentScreen) {
        Screen.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MysticBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MysticGold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("加载中...", color = TextSecondary)
                }
            }
        }

        Screen.Profile -> {
            ProfileScreen(
                existingProfile = null,
                onSubmit = { userProfile ->
                    viewModel.setProfile(userProfile)
                    currentScreen = Screen.Results
                }
            )
        }

        Screen.EditProfile -> {
            ProfileScreen(
                existingProfile = profile,
                onSubmit = { userProfile ->
                    pendingProfile = userProfile
                    showUpdateConfirm = true
                }
            )
        }

        Screen.Results -> {
            ResultScreen(
                results = results,
                overallSummary = overallSummary,
                isGeneratingSummary = isGeneratingSummary,
                selectedTab = selectedTab,
                onTabChange = { viewModel.setSelectedTab(it) },
                onMethodClick = { method ->
                    results[method]?.let {
                        selectedResult = it
                        currentScreen = Screen.Detail
                    }
                },
                onInteractiveClick = { method ->
                    interactiveMethod = method
                    interactiveResult = null
                    currentScreen = Screen.Interactive
                },
                onSummaryClick = {
                    currentScreen = Screen.Summary
                },
                onSettingsClick = {
                    currentScreen = Screen.Settings
                }
            )
        }

        Screen.Detail -> {
            selectedResult?.let { result ->
                DetailScreen(
                    result = result,
                    onBack = { currentScreen = Screen.Results },
                    onChat = { currentScreen = Screen.Chat }
                )
            }
        }

        Screen.Chat -> {
            selectedResult?.let { result ->
                ChatScreen(
                    method = result.method,
                    initialResult = result,
                    onBack = { currentScreen = Screen.Detail },
                    onSendMessage = { question, onReply ->
                        val context = "${result.plainSummary}\n${result.detail}"
                        viewModel.chat(result.method, context, question) { reply ->
                            onReply(reply)
                        }
                    }
                )
            }
        }

        Screen.Interactive -> {
            interactiveMethod?.let { method ->
                InteractiveScreen(
                    method = method,
                    onBack = { currentScreen = Screen.Results },
                    onSubmit = { question, extra ->
                        viewModel.divineInteractive(method, question, extra) { result ->
                            interactiveResult = result
                        }
                    },
                    result = interactiveResult,
                    onReset = {
                        interactiveResult = null
                    }
                )
            }
        }

        Screen.Summary -> {
            SummaryScreen(
                summary = overallSummary ?: "",
                onBack = { currentScreen = Screen.Results }
            )
        }

        Screen.Settings -> {
            SettingsScreen(
                onBack = { currentScreen = Screen.Results },
                onEditProfile = { currentScreen = Screen.EditProfile },
                onReRun = {
                    viewModel.reRunFullReading()
                    currentScreen = Screen.Results
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(summary: String, onBack: () -> Unit) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticBlack)
    ) {
        TopAppBar(
            title = { Text("命运总评", color = MysticGold, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MysticGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MysticDarkPurple)
        )

        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top insight card
                val firstLine = summary.lines().firstOrNull { it.isNotBlank() }?.replace(Regex("[*#]"), "")?.trim()
                if (firstLine != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MysticPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "✧ 综合总评",
                                fontSize = 12.sp,
                                color = MysticGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                firstLine,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MysticLightGold,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }

                TtsPlayerBar(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Body text with gold highlights for **bold**
                val bodyText = summary.lines().drop(1).joinToString("\n").trim()
                SummaryStyledText(
                    text = if (bodyText.isNotBlank()) bodyText else summary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStyledText(text: String, modifier: Modifier = Modifier) {
    val annotated = buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            when {
                remaining.startsWith("**") -> {
                    val end = remaining.indexOf("**", startIndex = 2)
                    if (end > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MysticLightGold)) {
                            append(remaining.substring(2, end))
                        }
                        remaining = remaining.substring(end + 2)
                    } else {
                        append("**")
                        remaining = remaining.substring(2)
                    }
                }
                else -> {
                    val next = remaining.indexOf("**")
                    if (next > 0) {
                        append(remaining.substring(0, next))
                        remaining = remaining.substring(next)
                    } else {
                        append(remaining)
                        remaining = ""
                    }
                }
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        lineHeight = 26.sp,
        modifier = modifier
    )
}

sealed class Screen {
    data object Loading : Screen()
    data object Profile : Screen()
    data object EditProfile : Screen()
    data object Results : Screen()
    data object Detail : Screen()
    data object Chat : Screen()
    data object Interactive : Screen()
    data object Summary : Screen()
    data object Settings : Screen()
}
