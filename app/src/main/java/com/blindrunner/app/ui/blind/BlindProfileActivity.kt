package com.blindrunner.app.ui.blind

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.ui.auth.LoginActivity
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class BlindProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blind_profile)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_emergency).setOnClickListener {
            startActivity(Intent(this, EmergencyContactActivity::class.java))
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val name = findViewById<EditText>(R.id.et_name).text.toString()
            val contactPhone = findViewById<EditText>(R.id.et_phone).text.toString()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写姓名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 按手机号隔离，不同账号互不干扰
            val phone = AppPrefs.currentUserPhone
            getSharedPreferences("user_$phone", MODE_PRIVATE).edit()
                .putString("profile_name", name)
                .putString("profile_phone", contactPhone)
                .apply()
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 按当前手机号加载个人资料
        val phone = AppPrefs.currentUserPhone
        val prefs = getSharedPreferences("user_$phone", MODE_PRIVATE)
        findViewById<EditText>(R.id.et_name).setText(prefs.getString("profile_name", ""))
        findViewById<EditText>(R.id.et_phone).setText(prefs.getString("profile_phone", ""))

        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.btn_switch).setOnClickListener {
            // 写入正确的SharedPreferences文件（与登录路由同源）
            val phone = AppPrefs.currentUserPhone
            getSharedPreferences("user_$phone", MODE_PRIVATE).edit()
                .putString("user_type", "volunteer").apply()
            AppPrefs.userType = "volunteer"
            TtsHelper.speak("已切换为志愿者身份", true)
            Toast.makeText(this, "已切换为志愿者身份", Toast.LENGTH_SHORT).show()
            // 直接跳转志愿者首页，不走身份选择页
            startActivity(Intent(this, com.blindrunner.app.ui.volunteer.VolunteerHomeActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            AppPrefs.currentUserPhone = ""
            AppPrefs.userType = "blind"
            TtsHelper.speak("已退出登录", true)
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
