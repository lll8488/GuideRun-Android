package com.example.guiderun.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.guiderun.data.dao.DemandDao
import com.example.guiderun.data.dao.RunningRecordDao
import com.example.guiderun.data.dao.UserDao
import com.example.guiderun.data.model.Demand
import com.example.guiderun.data.model.RunningRecord
import com.example.guiderun.data.model.User

@Database(
    entities = [User::class, Demand::class, RunningRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // 提供Dao实例
    abstract fun userDao(): UserDao
    abstract fun demandDao(): DemandDao
    abstract fun runningRecordDao(): RunningRecordDao

    companion object {
        // 单例实例（volatile保证多线程可见性）
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guiderun_room.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}