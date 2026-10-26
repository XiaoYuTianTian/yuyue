package com.yuyue.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.components.CaptureQueueBar
import com.yuyue.app.ui.components.VoiceCaptureDock
import com.yuyue.app.ui.theme.InkMute
import com.yuyue.app.ui.theme.Moss
import com.yuyue.app.ui.theme.PaperRaised

private data class ShellTab(
    val label: String,
    val route: Any,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val shellTabs = listOf(
    ShellTab("首页", Routes.Home, Icons.Outlined.Home),
    ShellTab("时间", Routes.Time, Icons.Outlined.CalendarMonth),
    ShellTab("回忆", Routes.Recall, Icons.Outlined.Search),
    ShellTab("保险库", Routes.Vault, Icons.Outlined.Lock),
)

fun NavDestination?.isShellTab(): Boolean =
    this != null && shellTabs.any { hasRoute(it.route::class) }

/**
 * App shell: bottom tabs + global Capture overlay layer (not a tab).
 * Capture dock is shared across Home / Time / Recall; hidden on Vault.
 */
@Composable
fun ShellScaffold(
    navController: NavHostController,
    captureVm: CaptureViewModel,
    appVm: AppViewModel,
    transcriber: SpeechTranscriber,
    content: @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination.isShellTab()
    val onVault = destination?.hasRoute(Routes.Vault::class) == true
    val showCaptureDock = showBottomBar && !onVault
    val queue by captureVm.activeQueue.collectAsState()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    CaptureOverlaySlot(
                        visible = showCaptureDock,
                        queueOnly = onVault && queue.isNotEmpty(),
                        captureVm = captureVm,
                        appVm = appVm,
                        transcriber = transcriber,
                    )
                    NavigationBar(
                        containerColor = PaperRaised,
                        contentColor = Moss,
                    ) {
                        val colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Moss,
                            selectedTextColor = Moss,
                            unselectedIconColor = InkMute,
                            unselectedTextColor = InkMute,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                        shellTabs.forEach { tab ->
                            val selected = destination?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.Shell) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = colors,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            content()
        }
    }
}

/**
 * Shared capture chrome across Home / Time / Recall.
 * Session state lives in [CaptureViewModel]; submit/toasts stay on [AppViewModel].
 */
@Composable
fun CaptureOverlaySlot(
    captureVm: CaptureViewModel,
    appVm: AppViewModel,
    transcriber: SpeechTranscriber,
    visible: Boolean = true,
    queueOnly: Boolean = false,
) {
    val queue by captureVm.activeQueue.collectAsState()
    val jobRunning by appVm.jobRunning.collectAsState()
    val savedTitle by appVm.savedTitle.collectAsState()
    val clarifyQuestion by appVm.clarifyQuestion.collectAsState()
    val pendingDraft by appVm.pendingDraft.collectAsState()
    val voiceNonce by appVm.voiceNonce.collectAsState()
    val recallHits by appVm.recallHits.collectAsState()
    val analyzingLabel by appVm.analyzingLabel.collectAsState()
    val captureFocus by captureVm.composerFocusNonce.collectAsState()
    val appFocus by appVm.composerFocusNonce.collectAsState()
    val composerFocusNonce = captureFocus + appFocus

    if (queueOnly) {
        CaptureQueueBar(
            items = queue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            onDiscard = { appVm.discardCapture(it) },
            onRetry = { appVm.retryCapture(it) },
            onContinue = { appVm.continueCapture(it) },
            onPause = { appVm.pauseCapture(it) },
            onResume = { id, raw -> appVm.resumeCapture(id, raw) },
        )
        return
    }
    if (!visible) return

    VoiceCaptureDock(
        transcriber = transcriber,
        jobRunning = jobRunning,
        clarifyQuestion = clarifyQuestion,
        dismissKey = savedTitle ?: recallHits?.let { "recall" }
            ?: voiceNonce.takeIf { it > 0 }?.let { "v$it" },
        pendingDraft = pendingDraft,
        analyzingLabel = analyzingLabel,
        composerFocusNonce = composerFocusNonce,
        consumeNavigationBars = false,
        extraAbove = {
            CaptureQueueBar(
                items = queue,
                onDiscard = { appVm.discardCapture(it) },
                onRetry = { appVm.retryCapture(it) },
                onContinue = { appVm.continueCapture(it) },
                onPause = { appVm.pauseCapture(it) },
                onResume = { id, raw -> appVm.resumeCapture(id, raw) },
            )
        },
        onSubmit = { text, uris, source ->
            // Toasts / clarify / pending draft stay on AppViewModel;
            // CaptureViewModel keeps shell-scoped draft/focus across tabs.
            appVm.submitStructuredTurn(text, uris, source)
        },
        onSaveNow = { appVm.commitPendingDraft() },
        onCancelPending = { appVm.dismissPendingDraft() },
    )
}
