package com.blindrunner.app.ui.blind

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.blindrunner.app.R
import com.blindrunner.app.base.BaseActivity
import com.blindrunner.app.util.AppPrefs

class EmergencyContactActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contact)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        // Load existing contacts
        val contacts = AppPrefs.getEmergencyContacts()
        findViewById<EditText>(R.id.et_contact1_name).setText(contacts.getOrElse(0) { Pair("", "") }.first)
        findViewById<EditText>(R.id.et_contact1_phone).setText(contacts.getOrElse(0) { Pair("", "") }.second)
        findViewById<EditText>(R.id.et_contact2_name).setText(contacts.getOrElse(1) { Pair("", "") }.first)
        findViewById<EditText>(R.id.et_contact2_phone).setText(contacts.getOrElse(1) { Pair("", "") }.second)
        findViewById<EditText>(R.id.et_contact3_name).setText(contacts.getOrElse(2) { Pair("", "") }.first)
        findViewById<EditText>(R.id.et_contact3_phone).setText(contacts.getOrElse(2) { Pair("", "") }.second)

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val list = listOf(
                findViewById<EditText>(R.id.et_contact1_name).text.toString() to
                findViewById<EditText>(R.id.et_contact1_phone).text.toString(),
                findViewById<EditText>(R.id.et_contact2_name).text.toString() to
                findViewById<EditText>(R.id.et_contact2_phone).text.toString(),
                findViewById<EditText>(R.id.et_contact3_name).text.toString() to
                findViewById<EditText>(R.id.et_contact3_phone).text.toString()
            ).filter { it.second.isNotEmpty() }

            if (list.isEmpty()) {
                showToast("请至少填写一位紧急联系人电话")
                return@setOnClickListener
            }
            AppPrefs.saveEmergencyContacts(list)
            showToast("已保存 ${list.size} 位紧急联系人")
            finish()
        }
    }
}
