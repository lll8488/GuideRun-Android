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
import com.blindrunner.app.ui.blind.BlindHomeActivity
import com.blindrunner.app.ui.volunteer.VolunteerHomeActivity
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class LoginActivity : AppCompatActivity() {

    private var countdown = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etPhone = findViewById<EditText>(R.id.et_phone)
        val etCode = findViewById<EditText>(R.id.et_code)
        val btnGetCode = findViewById<Button>(R.id.btn_get_code)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnRegister = findViewById<Button>(R.id.btn_register)

        // 获取验证码
        btnGetCode.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.length != 11) {
                Toast.makeText(this, "请输入正确的11位手机号", Toast.LENGTH_SHORT).show()
                TtsHelper.speak("请输入正确的11位手机号", true)
                return@setOnClickListener
            }
            TtsHelper.speak("验证码已发送至您的手机", true)
            val mockCode = (100000..999999).random().toString()
            Toast.makeText(this, "验证码（测试）：$mockCode", Toast.LENGTH_LONG).show()
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putString("verify_code_$phone", mockCode)
                .apply()
            startCountdown(btnGetCode)
        }

        // 登录
        btnLogin.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val code = etCode.text.toString().trim()

            if (phone.length != 11) {
                Toast.makeText(this, "请输入正确的11位手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.length != 6) {
                Toast.makeText(this, "请输入6位验证码", Toast.LENGTH_SHORT).show()
                TtsHelper.speak("请输入6位验证码", true)
                return@setOnClickListener
            }

            val savedCode = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("verify_code_$phone", null)
            if (code != savedCode) {
                Toast.makeText(this, "验证码错误", Toast.LENGTH_SHORT).show()
                TtsHelper.speak("验证码错误，请重新输入", true)
                return@setOnClickListener
            }

            AppPrefs.currentUserPhone = phone
            navigateToHome(phone)
            finish()
        }

        // 注册
        btnRegister.setOnClickListener {
            TtsHelper.speak("正在跳转到注册页面", true)
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 语音引导
        handler.postDelayed({
            TtsHelper.speak("欢迎使用助盲跑。请输入手机号，获取验证码后登录。")
        }, 1500)
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

    private fun navigateToHome(phone: String) {
        val prefs = getSharedPreferences("user_$phone", MODE_PRIVATE)
        val userType = prefs.getString("user_type", "") ?: ""
        val firstLogin = prefs.getBoolean("first_login", true)

        if (userType.isEmpty() || firstLogin) {
            startActivity(Intent(this, IdentitySelectActivity::class.java).apply {
                putExtra("is_register", false)
            })
        } else if (userType == "volunteer") {
            startActivity(Intent(this, VolunteerHomeActivity::class.java))
        } else {
            startActivity(Intent(this, BlindHomeActivity::class.java))
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
