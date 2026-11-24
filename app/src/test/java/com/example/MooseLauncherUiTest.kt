package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
    fun `minimal desktop has no home menu bar opens context sheet on long press and navigates via macos dock`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_clock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_apps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_spotlight_button").assertIsDisplayed()

        // No unnecessary menus or navigation bars on Home screen
        composeRule.onNodeWithTag("launcher_menu_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("home_spotlight_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("widget_system_status_card").assertDoesNotExist()
        composeRule.onNodeWithTag("widget_quick_notes_card").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()

        // Long-press empty desktop opens contextual sheet -> create folder
        composeRule.onNodeWithTag("desktop_surface").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_context_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_menu_new_folder").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_folder_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_name_input").performTextInput("Work")
        composeRule.onNodeWithTag("folder_create_confirm_button").performClick()
        composeRule.waitForIdle()

        // Open Spotlight from macOS Dock
        composeRule.onNodeWithTag("dock_spotlight_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("spotlight_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("spotlight_action_settings").performClick()
        composeRule.waitForIdle()

        // Verify Settings screen (widgets & JSON backup)
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
        composeRule.onNodeWithTag("subscreen_back_button").performClick()
        composeRule.waitForIdle()

        // Open App Library via Launchpad button in macOS Dock
        composeRule.onNodeWithTag("dock_apps_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subscreen_top_bar_title")
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.tab_library))
        composeRule.onNodeWithTag("library_category_all").assertIsDisplayed()
        composeRule.onNodeWithTag("library_category_productivity").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp")
    fun `tablet desktop renders minimalist full screen launcher without menu bar or rail`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_clock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_items_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_apps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_spotlight_button").assertIsDisplayed()
        composeRule.onNodeWithTag("launcher_menu_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()
    }
}
