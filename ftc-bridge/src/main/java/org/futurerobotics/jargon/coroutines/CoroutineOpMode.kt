package org.futurerobotics.jargon.coroutines

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.RobotLog
import kotlinx.coroutines.*
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryInternal
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Base class for user defined coroutine op modes for awesome concurrency stuff.
 *
 * An `initialContext` can be provided.
 */
@Suppress("KDocMissingDocumentation")
abstract class CoroutineOpMode(initialContext: CoroutineContext = EmptyCoroutineContext) : OpMode(), CoroutineScope {

    private var inited = false
    private val job = Job()
    final override val coroutineContext: CoroutineContext = initialContext + job
    //Waits for start
    private val startGate: CoroutineGate = CoroutineGate()
    //background processes
    private val backgroundProcesses: Queue<Job> = ConcurrentLinkedQueue()
    //exception handling
    private lateinit var thread: Thread
    private var exception: Throwable? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        exception = throwable
        thread.interrupt()
    }

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
     * [launch]es a new coroutine that runs the given [block], that the op mode will wait for
     * to complete before stopping by itself.
     *
     * This is so that users do not have to worry about joining for a process that just runs in the background.
     */
    protected fun backgroundProcess(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        backgroundProcesses.add(launch(context, block = block))
    }

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
        thread = Thread.currentThread()

        launch(exceptionHandler) {
            RobotLog.v("CoroutineOpMode starting...")
            try {
                runOpMode()
                while (backgroundProcesses.isNotEmpty()) {
                    backgroundProcesses.remove().join()
                }
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
                job.cancel("OpMode Stop")
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
@UseExperimental(ObsoleteCoroutinesApi::class)
abstract class ThreadPoolCoroutineOpMode(private val nThreads: Int = 4) : CoroutineOpMode(
    newFixedThreadPoolContext(nThreads, "ThreadPoolCoroutineOpMode dispatcher")
) {

    private var executorCoroutineDispatcher: ExecutorCoroutineDispatcher? = null
    final override fun doStop() {
        executorCoroutineDispatcher?.close()
    }
}
