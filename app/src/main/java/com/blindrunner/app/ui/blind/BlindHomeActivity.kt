package com.blindrunner.app.ui.blind

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.domain.model.RunningRecord
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper
import kotlinx.coroutines.launch

class BlindHomeActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private val demands = mutableListOf<RunningRecord>()
    private lateinit var adapter: BlindDemandAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blind_home)

        TtsHelper.speak("视障用户首页。四个功能按钮：发布陪跑需求、开始跑步、历史记录、个人中心。长按任意按钮可听取功能说明。", true)

        findViewById<CardView>(R.id.btn_publish).apply {
            setOnClickListener {
                TtsHelper.speakNavigation("发布需求")
                startActivity(Intent(this@BlindHomeActivity, PublishDemandActivity::class.java))
            }
            setOnLongClickListener {
                TtsHelper.speak("发布陪跑需求：设置跑步日期、时间和地点，等待志愿者接单", true); true
            }
        }
        findViewById<CardView>(R.id.btn_start_run).apply {
            setOnLongClickListener {
                TtsHelper.speak("开始跑步：使用GPS定位并记录您的跑步数据", true); true
            }
        }
        findViewById<CardView>(R.id.btn_start_run).setOnClickListener {
            lifecycleScope.launch {
                val phone = AppPrefs.currentUserPhone
                val confirmed = app.database.runningRecordDao()
                    .getRecordsByOwnerPhone(phone)
                    .find { it.status == "accepted" && it.blindConfirmed }
                if (confirmed != null) {
                    TtsHelper.speakNavigation("开始陪跑 — ${confirmed.location}")
                    startActivity(Intent(this@BlindHomeActivity, StartRunningActivity::class.java).apply {
                        putExtra("demand_id", confirmed.id)
                    })
                } else {
                    TtsHelper.speakNavigation("独立跑步模式")
                    startActivity(Intent(this@BlindHomeActivity, StartRunningActivity::class.java))
                }
            }
        }
        findViewById<CardView>(R.id.btn_history).apply {
            setOnClickListener {
                TtsHelper.speakNavigation("历史记录")
                startActivity(Intent(this@BlindHomeActivity, HistoryActivity::class.java))
            }
            setOnLongClickListener {
                TtsHelper.speak("历史记录：查看您所有的跑步记录和统计", true); true
            }
        }
        findViewById<Button>(R.id.btn_profile).apply {
            setOnClickListener {
                TtsHelper.speakNavigation("个人中心")
                startActivity(Intent(this@BlindHomeActivity, BlindProfileActivity::class.java))
            }
            setOnLongClickListener {
                TtsHelper.speak("个人中心：管理您的个人信息和紧急联系人", true); true
            }
        }

        findViewById<androidx.cardview.widget.CardView>(R.id.btn_settings).apply {
            setOnClickListener {
                TtsHelper.speakNavigation("设置")
                startActivity(Intent(this@BlindHomeActivity, SettingsActivity::class.java))
            }
        }

        adapter = BlindDemandAdapter { demand ->
            showDemandDetail(demand)
        }

        findViewById<RecyclerView>(R.id.rv_my_demands).apply {
            layoutManager = LinearLayoutManager(this@BlindHomeActivity)
            adapter = this@BlindHomeActivity.adapter
        }

        loadMyDemands()
    }

    override fun onResume() {
        super.onResume()
        loadMyDemands()
    }

    private fun loadMyDemands() {
        lifecycleScope.launch {
            try {
                val phone = AppPrefs.currentUserPhone
                val entities = app.database.runningRecordDao().getRecordsByOwnerPhone(phone)
                // PRD 4.1: 视障用户可查看"我的发布"四状态列表（待接单/已接单/已完成/已取消）
                val mapped = entities
                    .sortedByDescending { it.id }
                    .map { RunningRecord(it.id, it.date, it.durationMinutes, it.location, it.distanceKm, it.status) }
                adapter.submitList(mapped)
                demands.clear()
                demands.addAll(mapped)
                val hasData = demands.isNotEmpty()
                findViewById<TextView>(R.id.tv_demand_section).visibility = if (hasData) View.VISIBLE else View.GONE
                findViewById<RecyclerView>(R.id.rv_my_demands).visibility = if (hasData) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@BlindHomeActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDemandDetail(demand: RunningRecord) {
        lifecycleScope.launch {
            val entity = app.database.runningRecordDao().getRecordById(demand.id)
            if (entity == null) { Toast.makeText(this@BlindHomeActivity, "需求不存在", Toast.LENGTH_SHORT).show(); return@launch }

            when (entity.status) {
                "pending" -> {
                    AlertDialog.Builder(this@BlindHomeActivity)
                        .setTitle("需求详情")
                        .setMessage("地点：${entity.location}\n日期：${entity.date}\n时长：${entity.durationMinutes}分钟\n\n状态：等待志愿者接单中...")
                        .setPositiveButton("知道了", null)
                        .setNegativeButton("取消需求") { _, _ ->
                            lifecycleScope.launch { app.database.runningRecordDao().cancelDemand(demand.id); loadMyDemands() }
                        }
                        .show()
                }
                "accepted" -> {
                    val vPhone = entity.volunteerPhone.ifEmpty { "未知" }
                    val vNote = entity.volunteerNote.ifEmpty { "无" }
                    val confirmed = entity.blindConfirmed

                    // Load volunteer stats
                    val vUser = app.database.userDao().getUserByPhone(vPhone)
                    val ratingStr = if (vUser != null && vUser.ratingCount > 0)
                        "⭐%.1f (${vUser.totalRuns}次)".format(vUser.rating) else "暂无评分"
                    val vInfo = "志愿者：$vPhone\n评分：$ratingStr\n备注：$vNote"

                    TtsHelper.speak("您的需求已被接单，志愿者电话：${vPhone}，备注：${vNote}", true)

                    AlertDialog.Builder(this@BlindHomeActivity)
                        .setTitle("🎉 已被接单！")
                        .setMessage("地点：${entity.location}\n日期：${entity.date}\n\n$vInfo\n\n${if (confirmed) "✅ 已确认由该志愿者领跑" else "是否确认由这位志愿者领跑？"}")
                        .apply {
                            if (!confirmed) {
                                setPositiveButton("✅ 确认领跑") { _, _ ->
                                    lifecycleScope.launch { app.database.runningRecordDao().confirmVolunteer(demand.id); loadMyDemands() }
                                    TtsHelper.speak("已确认")
                                }
                                setNegativeButton("❌ 拒绝") { _, _ ->
                                    lifecycleScope.launch { app.database.runningRecordDao().cancelDemand(demand.id); loadMyDemands() }
                                }
                            } else {
                                setPositiveButton("✅ 完成跑步") { _, _ ->
                                    lifecycleScope.launch {
                                        app.database.runningRecordDao().acceptDemand(demand.id, "completed", entity.volunteerPhone, entity.volunteerNote)
                                        loadMyDemands()
                                    }
                                    TtsHelper.speak("跑步已完成，感谢志愿者的陪伴")
                                }
                                setNegativeButton("取消需求") { _, _ ->
                                    lifecycleScope.launch { app.database.runningRecordDao().cancelDemand(demand.id); loadMyDemands() }
                                }
                            }
                        }
                        .show()
                }
                "completed" -> {
                    AlertDialog.Builder(this@BlindHomeActivity)
                        .setTitle("✅ 已完成")
                        .setMessage("地点：${entity.location}\n日期：${entity.date}\n时长：${entity.durationMinutes}分钟\n\n志愿者电话：${entity.volunteerPhone}\n\n感谢使用助盲跑！")
                        .setPositiveButton("知道了", null)
                        .show()
                }
                else -> Toast.makeText(this@BlindHomeActivity, "状态：${entity.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class BlindDemandAdapter(
    private val onClick: (RunningRecord) -> Unit
) : ListAdapter<RunningRecord, BlindDemandAdapter.VH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RunningRecord>() {
        override fun areItemsTheSame(old: RunningRecord, new: RunningRecord) = old.id == new.id
        override fun areContentsTheSame(old: RunningRecord, new: RunningRecord) = old == new
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvLocation: TextView = view.findViewById(R.id.tv_demand_location)
        val tvDate: TextView = view.findViewById(R.id.tv_demand_date)
        val tvStatus: TextView = view.findViewById(R.id.tv_demand_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blind_demand, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvLocation.text = item.location
        holder.tvDate.text = "${item.date}  ${item.durationMinutes}分钟"
        holder.tvStatus.text = item.statusLabel()
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            Toast.makeText(holder.itemView.context,
                "${item.statusLabel()} — 长按可查看详情", Toast.LENGTH_SHORT).show()
            true
        }
    }
}

fun RunningRecord.statusLabel(): String = when (status) {
    "pending" -> "待接单"
    "accepted" -> "已接单 ✓"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    else -> status
}
