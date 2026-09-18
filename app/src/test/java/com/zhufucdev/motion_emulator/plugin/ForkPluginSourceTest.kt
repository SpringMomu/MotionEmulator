package com.zhufucdev.motion_emulator.plugin

import com.zhufucdev.sdk.ProductQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ForkPluginSourceTest {
    private val url = "https://github.com/SpringMomu/MotionEmulator/releases/download/" +
        "ws-plugin-v1.2.3/motion-emulator-ws-plugin-1.2.3-release.apk"
    private val manifest = """{
        "packageName":"com.zhufucdev.ws_plugin", "versionCode":4, "versionName":"1.2.3",
        "url":"$url", "sha256":"additional manifest fields are allowed"
    }"""

    private fun client(body: String = manifest, status: HttpStatusCode = HttpStatusCode.OK) =
        HttpClient(MockEngine { request ->
            assertEquals(ForkPluginSource.MANIFEST_URL, request.url.toString())
            respond(body, status)
        })

    @Test fun catalogIncludesForkWithoutUpstream() {
        assertEquals(listOf(ForkPluginSource.product), ForkPluginSource.catalog(emptyList()))
        assertEquals(ForkPluginSource.PACKAGE_NAME, ForkPluginSource.product.packageId)
    }

    @Test fun catalogReplacesOnlyTheWebSocketPlugin() {
        val old = ProductQuery("Old WebSocket", "old-ws", listOf(ForkPluginSource.PACKAGE_NAME))
        val other = ProductQuery("Mock Location", "mock", listOf("com.example.mock"))
        assertEquals(listOf(ForkPluginSource.product, other),
            ForkPluginSource.catalog(listOf(old, other, ForkPluginSource.product)))
    }

    @Test fun freshInstallUsesForkReleaseAndSeparateCacheName() = runBlocking {
        client().use {
            val release = ForkPluginSource.check(it)!!
            assertEquals(url, release.url)
            assertEquals("1.2.3", release.versionName)
            assertEquals("MotionEmulator-WebSocket-SpringMomu", release.productName)
        }
    }

    @Test fun oldPluginReceivesTheFixedVersion() = runBlocking {
        client().use { assertNotNull(ForkPluginSource.check(it, 3)) }
    }

    @Test fun installedCurrentVersionHasNoUpdate() = runBlocking {
        client().use { assertNull(ForkPluginSource.check(it, 4)) }
    }

    @Test fun newerInstalledVersionIsNeverDowngraded() = runBlocking {
        client().use { assertNull(ForkPluginSource.check(it, 5)) }
    }

    @Test fun comparisonUsesVersionCodeInsteadOfVersionName() = runBlocking {
        client(manifest.replace("1.2.3", "0.9.0")).use {
            assertNotNull(ForkPluginSource.check(it, 3))
        }
    }

    @Test fun wrongPackageIsRejected() = runBlocking {
        client(manifest.replace("com.zhufucdev.ws_plugin", "com.example.other")).use {
            assertNull(ForkPluginSource.check(it))
        }
    }

    @Test fun preFixVersionIsRejected() = runBlocking {
        client(manifest.replace("\"versionCode\":4", "\"versionCode\":3")).use {
            assertNull(ForkPluginSource.check(it))
        }
    }

    @Test fun upstreamOrNonApkUrlIsRejected() = runBlocking {
        for (body in listOf(manifest.replace("SpringMomu", "zhufucdev"),
            manifest.replace("release.apk", "release.zip"))) {
            client(body).use { assertNull(ForkPluginSource.check(it)) }
        }
    }

    @Test fun missingOrMalformedManifestIsNotAnUpdate() = runBlocking {
        client(status = HttpStatusCode.NotFound).use { assertNull(ForkPluginSource.check(it)) }
        client("not json").use { assertNull(ForkPluginSource.check(it)) }
    }

    @Test fun networkFailureDoesNotFallBackToUpstream() = runBlocking {
        var requests = 0
        HttpClient(MockEngine {
            requests++
            throw IOException("offline")
        }).use { assertNull(ForkPluginSource.check(it)) }
        assertEquals(1, requests)
    }

    @Test fun cancellationIsPropagated() = runBlocking {
        HttpClient(MockEngine { throw CancellationException("cancel check") }).use {
            try {
                ForkPluginSource.check(it)
                fail("Cancellation must propagate")
            } catch (_: CancellationException) {
                // Expected: leaving the page must cancel its request.
            }
        }
    }
}
