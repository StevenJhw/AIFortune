package com.fortune.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fortune.ai.R
import com.fortune.ai.data.api.EdgeTtsClient
import com.fortune.ai.ui.theme.MysticGold
import com.fortune.ai.ui.theme.MysticPurple
import com.fortune.ai.ui.theme.TextTertiary
import kotlinx.coroutines.launch

enum class TtsState { IDLE, LOADING, PLAYING, PAUSED }

@Composable
fun TtsPlayerBar(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsClient = remember { EdgeTtsClient() }
    var state by remember { mutableStateOf(TtsState.IDLE) }

    DisposableEffect(Unit) {
        onDispose { ttsClient.stop() }
    }

    fun startSpeak() {
        state = TtsState.LOADING
        scope.launch {
            try {
                ttsClient.speak(text, context.cacheDir) {
                    state = if (ttsClient.isPlaying) TtsState.PLAYING else TtsState.IDLE
                }
            } catch (_: Exception) {
                state = TtsState.IDLE
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            TtsState.IDLE -> {
                // Speaker icon = start
                IconButton(
                    onClick = { startSpeak() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up),
                        contentDescription = "朗读",
                        tint = MysticGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            TtsState.LOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MysticGold,
                    strokeWidth = 2.dp
                )
            }
            TtsState.PLAYING -> {
                // Speaker icon = pause
                IconButton(
                    onClick = {
                        ttsClient.pause()
                        state = TtsState.PAUSED
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_up),
                        contentDescription = "暂停",
                        tint = MysticGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Circle arrow = restart
                IconButton(
                    onClick = { startSpeak() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_replay),
                        contentDescription = "重新开始",
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            TtsState.PAUSED -> {
                // Speaker icon (muted look) = resume
                IconButton(
                    onClick = {
                        ttsClient.resume()
                        state = TtsState.PLAYING
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_volume_off),
                        contentDescription = "继续",
                        tint = TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Circle arrow = restart
                IconButton(
                    onClick = { startSpeak() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_replay),
                        contentDescription = "重新开始",
                        tint = MysticGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
