package com.zhufucdev.motion_emulator

import android.app.Application
import androidx.core.content.edit
import com.zhufucdev.motion_emulator.extension.sharedPreferences
import com.google.android.material.color.DynamicColors
import com.zhufucdev.motion_emulator.plugin.Plugins

class MeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        val prefs = sharedPreferences()
        val missingAmap = BuildConfig.AMAP_SDK_KEY == "UNCONFIGURED"
        val missingGoogle = BuildConfig.GCP_MAPS_KEY == "UNCONFIGURED"
        prefs.edit {
            for (key in listOf("map_provider", "poi_provider")) {
                val provider = prefs.getString(key, null)
                if (!missingAmap && !prefs.getBoolean("amap_default_applied", false)) {
                    putString(key, "amap")
                } else if (provider == null || (provider == "amap" && missingAmap) ||
                    (provider == "gcp_maps" && missingGoogle)) putString(key, if (missingAmap) "osm" else "amap")
            }
            if (!missingAmap) putBoolean("amap_default_applied", true)
        }
        Plugins.init(this)
    }
}