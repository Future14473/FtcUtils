package org.futurerobotics.jargon.coroutines

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base class for user defined coroutine op modes for awesome concurrency stuff.
 *
 * A starting [CoroutineContext] must be supplied in [initContext].
 */
@Suppress("KDocMissingDocumentation")
abstract class CoroutineOpMode : OpMode(), CoroutineScope {

    private var inited = false
    private val job = Job()
    final override lateinit var coroutineContext: CoroutineContext
        private set
    //Waits for start
    private val startGate: CoroutineGate = CoroutineGate()
    //exception handling
    private lateinit var thread: Thread
    private var exception: Throwable? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        exception = throwable
        thread.interrupt()
    }

    /**
     * Runs right before [runOpMode], in the non-suspending world.
     *
     * You may initialize executors or thread stuff here, and
     * return the initial [CoroutineContext] that will be used.
     *
     * If you are clueless return [EmptyCoroutineContext] which will use
     * default settings. (This may impact the speed of the rest of your android app if it
     * hogs processing time, though)
     */
    protected abstract fun initContext(): CoroutineContext

    /**
     * Override this method and place your awesome coroutine code here.
     *
     * You can [cancel] this coroutine scope to end the coroutine prematurely.
     */
    protected abstract suspend fun runOpMode()

    /**
     * Runs after [runOpMode], in the non-suspending world.
     *
     * IMPORTANT: Use to clean up any thread resources here.
     */
    protected abstract fun doStop()

    /**
     * Pauses the linear op mode until start has been pressed,
     * or job is canceled.
     */
    protected suspend fun waitForStart() {
        startGate.wait()
    }

    /**
     * Allows other coroutines to run a bit, as if it has nothing better to do.
     *
     * (calls [yield])
     *
     * Also may throw [CancellationException] if the op mode has ended.
     *
     * Can when you have nothing to do.
     */
    protected suspend fun idle() {
        yield()
    }

    /**
     * Has the op mode been started?
     * @see waitForStart
     */
    protected val isStarted: Boolean get() = startGate.isOpen

    @UseExperimental(ObsoleteCoroutinesApi::class)
    final override fun init() {
        //reset
        inited = true
        coroutineContext = initContext() + job
        thread = Thread.currentThread()

        launch(exceptionHandler) {
            RobotLog.v("CoroutineOpMode starting...")
            try {
                runOpMode()
                RobotLog.v("CoroutineOpMode ended successfully.")
                requestOpModeStop()
            } finally {
                RobotLog.v("CoroutineOpMode terminating...")
            }
        }
    }

    final override fun init_loop() {
        doLoop()
    }

    final override fun start() {
        startGate.open()
    }

    final override fun loop() {
        doLoop()
    }

    final override fun stop() {
        try {
            if (inited) {
                //reset
                exception = null
                job.cancel("OpMode Manual Stop")
                runBlocking {
                    job.join()
                }
            }
        } finally {
            doStop()
        }
    }

    private fun doLoop() {
        Thread.yield()
        Thread.sleep(1)
        exception?.let { throw it }
    }

    final override fun internalPostInitLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }

    final override fun internalPostLoop() {
        (telemetry as? TelemetryInternal)?.tryUpdateIfDirty()
    }
}

/**
 * A [CoroutineOpMode] that uses a thread pool of size [nThreads] as its dispatcher.
 *
 * The most common op mode.
 */
abstract class ThreadPoolCoroutineOpMode(private val nThreads: Int = 4) : CoroutineOpMode() {

    private var executorCoroutineDispatcher: ExecutorCoroutineDispatcher? = null
    @UseExperimental(ObsoleteCoroutinesApi::class)
    final override fun initContext(): CoroutineContext {
        executorCoroutineDispatcher = newFixedThreadPoolContext(nThreads, "ThreadPoolCoroutineOpMode dispatcher")
        return executorCoroutineDispatcher!!
    }

    final override fun doStop() {
        executorCoroutineDispatcher?.close()
    }
}
