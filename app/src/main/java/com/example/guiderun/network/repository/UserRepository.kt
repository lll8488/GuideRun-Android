package com.example.guiderun.repository

import com.example.guiderun.data.dao.UserDao
import com.example.guiderun.data.model.User
import com.example.guiderun.network.ApiService
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService
) {
    // 登录：先查本地，本地没有则从网络获取
    suspend fun login(phone: String): User? {
        var user = userDao.getUserByPhone(phone)
        if (user == null) {
            try {
                // 模拟从网络获取用户信息（用ID=1作为测试）
                val response = apiService.getUserById(1)
                if (response.isSuccessful && response.body() != null) {
                    user = response.body()!!
                    userDao.insertUser(user)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return user
    }

    // 注册：保存到本地
    suspend fun register(user: User): Long {
        return userDao.insertUser(user)
    }

    // 获取所有志愿者
    fun getAllVolunteers(): Flow<List<User>> {
        return userDao.getAllVolunteers()
    }
}