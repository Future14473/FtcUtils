package org.futurerobotics.jargon.ftcbridge

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.TimestampedI2cData
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Base class for user defined coroutine op modes for awesome concurrency stuff.
 * This is meant to emulate `LinearOpMode`, while using coroutines.
 *
 * Instead of `Thread.interrupted`, the entire coroutine will be cancelled when
 * the op mode is stopped. **In this case,`CancellationException` will be thrown whenever (most)
 * suspend functions are called.** If cleanup is wanted, use either a try/finally block,
 * or explicitly check for coroutines being active using [CoroutineContext.isActive].
 *
 * An `initialContext` can be provided.
 */
abstract class CoroutineOpMode(initialContext: CoroutineContext = EmptyCoroutineContext) : OpMode() {

    private val mainScope = CoroutineScope(initialContext)
    private var opModeJob: Job? = null
    @Volatile
    private var startedDeferred: CompletableDeferred<Unit>? = null
    /**
     * A [Deferred] that is complete when the start button has been pressed.
     */
    val started: Deferred<Unit>
        get() = startedDeferred ?: throw IllegalStateException("Op mode has not yet been initialized!!")
    //exception handling
    private var exception: Throwable? = null

    /**
     * Override this method and place your awesome coroutine code here.
     *
     * THe coroutine may be cancelled if stop is manually pressed. **In this case,`CancellationException` will be
     * thrown whenever (most) suspend functions are called.**
     *
     * One may typically start this function using `= coroutineScope { ... }` to launch a series of coroutines that
     * will live and die together (see [coroutineScope]).
     *
     * Other notes:
     *
     * The convention for coroutine functions is:
     * - `suspend fun foo()` if it performs some actions that may suspend the current
     *   coroutine. (Use `launch { foo() } if concurrency is really wanted ).
     * - `fun CoroutineScope.foo()` if it immediately returns but _launches_ another coroutine (using
     *   the supplied scope).
     */
    protected abstract suspend fun runOpMode()

    /**
     * Suspends the current coroutine op mode until start has been pressed.
     *
     * Can be called from _any_ coroutine.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    protected suspend fun waitForStart() {
        started.await()
    }

    /**
     * Allows other coroutines to run a bit, when you have nothing to do (calls [yield]).
     *
     * Spin-waiting is generally discouraged for coroutines, but sometimes you have no better option.
     *
     * @throws CancellationException if coroutine is cancelled.
     */
    @Throws(CancellationException::class)
    protected suspend fun idle() {
        yield()
    }

    /**
     * Sleeps for the given amount of milliseconds, or until the coroutine is cancelled.
     *
     * This simply calls [delay].
     */
    @Throws(CancellationException::class)
    protected suspend fun sleep(milliseconds: Long) {
        delay(milliseconds)
    }

    /**
     * If the op mode is started and still running.
     *
     * This will [idle] (call [yield]) if is active, as this is intended for use in loops.
     *
     * This will _not_ throw [CancellationException] if coroutine is cancelled.
     */
    protected suspend fun opModeIsActive(): Boolean {
        val isActive = isStarted && coroutineContext.isActive
        if (isActive)
            try {
                idle()
            } catch (_: CancellationException) {
            }
        return isActive
    }

    /**
     * Has the op mode been started (start button is pressed)?
     * @see waitForStart
     */
    protected val isStarted: Boolean get() = started.isCompleted

    /** From the normal op mode */
    final override fun init() {
        exception = null
        startedDeferred = CompletableDeferred()
        launchOpMode()
    }

    /** From the normal op mode */
    final override fun init_loop() {
        doLoop()
    }

    /** From the normal op mode */
    final override fun start() {
        startedDeferred!!.complete(Unit)
    }

    /** From the normal op mode */
    final override fun loop() {
        doLoop()
    }

    /** From the normal op mode */
    final override fun stop() {
        opModeJob?.let {
            it.cancel("Op mode stop")
            runBlocking {
                it.join()
            }
            opModeJob = null
            exception = null
            startedDeferred = null
        }
    }

    private fun doLoop() {
        Thread.yield()
        try {
            Thread.sleep(1)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        // if there is a exception in user code, throw it.
        exception?.let {
            throw it
        }
    }

    private fun launchOpMode() {
        opModeJob = mainScope.launch {
            RobotLog.vv("CoroutineOpMode", "CoroutineOpMode starting...")
            try {
                runOpMode()
                requestOpModeStop()
            } catch (e: CancellationException) {
                RobotLog.dd(
                    "CoroutineOpMode",
                    "CoroutineOpMode received an CancellationException; shutting down this coroutine op mode"
                )
                throw e //normal.
            } catch (e: Exception) {
                exception = e
            } finally {
                //from linear op mode
                //flush telemetry
                TimestampedI2cData.suppressNewHealthWarningsWhile {
                    val telemetry = telemetry
                    if (telemetry is TelemetryInternal) {
                        telemetry.msTransmissionInterval = 0
                        telemetry.tryUpdateIfDirty()
                    }
                }
                RobotLog.vv("CoroutineOpMode", "...terminating CoroutineOpMode")
            }
        }
    }

    /***/
    final override fun internalPostInitLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }

    /***/
    final override fun internalPostLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }
}
