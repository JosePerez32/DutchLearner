# 🛠️ Guía de instalación - Dutch Learner

Esta guía te ayudará a configurar el proyecto en tu máquina local.

---

## 📋 Requisitos previos

### Software necesario:
- ✅ **Android Studio** Hedgehog (2023.1.1) o superior
- ✅ **Java JDK 17** (incluido con Android Studio)
- ✅ **Git** para clonar el repositorio
- ✅ **8GB RAM mínimo** (Gradle + emulador + Vosk)

### Hardware recomendado:
- 💾 **30GB de espacio libre** (IDE + SDK + modelo Vosk)
- 🖥️ **CPU de 4 núcleos o más** para compilación rápida

---

## 📦 Instalación paso a paso

### 1️⃣ Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/DutchLearner.git
cd DutchLearner
```

---

### 2️⃣ Configurar API key de DeepL

#### a) Obtener API key gratuita:

1. Ve a https://www.deepl.com/pro-api
2. Click en **"Sign up for free"** (NO requiere tarjeta)
3. Confirma tu email
4. Ve a **Account** → **API Keys**
5. Copia tu **Authentication Key**

#### b) Crear archivo `secrets.properties`:

En la **raíz del proyecto** (junto a `settings.gradle.kts`), crea un archivo llamado `secrets.properties`:

```properties
DEEPL_API_KEY=tu_clave_de_deepl_aqui
```

**Ejemplo:**
```properties
DEEPL_API_KEY=a1b2c3d4-5678-90ef-ghij-klmnopqrstuv:fx
```

⚠️ **IMPORTANTE:** Este archivo está en `.gitignore` y NO se sube a Git.

---

### 3️⃣ Descargar e instalar modelo de Vosk

Vosk es necesario para la transcripción de voz offline.

#### a) Descargar el modelo:

Ve a https://alphacephei.com/vosk/models y descarga **uno de estos**:

| Modelo | Tamaño | Precisión | Recomendado para |
|--------|--------|-----------|------------------|
| `vosk-model-small-es-0.42` | ~39MB | Media | Testing rápido |
| `vosk-model-es-0.42` | ~1.4GB | Alta | Producción |

**Descarga:** Click en el nombre del modelo → Descargar ZIP

#### b) Extraer e instalar:

1. Extrae el archivo ZIP descargado
2. Verás una carpeta llamada `vosk-model-small-es-0.42` (o similar)
3. **IMPORTANTE:** Copia el **contenido** de esa carpeta (no la carpeta misma)
4. Pégalo en: `app/src/main/assets/model-es/`

**Estructura correcta:**

```
app/src/main/assets/model-es/
├── am/
├── conf/
├── graph/
├── ivector/
├── rescore/
├── rnnlm/
└── README
```

**❌ Estructura INCORRECTA (no funciona):**

```
app/src/main/assets/model-es/
└── vosk-model-small-es-0.42/  ← Esta carpeta NO debe estar
    ├── am/
    ├── conf/
    └── ...
