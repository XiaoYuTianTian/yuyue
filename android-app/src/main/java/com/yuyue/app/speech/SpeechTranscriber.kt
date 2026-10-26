package com.yuyue.app.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * 本机按住录音 + Vosk 离线中文识别。
 * MiniMax 不接收音频，识别出的文字再交给 Agent。
 */
class SpeechTranscriber(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val recording = AtomicBoolean(false)
    private val session = AtomicInteger(0)

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    @Volatile private var worker: Thread? = null

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _error.value = "未授予麦克风权限，请在系统设置中允许「语曰」录音"
            return
        }
        val id = session.incrementAndGet()
        _error.value = null
        _partial.value = ""
        _listening.value = false
        _busy.value = true
        recording.set(true)
        worker = thread(name = "yuyue-asr-$id", priority = Thread.NORM_PRIORITY) {
            runCatching { recordAndRecognize(id) }
                .onFailure { e ->
                    Log.e(TAG, "asr failed", e)
                    if (session.get() == id) postError(e.message ?: "语音识别失败")
                }
            if (session.get() == id) {
                recording.set(false)
                main.post {
                    _listening.value = false
                    _busy.value = false
                }
            }
        }
    }

    fun stop() {
        recording.set(false)
    }

    fun clearLive() {
        _partial.value = ""
    }

    fun destroy() {
        session.incrementAndGet()
        recording.set(false)
        worker?.interrupt()
    }

    private fun recordAndRecognize(id: Int) {
        synchronized(modelLock) {
            if (cachedModel == null) {
                LibVosk.setLogLevel(LogLevel.WARNINGS)
                cachedModel = Model(VoskModelStore.ensure(context).absolutePath)
            }
        }
        if (session.get() != id || !recording.get()) return
        val model = cachedModel ?: throw IllegalStateException("语音模型未加载")
        val sampleRate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) throw IllegalStateException("无法初始化录音缓冲")
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuf, sampleRate) * 2,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("麦克风初始化失败")
        }

        val recognizer = Recognizer(model, sampleRate.toFloat())
        val buf = ShortArray(max(minBuf / 2, 4096))
        val segments = mutableListOf<String>()
        var currentPartial = ""
        var maxAbs = 0
        var frames = 0
        val startedAt = System.currentTimeMillis()

        fun publish() {
            val live = tidyTranscript(joinSegments(segments, currentPartial))
            main.post { _partial.value = live }
        }

        recorder.startRecording()
        main.post {
            _listening.value = true
            _busy.value = false
            _error.value = null
        }
        Log.i(TAG, "AudioRecord started")
        try {
            while (recording.get() && System.currentTimeMillis() - startedAt < 60_000L) {
                val n = recorder.read(buf, 0, buf.size)
                if (n <= 0) continue
                frames += n
                for (i in 0 until n) maxAbs = max(maxAbs, abs(buf[i].toInt()))
                if (recognizer.acceptWaveForm(buf, n)) {
                    parseVoskText(recognizer.result, final = true)?.let { segments += it }
                    currentPartial = ""
                } else {
                    currentPartial = parseVoskText(recognizer.partialResult, final = false).orEmpty()
                }
                publish()
            }
            parseVoskText(recognizer.finalResult, final = true)?.let { leftover ->
                if (leftover.isNotBlank()) segments += leftover
            }
            currentPartial = ""
            publish()
            val live = tidyTranscript(joinSegments(segments, ""))
            val elapsed = (System.currentTimeMillis() - startedAt) / 1000
            Log.i(TAG, "stopped frames=$frames maxAbs=$maxAbs elapsed=${elapsed}s")
            main.post {
                _partial.value = live
                _error.value = when {
                    live.isNotBlank() -> null
                    elapsed < 1 -> null
                    maxAbs < 400 -> "几乎没录到声音，请靠近麦克风再按住说"
                    else -> null
                }
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            recognizer.close()
        }
    }

    private fun parseVoskText(raw: String?, final: Boolean): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(raw)
            val key = if (final) "text" else "partial"
            obj.optString(key).trim().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun postError(msg: String) {
        main.post {
            _listening.value = false
            _busy.value = false
            _error.value = msg
        }
    }

    companion object {
        private const val TAG = "YuyueSpeech"
        private val modelLock = Any()
        @Volatile private var cachedModel: Model? = null
    }
}

internal fun tidyTranscript(text: String): String =
    text.replace(Regex("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun joinTranscript(left: String, right: String): String {
    val a = left.trim()
    val b = right.trim()
    if (a.isEmpty()) return b
    if (b.isEmpty()) return a
    val cjk = '\u4e00'..'\u9fff'
    val glueSpace = a.last() !in cjk && b.first() !in cjk &&
        (a.last().isLetterOrDigit() && b.first().isLetterOrDigit())
    return if (glueSpace) "$a $b" else "$a$b"
}

private fun joinSegments(segments: List<String>, partial: String): String {
    var acc = ""
    for (s in segments) acc = joinTranscript(acc, s)
    return joinTranscript(acc, partial)
}
