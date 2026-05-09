package com.example.guiderun.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "guiderun.db"
        private const val DATABASE_VERSION = 1

        // 用户表
        private const val CREATE_TABLE_USER = """
            CREATE TABLE user (
                userId INTEGER PRIMARY KEY AUTOINCREMENT,
                phone TEXT UNIQUE NOT NULL,
                identity INTEGER NOT NULL,
                nickname TEXT,
                avatar TEXT,
                emergencyContact1 TEXT,
                emergencyPhone1 TEXT,
                emergencyContact2 TEXT,
                emergencyPhone2 TEXT,
                emergencyContact3 TEXT,
                emergencyPhone3 TEXT,
                examPassed INTEGER NOT NULL DEFAULT 0,
                examScore INTEGER NOT NULL DEFAULT 0,
                examTime INTEGER
            )
        """

        // 需求表
        private const val CREATE_TABLE_DEMAND = """
            CREATE TABLE demand (
                demandId INTEGER PRIMARY KEY AUTOINCREMENT,
                publisherId INTEGER NOT NULL,
                runnerId INTEGER,
                runDate TEXT NOT NULL,
                runTime TEXT NOT NULL,
                location TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                duration INTEGER NOT NULL,
                remark TEXT,
                status INTEGER NOT NULL DEFAULT 0,
                createTime INTEGER NOT NULL,
                updateTime INTEGER NOT NULL,
                FOREIGN KEY (publisherId) REFERENCES user(userId),
                FOREIGN KEY (runnerId) REFERENCES user(userId)
            )
        """

        // 跑步记录表
        private const val CREATE_TABLE_RECORD = """
            CREATE TABLE running_record (
                recordId INTEGER PRIMARY KEY AUTOINCREMENT,
                demandId INTEGER NOT NULL UNIQUE,
                userId INTEGER NOT NULL,
                runnerId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER NOT NULL,
                actualDuration INTEGER NOT NULL,
                location TEXT NOT NULL,
                FOREIGN KEY (demandId) REFERENCES demand(demandId),
                FOREIGN KEY (userId) REFERENCES user(userId),
                FOREIGN KEY (runnerId) REFERENCES user(userId)
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_USER)
        db.execSQL(CREATE_TABLE_DEMAND)
        db.execSQL(CREATE_TABLE_RECORD)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // MVP阶段暂不处理数据库升级
        db.execSQL("DROP TABLE IF EXISTS running_record")
        db.execSQL("DROP TABLE IF EXISTS demand")
        db.execSQL("DROP TABLE IF EXISTS user")
        onCreate(db)
    }
}