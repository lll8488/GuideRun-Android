package com.blindrunner.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.blindrunner.app.R
import com.blindrunner.app.ui.blind.RunningModeActivity
import com.blindrunner.app.util.AppPrefs

class RunningService : Service() {

    private val binder = RunningBinder()
    private var seconds = 0
    private var isPaused = false
    private var voiceIndex = 0

    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0f
    private val trackPoints = mutableListOf<DoubleArray>()

    private val voicePrompts = listOf(
        "保持平稳呼吸，注意路面情况。",
        "前方若有转弯，请提前通过陪跑绳传递信号。",
        "保持当前节奏，配合陪跑绳力度反馈。",
        "注意避让障碍，提前减速。"
    )

    inner class RunningBinder : Binder() {
        fun getService(): RunningService = this@RunningService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // TTS已由全局TtsHelper管理，不创建独立实例（避免双TTS冲突）
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(1001, notification)
        return START_STICKY
    }

    fun startTimer() {
        seconds = 0
        isPaused = false
        totalDistanceMeters = 0f
        lastLocation = null
        trackPoints.clear()
        startLocationTracking()
    }

    fun togglePause(): Boolean {
        isPaused = !isPaused
        return isPaused
    }

    fun getSeconds(): Int = seconds
    fun getDistanceKm(): Float = totalDistanceMeters / 1000f
    fun getLastLocation(): Location? = lastLocation
    fun getTrackPoints(): List<DoubleArray> = trackPoints.toList()

    fun getTrackJson(): String {
        if (trackPoints.isEmpty()) return ""
        val sb = StringBuilder("[")
        for ((i, pt) in trackPoints.withIndex()) {
            if (i > 0) sb.append(",")
            sb.append("[${pt[0]},${pt[1]}]")
        }
        sb.append("]")
        return sb.toString()
    }

    fun incrementSecond() {
        if (!isPaused) seconds++
    }

    fun speakPrompt() {
        if (isPaused) return
        if (!AppPrefs.voicePromptsEnabled) return
        // 使用全局TtsHelper而非独立TTS实例，避免双重TTS争抢音频焦点
        com.blindrunner.app.util.TtsHelper.speak(
            voicePrompts[voiceIndex % voicePrompts.size], true
        )
        voiceIndex++
    }

    fun updateNotification() {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val timeText = "%02d:%02d:%02d".format(h, m, s)
        val distText = "%.2f km".format(totalDistanceMeters / 1000f)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("助盲跑进行中")
            .setContentText("已跑步 $timeText · $distText")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, RunningModeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build())
    }

    @Suppress("MissingPermission")
    private fun startLocationTracking() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000L, 5f,
                locationListener,
                mainLooper
            )
        } catch (_: Exception) {}
    }

    private val locationListener = LocationListener { loc ->
        if (isPaused) return@LocationListener
        lastLocation?.let { last ->
            val dist = FloatArray(1)
            Location.distanceBetween(last.latitude, last.longitude, loc.latitude, loc.longitude, dist)
            totalDistanceMeters += dist[0]
        }
        lastLocation = loc
        trackPoints.add(doubleArrayOf(loc.longitude, loc.latitude))
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(locationListener)
        locationManager = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("助盲跑进行中")
            .setContentText("正在计时...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, RunningModeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "助盲跑服务", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "跑步计时前台服务通知，请勿关闭以确保跑步安全"
            setShowBadge(true)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "running_service_channel"
    }
}