```

---

### 4️⃣ Abrir proyecto en Android Studio

1. Abre **Android Studio**
2. **File** → **Open**
3. Selecciona la carpeta `DutchLearner/`
4. Espera a que Gradle sincronice (~2-5 minutos la primera vez)

---

### 5️⃣ Configurar emulador o dispositivo

#### Opción A: Emulador (recomendado para testing)

1. En Android Studio: **Tools** → **Device Manager**
2. Click en **Create Device**
3. Selecciona **Pixel 6** (o similar)
4. Selecciona **System Image**:
   - **API 34** (Android 14) - Recomendado
   - **x86_64** (más rápido en PC con Intel/AMD)
5. En **Advanced Settings**:
   - **RAM:** 4GB mínimo
   - **Internal Storage:** 8GB mínimo
6. Click **Finish**

#### Opción B: Dispositivo físico

1. Activa **Opciones de desarrollador** en tu Android
2. Activa **Depuración USB**
3. Conecta el teléfono por USB
4. Acepta el diálogo de depuración USB

---

### 6️⃣ Instalar voces en holandés (TTS)

#### En el emulador o dispositivo:

1. **Configuración** → **Sistema** → **Idioma e introducción**
2. **Síntesis de voz** → **Motor de Google TTS**
3. Click en **⚙️ Configuración** (del motor)
4. **Instalar datos de voz**
5. Busca **"Nederlands (Nederland)"** o **"Dutch (Netherlands)"**
6. Descarga e instala

**Alternativa (algunos dispositivos):**
- **Configuración** → **Accesibilidad** → **Síntesis de voz**

---

### 7️⃣ Compilar y ejecutar

#### a) Sync del proyecto:

En Android Studio, click en el ícono 🐘 **"Sync Project with Gradle Files"**

#### b) Verificar que no hay errores:

- ✅ `secrets.properties` existe y tiene tu API key
- ✅ `app/src/main/assets/model-es/` contiene las carpetas del modelo
- ✅ No hay líneas rojas en el código

#### c) Ejecutar:

1. Selecciona tu emulador/dispositivo en el dropdown superior
2. Click en ▶️ **Run 'app'** (o presiona Shift+F10)
3. Espera a que compile (~3-5 minutos la primera vez)
4. La app se instalará y abrirá automáticamente

---

## 🎯 Primer uso

1. **Acepta el permiso de micrófono** cuando lo solicite
2. Presiona 🎤 **Grabar**
3. Habla en español claramente (ej: "Hola, quiero aprender holandés")
4. Presiona ⏹ **Detener**
5. Espera ~5-10 segundos (Vosk transcribe en background)
6. Verás el texto transcrito en español
7. La traducción aparecerá en holandés
8. Presiona 🔊 para escuchar la pronunciación

---

## 🐛 Solución de problemas

### ❌ "Could not find BuildConfig"

**Solución:** Sync del proyecto y Rebuild:
```
Build → Clean Project
Build → Rebuild Project
```

### ❌ "INSTALL_FAILED_INSUFFICIENT_STORAGE"

**Solución:** El emulador/dispositivo no tiene espacio:
- Crea un nuevo emulador con más almacenamiento (8GB+)
- O libera espacio en tu dispositivo

### ❌ "Java heap space" durante compilación

**Solución:** Aumenta memoria de Gradle en `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m
```

### ❌ La app crashea al abrir

**Causas posibles:**
1. Falta `secrets.properties` con API key
2. Modelo Vosk mal instalado (estructura incorrecta)
3. Permisos no aceptados

**Revisar Logcat:**
- Abre pestaña **Logcat** (abajo)
- Busca líneas con `E/` (errores)
- Busca `DutchLearner` para ver logs específicos

### ❌ No transcribe mi voz (placeholder siempre)

**Causas:**
1. Modelo Vosk no se inicializó correctamente
2. Estructura de carpetas incorrecta

**Verificar en Logcat:**
```
D/DutchLearner: Vosk initialized: true  ← Debe decir "true"
```

Si dice `false`, revisa la estructura de `model-es/`

### ❌ "Voces en holandés no disponibles"

**Solución:** Instala voces TTS (ver paso 6 arriba)

---

## 📊 Especificaciones técnicas

### Límites y capacidades:

| Recurso | Valor |
|---------|-------|
| DeepL API (gratis) | 500,000 caracteres/mes |
| Traducciones aprox. | ~5,000 frases/mes |
| Modelo Vosk pequeño | ~39MB |
| Modelo Vosk grande | ~1.4GB |
| Tiempo transcripción | 5-10 seg por frase |
| Base de datos | SQLite (sin límite práctico) |

### Uso de memoria:

- **App en ejecución:** ~150-200MB
- **Con modelo pequeño:** +50MB
- **Con modelo grande:** +300MB

---

## 🔄 Actualizar el proyecto

```bash
# Obtener últimos cambios
git pull origin main

# Si agregaste modelo Vosk localmente, no se sobrescribirá
# Si actualizaste secrets.properties, tampoco (está en .gitignore)

# Sync y rebuild
# En Android Studio:
# File → Sync Project with Gradle Files
# Build → Rebuild Project
```

---

## 📞 Soporte

Si tienes problemas:

1. Revisa esta guía completa
2. Busca el error en **Logcat**
3. Abre un **Issue** en GitHub con:
   - Descripción del problema
   - Log completo del error
   - Versión de Android Studio
   - Sistema operativo

---

## ✅ Checklist de instalación exitosa

- [ ] Repositorio clonado
- [ ] `secrets.properties` creado con API key válida
- [ ] Modelo Vosk descargado y en `app/src/main/assets/model-es/`
- [ ] Android Studio abierto y proyecto sincronizado
- [ ] Emulador/dispositivo configurado con 8GB+ almacenamiento
- [ ] Voces en holandés instaladas en el dispositivo
- [ ] App compila sin errores
- [ ] App ejecuta y muestra interfaz principal
- [ ] Permiso de micrófono aceptado
- [ ] Grabación → Transcripción → Traducción funciona

Si marcaste todo ✅, ¡estás listo para usar Dutch Learner! 🎉

---

**Última actualización:** Diciembre 2024  
**Versión del proyecto:** 0.1.0