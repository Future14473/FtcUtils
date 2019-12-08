package org.futurerobotics.jargon.ftcbridge

import org.futurerobotics.jargon.blocks.Block
import org.futurerobotics.jargon.blocks.control.MotorsBlock
import org.futurerobotics.jargon.linalg.Vec
import org.futurerobotics.jargon.linalg.get
import org.futurerobotics.jargon.linalg.mapToVec
import org.futurerobotics.jargon.math.TAU

/**
 * A [MotorsBlock] block that uses [getBulkData] to read data.
 */
class BulkMotorBlock(
    motors: List<FtcMotor>,
    private val getBulkData: () -> MotorBulkData
) : Block(Processing.OUT_FIRST), MotorsBlock {

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
        motorVolts.get?.let { volts ->
            motors.forEachIndexed { i, motor ->
                motor.voltage = volts[i]
            }
        }
    }
}
