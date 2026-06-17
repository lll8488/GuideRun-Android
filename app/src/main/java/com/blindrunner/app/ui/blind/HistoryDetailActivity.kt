package com.blindrunner.app.ui.blind

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.PolylineOptions
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class HistoryDetailActivity : AppCompatActivity() {

    private val app get() = application as BlindRunnerApp
    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var recordId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail)

        recordId = intent.getLongExtra("record_id", 0L)

        mapView = findViewById(R.id.map_track_detail)
        mapView.onCreate(savedInstanceState)

        AMapLocationClient.setApiKey(com.blindrunner.app.util.MapConfig.getAmapKey(this))
        aMap = mapView.map

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        loadRecord()
    }

    private fun loadRecord() {
        lifecycleScope.launch {
            try {
                val entity = withContext(Dispatchers.IO) {
                    app.database.runningRecordDao().getRecordById(recordId)
                }
                if (entity == null) {
                    Toast.makeText(this@HistoryDetailActivity, "记录不存在", Toast.LENGTH_SHORT).show()
                    finish(); return@launch
                }

                findViewById<TextView>(R.id.tv_detail_date).text = entity.date
                findViewById<TextView>(R.id.tv_detail_duration).text = "${entity.durationMinutes} 分钟"
                findViewById<TextView>(R.id.tv_detail_distance).text = "%.2f km".format(entity.distanceKm)
                findViewById<TextView>(R.id.tv_detail_location).text = entity.location

                // Draw track on map
                if (entity.trackJson.isNotEmpty()) {
                    drawTrack(entity.trackJson)
                } else {
                    findViewById<TextView>(R.id.tv_detail_note).text = "本次跑步未记录轨迹"
                }
            } catch (e: Exception) {
                Toast.makeText(this@HistoryDetailActivity, "加载失败", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun drawTrack(trackJson: String) {
        try {
            val arr = JSONArray(trackJson)
            if (arr.length() < 2) return
            val points = mutableListOf<LatLng>()
            for (i in 0 until arr.length()) {
                val pt = arr.getJSONArray(i)
                points.add(LatLng(pt.getDouble(1), pt.getDouble(0)))
            }

            aMap?.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .width(14f)
                    .color(Color.argb(200, 27, 94, 32))
            )

            // Fit map to show entire track
            val builder = LatLngBounds.builder()
            for (p in points) builder.include(p)
            aMap?.animateCamera(
                CameraUpdateFactory.newLatLngBounds(builder.build(), 80)
            )
        } catch (e: Exception) {
            android.util.Log.e("HistoryDetail", "drawTrack failed", e)
        }
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
}
