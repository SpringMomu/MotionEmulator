package com.zhufucdev.me.xposed

import org.junit.Assert.*
import org.junit.Test

class AMapClientOptionsTest {
    class Option(var offset: Boolean) { fun isOffset(): Boolean = offset }

    @Test fun defaultMatchesAMapSdk() {
        assertTrue(AMapClientOptions().offset(Any()))
    }

    @Test fun differentClientsKeepTheirOwnCoordinateSystems() {
        val options = AMapClientOptions()
        val first = Any()
        val second = Any()
        options.set(first, Option(false))
        options.set(second, Option(true))
        assertFalse(options.offset(first))
        assertTrue(options.offset(second))
    }

    @Test fun updatedOptionChangesFutureCallbacks() {
        val options = AMapClientOptions()
        val client = Any()
        options.set(client, Option(false))
        options.set(client, Option(true))
        assertTrue(options.offset(client))
    }

    @Test fun laterMutationRequiresResubmittingTheOption() {
        val options = AMapClientOptions()
        val client = Any()
        val option = Option(false)
        options.set(client, option)
        option.offset = true
        assertFalse(options.offset(client))
        options.set(client, option)
        assertTrue(options.offset(client))
    }

    @Test fun nullOptionDoesNotReplaceExistingSetting() {
        val options = AMapClientOptions()
        val client = Any()
        options.set(client, Option(false))
        options.set(client, null)
        assertFalse(options.offset(client))
    }
}
