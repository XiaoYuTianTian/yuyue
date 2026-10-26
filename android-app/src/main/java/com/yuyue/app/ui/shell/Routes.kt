package com.yuyue.app.ui.shell

import kotlinx.serialization.Serializable

/**
 * Typed Navigation Compose routes for the rebuild shell (plan §5).
 *
 * Capture is intentionally not a route — it is a global overlay owned by [ShellScaffold].
 */
object Routes {
    @Serializable
    data object Onboarding

    /** Nested graph for bottom tabs. */
    @Serializable
    data object Shell

    @Serializable
    data object Home

    @Serializable
    data object Time

    @Serializable
    data object Recall

    @Serializable
    data object Vault

    @Serializable
    data class Entry(val id: String)

    @Serializable
    data class EntryEdit(val id: String)

    /** Day timeline; [epochDay] is [java.time.LocalDate.toEpochDay]. */
    @Serializable
    data class Day(val epochDay: Long)

    @Serializable
    data object Trash

    @Serializable
    data class WorkspaceDetail(val notebookId: String)

    @Serializable
    data class WorkspaceEntries(val notebookId: String)

    /** Facet index: person | place | topic. */
    @Serializable
    data class FacetBrowse(val axis: String)

    /** Facet timeline for one key (normalized lowercase label). */
    @Serializable
    data class FacetTimeline(val axis: String, val key: String)

    @Serializable
    data object WeekReview

    @Serializable
    data object MonthReview

    @Serializable
    data object PhotoWall

    /** Map / weather / raw-audio local stubs — no network APIs. */
    @Serializable
    data object LocalExtras
}

/** Tab destinations shown in the shell bottom bar (Capture is overlay, not a tab). */
val ShellTabRoutes: Set<Any> = setOf(
    Routes.Home,
    Routes.Time,
    Routes.Recall,
    Routes.Vault,
)
