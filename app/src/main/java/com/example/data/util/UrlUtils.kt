package com.example.data.util

/**
 * Normaliza la dirección del servidor Xtream Codes:
 * - Agrega "http://" si no tiene esquema
 * - Elimina espacios en blanco internos y bordes
 * - Quita barras diagonales finales
 * - Elimina sufijos accidentales como "/player_api.php" (con o sin query params) o "/c"
 */
fun normalizeServerUrl(rawUrl: String): String {
    var s = rawUrl.trim().replace(Regex("""\s+"""), "")
    if (s.isEmpty()) return "http://sdmx.vip:80"
    if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
        s = "http://$s"
    }
    // Si el usuario pegó la URL de la API completa (ej: http://servidor:80/player_api.php?...)
    val apiIndex = s.indexOf("/player_api.php", ignoreCase = true)
    if (apiIndex != -1) {
        s = s.substring(0, apiIndex)
    }
    // Si el usuario pegó el portal tipo MAG /c o /c/
    s = s.trimEnd('/')
    if (s.endsWith("/c", ignoreCase = true)) {
        s = s.substring(0, s.length - 2)
    }
    return s.trimEnd('/')
}

/**
 * Enmascara la contraseña en URLs para propósitos de registro y diagnóstico seguro.
 */
fun maskPasswordInUrl(url: String): String {
    var masked = url.replace(Regex("""([?&]password=)[^&]*""", RegexOption.IGNORE_CASE), "$1***")
    // También enmascarar rutas de stream /live/user/pass/id o /movie/user/pass/id o /series/user/pass/id
    masked = masked.replace(Regex("""(/(live|movie|series)/[^/]+/)([^/]+)(/.*)"""), "$1***$4")
    return masked
}
