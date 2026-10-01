package com.example.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

class AudioRecordManager(
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onRmsLevel: (Float) -> Unit,
    private val onSpeechEnded: (ByteArray) -> Unit,
    private val onBargeIn: () -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

    @Volatile
    var isListening = false
        private set

    @Volatile
    var isJarvisSpeaking = false

    private val speechBuffer = ByteArrayOutputStream()
    private var isUserTalking = false
    private var silenceFramesCount = 0
    private val silenceThresholdFrames = 15 // ~750ms of silence after speech

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope) {
        if (isListening) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isListening = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(2048)

                while (isActive && isListening) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        val rms = calculateRms(chunk)
                        onRmsLevel(rms)

                        // Check Barge-In if JARVIS is speaking
                        if (isJarvisSpeaking) {
                            if (rms > 0.35f) { // High threshold to prevent speaker echo
                                onBargeIn()
                            }
                            continue
                        }

                        // Stream chunk to live socket
                        onAudioChunk(chunk)

                        // Voice Activity Detection for automated turns
                        val speechThreshold = 0.08f
                        if (rms > speechThreshold) {
                            isUserTalking = true
                            silenceFramesCount = 0
                            speechBuffer.write(chunk)
                        } else if (isUserTalking) {
                            speechBuffer.write(chunk)
                            silenceFramesCount++
                            if (silenceFramesCount >= silenceThresholdFrames) {
                                // End of speech detected!
                                isUserTalking = false
                                silenceFramesCount = 0
                                val completeUtterance = speechBuffer.toByteArray()
                                speechBuffer.reset()
                                if (completeUtterance.size > sampleRate) { // at least 0.5s of audio
                                    onSpeechEnded(completeUtterance)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isListening = false
        }
    }

    fun stopRecording() {
        isListening = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
        speechBuffer.reset()
        isUserTalking = false
        onRmsLevel(0f)
    }

    private fun calculateRms(buffer: ByteArray): Float {
        var sum = 0.0
        val shortCount = buffer.size / 2
        if (shortCount == 0) return 0f

        for (i in 0 until shortCount) {
            val sample = (buffer[i * 2].toInt() and 0xFF) or (buffer[i * 2 + 1].toInt() shl 8)
            val normalized = sample.toShort() / 32768.0
            sum += normalized * normalized
        }
        val rms = sqrt(sum / shortCount).toFloat()
        return (rms * 3.5f).coerceIn(0f, 1f)
    }
}
