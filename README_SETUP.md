# Configuración del modelo Vosk

1. Descarga el modelo de español:
   https://alphacephei.com/vosk/models
   - Modelo pequeño: vosk-model-small-es-0.42 (~39MB)
   - Modelo grande: vosk-model-es-0.42 (~1.4GB)

2. Extrae el archivo ZIP

3. Copia el contenido a:
   

4. La estructura debe quedar:
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
