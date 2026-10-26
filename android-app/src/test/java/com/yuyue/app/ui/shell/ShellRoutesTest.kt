package com.yuyue.app.ui.shell

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Instrumentation-free route parsing tests for typed Navigation Compose routes.
 */
class ShellRoutesTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun entryRoute_roundTrips() {
        val route = Routes.Entry(id = "entry-abc-123")
        val encoded = json.encodeToString(Routes.Entry.serializer(), route)
        val decoded = json.decodeFromString(Routes.Entry.serializer(), encoded)
        assertEquals(route, decoded)
        assertTrue(encoded.contains("entry-abc-123"))
    }

    @Test
    fun entryEditRoute_roundTrips() {
        val route = Routes.EntryEdit(id = "e1")
        val decoded = json.decodeFromString(
            Routes.EntryEdit.serializer(),
            json.encodeToString(Routes.EntryEdit.serializer(), route),
        )
        assertEquals("e1", decoded.id)
    }

    @Test
    fun dayRoute_usesEpochDay() {
        val route = Routes.Day(epochDay = 20_000L)
        val decoded = json.decodeFromString(
            Routes.Day.serializer(),
            json.encodeToString(Routes.Day.serializer(), route),
        )
        assertEquals(20_000L, decoded.epochDay)
    }

    @Test
    fun workspaceRoutes_roundTrip() {
        val detail = Routes.WorkspaceDetail(notebookId = "nb-1")
        val entries = Routes.WorkspaceEntries(notebookId = "nb-1")
        assertEquals(
            detail,
            json.decodeFromString(
                Routes.WorkspaceDetail.serializer(),
                json.encodeToString(Routes.WorkspaceDetail.serializer(), detail),
            ),
        )
        assertEquals(
            entries,
            json.decodeFromString(
                Routes.WorkspaceEntries.serializer(),
                json.encodeToString(Routes.WorkspaceEntries.serializer(), entries),
            ),
        )
    }

    @Test
    fun tabSingletons_areDistinctTypes() {
        // Compile-time typed destinations; runtime identity for nested graph start.
        assertEquals(Routes.Home, Routes.Home)
        assertEquals(Routes.Time, Routes.Time)
        assertEquals(Routes.Recall, Routes.Recall)
        assertEquals(Routes.Vault, Routes.Vault)
        assertEquals(Routes.Shell, Routes.Shell)
        assertEquals(Routes.Onboarding, Routes.Onboarding)
        assertEquals(Routes.Trash, Routes.Trash)
    }

    @Test
    fun shellTabRoutes_coversFourTabs() {
        assertEquals(4, ShellTabRoutes.size)
        assertTrue(ShellTabRoutes.contains(Routes.Home))
        assertTrue(ShellTabRoutes.contains(Routes.Time))
        assertTrue(ShellTabRoutes.contains(Routes.Recall))
        assertTrue(ShellTabRoutes.contains(Routes.Vault))
        assertTrue(Routes.Onboarding !in ShellTabRoutes)
        assertTrue(Routes.Trash !in ShellTabRoutes)
    }

    @Test
    fun entryDetailState_loadingIsDistinctFromMissing() {
        assertTrue(EntryDetailState.Loading != EntryDetailState.Missing)
    }

    @Test
    fun differentiatorRoutes_roundTrip() {
        val browse = Routes.FacetBrowse(axis = "person")
        val timeline = Routes.FacetTimeline(axis = "place", key = "cafe")
        assertEquals(
            browse,
            json.decodeFromString(
                Routes.FacetBrowse.serializer(),
                json.encodeToString(Routes.FacetBrowse.serializer(), browse),
            ),
        )
        assertEquals(
            timeline,
            json.decodeFromString(
                Routes.FacetTimeline.serializer(),
                json.encodeToString(Routes.FacetTimeline.serializer(), timeline),
            ),
        )
        assertEquals(Routes.WeekReview, Routes.WeekReview)
        assertEquals(Routes.MonthReview, Routes.MonthReview)
        assertEquals(Routes.PhotoWall, Routes.PhotoWall)
        assertEquals(Routes.LocalExtras, Routes.LocalExtras)
        assertTrue(Routes.PhotoWall !in ShellTabRoutes)
    }
}
