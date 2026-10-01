package com.example.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AudioTrackPlayer(
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackEnded: () -> Unit
) {
    private var audioTrack: AudioTrack? = null
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null
    private val sampleRate = 24000 // Standard Gemini Live output sample rate
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @Volatile
    var isPlaying = false
        private set

    fun initialize(scope: CoroutineScope) {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = minBufferSize.coerceAtLeast(8192)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playbackJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val data = audioQueue.receive()
                    if (!isPlaying) {
                        isPlaying = true
                        onPlaybackStarted()
                    }
                    audioTrack?.write(data, 0, data.size)

                    // If queue is now empty, wait briefly before declaring playback ended
                    if (audioQueue.isEmpty) {
                        isPlaying = false
                        onPlaybackEnded()
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    fun playChunk(pcmData: ByteArray) {
        audioQueue.trySend(pcmData)
    }

    fun interrupt() {
        while (audioQueue.tryReceive().isSuccess) {
            // Drain remaining queued chunks
        }
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            // ignore
        }
        if (isPlaying) {
            isPlaying = false
            onPlaybackEnded()
        }
    }

    fun release() {
        interrupt()
        playbackJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
    }
}
