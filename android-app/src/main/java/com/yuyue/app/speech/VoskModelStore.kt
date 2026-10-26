package com.yuyue.app.speech

import android.content.Context
import android.util.Log
import java.io.File
import java.util.zip.ZipInputStream

object VoskModelStore {
    private const val TAG = "YuyueSpeech"
    private const val ASSET_ZIP = "vosk-model-small-cn-0.22.zip"
    private const val ROOT = "vosk-model-small-cn-0.22"

    fun ensure(context: Context): File {
        val dir = File(context.filesDir, "vosk-cn")
        val ready = File(dir, ".ready")
        val am = File(dir, "am/final.mdl")
        if (ready.exists() && am.exists()) return dir

        dir.deleteRecursively()
        dir.mkdirs()
        Log.i(TAG, "extracting $ASSET_ZIP to ${dir.absolutePath}")
        context.assets.open(ASSET_ZIP).use { raw ->
            ZipInputStream(raw).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    var rel = entry.name.replace('\\', '/')
                    if (rel.startsWith("$ROOT/")) rel = rel.removePrefix("$ROOT/")
                    if (rel.isBlank() || rel == "/") continue
                    val out = File(dir, rel)
                    val canonical = out.canonicalPath
                    if (!canonical.startsWith(dir.canonicalPath)) {
                        throw IllegalStateException("zip path traversal: $rel")
                    }
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                }
            }
        }
        if (!am.exists()) {
            throw IllegalStateException("语音模型解压失败，缺少 am/final.mdl")
        }
        ready.writeText("ok")
        Log.i(TAG, "vosk model ready")
        return dir
    }
}
