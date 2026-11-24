package com.example.data.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.example.domain.model.AppCategory
import com.example.domain.model.DeviceStatusSnapshot
import com.example.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface InstalledAppsProvider {
    suspend fun loadInstalledApps(forceRefresh: Boolean = false): List<InstalledAppInfo>
    fun getCachedInstalledApps(): List<InstalledAppInfo>
    fun getAppIconBitmap(packageName: String): Bitmap?
    fun getAppIconImageBitmap(packageName: String): ImageBitmap?
    suspend fun readDeviceStatus(): DeviceStatusSnapshot
    fun isDefaultHomeLauncher(): Boolean
    fun isNotificationAccessEnabled(): Boolean
    fun launchApp(packageName: String): Boolean
    fun openDefaultHomeSettings(): Boolean
    fun openNotificationAccessSettings(): Boolean
    fun openAppDetailsSettings(packageName: String): Boolean
}

class AndroidInstalledAppsProvider(
    private val context: Context
) : InstalledAppsProvider {

    private val iconCache = ConcurrentHashMap<String, Bitmap>()
    private val imageBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

    @Volatile
    private var cachedApps: List<InstalledAppInfo> = emptyList()

    override suspend fun loadInstalledApps(forceRefresh: Boolean): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            if (!forceRefresh && cachedApps.isNotEmpty()) {
                return@withContext cachedApps
            }

            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            val selfPackage = context.packageName
            val seenPackages = HashSet<String>()
            val result = mutableListOf<InstalledAppInfo>()

            for (info in resolveInfos) {
                val activityInfo = info.activityInfo ?: continue
                val packageName = activityInfo.packageName ?: continue
                if (packageName == selfPackage || !seenPackages.add(packageName)) continue

                val label = info.loadLabel(pm).toString().trim()
                    .ifEmpty { packageName }
                val appInfo = activityInfo.applicationInfo
                val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = resolveCategory(appInfo, packageName, isSystem)

                if (!imageBitmapCache.containsKey(packageName)) {
                    runCatching { info.loadIcon(pm) }.getOrNull()?.let { drawable ->
                        val bmp = drawableToBitmap(drawable)
                        iconCache[packageName] = bmp
                        imageBitmapCache[packageName] = bmp.asImageBitmap()
                    }
                }

                result += InstalledAppInfo(
                    packageName = packageName,
                    label = label,
                    category = category,
                    isSystemApp = isSystem
                )
            }

            val sorted = result.sortedBy { it.label.lowercase() }
            cachedApps = sorted
            sorted
        }

    override fun getCachedInstalledApps(): List<InstalledAppInfo> = cachedApps

    override fun getAppIconBitmap(packageName: String): Bitmap? = iconCache[packageName]

    override fun getAppIconImageBitmap(packageName: String): ImageBitmap? = imageBitmapCache[packageName]

    override suspend fun readDeviceStatus(): DeviceStatusSnapshot = withContext(Dispatchers.IO) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val freeGb = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
        }.getOrDefault(0.0)

        DeviceStatusSnapshot(
            batteryPercent = percent,
            isCharging = isCharging,
            freeStorageGb = freeGb
        )
    }

    override fun isDefaultHomeLauncher(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    override fun isNotificationAccessEnabled(): Boolean =
        MooseNotificationListenerService.isNotificationAccessEnabled(context)

    override fun launchApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: return false
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    override fun openDefaultHomeSettings(): Boolean {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    override fun openNotificationAccessSettings(): Boolean =
        MooseNotificationListenerService.openNotificationAccessSettings(context)

    override fun openAppDetailsSettings(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun resolveCategory(
        appInfo: ApplicationInfo?,
        packageName: String,
        isSystem: Boolean
    ): AppCategory {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appInfo != null) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE,
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL,
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> return AppCategory.UTILITIES
            }
        }
        val pkg = packageName.lowercase()
        return when {
            "mail" in pkg || "calendar" in pkg || "docs" in pkg || "notes" in pkg || "office" in pkg ->
                AppCategory.PRODUCTIVITY
            "music" in pkg || "video" in pkg || "player" in pkg || "spotify" in pkg || "youtube" in pkg || "camera" in pkg || "gallery" in pkg ->
                AppCategory.MEDIA
            "chat" in pkg || "messag" in pkg || "dialer" in pkg || "contacts" in pkg || "whatsapp" in pkg || "telegram" in pkg ->
                AppCategory.COMMUNICATION
            isSystem -> AppCategory.SYSTEM
            else -> AppCategory.UTILITIES
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val size = 128
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
