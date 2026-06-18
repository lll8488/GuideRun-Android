package com.blindrunner.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class RegisterActivity : AppCompatActivity() {

    private var countdown = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        TtsHelper.speak("欢迎注册助盲跑。请输入您的11位手机号码，然后获取验证码。", true)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etCode = findViewById<EditText>(R.id.et_code)
        val btnGetCode = findViewById<Button>(R.id.btn_get_code)
        val btnNext = findViewById<Button>(R.id.btn_next)

        etPhone.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) TtsHelper.speak("手机号输入框，请输入11位数字", true)
        }

        // 获取验证码
        btnGetCode.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.length != 11) {
                TtsHelper.speak("请输入正确的11位手机号", true)
                Toast.makeText(this, "请输入正确的11位手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 检查是否已注册
            val prefs = getSharedPreferences("user_$phone", MODE_PRIVATE)
            if (prefs.getString("user_type", null) != null) {
                TtsHelper.speak("该手机号已注册，请返回登录", true)
                Toast.makeText(this, "该手机号已注册", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 发送验证码
            TtsHelper.speak("验证码已发送至您的手机", true)
            val mockCode = (100000..999999).random().toString()
            Toast.makeText(this, "验证码（测试）：$mockCode", Toast.LENGTH_LONG).show()
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putString("verify_code_$phone", mockCode)
                .apply()
            startCountdown(btnGetCode)
        }

        // 下一步：验证验证码 → 选择身份
        btnNext.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val code = etCode.text.toString().trim()

            if (phone.length != 11) {
                TtsHelper.speak("请输入正确的11位手机号", true)
                Toast.makeText(this, "请输入正确的11位手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.length != 6) {
                TtsHelper.speak("请输入6位验证码", true)
                Toast.makeText(this, "请输入6位验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedCode = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("verify_code_$phone", null)
            if (code != savedCode) {
                TtsHelper.speak("验证码错误，请重新输入", true)
                Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AppPrefs.currentUserPhone = phone
            TtsHelper.speak("验证成功！请选择您的身份。", true)
            startActivity(Intent(this, IdentitySelectActivity::class.java).apply {
                putExtra("is_register", true)
            })
            finish()
        }
    }

    private fun startCountdown(btn: Button) {
        countdown = 60
        btn.isEnabled = false
        val runnable = object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    btn.text = "${countdown}秒后重试"
                    countdown--
                    handler.postDelayed(this, 1000L)
                } else {
                    btn.text = "获取验证码"
                    btn.isEnabled = true
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
