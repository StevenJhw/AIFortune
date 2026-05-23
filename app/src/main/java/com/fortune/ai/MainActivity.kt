package com.fortune.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fortune.ai.data.api.ApiKeyStore
import com.fortune.ai.data.model.FortuneMethod
import com.fortune.ai.data.model.FortuneResult
import com.fortune.ai.ui.screens.*
import com.fortune.ai.ui.theme.AiFortuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiKeyStore.init(this)
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

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Profile) }
    var selectedResult by remember { mutableStateOf<FortuneResult?>(null) }
    var interactiveMethod by remember { mutableStateOf<FortuneMethod?>(null) }
    var interactiveResult by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        Screen.Profile -> {
            ProfileScreen(onSubmit = { userProfile ->
                viewModel.setProfile(userProfile)
                currentScreen = Screen.Results
            })
        }

        Screen.Results -> {
            ResultScreen(
                results = results,
                overallSummary = overallSummary,
                isGeneratingSummary = isGeneratingSummary,
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
                    onChat = { /* TODO: chat screen */ }
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
                    result = interactiveResult
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
            SettingsScreen(onBack = { currentScreen = Screen.Results })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(summary: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("命运总评") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "📊 综合各方大师意见",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                summary,
                fontSize = 15.sp,
                lineHeight = 24.sp
            )
        }
    }
}

sealed class Screen {
    data object Profile : Screen()
    data object Results : Screen()
    data object Detail : Screen()
    data object Interactive : Screen()
    data object Summary : Screen()
    data object Settings : Screen()
}
