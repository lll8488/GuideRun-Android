package com.blindrunner.app.ui.blind

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper
import kotlinx.coroutines.launch

class StartRunningActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private var demandId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_running)

        demandId = intent.getLongExtra("demand_id", 0L)

        val tvInfo = findViewById<TextView>(R.id.tv_run_info)

        if (demandId > 0) {
            lifecycleScope.launch {
                try {
                    val entity = app.database.runningRecordDao().getRecordById(demandId)
                    if (entity != null) {
                        tvInfo.text = "📍 地点：${entity.location}\n⏱ 时长：${entity.durationMinutes}分钟\n👤 志愿者：${entity.volunteerPhone}\n📝 备注：${entity.volunteerNote.ifEmpty { "无" }}\n\n模式：陪跑绳引导模式"
                        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                            .putString("last_run_location", entity.location).apply()
                        TtsHelper.speak("配对跑步 — 地点${entity.location}，时长${entity.durationMinutes}分钟", true)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StartRunning", "load demand info failed", e)
                }
            }
        } else {
            tvInfo.text = "模式：独立跑步模式\n请确认周围环境安全后开始跑步"
            TtsHelper.speak("准备开始跑步，确认信息后点击开始跑步按钮")
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            // PRD 4.4: 开始跑步前弹窗确认
            TtsHelper.speak("确认开始跑步，请做好准备", true)
            AlertDialog.Builder(this)
                .setTitle("开始跑步")
                .setMessage("确定要开始跑步吗？\n开始后将启动GPS定位和计时。")
                .setPositiveButton("确定") { _, _ ->
                    startActivity(Intent(this, RunningModeActivity::class.java).apply {
                        putExtra("demand_id", demandId)
                    })
                }
                .setNegativeButton("取消", null)
                .show()
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }
}
