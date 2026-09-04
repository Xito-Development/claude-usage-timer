# ⏱️ Temporizador Claude — Xito Development

Notificación permanente con estética de Claude que cuenta atrás el tiempo de tu
límite de uso. Ahora puedes **elegir cuánto dura** cada sesión.

## ✨ Funciones
- **Modo Automático (nuevo)**: inicia sesión con tu cuenta de Claude desde la app
  (WebView) y verás tu **uso real**: barras de sesión de 5 h y límite semanal, con la
  hora de reinicio que reporta Claude. Se actualiza solo cada 2 minutos.
- **Modo Manual**: el temporizador de siempre, sin cuenta.
  Alterna entre ambos con un toque.
- **Widget para la pantalla de inicio**: cuenta atrás con hora de reinicio, barra de
  progreso, y tres cifras — % de sesión usado, % semanal y último gasto detectado.
  Se **repinta solo cada minuto** y muestra cuándo fue la última sincronización.
  Botón de refresco para forzar una consulta al momento.
- **Uso en la notificación**: la notificación permanente muestra el % consumido; al
  expandirla, barras de sesión y semanal más el último gasto detectado.
- **Actividad de consumo**: registra cada subida de tu uso (lo que gasta cada mensaje),
  con el último consumo destacado, total de la sesión, número de consumos e historial.
  Botón de **seguimiento en vivo** que consulta cada 30 s en lugar de cada 2 min.
- **Recordatorios por uso**: elige a qué porcentajes quieres que te avise (50, 75, 80,
  90, 95 y al agotarse), por separado para la **sesión de 5 h** y para el **límite
  semanal**. Cada aviso salta una sola vez y se rearma al restablecerse el límite.
  Disponible en Modo Automático (necesita el uso real de tu cuenta).
- **Dos modos, alternables con un toque**:
  - **Duración** → elige horas y minutos, o usa los atajos (5h · 4h · 3h · 2h · 1h).
  - **Hora exacta** → indica la hora a la que se restablece tu límite (si ya pasó, cuenta hasta mañana).
- **Sonido propio al restablecerse**: campanilla suave y moderna, distinta del tono normal del móvil.
- Notificación fija **totalmente personalizada** (tarjeta con colores de Claude y botones coral):
  no es el diseño gris de Android.
- Cuenta atrás **al segundo**; al expandir la notificación, cronómetro gigante.
- Botones en la notificación: **Comenzar**, **Reiniciar** y **Finalizar servicio**.
- **Finalizar servicio** cierra el temporizador y quita la notificación.
- **Modo oscuro** automático: la app y la notificación se adaptan al tema del móvil.
- **Icono dinámico**: el asterisco de Claude dentro de un reloj muestra una insignia
  con las horas restantes (5→1) y vuelve al normal al terminar.
- Aviso con sonido/vibración cuando el límite se restablece.
- Pantalla de app rediseñada con cuenta atrás en vivo y barra de progreso.

## 📱 Cómo se usa
1. Abre la app, elige el tiempo y pulsa **Comenzar sesión** (o usa el botón de la notificación).
2. Se inicia la cuenta atrás. Cuando llega a cero, te avisa y vuelve al inicio.

## 📦 Compilar el APK (gratis con GitHub Actions)
1. El repo incluye `.github/workflows/build.yml` (¡carpeta **workflows**, con "s"!).
2. Al subir/cambiar archivos, GitHub compila solo.
3. Pestaña **Actions** → última ejecución con ✅ verde → sección **Artifacts** → descarga **ClaudeTimer-APK**.
4. Descomprime, pasa `app-debug.apk` al móvil e instálalo (permite "orígenes desconocidos").

## 🔄 Cómo ACTUALIZAR la app
1. Descarga el nuevo ZIP que te dé Claude y descomprímelo.
2. En GitHub: repo → **Add file → Upload files** → arrastra **todo el contenido**
   de la carpeta (no la carpeta en sí) → **Commit changes**.
3. Espera el ✅ en **Actions**, descarga el APK nuevo e instálalo encima del anterior.
   - Si Android se queja por firmas, desinstala la versión antigua e instala la nueva.

## 🎨 Personalizar a tu gusto (sin programar)
Edita el archivo en GitHub (icono del lápiz → cambia texto → Commit):
- **Colores**: `app/src/main/res/values/colors.xml` (claro) y `values-night/colors.xml` (oscuro).
  El coral es `#D97757`. Cámbialo por otro código hex.
- **Textos de la notificación**: `app/src/main/res/layout/notification_*.xml` (líneas `android:text="..."`).
- **Tamaño del cronómetro**: en esos mismos archivos, `android:textSize="44sp"`.
- **Nombre de la app**: `app/src/main/res/values/strings.xml`.
- **Duración máxima del selector**: `MainActivity.kt`, línea `pickHours.maxValue = 12`.

Regla de oro: cambia solo lo que hay **entre comillas**; no borres comillas, `<etiquetas>` ni paréntesis.
Haz un cambio, compila y comprueba.

---
Hecho con Claude · Xito Development

## 📄 Licencia
Publicado bajo licencia **MIT** (ver archivo `LICENSE`). Puedes usar, modificar y
compartir la app libremente; solo hay que conservar el aviso de copyright.


## ⚠️ Nota sobre el Modo Automático
Usa un endpoint interno de claude.ai (`/api/organizations/{id}/usage`) que **no es una
API oficial**. Puede cambiar sin aviso y dejar de funcionar; además la sesión caduca
cada pocos días y habrá que volver a iniciar sesión. Tus credenciales se guardan
solo en tu móvil y no se envían a ningún sitio salvo a claude.ai.
Si el modo automático falla, el Modo Manual sigue funcionando siempre.

## 📲 Añadir el widget
Mantén pulsado un hueco vacío de tu pantalla de inicio → **Widgets** →
busca **Temporizador Claude** → arrastra el widget. Tócalo para abrir la app,
o pulsa el icono de refrescar para actualizar al momento.


## 🔎 Cómo funciona la "Actividad de consumo"
Android no permite que una app vea lo que ocurre dentro de otra, así que la app **no
puede** leer la petición que envías en Claude ni sus tokens exactos. Lo que hace es
consultar tu uso cada 2 minutos y, cuando detecta que el porcentaje ha subido, registra
ese salto como un consumo. Verás el gasto de cada mensaje unos segundos después de
enviarlo, no mientras se genera. Con el seguimiento en vivo (30 s) la detección es
casi inmediata, a cambio de algo más de batería y datos.
