package com.yuyue.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuyue.app.ui.AppViewModel

/** Soft capsule — sits gently on the navy canvas without a hard white slab. */
private val ToastShape = RoundedCornerShape(999.dp)

@Composable
fun YuyueToastHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val bubble = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        val action = data.visuals.actionLabel
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .fillMaxWidth(),
            shape = ToastShape,
            color = bubble,
            shadowElevation = 2.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = if (action != null) 8.dp else 20.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ink,
                    textAlign = if (action != null) TextAlign.Start else TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                )
                if (action != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(action, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.YuyueTopToastHost(hostState: SnackbarHostState) {
    YuyueToastHost(
        hostState = hostState,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
    )
}

@Composable
fun ObserveYuyueToasts(vm: AppViewModel, snackbar: SnackbarHostState) {
    val message by vm.message.collectAsState()
    val action by vm.toastAction.collectAsState()
    LaunchedEffect(message, action) {
        val text = message ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = text,
            actionLabel = action,
            duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.onToastAction()
        vm.clearMessage()
    }
}
