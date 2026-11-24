package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MooseLauncherUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `desktop renders widgets folders manual dock and library without navigation bar or rail`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launcher_menu_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_widgets_section").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_clock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_apps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()

        // Create a folder from the desktop menu bar and verify dialog flow
        composeRule.onNodeWithTag("menu_new_folder_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_folder_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_name_input").performTextInput("Work")
        composeRule.onNodeWithTag("folder_create_confirm_button").performClick()
        composeRule.waitForIdle()

        // Navigate to App Library via Manual Floating Dock Apps button
        composeRule.onNodeWithTag("dock_apps_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subscreen_top_bar_title")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.tab_library))
        composeRule.onNodeWithTag("library_category_all").assertIsDisplayed()
        composeRule.onNodeWithTag("library_category_productivity").assertIsDisplayed()
        composeRule.onNodeWithTag("subscreen_back_button").performClick()
        composeRule.waitForIdle()

        // Open Settings from menu bar and verify Widgets & JSON Backup sections
        composeRule.onNodeWithTag("menu_settings_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subscreen_top_bar_title")
            .assertTextEquals(context.getString(R.string.tab_settings))
        composeRule.onNodeWithTag("settings_widgets_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_export_backup_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_import_backup_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp")
    fun `tablet desktop renders two pane launcher without navigation rail`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("launcher_menu_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_widgets_section").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_items_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()
    }
}
