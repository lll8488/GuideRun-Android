package com.blindrunner.app.ui.blind

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class PublishDemandActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedLat = 0.0
    private var selectedLng = 0.0

    private var selectedDistanceMin = 1
    private var selectedDistanceMax = 3

    private val mapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getStringExtra("selected_address")
                ?: return@registerForActivityResult
            selectedLat = result.data?.getDoubleExtra("selected_lat", 0.0) ?: 0.0
            selectedLng = result.data?.getDoubleExtra("selected_lng", 0.0) ?: 0.0
            findViewById<EditText>(R.id.et_location).setText(address)
            Toast.makeText(this, "已填入地址", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish_demand)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // Date picker
        findViewById<Button>(R.id.btn_date).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "%d-%02d-%02d".format(y, m + 1, d)
                findViewById<Button>(R.id.btn_date).text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Time picker
        findViewById<Button>(R.id.btn_time).setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, min ->
                selectedTime = "%02d:%02d".format(h, min)
                findViewById<Button>(R.id.btn_time).text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // Map pick
        findViewById<Button>(R.id.btn_map_pick).setOnClickListener {
            val intent = android.content.Intent(this, MapPickerActivity::class.java)
            val searchQuery = findViewById<EditText>(R.id.et_location).text.toString()
            if (searchQuery.isNotEmpty()) {
                intent.putExtra("search_query", searchQuery)
            }
            mapLauncher.launch(intent)
        }

        // Address search button
        findViewById<Button>(R.id.btn_search_address).setOnClickListener {
            val query = findViewById<EditText>(R.id.et_location).text.toString()
            if (query.isNotEmpty()) {
                val intent = android.content.Intent(this, MapPickerActivity::class.java)
                intent.putExtra("search_query", query)
                mapLauncher.launch(intent)
            } else {
                Toast.makeText(this, "请先输入地址", Toast.LENGTH_SHORT).show()
            }
        }

        // Distance buttons (large, good for blind users)
        val distBtns = mapOf(
            R.id.btn_dist_1_3 to (1 to 3), R.id.btn_dist_3_5 to (3 to 5),
            R.id.btn_dist_5_10 to (5 to 10), R.id.btn_dist_10_15 to (10 to 15),
            R.id.btn_dist_15_20 to (15 to 20), R.id.btn_dist_20_plus to (20 to 99)
        )
        val allDistIds = distBtns.keys.toList()
        distBtns.forEach { (id, range) ->
            findViewById<Button>(id).setOnClickListener {
                selectedDistanceMin = range.first
                selectedDistanceMax = range.second
                allDistIds.forEach { bid ->
                    findViewById<Button>(bid).apply {
                        val selected = bid == id
                        setBackgroundResource(if (selected) R.drawable.btn_green_round else R.drawable.btn_light_round)
                        setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFF333333.toInt())
                    }
                }
                com.blindrunner.app.util.TtsHelper.speak(
                    "已选择 ${range.first} 到 ${if (range.second == 99) "20以上" else "${range.second}"} 公里", true)
            }
        }

        // Submit — PRD 4.3: 发布前二次语音确认，需求标注"陪跑绳模式"标签
        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            val location = findViewById<EditText>(R.id.et_location).text.toString()

            if (selectedDate.isEmpty() || selectedTime.isEmpty() || location.isEmpty()) {
                TtsHelper.speak("请完善跑步日期、时间和地点", true)
                Toast.makeText(this, "请完善跑步日期、时间和地点", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val confirmMsg = "确认发布陪跑需求？\n" +
                "日期：${selectedDate}\n" +
                "时间：${selectedTime}\n" +
                "地点：${location}\n" +
                "距离：${selectedDistanceMin}-${selectedDistanceMax}公里\n" +
                "模式：陪跑绳引导模式"

            TtsHelper.speak(
                "请确认需求信息。${selectedDate}，${selectedTime}，" +
                "${location}，距离${selectedDistanceMin}到${selectedDistanceMax}公里，" +
                "模式为陪跑绳引导模式。确认请点击确定。",
                true
            )

            AlertDialog.Builder(this)
                .setTitle("确认发布需求")
                .setMessage(confirmMsg)
                .setPositiveButton("确认发布") { _, _ ->
                    doPublish(location)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        loadFrequentAddresses()
    }

    private fun doPublish(location: String) {
        lifecycleScope.launch {
            try {
                app.database.runningRecordDao().insert(
                    com.blindrunner.app.data.local.entity.RunningRecordEntity(
                        date = "$selectedDate $selectedTime",
                        durationMinutes = selectedDistanceMax * 10,
                        location = location,
                        distanceKm = (selectedDistanceMin + selectedDistanceMax) / 2f,
                        status = "pending",
                        ownerPhone = AppPrefs.currentUserPhone,
                        lat = selectedLat,
                        lng = selectedLng
                    )
                )
                TtsHelper.speak("需求发布成功！等待志愿者接单。", true)
                Toast.makeText(this@PublishDemandActivity, "需求发布成功！等待志愿者接单", Toast.LENGTH_LONG).show()
                com.blindrunner.app.util.NotificationHelper.notifyDemandStatusChange(
                    this@PublishDemandActivity, "新需求：$location")
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                TtsHelper.speak("发布失败，请重试", true)
                Toast.makeText(this@PublishDemandActivity, "发布失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFrequentAddresses() {
        lifecycleScope.launch {
            try {
                val phone = AppPrefs.currentUserPhone
                val all = withContext(Dispatchers.IO) {
                    app.database.runningRecordDao().getRecordsByOwnerPhone(phone)
                }
                val locations = all.map { it.location }
                    .filter { it.isNotEmpty() && it != "未指定地点" }
                    .distinct()
                    .take(4)

                if (locations.isNotEmpty()) {
                    val title = findViewById<TextView>(R.id.tv_frequent_title)
                    val container = findViewById<LinearLayout>(R.id.ll_frequent_addresses)
                    title.visibility = android.view.View.VISIBLE
                    container.visibility = android.view.View.VISIBLE
                    container.removeAllViews()

                    for (loc in locations) {
                        val chip = LayoutInflater.from(this@PublishDemandActivity)
                            .inflate(android.R.layout.simple_list_item_1, container, false) as TextView
                        chip.text = loc
                        chip.setTextColor(0xFF1B5E20.toInt())
                        chip.setBackgroundResource(R.drawable.tag_normal)
                        chip.setPadding(24, 12, 24, 12)
                        chip.setOnClickListener {
                            findViewById<EditText>(R.id.et_location).setText(loc)
                            Toast.makeText(this@PublishDemandActivity, "已填入常用地址", Toast.LENGTH_SHORT).show()
                        }
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { marginEnd = 12 }
                        container.addView(chip, params)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PublishDemand", "loadFrequent failed", e)
            }
        }
    }
}
