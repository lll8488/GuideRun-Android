package com.example.guiderun

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guiderun.data.dao.UserDao
import com.example.guiderun.data.db.AppDatabase
import com.example.guiderun.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setupDatabase() {
        // 使用内存数据库，测试结束后自动销毁
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    // 测试方法1：插入用户并查询
    @Test
    fun insertAndGetUser() = runTest {
        // 创建测试用户
        val testUser = User(
            phone = "13800138000",
            identity = 0, // 视障用户
            nickname = "测试视障用户"
        )

        // 插入用户，获取生成的ID
        val userId = userDao.insertUser(testUser)
        assertNotEquals(0, userId) // 插入成功返回非0ID

        // 根据ID查询用户
        val retrievedUser = userDao.getUserById(userId)
        assertNotNull(retrievedUser)
        assertEquals(testUser.phone, retrievedUser?.phone)
        assertEquals(testUser.nickname, retrievedUser?.nickname)
    }

    // 测试方法2：更新用户考核状态
    @Test
    fun updateUserExamStatus() = runTest {
        // 插入志愿者用户
        val volunteer = User(
            phone = "13900139000",
            identity = 1, // 志愿者
            examPassed = false,
            examScore = 0
        )
        val userId = userDao.insertUser(volunteer)

        // 更新考核状态
        val updatedVolunteer = volunteer.copy(
            userId = userId,
            examPassed = true,
            examScore = 90,
            examTime = System.currentTimeMillis()
        )
        userDao.updateUser(updatedVolunteer)

        // 查询验证
        val retrievedUser = userDao.getUserById(userId)
        assertTrue(retrievedUser?.examPassed == true)
        assertEquals(90, retrievedUser?.examScore)
    }

    // 测试方法3：查询所有志愿者
    @Test
    fun getAllVolunteers() = runTest {
        // 插入2个志愿者和1个视障用户
        userDao.insertUser(User(phone = "13900000001", identity = 1))
        userDao.insertUser(User(phone = "13900000002", identity = 1))
        userDao.insertUser(User(phone = "13800000001", identity = 0))

        // 查询所有志愿者
        val volunteers = userDao.getAllVolunteers().first()
        assertEquals(2, volunteers.size)
    }
}
