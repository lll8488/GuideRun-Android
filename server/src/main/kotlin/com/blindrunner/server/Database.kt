package com.blindrunner.server

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private const val URL = "jdbc:h2:file:./blindrunner_data;AUTO_SERVER=TRUE"

    lateinit var conn: Connection
        private set

    fun init() {
        conn = DriverManager.getConnection(URL, "sa", "")
        createTables()
        println("✅ Database ready at $URL")
    }

    private fun createTables() {
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    phone VARCHAR(11) NOT NULL UNIQUE,
                    password VARCHAR(255),
                    name VARCHAR(100) NOT NULL DEFAULT '',
                    user_type VARCHAR(20) NOT NULL DEFAULT 'blind',
                    rating FLOAT DEFAULT 0,
                    rating_count INT DEFAULT 0,
                    total_runs INT DEFAULT 0,
                    total_distance_km FLOAT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS verify_codes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    phone VARCHAR(11) NOT NULL,
                    code VARCHAR(6) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    used BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS demands (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    owner_phone VARCHAR(11) NOT NULL,
                    date VARCHAR(20) NOT NULL,
                    time VARCHAR(10) NOT NULL,
                    location VARCHAR(255) NOT NULL,
                    duration_minutes INT NOT NULL,
                    distance_km FLOAT NOT NULL DEFAULT 0,
                    lat DOUBLE DEFAULT 0,
                    lng DOUBLE DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    volunteer_phone VARCHAR(11) DEFAULT '',
                    volunteer_note VARCHAR(255) DEFAULT '',
                    blind_confirmed BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS running_records (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    owner_phone VARCHAR(11) NOT NULL,
                    date VARCHAR(20) NOT NULL,
                    duration_minutes INT NOT NULL,
                    location VARCHAR(255) NOT NULL DEFAULT '',
                    distance_km FLOAT NOT NULL DEFAULT 0,
                    demand_id BIGINT DEFAULT 0,
                    volunteer_phone VARCHAR(11) DEFAULT '',
                    track_json TEXT DEFAULT '',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS exam_results (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    phone VARCHAR(11) NOT NULL,
                    score INT NOT NULL,
                    passed BOOLEAN NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())
        }
        println("✅ Tables created/verified")
    }
}
