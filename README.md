# 🇳🇱 Dutch Learner

An advanced Dutch learning Android app using **offline voice recognition**, **automatic translation**, and an **inverted vocabulary system** that tracks only unknown words.

[![License: CC0-1.0](https://img.shields.io/badge/License-CC0_1.0-lightgrey.svg)](http://creativecommons.org/publicdomain/zero/1.0/)
[![Android](https://img.shields.io/badge/Android-26%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)](https://kotlinlang.org)

> **Built for advanced learners (B2-C1)** who want to focus on mastering difficult vocabulary through real conversations.

---

## ✨ Features

### 🎤 **Voice Recording & Translation**
- **Offline speech recognition** using Vosk (Spanish → Text)
- **Automatic translation** to Dutch using DeepL API
- **Text-to-Speech** for Dutch pronunciation practice
- Works without internet (saves phrases for later translation)

### 📚 **Inverted Vocabulary System**
- Tracks **UNKNOWN words only** (everything else is assumed known)
- Tap words in phrases to add them to your learning list
- Smart tokenization detects plurals, diminutives, and conjugations
- Difficulty calculation based on word length and frequency

### ⏰ **Smart Daily Notifications**
- Randomly shows either:
    - **3 words + Spanish translation**, or
    - **A complete phrase** with unknown words highlighted
- Customizable alarm times (up to 5 alarms)
- Actions: 🔊 Listen or 🎤 Record directly from notification

### 📱 **Quick Record Widget**
- **Home screen widget** for instant recording
- Records in background (no need to open app)
- Visual feedback: Purple (idle) → Red (recording)
- Auto-saves phrases after 10 seconds

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM-like (no ViewModels, state in MainActivity) |
| **Database** | Room (SQLite) |
| **Speech Recognition** | Vosk (offline, ~39MB Spanish model) |
| **Translation** | DeepL Free API (500k chars/month) |
| **Text-to-Speech** | Android TTS (Dutch) |
| **Background Tasks** | Foreground Services + AlarmManager |

---

## 📂 Project Structure

```
app/src/main/java/com/perez/dutchlearner/
├── MainActivity.kt                 # Main coordinator (no ViewModels)
├── audio/
│   └── AudioRecorderHelper.kt     # WAV recording (16kHz PCM)
├── database/
│   ├── PhraseEntity.kt            # Phrases + Unknown Words tables
│   ├── AlarmEntity.kt             # Multi-alarm support
│   └── AppDatabase.kt             # Room database
├── language/
│   └── DutchTokenizer.kt          # Dutch word analysis
├── notifications/
│   ├── NotificationHelper.kt      # Smart notifications
│   ├── NotificationContentGenerator.kt  # Random content
│   └── AlarmReceiver.kt           # Broadcast receiver
├── speech/
│   └── VoskSpeechRecognizer.kt    # Offline transcription
├── translation/
│   └── DeepLTranslationService.kt # DeepL API client
├── ui/
│   ├── PhrasesScreen.kt           # Saved phrases list
│   ├── UnknownWordsScreen.kt      # Vocabulary management
│   ├── AlarmsScreen.kt            # Alarm configuration
│   └── SettingsScreen.kt          # App settings
└── widget/
    ├── QuickRecordWidget.kt       # Home screen widget
    └── RecordingService.kt        # Background recording
```

---

## 🚀 Setup Instructions

### 1. **Prerequisites**
- Android Studio Hedgehog or newer
- Android device/emulator with API 26+ (Android 8.0+)
- DeepL API key (free tier: https://www.deepl.com/pro-api)

### 2. **Clone Repository**
```bash
git clone https://github.com/JosePerez32/DutchLearner.git
cd DutchLearner
```

### 3. **Configure API Key**
Create `secrets.properties` in project root:
```properties
DEEPL_API_KEY=your_api_key_here
```

### 4. **Download Vosk Model**
1. Download `vosk-model-small-es-0.42.zip` from [Vosk Models](https://alphacephei.com/vosk/models)
2. Extract to `app/src/main/assets/model-es/`
3. Ensure `am/` folder exists inside `model-es/`

**⚠️ Important**: The model is ~39MB and **not included** in Git due to size.

### 5. **Install Dutch TTS**
On your Android device:
1. Settings → Language & Input → Text-to-Speech
2. Install **Dutch (Netherlands)** voice pack

### 6. **Build & Run**
```bash
./gradlew assembleDebug
```

---

## 📊 Database Schema

### **phrases** (Saved translations)
| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | Primary key |
| `spanish_text` | String | Original Spanish |
| `dutch_text` | String | Dutch translation |
| `unknown_words_count` | Int | Number of unknown words |
| `unknown_words` | String | CSV of unknown words |
| `created_at` | Long | Timestamp |

### **unknown_words** (Inverted vocabulary)
| Column | Type | Description |
|--------|------|-------------|
| `word` | String | Primary key (normalized) |
| `times_seen` | Int | Frequency counter |
| `learned` | Boolean | Mastered flag |
| `difficulty` | Int | 0=easy, 1=medium, 2=hard |

### **alarms** (Notification schedule)
| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | Primary key |
| `hour` | Int | 0-23 |
| `minute` | Int | 0-59 |
| `enabled` | Boolean | Active flag |
| `days_of_week` | String | CSV (1=Mon, 7=Sun) |

---

## 🎯 Usage Guide

### **Recording a Phrase**
1. Tap **🎤 Grabar** button
2. Speak in Spanish (10 seconds)
3. Wait for transcription + translation
4. Review and tap **💾 Guardar frase**

### **Managing Vocabulary**
1. Open **📚 Palabras por aprender**
2. Tap **✅** to mark words as learned
3. Or add words manually with **+** button

### **Tapping Words in Phrases**
1. Open **📋 Mis frases guardadas**
2. Tap **"Toca palabras para agregar ➕"**
3. Tap any word → automatically added to unknown list

### **Using the Widget**
1. Long-press home screen → Add widget → Dutch Learner
2. Tap widget → records 10 seconds in background
3. Widget turns red while recording
4. Notification shows "✅ Frase guardada"

---

## 🧠 AI Context

This project was developed collaboratively with Claude (Anthropic) in January 2026. For AI assistants continuing this work, see [`AI_CONTEXT.json`](./AI_CONTEXT.json) for:
- Architecture decisions (NO ViewModels pattern)
- Critical implementation details
- Database schema and relationships
- Known issues and solutions
- Future enhancement roadmap

---

## 📝 License

This project is released under the [CC0 1.0 Universal](LICENSE) license.  
**TL;DR**: You can copy, modify, distribute, and use this project for any purpose, even commercially, without asking permission.

---

## 👨‍💻 Author

**Jose Perez**  
🌐 [jcperez.dev](https://jcperez.dev)  
💼 [GitHub @JosePerez32](https://github.com/JosePerez32)

---

## 🙏 Acknowledgments

- [Vosk](https://alphacephei.com/vosk/) for offline speech recognition
- [DeepL](https://www.deepl.com/) for high-quality translations
- [Anthropic](https://www.anthropic.com/) for Claude AI assistance

---

## 📅 Project Timeline

- **Started**: December 2024
- **Version 1.0**: January 2026
- **Status**: Active development

---

## 🐛 Known Issues

1. **Widget on emulator**: May not record audio properly (test on physical device)
2. **TTS speed**: Some users report it's too fast (configurable in code: `setSpeechRate(0.75f)`)
3. **Large model size**: Vosk model is 39MB (not included in repo)

---

## 🚧 Roadmap

- [ ] Spaced repetition algorithm
- [ ] Export/import vocabulary lists
- [ ] Dutch pronunciation scoring
- [ ] Integration with Anki
- [ ] iOS version

---

**Star ⭐ this repo if it helps you learn Dutch!**