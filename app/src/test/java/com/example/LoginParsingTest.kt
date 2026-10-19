package com.example

import com.example.data.util.LoginParseResult
import com.example.data.util.asBoolean
import com.example.data.util.asInt
import com.example.data.util.asLong
import com.example.data.util.asString
import com.example.data.util.parseAuthResponse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginParsingTest {

    // JSON exacto devuelto por el servidor XUI.one especificado en la incidencia
    private val exactServerJson = """
        {"user_info":{"username":"Sandybell69","password":"Sandybell69","message":"Welcome to XUI.one","auth":1,"status":"Active","exp_date":"1792531606","is_trial":"1","active_cons":0,"created_at":"1792445206","max_connections":"2","allowed_output_formats":["m3u8","ts","rtmp"]},"server_info":{"xui":true,"version":"1.5.13","url":"sdmx.vip","port":"80","https_port":"443","server_protocol":"http","rtmp_port":"1755","timestamp_now":1792448168,"time_now":"2026-10-19 22:16:08","timezone":"UTC"}}
    """.trimIndent()

    // 1792448168 corresponde a 2026-10-19 22:16:08 (timestamp_now de la respuesta)
    private val serverCurrentTimestamp = 1792448168L

    @Test
    fun testExactServerResponse_isValidLogin() {
        val result = parseAuthResponse(exactServerJson, currentTimeSeconds = serverCurrentTimestamp)
        
        assertTrue("El login debe ser válido para el JSON exacto del servidor", result is LoginParseResult.Success)
        val success = result as LoginParseResult.Success
        val info = success.userInfo

        assertEquals("Sandybell69", info.username)
        assertEquals("Sandybell69", info.password)
        assertEquals(1, info.auth)
        assertEquals("Active", info.status)
        assertEquals("1792531606", info.expDate)
        assertEquals("0", info.activeCons)
        assertEquals("2", info.maxConnections)
        assertEquals("1", info.isTrial)
        assertTrue("isAuthed debe ser true", info.isAuthed)
    }

    @Test
    fun testAuthVariantWithStringOne_isValidLogin() {
        val jsonStringAuth = exactServerJson.replace("\"auth\":1", "\"auth\":\"1\"")
        val result = parseAuthResponse(jsonStringAuth, currentTimeSeconds = serverCurrentTimestamp)

        assertTrue("El login debe ser válido cuando auth es \"1\" (cadena)", result is LoginParseResult.Success)
        val info = (result as LoginParseResult.Success).userInfo
        assertEquals("1", info.auth)
        assertTrue(info.isAuthed)
    }

    @Test
    fun testAuthVariantWithBooleanTrue_isValidLogin() {
        val jsonBooleanAuth = exactServerJson.replace("\"auth\":1", "\"auth\":true")
        val result = parseAuthResponse(jsonBooleanAuth, currentTimeSeconds = serverCurrentTimestamp)

        assertTrue("El login debe ser válido cuando auth es true (booleano)", result is LoginParseResult.Success)
        val info = (result as LoginParseResult.Success).userInfo
        assertEquals(true, info.auth)
        assertTrue(info.isAuthed)
    }

    @Test
    fun testAuthVariantWithZero_isInvalidCredentials() {
        // Variante con auth: 0
        val jsonZero = exactServerJson.replace("\"auth\":1", "\"auth\":0")
        val resultZero = parseAuthResponse(jsonZero, currentTimeSeconds = serverCurrentTimestamp)
        assertTrue("auth: 0 debe resultar en InvalidCredentials", resultZero is LoginParseResult.InvalidCredentials)
        assertEquals("Usuario o contraseña incorrectos.", (resultZero as LoginParseResult.InvalidCredentials).message)

        // Variante con auth: "0"
        val jsonStringZero = exactServerJson.replace("\"auth\":1", "\"auth\":\"0\"")
        val resultStringZero = parseAuthResponse(jsonStringZero, currentTimeSeconds = serverCurrentTimestamp)
        assertTrue("auth: \"0\" debe resultar en InvalidCredentials", resultStringZero is LoginParseResult.InvalidCredentials)
        assertEquals("Usuario o contraseña incorrectos.", (resultStringZero as LoginParseResult.InvalidCredentials).message)

        // Variante con auth: false
        val jsonFalse = exactServerJson.replace("\"auth\":1", "\"auth\":false")
        val resultFalse = parseAuthResponse(jsonFalse, currentTimeSeconds = serverCurrentTimestamp)
        assertTrue("auth: false debe resultar en InvalidCredentials", resultFalse is LoginParseResult.InvalidCredentials)
        assertEquals("Usuario o contraseña incorrectos.", (resultFalse as LoginParseResult.InvalidCredentials).message)
    }

    @Test
    fun testMissingUserInfo_isInvalidCredentials() {
        val missingUserInfoJson = """{"server_info":{"xui":true}}"""
        val result = parseAuthResponse(missingUserInfoJson, currentTimeSeconds = serverCurrentTimestamp)
        assertTrue("Falta de user_info debe dar InvalidCredentials", result is LoginParseResult.InvalidCredentials)
        assertEquals("Usuario o contraseña incorrectos.", (result as LoginParseResult.InvalidCredentials).message)
    }

    @Test
    fun testExpiredAccount_showsAccountExpiredMessage() {
        // Cuenta vencida (exp_date menor al tiempo actual)
        val expiredTimestamp = serverCurrentTimestamp - 86400 // venció ayer
        val expiredJson = exactServerJson.replace("\"exp_date\":\"1792531606\"", "\"exp_date\":\"$expiredTimestamp\"")
        val result = parseAuthResponse(expiredJson, currentTimeSeconds = serverCurrentTimestamp)

        assertTrue("Cuenta con exp_date en el pasado debe informar vencimiento", result is LoginParseResult.AccountExpiredOrInactive)
        val expired = result as LoginParseResult.AccountExpiredOrInactive
        assertTrue("Debe contener mensaje de vencimiento", expired.message.startsWith("La cuenta venció o está inactiva"))
        assertNotNull(expired.expDateFormatted)
    }

    @Test
    fun testInactiveStatus_showsAccountExpiredOrInactiveMessage() {
        val inactiveJson = exactServerJson.replace("\"status\":\"Active\"", "\"status\":\"Disabled\"")
        val result = parseAuthResponse(inactiveJson, currentTimeSeconds = serverCurrentTimestamp)

        assertTrue("Status distinto de Active debe indicar inactiva", result is LoginParseResult.AccountExpiredOrInactive)
        val inactive = result as LoginParseResult.AccountExpiredOrInactive
        assertTrue(inactive.message.startsWith("La cuenta venció o está inactiva"))
    }

    @Test
    fun testUnexpectedResponse_showsUnexpectedResponseMessage() {
        val invalidJson = "<html><body>502 Bad Gateway</body></html>"
        val result = parseAuthResponse(invalidJson, currentTimeSeconds = serverCurrentTimestamp)

        assertTrue("HTML o formato erróneo debe reportar respuesta inesperada", result is LoginParseResult.UnexpectedResponse)
        assertEquals("Respuesta inesperada del servidor", (result as LoginParseResult.UnexpectedResponse).message)
    }

    @Test
    fun testTolerantHelpers_neverThrowExceptions() {
        // asString
        assertEquals("123", (123).asString())
        assertEquals("true", true.asString())
        assertEquals("Sandy", "Sandy".asString())
        assertNull(null.asString())
        assertNull(JSONObject.NULL.asString())

        // asInt
        assertEquals(1, 1.asInt())
        assertEquals(1, "1".asInt())
        assertEquals(1, true.asInt())
        assertEquals(0, false.asInt())
        assertEquals(0, "0".asInt())
        assertNull("invalido".asInt())
        assertEquals(99, "invalido".asInt(default = 99))
        assertNull(null.asInt())

        // asLong
        assertEquals(1792531606L, 1792531606L.asLong())
        assertEquals(1792531606L, "1792531606".asLong())
        assertEquals(1L, true.asLong())
        assertEquals(0L, false.asLong())
        assertNull("invalido".asLong())
        assertNull(null.asLong())

        // asBoolean
        assertTrue(true.asBoolean() == true)
        assertTrue(false.asBoolean() == false)
        assertTrue(1.asBoolean() == true)
        assertTrue(0.asBoolean() == false)
        assertTrue("true".asBoolean() == true)
        assertTrue("1".asBoolean() == true)
        assertTrue("false".asBoolean() == false)
        assertTrue("0".asBoolean() == false)
        assertNull("random".asBoolean())
        assertNull(null.asBoolean())
    }
}
