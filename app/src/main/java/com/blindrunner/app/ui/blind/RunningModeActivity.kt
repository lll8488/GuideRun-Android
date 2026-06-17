package com.blindrunner.app.ui.blind

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.PolylineOptions
import com.blindrunner.app.R
import com.blindrunner.app.service.RunningService
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class RunningModeActivity : AppCompatActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var tvTrackInfo: TextView
    private var service: RunningService? = null
    private var bound = false
    private var demandId = 0L

    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var trackPolyline: com.amap.api.maps.model.Polyline? = null

    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: Vibrator? = null
    private var timerRunning = false

    private var lastAnnounceSecond = 0

    private val tickRunnable = object : Runnable {
        override fun run() {
            service?.let { s ->
                s.incrementSecond()
                val secs = s.getSeconds()
                val h = secs / 3600
                val m = (secs % 3600) / 60
                val ss = secs % 60
                tvTimer.text = if (h > 0) "%02d:%02d:%02d".format(h, m, ss)
                else "%02d:%02d".format(m, ss)
                val dist = s.getDistanceKm()
                tvTrackInfo.text = "%.2f km".format(dist)

                // PRD 4.4.1: 默认每30秒播报，支持用户调整频率
                val interval = AppPrefs.voicePromptInterval
                if (secs > 0 && secs % interval == 0 && secs != lastAnnounceSecond) {
                    lastAnnounceSecond = secs
                    if (AppPrefs.voicePromptsEnabled) {
                        val minText = if (m > 0) "${m}分" else ""
                        // flush=true 打断之前的播报，防止TTS队列堆积
                        TtsHelper.speak("已跑${minText}${ss}秒，%.1f公里".format(dist), true)
                    }
                }

                updateTrackLine()
            }
            handler.postDelayed(this, 1000L)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as RunningService.RunningBinder).getService()
            bound = true
            service?.startTimer()
            waitForGpsThenCountdown()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_mode)

        // PRD 4.4.1: 屏幕常亮，不会自动锁屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        demandId = intent.getLongExtra("demand_id", 0L)
        tvTimer = findViewById(R.id.tv_timer)
        tvTrackInfo = findViewById(R.id.tv_track_info)

        mapView = findViewById(R.id.map_track)
        mapView.onCreate(savedInstanceState)
        initMap()

        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        findViewById<Button>(R.id.btn_pause).setOnClickListener {
            vibrate(50)
            val paused = service?.togglePause() ?: false
            (it as Button).text = if (paused) "▶" else "⏸"
            Toast.makeText(this, if (paused) "已暂停" else "已继续", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_end).setOnClickListener {
            vibrate(100)
            // PRD 4.4: 结束跑步前弹窗确认
            TtsHelper.speak("确认结束跑步？", true)
            AlertDialog.Builder(this)
                .setTitle("结束跑步")
                .setMessage("确定要结束本次跑步吗？\n计时将停止，跑步记录会被保存。")
                .setPositiveButton("确定结束") { _, _ ->
                    val secs = service?.getSeconds() ?: 0
                    val distKm = service?.getDistanceKm() ?: 0f
                    val track = service?.getTrackJson() ?: ""
                    stopRunning()
                    val intent2 = Intent(this@RunningModeActivity, RunningEndActivity::class.java).apply {
                        putExtra("duration_seconds", secs)
                        putExtra("duration_text", tvTimer.text.toString())
                        putExtra("distance_km", distKm)
                        putExtra("track_json", track)
                        putExtra("demand_id", demandId)
                    }
                    startActivity(intent2)
                    finish()
                }
                .setNegativeButton("继续跑步", null)
                .show()
        }

        findViewById<Button>(R.id.btn_help).setOnClickListener {
            vibrate(200)
            // PRD 4.4.2: 仅拉起系统拨号盘，不自动拨号；按设置顺序优先填入第一位紧急联系人号码
            val contacts = AppPrefs.getEmergencyContacts()
            val emergencyPhone = if (contacts.isNotEmpty()) {
                contacts.first().second
            } else {
                AppPrefs.emergencyContact
            }
            val contactName = if (contacts.isNotEmpty()) contacts.first().first else ""
            val announcePhone = if (contactName.isNotEmpty()) contactName else emergencyPhone

            TtsHelper.speak("紧急求助：即将拨打${announcePhone}，请确认", true)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:$emergencyPhone")
                }
                startActivity(dialIntent)
            } catch (e: Exception) {
                TtsHelper.speak("无法启动拨号盘", true)
                Toast.makeText(this, "无法启动拨号盘", Toast.LENGTH_LONG).show()
            }
            service?.speakPrompt()
        }

        bindService()
    }

    private fun initMap() {
        AMapLocationClient.setApiKey(com.blindrunner.app.util.MapConfig.getAmapKey(this))
        aMap = mapView.map
        aMap?.apply {
            // Follow mode — map auto-centers on user and follows
            myLocationStyle = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW)
                interval(2000)
            }
            isMyLocationEnabled = true
            uiSettings.apply {
                isZoomControlsEnabled = false
                isMyLocationButtonEnabled = false
            }
            mapType = AMap.MAP_TYPE_NORMAL
        }
    }

    // ========== GPS wait + countdown ==========

    private var amapLocated = false

    private fun waitForGpsThenCountdown() {
        tvTimer.text = "定位中"
        tvTrackInfo.text = "等待GPS信号..."
        TtsHelper.speak("正在定位，请稍候", true)

        // Listen for AMap's own location fix — ensures map is centered before starting
        aMap?.setOnMyLocationChangeListener { location ->
            if (!amapLocated && location != null && location.latitude != 0.0) {
                amapLocated = true
                aMap?.moveCamera(CameraUpdateFactory.zoomTo(19f))
                TtsHelper.speak("定位成功", true)
                startCountdown()
            }
        }

        // Fallback timeout — start after 10s even without AMap fix
        handler.postDelayed({
            if (!amapLocated) {
                amapLocated = true
                TtsHelper.speak("定位超时，开始跑步", true)
                startCountdown()
            }
        }, 10000L)
    }

    private fun startCountdown() {
        var count = 3
        val cdRunnable = object : Runnable {
            override fun run() {
                if (count > 0) {
                    tvTimer.text = count.toString()
                    TtsHelper.speak(count.toString(), false)
                    count--
                    handler.postDelayed(this, 1000L)
                } else {
                    tvTimer.text = "00:00"
                    TtsHelper.speak("开始", false)
                    handler.post(tickRunnable)
                    timerRunning = true
                }
            }
        }
        handler.post(cdRunnable)
    }

    // ========== Track line ==========

    private fun updateTrackLine() {
        val pts = service?.getTrackPoints() ?: return
        if (pts.size < 2) return
        val latLngs = pts.map { LatLng(it[1], it[0]) }
        trackPolyline?.remove()
        trackPolyline = aMap?.addPolyline(
            PolylineOptions()
                .addAll(latLngs)
                .width(12f)
                .color(Color.argb(180, 76, 175, 80))
        )
    }

    private fun bindService() {
        val intent = Intent(this, RunningService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun stopRunning() {
        handler.removeCallbacks(tickRunnable)
        timerRunning = false
        TtsHelper.stopSpeaking()
        if (bound) {
            unbindService(connection)
            bound = false
        }
        stopService(Intent(this, RunningService::class.java))
    }

    @SuppressLint("MissingPermission")
    private fun vibrate(ms: Long) {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(ms)
        }
    }

    // ========== 音量上键长按3秒紧急求助（前台） ==========

    private var volumeUpPressTime = 0L
    private var volumeLongPressFired = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!volumeLongPressFired) {
                        volumeUpPressTime = System.currentTimeMillis()
                        // 3秒后触发
                        handler.postDelayed({
                            if (!volumeLongPressFired &&
                                System.currentTimeMillis() - volumeUpPressTime >= 2800L
                            ) {
                                volumeLongPressFired = true
                                triggerEmergencyCall()
                            }
                        }, 3000L)
                    }
                }
                KeyEvent.ACTION_UP -> {
                    // 松手后重置
                    val held = System.currentTimeMillis() - volumeUpPressTime
                    if (held < 2800L) {
                        // 短按：正常调节音量，交给系统处理
                        volumeUpPressTime = 0L
                    }
                }
            }
            // 只有长按触发紧急求助时才消费事件；短按交给系统
            return volumeLongPressFired
        }

        // 音量下键松开时重置长按标记（防止误触后无法再次触发）
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_UP) {
            volumeLongPressFired = false
            volumeUpPressTime = 0L
        }

        return super.dispatchKeyEvent(event)
    }

    private fun triggerEmergencyCall() {
        vibrate(500)
        val contacts = AppPrefs.getEmergencyContacts()
        val emergencyPhone = if (contacts.isNotEmpty()) contacts.first().second
        else AppPrefs.emergencyContact
        val contactName = if (contacts.isNotEmpty()) contacts.first().first else ""

        TtsHelper.speak(
            "紧急求助：即将拨打${if (contactName.isNotEmpty()) contactName else emergencyPhone}",
            true
        )
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$emergencyPhone")
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            TtsHelper.speak("无法启动拨号盘", true)
            Toast.makeText(this, "无法启动拨号盘", Toast.LENGTH_LONG).show()
        }
        // 重置标记，允许再次触发
        handler.postDelayed({ volumeLongPressFired = false }, 2000L)
    }

    // ========== Map lifecycle ==========

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mapView.onDestroy()
        handler.removeCallbacksAndMessages(null)  // 清空所有延迟任务（计时+倒计时+音量长按+超时）
        if (bound) {
            unbindService(connection)
            bound = false
        }
        TtsHelper.stopSpeaking()
        super.onDestroy()
    }
}
