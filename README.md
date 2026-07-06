<div align="center">
  <img src="../logo.png" alt="Khipu AI Logo" width="200"/>
  <h1>Khipu AI - Aplicación Móvil (Android)</h1>
  <p><strong>Tu tutor personal universitario potenciado con Inteligencia Artificial</strong></p>
</div>

---

## 📌 ¿Qué es Khipu AI?

**Khipu AI** es una aplicación móvil nativa para Android diseñada para revolucionar la forma en que los estudiantes universitarios interactúan con su material de estudio. Ya no necesitas leer documentos PDF extensos sin apoyo; Khipu AI extrae el contenido de tus lecturas y te permite **conversar con un tutor inteligente** que responderá tus preguntas basándose exactamente en tus propios apuntes.

### ✨ Características Principales

* 🧠 **Tutoría Personalizada con IA:** Chatea en tiempo real con un tutor impulsado por Google Gemini. El tutor conoce el contexto de tus apuntes y te explica conceptos difíciles de manera interactiva.
* 📄 **Escáner y Procesamiento de PDFs:** Sube tus lecturas o apuntes en PDF. Nuestro backend extrae el texto, incluso de imágenes, y lo vectoriza para búsquedas semánticas ultra rápidas.
* 🔔 **Notificaciones Push en Tiempo Real:** Subir un PDF largo puede demorar. No te preocupes, puedes seguir usando la app o salir de ella; Firebase Cloud Messaging te enviará una notificación a tu celular apenas tus apuntes estén listos para estudiar.
* 👤 **Onboarding Interactivo:** Perfil adaptado a tu universidad para ofrecerte una experiencia académica más cercana.
* 🔐 **Seguridad Integrada:** Autenticación fluida usando Google Sign-In o correo electrónico/contraseña, protegiendo tus datos.
* 🎨 **Diseño Moderno y Dark Mode:** Interfaz de usuario construida con Material Design 3, ofreciendo colores dinámicos y adaptación automática al modo oscuro de tu sistema.

---

## 🛠️ Stack Tecnológico

El proyecto está desarrollado utilizando los más modernos estándares recomendados por Google para el ecosistema Android:

- **Lenguaje:** Kotlin 1.9+
- **Interfaz de Usuario:** Jetpack Compose
- **Arquitectura:** Clean Architecture + MVVM (Model-View-ViewModel)
- **Inyección de Dependencias:** Dagger Hilt
- **Consumo de APIs:** Retrofit + OkHttp
- **Navegación:** Jetpack Navigation Compose
- **Almacenamiento Local:** DataStore (para preferencias de sesión)
- **Servicios Cloud:** Firebase (Authentication, Cloud Messaging)

---

## 🚀 Guía de Instalación para Desarrolladores

Si deseas clonar y ejecutar este proyecto localmente, sigue estos pasos:

### 1. Requisitos Previos
- [Android Studio Iguana](https://developer.android.com/studio) o una versión más reciente.
- Java Development Kit (JDK) 17.
- SDK de Android API 34.

### 2. Pasos para Compilar
1. Clona este repositorio:
   ```bash
   git clone https://github.com/khipu-ai-app/khipu-ai-android.git
   ```
2. Abre la carpeta `KhipuAI` en Android Studio.
3. Permite que **Gradle** descargue y sincronice todas las dependencias.
4. Conecta un dispositivo físico o inicia un emulador de Android (Mínimo API 26).
5. Haz clic en el botón de **Run** (`Shift + F10`) en Android Studio.

### 3. Generar el Archivo APK (Para Compartir)
Si solo quieres generar el instalador para probar en un dispositivo físico sin necesidad de cables:
1. En Android Studio, dirígete al menú superior: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. O desde la terminal en la raíz del proyecto, ejecuta:
   ```bash
   ./gradlew assembleDebug
   ```
3. El archivo resultante estará listo para compartir en la ruta: `app/build/outputs/apk/debug/app-debug.apk`.

---
*Hecho con 🦙 por el equipo de Khipu AI.*
