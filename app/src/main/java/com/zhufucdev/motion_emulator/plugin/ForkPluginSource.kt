package com.zhufucdev.motion_emulator.plugin

import com.zhufucdev.sdk.ProductQuery
import com.zhufucdev.sdk.ReleaseAsset
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The bundled WebSocket plugin has its own release stream in this fork. */
object ForkPluginSource {
    const val PACKAGE_NAME = "com.zhufucdev.ws_plugin"
    const val PRODUCT_KEY = "springmomu-ws-plugin"
    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/SpringMomu/MotionEmulator/main/ws-plugin/release.json"
    private const val RELEASE_URL_PREFIX =
        "https://github.com/SpringMomu/MotionEmulator/releases/download/"
    private val json = Json { ignoreUnknownKeys = true }

    val product = ProductQuery(
        name = "Motion Emulator WebSocket Plugin (SpringMomu)",
        key = PRODUCT_KEY,
        category = listOf("me", "plugin", PACKAGE_NAME)
    )

    fun catalog(upstream: List<ProductQuery>): List<ProductQuery> =
        listOf(product) + upstream.filter {
            it.packageId != PACKAGE_NAME && it.key != PRODUCT_KEY
        }

    suspend fun check(client: HttpClient, installedVersionCode: Long? = null): ReleaseAsset? {
        return try {
            val response = client.get(MANIFEST_URL)
            if (!response.status.isSuccess()) return null
            val release = json.decodeFromString<PluginRelease>(response.bodyAsText())
            if (release.packageName != PACKAGE_NAME || release.versionCode < 4 ||
                release.versionName.isBlank() ||
                !release.url.startsWith(RELEASE_URL_PREFIX) || !release.url.endsWith(".apk")
            ) return null
            if (installedVersionCode != null && release.versionCode <= installedVersionCode) {
                return null
            }
            // Keep a fork-specific cache name so an upstream APK cannot be reused.
            ReleaseAsset(release.versionName, "MotionEmulator-WebSocket-SpringMomu", release.url)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // An unavailable fork must never fall back to the old upstream plugin.
            null
        }
    }
}

@Serializable
private data class PluginRelease(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val url: String
)
