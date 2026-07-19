# ⏱️ Temporizador Claude — Xito Development

Notificación permanente con estética de Claude que cuenta atrás el tiempo de tu
límite de uso. Ahora puedes **elegir cuánto dura** cada sesión.

## ✨ Funciones
- **Selector de tiempo**: elige horas y minutos, o usa los atajos (5h · 4h · 3h · 2h · 1h).
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
