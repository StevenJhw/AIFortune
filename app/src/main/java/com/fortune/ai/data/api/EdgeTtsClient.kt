package com.fortune.ai.data.api

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EdgeTtsClient {

    companion object {
        private const val TAG = "EdgeTTS"
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
        private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
        private const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_FULL_VERSION"
        private const val WIN_EPOCH = 11644473600L
        private const val SEGMENT_SIZE = 400
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var currentWebSocket: WebSocket? = null
    private var isPaused = false
    private var isStopped = false
    private var clockSkewSeconds: Long = 0
    private val pendingFiles = ConcurrentLinkedQueue<File>()

    val isPlaying: Boolean get() = mediaPlayer?.isPlaying == true || pendingFiles.isNotEmpty()
    val isPausedState: Boolean get() = isPaused

    suspend fun speak(text: String, cacheDir: File, onStateChange: () -> Unit) {
        stop()
        isStopped = false
        Log.d(TAG, "TTS speak: text length=${text.length}")

        val segments = splitIntoSegments(cleanTextForSpeech(text))
        Log.d(TAG, "Split into ${segments.size} segments")

        withContext(Dispatchers.IO) {
            for ((index, segment) in segments.withIndex()) {
                if (isStopped) break

                val audioFile = synthesizeSegment(segment, cacheDir, index)
                if (isStopped) break

                if (index == 0) {
                    withContext(Dispatchers.Main) {
                        playAudio(audioFile, onStateChange, cacheDir)
                        onStateChange()
                    }
                } else {
                    pendingFiles.add(audioFile)
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPaused = true
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (isPaused) {
                it.start()
                isPaused = false
            }
        }
    }

    fun stop() {
        isStopped = true
        currentWebSocket?.cancel()
        currentWebSocket = null
        mediaPlayer?.apply {
            try {
                if (isPlaying || isPaused) stop()
                release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        isPaused = false
        pendingFiles.clear()
    }

    private fun playAudio(file: File, onStateChange: () -> Unit, cacheDir: File) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener {
                file.delete()
                playNext(onStateChange, cacheDir)
            }
            start()
        }
        isPaused = false
    }

    private fun playNext(onStateChange: () -> Unit, cacheDir: File) {
        val nextFile = pendingFiles.poll()
        if (nextFile != null && !isStopped) {
            mediaPlayer?.release()
            playAudio(nextFile, onStateChange, cacheDir)
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
            isPaused = false
            onStateChange()
        }
    }

    private fun splitIntoSegments(text: String): List<String> {
        if (text.length <= SEGMENT_SIZE) return listOf(text)

        val segments = mutableListOf<String>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            if (remaining.length <= SEGMENT_SIZE) {
                segments.add(remaining)
                break
            }
            var cutAt = remaining.lastIndexOf('。', SEGMENT_SIZE)
            if (cutAt < SEGMENT_SIZE / 2) cutAt = remaining.lastIndexOf('，', SEGMENT_SIZE)
            if (cutAt < SEGMENT_SIZE / 2) cutAt = remaining.lastIndexOf('\n', SEGMENT_SIZE)
            if (cutAt < SEGMENT_SIZE / 2) cutAt = SEGMENT_SIZE

            segments.add(remaining.substring(0, cutAt + 1))
            remaining = remaining.substring(cutAt + 1).trimStart()
        }
        return segments
    }

    private fun cleanTextForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*#>|`~]"), "")
            .replace(Regex("\\[([^]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("【([^】]*)】"), "$1，")
            .replace(Regex("^\\d+[.)]+\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[-•]\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("---+"), "")
            .replace(Regex("===+"), "")
            .replace(Regex("[()（）{}\\[\\]「」『』]"), "")
            .replace(Regex("\\s{2,}"), "\n")
            .trim()
    }

    private suspend fun synthesizeSegment(text: String, cacheDir: File, index: Int): File {
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val audioFile = File(cacheDir, "tts_${requestId}_$index.mp3")

        val secMsGec = generateSecMsGec()
        val muid = generateMuid()

        val url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
            "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&ConnectionId=$connectionId" +
            "&Sec-MS-GEC=$secMsGec" +
            "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION"

        if (index == 0) Log.d(TAG, "Connecting segment $index (${text.length} chars)")

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Cookie", "muid=$muid;")
            .build()

        return suspendCancellableCoroutine { continuation ->
            val audioBytes = mutableListOf<ByteArray>()
            var audioStarted = false

            val ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val configMessage = "X-Timestamp:${getTimestamp()}\r\n" +
                        "Content-Type:application/json; charset=utf-8\r\n" +
                        "Path:speech.config\r\n\r\n" +
                        """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}"""
                    webSocket.send(configMessage)

                    val ssml = buildSsml(text)
                    val ssmlMessage = "X-RequestId:$requestId\r\n" +
                        "Content-Type:application/ssml+xml\r\n" +
                        "X-Timestamp:${getTimestamp()}\r\n" +
                        "Path:ssml\r\n\r\n" +
                        ssml
                    webSocket.send(ssmlMessage)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray()
                    if (data.size > 2) {
                        val headerLen = (data[0].toInt() and 0xFF shl 8) or (data[1].toInt() and 0xFF)
                        if (data.size > headerLen + 2) {
                            audioBytes.add(data.copyOfRange(headerLen + 2, data.size))
                            audioStarted = true
                        }
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.contains("Path:turn.end")) {
                        webSocket.close(1000, "done")
                        try {
                            FileOutputStream(audioFile).use { fos ->
                                audioBytes.forEach { fos.write(it) }
                            }
                            if (continuation.isActive) continuation.resume(audioFile)
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Segment $index failure: code=${response?.code}")
                    if (response?.code == 403) {
                        val serverDate = response.header("Date")
                        if (serverDate != null) {
                            try {
                                val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
                                val serverTime = sdf.parse(serverDate)?.time?.div(1000) ?: 0L
                                clockSkewSeconds = serverTime - System.currentTimeMillis() / 1000L
                                Log.d(TAG, "Clock skew: ${clockSkewSeconds}s")
                            } catch (_: Exception) {}
                        }
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("TTS失败(${response?.code})"))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!audioStarted && continuation.isActive) {
                        continuation.resumeWithException(Exception("TTS无音频"))
                    }
                }
            })

            currentWebSocket = ws
            continuation.invokeOnCancellation { ws.cancel() }
        }
    }

    private fun buildSsml(text: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        return """<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>
<voice name='zh-CN-XiaoxiaoNeural'>
<prosody pitch='+0Hz' rate='+10%' volume='+0%'>
$escaped
</prosody>
</voice>
</speak>"""
    }

    private fun generateSecMsGec(): String {
        val unixTimestamp = System.currentTimeMillis() / 1000L + clockSkewSeconds
        var ticks = unixTimestamp + WIN_EPOCH
        ticks -= ticks % 300
        val windowsTicks = ticks * 10_000_000L
        val strToHash = "$windowsTicks$TRUSTED_CLIENT_TOKEN"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(strToHash.toByteArray(Charsets.US_ASCII))
        return hashBytes.joinToString("") { "%02X".format(it) }
    }

    private fun generateMuid(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun getTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }
}
