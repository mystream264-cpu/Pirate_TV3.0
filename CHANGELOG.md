# Historial de Versiones - Pirate_TV

## Versión 1.1 (Actualización de Conectividad y Diagnóstico)

### Correcciones y Mejoras Principales:
1. **User-Agent unificado en todo el cliente:**
   - Se configuró el encabezado `User-Agent` global con el valor `"VLC/3.0.20 LibVLC/3.0.20"` a través de una constante centralizada (`NetworkConfig.USER_AGENT`) y un interceptor de OkHttp (`UserAgentInterceptor`).
   - Se aplica este mismo User-Agent de manera consistente en:
     - Todas las peticiones API (`/player_api.php`).
     - La carga de imágenes y logotipos con Coil mediante `PirateTvApp` (`ImageLoaderFactory`).
     - La reproducción de streams multimedia en Media3 ExoPlayer (`DefaultHttpDataSource.Factory.setUserAgent`).
   - Esto soluciona el bloqueo de servidores Xtream Codes que rechazan el User-Agent por defecto `"okhttp"`.

2. **Lectura y validación segura de respuestas (Fin de errores técnicos de Gson/Moshi):**
   - En el inicio de sesión (`login`), categorías (`get_*_categories`), listas (`get_*_streams`) e información (`get_*_info`), las respuestas HTTP ahora se leen primero como texto plano (`String`).
   - Se valida el código de respuesta HTTP y se comprueba si el cuerpo comienza con `{` o `[`.
   - Si el servidor devuelve texto que no es JSON (como HTML de error o bloqueo de texto plano), no se muestra la excepción técnica de parseo (`Expected BEGIN_OBJECT but was STRING`), sino un mensaje amigable en español:
     `"El servidor respondió algo inesperado (código HTTP XXX)"`.

3. **Diagnóstico visible en pantalla si el login falla:**
   - Se añadió un panel de diagnóstico en texto gris claro debajo del mensaje de error en la pantalla de inicio de sesión:
     - **(a)** URL exacta utilizada en la petición, con la contraseña enmascarada (`password=***`).
     - **(b)** Código de estado HTTP devuelto por el servidor (o indicación si no hubo respuesta).
     - **(c)** Los primeros 120 caracteres del cuerpo de la respuesta para facilitar la copia y diagnóstico rápido del proveedor.
   - El texto es seleccionable (`SelectionContainer`) para permitir copiarlo directamente.

4. **Configuración de red y reintentos en OkHttp:**
   - Se activaron `followRedirects(true)` y `followSslRedirects(true)`.
   - Timeout de conexión configurado a **15 segundos**.
   - Timeout de lectura configurado a **30 segundos**.
   - Timeout de escritura configurado a **30 segundos**.
   - Reintento automático ante fallos de conexión (`RetryInterceptor`).

5. **Normalización del campo Servidor:**
   - Función `normalizeServerUrl`:
     - Agrega automáticamente el prefijo `http://` si no tiene esquema.
     - Remueve espacios en blanco internos y bordes.
     - Remueve barras diagonales finales (`/`).
     - Remueve sufijos pegados por error como `/player_api.php` o `/c`.

6. **Automatización de compilación y Releases:**
   - Flujo de GitHub Actions (`build.yml`) configurado para compilar el APK en verde y actualizar automáticamente el release con etiqueta `latest`.

---

## Versión 1.0 (Inicial)
- Estructura base de la aplicación IPTV en Jetpack Compose.
- Soporte para TV en vivo, Películas (VOD) y Series con reproductor integrado Media3.
- Búsqueda global con filtro por tipo de contenido.
- Diálogo de ajustes protegido por contraseña ("3623").
- Integración opcional con OMDb para sinopsis, notas IMDb y puntuación Rotten Tomatoes.
