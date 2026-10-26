package com.yuyue.app.di

import android.content.Context
import com.yuyue.app.BuildConfig
import com.yuyue.app.agent.MiniMaxChatClient
import com.yuyue.app.data.AiConfigStore
import com.yuyue.app.data.ProfileStore
import com.yuyue.app.data.YuyueRepository
import com.yuyue.app.data.ai.LocalVectorStore
import com.yuyue.app.data.ai.OnnxBgeEmbedder
import com.yuyue.app.data.capture.CaptureLedgerService
import com.yuyue.app.data.capture.CaptureKernelFactory
import com.yuyue.app.data.EncryptedBackupStore
import com.yuyue.app.data.EncryptedBackupEngine
import com.yuyue.app.data.local.YuyueDatabase
import com.yuyue.app.domain.DomainRepository
import com.yuyue.app.domain.local.YuyueDomainDatabase
import com.yuyue.app.domain.migrate.DomainMigrationOutcome
import com.yuyue.app.domain.migrate.DomainMigrationRunner
import com.yuyue.app.domain.migrate.LegacyV7SnapshotLoader
import com.yuyue.app.util.NetworkProbe
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    val aiConfig = AiConfigStore(context)
    val profileStore = ProfileStore(context)
    val database = YuyueDatabase.get(context)
    /** Independent domain DB (yuyue-domain.db); opened alongside legacy yuyue.db. */
    val domainDatabase = YuyueDomainDatabase.get(context)
    val domainWriteEnabled: Boolean = BuildConfig.FEATURE_DOMAIN_WRITE
    val domainRepository = DomainRepository(
        db = domainDatabase,
        writeEnabled = domainWriteEnabled,
    )
    private val domainSnapshotLoader = LegacyV7SnapshotLoader(database)
    val domainMigrationRunner = DomainMigrationRunner(
        snapshotLoader = domainSnapshotLoader,
        domainRepository = domainRepository,
    )
    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (com.yuyue.app.BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    val embedder = OnnxBgeEmbedder(context.applicationContext)
    val vectorStore = LocalVectorStore(database, embedder)
    private val chatClient = MiniMaxChatClient(aiConfig, http, json)
    val structuredTurnClient = com.yuyue.app.llm.StructuredTurnClient(chatClient)
    val fastExtractClient = com.yuyue.app.llm.FastExtractClient(chatClient, aiConfig)

    val repository = YuyueRepository(
        database,
        vectorStore,
        json,
        mediaDir = java.io.File(context.filesDir, "entry_media"),
    )
    val captureLedger = CaptureLedgerService(
        dao = database.captureLedgerDao(),
        repository = repository,
        structuredTurn = structuredTurnClient,
        fastExtract = fastExtractClient,
        aiConfig = aiConfig,
        networkOnline = { NetworkProbe.isOnline(context) },
    )
    val captureKernel = CaptureKernelFactory.create(
        dao = database.captureLedgerDao(),
        repository = repository,
        fastExtract = fastExtractClient,
        domainRepository = domainRepository,
        domainWriteEnabled = domainWriteEnabled,
    )
    val backupStore = EncryptedBackupStore(context)
    val backupEngine = EncryptedBackupEngine(context, repository, backupStore)
    val themeStore = com.yuyue.app.ui.ThemeStore(context)
    val undoStore = com.yuyue.app.ui.PersistentUndoStore(context)
    val appLockStore = com.yuyue.app.security.AppLockStore(context)

    /** First-launch / explicit migrate gate; soft-fails without deleting legacy. */
    suspend fun ensureDomainMigrated(): DomainMigrationOutcome =
        domainMigrationRunner.runIfNeeded()
}
