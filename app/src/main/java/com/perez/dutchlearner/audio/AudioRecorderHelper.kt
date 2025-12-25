package com.perez.dutchlearner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Properties

/**
 * Grabador de audio compatible con Vosk
 * Graba en formato PCM WAV a 16kHz mono
 */
class AudioRecorderHelper {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
    }

    /**
     * Inicia la grabación y guarda en formato WAV
     */
    @SuppressLint("MissingPermission")
    suspend fun startRecording(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext false
            }

            audioRecord?.startRecording()
            isRecording = true

            // Grabar en un thread separado
            recordingThread = Thread {
                writeAudioDataToFile(outputFile)
            }
            recordingThread?.start()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Detiene la grabación
     */
    fun stopRecording() {
        isRecording = false

        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
            }
            release()
        }
        audioRecord = null

        recordingThread?.join()
        recordingThread = null
    }

    /**
     * Escribe datos de audio a archivo WAV
     */
    private fun writeAudioDataToFile(outputFile: File) {
        val data = ByteArray(BUFFER_SIZE)
        val tempFile = File(outputFile.parent, "temp_${outputFile.name}")

        try {
            FileOutputStream(tempFile).use { os ->
                while (isRecording) {
                    val read = audioRecord?.read(data, 0, data.size) ?: 0
                    if (read > 0) {
                        os.write(data, 0, read)
                    }
                }
            }

            // Convertir raw PCM a WAV con headers
            convertPcmToWav(tempFile, outputFile)
            tempFile.delete()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Convierte archivo PCM raw a formato WAV con headers
     */
    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val pcmData = pcmFile.readBytes()
        val totalDataLen = pcmData.size + 36
        val channels = 1
        val byteRate = SAMPLE_RATE * channels * 2

        FileOutputStream(wavFile).use { os ->
            // WAV header
            writeString(os, "RIFF")
            writeInt(os, totalDataLen)
            writeString(os, "WAVE")

            // fmt chunk
            writeString(os, "fmt ")
            writeInt(os, 16) // fmt chunk size
            writeShort(os, 1) // audio format (PCM)
            writeShort(os, channels.toShort())
            writeInt(os, SAMPLE_RATE)
            writeInt(os, byteRate)
            writeShort(os, (channels * 2).toShort()) // block align
            writeShort(os, 16) // bits per sample

            // data chunk
            writeString(os, "data")
            writeInt(os, pcmData.size)
            os.write(pcmData)
        }
    }

    private fun writeString(os: FileOutputStream, value: String) {
        os.write(value.toByteArray())
    }

    private fun writeInt(os: FileOutputStream, value: Int) {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(value)
        os.write(buffer.array())
    }

    private fun writeShort(os: FileOutputStream, value: Short) {
        val buffer = ByteBuffer.allocate(2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(value)
        os.write(buffer.array())
    }
}