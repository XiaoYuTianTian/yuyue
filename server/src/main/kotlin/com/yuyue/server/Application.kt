package com.yuyue.server

import com.yuyue.server.config.AppConfig
import com.yuyue.server.db.DatabaseFactory
import com.yuyue.server.plugins.*
import com.yuyue.server.service.JobWorker
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    AppConfig.logStartup()
    embeddedServer(Netty, port = AppConfig.port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureHTTP()
    configureSecurity()
    configureRouting()
    JobWorker.start()
    environment.monitor.subscribe(ApplicationStopped) {
        JobWorker.stop()
    }
}
