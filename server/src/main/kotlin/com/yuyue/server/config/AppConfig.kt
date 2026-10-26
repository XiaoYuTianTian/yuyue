package com.yuyue.server.config

object AppConfig {
    val port: Int = ConfigLoader.getInt("PORT", 8080)
    val jdbcUrl: String = ConfigLoader.get("DATABASE_URL")
        ?: "jdbc:h2:file:./data/yuyue;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
    val jdbcUser: String = ConfigLoader.get("DATABASE_USER") ?: "sa"
    val jdbcPassword: String = ConfigLoader.get("DATABASE_PASSWORD") ?: ""
    val jwtSecret: String = ConfigLoader.get("JWT_SECRET") ?: "yuyue-dev-secret-change-in-production"
    val llmApiKey: String? = ConfigLoader.get("LLM_API_KEY")
    val llmBaseUrl: String = ConfigLoader.get("LLM_BASE_URL") ?: "https://api.deepseek.com"
    val llmModel: String = ConfigLoader.get("LLM_MODEL") ?: "deepseek-chat"
    val devMode: Boolean = ConfigLoader.getBoolean("DEV_MODE", true)
    val dailyIngestLimit: Int = ConfigLoader.getInt("DAILY_INGEST_LIMIT", 50)
    val dailyRecallLimit: Int = ConfigLoader.getInt("DAILY_RECALL_LIMIT", 30)

    fun logStartup() {
        val source = ConfigLoader.configFile?.path ?: "defaults / environment variables"
        println("[Yuyue] Config: $source")
        println("[Yuyue] DEV_MODE=$devMode, LLM=${if (llmApiKey.isNullOrBlank()) "mock (no API key)" else "enabled (${llmModel})"}")
    }
}
