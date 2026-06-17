package com.blindrunner.app.ui.blind

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.domain.model.RunningRecord
import com.blindrunner.app.util.AppPrefs
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private lateinit var adapter: HistoryAdapter
    private var startDate = "2026-01-01"
    private var endDate = "2026-12-31"
    private val currentRecords = mutableListOf<RunningRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        adapter = HistoryAdapter(
            onClick = { r ->
                startActivity(Intent(this, HistoryDetailActivity::class.java).apply {
                    putExtra("record_id", r.id)
                })
            },
            onLongClick = { r ->
                AlertDialog.Builder(this)
                    .setTitle("确认删除").setMessage("确定要删除这条跑步记录吗？")
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch { app.database.runningRecordDao().deleteById(r.id); loadRecords() }
                    }
                    .setNegativeButton("取消", null).show()
            }
        )

        findViewById<RecyclerView>(R.id.rv_history).apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            this.adapter = this@HistoryActivity.adapter
        }

        findViewById<SwipeRefreshLayout>(R.id.swipe_refresh).setOnRefreshListener { loadRecords() }

        findViewById<Button>(R.id.btn_start_date).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                startDate = "%d-%02d-%02d".format(y, m + 1, d)
                findViewById<Button>(R.id.btn_start_date).text = startDate
                loadRecords()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        findViewById<Button>(R.id.btn_end_date).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                endDate = "%d-%02d-%02d".format(y, m + 1, d)
                findViewById<Button>(R.id.btn_end_date).text = endDate
                loadRecords()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // PRD 4.5: 跳转统计页面（GraphView折线图）
        findViewById<Button>(R.id.btn_statistics).setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        loadRecords()
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val swipe = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
            swipe.isRefreshing = true
            try {
                val phone = AppPrefs.currentUserPhone
                val all = app.database.runningRecordDao().getRecordsByOwnerPhone(phone)
                val filtered = all.filter { it.date >= startDate && it.date <= endDate }
                    .map { RunningRecord(it.id, it.date, it.durationMinutes, it.location, it.distanceKm, it.status) }
                currentRecords.clear(); currentRecords.addAll(filtered)
                adapter.submitList(filtered)
                findViewById<TextView>(R.id.tv_total_count).text = "${filtered.size}"
                findViewById<TextView>(R.id.tv_total_duration).text = "${filtered.sumOf { it.durationMinutes }}分钟"
                findViewById<TextView>(R.id.tv_empty_history).visibility =
                    if (filtered.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@HistoryActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipe.isRefreshing = false
            }
        }
    }
}

class HistoryAdapter(
    private val onClick: (RunningRecord) -> Unit,
    private val onLongClick: (RunningRecord) -> Unit
) : ListAdapter<RunningRecord, HistoryAdapter.VH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RunningRecord>() {
        override fun areItemsTheSame(old: RunningRecord, new: RunningRecord) = old.id == new.id
        override fun areContentsTheSame(old: RunningRecord, new: RunningRecord) = old == new
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_item_date)
        val tvLocation: TextView = view.findViewById(R.id.tv_item_location)
        val tvDuration: TextView = view.findViewById(R.id.tv_item_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvDate.text = item.date
        holder.tvLocation.text = item.location
        holder.tvDuration.text = "${item.durationMinutes}分钟"
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }
}
