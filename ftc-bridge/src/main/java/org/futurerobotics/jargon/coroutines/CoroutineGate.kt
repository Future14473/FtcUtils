package org.futurerobotics.jargon.coroutines

import kotlinx.coroutines.CompletableDeferred

/**
 * Mechanism for waiting for a condition to be true; or the gate to be opened, via coroutines.
 */
class CoroutineGate {

    private val deffered = CompletableDeferred<Unit>()
    /** Checks if this gate was open. */
    val isOpen: Boolean get() = deffered.isCompleted

    /** Opens the gate, so that all coroutines can pass. */
    fun open() {
        deffered.complete(Unit)
    }

    /** Waits until `open` is called, or returns immediately if already opened. */
    suspend fun wait() {
        deffered.await()
    }
}
