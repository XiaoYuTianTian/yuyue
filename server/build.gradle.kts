plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.yuyue.server.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.h2)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.logback)
    implementation(libs.jwt)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.yuyue.server.ApplicationKt"
    }
}
