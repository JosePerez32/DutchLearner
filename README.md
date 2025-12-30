# 🇳🇱 Dutch Learner

Una aplicación Android para aprender holandés mediante reconocimiento de voz y traducción automática.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

---

## 📖 Descripción

Dutch Learner es una app educativa que te permite practicar holandés de forma interactiva:

1. 🎤 **Hablas en español** → La app graba tu voz
2. 📝 **Transcripción automática** → Convierte tu audio a texto (offline con Vosk)
3. 🌐 **Traducción a holandés** → Usa DeepL API para traducción de alta calidad
4. 🔊 **Pronunciación** → Escucha cómo se dice en holandés
5. 💾 **Guardado local** → Almacena tus frases para revisar después

---

## ✨ Características

### ✅ Fase 1 (Actual)
- [x] Grabación de voz con AudioRecord
- [x] Transcripción offline con Vosk (español)
- [x] Traducción online con DeepL API
- [x] Síntesis de voz en holandés (TTS)
- [x] Base de datos local con Room (SQLite)
- [x] Interfaz moderna con Jetpack Compose

### 🚧 Fase 2 (En desarrollo)
- [ ] Análisis de palabras desconocidas
- [ ] Sistema de ranking de frases por dificultad
- [ ] Visualización de vocabulario conocido

### 🔮 Fase 3 (Planificada)
- [ ] Notificaciones programadas para repaso
- [ ] Sistema de repetición espaciada
- [ ] Estadísticas de aprendizaje

---

## 🛠️ Tecnologías

| Componente | Tecnología |
|------------|-----------|
| **Lenguaje** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Base de datos** | Room (SQLite) |
| **Speech-to-Text** | Vosk (offline) |
| **Traducción** | DeepL API (500k caracteres/mes gratis) |
| **Text-to-Speech** | Android TTS nativo |
| **Audio** | AudioRecord (formato WAV/PCM) |
| **Arquitectura** | MVVM + Coroutines |

---

## 📱 Capturas de pantalla

> *TODO: Agregar capturas cuando la UI esté más completa*

---

## 🚀 Instalación y configuración

Ver [SETUP.md](SETUP.md) para instrucciones detalladas.

### Requisitos rápidos:
- Android Studio Hedgehog o superior
- SDK mínimo: API 26 (Android 8.0)
- SDK objetivo: API 34 (Android 14)
- Cuenta gratuita en DeepL (para API key)
- Modelo Vosk español (~40MB o 1.4GB)

---

## 🎯 Uso básico

1. Abre la app
2. Acepta el permiso de micrófono
3. Presiona 🎤 **Grabar**
4. Habla en español (ej: "Hola, ¿cómo estás?")
5. Presiona ⏹ **Detener**
6. Espera la transcripción y traducción
7. Presiona 🔊 para escuchar en holandés
8. Presiona 💾 para guardar la frase

---

## 🗂️ Estructura del proyecto

```
app/
├── src/main/
│   ├── java/com/perez/dutchlearner/
│   │   ├── MainActivity.kt          # Activity principal
│   │   ├── audio/
│   │   │   └── AudioRecorderHelper.kt   # Grabación en formato WAV
│   │   ├── database/
│   │   │   └── PhraseEntity.kt      # Modelos de Room Database
│   │   ├── speech/
│   │   │   └── VoskSpeechRecognizer.kt  # Integración con Vosk
│   │   └── translation/
│   │       └── DeepLTranslationService.kt  # Cliente DeepL API
│   └── assets/
│       └── model-es/                # Modelo Vosk (no versionado)
├── build.gradle.kts                 # Dependencias del módulo
└── secrets.properties               # API keys (no versionado)
```

---

## 🔐 Seguridad

- ✅ Las API keys se almacenan en `secrets.properties` (no versionado)
- ✅ Los datos se guardan localmente (SQLite)
- ✅ No se envían audios a servidores externos (transcripción offline)
- ✅ Solo la traducción requiere conexión a internet

---

## 🤝 Contribuciones

Este es un proyecto personal de aprendizaje, pero las sugerencias son bienvenidas.

Si encuentras un bug o tienes una idea:
1. Abre un **Issue** describiendo el problema
2. Si quieres contribuir código, abre un **Pull Request**

---

## 📄 Licencia

CC0-1.0 license - Ver [LICENSE](LICENSE) para más detalles.

---

## 👤 Autor

**José C. Pérez R.**
- Website: https://jcperez.dev
- GitHub: [@Jose Perez](https://github.com/JosePerez32)

---

## 🙏 Agradecimientos

- [Vosk](https://alphacephei.com/vosk/) - Reconocimiento de voz offline
- [DeepL](https://www.deepl.com/) - API de traducción
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - UI moderna de Android

---

## 📊 Estado del proyecto

**Versión actual:** 0.1.0 (Fase 1 MVP)  
**Última actualización:** Diciembre 2024  
**Estado:** 🟢 En desarrollo activo