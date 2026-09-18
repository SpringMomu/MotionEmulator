package com.zhufucdev.motion_emulator.extension

import android.content.Context
import com.zhufucdev.motion_emulator.BuildConfig
import java.io.File
import androidx.core.content.pm.PackageInfoCompat
import com.zhufucdev.motion_emulator.plugin.ForkManagerSource

fun Updater(product: String, context: Context) = com.zhufucdev.update.Updater(
    BuildConfig.server_uri,
    product,
    context,
    File(context.externalCacheDir, "update"),
    releaseResolver = if (product == BuildConfig.product) ({ client ->
        ForkManagerSource.check(client, PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)))
    }) else null
)

fun Updater(context: Context) = Updater(BuildConfig.product, context)
