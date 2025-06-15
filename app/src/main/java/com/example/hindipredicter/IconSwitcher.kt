package com.example.hindipredicter

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

// This class encapsulates the logic for switching app icons.
class IconSwitcher(private val context: Context) {

    val iconAliases = listOf(
        ".IconAlias_Default",
        ".IconAlias_a",
        ".IconAlias_ba",
        ".IconAlias_ch",
        ".IconAlias_da",
        ".IconAlias_ga",
        ".IconAlias_gha",
        ".IconAlias_ha",
        ".IconAlias_ka",
        ".IconAlias_kha",
        ".IconAlias_la",
        ".IconAlias_ma",
        ".IconAlias_mna",
        ".IconAlias_na",
        ".IconAlias_pa",
        ".IconAlias_ra",
        ".IconAlias_ri",
        ".IconAlias_sa",
        ".IconAlias_ta",
        ".IconAlias_ya"
    )

    private val PREFS_NAME = "IconSwitcherPrefs"
    private val CURRENT_ICON_INDEX_KEY = "current_icon_index"
    private val CURRENT_ICON_ALIAS_NAME_KEY = "current_icon_alias_name"

    // Disables the currently active icon alias and enables the next one in the sequence.
    fun switchIcon() {
        val packageManager = context.packageManager
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Get the current icon's alias name. Default to the first alias if not found.
        val currentActiveAliasName = sharedPrefs.getString(CURRENT_ICON_ALIAS_NAME_KEY, iconAliases[0])
        // Get the current index for calculation.
        var currentIndex = sharedPrefs.getInt(CURRENT_ICON_INDEX_KEY, 0)

        Log.d("IconSwitcher", "Retrieved - Current Index: $currentIndex, Current Active Alias Name: $currentActiveAliasName")

        // Disable the currently active component (alias)
        // The ComponentName constructor correctly handles package + relative class name.
        val currentActiveComponentName = ComponentName(context.packageName, context.packageName + currentActiveAliasName)
        disableComponent(packageManager, currentActiveComponentName)

        // Calculate the next icon index and its alias name
        val nextIndex = (currentIndex + 1) % iconAliases.size
        val nextIconAliasName = iconAliases[nextIndex]
        val nextIconComponentName = ComponentName(context.packageName, context.packageName + nextIconAliasName)

        // Enable the next icon alias
        enableComponent(packageManager, nextIconComponentName)
        Log.d("IconSwitcher", "Enabled: ${nextIconComponentName.className}")

        // Save the next index and alias name for the next update
        sharedPrefs.edit().putInt(CURRENT_ICON_INDEX_KEY, nextIndex).apply()
        sharedPrefs.edit().putString(CURRENT_ICON_ALIAS_NAME_KEY, nextIconAliasName).apply()
        Log.d("IconSwitcher", "Saved - Next Index: $nextIndex, Next Alias: $nextIconAliasName")
    }

    // Helper function to enable a component
    private fun enableComponent(packageManager: PackageManager, componentName: ComponentName) {
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("IconSwitcher", "Enabled: ${componentName.className}")
        }
    }

    // Helper function to disable a component
    private fun disableComponent(packageManager: PackageManager, componentName: ComponentName) {
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("IconSwitcher", "Disabled: ${componentName.className}")
        }
    }
}