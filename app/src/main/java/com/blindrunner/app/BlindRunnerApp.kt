package com.blindrunner.app

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.blindrunner.app.data.local.AppDatabase
import com.blindrunner.app.data.remote.RetrofitClient
import com.blindrunner.app.data.repository.RunningRepository
import com.blindrunner.app.util.AppPrefs
import com.blindrunner.app.util.NotificationHelper
import com.blindrunner.app.util.TtsHelper

class BlindRunnerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: RunningRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AppPrefs.init(this)
        TtsHelper.init(this)
        NotificationHelper.init(this)

        // AMap SDK 隐私合规 — 必须在任何定位接口调用前执行
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        database = AppDatabase.getInstance(this)
        repository = RunningRepository(
            runningRecordDao = database.runningRecordDao(),
            apiService = RetrofitClient.getApiService()
        )

        // Pre-seed existing account 17825322628
        val prefs = getSharedPreferences("user_17825322628", MODE_PRIVATE)
        if (prefs.getString("password", null) == null) {
            prefs.edit()
                .putString("password", "123456")
                .putBoolean("first_login", false)
                .putString("user_type", "blind")
                .apply()
        }
    }

    override fun onTerminate() {
        TtsHelper.shutdown()
        super.onTerminate()
    }
}
