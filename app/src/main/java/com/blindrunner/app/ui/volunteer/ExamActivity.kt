package com.blindrunner.app.ui.volunteer

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.util.AppPrefs
import kotlinx.coroutines.launch

class ExamActivity : AppCompatActivity() {

    private val questions = listOf(
        "陪跑绳的标准长度约为1米，用于建立志愿者与视障跑者的物理连接。" to true,
        "陪跑绳越长越好，以便志愿者有足够的活动空间。" to false,
        "志愿者在引导视障跑者转弯时应通过陪跑绳传递方向信号。" to true,
        "陪跑时志愿者应在视障跑者前方至少5米引路。" to false,
        "陪跑绳引导模式下，志愿者和视障跑者通过约1米长的绳子连接。" to true,
        "志愿者发现前方障碍时应提前减速并通过陪跑绳传递预警信号。" to true,
        "启停时需要遵循标准流程，包括起步和停止的标准化口令。" to true,
        "陪跑过程中如果视障跑者感到不适，应立即停止并评估情况。" to true,
        "陪跑绳仅用于装饰，没有实际功能用途。" to false,
        "突发情况时，志愿者应保持冷静并及时与视障跑者沟通。" to true
    )

    private val answerGroups = mutableListOf<RadioGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.questions_container)
        questions.forEachIndexed { i, (text, _) ->
            addQuestionCard(container, i + 1, text)
        }

        findViewById<Button>(R.id.btn_submit).setOnClickListener { submitExam() }
    }

    private fun addQuestionCard(container: LinearLayout, number: Int, text: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            elevation = 2f
        }

        card.addView(TextView(this).apply {
            this.text = "第 $number 题（10分）"
            setTextColor(Color.parseColor("#1B5E20"))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        card.addView(TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#333333"))
            textSize = 15f
            setPadding(0, 12, 0, 12)
        })

        val rg = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            contentDescription = "第${number}题答案选择"
        }
        val rbTrue = RadioButton(this).apply {
            this.text = "正确"
            this.contentDescription = "第${number}题选项：正确"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val rbFalse = RadioButton(this).apply {
            this.text = "错误"
            this.contentDescription = "第${number}题选项：错误"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        rg.addView(rbTrue)
        rg.addView(rbFalse)
        card.addView(rg)
        answerGroups.add(rg)

        container.addView(card)
    }

    private fun submitExam() {
        var score = 0
        questions.forEachIndexed { i, (_, correctAnswer) ->
            val rg = answerGroups.getOrNull(i) ?: return@forEachIndexed
            val selected = when (rg.checkedRadioButtonId) {
                rg.getChildAt(0).id -> true
                rg.getChildAt(1).id -> false
                else -> null
            }
            if (selected == correctAnswer) score += 10
        }

        val pass = score >= 80

        // PRD 4.2: 考核结果永久存储 — SharedPreferences + SQLite
        AppPrefs.examPassed = pass
        AppPrefs.examScore = score

        val phone = AppPrefs.currentUserPhone
        if (phone.isNotEmpty()) {
            lifecycleScope.launch {
                try {
                    val app = application as BlindRunnerApp
                    // 先确保用户存在
                    val existing = app.database.userDao().getUserByPhone(phone)
                    if (existing != null) {
                        app.database.userDao().updateExamResult(phone, pass, score)
                    } else {
                        // 不存在则插入
                        app.database.userDao().insert(
                            com.blindrunner.app.data.local.entity.UserEntity(
                                phone = phone,
                                name = "",
                                userType = AppPrefs.userType,
                                examPassed = pass,
                                examScore = score
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Exam", "save to SQLite failed", e)
                }
            }
        }

        if (pass) {
            Toast.makeText(this, "恭喜！考核通过！分数：$score 分", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "未通过，分数：$score 分。请重新学习后再次考核", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
