# Recursos de Pirate_TV

Esta carpeta contiene los archivos multimedia y gráficos que la aplicación carga como assets dentro del APK.
Puedes subir tus archivos directamente desde la interfaz web de GitHub en esta carpeta (`recursos/`).

---

## Archivos que debes subir y nombres EXACTOS:

| Nombre de archivo | Descripción | Formato / Detalles |
| :--- | :--- | :--- |
| `pirate_tv_intro.mp4` | Video de bienvenida | Video MP4 que se reproduce a pantalla completa tras iniciar sesión. Puede omitirse tocando la pantalla o terminar solo. Si no está, la app inicia directamente sin error. |
| `img_live.png` | Portada de inicio: TV en vivo | Imagen para la tarjeta de TV en vivo en la pantalla principal. |
| `img_vod.png` | Portada de inicio: Películas | Imagen para la tarjeta de Películas en la pantalla principal. |
| `img_series.png` | Portada de inicio: Series | Imagen para la tarjeta de Series en la pantalla principal. |
| `img_logo.png` | Logo de inicio de sesión | Logo grande centrado sobre los campos de usuario y contraseña en el Login. |
| `img_logo_small.png` | Logo de la barra superior | Logo pequeño (aprox 44x44 dp) situado al lado del texto "Pirate_TV" en la barra superior. |
| `ic_launcher_source.png` | Ícono base de la app | Imagen cuadrada con esquinas redondeadas para el ícono de la aplicación. Se excluye del empaquetado de assets del APK. |

---

## Instrucciones paso a paso para subirlos en GitHub:

1. Entra a tu repositorio en GitHub desde tu navegador.
2. Navega a la carpeta **`recursos`**.
3. Haz clic en el botón **Add file** (arriba a la derecha) y selecciona **Upload files**.
4. Arrastra o selecciona tus archivos asegurándote de que los nombres coincidan exactamente con la tabla anterior (en minúsculas y con la extensión correcta: `.png` o `.mp4`).
5. Escribe un mensaje de commit (por ejemplo, `Subir recursos multimedia de Pirate_TV`) y presiona **Commit changes**.
6. GitHub Actions compilará automáticamente una nueva versión del APK incluyendo los recursos empaquetados.

---

## Comportamiento ante archivos faltantes:
La app está protegida y es tolerante a fallos:
- Si falta `img_logo.png` o `img_logo_small.png`, muestra el texto estilizado **"Pirate_TV"** en tipografía serif dorada.
- Si faltan `img_live.png`, `img_vod.png` o `img_series.png`, muestra tarjetas elegantes con degradado y los títulos correspondientes ("TV en vivo", "Películas", "Series").
- Si falta `pirate_tv_intro.mp4`, la app salta la intro silenciosamente y va directo a la pantalla principal.
