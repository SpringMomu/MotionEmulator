package com.zhufucdev.motion_emulator.plugin

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ForkManagerSourceTest {
    private val url = "https://github.com/SpringMomu/MotionEmulator/releases/download/motionemulator-v1.2.4/motion-emulator-1.2.4-release.apk"
    private val manifest = """{"packageName":"com.zhufucdev.motion_emulator","versionCode":26,"versionName":"1.2.4","url":"$url"}"""
    private fun client(body: String = manifest) = HttpClient(MockEngine { request ->
        assertEquals(ForkManagerSource.MANIFEST_URL, request.url.toString())
        respond(body)
    })
    @Test fun stableManagerGetsForkRelease() = runBlocking {
        client().use { assertEquals(url, ForkManagerSource.check(it,24)!!.url) }
    }
    @Test fun currentAndNewerManagersAreNotDowngraded() = runBlocking {
        client().use { assertNull(ForkManagerSource.check(it,26)); assertNull(ForkManagerSource.check(it,27)) }
    }
    @Test fun pluginManifestCannotReplaceManager() = runBlocking {
        client(manifest.replace("com.zhufucdev.motion_emulator","com.zhufucdev.ws_plugin")).use {
            assertNull(ForkManagerSource.check(it,24))
        }
    }
    @Test fun untrustedDownloadIsRejected() = runBlocking {
        client(manifest.replace(url,"https://example.com/manager.apk")).use {
            assertNull(ForkManagerSource.check(it,24))
        }
    }
}
