package com.example.guiderun.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object TTSManager {
    private const val TAG = "TTSManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "中文语音不支持")
                } else {
                    isInitialized = true
                    tts?.setSpeechRate(1.0f) // 正常语速
                }
            } else {
                Log.e(TAG, "TTS初始化失败")
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized && tts != null && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}