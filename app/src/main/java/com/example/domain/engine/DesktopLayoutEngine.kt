package com.example.domain.engine

import com.example.domain.model.DesktopFolder
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.SpotlightResults

data class FolderRecord(
    val id: String,
    val name: String,
    val position: Int
)

data class FolderAppMembership(
    val folderId: String,
    val packageName: String,
    val position: Int
)

/**
 * Pure JVM-testable engine for resolving manual dock items, desktop folders,
 * desktop shortcuts, and Spotlight search results.
 */
object DesktopLayoutEngine {

    const val DEFAULT_MAX_DOCK_APPS = 6

    fun resolveManualDock(
        installedApps: List<InstalledAppInfo>,
        pinnedDockPackages: List<String>,
        maxDockApps: Int = DEFAULT_MAX_DOCK_APPS
    ): List<InstalledAppInfo> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        if (appsByPkg.isEmpty()) return emptyList()

        val seen = HashSet<String>()
        val result = mutableListOf<InstalledAppInfo>()
        for (pkg in pinnedDockPackages) {
            if (result.size >= maxDockApps) break
            val app = appsByPkg[pkg] ?: continue
            if (seen.add(pkg)) {
                result += app
            }
        }
        return result
    }

    fun resolveDesktopShortcuts(
        installedApps: List<InstalledAppInfo>,
        pinnedHomePackages: List<String>
    ): List<InstalledAppInfo> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        val seen = HashSet<String>()
        return pinnedHomePackages.mapNotNull { pkg ->
            val app = appsByPkg[pkg] ?: return@mapNotNull null
            if (seen.add(pkg)) app else null
        }
    }

    fun resolveFolders(
        folders: List<FolderRecord>,
        memberships: List<FolderAppMembership>,
        installedApps: List<InstalledAppInfo>
    ): List<DesktopFolder> {
        val appsByPkg = installedApps.associateBy { it.packageName }
        val appsByFolder = memberships
            .sortedBy { it.position }
            .groupBy { it.folderId }

        return folders
            .sortedBy { it.position }
            .map { folder ->
                val seen = HashSet<String>()
                val folderApps = appsByFolder[folder.id].orEmpty().mapNotNull { member ->
                    val app = appsByPkg[member.packageName] ?: return@mapNotNull null
                    if (seen.add(member.packageName)) app else null
                }
                DesktopFolder(
                    id = folder.id,
                    name = folder.name,
                    position = folder.position,
                    apps = folderApps
                )
            }
    }

    fun searchSpotlight(
        query: String,
        folders: List<DesktopFolder>,
        installedApps: List<InstalledAppInfo>,
        maxApps: Int = 18
    ): SpotlightResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            return SpotlightResults(
                folders = folders.take(4),
                apps = installedApps.take(12)
            )
        }
        val matchedFolders = folders.filter {
            it.name.lowercase().contains(q) ||
                it.apps.any { app -> app.label.lowercase().contains(q) }
        }
        val matchedApps = installedApps.filter {
            it.label.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q)
        }.take(maxApps)

        return SpotlightResults(
            folders = matchedFolders,
            apps = matchedApps
        )
    }
}
