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
    fun `minimal desktop removes dock search button opens context sheet with wallpaper and notifications and navigates to settings and library`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_clock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_apps_button").assertIsDisplayed()

        // Search button must NOT exist in the Dock
        composeRule.onNodeWithTag("dock_spotlight_button").assertDoesNotExist()

        // No unnecessary menus or navigation bars on Home screen
        composeRule.onNodeWithTag("launcher_menu_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("home_spotlight_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("widget_system_status_card").assertDoesNotExist()
        composeRule.onNodeWithTag("widget_quick_notes_card").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()

        // Long-press empty desktop opens contextual sheet -> verify wallpaper, notifications, widgets, and create folder
        composeRule.onNodeWithTag("desktop_surface").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_context_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_menu_wallpaper").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_menu_notifications").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_menu_widgets").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_menu_new_folder").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_folder_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_name_input").performTextInput("Work")
        composeRule.onNodeWithTag("folder_create_confirm_button").performClick()
        composeRule.waitForIdle()

        // Open Widgets dialog from desktop context sheet -> verify Add App Widget button
        composeRule.onNodeWithTag("desktop_surface").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_menu_widgets").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manage_widgets_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("widgets_add_external_button").assertIsDisplayed()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // Open Settings from desktop context sheet
        composeRule.onNodeWithTag("desktop_surface").performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_menu_settings").performClick()
        composeRule.waitForIdle()

        // Verify Settings screen (gallery wallpaper, external widgets, notification dots, JSON backup)
        composeRule.onNodeWithTag("subscreen_top_bar_title")
            .assertTextEquals(context.getString(R.string.tab_settings))
        composeRule.onNodeWithTag("wallpaper_option_custom_photo")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_pick_gallery_wallpaper_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_widgets_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_add_external_widget_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notifications_button")
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
    fun `tablet desktop renders minimalist full screen launcher without search in dock`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("desktop_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("widget_clock_card").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_items_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_floating_dock").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_apps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dock_spotlight_button").assertDoesNotExist()
        composeRule.onNodeWithTag("launcher_menu_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("navigation_rail").assertDoesNotExist()
    }
}
