package com.yuyue.app

import android.app.Application
import com.yuyue.app.data.EncryptedBackupWork
import com.yuyue.app.data.capture.CaptureQueueWork
import com.yuyue.app.di.AppContainer
import com.yuyue.app.notify.OnThisDayWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YuyueApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        OnThisDayWork.enqueue(this)
        if (container.backupStore.passphrase.isNotBlank()) {
            EncryptedBackupWork.enqueue(this)
        }
        appScope.launch {
            container.captureLedger.pendingRequestIds()
                .forEach { requestId -> CaptureQueueWork.enqueue(this@YuyueApplication, requestId) }
        }
        appScope.launch {
            // Domain cutover: empty domain ← legacy via LegacyV7Importer (soft-fail).
            container.ensureDomainMigrated()
        }
    }
}
