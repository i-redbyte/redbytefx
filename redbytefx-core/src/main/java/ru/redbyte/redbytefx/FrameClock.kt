package ru.redbyte.redbytefx

/**
 * Elapsed animation time for a time uniform, fed by frame timestamps in nanoseconds.
 *
 * Time accumulates as whole nanoseconds, so hours of frames do not drift the way repeated float
 * additions do; only the reported value is a [Float]. The first tick and the first tick after
 * [pause] start a new run without a jump. A step longer than [maxStepNanos], such as a stall or a
 * return from the background, advances by [maxStepNanos]. Timestamps that go backwards add nothing.
 *
 * Not thread-safe: tick it from the thread that renders.
 */
public class FrameClock(
    private val maxStepNanos: Long = DEFAULT_MAX_STEP_NANOS,
) {
    init {
        require(maxStepNanos > 0L) { "Maximum step must be positive, was $maxStepNanos" }
    }

    private var elapsedNanos: Long = 0L
    private var lastFrameNanos: Long = NO_FRAME

    /** Seconds accumulated so far. */
    public val seconds: Float
        get() = (elapsedNanos / NANOS_PER_SECOND).toFloat()

    /** Advances to [frameNanos] and returns [seconds]. */
    public fun tick(frameNanos: Long): Float {
        val last = lastFrameNanos
        if (last != NO_FRAME) {
            elapsedNanos += (frameNanos - last).coerceIn(0L, maxStepNanos)
        }
        lastFrameNanos = frameNanos
        return seconds
    }

    /** The next [tick] starts a new run instead of counting the time spent paused. */
    public fun pause() {
        lastFrameNanos = NO_FRAME
    }

    /** [seconds] shifted by [offsetSeconds], computed in double precision. */
    public fun secondsWithOffset(offsetSeconds: Float): Float =
        (elapsedNanos / NANOS_PER_SECOND + offsetSeconds).toFloat()

    public companion object {
        /** A quarter second: longer frame gaps are treated as a pause, not as animation. */
        public const val DEFAULT_MAX_STEP_NANOS: Long = 250_000_000L
    }
}

private const val NO_FRAME = Long.MIN_VALUE
private const val NANOS_PER_SECOND = 1_000_000_000.0
