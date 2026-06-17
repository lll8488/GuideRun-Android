package com.blindrunner.app.ui.volunteer

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.util.TtsHelper

class CourseDetailActivity : AppCompatActivity() {

    // PRD 4.2: 4节陪跑绳规范课程具体内容
    private val courseContents = mapOf(
        "第1节：陪跑绳的标准长度与正确握持方式" to """
【第1节 课程正文】

一、陪跑绳标准规格
陪跑绳的标准长度为1米，两端各有一个握环。标准长度确保了志愿者和视障跑者之间保持安全有效的物理连接。

二、正确握持方式
1. 志愿者握持端：使用优势手握住握环，保持手臂自然弯曲约90度
2. 视障跑者握持端：使用任意手握住握环，手臂自然下垂
3. 关键要点：绳子应保持适度张力，既不能太松（失去传递信号功能），也不能太紧（影响跑姿）

三、安全注意事项
• 开机前检查陪跑绳是否有磨损
• 握环内径应大于3cm，确保紧急情况下可快速脱手
• 陪跑绳应具有反光材质，提高夜间可见性

四、常见错误
✗ 将绳子缠绕在手腕上（危险！）
✗ 单手同时握住两端握环
✗ 使用非标准绳索替代陪跑绳
        """.trimIndent(),

        "第2节：方向引导的力度控制与标准化口令" to """
【第2节 课程正文】

一、力度控制原则
陪跑绳引导的核心是通过绳子的张力变化传递方向信息：
• 轻度拉力（约0.5kg）：提示前方缓慢转弯
• 中度拉力（约1kg）：提示前方90度转弯
• 力度保持均匀，避免突然用力

二、标准化口令体系
• 起步口令：志愿者说"3、2、1，开始"，然后同步迈步
• 转弯口令："前方左转/右转，请准备"，同时配合陪跑绳拉力
• 减速口令："前方减速"，同时放慢速度并收短绳子
• 停止口令："停止"，同时站定并给绳子一个明确停止信号
• 危险口令："停！"，立即停止并引导视障跑者移向安全区域

三、配合技巧
• 口令应在行动前2-3秒发出，给视障跑者反应时间
• 口令与陪跑绳信号应同步，避免信息不一致
• 重要口令应重复一次确认
        """.trimIndent(),

        "第3节：转弯、避让障碍、启停的标准流程" to """
【第3节 课程正文】

一、转弯标准流程
1. 预判：距转弯点约10米时开始准备
2. 口令："前方左转/右转，请准备"
3. 信号：通过陪跑绳传递方向拉力
4. 执行：保持双方同步速度完成转弯
5. 确认："转弯完成"，恢复正常跑步

二、避让障碍标准流程
1. 发现障碍：志愿者提前5米识别障碍物
2. 预警口令："前方有障碍，请注意"
3. 引导偏移：通过陪跑绳拉力引导侧向偏移
4. 通过确认："已通过"，恢复正常路线

三、启停标准流程
• 起步三步骤：口令倒计时 → 同步发力 → 正常跑步
• 停止三步骤：减速口令 → 收短绳子 → 安全站定
• 紧急停止："停！" → 志愿者立即站定 → 引导跑者靠边
        """.trimIndent(),

        "第4节：突发情况应急处理与沟通注意事项" to """
【第4节 课程正文】

一、常见突发情况处理
1. 视障跑者身体不适
   → 立即喊"停止"口令并站定
   → 引导跑者移向安全区域坐下
   → 询问情况，必要时拨打120

2. 陪跑绳意外脱落
   → 立即喊"停止"口令
   → 志愿者快速靠近跑者
   → 重新连接陪跑绳，确认牢固后继续

3. 路面突发危险（坑洞、障碍物等）
   → 紧急口令"停！"
   → 用身体阻挡跑者前进
   → 引导绕行或选择替代路线

4. 天气突变（暴雨、雷电等）
   → 立即停止跑步
   → 寻找就近避雨/避雷场所
   → 通知紧急联系人

二、沟通注意事项
• 始终保持平和、清晰的语调
• 使用具体的方向描述（"向前2米"而非"前面一点"）
• 描述环境信息时使用方位词（以跑者面向为前方）
• 定期询问跑者状态（建议每5分钟一次）
• 尊重跑者的独立性，不做过多的非必要干预
        """.trimIndent()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        val title = intent.getStringExtra("title") ?: "课程详情"
        val content = courseContents[title] ?: "课程内容加载中..."

        findViewById<TextView>(R.id.tv_title).text = title
        findViewById<TextView>(R.id.tv_content).text = content

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_play).setOnClickListener {
            // PRD 4.2: 全量语音播报 — 点击"语音播报"按钮朗读全部课程内容
            TtsHelper.speak(content, true)
        }
    }
}
