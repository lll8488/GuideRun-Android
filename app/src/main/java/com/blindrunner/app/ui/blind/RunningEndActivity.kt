package com.blindrunner.app.ui.blind

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.util.AppPrefs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RunningEndActivity : AppCompatActivity() {

    private val app get() = application as BlindRunnerApp
    private var durationSeconds = 0
    private var durationText = ""
    private var distanceKm = 0f
    private var trackJson = ""
    private var demandId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_running_end)

        durationSeconds = intent.getIntExtra("duration_seconds", 0)
        durationText = intent.getStringExtra("duration_text") ?: "00:00:00"
        demandId = intent.getLongExtra("demand_id", 0L)
        distanceKm = intent.getFloatExtra("distance_km", 0f)
        trackJson = intent.getStringExtra("track_json") ?: ""

        val durDisplay = "用时：$durationText"
        val distDisplay = "距离：${String.format("%.2f", distanceKm)} km"
        findViewById<TextView>(R.id.tv_duration).text = durDisplay
        findViewById<TextView>(R.id.tv_distance).text = distDisplay

        // Auto-read results for blind users
        com.blindrunner.app.util.TtsHelper.speak(
            "跑步完成。$durDisplay。$distDisplay", true
        )

        findViewById<Button>(R.id.btn_view_record).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btn_share).setOnClickListener {
            val shareText = "🏃 我刚刚完成了助盲跑！\n用时：$durationText\n距离：${
                String.format("%.2f", distanceKm)
            } km"
            startActivity(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            })
        }

        findViewById<Button>(R.id.btn_back_home).setOnClickListener {
            startActivity(Intent(this, BlindHomeActivity::class.java))
            finish()
        }

        saveRunningRecord()
    }

    private fun saveRunningRecord() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val location = getLocationFromActiveDemand()
        val phone = AppPrefs.currentUserPhone

        lifecycleScope.launch {
            var volunteerPhone = ""
            try {
                // 事务包裹（room-ktx 的 withTransaction 支持 suspend DAO 调用）
                app.database.withTransaction {
                        app.database.runningRecordDao().insert(
                            RunningRecordEntity(
                                date = today,
                                durationMinutes = (durationSeconds + 59) / 60,
                                location = location,
                                distanceKm = distanceKm,
                                status = "completed",
                                ownerPhone = phone,
                                demandId = demandId,
                                trackJson = trackJson
                            )
                        )

                        if (demandId > 0) {
                            val demand = app.database.runningRecordDao().getRecordById(demandId)
                            if (demand != null && demand.status == "accepted" && demand.blindConfirmed) {
                                app.database.runningRecordDao().acceptDemand(
                                    demandId, "completed",
                                    demand.volunteerPhone,
                                    demand.volunteerNote
                                )
                                volunteerPhone = demand.volunteerPhone
                            }
                        }

                        app.database.userDao().incrementRunStats(phone, distanceKm)
                        if (volunteerPhone.isNotEmpty()) {
                            app.database.userDao().incrementRunStats(volunteerPhone, distanceKm)
                        }
                }

                Toast.makeText(this@RunningEndActivity, "跑步记录已保存", Toast.LENGTH_SHORT).show()
                if (volunteerPhone.isNotEmpty()) showRatingDialog(volunteerPhone)
                checkAchievements(phone)
            } catch (e: Exception) {
                Toast.makeText(this@RunningEndActivity, "记录保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocationFromActiveDemand(): String {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getString("last_run_location", null) ?: "未指定地点"
    }

    private fun checkAchievements(phone: String) {
        lifecycleScope.launch {
            try {
                val user = app.database.userDao().getUserByPhone(phone) ?: return@launch
                val badges = mutableListOf<String>()
                if (user.totalRuns == 1) badges.add("🏃 首次跑步！")
                if (user.totalRuns == 10) badges.add("🏅 完成10次跑步")
                if (user.totalRuns == 50) badges.add("🏆 完成50次跑步")
                if (user.totalDistanceKm >= 10f && user.totalDistanceKm - distanceKm < 10f)
                    badges.add("🛣️ 累计10公里")
                if (user.totalDistanceKm >= 100f && user.totalDistanceKm - distanceKm < 100f)
                    badges.add("🏅 累计100公里")
                if (badges.isNotEmpty()) {
                    Toast.makeText(this@RunningEndActivity,
                        "🎉 ${badges.joinToString("\n")}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("RunningEnd", "achievement check failed", e)
            }
        }
    }

    private fun showRatingDialog(volunteerPhone: String) {
        com.blindrunner.app.util.TtsHelper.speak("请为本次陪跑体验打分，1到5分，5分为最好", true)
        val stars = arrayOf("⭐1 差", "⭐⭐2", "⭐⭐⭐3 一般", "⭐⭐⭐⭐4 好", "⭐⭐⭐⭐⭐5 非常好")
        AlertDialog.Builder(this)
            .setTitle("评价志愿者 (请打分)")
            .setItems(stars) { _, which ->
                val rating = (which + 1).toFloat()
                lifecycleScope.launch {
                    try {
                        val user = app.database.userDao().getUserByPhone(volunteerPhone)
                        if (user != null) {
                            val newCount = user.ratingCount + 1
                            val newRating = (user.rating * user.ratingCount + rating) / newCount
                            app.database.userDao().updateRating(volunteerPhone, newRating, newCount)
                            com.blindrunner.app.util.TtsHelper.speak("感谢您的${which + 1}分评价！", true)
                            Toast.makeText(this@RunningEndActivity, "感谢您的${which + 1}分评价！", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                android.util.Log.e("RunningEnd", "achievement check failed", e)
            }
                }
            }
            .setNegativeButton("跳过") { _, _ ->
                com.blindrunner.app.util.TtsHelper.speak("已跳过评价", false)
            }
            .show()
    }
}
