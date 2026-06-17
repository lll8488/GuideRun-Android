package com.blindrunner.app.base

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blindrunner.app.R
import com.blindrunner.app.util.ThemeHelper
import com.blindrunner.app.util.TtsHelper

open class BaseActivity : AppCompatActivity() {

    private var progressBar: ProgressBar? = null
    private var emptyView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
    }

    // ====== 通用 UI ======

    fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    fun showLongToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    fun showLoading() { progressBar?.visibility = View.VISIBLE }
    fun hideLoading() { progressBar?.visibility = View.GONE }

    fun showEmpty(text: String = "暂无数据") {
        emptyView?.text = text
        emptyView?.visibility = View.VISIBLE
    }
    fun hideEmpty() { emptyView?.visibility = View.GONE }

    fun bindProgressBar(id: Int) { progressBar = findViewById(id) }
    fun bindEmptyView(id: Int) { emptyView = findViewById(id) }

    fun handleError(e: Exception? = null, userMsg: String = "操作失败，请重试") {
        hideLoading()
        val msg = if (e != null) "$userMsg: ${e.message}" else userMsg
        showToast(msg)
        TtsHelper.speak(msg, true)
    }

    // ====== 导航 ======

    /** 统一返回按钮：find + click → finish */
    fun setupBackButton(id: Int = R.id.btn_back) {
        findViewById<Button>(id)?.setOnClickListener { finish() }
    }

    /** 带语音导航的页面跳转 */
    fun navigateTo(cls: Class<*>, pageName: String = "", extras: Bundle? = null) {
        if (pageName.isNotEmpty()) TtsHelper.speakNavigation(pageName)
        val intent = Intent(this, cls)
        if (extras != null) intent.putExtras(extras)
        startActivity(intent)
    }

    /** 带语音导航 + finish 的页面跳转 */
    fun navigateAndFinish(cls: Class<*>, pageName: String = "", extras: Bundle? = null) {
        navigateTo(cls, pageName, extras)
        finish()
    }

    // ====== 弹窗 ======

    /** 统一确认弹窗（无障碍适配） */
    fun showConfirmDialog(
        title: String,
        message: String,
        positiveText: String = "确定",
        negativeText: String = "取消",
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        TtsHelper.speak("$title。$message", true)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> onConfirm() }
            .setNegativeButton(negativeText) { _, _ -> onCancel?.invoke() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 子类不需要额外清理 Handler/Callback
    }
}
