package com.blindrunner.app.ui.blind

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.TtsHelper
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.coroutines.launch

/**
 * PRD 4.5: 跑步数据统计页面
 * - 总次数、总时长、平均时长
 * - 使用GraphView绘制跑步时长折线图，数据点≤30个
 */
class StatisticsActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        TtsHelper.speak("跑步统计页面，显示您的跑步次数、时长和折线图", true)

        lifecycleScope.launch {
            try {
                val phone = AppPrefs.currentUserPhone
                val all = app.database.runningRecordDao().getRecordsByOwnerPhone(phone)
                val completed = all.filter { it.status == "completed" }
                    .sortedByDescending { it.date }
                    .take(30) // PRD: 数据点≤30个

                val totalCount = completed.size
                val totalMin = completed.sumOf { it.durationMinutes }
                val avgMin = if (totalCount > 0) totalMin / totalCount else 0
                val totalDist = completed.sumOf { it.distanceKm.toDouble() }

                findViewById<TextView>(R.id.tv_stat_count).text = "$totalCount 次"
                findViewById<TextView>(R.id.tv_stat_duration).text = "${totalMin} 分钟"
                findViewById<TextView>(R.id.tv_stat_avg).text = "${avgMin} 分钟"
                findViewById<TextView>(R.id.tv_stat_distance).text = "${"%.1f".format(totalDist)} km"

                // GraphView折线图
                if (completed.isNotEmpty()) {
                    val graph = findViewById<GraphView>(R.id.graph_duration)
                    val reversed = completed.reversed()
                    val dataPoints = reversed.mapIndexed { i, record ->
                        DataPoint(i.toDouble(), record.durationMinutes.toDouble())
                    }.toTypedArray()

                    val series = LineGraphSeries(dataPoints).apply {
                        color = Color.parseColor("#4CAF50")
                        thickness = 6
                        dataPointsRadius = 8f
                        setAnimated(true)
                    }

                    graph.apply {
                        addSeries(series)
                        viewport.apply {
                            isXAxisBoundsManual = true
                            setMinX(0.0)
                            setMaxX((completed.size - 1).coerceAtLeast(1).toDouble())
                            setMinY(0.0)
                            setMaxY((completed.maxOf { it.durationMinutes } * 1.2).toDouble())
                            isScrollable = true
                            isScalable = true
                        }
                        gridLabelRenderer.apply {
                            horizontalAxisTitle = "跑步次数"
                            verticalAxisTitle = "时长(分钟)"
                            setHorizontalAxisTitleTextSize(28f)
                            setVerticalAxisTitleTextSize(28f)
                            labelVerticalWidth = 100
                            contentDescription = "跑步时长折线图，共${completed.size}次记录" +
                                "，X轴表示跑步次数，Y轴表示时长分钟"
                        }
                        title = "跑步时长趋势"
                        titleTextSize = 32f
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@StatisticsActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
