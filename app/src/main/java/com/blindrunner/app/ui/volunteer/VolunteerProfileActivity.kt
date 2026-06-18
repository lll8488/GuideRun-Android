package com.blindrunner.app.ui.volunteer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.ui.auth.LoginActivity
import com.blindrunner.app.ui.blind.BlindHomeActivity
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper

class VolunteerProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_profile)

        val passed = AppPrefs.examPassed
        val score = AppPrefs.examScore

        findViewById<TextView>(R.id.tv_exam_status).apply {
            text = if (passed) "考核状态：已通过（$score 分）" else "考核状态：未通过"
            setBackgroundColor(
                if (passed) android.graphics.Color.parseColor("#C8E6C9") else android.graphics.Color.parseColor("#FFF3E0")
            )
            setTextColor(
                if (passed) android.graphics.Color.parseColor("#2E7D32") else android.graphics.Color.parseColor("#E65100")
            )
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_training).setOnClickListener {
            startActivity(Intent(this, TrainingCampActivity::class.java))
        }

        // 按当前手机号加载个人资料（不同账号隔离）
        val myPhone = AppPrefs.currentUserPhone
        val userPrefs = getSharedPreferences("user_$myPhone", MODE_PRIVATE)
        findViewById<EditText>(R.id.et_name).setText(userPrefs.getString("profile_name", ""))
        findViewById<EditText>(R.id.et_phone).setText(userPrefs.getString("profile_phone", ""))

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val name = findViewById<EditText>(R.id.et_name).text.toString()
            val contactPhone = findViewById<EditText>(R.id.et_phone).text.toString()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写姓名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            userPrefs.edit()
                .putString("profile_name", name)
                .putString("profile_phone", contactPhone)
                .apply()
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_switch).setOnClickListener {
            // 写入正确的SharedPreferences文件（与登录路由同源）
            val phone = AppPrefs.currentUserPhone
            getSharedPreferences("user_$phone", MODE_PRIVATE).edit()
                .putString("user_type", "blind").apply()
            AppPrefs.userType = "blind"
            TtsHelper.speak("已切换为视障用户身份", true)
            Toast.makeText(this, "已切换为视障用户身份", Toast.LENGTH_SHORT).show()
            // 直接跳转视障首页
            startActivity(Intent(this, BlindHomeActivity::class.java))
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
