package com.blindrunner.app.ui.auth

import android.os.Bundle
import android.widget.Button
import com.blindrunner.app.R
import com.blindrunner.app.base.BaseActivity

class PrivacyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)
        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }
    }
}
