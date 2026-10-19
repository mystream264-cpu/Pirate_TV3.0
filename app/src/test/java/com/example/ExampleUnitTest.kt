package com.example

import com.example.data.util.maskPasswordInUrl
import com.example.data.util.normalizeServerUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNormalizeServerUrl_addsHttpIfMissing() {
        val result = normalizeServerUrl("sdmx.vip:80")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testNormalizeServerUrl_removesTrailingSlash() {
        val result = normalizeServerUrl("http://sdmx.vip:80/")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testNormalizeServerUrl_removesPlayerApiPhp() {
        val result = normalizeServerUrl("http://sdmx.vip:80/player_api.php")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testNormalizeServerUrl_removesPlayerApiPhpWithQueryParams() {
        val result = normalizeServerUrl("http://sdmx.vip:80/player_api.php?username=U&password=P")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testNormalizeServerUrl_removesMagPortalSuffix() {
        val result = normalizeServerUrl("http://sdmx.vip:80/c/")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testNormalizeServerUrl_removesInternalAndBorderSpaces() {
        val result = normalizeServerUrl("  http:// sdmx.vip:80 /  ")
        assertEquals("http://sdmx.vip:80", result)
    }

    @Test
    fun testMaskPasswordInUrl_masksPasswordQueryParam() {
        val raw = "http://sdmx.vip:80/player_api.php?username=myuser&password=supersecret&action=get_live"
        val masked = maskPasswordInUrl(raw)
        assertEquals("http://sdmx.vip:80/player_api.php?username=myuser&password=***&action=get_live", masked)
    }

    @Test
    fun testMaskPasswordInUrl_masksStreamPathPassword() {
        val raw = "http://sdmx.vip:80/live/myuser/secretpass/12345.ts"
        val masked = maskPasswordInUrl(raw)
        assertEquals("http://sdmx.vip:80/live/myuser/***/12345.ts", masked)
    }
}
