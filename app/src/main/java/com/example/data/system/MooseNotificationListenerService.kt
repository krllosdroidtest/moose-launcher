package com.example.data.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only NotificationListenerService that tracks which package names
 * currently have active non-ongoing notifications so the launcher can render
 * minimalist Material 3 notification dots without storing or inspecting content.
 */
class MooseNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        syncAllActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        syncAllActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        syncAllActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _activeNotificationPackages.value = emptySet()
    }

    private fun syncAllActiveNotifications() {
        val current = runCatching { activeNotifications }.getOrNull() ?: return
        val selfPkg = packageName
        val packages = current
            .asSequence()
            .filter { !it.isOngoing && it.packageName != selfPkg }
            .map { it.packageName }
            .toSet()
        _activeNotificationPackages.value = packages
    }

    companion object {
        private val _activeNotificationPackages = MutableStateFlow<Set<String>>(emptySet())
        val activeNotificationPackages: StateFlow<Set<String>> = _activeNotificationPackages.asStateFlow()

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (context.packageName in enabledPackages) return true

            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val expected = ComponentName(context, MooseNotificationListenerService::class.java).flattenToString()
            return flat.split(":").any { it.equals(expected, ignoreCase = true) }
        }

        fun openNotificationAccessSettings(context: Context): Boolean {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
    }
}
