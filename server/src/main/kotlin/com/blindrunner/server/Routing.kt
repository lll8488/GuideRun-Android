package com.blindrunner.server

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.random.Random

@Serializable
data class LoginRequest(val phone: String, val code: String)

@Serializable
data class SendCodeRequest(val phone: String)

@Serializable
data class RegisterRequest(val phone: String, val password: String, val userType: String)

@Serializable
data class UserResponse(val phone: String, val name: String, val userType: String,
                         val rating: Float, val totalRuns: Int, val totalDistanceKm: Float,
                         val examPassed: Boolean, val examScore: Int)

@Serializable
data class DemandRequest(val ownerPhone: String, val date: String, val time: String,
                          val location: String, val durationMinutes: Int, val distanceKm: Float,
                          val lat: Double, val lng: Double)

@Serializable
data class AcceptRequest(val volunteerPhone: String, val note: String)

@Serializable
data class RunningRecordRequest(val ownerPhone: String, val date: String,
                                 val durationMinutes: Int, val location: String,
                                 val distanceKm: Float, val demandId: Long,
                                 val volunteerPhone: String, val trackJson: String)

@Serializable
data class ExamSubmitRequest(val phone: String, val score: Int)

fun Application.routing() {
    routing {
        // ====== 健康检查 ======
        get("/") {
            call.respondText("🏃 BlindRunner Server is running!", ContentType.Text.Plain)
        }

        // ====== 发送验证码 ======
        post("/auth/send-code") {
            val req = call.receive<SendCodeRequest>()
            val code = (100000..999999).random().toString()
            val expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5))

            val db = Database.conn
            db.prepareStatement("DELETE FROM verify_codes WHERE phone = ?").use {
                it.setString(1, req.phone)
                it.executeUpdate()
            }
            db.prepareStatement("INSERT INTO verify_codes (phone, code, expires_at) VALUES (?, ?, ?)").use {
                it.setString(1, req.phone)
                it.setString(2, code)
                it.setTimestamp(3, expiresAt)
                it.executeUpdate()
            }

            println("📱 Code for ${req.phone}: $code")
            call.respond(mapOf("success" to true, "message" to "验证码已发送", "code" to code))
        }

        // ====== 登录 ======
        post("/auth/login") {
            val req = call.receive<LoginRequest>()

            // 验证验证码
            val db = Database.conn
            val codeValid = db.prepareStatement(
                "SELECT * FROM verify_codes WHERE phone = ? AND code = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP"
            ).use { ps ->
                ps.setString(1, req.phone)
                ps.setString(2, req.code)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    // 标记已使用
                    db.prepareStatement("UPDATE verify_codes SET used = TRUE WHERE id = ?").use {
                        it.setLong(1, rs.getLong("id"))
                        it.executeUpdate()
                    }
                    true
                } else false
            }

            if (!codeValid) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "message" to "验证码错误或已过期"))
                return@post
            }

            val user = db.prepareStatement("SELECT * FROM users WHERE phone = ?").use { ps ->
                ps.setString(1, req.phone)
                val rs = ps.executeQuery()
                if (rs.next()) rs.toUser() else null
            }

            if (user == null) {
                call.respond(mapOf("success" to true, "newUser" to true, "phone" to req.phone, "message" to "新用户，请选择身份"))
            } else {
                call.respond(mapOf("success" to true, "newUser" to false, "user" to user, "message" to "登录成功"))
            }
        }

        // ====== 注册（设置身份） ======
        post("/auth/register") {
            val req = call.receive<RegisterRequest>()
            val db = Database.conn

            val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())

            db.prepareStatement(
                "INSERT INTO users (phone, password, name, user_type) VALUES (?, ?, ?, ?)"
            ).use { ps ->
                ps.setString(1, req.phone)
                ps.setString(2, hash)
                ps.setString(3, if (req.userType == "blind") "视障用户" else "志愿者")
                ps.setString(4, req.userType)
                ps.executeUpdate()
            }

            val user = db.prepareStatement("SELECT * FROM users WHERE phone = ?").use { ps ->
                ps.setString(1, req.phone)
                rsToUser(ps.executeQuery())
            }

            call.respond(mapOf("success" to true, "user" to user, "message" to "注册成功"))
        }

        // ====== 获取用户信息 ======
        get("/user/{phone}") {
            val phone = call.parameters["phone"] ?: ""
            val db = Database.conn
            val user = db.prepareStatement("SELECT * FROM users WHERE phone = ?").use { ps ->
                ps.setString(1, phone)
                val rs = ps.executeQuery()
                if (rs.next()) rs.toUser() else null
            }
            if (user != null) call.respond(mapOf("success" to true, "user" to user))
            else call.respond(HttpStatusCode.NotFound, mapOf("success" to false, "message" to "用户不存在"))
        }

        // ====== 发布需求 ======
        post("/demands") {
            val req = call.receive<DemandRequest>()
            val db = Database.conn

            val id = db.prepareStatement(
                "INSERT INTO demands (owner_phone, date, time, location, duration_minutes, distance_km, lat, lng) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("id")
            ).use { ps ->
                ps.setString(1, req.ownerPhone)
                ps.setString(2, req.date)
                ps.setString(3, req.time)
                ps.setString(4, req.location)
                ps.setInt(5, req.durationMinutes)
                ps.setFloat(6, req.distanceKm)
                ps.setDouble(7, req.lat)
                ps.setDouble(8, req.lng)
                ps.executeUpdate()
                ps.generatedKeys.use { rs -> if (rs.next()) rs.getLong(1) else 0L }
            }

            call.respond(mapOf("success" to true, "id" to id, "message" to "需求发布成功"))
        }

        // ====== 获取待接单列表 ======
        get("/demands") {
            val db = Database.conn
            val demands = db.prepareStatement(
                "SELECT * FROM demands WHERE status = 'pending' ORDER BY created_at DESC"
            ).use { ps ->
                val rs = ps.executeQuery()
                val list = mutableListOf<Map<String, Any?>>()
                while (rs.next()) list.add(rs.toDemandMap())
                list
            }
            call.respond(mapOf("success" to true, "demands" to demands))
        }

        // ====== 接单 ======
        post("/demands/{id}/accept") {
            val demandId = call.parameters["id"]?.toLongOrNull() ?: return@post
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false))
            val req = call.receive<AcceptRequest>()
            val db = Database.conn

            db.prepareStatement(
                "UPDATE demands SET status = 'accepted', volunteer_phone = ?, volunteer_note = ? WHERE id = ? AND status = 'pending'"
            ).use { ps ->
                ps.setString(1, req.volunteerPhone)
                ps.setString(2, req.note)
                ps.setLong(3, demandId)
                val updated = ps.executeUpdate()
                if (updated > 0) {
                    val demand = db.prepareStatement("SELECT * FROM demands WHERE id = ?").use { s ->
                        s.setLong(1, demandId)
                        val rs = s.executeQuery()
                        if (rs.next()) rs.toDemandMap() else null
                    }
                    call.respond(mapOf("success" to true, "demand" to demand, "message" to "接单成功"))
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("success" to false, "message" to "该需求已被接单"))
                }
            }
        }

        // ====== 获取用户发布的需求 ======
        get("/demands/owner/{phone}") {
            val phone = call.parameters["phone"] ?: ""
            val db = Database.conn
            val demands = db.prepareStatement(
                "SELECT * FROM demands WHERE owner_phone = ? ORDER BY created_at DESC"
            ).use { ps ->
                ps.setString(1, phone)
                val rs = ps.executeQuery()
                val list = mutableListOf<Map<String, Any?>>()
                while (rs.next()) list.add(rs.toDemandMap())
                list
            }
            call.respond(mapOf("success" to true, "demands" to demands))
        }

        // ====== 保存跑步记录 ======
        post("/running/records") {
            val req = call.receive<RunningRecordRequest>()
            val db = Database.conn

            val id = db.prepareStatement(
                "INSERT INTO running_records (owner_phone, date, duration_minutes, location, distance_km, demand_id, volunteer_phone, track_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("id")
            ).use { ps ->
                ps.setString(1, req.ownerPhone)
                ps.setString(2, req.date)
                ps.setInt(3, req.durationMinutes)
                ps.setString(4, req.location)
                ps.setFloat(5, req.distanceKm)
                ps.setLong(6, req.demandId)
                ps.setString(7, req.volunteerPhone)
                ps.setString(8, req.trackJson)
                ps.executeUpdate()
                ps.generatedKeys.use { rs -> if (rs.next()) rs.getLong(1) else 0L }
            }

            // 更新用户统计
            db.prepareStatement("UPDATE users SET total_runs = total_runs + 1, total_distance_km = total_distance_km + ? WHERE phone = ?").use {
                it.setFloat(1, req.distanceKm)
                it.setString(2, req.ownerPhone)
                it.executeUpdate()
            }

            call.respond(mapOf("success" to true, "id" to id, "message" to "记录已保存"))
        }

        // ====== 获取用户的跑步记录 ======
        get("/running/records/{phone}") {
            val phone = call.parameters["phone"] ?: ""
            val db = Database.conn
            val records = db.prepareStatement(
                "SELECT * FROM running_records WHERE owner_phone = ? ORDER BY created_at DESC"
            ).use { ps ->
                ps.setString(1, phone)
                val rs = ps.executeQuery()
                val list = mutableListOf<Map<String, Any?>>()
                while (rs.next()) {
                    list.add(mapOf(
                        "id" to rs.getLong("id"),
                        "date" to rs.getString("date"),
                        "durationMinutes" to rs.getInt("duration_minutes"),
                        "location" to rs.getString("location"),
                        "distanceKm" to rs.getFloat("distance_km"),
                        "demandId" to rs.getLong("demand_id"),
                        "trackJson" to rs.getString("track_json")
                    ))
                }
                list
            }
            call.respond(mapOf("success" to true, "records" to records))
        }

        // ====== 提交考核 ======
        post("/exam/submit") {
            val req = call.receive<ExamSubmitRequest>()
            val passed = req.score >= 80
            val db = Database.conn

            db.prepareStatement("INSERT INTO exam_results (phone, score, passed) VALUES (?, ?, ?)").use {
                it.setString(1, req.phone)
                it.setInt(2, req.score)
                it.setBoolean(3, passed)
                it.executeUpdate()
            }

            call.respond(mapOf("success" to true, "passed" to passed, "score" to req.score,
                "message" to if (passed) "考核通过" else "未通过，需80分及以上"))
        }

        // ====== 志愿者排行榜 ======
        get("/leaderboard") {
            val db = Database.conn
            val list = db.prepareStatement(
                "SELECT phone, name, total_runs, rating, rating_count FROM users WHERE user_type = 'volunteer' ORDER BY total_runs DESC LIMIT 20"
            ).use { ps ->
                val rs = ps.executeQuery()
                val result = mutableListOf<Map<String, Any?>>()
                while (rs.next()) {
                    result.add(mapOf(
                        "phone" to rs.getString("phone"),
                        "name" to rs.getString("name"),
                        "totalRuns" to rs.getInt("total_runs"),
                        "rating" to rs.getFloat("rating"),
                        "ratingCount" to rs.getInt("rating_count")
                    ))
                }
                result
            }
            call.respond(mapOf("success" to true, "leaderboard" to list))
        }
    }
}

