# PreForge 🎓

**PreForge** es una aplicación Android diseñada para revolucionar la forma en que los estudiantes se preparan para sus exámenes. Utiliza Inteligencia Artificial de vanguardia para transformar apuntes físicos en simuladores de examen personalizados de manera instantánea.

## 🚀 Características Principales

- **Generación de Cuestionarios con IA:** Sube una foto de tus apuntes y deja que la IA (**Gemini 3.7 Flash**) analice el contenido y genere automáticamente preguntas de opción múltiple.
- **Base de Datos Local (Room):** Todas tus preguntas generadas se guardan de forma segura en tu dispositivo utilizando **Room/SQLite**, permitiéndote estudiar sin conexión.
- **Simulador de Examen:** Una interfaz interactiva para poner a prueba tus conocimientos con retroalimentación en tiempo real, puntuación y seguimiento de progreso.
- **Interfaz Moderna:** Desarrollada íntegramente con **Jetpack Compose**, ofreciendo una experiencia de usuario fluida, intuitiva y con un diseño visual atractivo basado en temas naturales.

## 🛠️ Tecnologías Utilizadas

- **Kotlin:** Lenguaje de programación principal.
- **Jetpack Compose:** Toolkit moderno para el desarrollo de interfaces nativas.
- **Google Generative AI SDK:** Integración con Gemini para el procesamiento de lenguaje natural y visión.
- **Room Persistence Library:** Para la gestión de la base de datos local SQLite.
- **Navigation Compose:** Para la gestión de rutas y pantallas dentro de la app.
- **Firebase:** Analytics y servicios de Google integrados.

## 📦 Estructura del Proyecto

- `app/src/main/java/com/example/preforge/`: Contiene la lógica de la aplicación y las pantallas (UI).
- `data/local/`: Definición de la base de datos Room, Entidades y DAOs.
- `AiEngine.kt`: Motor central para la interacción con la API de Gemini.
- `ui/theme/`: Definición de colores, tipografía y temas visuales.

## 🔧 Configuración

Para ejecutar este proyecto localmente, necesitarás:

1. Clonar el repositorio.
2. Obtener una **API KEY** de [Google AI Studio](https://aistudio.google.com/).
3. Configurar la API KEY en `AiEngine.kt` (o mediante variables de entorno en una versión de producción).
4. Asegurarte de tener el archivo `google-services.json` correctamente configurado en el módulo `app`.

---
Desarrollado con ❤️ para ayudar a los estudiantes a alcanzar su máximo potencial.
