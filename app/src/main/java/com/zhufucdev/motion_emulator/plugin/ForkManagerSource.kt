package com.zhufucdev.motion_emulator.plugin

import com.zhufucdev.sdk.ReleaseAsset
import io.ktor.client.HttpClient

object ForkManagerSource {
    const val MANIFEST_URL = "https://raw.githubusercontent.com/SpringMomu/MotionEmulator/main/release.json"
    suspend fun check(client: HttpClient, installedVersionCode: Long): ReleaseAsset? =
        ForkPluginSource.checkManifest(client, MANIFEST_URL, "com.zhufucdev.motion_emulator",
            26, "MotionEmulator-SpringMomu", installedVersionCode)
}
