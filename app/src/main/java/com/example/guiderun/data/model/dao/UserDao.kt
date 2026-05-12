package com.example.guiderun.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.guiderun.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // 插入用户
    @Insert
    suspend fun insertUser(user: User): Long // 返回生成的userId

    // 更新用户信息
    @Update
    suspend fun updateUser(user: User)

    // 删除用户
    @Delete
    suspend fun deleteUser(user: User)

    // 根据手机号查询用户（登录用）
    @Query("SELECT * FROM user WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    // 根据userId查询用户
    @Query("SELECT * FROM user WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    // 查询所有志愿者（用于测试）
    @Query("SELECT * FROM user WHERE identity = 1")
    fun getAllVolunteers(): Flow<List<User>>
}