package com.yuyue.server.config

import java.io.File
import java.util.Properties

object ConfigLoader {
    private val fileProps: Properties by lazy { loadFileProperties() }
    val configFile: File? by lazy { resolveConfigFile() }

    fun get(key: String): String? {
        val env = System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
        if (env != null) return env
        return fileProps.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun getInt(key: String, default: Int): Int = get(key)?.toIntOrNull() ?: default

    fun getBoolean(key: String, default: Boolean): Boolean = get(key)?.toBooleanStrictOrNull() ?: default

    private fun loadFileProperties(): Properties {
        val props = Properties()
        resolveConfigFile()?.inputStream()?.use { props.load(it) }
        return props
    }

    private fun resolveConfigFile(): File? {
        System.getenv("YUYUE_CONFIG")?.let { path ->
            val file = File(path)
            if (file.isFile) return file.absoluteFile
        }

        val candidates = mutableListOf<File>()
        var dir: File? = File(System.getProperty("user.dir"))
        repeat(4) {
            if (dir != null) {
                candidates += File(dir, "config.local.properties")
                candidates += File(dir, "server/config.local.properties")
                dir = dir.parentFile
            }
        }

        return candidates.distinct().firstOrNull { it.isFile }?.absoluteFile
    }
}
