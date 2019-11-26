package org.futurerobotics.jargon.ftcbridge

import org.futurerobotics.jargon.blocks.Block
import org.futurerobotics.jargon.blocks.control.MotorInterface
import org.futurerobotics.jargon.linalg.Vec
import org.futurerobotics.jargon.linalg.mapToVec
import org.futurerobotics.jargon.math.TAU
import org.openftc.revextensions2.RevBulkData

/**
 * A [MotorInterface] block that uses [getBulkData] to read data.
 */
class FtcBulkMotor(
    private val getBulkData: () -> RevBulkData,
    motors: List<FtcMotor>
) : Block(Processing.OUT_FIRST), MotorInterface {

    override val motorPositions: Output<Vec> = newOutput()
    override val motorVelocities: Output<Vec> = newOutput()
    override val motorVolts: Input<Vec?> = newOptionalInput()
    private val motors = motors.toList()
    override val numMotors: Int get() = motors.size
    override fun Context.process() {
        val data = getBulkData()
        motorPositions.set = motors.mapToVec {
            data.getMotorCurrentPosition(it.motor) / it.ticksPerRev * TAU
        }
        motorVelocities.set = motors.mapToVec {
            data.getMotorVelocity(it.motor) / it.ticksPerRev * TAU
        }
    }
}
