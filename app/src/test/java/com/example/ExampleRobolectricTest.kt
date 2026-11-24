package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name and offline permissions match requirements`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Moose Launcher", context.getString(R.string.app_name))

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = packageInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "App must remain 100% offline without INTERNET permission",
            permissions.contains(Manifest.permission.INTERNET)
        )
    }

    @Test
    fun `localized strings adapt to english portuguese and spanish`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()

        val enContext = localizedContext(baseContext, Locale.ENGLISH)
        assertEquals("Desktop", enContext.getString(R.string.tab_desktop))
        assertEquals("New Folder", enContext.getString(R.string.action_new_folder))
        assertEquals("Merge", enContext.getString(R.string.settings_import_merge))

        val ptContext = localizedContext(baseContext, Locale.forLanguageTag("pt"))
        assertEquals("Desktop", ptContext.getString(R.string.tab_desktop))
        assertEquals("Nova Pasta", ptContext.getString(R.string.action_new_folder))
        assertEquals("Mesclar", ptContext.getString(R.string.settings_import_merge))
        assertEquals("Substituir tudo", ptContext.getString(R.string.settings_import_replace))

        val esContext = localizedContext(baseContext, Locale.forLanguageTag("es"))
        assertEquals("Escritorio", esContext.getString(R.string.tab_desktop))
        assertEquals("Nueva Carpeta", esContext.getString(R.string.action_new_folder))
        assertEquals("Combinar", esContext.getString(R.string.settings_import_merge))
        assertEquals("Reemplazar todo", esContext.getString(R.string.settings_import_replace))
    }

    private fun localizedContext(context: Context, locale: Locale): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
