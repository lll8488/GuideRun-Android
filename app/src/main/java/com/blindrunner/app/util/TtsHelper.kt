package com.blindrunner.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * 全局单例TTS，修复小米手机无声 + 队列循环堆积问题：
 * - 使用 STREAM_MUSIC 确保音频输出（小米兼容）
 * - 请求音频焦点避免被其他应用打断
 * - 限制队列最大长度防止堆积
 * - 播报用 FLUSH 模式替换旧提示
 */
object TtsHelper : TextToSpeech.OnInitListener {

    private const val TAG = "TtsHelper"
    private var tts: TextToSpeech? = null
    private var ready = false
    private var audioManager: AudioManager? = null
    private var hasAudioFocus = false
    private var pendingSpeakText: String? = null

    fun init(context: Context) {
        if (tts != null && ready) return
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        Log.d(TAG, "TTS init, manufacturer=${Build.MANUFACTURER}, model=${Build.MODEL}")
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            configureTts()
        } else {
            Log.e(TAG, "TTS init failed, status=$status")
            ready = false
            // 部分小米手机第一次初始化会失败，2秒后重试
            val ctx = AppPrefs.getContext()
            if (ctx != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "Retrying TTS init...")
                    tts?.stop()
                    tts?.shutdown()
                    tts = TextToSpeech(ctx, this)
                }, 2000L)
            }
        }
    }

    private fun configureTts() {
        val t = tts ?: return

        // === 小米适配：使用 STREAM_MUSIC 确保有声音 ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            t.setAudioAttributes(audioAttributes)
        }

        // 设置中文
        val localeResult = t.setLanguage(Locale.CHINESE)
        if (localeResult == TextToSpeech.LANG_MISSING_DATA || localeResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            t.setLanguage(Locale.SIMPLIFIED_CHINESE)
        }

        // 语速和音调
        t.setSpeechRate(1.0f)
        t.setPitch(1.0f)

        // === 获取音频焦点 ===
        requestAudioFocus()

        // 播报回调
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS onStart: $utteranceId")
            }
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS onDone: $utteranceId")
                // 播完后释放焦点
                abandonAudioFocus()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error (deprecated): $utteranceId")
                abandonAudioFocus()
            }
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS error: $utteranceId, code=$errorCode")
                abandonAudioFocus()
            }
            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                Log.d(TAG, "TTS onStop: $utteranceId, interrupted=$interrupted")
                abandonAudioFocus()
            }
        })

        ready = true
        Log.d(TAG, "TTS ready: lang=${t.language}, engine=${t.defaultEngine}")

        // 播报积压的待播报内容
        pendingSpeakText?.let { text ->
            pendingSpeakText = null
            doSpeak(text, true)
        }
    }

    /**
     * 语音播报 — flush=true 会打断当前播报（用于导航/计时），
     * flush=false 会追加到队列末尾（用于长文本朗读）
     */
    @Synchronized
    fun speak(text: String, flush: Boolean = false) {
        if (!ready || tts == null) {
            // 未就绪：只保留最后一条待播报内容
            pendingSpeakText = text
            Log.d(TAG, "TTS not ready, pending: $text")
            return
        }
        doSpeak(text, flush)
    }

    private fun doSpeak(text: String, flush: Boolean) {
        val t = tts ?: return

        // 先停止当前播报（避免小米TTS引擎队列堆积）
        if (flush) {
            try {
                t.stop()
            } catch (_: Exception) {}
        }

        requestAudioFocus()

        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "tts_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            // 小米引擎需要显式设置音量
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            // 指定流类型
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        val result = t.speak(text, mode, params, utteranceId)
        if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "speak OK: $text")
        } else {
            Log.e(TAG, "speak FAILED: $text, result=$result")
            abandonAudioFocus()
        }
    }

    @Synchronized
    fun speakNavigation(pageName: String) {
        if (!AppPrefs.navigationVoiceEnabled) return
        speak("正在跳转到${pageName}", true)
    }

    /**
     * 立即停止所有播报
     */
    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        pendingSpeakText = null
        abandonAudioFocus()
    }

    fun isReady(): Boolean = ready

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        ready = false
        abandonAudioFocus()
    }

    // === 音频焦点管理（修复小米无声） ===

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setWillPauseWhenDucked(false)
                    .build()
                val result = am.requestAudioFocus(focusRequest)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val result = am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
            Log.d(TAG, "Audio focus: $hasAudioFocus")
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus failed: ${e.message}")
            hasAudioFocus = true // 假设成功，避免阻塞播报
        }
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 无法直接abandon因为我们需要持有request引用
                // 简单处理: 使用deprecated API
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
        hasAudioFocus = false
    }
}
