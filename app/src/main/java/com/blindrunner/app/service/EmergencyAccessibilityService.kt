package com.blindrunner.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

/**
 * PRD 4.4.2: 无障碍服务监听音量上键长按3秒触发紧急求助
 * 覆盖前台、后台、锁屏所有状态
 * 需要在 设置→无障碍 中手动开启
 */
class EmergencyAccessibilityService : AccessibilityService() {

    private var volumeUpDownTime = 0L
    private var fired = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        TtsHelper.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不处理常规无障碍事件，只做按键监听
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (!fired) {
                    volumeUpDownTime = System.currentTimeMillis()
                    handler.postDelayed({
                        if (!fired &&
                            System.currentTimeMillis() - volumeUpDownTime >= 2800L
                        ) {
                            fired = true
                            triggerEmergency()
                        }
                    }, 3000L)
                }
            }
            KeyEvent.ACTION_UP -> {
                if (System.currentTimeMillis() - volumeUpDownTime < 2800L) {
                    // 短按交给系统
                }
            }
        }
        return fired // 长按触发时消费事件
    }

    private fun triggerEmergency() {
        // 震动500ms
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }

        // 语音提示
        val contacts = AppPrefs.getEmergencyContacts()
        val phone = if (contacts.isNotEmpty()) contacts.first().second
        else AppPrefs.emergencyContact
        val name = if (contacts.isNotEmpty()) contacts.first().first else phone

        TtsHelper.speak("紧急求助：即将拨打$name", true)

        // 拉起拨号盘（不自动拨号）
        if (phone.isNotEmpty()) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$phone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(dialIntent)
            } catch (_: Exception) {}
        }

        // 2秒后重置
        handler.postDelayed({ fired = false }, 2000L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
