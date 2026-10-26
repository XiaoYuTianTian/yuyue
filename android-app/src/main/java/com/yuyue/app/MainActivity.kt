package com.yuyue.app

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuyue.app.data.BackupFileNames
import com.yuyue.app.notify.OnThisDayWorker
import com.yuyue.app.security.AppLockPolicy
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.CaptureLaunch
import com.yuyue.app.ui.LegacyNavHost
import com.yuyue.app.ui.ThemeMode
import com.yuyue.app.ui.shell.YuyueNavHost
import com.yuyue.app.ui.theme.YuyueTheme
import kotlinx.coroutines.launch

/**
 * Intent / import / export / Compose host.
 * Navigation lives in [YuyueNavHost] (rebuild shell) or [LegacyNavHost].
 */
class MainActivity : ComponentActivity() {
    private lateinit var transcriber: SpeechTranscriber
    private var pendingSharedText: String? = null
    private var pendingSharedImage: Pair<String?, Uri?>? = null
    private var pendingQuickCapture: Boolean = false
    private var sharedTextHandler: ((String) -> Unit)? = null
    private var sharedMediaHandler: ((String?, String?, Uri?) -> Unit)? = null
    private var quickCaptureHandler: (() -> Unit)? = null
    private var importBytesHandler: ((ByteArray) -> Unit)? = null

    private var pendingExportBytes: ByteArray? = null

