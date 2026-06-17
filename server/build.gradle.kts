plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
}

group = "com.blindrunner.server"
version = "1.0"

application {
    mainClass.set("com.blindrunner.server.ApplicationKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")

    // H2 嵌入式数据库（零安装）
    implementation("com.h2database:h2:2.2.224")

    // bcrypt 密码哈希
    implementation("at.favre.lib:bcrypt:0.10.2")

    // 日志
    implementation("ch.qos.logback:logback-classic:1.4.11")
}
