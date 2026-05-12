package com.example.guiderun.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val phone: String, // 手机号，唯一
    val identity: Int, // 0=视障用户，1=陪跑志愿者
    val nickname: String = "",
    val avatar: String? = null,
    val emergencyContact1: String? = null,
    val emergencyPhone1: String? = null,
    val emergencyContact2: String? = null,
    val emergencyPhone2: String? = null,
    val emergencyContact3: String? = null,
    val emergencyPhone3: String? = null,
    val examPassed: Boolean = false, // 志愿者是否通过考核
    val examScore: Int = 0, // 考核分数
    val examTime: Long? = null // 考核通过时间戳
)