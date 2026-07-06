# Khipu AI - Android App

Khipu AI is an intelligent mobile application built for Android that helps students manage their studies, summarize their PDF notes, and interact with a personalized AI tutor. 

## Tech Stack
- **UI:** Jetpack Compose (Material Design 3)
- **Architecture:** MVVM, Clean Architecture
- **Dependency Injection:** Dagger Hilt
- **Network:** Retrofit + OkHttp
- **Local Storage:** DataStore (Preferences) & Room Database
- **Authentication:** Google Sign-In & Email/Password

## Requirements
- Android Studio Iguana (or newer)
- JDK 17
- Android SDK 34
- Minimum SDK 26

## Building the App

1. Open the `KhipuAI` folder in Android Studio.
2. Wait for Gradle to sync dependencies.
3. To generate a debug APK, go to **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** or run the following in terminal:
   ```bash
   ./gradlew assembleDebug
   ```

4. The generated APK will be available in `app/build/outputs/apk/debug/app-debug.apk`. You can share this file via GitHub Releases.

## Key Features

- **Personalized Tutor:** Chat interface with context-aware AI based on uploaded notes.
- **Note Scanner:** Integration with PDF documents for text extraction and summaries.
- **Push Notifications:** Firebase Cloud Messaging to notify you when heavy server-side processing is completed.
- **Dark Mode Support:** Automatic Material 3 dynamic theming.
