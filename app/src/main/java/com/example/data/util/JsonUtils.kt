package com.example.data.util

import com.example.data.model.UserInfo
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helpers tolerantes para extraer valores de JSON sin lanzar excepciones.
 * Aceptan números, cadenas o booleanos de forma intercambiable.
 */
fun Any?.asString(): String? {
    if (this == null || this == JSONObject.NULL) return null
    return try {
        this.toString()
    } catch (_: Exception) {
        null
    }
}

fun Any?.asInt(default: Int? = null): Int? {
    if (this == null || this == JSONObject.NULL) return default
    return try {
        when (this) {
            is Number -> this.toInt()
            is Boolean -> if (this) 1 else 0
            is String -> this.trim().toIntOrNull() ?: default
            else -> this.toString().trim().toIntOrNull() ?: default
        }
    } catch (_: Exception) {
        default
    }
}

fun Any?.asLong(default: Long? = null): Long? {
    if (this == null || this == JSONObject.NULL) return default
    return try {
        when (this) {
            is Number -> this.toLong()
            is Boolean -> if (this) 1L else 0L
            is String -> this.trim().toLongOrNull() ?: default
            else -> this.toString().trim().toLongOrNull() ?: default
        }
    } catch (_: Exception) {
        default
    }
}

fun Any?.asBoolean(default: Boolean? = null): Boolean? {
    if (this == null || this == JSONObject.NULL) return default
    return try {
        when (this) {
            is Boolean -> this
            is Number -> this.toInt() != 0
            is String -> when (this.trim().lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> default
            }
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

/**
 * Comprueba si un valor es explícitamente 0, "0" o false.
 */
fun isExplicitlyZeroOrFalse(value: Any?): Boolean {
    if (value == null || value == JSONObject.NULL) return false
    return when (value) {
        is Boolean -> !value
        is Number -> value.toInt() == 0
        is String -> {
            val trimmed = value.trim()
            trimmed == "0" || trimmed.equals("false", ignoreCase = true)
        }
        else -> false
    }
}

/**
 * Comprueba si un valor es explícitamente 1, "1" o true.
 */
fun isExplicitlyOneOrTrue(value: Any?): Boolean {
    if (value == null || value == JSONObject.NULL) return false
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() == 1
        is String -> {
            val trimmed = value.trim()
            trimmed == "1" || trimmed.equals("true", ignoreCase = true)
        }
        else -> false
    }
}

/**
 * Resultado estructurado del análisis de la respuesta de autenticación del servidor Xtream Codes.
 */
sealed class LoginParseResult {
    data class Success(val userInfo: UserInfo) : LoginParseResult()
    data class InvalidCredentials(val message: String = "Usuario o contraseña incorrectos.") : LoginParseResult()
    data class AccountExpiredOrInactive(val message: String, val expDateFormatted: String?) : LoginParseResult()
    data class UnexpectedResponse(val message: String = "Respuesta inesperada del servidor", val reason: String? = null) : LoginParseResult()
}

/**
 * Analiza la respuesta JSON de autenticación de forma tolerante a fallos de tipo (número vs texto).
 *
 * Reglas de negocio:
 * 1) No modelar user_info con data class estricta para evitar JsonDataException de Moshi.
 * 2) El login es válido si auth es 1, "1" o true. También es válido si status == "Active" y auth no es explícitamente 0, "0" o false.
 *    SOLO muestra "Usuario o contraseña incorrectos." si auth es explícitamente 0/"0"/false o si falta user_info.
 *    Cualquier otro fallo de lectura muestra "Respuesta inesperada del servidor".
 * 3) Si exp_date es menor a la hora actual o status es distinto de "Active", muestra "La cuenta venció o está inactiva" con la fecha de vencimiento.
 */
fun parseAuthResponse(
    rawJson: String,
    currentTimeSeconds: Long = System.currentTimeMillis() / 1000L
): LoginParseResult {
    val trimmed = rawJson.trim()
    if (!trimmed.startsWith("{")) {
        return LoginParseResult.UnexpectedResponse(
            message = "Respuesta inesperada del servidor",
            reason = "La respuesta no es un objeto JSON"
        )
    }

    val jsonObj = try {
        JSONObject(trimmed)
    } catch (e: Exception) {
        return LoginParseResult.UnexpectedResponse(
            message = "Respuesta inesperada del servidor",
            reason = "Error al parsear JSON: ${e.localizedMessage}"
        )
    }

    // Regla 2: falta user_info o es nulo
    if (!jsonObj.has("user_info") || jsonObj.isNull("user_info")) {
        return LoginParseResult.InvalidCredentials("Usuario o contraseña incorrectos.")
    }

    val userInfoRaw = jsonObj.opt("user_info")
    if (userInfoRaw !is JSONObject) {
        // En servidores que responden {"user_info": 0} o {"user_info": false} ante credenciales erróneas
        if (isExplicitlyZeroOrFalse(userInfoRaw)) {
            return LoginParseResult.InvalidCredentials("Usuario o contraseña incorrectos.")
        }
        return LoginParseResult.UnexpectedResponse(
            message = "Respuesta inesperada del servidor",
            reason = "user_info no es un objeto JSON"
        )
    }

    val userInfoObj = userInfoRaw
    val authRaw = userInfoObj.opt("auth")
    val status = userInfoObj.opt("status").asString()
    val expDateLong = userInfoObj.opt("exp_date").asLong()
    val username = userInfoObj.opt("username").asString()
    val password = userInfoObj.opt("password").asString()
    val activeCons = userInfoObj.opt("active_cons").asString()
    val maxConnections = userInfoObj.opt("max_connections").asString()
    val message = userInfoObj.opt("message").asString()
    val isTrial = userInfoObj.opt("is_trial").asString()
    val createdAt = userInfoObj.opt("created_at").asString()

    // Si auth es explícitamente 0, "0" o false -> contraseña incorrecta
    if (isExplicitlyZeroOrFalse(authRaw)) {
        return LoginParseResult.InvalidCredentials("Usuario o contraseña incorrectos.")
    }

    // El login es válido si auth es 1, "1" o true. También es válido si status == "Active" y auth no es explícitamente 0, "0" o false.
    val isAuthValid = isExplicitlyOneOrTrue(authRaw) ||
            (status.equals("Active", ignoreCase = true) && !isExplicitlyZeroOrFalse(authRaw))

    if (!isAuthValid) {
        // Cualquier otro fallo de lectura muestra "Respuesta inesperada del servidor"
        return LoginParseResult.UnexpectedResponse(
            message = "Respuesta inesperada del servidor",
            reason = "Campo auth ($authRaw) o status ($status) no válidos para autenticación"
        )
    }

    // Regla 4: Si exp_date es menor a la hora actual o status es distinto de "Active", muestra "La cuenta venció o está inactiva" con la fecha de vencimiento
    val isExpired = expDateLong != null && expDateLong > 0 && expDateLong < currentTimeSeconds
    val isInactive = status != null && !status.equals("Active", ignoreCase = true)

    if (isExpired || isInactive) {
        val dateStr = if (expDateLong != null && expDateLong > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(expDateLong * 1000L))
        } else {
            null
        }
        val expMsg = if (dateStr != null) {
            "La cuenta venció o está inactiva ($dateStr)"
        } else {
            "La cuenta venció o está inactiva"
        }
        return LoginParseResult.AccountExpiredOrInactive(
            message = expMsg,
            expDateFormatted = dateStr
        )
    }

    val userInfo = UserInfo(
        username = username,
        password = password,
        auth = authRaw,
        status = status,
        expDate = expDateLong?.toString(),
        activeCons = activeCons,
        maxConnections = maxConnections,
        message = message,
        isTrial = isTrial,
        createdAt = createdAt
    )
    return LoginParseResult.Success(userInfo)
}
