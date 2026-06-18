package com.blindrunner.app.ui.volunteer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs

class TrainingCampActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_camp)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // Bind exam status
        val tvStatus = findViewById<TextView>(R.id.tv_exam_status)
        if (AppPrefs.examPassed) {
            tvStatus.text = "考核状态：已通过（${AppPrefs.examScore} 分）"
            tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
            tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else {
            tvStatus.text = "考核状态：未通过，请完成所有课程后参加考核"
        }

        listOf(
            R.id.btn_course1 to "第1节：陪跑绳的标准长度与正确握持方式",
            R.id.btn_course2 to "第2节：方向引导的力度控制与标准化口令",
            R.id.btn_course3 to "第3节：转弯、避让障碍、启停的标准流程",
            R.id.btn_course4 to "第4节：突发情况应急处理与沟通注意事项"
        ).forEach { (id, title) ->
            findViewById<Button>(id).setOnClickListener {
                startActivity(Intent(this, CourseDetailActivity::class.java).apply {
                    putExtra("title", title)
                })
            }
        }

        findViewById<Button>(R.id.btn_exam).setOnClickListener {
            startActivity(Intent(this, ExamActivity::class.java))
        }
    }
}