private fun ResultSet.toUser(): UserResponse {
    val db = Database.conn
    val exam = db.prepareStatement(
        "SELECT * FROM exam_results WHERE phone = ? ORDER BY created_at DESC LIMIT 1"
    ).use { ps ->
        ps.setString(1, getString("phone"))
        val rs = ps.executeQuery()
        if (rs.next()) rs.getBoolean("passed") to rs.getInt("score")
        else false to 0
    }
    return UserResponse(
        phone = getString("phone"),
        name = getString("name"),
        userType = getString("user_type"),
        rating = getFloat("rating"),
        totalRuns = getInt("total_runs"),
        totalDistanceKm = getFloat("total_distance_km"),
        examPassed = exam.first,
        examScore = exam.second
    )
}

private fun rsToUser(rs: ResultSet) = rs.use {
    if (it.next()) it.toUser() else null
}

private fun ResultSet.toDemandMap(): Map<String, Any?> = mapOf(
    "id" to getLong("id"),
    "ownerPhone" to getString("owner_phone"),
    "date" to getString("date"),
    "time" to getString("time"),
    "location" to getString("location"),
    "durationMinutes" to getInt("duration_minutes"),
    "distanceKm" to getFloat("distance_km"),
    "lat" to getDouble("lat"),
    "lng" to getDouble("lng"),
    "status" to getString("status"),
    "volunteerPhone" to getString("volunteer_phone"),
    "volunteerNote" to getString("volunteer_note"),
    "blindConfirmed" to getBoolean("blind_confirmed")
)
