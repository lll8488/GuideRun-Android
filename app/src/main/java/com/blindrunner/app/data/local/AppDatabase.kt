package com.blindrunner.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blindrunner.app.data.local.dao.RunningRecordDao
import com.blindrunner.app.data.local.dao.UserDao
import com.blindrunner.app.data.local.entity.RunningRecordEntity
import com.blindrunner.app.data.local.entity.UserEntity

@Database(
    entities = [RunningRecordEntity::class, UserEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun runningRecordDao(): RunningRecordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // PRD: 所有数据库迁移必须保留用户数据，严禁 fallbackToDestructiveMigration
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running_records ADD COLUMN ownerPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running_records ADD COLUMN volunteerPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE running_records ADD COLUMN volunteerNote TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running_records ADD COLUMN blindConfirmed INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running_records ADD COLUMN demandId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE running_records ADD COLUMN lat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE running_records ADD COLUMN lng REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE running_records ADD COLUMN trackJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN examPassed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN examScore INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blind_runner_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build().also { INSTANCE = it }
            }
        }
    }
}
