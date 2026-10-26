package com.yuyue.app.domain

import com.yuyue.app.data.capture.CaptureCommand
import com.yuyue.app.data.capture.CaptureEntryWriter
import com.yuyue.app.data.capture.CapturePhase
import com.yuyue.app.data.capture.CaptureRawStore
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.InteractiveKind
import com.yuyue.app.data.capture.MaterializePayload

/**
 * Thin dual-write wrappers for CaptureKernel adapters.
 * Legacy remains source of truth for UI; domain mirrors when [enabled].
 */
object DomainDualWrite {
    fun wrapRawStore(
        legacy: CaptureRawStore,
        domain: DomainRepository,
        enabled: Boolean,
    ): CaptureRawStore {
        if (!enabled) return legacy
        return object : CaptureRawStore {
            override suspend fun saveRaw(command: CaptureCommand, requestId: String) {
                legacy.saveRaw(command, requestId)
                domain.mirrorInboxCapture(command, requestId)
            }

            override suspend fun updateMeta(
                requestId: String,
                status: CaptureStatus,
                phase: CapturePhase,
                interactiveKind: InteractiveKind?,
                message: String?,
            ) {
                legacy.updateMeta(requestId, status, phase, interactiveKind, message)
                domain.updateCaptureMeta(
                    id = "cap:ledger:$requestId",
                    status = when (status) {
                        CaptureStatus.Completed -> "committed"
                        CaptureStatus.Pending, CaptureStatus.Processing, CaptureStatus.Retryable -> "inbox"
                        else -> "inbox"
                    },
                )
            }
        }
    }

    fun wrapEntryWriter(
        legacy: CaptureEntryWriter,
        domain: DomainRepository,
        enabled: Boolean,
    ): CaptureEntryWriter {
        if (!enabled) return legacy
        return object : CaptureEntryWriter {
            override suspend fun writeRecord(payload: MaterializePayload): List<String> {
                val ids = legacy.writeRecord(payload)
                domain.mirrorMaterialized(payload, ids)
                return ids
            }
        }
    }
}
