package com.yuyue.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.LlmCatalog
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.ProfileAvatar
import com.yuyue.app.ui.components.YuyueAlertDialog
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.theme.parseNotebookColor
import com.yuyue.app.ui.theme.yuyueTopBarColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onExportPlain: () -> Unit = {},
    onExportMarkdown: () -> Unit = {},
    onExportTxt: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onImport: () -> Unit,
    onOpenTrash: () -> Unit,
    onEditWorkspace: (NotebookEntity) -> Unit,
) {
    var showModel by remember { mutableStateOf(true) }
    var showProfile by remember { mutableStateOf(true) }
    var showAbout by remember { mutableStateOf(false) }
    var showWorkspaces by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }
    var deleteNotebook by remember { mutableStateOf<NotebookEntity?>(null) }
    val cfg = remember { vm.currentAiConfig() }
    val profile by vm.profile.collectAsState()
    val notebooks by vm.notebooks.collectAsState()
    var selectedModelId by remember {
        mutableStateOf(LlmCatalog.matching(cfg.model, cfg.baseUrl).id)
    }
    var selectedFastId by remember {
        mutableStateOf(LlmCatalog.matchingFast(cfg.fastModel, cfg.fastBaseUrl).id)
    }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var gender by remember { mutableStateOf(profile.gender) }
    var phone by remember { mutableStateOf(profile.phone) }
    var age by remember { mutableStateOf(profile.age) }
    var profileSavedFlash by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(profile) {
        if (!editingProfile) {
            displayName = profile.displayName
            gender = profile.gender
            phone = profile.phone
            age = profile.age
        }
    }

    LaunchedEffect(profileSavedFlash) {
        if (profileSavedFlash) {
            delay(1600)
            profileSavedFlash = false
        }
    }

    val snackbar = remember { SnackbarHostState() }
    val importPreview by vm.importPreview.collectAsState()
    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        vm.setAvatarFromGallery(uri)
    }
    ObserveYuyueToasts(vm, snackbar)

    fun hideIme() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
    }

    fun resetProfileDraft() {
        displayName = profile.displayName
        gender = profile.gender
        phone = profile.phone
        age = profile.age
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {},
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    colors = yuyueTopBarColors(),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsGroup {
                    SettingsRow(
                        title = "个人资料",
                        subtitle = displayName.ifBlank { "个性化定制" },
                        expanded = showProfile,
                        onClick = {
                            showProfile = !showProfile
                            if (!showProfile) {
                                editingProfile = false
                                resetProfileDraft()
                            }
                        },
                    )
                    if (showProfile) {
                        HorizontalDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ProfileAvatar(
                                    displayName = displayName.ifBlank { profile.displayName },
                                    avatarUri = profile.avatarUri,
                                    size = 56.dp,
                                )
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "头像",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (editingProfile) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { pickAvatar.launch("image/*") },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            ) { Text("更换头像") }
                                            if (profile.avatarUri != null) {
                                                OutlinedButton(
                                                    onClick = { vm.clearAvatar() },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                ) { Text("恢复默认") }
                                            }
                                        }
                                    }
                                }
                            }
                            if (editingProfile) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("名字") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                Text(
                                    "性别",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("女", "男", "其他").forEach { option ->
                                        FilterChip(
                                            selected = gender == option,
                                            onClick = { gender = if (gender == option) "" else option },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("手机号（可选）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                )
                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it.filter { ch -> ch.isDigit() }.take(3) },
                                    label = { Text("年龄（可选）") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            hideIme()
                                            resetProfileDraft()
                                            editingProfile = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("取消") }
                                    Button(
                                        onClick = {
                                            hideIme()
                                            vm.saveProfile(displayName, gender, phone, age)
                                            profileSavedFlash = true
                                            editingProfile = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) { Text(if (profileSavedFlash) "已保存" else "保存") }
                                }
                            } else {
                                SettingsPreviewLine("名字", displayName.ifBlank { "未填写" })
                                SettingsPreviewLine("性别", gender.ifBlank { "未填写" })
                                SettingsPreviewLine("手机号", phone.ifBlank { "未填写" })
                                SettingsPreviewLine("年龄", age.ifBlank { "未填写" })
                                Button(
                                    onClick = { editingProfile = true },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("修改") }
                            }
                        }
                    }
                }
                SettingsGroup {
                    SettingsRow(
                        title = "加密备份",
                        subtitle = "默认 · YUY2 加密到下载目录",
                        showChevron = false,
                        onClick = onExport,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "未加密导出",
                        subtitle = "明文 ZIP，勿当作安全备份",
                        showChevron = false,
                        onClick = onExportPlain,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "恢复日记",
                        subtitle = "恢复以前的备份",
                        showChevron = false,
                        onClick = onImport,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "导出 Markdown",
                        subtitle = "可读归档",
                        showChevron = false,
                        onClick = onExportMarkdown,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "导出 TXT",
                        subtitle = "纯文本归档",
                        showChevron = false,
                        onClick = onExportTxt,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "导出 PDF",
                        subtitle = "简易 PDF（拉丁回退）",
                        showChevron = false,
                        onClick = onExportPdf,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        title = "废纸篓",
                        subtitle = "删掉的记录暂时放在这里",
                        onClick = onOpenTrash,
                    )
                }
                SettingsGroup {
                    SettingsRow(
                        title = "工作区管理",
                        subtitle = "${notebooks.size} 个工作区",
                        expanded = showWorkspaces,
                        onClick = { showWorkspaces = !showWorkspaces },
                    )
                    if (showWorkspaces) {
                        HorizontalDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            notebooks.forEach { nb ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseNotebookColor(nb.color)),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(nb.name, style = MaterialTheme.typography.titleMedium)
                                        if (nb.isDefault) {
                                            Text(
                                                "默认",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { onEditWorkspace(nb) },
                                    ) { Text("编辑") }
                                    if (notebooks.size > 1) {
                                        TextButton(onClick = { deleteNotebook = nb }) {
                                            Text("删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                SettingsGroup {
                    SettingsRow(
                        title = "模型设置",
                        subtitle = "${LlmCatalog.byIdFast(selectedFastId).title} · ${LlmCatalog.byId(selectedModelId).title}",
                        expanded = showModel,
                        onClick = { showModel = !showModel },
                    )
                    if (showModel) {
                        HorizontalDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModelSection(
                                heading = "快速提取",
                                caption = "有没有说出日期、记到哪一天、标题和一两句事实。",
                                options = LlmCatalog.fast,
                                selectedId = selectedFastId,
                                onSelect = { id ->
                                    selectedFastId = id
                                    vm.selectFastLlmModel(id)
                                },
                            )
                            ModelSection(
                                heading = "整理润色",
                                caption = "把日记写通顺；查找和修改也走这里。能看懂照片请选 M3。",
                                options = LlmCatalog.organize,
                                selectedId = selectedModelId,
                                onSelect = { id ->
                                    selectedModelId = id
                                    vm.selectLlmModel(id)
                                },
                            )
                        }
                    }
                }
                SettingsGroup {
                    SettingsRow(
                        title = "关于语曰",
                        subtitle = "v${com.yuyue.app.BuildConfig.VERSION_NAME}",
                        expanded = showAbout,
                        onClick = { showAbout = !showAbout },
                    )
                    if (showAbout) {
                        HorizontalDivider()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "美妙的回忆，尽在语间。",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "随时随地记录我的生活。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        YuyueTopToastHost(snackbar)
    }

    deleteNotebook?.let { nb ->
        YuyueAlertDialog(
            onDismissRequest = { deleteNotebook = null },
            title = "删除工作区？",
            text = "「${nb.name}」里的日记都会清空（可在废纸篓找回）。此操作不可撤销工作区本身。",
            confirmLabel = "删除",
            onConfirm = {
                vm.deleteNotebook(nb.id)
                deleteNotebook = null
            },
            dismissLabel = "取消",
        )
    }

    importPreview?.let { preview ->
        YuyueAlertDialog(
            onDismissRequest = { vm.cancelImport() },
            title = "从备份恢复",
            text = {
                Text(
                    buildString {
                        append("将合并 ${preview.notebooks} 本、${preview.entries} 则。")
                        if (preview.skipped > 0) append(" 跳过 ${preview.skipped} 则。")
                        append(" 只合并本机，不上传。")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            buttons = {
                TextButton(onClick = { vm.cancelImport() }) { Text("取消") }
                TextButton(onClick = { vm.confirmImport(true) }) { Text("恢复为副本") }
                TextButton(onClick = { vm.confirmImport(false) }) { Text("较新优先") }
            },
        )
    }
}

@Composable
private fun ModelSection(
    heading: String,
    caption: String,
    options: List<com.yuyue.app.data.LlmModelOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(heading, style = MaterialTheme.typography.titleMedium)
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(option.id) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RadioButton(
                    selected = selectedId == option.id,
                    onClick = { onSelect(option.id) },
                )
                Column(Modifier.weight(1f)) {
                    Text(option.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        option.subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPreviewLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    expanded: Boolean? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded == true) 90f else 0f,
        label = "settingsChevron",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded == true) {
                    "收起"
                } else if (expanded == false) {
                    "展开"
                } else {
                    null
                },
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}
