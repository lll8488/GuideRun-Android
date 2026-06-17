package com.blindrunner.app.ui.blind

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import android.content.Intent
import com.blindrunner.app.R
import com.blindrunner.app.base.BaseActivity
import com.blindrunner.app.ui.auth.PrivacyActivity
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Switch>(R.id.sw_contrast).apply {
            isChecked = AppPrefs.highContrastMode
            setOnCheckedChangeListener { _, checked ->
                AppPrefs.highContrastMode = checked
                showToast("已${if (checked) "开启" else "关闭"}高对比度主题，重启后生效")
                TtsHelper.speak(if (checked) "高对比度主题已开启，请重启应用" else "已恢复默认主题")
            }
        }

        findViewById<Switch>(R.id.sw_nav_voice).apply {
            isChecked = AppPrefs.navigationVoiceEnabled
            setOnCheckedChangeListener { _, checked ->
                AppPrefs.navigationVoiceEnabled = checked
                TtsHelper.speak(if (checked) "页面跳转语音已开启" else "页面跳转语音已关闭")
            }
        }

        findViewById<Switch>(R.id.sw_voice_prompts).apply {
            isChecked = AppPrefs.voicePromptsEnabled
            setOnCheckedChangeListener { _, checked ->
                AppPrefs.voicePromptsEnabled = checked
                TtsHelper.speak(if (checked) "跑步语音口令已开启" else "跑步语音口令已关闭")
            }
        }

        listOf(R.id.btn_15s to 15, R.id.btn_30s to 30, R.id.btn_60s to 60).forEach { (id, secs) ->
            findViewById<Button>(id).setOnClickListener {
                AppPrefs.voicePromptInterval = secs
                showToast("语音间隔已设置${secs}秒")
                TtsHelper.speak("播报间隔已设置为${secs}秒")
            }
        }

        findViewById<Button>(R.id.btn_privacy).setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
    }
}
