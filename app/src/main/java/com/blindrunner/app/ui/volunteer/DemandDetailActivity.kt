package com.blindrunner.app.ui.volunteer

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DemandDetailActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private var recordId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demand_detail)

        val location = intent.getStringExtra("location") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val duration = intent.getStringExtra("duration") ?: ""
        val distance = intent.getStringExtra("distance") ?: ""
        recordId = intent.getLongExtra("id", 0)

        findViewById<TextView>(R.id.tv_detail_location).text = "📍 $location"
        findViewById<TextView>(R.id.tv_detail_address).text = location
        findViewById<TextView>(R.id.tv_detail_date).text = date
        findViewById<TextView>(R.id.tv_detail_duration).text = "${duration}分钟"
        findViewById<TextView>(R.id.tv_detail_distance).text = "${distance}km"

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // PRD 4.2: 未通过考核者接单按钮置灰
        val btnAccept = findViewById<Button>(R.id.btn_accept)
        if (!AppPrefs.examPassed) {
            btnAccept.isEnabled = false
            btnAccept.text = "请先通过考核（80分及以上）"
            btnAccept.alpha = 0.4f
            btnAccept.contentDescription = "接单按钮已禁用，请先通过陪跑绳规范考核达到80分及以上"
        } else {
            btnAccept.isEnabled = true
            btnAccept.text = "确认接单"
            btnAccept.alpha = 1.0f
        }
        btnAccept.setOnClickListener { acceptDemand() }

        findViewById<Button>(R.id.btn_navigate).setOnClickListener {
            try {
                val uri = android.net.Uri.parse("geo:0,0?q=$location")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                Toast.makeText(this, "未找到地图应用", Toast.LENGTH_SHORT).show()
            }
        }

        // PRD 4.6: 加载静态地图（约300×200dp）
        loadStaticMap()
    }

    /**
     * PRD 4.6: 使用高德静态地图API加载跑步地点地图
     * 尺寸约300×200dp（600×400px at 2x），带标记点
     */
    private fun loadStaticMap() {
        lifecycleScope.launch {
            try {
                val entity = withContext(Dispatchers.IO) {
                    app.database.runningRecordDao().getRecordById(recordId)
                } ?: return@launch

                if (entity.lat == 0.0 && entity.lng == 0.0) return@launch

                val lat = entity.lat
                val lng = entity.lng
                val location = entity.location

                // 高德静态地图 API
                val key = com.blindrunner.app.util.MapConfig.getAmapKey(this@DemandDetailActivity)
                val width = 600
                val height = 400
                val url = "https://restapi.amap.com/v3/staticmap?" +
                    "location=$lng,$lat&zoom=15&size=${width}*${height}&" +
                    "markers=mid,0xFF1B5E20,A:$lng,$lat&key=$key"

                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.doInput = true
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.connect()
                        val input = conn.inputStream
                        val bmp = BitmapFactory.decodeStream(input)
                        input.close()
                        conn.disconnect()
                        bmp
                    } catch (_: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    val mapImage = findViewById<ImageView>(R.id.iv_static_map)
                    val mapPlaceholder = findViewById<View>(R.id.map_placeholder)
                    mapImage.setImageBitmap(bitmap)
                    mapImage.visibility = View.VISIBLE
                    mapImage.contentDescription = "跑步地点地图：$location"
                    mapPlaceholder.visibility = View.GONE
                }
            } catch (e: Exception) {
                android.util.Log.e("DemandDetail", "loadStaticMap failed", e)
            }
        }
    }

    private fun acceptDemand() {
        if (!AppPrefs.examPassed) {
            Toast.makeText(this, "请先通过陪跑绳规范考核", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, TrainingCampActivity::class.java))
            return
        }

        // Show notes dialog first
        val noteInput = EditText(this).apply {
            hint = "给视障跑者留言（可选）"
            textSize = 16f
            setPadding(32, 32, 32, 32)
            maxLines = 3
        }

        AlertDialog.Builder(this)
            .setTitle("确认接单")
            .setMessage("接单后双方联系方式将互相可见\n\n可添加备注信息：")
            .setView(noteInput)
            .setPositiveButton("确认接单") { _, _ ->
                doAccept(noteInput.text.toString().trim())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doAccept(note: String) {
        val volunteerPhone = AppPrefs.currentUserPhone
        val location = intent.getStringExtra("location") ?: ""

        lifecycleScope.launch {
            try {
                // UPDATE existing demand status, not insert new record
                app.database.runningRecordDao().acceptDemand(
                    id = recordId,
                    status = "accepted",
                    volunteerPhone = volunteerPhone,
                    note = note
                )

                // Get owner contact
                val entity = app.database.runningRecordDao().getRecordById(recordId)
                val ownerPhone = entity?.ownerPhone ?: "未知"

                NotificationHelper.notifyDemandAccepted(
                    this@DemandDetailActivity, location, volunteerPhone
                )

                AlertDialog.Builder(this@DemandDetailActivity)
                    .setTitle("接单成功！")
                    .setMessage("跑步地点：$location\n视障用户电话：$ownerPhone\n\n双方可互相查看联系方式，请及时联系确认。")
                    .setPositiveButton("确定") { _, _ -> finish() }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@DemandDetailActivity, "接单失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
