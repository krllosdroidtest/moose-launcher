package com.example

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.ui.LauncherViewModel
import com.example.ui.MooseLauncherApp

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application)
    }

    private var homePressTrigger by mutableIntStateOf(0)

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.reloadInstalledApps(forceRefresh = true)
        }
    }

    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingWidgetId
        ) ?: pendingWidgetId

        if (result.resultCode == RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info == null) {
                deleteWidgetIdSafely(widgetId)
            } else if (info.configure != null) {
                pendingWidgetId = widgetId
                val launchedConfigure = runCatching {
                    appWidgetHost.startAppWidgetConfigureActivityForResult(
                        this,
                        widgetId,
                        0,
                        REQUEST_CONFIGURE_APPWIDGET,
                        null
                    )
                    true
                }.getOrDefault(false)
                if (!launchedConfigure) {
                    persistExternalWidget(widgetId, info)
                }
            } else {
                persistExternalWidget(widgetId, info)
            }
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            deleteWidgetIdSafely(widgetId)
        }
        if (pendingWidgetId == widgetId && result.resultCode != RESULT_OK) {
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        appWidgetHost = AppWidgetHost(applicationContext, APPWIDGET_HOST_ID)

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                this,
                packageChangeReceiver,
                pkgFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        setContent {
            MooseLauncherApp(
                viewModel = viewModel,
                homePressTrigger = homePressTrigger,
                appWidgetHost = appWidgetHost,
                appWidgetManager = appWidgetManager,
                onRequestAddExternalWidget = ::launchExternalWidgetPicker,
                onRemoveExternalWidgetHostId = ::removeExternalWidget
            )
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(packageChangeReceiver) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressTrigger++
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeLightweight()
    }

    @Deprecated("Required for AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_APPWIDGET) {
            val widgetId = data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                pendingWidgetId
            ) ?: pendingWidgetId
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (resultCode == RESULT_OK) {
                    val info = appWidgetManager.getAppWidgetInfo(widgetId)
                    if (info != null) {
                        persistExternalWidget(widgetId, info)
                    } else {
                        deleteWidgetIdSafely(widgetId)
                    }
                } else {
                    deleteWidgetIdSafely(widgetId)
                }
            }
        }
    }

    private fun launchExternalWidgetPicker() {
        val allocatedId = runCatching { appWidgetHost.allocateAppWidgetId() }
            .getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        if (allocatedId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        pendingWidgetId = allocatedId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, allocatedId)
        }
        runCatching { widgetPickLauncher.launch(pickIntent) }
            .onFailure {
                deleteWidgetIdSafely(allocatedId)
                pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
    }

    private fun persistExternalWidget(widgetId: Int, info: AppWidgetProviderInfo) {
        val label = runCatching { info.loadLabel(packageManager).toString().trim() }
            .getOrNull()
            .orEmpty()
            .ifBlank { getString(R.string.widget_external_fallback) }
        val minHeightDp = info.minHeight.coerceIn(140, 280)
        viewModel.addExternalWidget(
            appWidgetId = widgetId,
            providerPackage = info.provider.packageName,
            providerLabel = label,
            spanColumns = 4,
            heightDp = minHeightDp
        )
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun removeExternalWidget(appWidgetId: Int) {
        viewModel.removeExternalWidget(appWidgetId)
        deleteWidgetIdSafely(appWidgetId)
    }

    private fun deleteWidgetIdSafely(widgetId: Int) {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
        }
    }

    companion object {
        private const val APPWIDGET_HOST_ID = 1042
        private const val REQUEST_CONFIGURE_APPWIDGET = 2042
    }
}
