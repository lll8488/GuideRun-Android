package com.blindrunner.app

import android.app.Application
import com.blindrunner.app.data.local.AppDatabase
import com.blindrunner.app.data.remote.RetrofitClient
import com.blindrunner.app.data.repository.RunningRepository

class BlindRunnerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: RunningRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = RunningRepository(
            runningRecordDao = database.runningRecordDao(),
            apiService = RetrofitClient.apiService
        )
    }
}
