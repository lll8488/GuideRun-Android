package com.blindrunner.app.ui.volunteer

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DemandListActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp
    private val allEntities = mutableListOf<RunningRecordEntity>()
    private val demands = mutableListOf<RunningRecordEntity>()
    private var distanceMap = mutableMapOf<Long, Float>()
    private lateinit var adapter: DemandAdapter
    private var currentFilter = "全部"
    private var volunteerLat = 0.0
    private var volunteerLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demand_list)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val tvExamStatus = findViewById<TextView>(R.id.tv_exam_status)
        if (!AppPrefs.examPassed) {
            tvExamStatus.visibility = View.VISIBLE
            tvExamStatus.text = "请先通过陪跑绳规范考核，否则无法接单"
        }

        adapter = DemandAdapter(distanceMap) { demand ->
            startActivity(Intent(this, DemandDetailActivity::class.java).apply {
                putExtra("id", demand.id)
                putExtra("location", demand.location)
                putExtra("date", demand.date)
                putExtra("duration", demand.durationMinutes.toString())
                putExtra("distance", demand.distanceKm.toString())
                putExtra("status", demand.status)
            })
        }

        findViewById<RecyclerView>(R.id.rv_demands).apply {
            layoutManager = LinearLayoutManager(this@DemandListActivity)
            adapter = this@DemandListActivity.adapter
        }

        findViewById<EditText>(R.id.et_search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val tags = mapOf(
            R.id.tag_all to "全部",
            R.id.tag_30min to "1-3km",
            R.id.tag_60min to "3-5km",
            R.id.tag_90min to "5-10km",
            R.id.tag_120min to "10km+"
        )
        val tagIds = listOf(R.id.tag_all, R.id.tag_30min, R.id.tag_60min, R.id.tag_90min, R.id.tag_120min)
        tags.forEach { (id, label) ->
            findViewById<TextView>(id).setOnClickListener {
                currentFilter = label
                tagIds.forEach { tid ->
                    findViewById<TextView>(tid).apply {
                        setBackgroundResource(if (tid == id) R.drawable.tag_selected else R.drawable.tag_normal)
                        setTextColor(if (tid == id) 0xFFFFFFFF.toInt() else 0xFF888888.toInt())
                    }
                }
                applyFilters()
            }
        }

        // Distance sort toggle
        findViewById<TextView>(R.id.tag_distance).setOnClickListener {
            currentFilter = "按距离"
            findViewById<TextView>(R.id.tag_distance).apply {
                setBackgroundResource(R.drawable.tag_selected)
                setTextColor(0xFFFFFFFF.toInt())
            }
            tagIds.forEach { tid ->
                findViewById<TextView>(tid).apply {
                    setBackgroundResource(R.drawable.tag_normal)
                    setTextColor(0xFF888888.toInt())
                }
            }
            applyFilters()
        }

        getVolunteerLocationAndLoad()
    }

    private fun getVolunteerLocationAndLoad() {
        AMapLocationClient.setApiKey(com.blindrunner.app.util.MapConfig.getAmapKey(applicationContext))
        val locClient = AMapLocationClient(applicationContext)
        val option = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            httpTimeOut = 8000
        }
        locClient.setLocationOption(option)
        locClient.setLocationListener { loc ->
            if (loc != null && loc.errorCode == 0) {
                volunteerLat = loc.latitude
                volunteerLng = loc.longitude
            }
            loadDemands()
            locClient.onDestroy()
        }
        locClient.startLocation()
    }

    private fun loadDemands() {
        lifecycleScope.launch {
            try {
                val entities = withContext(Dispatchers.IO) {
                    app.database.runningRecordDao().getAllRecordsRaw()
                }
                allEntities.clear()
                allEntities.addAll(entities.filter { it.status == "pending" })

                // Calculate distances
                if (volunteerLat != 0.0 && volunteerLng != 0.0) {
                    val distResults = FloatArray(1)
                    distanceMap.clear()
                    for (e in allEntities) {
                        if (e.lat != 0.0 && e.lng != 0.0) {
                            Location.distanceBetween(
                                volunteerLat, volunteerLng, e.lat, e.lng, distResults
                            )
                            distanceMap[e.id] = distResults[0]
                        }
                    }
                }
                applyFilters()
            } catch (e: Exception) {
                Toast.makeText(this@DemandListActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        val searchEt = findViewById<EditText>(R.id.et_search)
        val searchText = searchEt?.text?.toString()?.trim() ?: ""
        demands.clear()

        val filtered = allEntities.filter { entity ->
            val matchSearch = searchText.isEmpty() || entity.location.contains(searchText, ignoreCase = true)
            val matchDistance = when (currentFilter) {
                "1-3km" -> entity.distanceKm >= 1f && entity.distanceKm <= 3f
                "3-5km" -> entity.distanceKm >= 3f && entity.distanceKm <= 5f
                "5-10km" -> entity.distanceKm >= 5f && entity.distanceKm <= 10f
                "10km+" -> entity.distanceKm >= 10f
                else -> true // "全部" or "按距离"
            }
            matchSearch && matchDistance
        }

        if (currentFilter == "按距离") {
            demands.addAll(filtered.sortedBy { distanceMap[it.id] ?: Float.MAX_VALUE })
        } else {
            demands.addAll(filtered)
        }

        adapter.updateDistanceMap(distanceMap)
        adapter.submitList(filtered.toList())

        val emptyView = findViewById<TextView>(R.id.tv_empty_demands)
        val rv = findViewById<RecyclerView>(R.id.rv_demands)
        if (demands.isEmpty()) {
            rv.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }
}

class DemandAdapter(
    private var distanceMap: Map<Long, Float>,
    private val onClick: (RunningRecordEntity) -> Unit
) : ListAdapter<RunningRecordEntity, DemandAdapter.VH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<RunningRecordEntity>() {
        override fun areItemsTheSame(old: RunningRecordEntity, new: RunningRecordEntity) = old.id == new.id
        override fun areContentsTheSame(old: RunningRecordEntity, new: RunningRecordEntity) = old == new
    }

    fun updateDistanceMap(map: Map<Long, Float>) {
        distanceMap = map
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvLocation: TextView = view.findViewById(R.id.tv_location)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvDuration: TextView = view.findViewById(R.id.tv_duration)
        val tvDistance: TextView = view.findViewById(R.id.tv_item_distance)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_demand, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvLocation.text = "📍 ${item.location}"
        holder.tvDate.text = "📅 ${item.date}"
        holder.tvDuration.text = "⏱ ${item.durationMinutes}分钟"
        val dist = distanceMap[item.id]
        holder.tvDistance.text = if (dist != null && dist >= 0f) {
            if (dist >= 1000f) "📏 %.1f km".format(dist / 1000f) else "📏 %d m".format(dist.toInt())
        } else ""
        holder.tvStatus.text = "待接单"
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
