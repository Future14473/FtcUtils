package org.futurerobotics.jargon.ftcbridge

import org.futurerobotics.jargon.blocks.BaseBlock
import org.futurerobotics.jargon.blocks.control.MotorsBlock
import org.futurerobotics.jargon.math.TAU
import org.openftc.revextensions2.RevBulkData

class FtcBulkMotor(
    private val getBulkData: () -> RevBulkData,
    motors: List<FtcMotor>
) : BaseBlock(Processing.OUT_FIRST), MotorsBlock {

    override val motorPositions: Output<List<Double>> = newOutput()
    override val motorVelocities: Output<List<Double>> = newOutput()
    override val motorVolts: Input<List<Double>?> = newInput(true)
    private val motors = motors.toList()
    override val numMotors: Int get() = motors.size
    override fun Context.process() {
        val data = getBulkData()
        motorPositions.set = motors.map {
            data.getMotorCurrentPosition(it.motor) / it.ticksPerRev * TAU
        }
        motorVelocities.set = motors.map {
            data.getMotorVelocity(it.motor) / it.ticksPerRev * TAU
        }
    }
}
