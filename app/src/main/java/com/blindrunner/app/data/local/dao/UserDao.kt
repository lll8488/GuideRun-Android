package com.blindrunner.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.blindrunner.app.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE userType = :userType")
    suspend fun getUsersByType(userType: String): List<UserEntity>

    @Query("UPDATE users SET rating = :rating, ratingCount = :count WHERE phone = :phone")
    suspend fun updateRating(phone: String, rating: Float, count: Int)

    @Query("UPDATE users SET totalRuns = totalRuns + 1, totalDistanceKm = totalDistanceKm + :dist WHERE phone = :phone")
    suspend fun incrementRunStats(phone: String, dist: Float)

    @Query("UPDATE users SET examPassed = :passed, examScore = :score WHERE phone = :phone")
    suspend fun updateExamResult(phone: String, passed: Boolean, score: Int)

    @Query("SELECT * FROM users WHERE userType = 'volunteer' ORDER BY totalRuns DESC")
    suspend fun getVolunteerLeaderboard(): List<UserEntity>
}