    private var unlockedSession: Boolean = false
    private var backgroundedAt: Long? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val bytes = pendingExportBytes ?: return@registerForActivityResult
        uri ?: return@registerForActivityResult
        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        pendingExportBytes = null
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes != null && bytes.isNotEmpty()) importBytesHandler?.invoke(bytes)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as YuyueApplication
        val container = app.container
        unlockedSession = !container.appLockStore.enabled
        transcriber = SpeechTranscriber(this)
        handleIncomingIntent(intent)

        setContent {
            val vm: AppViewModel = viewModel(
                factory = AppViewModel.Factory(
                    container.repository,
                    container.aiConfig,
                    container.profileStore,
                    container.captureLedger,
                    app,
                    container.captureKernel,
                ),
            )
            val themeMode by vm.themeMode.collectAsState()
            val dark = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> null
            }
            var unlockedUi by remember { androidx.compose.runtime.mutableStateOf(unlockedSession) }
            androidx.compose.runtime.DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (container.appLockStore.enabled) {
                            unlockedSession = AppLockPolicy.stillUnlocked(
                                wasUnlocked = unlockedSession,
                                backgroundedAtMillis = backgroundedAt,
                                nowMillis = System.currentTimeMillis(),
                            )
                        }
                        backgroundedAt = null
                        unlockedUi = unlockedSession
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            val locked = AppLockPolicy.shouldChallenge(
                enabled = container.appLockStore.enabled,
                unlocked = unlockedUi,
            )
            YuyueTheme(darkTheme = dark) {
                val scope = rememberCoroutineScope()
                importBytesHandler = { vm.previewImportBytes(it) }
                sharedTextHandler = { vm.offerSharedText(it) }
                sharedMediaHandler = { text, mime, uri -> vm.offerSharedMedia(text, mime, uri) }
                quickCaptureHandler = { vm.requestVoiceCapture() }
                LaunchedEffect(Unit) {
                    pendingSharedText?.let { text ->
                        pendingSharedText = null
                        vm.offerSharedText(text)
                    }
                    pendingSharedImage?.let { (caption, uri) ->
                        pendingSharedImage = null
                        vm.offerSharedMedia(caption, "image/*", uri)
                    }
                    if (pendingQuickCapture) {
                        pendingQuickCapture = false
                        vm.requestVoiceCapture()
                    }
                }

                suspend fun exportPlainZip() {
                    runCatching {
                        val bytes = vm.exportArchive()
                        val name = saveBytesToDownloads(
                            bytes,
                            BackupFileNames.zip(System.currentTimeMillis()),
                            "application/zip",
                        )
                        vm.showMessage("未加密导出已保存到「下载」/$name")
                    }.onFailure {
                        val bytes = vm.exportArchive()
                        pendingExportBytes = bytes
                        exportLauncher.launch(BackupFileNames.zip(System.currentTimeMillis()))
                    }
                }

                suspend fun exportEncrypted() {
                    if (!vm.backupPassphraseSet()) {
                        vm.showMessage("请先在设置里填写恢复密钥")
                        return
                    }
                    runCatching {
                        val name = vm.runEncryptedBackupToDownloads()
                        if (name != null && name.endsWith(".enc")) {
                            vm.showMessage("加密备份已保存到「下载」/$name")
                        } else {
                            vm.showMessage(name ?: "加密备份失败")
                        }
                    }.onFailure { vm.showMessage(it.message ?: "加密备份失败") }
                }

                suspend fun exportDoc(kind: String) {
                    runCatching {
                        when (kind) {
                            "md" -> {
                                val text = vm.exportDiaryMarkdown()
                                val name = "yuyue-export-${System.currentTimeMillis()}.md"
                                saveBytesToDownloads(text.toByteArray(), name, "text/markdown")
                                vm.showMessage("已导出 Markdown 到「下载」/$name")
                            }
                            "txt" -> {
                                val text = vm.exportDiaryTxt()
                                val name = "yuyue-export-${System.currentTimeMillis()}.txt"
                                saveBytesToDownloads(text.toByteArray(), name, "text/plain")
                                vm.showMessage("已导出 TXT 到「下载」/$name")
                            }
                            "pdf" -> {
                                val bytes = vm.exportDiaryPdf()
                                val name = "yuyue-export-${System.currentTimeMillis()}.pdf"
                                saveBytesToDownloads(bytes, name, "application/pdf")
                                vm.showMessage("已导出 PDF 到「下载」/$name")
                            }
                        }
                    }.onFailure { vm.showMessage(it.message ?: "导出失败") }
                }

                Box(Modifier.fillMaxSize()) {
                    if (BuildConfig.FEATURE_REBUILD_SHELL) {
                        YuyueNavHost(
                            appVm = vm,
                            transcriber = transcriber,
                            captureLedger = container.captureLedger,
                            onExport = { scope.launch { exportEncrypted() } },
                            onExportPlain = { scope.launch { exportPlainZip() } },
                            onExportMarkdown = { scope.launch { exportDoc("md") } },
                            onExportTxt = { scope.launch { exportDoc("txt") } },
                            onExportPdf = { scope.launch { exportDoc("pdf") } },
                            onImport = { openBackupFromDownloads() },
                        )
                    } else {
                        LegacyNavHost(
                            vm = vm,
                            transcriber = transcriber,
                            onExport = { scope.launch { exportEncrypted() } },
                            onExportPlain = { scope.launch { exportPlainZip() } },
                            onExportMarkdown = { scope.launch { exportDoc("md") } },
                            onExportTxt = { scope.launch { exportDoc("txt") } },
                            onExportPdf = { scope.launch { exportDoc("pdf") } },
                            onImport = { openBackupFromDownloads() },
                        )
                    }
                    if (locked) {
                        AppLockOverlay(
                            onUnlockWithPin = { pin ->
                                if (vm.verifyAppLockPin(pin)) {
                                    unlockedSession = true
                                    unlockedUi = true
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundedAt = System.currentTimeMillis()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_SEND -> {
                val mime = intent.type
                val stream = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_STREAM)
                if (stream != null || (mime != null && mime.startsWith("image/"))) {
                    val handler = sharedMediaHandler
                    if (handler != null) {
                        handler(text, mime, stream)
                    } else {
                        pendingSharedImage = text to stream
                    }
                } else {
                    val t = text?.takeIf { it.isNotBlank() } ?: return
                    val handler = sharedTextHandler
                    if (handler != null) handler(t) else pendingSharedText = t
                }
            }
            CaptureLaunch.isQuickCapture(intent?.action) -> {
                intent?.action = Intent.ACTION_MAIN
                val handler = quickCaptureHandler
                if (handler != null) handler() else pendingQuickCapture = true
            }
            intent?.getBooleanExtra(OnThisDayWorker.EXTRA_OPEN_ON_THIS_DAY, false) == true -> {
                // Banner already on Home; notification just brings app to front.
            }
        }
    }

    override fun onDestroy() {
        transcriber.destroy()
        super.onDestroy()
    }

    private fun openBackupFromDownloads() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/zip", "application/json", "application/octet-stream", "*/*"),
            )
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"),
            )
        }
        importLauncher.launch(intent)
    }

    private fun saveBytesToDownloads(bytes: ByteArray, name: String, mime: String): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法写入下载目录")
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("无法写入下载目录")
            contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
        } catch (e: Exception) {
            runCatching { contentResolver.delete(uri, null, null) }
            throw e
        }
        return name
    }
}

@androidx.compose.runtime.Composable
private fun AppLockOverlay(
    onUnlockWithPin: (String) -> Boolean,
) {
    var pin by remember { androidx.compose.runtime.mutableStateOf("") }
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("应用已锁定", style = MaterialTheme.typography.headlineSmall)
            Text("输入 PIN 继续", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter(Char::isDigit).take(8)
                    error = null
                },
                label = { Text("PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    if (!onUnlockWithPin(pin)) error = "PIN 不对"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("解锁") }
            Text(
                "生物识别入口已预留；真机需 androidx.biometric（离线构建未拉取）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
