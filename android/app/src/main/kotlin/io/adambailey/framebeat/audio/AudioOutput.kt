package io.adambailey.framebeat.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Process
import io.adambailey.framebeat.engine.Mixer
import io.adambailey.framebeat.engine.Sound
import kotlin.math.max
import kotlin.math.min

/**
 * Plays [mixer] through a low-latency `AudioTrack`: the Android counterpart of
 * LiveAudioEngine.swift and drumAudio.ts's `AudioContext`. One thread pulls
 * blocks from the mixer and writes them to the track. The mixer's frame count
 * is the audio clock: drum taps strike at it, and the sequencer books against
 * it through `Mixer.seconds` and `Mixer.book`.
 *
 * Sound only plays between [start] and [stop]. The activity starts the output
 * when it comes to the foreground and stops it when it leaves, so nothing
 * plays in the background. Stopping fades out over [FADE_SECONDS] and lets the
 * fade play out before the track is released, so a ringing bell does not click.
 *
 * Threads: call [start], [stop], and [latency] from the main thread. [play] and
 * [playDing] may be called from any thread.
 */
class AudioOutput(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    /** The device's native rate, so the track can take the low-latency path without resampling. */
    val sampleRate: Int = audioManager.intProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: DEFAULT_SAMPLE_RATE

    /** The device's native burst: one block from the mixer per write. */
    private val burst: Int = audioManager.intProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: DEFAULT_BURST

    val mixer = Mixer(sampleRate, maxBlockFrames = burst)

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    /** [Mixer.frame] when the current track started: its frame 0. */
    private var startFrame = 0L
    private val timestamp = AudioTimestamp()

    /** Opens the track and starts pulling from the mixer. Does nothing while already running. */
    fun start() {
        if (thread != null) return
        val track = buildTrack()
        this.track = track
        startFrame = mixer.frame
        running = true
        thread = Thread({ run(track) }, "FrameBeat audio").apply { start() }
    }

    /** Fades out, waits for the fade to be heard, and releases the track. Does nothing while stopped. */
    fun stop() {
        val thread = thread ?: return
        running = false
        thread.join()
        this.thread = null
        track = null
    }

    /** Strikes [sound] now: at the start of the next block, as drumAudio.ts clamps `when` to `currentTime`. */
    fun play(sound: Sound) = mixer.trigger(sound, mixer.frame)

    /** Rings the bell now. */
    fun playDing() = mixer.triggerDing(mixer.frame)

    /**
     * Seconds from the mixer's clock to the frame being heard now: the web's
     * `outputLatency + baseLatency`, which the sequencer's frame sync adds so
     * a step lights up when it is heard. 0 while stopped.
     */
    val latency: Double
        get() {
            val track = track ?: return 0.0
            val pending = mixer.frame - startFrame
            val heard = if (track.getTimestamp(timestamp)) {
                timestamp.framePosition + (System.nanoTime() - timestamp.nanoTime) * sampleRate / 1e9
            } else {
                // No timestamp yet (the first few blocks): assume the buffer is full.
                (pending - track.bufferSizeInFrames).toDouble()
            }
            return max(0.0, (pending - heard) / sampleRate)
        }

    private fun buildTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(max(minBytes, START_BURSTS * burst * Float.SIZE_BYTES))
            .build()
        // Start with two bursts queued; [run] adds a burst each time the track underruns.
        track.bufferSizeInFrames = min(START_BURSTS * burst, track.bufferCapacityInFrames)
        return track
    }

    /** The audio thread: render, write, repeat, then fade out and release. */
    private fun run(track: AudioTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        // A tail cut off by the last stop, or a strike made while stopped, stays silent.
        mixer.clear()
        val block = FloatArray(burst)
        var underruns = track.underrunCount
        track.play()
        try {
            while (running) {
                mixer.render(block)
                if (!track.writeAll(block)) return
                val now = track.underrunCount
                if (now > underruns) {
                    underruns = now
                    track.bufferSizeInFrames = min(track.bufferSizeInFrames + burst, track.bufferCapacityInFrames)
                }
            }
            // Fade whatever is still ringing to silence, then write a whole
            // buffer of silence: a blocking write returns once its frames fit,
            // so by then the fade has been heard and cutting cuts silence.
            val fadeFrames = (FADE_SECONDS * sampleRate).toInt()
            val total = fadeFrames + track.bufferSizeInFrames
            var written = 0
            while (written < total) {
                mixer.render(block)
                for (i in block.indices) block[i] *= max(0f, 1f - (written + i).toFloat() / fadeFrames)
                if (!track.writeAll(block)) return
                written += block.size
            }
            track.pause()
            track.flush()
        } finally {
            track.release()
        }
    }

    private companion object {
        /** A fallback when the device does not report its output rate. */
        const val DEFAULT_SAMPLE_RATE = 48_000

        /** A fallback when the device does not report its burst size. */
        const val DEFAULT_BURST = 256

        /** Bursts queued in the track at start. */
        const val START_BURSTS = 2

        /** How long [stop] fades out a ringing voice. */
        const val FADE_SECONDS = 0.01

        fun AudioManager.intProperty(key: String): Int? = getProperty(key)?.toIntOrNull()?.takeIf { it > 0 }

        /** Writes all of [block], blocking. False when the track has failed and the thread should end. */
        fun AudioTrack.writeAll(block: FloatArray): Boolean {
            var offset = 0
            while (offset < block.size) {
                val n = write(block, offset, block.size - offset, AudioTrack.WRITE_BLOCKING)
                if (n < 0) return false
                offset += n
            }
            return true
        }
    }
}
