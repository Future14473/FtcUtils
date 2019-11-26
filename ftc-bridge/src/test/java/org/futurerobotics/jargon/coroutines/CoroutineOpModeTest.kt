package org.futurerobotics.jargon.coroutines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

internal class CoroutineTest {

    @Test
    fun exception() = runBlocking<Unit> {
        try {
            coroutineScope {
                launch {
                    delay(1000)
                    println("After delay")
                }
//               throw Exception()
            }
        } catch (e: Exception) {
            println("Caught $e")
        }
    }
}
