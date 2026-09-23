<div align="center">

<img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
<img src="https://img.shields.io/badge/Min%20SDK-24-green?style=for-the-badge&logo=android&logoColor=white" alt="Min SDK">
<img src="https://img.shields.io/badge/Target%20SDK-37-green?style=for-the-badge&logo=android&logoColor=white" alt="Target SDK">

# 🌿 PreForge

### _LA EXAM ENGINE_

**De tus apuntes a tu examen perfecto en segundos**

Carga tus resúmenes o PDFs y nuestra *inteligencia artificial* generará simuladores interactivos de alta fidelidad al instante.

</div>

---

## ✨ Características

| Pantalla | Descripción |
| :--- | :--- |
| 🏠 **Welcome** | Pantalla de bienvenida con acceso a *Iniciar Sesión* y *Registrarse*. |
| 📂 **Dashboard** | Carga tus apuntes (**PDF**, **DOCX**, **TXT** hasta 20 MB) y genera tu simulador. |
| ⏱️ **Simulator** | Examen realista con cronómetro, barra de progreso y preguntas de opción múltiple. |

## 🗺️ Navegación

```
welcome ──▶ dashboard ──▶ simulator
```

La navegación se gestiona con **Navigation Compose** desde `MainActivity.kt`.

## 🖼️ Screenshots

| Welcome | Dashboard | Simulator |
| :---: | :---: | :---: |
| <img src="screenshots/welcome.png" width="220"> | <img src="screenshots/dashboard.png" width="220"> | <img src="screenshots/simulator.png" width="220"> |

## 🛠️ Tecnologías

- **Lenguaje:** Kotlin 2.2.10
- **UI:** Jetpack Compose (Material 3) con BOM 2026.02.01
- **Navegación:** `androidx.navigation:navigation-compose:2.7.7`
- **Gradle:** AGP 9.3.2
- **Min SDK:** 24 | **Target SDK:** 37

## 🎨 Paleta de colores

| Color | Hex | Uso |
| :--- | :--- | :--- |
| <img src="https://via.placeholder.com/12/0x051F20/051F20" width="12"> DarkGreen | `#051F20` | Textos y botones principales |
| <img src="https://via.placeholder.com/12/163832/163832" width="12"> PrimaryGreen | `#163832` | Acentos y títulos |
| <img src="https://via.placeholder.com/12/8EB69B/8EB69B" width="12"> LightGreen | `#8EB69B` | Bordes y detalles |
| <img src="https://via.placeholder.com/12/DAF1DE/DAF1DE" width="12"> BackgroundGreen | `#DAF1DE` | Fondo de pantalla |

> Definidos en `app/src/main/java/com/example/preforge/ui/theme/Color.kt`

## 📁 Estructura del proyecto

```
PreForge/
├── app/
│   └── src/main/
│       ├── java/com/example/preforge/
│       │   ├── MainActivity.kt        # Entrada + NavHost
│       │   ├── WelcomeScreen.kt       # Pantalla de bienvenida
│       │   ├── DashboardScreen.kt     # Subida de apuntes
│       │   ├── SimulatorScreen.kt     # Simulador de examen
│       │   └── ui/theme/              # Colores, tema y tipografía
│       └── res/                       # Recursos (iconos, strings, etc.)
├── gradle/
├── screenshots/                       # Capturas de la app
├── build.gradle.kts
└── settings.gradle.kts
```

## 🚀 Cómo ejecutar

1. Abre el proyecto en **Android Studio** (última versión estable).
2. Espera a que Gradle sincronice las dependencias.
3. Presiona **Run** ▶️ con un emulador o dispositivo conectado (Android 7.0+).

```bash
# También puedes compilar desde la terminal
./gradlew assembleDebug
```

## ✅ TODO (próximos pasos)

- [✅] Integrar subida real de archivos (PDF/DOCX/TXT)
- [✅] Conectar el flujo de IA que genera las preguntas
- [✅] Persistencia de simuladores y resultados
- [ ] Autenticación real (login/registro)

---

<div align="center">

Hecho con 💚 para estudiantes · **PreForge**

</div>
