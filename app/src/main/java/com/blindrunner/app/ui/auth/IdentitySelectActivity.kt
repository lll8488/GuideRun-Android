package com.blindrunner.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.data.local.entity.UserEntity
import com.blindrunner.app.ui.blind.BlindHomeActivity
import com.blindrunner.app.ui.volunteer.VolunteerHomeActivity
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper
import kotlinx.coroutines.launch

class IdentitySelectActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_select)

        // Voice guidance for registration
        TtsHelper.speak("您好！请选择您的身份：视障用户，或陪跑志愿者。选择后将进入对应首页。", true)

        val isRegister = intent.getBooleanExtra("is_register", false)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<CardView>(R.id.btn_blind).setOnClickListener {
            saveAndGo("blind", BlindHomeActivity::class.java, isRegister)
        }

        findViewById<CardView>(R.id.btn_volunteer).setOnClickListener {
            saveAndGo("volunteer", VolunteerHomeActivity::class.java, isRegister)
        }
    }

    private fun saveAndGo(type: String, target: Class<*>, isRegister: Boolean) {
        val phone = AppPrefs.currentUserPhone
        val prefs = getSharedPreferences("user_$phone", MODE_PRIVATE)

        if (isRegister) {
            // 纯验证码注册，无需密码
            prefs.edit()
                .putBoolean("first_login", false)
                .putString("user_type", type)
                .apply()
            TtsHelper.speak("注册成功！欢迎使用助盲跑。正在进入${if (type == "blind") "视障" else "志愿者"}首页。", true)
        } else {
            prefs.edit()
                .putBoolean("first_login", false)
                .putString("user_type", type)
                .apply()
            TtsHelper.speak("身份已选择，您可以在个人中心切换身份。", true)
        }

        // 同步更新 AppPrefs.userType（其他页面读取时保持一致）
        AppPrefs.userType = type

        lifecycleScope.launch {
            val existing = app.database.userDao().getUserByPhone(phone)
            if (existing == null) {
                app.database.userDao().insert(UserEntity(
                    phone = phone,
                    name = if (type == "blind") "视障用户" else "志愿者",
                    userType = type
                ))
            }
        }

        Toast.makeText(this, if (isRegister) "注册成功" else "已选择${if (type == "blind") "视障用户" else "陪跑志愿者"}身份", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, target))
        finish()
    }
}
