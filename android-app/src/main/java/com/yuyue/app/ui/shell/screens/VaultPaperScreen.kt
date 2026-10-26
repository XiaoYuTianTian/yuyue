package com.yuyue.app.ui.shell.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yuyue.app.BuildConfig
import com.yuyue.app.data.LlmCatalog
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.security.AppLockPolicy
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.ThemeMode
import com.yuyue.app.ui.paper.PaperScaffold
import com.yuyue.app.ui.theme.PaperRaised
import com.yuyue.app.ui.theme.parseNotebookColor

/**
 * Vault: lock / backup / trash / notebooks / appearance / export / AI advanced.
 */
@Composable
fun VaultPaperScreen(
    appVm: AppViewModel,
    onExport: () -> Unit,
    onExportPlain: () -> Unit = {},
    onExportMarkdown: () -> Unit = {},
    onExportTxt: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onImport: () -> Unit,
    onOpenTrash: () -> Unit,
    onEditWorkspace: (String) -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    val notebooks by appVm.notebooks.collectAsState()
    val themeMode by appVm.themeMode.collectAsState()
    val cfg = remember { appVm.currentAiConfig() }
    var showAi by remember { mutableStateOf(false) }
    var selectedModelId by remember {
        mutableStateOf(LlmCatalog.matching(cfg.model, cfg.baseUrl).id)
    }
    var selectedFastId by remember {
        mutableStateOf(LlmCatalog.matchingFast(cfg.fastModel, cfg.fastBaseUrl).id)
    }
    var pinDraft by remember { mutableStateOf("") }
    var passphraseDraft by remember { mutableStateOf("") }
    var lockMessage by remember { mutableStateOf<String?>(null) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val lockOn = remember { mutableStateOf(appVm.appLockEnabled()) }

    PaperScaffold(
        title = "保险库",
        subtitle = "备份与本机优先 · 个人数据不出机",
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VaultCard(title = "应用锁", body = "PIN 或生物识别；离开超过 30 秒需再解锁") {
                if (lockOn.value) {
                    Text("已开启", style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = {
                            appVm.clearAppLock()
                            lockOn.value = false
                            lockMessage = "已关闭应用锁"
                        },
                    ) { Text("关闭应用锁") }
                    TextButton(
                        onClick = {
                            appVm.setAppLockBiometric(true)
                            lockMessage = "已记录偏好；生物识别 Prompt 待真机依赖"
                        },
                    ) { Text("允许生物识别（偏好）") }
                } else {
                    OutlinedTextField(
                        value = pinDraft,
                        onValueChange = { pinDraft = it.filter(Char::isDigit).take(8) },
                        label = { Text("设置 4–8 位 PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            if (!AppLockPolicy.isValidPin(pinDraft)) {
                                lockMessage = "PIN 须为 4–8 位数字"
                                return@TextButton
                            }
                            runCatching {
                                appVm.setAppLockPin(pinDraft)
                                appVm.setAppLockBiometric(true)
                                lockOn.value = true
                                pinDraft = ""
                                lockMessage = "应用锁已开启"
                            }.onFailure { lockMessage = it.message }
                        },
                    ) { Text("开启应用锁") }
                }
                lockMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            VaultCard(title = "备份与恢复", body = "默认加密（YUY2，盐在文件头）；明文 ZIP 单独标注") {
                OutlinedTextField(
                    value = passphraseDraft,
                    onValueChange = { passphraseDraft = it },
                    label = { Text("恢复密钥") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        if (passphraseDraft.isBlank()) {
                            backupMessage = "请先填写恢复密钥"
                            return@TextButton
                        }
                        appVm.setBackupPassphrase(passphraseDraft)
                        backupMessage = "已保存恢复密钥"
                    },
                ) { Text("保存恢复密钥") }
                VaultAction("加密备份（默认）", onClick = onExport)
                HorizontalDivider()
                VaultAction("未加密导出", onClick = onExportPlain)
                HorizontalDivider()
                VaultAction("恢复（先预览）", onClick = onImport)
                backupMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            VaultCard(title = "浏览与回顾", body = "人物 / 地点 / 主题时间线 · 周月回顾 · 照片墙（本机聚合）") {
                DifferentiatorLinks(
                    onPeople = onOpenPeople,
                    onPlaces = onOpenPlaces,
                    onTopics = onOpenTopics,
                    onWeek = onOpenWeekReview,
                    onMonth = onOpenMonthReview,
                    onPhotos = onOpenPhotoWall,
                    onExtras = onOpenLocalExtras,
                )
            }
            VaultCard(title = "废纸篓", body = "软删条目可恢复或永久删除") {
                VaultAction("打开废纸篓", onClick = onOpenTrash)
            }
            VaultCard(title = "记事本", body = "${notebooks.size} 个本 · 只做筛选，不是主叙事") {
                notebooks.forEach { nb ->
                    NotebookRow(nb, onEdit = { onEditWorkspace(nb.id) })
                }
            }
            VaultCard(title = "外观", body = "纸墨苔陶主题跟随系统或固定明暗") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { appVm.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.System -> "系统"
                                        ThemeMode.Light -> "浅色"
                                        ThemeMode.Dark -> "深色"
                                    },
                                )
                            },
                        )
                    }
                }
            }
            VaultCard(title = "可读导出", body = "Markdown / TXT / PDF（非加密备份）") {
                VaultAction("导出 Markdown", onClick = onExportMarkdown)
                HorizontalDivider()
                VaultAction("导出 TXT", onClick = onExportTxt)
                HorizontalDivider()
                VaultAction("导出 PDF", onClick = onExportPdf)
            }
            VaultCard(
                title = "AI 高级",
                body = if (showAi) {
                    "快速提取与润色模型；失败不得误写日记"
                } else {
                    "折叠 · 点开配置模型"
                },
            ) {
                Text(
                    if (showAi) "收起" else "展开",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showAi = !showAi },
                )
                if (showAi) {
                    Spacer(Modifier.height(8.dp))
                    Text("快速提取", style = MaterialTheme.typography.titleSmall)
                    LlmCatalog.fast.forEach { opt ->
                        FilterChip(
                            selected = selectedFastId == opt.id,
                            onClick = {
                                selectedFastId = opt.id
                                appVm.selectFastLlmModel(opt.id)
                            },
                            label = { Text(opt.title) },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("整理润色", style = MaterialTheme.typography.titleSmall)
                    LlmCatalog.organize.forEach { opt ->
                        FilterChip(
                            selected = selectedModelId == opt.id,
                            onClick = {
                                selectedModelId = opt.id
                                appVm.selectLlmModel(opt.id)
                            },
                            label = { Text(opt.title) },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                "语曰 v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun VaultCard(
    title: String,
    body: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = PaperRaised,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun VaultAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun NotebookRow(nb: NotebookEntity, onEdit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseNotebookColor(nb.color)),
            )
            Text(nb.name, style = MaterialTheme.typography.bodyLarge)
        }
        Text("编辑", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
