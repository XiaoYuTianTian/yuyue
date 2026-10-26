package com.yuyue.app.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yuyue.app.YuyueApplication
import com.yuyue.app.data.capture.CaptureLedgerService
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.AppViewModel
import kotlinx.coroutines.launch

/**
 * Rebuild shell root: typed NavHost + split ViewModels + global Capture layer.
 *
 * Capture writes go through [CaptureKernel] (shell [CaptureViewModel] + AppViewModel wrappers).
 */
@Composable
fun YuyueNavHost(
    appVm: AppViewModel,
    transcriber: SpeechTranscriber,
    captureLedger: CaptureLedgerService?,
    onExport: suspend () -> Unit,
    onExportPlain: suspend () -> Unit = {},
    onExportMarkdown: suspend () -> Unit = {},
    onExportTxt: suspend () -> Unit = {},
    onExportPdf: suspend () -> Unit = {},
    onImport: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as YuyueApplication
    val repository = app.container.repository
    val scope = rememberCoroutineScope()

    val captureVm: CaptureViewModel = viewModel(
        factory = CaptureViewModel.Factory(captureLedger, app.container.captureKernel),
    )
    // Ensure tab-scoped VMs are created (stubs wired to repository).
    viewModel<HomeViewModel>(factory = HomeViewModel.Factory(repository))
    viewModel<TimeViewModel>(factory = TimeViewModel.Factory(repository))
    val recallVm: RecallViewModel = viewModel(factory = RecallViewModel.Factory(repository))
    viewModel<VaultViewModel>(factory = VaultViewModel.Factory(repository))

    val selectedNotebookId by appVm.selectedNotebookId.collectAsState()
    val notebooks by appVm.notebooks.collectAsState()
    LaunchedEffect(selectedNotebookId, notebooks) {
        val name = notebooks.find { it.id == selectedNotebookId }?.name.orEmpty()
        captureVm.bindNotebook(selectedNotebookId, name)
    }

    val welcomeSeen by appVm.welcomeSeen.collectAsState()
    val unlockDay by appVm.unlockDay.collectAsState()

    LaunchedEffect(Unit) {
        appVm.openEntryReady.collect { entry ->
            navController.navigate(Routes.Entry(entry.id))
        }
    }

    LaunchedEffect(unlockDay) {
        val day = unlockDay ?: return@LaunchedEffect
        navController.navigate(Routes.Day(day.toEpochDay()))
        appVm.clearUnlockDay()
    }

    val startDestination: Any = if (welcomeSeen) Routes.Shell else Routes.Onboarding

    ShellScaffold(
        navController = navController,
        captureVm = captureVm,
        appVm = appVm,
        transcriber = transcriber,
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable<Routes.Onboarding> {
                OnboardingDestination(
                    appVm = appVm,
                    transcriber = transcriber,
                    onFinished = {
                        navController.navigate(Routes.Shell) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    },
                )
            }

            navigation<Routes.Shell>(startDestination = Routes.Home) {
                composable<Routes.Home> {
                    HomeDestination(
                        appVm = appVm,
                        onOpenVault = {
                            navController.navigate(Routes.Vault) {
                                popUpTo(Routes.Shell) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenTime = {
                            navController.navigate(Routes.Time) {
                                popUpTo(Routes.Shell) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenDay = { day ->
                            navController.navigate(Routes.Day(day.toEpochDay()))
                        },
                        onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                        onEditEntry = { id -> navController.navigate(Routes.EntryEdit(id)) },
                    )
                }
                composable<Routes.Time> {
                    TimeDestination(
                        appVm = appVm,
                        onOpenDay = { day ->
                            navController.navigate(Routes.Day(day.toEpochDay()))
                        },
                        onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                        onOpenPeople = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Person)))
                        },
                        onOpenPlaces = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Place)))
                        },
                        onOpenTopics = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Topic)))
                        },
                        onOpenWeekReview = { navController.navigate(Routes.WeekReview) },
                        onOpenMonthReview = { navController.navigate(Routes.MonthReview) },
                        onOpenPhotoWall = { navController.navigate(Routes.PhotoWall) },
                        onOpenLocalExtras = { navController.navigate(Routes.LocalExtras) },
                    )
                }
                composable<Routes.Recall> {
                    RecallDestination(
                        appVm = appVm,
                        recallVm = recallVm,
                        onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                        onExpandCapture = {
                            captureVm.requestComposerFocus()
                            appVm.requestVoiceCapture()
                        },
                        onOpenPeople = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Person)))
                        },
                        onOpenPlaces = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Place)))
                        },
                        onOpenTopics = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Topic)))
                        },
                        onOpenWeekReview = { navController.navigate(Routes.WeekReview) },
                        onOpenMonthReview = { navController.navigate(Routes.MonthReview) },
                        onOpenPhotoWall = { navController.navigate(Routes.PhotoWall) },
                        onOpenLocalExtras = { navController.navigate(Routes.LocalExtras) },
                    )
                }
                composable<Routes.Vault> {
                    VaultDestination(
                        appVm = appVm,
                        onExport = { scope.launch { onExport() } },
                        onExportPlain = { scope.launch { onExportPlain() } },
                        onExportMarkdown = { scope.launch { onExportMarkdown() } },
                        onExportTxt = { scope.launch { onExportTxt() } },
                        onExportPdf = { scope.launch { onExportPdf() } },
                        onImport = onImport,
                        onOpenTrash = { navController.navigate(Routes.Trash) },
                        onEditWorkspace = { id ->
                            navController.navigate(Routes.WorkspaceDetail(id))
                        },
                        onOpenPeople = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Person)))
                        },
                        onOpenPlaces = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Place)))
                        },
                        onOpenTopics = {
                            navController.navigate(Routes.FacetBrowse(DifferentiatorAggregate.axisRoute(FacetAxis.Topic)))
                        },
                        onOpenWeekReview = { navController.navigate(Routes.WeekReview) },
                        onOpenMonthReview = { navController.navigate(Routes.MonthReview) },
                        onOpenPhotoWall = { navController.navigate(Routes.PhotoWall) },
                        onOpenLocalExtras = { navController.navigate(Routes.LocalExtras) },
                    )
                }
            }

            composable<Routes.Entry> {
                EntryDestination(
                    repository = repository,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.EntryEdit(id)) },
                )
            }
            composable<Routes.EntryEdit> {
                EntryEditDestination(
                    repository = repository,
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.popBackStack(Routes.Shell, inclusive = false)
                    },
                )
            }
            composable<Routes.Day> { entry ->
                val route = entry.toRoute<Routes.Day>()
                DayDestination(
                    appVm = appVm,
                    transcriber = transcriber,
                    epochDay = route.epochDay,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
            composable<Routes.Trash> {
                TrashDestination(
                    appVm = appVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Routes.WorkspaceDetail> { entry ->
                val route = entry.toRoute<Routes.WorkspaceDetail>()
                WorkspaceDetailDestination(
                    appVm = appVm,
                    notebookId = route.notebookId,
                    onBack = { navController.popBackStack() },
                    onManageEntries = {
                        navController.navigate(Routes.WorkspaceEntries(route.notebookId))
                    },
                )
            }
            composable<Routes.WorkspaceEntries> { entry ->
                val route = entry.toRoute<Routes.WorkspaceEntries>()
                WorkspaceEntriesDestination(
                    appVm = appVm,
                    notebookId = route.notebookId,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
            composable<Routes.FacetBrowse> { entry ->
                val route = entry.toRoute<Routes.FacetBrowse>()
                FacetBrowseDestination(
                    repository = repository,
                    axis = route.axis,
                    onBack = { navController.popBackStack() },
                    onOpenBucket = { axis, key ->
                        navController.navigate(Routes.FacetTimeline(axis = axis, key = key))
                    },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
            composable<Routes.FacetTimeline> { entry ->
                val route = entry.toRoute<Routes.FacetTimeline>()
                FacetTimelineDestination(
                    repository = repository,
                    axis = route.axis,
                    key = route.key,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
            composable<Routes.WeekReview> {
                PeriodReviewDestination(
                    repository = repository,
                    kind = "week",
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                    onOpenPerson = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Person),
                                key = name.lowercase(),
                            ),
                        )
                    },
                    onOpenPlace = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Place),
                                key = name.lowercase(),
                            ),
                        )
                    },
                    onOpenTopic = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Topic),
                                key = name.lowercase(),
                            ),
                        )
                    },
                )
            }
            composable<Routes.MonthReview> {
                PeriodReviewDestination(
                    repository = repository,
                    kind = "month",
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                    onOpenPerson = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Person),
                                key = name.lowercase(),
                            ),
                        )
                    },
                    onOpenPlace = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Place),
                                key = name.lowercase(),
                            ),
                        )
                    },
                    onOpenTopic = { name ->
                        navController.navigate(
                            Routes.FacetTimeline(
                                axis = DifferentiatorAggregate.axisRoute(FacetAxis.Topic),
                                key = name.lowercase(),
                            ),
                        )
                    },
                )
            }
            composable<Routes.PhotoWall> {
                PhotoWallDestination(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
            composable<Routes.LocalExtras> {
                LocalExtrasDestination(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate(Routes.Entry(id)) },
                )
            }
        }
    }
}
