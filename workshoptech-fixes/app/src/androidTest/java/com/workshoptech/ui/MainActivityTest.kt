package com.workshoptech.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workshoptech.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI smoke tests for [MainActivity].
 *
 * These tests verify that the activity launches without crashing and that
 * core structural elements are present on screen.  Deep navigation-flow
 * tests belong to their own screen-level test classes.
 *
 * Coverage:
 *  - Activity starts without exception
 *  - At least one visible composable is displayed (app didn't crash)
 *  - BottomNavigation bar is present
 *  - Dashboard tab is the default selected destination
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ── Launch ────────────────────────────────────────────────────────────────

    @Test fun app_launches_without_crashing() {
        // If the activity throws during onCreate / setContent the rule itself fails.
        // Simply asserting the rule is active is sufficient for a launch smoke test.
        composeTestRule.waitForIdle()
    }

    @Test fun root_composable_is_displayed() {
        composeTestRule.onRoot().assertIsDisplayed()
    }

    // ── Bottom navigation ─────────────────────────────────────────────────────

    @Test fun bottom_navigation_bar_is_present() {
        // WorkshopTech uses a BottomNavigation with testTag "bottom_nav"
        // If the tag is not set, fall back to checking for any BottomNavigation
        // by looking for the nav items by content description.
        composeTestRule.waitForIdle()

        // At minimum, the Dashboard tab should be findable
        composeTestRule
            .onNodeWithContentDescription("لوحة التحكم", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test fun dashboard_tab_is_selected_on_launch() {
        composeTestRule.waitForIdle()

        // The Dashboard nav item should be in a "selected" state by default
        composeTestRule
            .onNodeWithContentDescription("لوحة التحكم", substring = true, ignoreCase = true)
            .assertIsSelected()
    }

    // ── Content area (Dashboard screen) ───────────────────────────────────────

    @Test fun dashboard_screen_renders_without_crash() {
        composeTestRule.waitForIdle()

        // There should be at least one text element visible (title, metric, etc.)
        composeTestRule.onAllNodesWithText("", substring = true).onFirst().assertIsDisplayed()
    }

    // ── Navigation to Cases ───────────────────────────────────────────────────

    @Test fun clicking_cases_tab_navigates_to_cases_screen() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("الحالات", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // After clicking Cases tab, the cases list screen should be displayed
        composeTestRule
            .onNodeWithContentDescription("الحالات", substring = true, ignoreCase = true)
            .assertIsSelected()
    }
}
