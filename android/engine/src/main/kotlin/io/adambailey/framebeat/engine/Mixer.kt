package io.adambailey.framebeat.engine

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.tanh

/**
 * Mixes struck voices into mono float blocks on its own frame clock. The live
 * output pulls blocks from it on the audio thread; [renderOffline] pulls one
 * long block. Either way the same voices render the same way.
 *
 * Threads: [trigger] may be called from any thread. It builds the voice there,
 * so the audio thread does not allocate one. [render] belongs to one thread.
 */
class Mixer(
    val sampleRate: Int,
    private val noise: DoubleArray = NoiseTable.make(sampleRate),
    maxBlockFrames: Int = 4096,
) {
    private val pending = ConcurrentLinkedQueue<Voice>()
    // Room for far more voices than can overlap, so adding one never resizes on the audio thread.
    private val active = ArrayList<Voice>(512)
    private val mix = DoubleArray(maxBlockFrames)

    /** Frames rendered so far: the frame the next [render] call starts on. */
    @Volatile
    var frame: Long = 0
        private set

    /** Voices still sounding or waiting to start, as of the last [render]. */
    val activeVoices: Int get() = active.size

    /**
     * Strikes [spec] at [atFrame]. A frame already rendered plays at the start
     * of the next block, as drumAudio.ts clamps `when` to `currentTime`.
     */
    fun trigger(spec: VoiceSpec, atFrame: Long) {
        pending.add(Voice(spec, sampleRate, noise, atFrame))
    }

    fun trigger(sound: Sound, atFrame: Long) = trigger(Voices.forSound(sound), atFrame)

    fun triggerDing(atFrame: Long) = trigger(Voices.ding, atFrame)

    /**
     * Drops every voice, sounding or waiting, without moving [frame]. Call it
     * from the [render] thread: the live output clears as each start begins, so
     * a tail cut off by the last stop does not resume mid-sound.
     */
    fun clear() {
        pending.clear()
        active.clear()
    }

    /** Renders the next [frames] frames into [out] from [offset], and advances [frame]. */
    fun render(out: FloatArray, offset: Int = 0, frames: Int = out.size - offset) {
        var done = 0
        while (done < frames) {
            val n = min(frames - done, mix.size)
            renderBlock(out, offset + done, n)
            done += n
        }
    }

    private fun renderBlock(out: FloatArray, offset: Int, frames: Int) {
        val blockStart = frame
        while (true) active.add(pending.poll() ?: break)
        if (active.isEmpty()) {
            out.fill(0f, offset, offset + frames)
        } else {
            for (v in active.indices) active[v].mixInto(mix, blockStart, frames)
            for (i in 0 until frames) {
                // The master gain, then a soft clip in place of the web's
                // DynamicsCompressorNode: a safety net, not a timbral match.
                out[offset + i] = tanh(mix[i] * MASTER_GAIN).toFloat()
                mix[i] = 0.0
            }
            active.removeAll { it.finished }
        }
        frame = blockStart + frames
    }

    companion object {
        /** drumAudio.ts's `masterGain.gain.value`. */
        const val MASTER_GAIN = 0.85

        /** Renders [frames] frames of the given strikes, each a (frame, voice) pair. */
        fun renderOffline(
            sampleRate: Int,
            frames: Int,
            strikes: List<Pair<Long, VoiceSpec>>,
            noise: DoubleArray = NoiseTable.make(sampleRate),
        ): FloatArray {
            val mixer = Mixer(sampleRate, noise)
            for ((atFrame, spec) in strikes) mixer.trigger(spec, atFrame)
            return FloatArray(frames).also { mixer.render(it) }
        }
    }
}
