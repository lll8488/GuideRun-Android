package com.blindrunner.app.ui.volunteer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.launch

class VolunteerHomeActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private val myOrders = mutableListOf<RunningRecord>()
    private val completedRuns = mutableListOf<RunningRecord>()
    private lateinit var adapter: MyOrdersAdapter
    private lateinit var completedAdapter: MyOrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_home)

        findViewById<CardView>(R.id.btn_demand_list).setOnClickListener {
            startActivity(Intent(this, DemandListActivity::class.java))
        }
        findViewById<CardView>(R.id.btn_training).setOnClickListener {
            startActivity(Intent(this, TrainingCampActivity::class.java))
        }
        findViewById<View>(R.id.btn_profile).setOnClickListener {
            startActivity(Intent(this, VolunteerProfileActivity::class.java))
        }
        findViewById<CardView>(R.id.btn_leaderboard).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        adapter = MyOrdersAdapter { demand ->
            AlertDialog.Builder(this)
                .setTitle("已接单")
                .setMessage("地点：${demand.location}\n日期：${demand.date}\n时长：${demand.durationMinutes}分钟\n\n如需取消接单，请点击下方按钮")
                .setPositiveButton("知道了", null)
                .setNegativeButton("取消接单") { _, _ ->
                    lifecycleScope.launch {
                        app.database.runningRecordDao().cancelDemand(demand.id)
                        loadMyOrders()
                        Toast.makeText(this@VolunteerHomeActivity, "已取消接单", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        findViewById<RecyclerView>(R.id.rv_my_orders).apply {
            layoutManager = LinearLayoutManager(this@VolunteerHomeActivity)
            adapter = this@VolunteerHomeActivity.adapter
        }

        completedAdapter = MyOrdersAdapter { demand ->
            Toast.makeText(this, "已完成：${demand.location} — ${demand.durationMinutes}分钟", Toast.LENGTH_SHORT).show()
        }

        findViewById<RecyclerView>(R.id.rv_completed).apply {
            layoutManager = LinearLayoutManager(this@VolunteerHomeActivity)
            adapter = this@VolunteerHomeActivity.completedAdapter
        }

        loadMyOrders()
    }

    override fun onResume() {
        super.onResume()
        loadMyOrders()
    }

    private fun loadMyOrders() {
        lifecycleScope.launch {
            try {
                val phone = AppPrefs.currentUserPhone
                val all = app.database.runningRecordDao().getAllRecordsRaw()

                // Accepted orders
                val accepted = all.filter { it.volunteerPhone == phone && it.status == "accepted" }
                val mapped = accepted.map { RunningRecord(it.id, it.date, it.durationMinutes, it.location, it.distanceKm, it.status) }
                myOrders.clear()
                myOrders.addAll(mapped)
                adapter.submitList(mapped)

                // Completed runs
                val completed = all.filter { it.volunteerPhone == phone && it.status == "completed" }
                val completedMapped = completed.map { RunningRecord(it.id, it.date, it.durationMinutes, it.location, it.distanceKm, it.status) }
                completedRuns.clear()
                completedRuns.addAll(completedMapped)
                completedAdapter.submitList(completedMapped)

                // Stats
                val totalCompleted = completed.size
                val totalMinutes = completed.sumOf { it.durationMinutes }
                val totalDistance = completed.sumOf { it.distanceKm.toDouble() }
                findViewById<TextView>(R.id.tv_volunteer_stats).text =
                    "累计完成 $totalCompleted 次 · ${totalMinutes}分钟 · ${"%.1f".format(totalDistance)}km"

                findViewById<RecyclerView>(R.id.rv_my_orders).visibility =
                    if (myOrders.isEmpty()) View.GONE else View.VISIBLE
                findViewById<TextView>(R.id.tv_completed_title).visibility =
                    if (completedRuns.isEmpty()) View.GONE else View.VISIBLE
                findViewById<RecyclerView>(R.id.rv_completed).visibility =
                    if (completedRuns.isEmpty()) View.GONE else View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@VolunteerHomeActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class MyOrdersAdapter(
    private val onClick: (RunningRecord) -> Unit
) : ListAdapter<RunningRecord, MyOrdersAdapter.VH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RunningRecord>() {
        override fun areItemsTheSame(old: RunningRecord, new: RunningRecord) = old.id == new.id
        override fun areContentsTheSame(old: RunningRecord, new: RunningRecord) = old == new
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view.findViewById(R.id.tv_order_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tv.text = "${item.date}  ${item.location}  ${item.durationMinutes}分钟"
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
