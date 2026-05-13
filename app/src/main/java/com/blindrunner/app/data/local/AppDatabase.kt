package com.blindrunner.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.blindrunner.app.data.local.dao.RunningRecordDao
import com.blindrunner.app.data.local.dao.UserDao
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.data.local.entity.UserEntity

@Database(
    entities = [RunningRecordEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun runningRecordDao(): RunningRecordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blind_runner_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
